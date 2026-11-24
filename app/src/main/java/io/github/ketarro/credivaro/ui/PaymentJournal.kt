package io.github.ketarro.credivaro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    var rows by remember { mutableStateOf<List<RecordedPayment>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(repository, obligation.id, retry) {
        loadError = false
        rows = null
        try {
            repository.observeFor(obligation.id).collect { rows = it }
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
            Button(onClick = { editing = true }, enabled = enabled && rows != null && !loadError,
                modifier = Modifier.fillMaxWidth().testTag("add-payment")) {
                Text(stringResource(R.string.payment_add))
            }
            when {
                loadError -> {
                    Text(stringResource(R.string.payment_read_error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text(stringResource(R.string.storage_retry)) }
                }
                rows == null -> CircularProgressIndicator(Modifier.size(24.dp))
                rows.orEmpty().isEmpty() -> Text(stringResource(R.string.payment_empty))
                else -> rows.orEmpty().forEach { row ->
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
                    }
                }
            }
        }
    }
    if (editing) PaymentEntryDialog(obligation, repository) { editing = false }
}

@Composable
private fun PaymentEntryDialog(obligation: Obligation, repository: PaymentRepository, close: () -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now(obligation.bankZone).toString()) }
    var amount by remember { mutableStateOf("") }
    var principal by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var fees by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<RecordedPayment?>(null) }
    val scope = rememberCoroutineScope()
    val editable = !busy && pending == null
    AlertDialog(
        onDismissRequest = { if (!busy) close() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.payment_add)) },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.payment_fact_hint))
                Text(obligation.currency.currencyCode, style = MaterialTheme.typography.labelLarge)
                PaymentInput(date, { date = it.take(10) }, R.string.payment_date, "payment-date", editable)
                Text(stringResource(R.string.payment_date_hint), style = MaterialTheme.typography.bodySmall)
                PaymentInput(amount, { amount = it.take(32) }, R.string.payment_amount, "payment-amount", editable, true)
                Text(stringResource(R.string.payment_allocation_hint), style = MaterialTheme.typography.bodySmall)
                PaymentInput(principal, { principal = it.take(32) }, R.string.payment_principal, "payment-principal", editable, true)
                PaymentInput(interest, { interest = it.take(32) }, R.string.payment_interest, "payment-interest", editable, true)
                PaymentInput(fees, { fees = it.take(32) }, R.string.payment_fees, "payment-fees", editable, true)
                PaymentInput(notes, { notes = it.take(500) }, R.string.field_notes, "payment-notes", editable)
                if (invalid) Text(stringResource(R.string.payment_invalid), color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("payment-invalid"))
                if (failed) Text(stringResource(R.string.payment_retry_hint), color = MaterialTheme.colorScheme.error)
                if (busy) CircularProgressIndicator(Modifier.size(24.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, modifier = Modifier.testTag("save-payment"), onClick = {
                val value = pending ?: runCatching {
                    buildPayment(obligation, UUID.randomUUID(), UUID.randomUUID(), date, amount,
                        principal, interest, fees, notes, LocalDate.now(obligation.bankZone))
                }.getOrNull()
                if (value == null) {
                    invalid = true
                } else {
                    invalid = false
                    pending = value
                    failed = false
                    busy = true
                    scope.launch {
                        try {
                            repository.record(value)
                            close()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            failed = true
                        } finally {
                            busy = false
                        }
                    }
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = close) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun PaymentInput(value: String, update: (String) -> Unit, label: Int, tag: String,
    enabled: Boolean, decimal: Boolean = false) {
    OutlinedTextField(value, update, modifier = Modifier.fillMaxWidth().testTag(tag),
        label = { Text(stringResource(label)) }, enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text))
}
