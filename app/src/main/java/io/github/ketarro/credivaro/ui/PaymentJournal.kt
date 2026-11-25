package io.github.ketarro.credivaro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.ketarro.credivaro.R
import io.github.ketarro.credivaro.database.PaymentRepository
import io.github.ketarro.credivaro.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Currency
import java.util.UUID

@Composable
private fun paymentMoney(value: Money): String {
    val c = Currency.getInstance(value.currency)
    return NumberFormat.getCurrencyInstance(LocalConfiguration.current.locales[0]).apply {
        currency = c
        minimumFractionDigits = c.defaultFractionDigits
        maximumFractionDigits = c.defaultFractionDigits
    }.format(BigDecimal.valueOf(value.minorUnits, c.defaultFractionDigits))
}

@Composable
internal fun PaymentJournal(obligation: Obligation, repository: PaymentRepository, enabled: Boolean) {
    var rows by remember { mutableStateOf<List<PaymentHistoryItem>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var correcting by remember { mutableStateOf<RecordedPayment?>(null) }
    var cancelling by remember { mutableStateOf<RecordedPayment?>(null) }
    LaunchedEffect(repository, obligation.id, retry) {
        loadError = false
        rows = null
        try {
            repository.observeHistoryFor(obligation.id).collect { rows = it }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadError = true
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.payment_history), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.payment_fact_hint), style = MaterialTheme.typography.bodySmall)
            when {
                loadError -> {
                    Text(stringResource(R.string.payment_read_error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text(stringResource(R.string.storage_retry)) }
                }
                rows == null -> CircularProgressIndicator(Modifier.size(24.dp))
                rows.orEmpty().isEmpty() -> Text(stringResource(R.string.payment_empty))
                else -> rows.orEmpty().forEach { item ->
                    val row = item.payment
                    HorizontalDivider()
                    Column(Modifier.fillMaxWidth().testTag("payment-row"), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(paymentMoney(row.amount), style = MaterialTheme.typography.titleMedium)
                        Text(row.bankDate.toString())
                        val a = row.allocation
                        Text(stringResource(R.string.payment_principal) + ": " + paymentMoney(a.principal))
                        Text(stringResource(R.string.payment_interest) + ": " + paymentMoney(a.interest))
                        Text(stringResource(R.string.payment_fees) + ": " + paymentMoney(a.fees))
                        Text(stringResource(R.string.payment_unallocated) + ": " + paymentMoney(a.unallocated))
                        row.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text(stringResource(when (item.status) {
                            PaymentHistoryStatus.ACTIVE -> R.string.ux_payment_active
                            PaymentHistoryStatus.REPLACED -> R.string.ux_payment_replaced
                            PaymentHistoryStatus.CANCELLED -> R.string.ux_payment_cancelled
                        }), modifier = Modifier.testTag("payment-status-" + item.status.name))
                        item.correctionReason?.let { Text(stringResource(R.string.ux_reason) + ": " + it) }
                        if (item.status == PaymentHistoryStatus.ACTIVE && enabled && obligation.status == ObligationStatus.ACTIVE) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { correcting = row }, modifier = Modifier.testTag("correct-payment")) { Text(stringResource(R.string.ux_correct)) }
                                TextButton(onClick = { cancelling = row }, modifier = Modifier.testTag("cancel-payment")) { Text(stringResource(R.string.ux_cancel_payment)) }
                            }
                        }
                    }
                }
            }
        }
    }
    correcting?.let { value -> key(value.id) {
        PaymentEntryDialog(obligation, repository, target = value, onSaved = { correcting = null }, close = { correcting = null })
    } }
    cancelling?.let { value -> key(value.id) { PaymentCancelDialog(obligation, repository, value) { cancelling = null } } }
}

