package io.github.ketarro.credivaro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.semantics.heading

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.ketarro.credivaro.R
import io.github.ketarro.credivaro.database.PaymentRepository
import io.github.ketarro.credivaro.model.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import java.util.UUID

private val currencies = listOf("RUB", "USD", "EUR", "GBP")
private fun typeLabel(type: ObligationType): Int = when (type) {
    ObligationType.LOAN -> R.string.loan_type
    ObligationType.CREDIT_CARD -> R.string.card_type
    ObligationType.INSTALLMENT -> R.string.installment_type
    ObligationType.OTHER -> R.string.other_type
}
private fun Money.raw(): String = BigDecimal.valueOf(minorUnits, Currency.getInstance(currency).defaultFractionDigits).toPlainString()
@Composable private fun Money?.display(): String {
    if (this == null) return stringResource(R.string.not_set)
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ENGLISH
    return NumberFormat.getCurrencyInstance(locale).apply { currency = Currency.getInstance(this@display.currency) }
        .format(BigDecimal(raw()))
}

private fun typeIcon(type: ObligationType): Int = when (type) {
    ObligationType.LOAN -> R.drawable.ic_loan
    ObligationType.CREDIT_CARD -> R.drawable.ic_credit_card
    ObligationType.INSTALLMENT -> R.drawable.ic_installment
    ObligationType.OTHER -> R.drawable.ic_wallet
}

private fun Obligation.payment(): Money? = when (val t = terms) {
    is ObligationTerms.Loan -> t.scheduledPayment
    is ObligationTerms.CreditCard -> t.minimumPayment
    is ObligationTerms.Installment -> t.installmentAmount
    is ObligationTerms.Other -> null
}
private fun Obligation.paymentDate(): LocalDate? = when (val t = terms) {
    is ObligationTerms.Loan -> t.firstDueDate
    is ObligationTerms.CreditCard -> t.minimumPaymentDueDate
    is ObligationTerms.Installment -> t.firstDueDate
    is ObligationTerms.Other -> null
}
@Composable
private fun LocalDate.displayDate(): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ENGLISH
    return format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale))
}
@Composable
private fun PreviewNotice() {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(R.drawable.ic_info), null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.preview_session), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PreviewDashboard(records: List<Obligation>, open: () -> Unit, add: () -> Unit,
    openCredit: (UUID) -> Unit, recordPayment: (UUID) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val summary = summarizePreview(records)
    var choosePayment by rememberSaveable { mutableStateOf(false) }
    val active = records.filter { it.status == ObligationStatus.ACTIVE }
    val next = active.filter { item -> item.paymentDate()?.let { !it.isBefore(LocalDate.now(item.bankZone)) } == true }
        .minByOrNull { requireNotNull(it.paymentDate()) }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.known_test_balances), style = MaterialTheme.typography.titleMedium)
            if (records.isEmpty()) Text(stringResource(R.string.empty_obligations))
            if (records.isNotEmpty() && summary.totals.isEmpty()) Text(stringResource(R.string.not_set), style = MaterialTheme.typography.headlineSmall)
            summary.totals.forEach { (code, total) ->
                Text(if (total == null) stringResource(R.string.preview_currency_overflow, code) else total.display(),
                    style = MaterialTheme.typography.headlineSmall)
            }
            if (records.isNotEmpty()) {
                Text(pluralStringResource(R.plurals.obligation_count, records.size, records.size))
                if (summary.unknown > 0) Text(stringResource(R.string.missing_balances, summary.unknown), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Button(onClick = add, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-add")) {
        Text("+ " + stringResource(R.string.add_obligation))
    }
    if (active.isNotEmpty()) OutlinedButton(onClick = {
        if (active.size == 1) recordPayment(active.single().id) else choosePayment = true
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-pay")) {
        Text(stringResource(R.string.payment_add))
    }
    if (next != null) {
        Text(stringResource(R.string.ux_next_known), style = MaterialTheme.typography.titleMedium)
        ObligationListCard(next, { openCredit(next.id) }, { recordPayment(next.id) })
    }
    if (records.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.obligations_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = open) { Text(stringResource(R.string.view_obligations)) }
        }
        records.take(3).forEach { item -> key(item.id) { ObligationListCard(item, { openCredit(item.id) }) } }
    }
    if (choosePayment) AlertDialog(onDismissRequest = { choosePayment = false },
        title = { Text(stringResource(R.string.ux_choose_credit)) },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            active.forEach { item ->
                TextButton(onClick = { choosePayment = false; recordPayment(item.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(item.title + " · " + item.currency.currencyCode)
                }
            }
        } }, confirmButton = {}, dismissButton = {
            TextButton(onClick = { choosePayment = false }) { Text(stringResource(R.string.cancel)) }
        })
    PreviewNotice()
}

@Composable
private fun ObligationListCard(item: Obligation, open: () -> Unit, pay: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Card(onClick = open, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = colors.secondaryContainer) {
                    Icon(painterResource(typeIcon(item.type)), null, tint = colors.onSecondaryContainer, modifier = Modifier.padding(9.dp).size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(typeLabel(item.type)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    item.lender?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                }
                Icon(painterResource(R.drawable.ic_chevron), null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Text(stringResource(R.string.ux_bank_balance), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(item.balance?.totalDebt.display(), style = MaterialTheme.typography.titleLarge)
            val payment = item.payment()
            val date = item.paymentDate()
            if (payment != null || date != null) {
                HorizontalDivider(color = colors.outlineVariant)
                Text(stringResource(if (item.type == ObligationType.CREDIT_CARD) R.string.form_minimum else R.string.form_payment),
                    style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                if (payment != null) Text(payment.display(), style = MaterialTheme.typography.bodyMedium)
                if (date != null) Text(date.displayDate(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (pay != null) OutlinedButton(onClick = pay, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.payment_add))
            }
        }
    }
}

@Composable
fun ObligationPreview(
    records: List<Obligation>, saveRecord: suspend (Obligation) -> Unit,
    deleteRecord: suspend (UUID) -> Unit, payments: PaymentRepository,
    route: String, onRoute: (String) -> Unit, paymentRequested: Boolean,
    onPaymentRequested: (Boolean) -> Unit, scroll: ScrollState
) {
    val editor = route == "new" || route.startsWith("edit:")
    val detailId = route.substringAfter(':', "").let { runCatching { UUID.fromString(it) }.getOrNull() }
    val detail = records.firstOrNull { it.id == detailId }
    var deleting by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var writeFailed by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var currencyFilter by rememberSaveable { mutableStateOf("ALL") }
    var tab by rememberSaveable(detailId?.toString()) { mutableStateOf("overview") }
    val scope = rememberCoroutineScope()
    fun write(operation: suspend () -> Unit, onSuccess: () -> Unit) {
        if (busy) return
        busy = true; writeFailed = false
        scope.launch {
            try { operation(); onSuccess() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { writeFailed = true }
            finally { busy = false }
        }
    }
    fun backToList() { writeFailed = false; menu = false; onPaymentRequested(false); onRoute("list") }
    BackHandler(busy || (!editor && route != "list")) { if (!busy) backToList() }
    if (busy) AlertDialog(onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        text = { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp)); Text(stringResource(R.string.storage_working))
        } }, confirmButton = {})
    if (editor && (route == "new" || detail != null)) {
        key(route) {
            PreviewEditor(detail, busy = busy, writeFailed = writeFailed,
                onCancel = { if (!busy) { writeFailed = false; onRoute(if (detailId == null) "list" else "detail:" + detailId) } }) { value ->
                write({ saveRecord(value) }) { tab = "overview"; onRoute("detail:" + value.id) }
            }
        }
    } else if (detail != null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { backToList() }, enabled = !busy) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
            }
            Text(detail.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { menu = true }, enabled = !busy, modifier = Modifier.testTag("credit-actions")) {
                    Text(stringResource(R.string.ux_actions))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit_obligation)) },
                        onClick = { menu = false; writeFailed = false; onRoute("edit:" + detail.id) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete_obligation), color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; deleting = true; writeFailed = false })
                }
            }
        }
        Text(stringResource(typeLabel(detail.type)) + (detail.lender?.let { " · " + it } ?: ""),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.ux_bank_balance), style = MaterialTheme.typography.labelLarge)
                Text(detail.balance?.totalDebt.display(), style = MaterialTheme.typography.headlineMedium)
                detail.balance?.let { Text(stringResource(R.string.preview_balance_date, it.bankAsOf.displayDate())) }
                detail.payment()?.let { Text(stringResource(if (detail.type == ObligationType.CREDIT_CARD) R.string.form_minimum else R.string.form_payment) + ": " + it.display()) }
                detail.paymentDate()?.let { Text(stringResource(R.string.field_due_date) + ": " + it.displayDate()) }
            }
        }
        if (detail.status == ObligationStatus.ACTIVE) Button(onClick = { onPaymentRequested(true) }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add-payment")) {
            Text(stringResource(R.string.payment_add))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("overview" to R.string.ux_overview, "payments" to R.string.ux_payments, "terms" to R.string.ux_terms).forEach { (id, label) ->
                FilterChip(selected = tab == id, onClick = { tab = id; scope.launch { scroll.scrollTo(0) } },
                    label = { Text(stringResource(label)) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("credit-tab-" + id))
            }
        }
        when (tab) {
            "payments" -> key(detail.id) { PaymentJournal(detail, payments, enabled = !busy) }
            "terms" -> Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rate = when (val t = detail.terms) {
                        is ObligationTerms.Loan -> t.annualRate
                        is ObligationTerms.CreditCard -> t.annualRate
                        is ObligationTerms.Installment -> t.annualRate
                        is ObligationTerms.Other -> null
                    }
                    Text(stringResource(R.string.form_rate) + ": " + (rate?.percent?.toPlainString() ?: stringResource(R.string.not_set)))
                    when (val t = detail.terms) {
                        is ObligationTerms.CreditCard -> {
                            Text(stringResource(R.string.form_limit) + ": " + t.creditLimit.display())
                            t.cashFeeFreeLimit?.let { Text(stringResource(R.string.field_cash_limit) + ": " + it.display()) }
                        }
                        else -> Unit
                    }
                    Text(stringResource(R.string.field_currency) + ": " + detail.currency.currencyCode)
                    TextButton(onClick = { onRoute("edit:" + detail.id) }, enabled = !busy) { Text(stringResource(R.string.edit_obligation)) }
                }
            }
            else -> Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.field_currency) + ": " + detail.currency.currencyCode)
                    detail.notes?.let { Text(stringResource(R.string.field_notes), style = MaterialTheme.typography.labelMedium); Text(it) }
                    Text(stringResource(R.string.payment_fact_hint), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { tab = "payments"; scope.launch { scroll.scrollTo(0) } }) { Text(stringResource(R.string.payment_history)) }
                }
            }
        }
        if (paymentRequested) key(detail.id) {
            PaymentEntryDialog(detail, payments, onSaved = {
                onPaymentRequested(false); tab = "payments"; scope.launch { scroll.scrollTo(0) }
            }, close = { onPaymentRequested(false) })
        }
        if (writeFailed) Text(stringResource(R.string.storage_write_error), color = MaterialTheme.colorScheme.error)
        if (deleting) AlertDialog(onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.delete_obligation)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = { TextButton(onClick = {
                deleting = false; write({ deleteRecord(detail.id) }) { backToList() }
            }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } })
    } else if (route != "list") {
        TextButton(onClick = { backToList() }) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.storage_loading))
    } else {
        Text(stringResource(R.string.obligations_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (writeFailed) Text(stringResource(R.string.storage_write_error), color = MaterialTheme.colorScheme.error)
        Button(onClick = { writeFailed = false; onRoute("new") }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("list-add")) {
            Text(stringResource(R.string.add_obligation))
        }
        if (records.isNotEmpty()) {
            OutlinedTextField(search, { search = it }, label = { Text(stringResource(R.string.ux_search)) },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("credit-search"))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(filter == "ALL", { filter = "ALL" }, label = { Text(stringResource(R.string.ux_all_types)) })
                ObligationType.entries.forEach { t -> FilterChip(filter == t.name, { filter = t.name }, label = { Text(stringResource(typeLabel(t))) }) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(currencyFilter == "ALL", { currencyFilter = "ALL" }, label = { Text(stringResource(R.string.ux_all_currencies)) })
                records.map { it.currency.currencyCode }.distinct().sorted().forEach { code ->
                    FilterChip(currencyFilter == code, { currencyFilter = code }, label = { Text(code) })
                }
            }
        }
        val visible = records.filter { (filter == "ALL" || it.type.name == filter) &&
            (currencyFilter == "ALL" || it.currency.currencyCode == currencyFilter) &&
            (search.isBlank() || it.title.contains(search.trim(), ignoreCase = true) || it.lender.orEmpty().contains(search.trim(), ignoreCase = true)) }
        if (visible.isEmpty()) Text(stringResource(if (records.isEmpty()) R.string.empty_obligations else R.string.ux_no_matches))
        visible.forEach { item -> key(item.id) {
            ObligationListCard(item, { onRoute("detail:" + item.id) }, if (item.status == ObligationStatus.ACTIVE) ({
                onRoute("detail:" + item.id); onPaymentRequested(true)
            }) else null)
        } }
        PreviewNotice()
    }
}

@Composable
private fun PreviewEditor(existing: Obligation?, busy: Boolean, writeFailed: Boolean, onCancel: () -> Unit, onSave: (Obligation) -> Unit) {
    var type by rememberSaveable { mutableStateOf(existing?.type ?: ObligationType.LOAN) }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var lender by rememberSaveable { mutableStateOf(existing?.lender.orEmpty()) }
    var currency by rememberSaveable { mutableStateOf(existing?.currency?.currencyCode ?: "RUB") }
    var balance by rememberSaveable { mutableStateOf(existing?.balance?.totalDebt?.raw().orEmpty()) }
    val oldTerms = existing?.terms
    var rate by rememberSaveable { mutableStateOf(when (oldTerms) {
        is ObligationTerms.Loan -> oldTerms.annualRate
        is ObligationTerms.CreditCard -> oldTerms.annualRate
        is ObligationTerms.Installment -> oldTerms.annualRate
        else -> null
    }?.percent?.toPlainString().orEmpty()) }
    var payment by rememberSaveable { mutableStateOf(when (oldTerms) {
        is ObligationTerms.Loan -> oldTerms.scheduledPayment
        is ObligationTerms.CreditCard -> oldTerms.minimumPayment
        is ObligationTerms.Installment -> oldTerms.installmentAmount
        else -> null
    }?.raw().orEmpty()) }
    var limit by rememberSaveable { mutableStateOf((oldTerms as? ObligationTerms.CreditCard)?.creditLimit?.raw().orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var due by rememberSaveable { mutableStateOf(existing?.paymentDate()?.toString().orEmpty()) }
    var cashLimit by rememberSaveable { mutableStateOf((oldTerms as? ObligationTerms.CreditCard)?.cashFeeFreeLimit?.raw().orEmpty()) }
    var expanded by rememberSaveable { mutableStateOf(existing != null) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    fun requestCancel() { if (!busy) { if (dirty) discard = true else onCancel() } }
    BackHandler(true) { requestCancel() }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(stringResource(R.string.ux_discard_title)) },
        text = { Text(stringResource(R.string.ux_discard_hint)) },
        confirmButton = { TextButton(onClick = { discard = false; onCancel() }) { Text(stringResource(R.string.ux_discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.ux_keep_editing)) } })
    var currencyMenu by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { requestCancel() }, enabled = !busy) { Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back)) }
        Text(stringResource(if (existing == null) R.string.add_obligation else R.string.edit_obligation), style = MaterialTheme.typography.headlineSmall)
    }
    PreviewNotice()
    Text(stringResource(R.string.form_required_hint), style = MaterialTheme.typography.bodySmall)
    FormSection(R.string.form_basics) {
        Text(stringResource(R.string.field_type), style = MaterialTheme.typography.labelLarge)
        Column(Modifier.selectableGroup()) {
            ObligationType.entries.forEach { choice ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = type == choice,
                    enabled = existing == null, role = Role.RadioButton, onClick = {
                        if (type != choice) { dirty = true; type = choice; rate = ""; payment = ""; limit = ""; due = ""; cashLimit = "" }
                    }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == choice, onClick = null, enabled = existing == null)
                    Spacer(Modifier.width(12.dp))
                    Icon(painterResource(typeIcon(choice)), null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(typeLabel(choice)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        StaticField(title, { dirty = true; title = it.take(120) }, R.string.field_title, optional = false)
        StaticField(lender, { dirty = true; lender = it.take(120) }, R.string.form_lender)
        Text(stringResource(R.string.field_currency), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { currencyMenu = true }, enabled = existing == null, modifier = Modifier.fillMaxWidth()) {
                Text(currency, Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_chevron), null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                currencies.forEach { code ->
                    DropdownMenuItem(text = { Text(code) }, onClick = {
                        if (currency != code) { dirty = true; currency = code; balance = ""; payment = ""; limit = ""; cashLimit = "" }
                        currencyMenu = false
                    })
                }
            }
        }
    }
    FormSection(R.string.form_finances) {
        AmountField(balance, { dirty = true; balance = it }, R.string.form_balance)
        Text(stringResource(R.string.field_amount_hint), style = MaterialTheme.typography.bodySmall)
    }
    OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(if (expanded) R.string.form_hide_additional else R.string.form_additional), Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_chevron), null, modifier = Modifier.size(18.dp))
    }
    if (expanded) FormSection(R.string.form_additional) {
        if (type != ObligationType.OTHER) {
            AmountField(payment, { dirty = true; payment = it }, if (type == ObligationType.CREDIT_CARD) R.string.form_minimum else R.string.form_payment)
            AmountField(rate, { dirty = true; rate = it }, R.string.form_rate)
        }
        if (type == ObligationType.CREDIT_CARD) AmountField(limit, { dirty = true; limit = it }, R.string.form_limit)
        Text(stringResource(R.string.field_amount_hint), style = MaterialTheme.typography.bodySmall)
            if (type != ObligationType.OTHER) {
                StaticField(due, { dirty = true; due = it.take(10) }, R.string.field_due_date)
                Text(stringResource(R.string.field_date_hint), style = MaterialTheme.typography.bodySmall)
            }
            if (type == ObligationType.CREDIT_CARD) AmountField(cashLimit, { dirty = true; cashLimit = it }, R.string.field_cash_limit)
            StaticField(notes, { dirty = true; notes = it.take(500) }, R.string.field_notes)
            Text(stringResource(R.string.preview_bank_zone), style = MaterialTheme.typography.bodySmall)
    }
    if (error) Text(stringResource(R.string.invalid_fields), color = MaterialTheme.colorScheme.error)
    if (writeFailed) Text(stringResource(R.string.storage_write_error), color = MaterialTheme.colorScheme.error)
    Button(modifier = Modifier.fillMaxWidth().testTag("save-obligation"), enabled = !busy, onClick = {
        val result = runCatching {
            val c = Currency.getInstance(currency)
            val zone = existing?.bankZone ?: ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val amount = parsePreviewMoney(balance, c)
            val p = parsePreviewMoney(payment, c)
            val annual = parsePreviewRate(rate)?.let { AnnualRate(it) }
            val date = parsePreviewDate(due)
            val terms = when (type) {
                ObligationType.LOAN -> ((oldTerms as? ObligationTerms.Loan) ?: ObligationTerms.Loan()).copy(annualRate = annual, scheduledPayment = p, firstDueDate = date)
                ObligationType.CREDIT_CARD -> ((oldTerms as? ObligationTerms.CreditCard) ?: ObligationTerms.CreditCard()).copy(
                    creditLimit = parsePreviewMoney(limit, c), annualRate = annual, minimumPayment = p,
                    minimumPaymentDueDate = date, cashFeeFreeLimit = parsePreviewMoney(cashLimit, c))
                ObligationType.INSTALLMENT -> ((oldTerms as? ObligationTerms.Installment) ?: ObligationTerms.Installment()).copy(installmentAmount = p, annualRate = annual, firstDueDate = date)
                ObligationType.OTHER -> (oldTerms as? ObligationTerms.Other) ?: ObligationTerms.Other()
            }
            val snapshot = if (amount == existing?.balance?.totalDebt) existing?.balance else
                amount?.let { BalanceSnapshot(today, totalDebt = it) }
            val value = (existing ?: Obligation(UUID.randomUUID(), title.trim(), currency = c, bankZone = zone, terms = terms)).copy(
                title = title.trim(), lender = lender.trim().takeIf { it.isNotEmpty() },
                terms = terms, balance = snapshot, notes = notes.trim().takeIf { it.isNotEmpty() })
            require(ObligationValidator.validate(value, today).isEmpty())
            value
        }.getOrNull()
        if (result == null) error = true else { focusManager.clearFocus(); onSave(result) }
    }) { Text(stringResource(R.string.save)) }
    TextButton(enabled = !busy, onClick = { focusManager.clearFocus(); requestCancel() }) { Text(stringResource(R.string.cancel)) }
}

@Composable
private fun AmountField(value: String, update: (String) -> Unit, label: Int) {
    StaticField(value, { update(it.take(32)) }, label, keyboardType = KeyboardType.Decimal)
}

@Composable
private fun FormSection(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StaticField(value: String, update: (String) -> Unit, label: Int,
    optional: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text) {
    val title = stringResource(label)
    val optionalLabel = stringResource(R.string.form_optional)
    val accessibleLabel = if (optional) stringResource(R.string.form_accessible_optional, title, optionalLabel) else title
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val ime = WindowInsets.ime.getBottom(LocalDensity.current)
    val focusManager = LocalFocusManager.current
    LaunchedEffect(focused, ime) { if (focused) requester.bringIntoView() }
    Column(Modifier.fillMaxWidth().bringIntoViewRequester(requester), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(if (optional) title else "$title *", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(value = value, onValueChange = update,
            modifier = Modifier.fillMaxWidth().testTag("field-$label").onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = accessibleLabel },
            singleLine = true, shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { if (!focusManager.moveFocus(FocusDirection.Next)) focusManager.clearFocus() }))
    }
}