@Composable
internal fun PaymentEntryDialog(obligation: Obligation, repository: PaymentRepository,
    target: RecordedPayment? = null, onSaved: () -> Unit, close: () -> Unit) {
    val initialDate = target?.bankDate?.toString() ?: LocalDate.now(obligation.bankZone).toString()
    fun raw(m: Money?) = m?.let { BigDecimal.valueOf(it.minorUnits, obligation.currency.defaultFractionDigits).toPlainString() }.orEmpty()
    var date by remember { mutableStateOf(initialDate) }
    var amount by remember { mutableStateOf(raw(target?.amount)) }
    var principal by remember { mutableStateOf(raw(target?.allocation?.principal)) }
    var interest by remember { mutableStateOf(raw(target?.allocation?.interest)) }
    var fees by remember { mutableStateOf(raw(target?.allocation?.fees)) }
    var notes by remember { mutableStateOf(target?.notes.orEmpty()) }
    var reason by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(target != null) }
    var discard by remember { mutableStateOf(false) }
    val correctionOperation = remember { UUID.randomUUID() }
    var invalid by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<RecordedPayment?>(null) }
    val scope = rememberCoroutineScope()
    val editable = !busy && pending == null
    val dirty = date != initialDate || amount != raw(target?.amount) || principal != raw(target?.allocation?.principal) ||
        interest != raw(target?.allocation?.interest) || fees != raw(target?.allocation?.fees) || notes != target?.notes.orEmpty() || reason.isNotBlank() || pending != null
    fun requestClose() { if (!busy) { if (dirty) discard = true else close() } }
    AlertDialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = false),
        title = { Text(stringResource(if (target == null) R.string.payment_add else R.string.ux_correct)) },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.payment_fact_hint))
                Text(obligation.title + " · " + obligation.currency.currencyCode, style = MaterialTheme.typography.titleMedium)
                PaymentInput(amount, { amount = it.take(32) }, R.string.payment_amount, "payment-amount", editable, true)
                PaymentInput(date, { date = it.take(10) }, R.string.payment_date, "payment-date", editable)
                Text(stringResource(R.string.payment_date_hint), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("payment-advanced")) {
                    Text(stringResource(if (advanced) R.string.form_hide_additional else R.string.ux_optional_details))
                }
                if (advanced) {
                Text(stringResource(R.string.payment_allocation_hint), style = MaterialTheme.typography.bodySmall)
                PaymentInput(principal, { principal = it.take(32) }, R.string.payment_principal, "payment-principal", editable, true)
                PaymentInput(interest, { interest = it.take(32) }, R.string.payment_interest, "payment-interest", editable, true)
                PaymentInput(fees, { fees = it.take(32) }, R.string.payment_fees, "payment-fees", editable, true)
                PaymentInput(notes, { notes = it.take(500) }, R.string.field_notes, "payment-notes", editable)
                }
                if (target != null) PaymentInput(reason, { reason = it.take(500) }, R.string.ux_reason, "correction-reason", editable)
                if (invalid) Text(stringResource(if (target != null && reason.isBlank()) R.string.ux_reason else R.string.payment_invalid), color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("payment-invalid"))
                if (failed) Text(stringResource(R.string.payment_retry_hint), color = MaterialTheme.colorScheme.error)
                if (busy) CircularProgressIndicator(Modifier.size(24.dp))
            }
        },
        confirmButton = {
            val focus = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            TextButton(enabled = !busy, modifier = Modifier.testTag("save-payment"), onClick = {
                val value = pending ?: runCatching {
                    buildPayment(obligation, UUID.randomUUID(), UUID.randomUUID(), date, amount,
                        principal, interest, fees, notes, LocalDate.now(obligation.bankZone))
                }.getOrNull()
                if (value == null || (target != null && reason.isBlank())) {
                    invalid = true
                } else {
                    focus.clearFocus(force = true)
                    keyboard?.hide()
                    invalid = false
                    pending = value
                    failed = false
                    busy = true
                    scope.launch {
                        try {
                            if (target == null) repository.record(value) else repository.correct(PaymentCorrection(correctionOperation, obligation.id, target.id, reason.trim(), value))
                            onSaved()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            failed = true
                        } finally {
                            busy = false
                        }
                    }
                }
            }) { Text(stringResource(R.string.ux_save_record)) }
        },
        dismissButton = {
            val focus = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            TextButton(enabled = !busy, onClick = {
                focus.clearFocus(force = true)
                keyboard?.hide()
                requestClose()
            }) { Text(stringResource(R.string.cancel)) }
        }
    )
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(stringResource(R.string.ux_discard_title)) },
        text = { Text(stringResource(if (pending != null) R.string.payment_retry_hint else R.string.ux_discard_hint)) },
        confirmButton = { TextButton(onClick = { discard = false; close() }) { Text(stringResource(R.string.ux_discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.ux_keep_editing)) } })
}

@Composable
private fun PaymentCancelDialog(obligation: Obligation, repository: PaymentRepository, target: RecordedPayment, close: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<PaymentCorrection?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun requestClose() { if (!busy) { if (reason.isNotBlank() || pending != null) discard = true else close() } }
    AlertDialog(onDismissRequest = { requestClose() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.ux_cancel_payment)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(paymentMoney(target.amount) + " · " + target.bankDate)
            Text(stringResource(R.string.ux_cancel_hint))
            PaymentInput(reason, { reason = it.take(500) }, R.string.ux_reason, "cancel-reason", !busy && pending == null)
            if (failed) Text(stringResource(R.string.payment_retry_hint), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(enabled = !busy && reason.isNotBlank(), modifier = Modifier.testTag("confirm-cancel-payment"), onClick = {
            val value = pending ?: PaymentCorrection(UUID.randomUUID(), obligation.id, target.id, reason.trim())
            pending = value; busy = true; failed = false
            scope.launch {
                try { repository.correct(value); close() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { failed = true }
                finally { busy = false }
            }
        }) { Text(stringResource(R.string.ux_cancel_payment)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { requestClose() }) { Text(stringResource(R.string.back)) } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(stringResource(R.string.ux_discard_title)) },
        text = { Text(stringResource(if (pending != null) R.string.payment_retry_hint else R.string.ux_discard_hint)) },
        confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.ux_discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.ux_keep_editing)) } })
}

@Composable
private fun PaymentInput(value: String, update: (String) -> Unit, label: Int, tag: String,
    enabled: Boolean, decimal: Boolean = false) {
    OutlinedTextField(value, update, modifier = Modifier.fillMaxWidth().testTag(tag),
        label = { Text(stringResource(label)) }, enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text))
}
