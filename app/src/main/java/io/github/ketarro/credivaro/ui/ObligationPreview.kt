package io.github.ketarro.credivaro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.ketarro.credivaro.R
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

@Composable
fun PreviewDashboard(records: List<Obligation>, open: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.known_test_balances), style = MaterialTheme.typography.titleMedium)
            if (records.isEmpty()) Text(stringResource(R.string.empty_obligations))
            records.mapNotNull { it.balance?.totalDebt }.groupBy { it.currency }.toSortedMap().forEach { (code, values) ->
                val total = runCatching { values.fold(Money(code, 0)) { a, b -> a + b } }.getOrNull()
                Text(if (total == null) stringResource(R.string.preview_overflow) else total.display(),
                    style = MaterialTheme.typography.headlineSmall)
            }
            if (records.isNotEmpty()) {
                Text(stringResource(R.string.record_count, records.size))
                Text(stringResource(R.string.missing_balances, records.count { it.balance?.totalDebt == null }),
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = open, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.view_obligations)) }
        }
    }
    Text(stringResource(R.string.preview_session), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun ObligationPreview(records: MutableList<Obligation>) {
    var editor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Obligation?>(null) }
    var detailId by remember { mutableStateOf<UUID?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val detail = records.firstOrNull { it.id == detailId }
    BackHandler(editor || detailId != null) {
        if (editor) editor = false else detailId = null
    }
    Text(stringResource(R.string.preview_session), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    if (editor) {
        PreviewEditor(editing, onCancel = { editor = false }) { value ->
            val index = records.indexOfFirst { it.id == value.id }
            if (index >= 0) records[index] = value else records.add(value)
            detailId = value.id
            editor = false
        }
    } else if (detail != null) {
        TextButton(onClick = { detailId = null }) { Text(stringResource(R.string.back)) }
        Text(detail.title, style = MaterialTheme.typography.headlineSmall)
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(typeLabel(detail.type)))
                detail.lender?.let { Text(it) }
                Text(detail.balance?.totalDebt.display(), style = MaterialTheme.typography.headlineMedium)
                detail.balance?.let { Text(stringResource(R.string.preview_balance_date, it.bankAsOf.toString())) }
                val rate = when (val t = detail.terms) {
                    is ObligationTerms.Loan -> t.annualRate
                    is ObligationTerms.CreditCard -> t.annualRate
                    is ObligationTerms.Installment -> t.annualRate
                    is ObligationTerms.Other -> null
                }
                Text(stringResource(R.string.field_rate) + ": " + (rate?.percent?.toPlainString() ?: stringResource(R.string.not_set)))
                when (val t = detail.terms) {
                    is ObligationTerms.Loan -> Text(stringResource(R.string.field_payment) + ": " + t.scheduledPayment.display())
                    is ObligationTerms.CreditCard -> {
                        Text(stringResource(R.string.field_limit) + ": " + t.creditLimit.display())
                        Text(stringResource(R.string.field_minimum) + ": " + t.minimumPayment.display())
                    }
                    is ObligationTerms.Installment -> Text(stringResource(R.string.field_payment) + ": " + t.installmentAmount.display())
                    is ObligationTerms.Other -> Unit
                }
            }
        }
        Button(onClick = { editing = detail; editor = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.edit_obligation)) }
        TextButton(onClick = { deleting = true }) { Text(stringResource(R.string.delete_obligation), color = MaterialTheme.colorScheme.error) }
        if (deleting) AlertDialog(onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.delete_obligation)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = { TextButton(onClick = { records.removeAll { it.id == detail.id }; detailId = null; deleting = false }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } })
    } else {
        Text(stringResource(R.string.obligations_title), style = MaterialTheme.typography.headlineSmall)
        if (records.isEmpty()) Text(stringResource(R.string.empty_obligations))
        records.forEach { item ->
            Card(onClick = { detailId = item.id }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(typeLabel(item.type)), style = MaterialTheme.typography.bodySmall)
                    Text(item.balance?.totalDebt.display(), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        Button(onClick = { editing = null; editor = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_obligation)) }
    }
}

@Composable
private fun PreviewEditor(existing: Obligation?, onCancel: () -> Unit, onSave: (Obligation) -> Unit) {
    var type by remember { mutableStateOf(existing?.type ?: ObligationType.LOAN) }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var lender by remember { mutableStateOf(existing?.lender.orEmpty()) }
    var currency by remember { mutableStateOf(existing?.currency?.currencyCode ?: "RUB") }
    var balance by remember { mutableStateOf(existing?.balance?.totalDebt?.raw().orEmpty()) }
    val oldTerms = existing?.terms
    var rate by remember { mutableStateOf(when (oldTerms) {
        is ObligationTerms.Loan -> oldTerms.annualRate
        is ObligationTerms.CreditCard -> oldTerms.annualRate
        is ObligationTerms.Installment -> oldTerms.annualRate
        else -> null
    }?.percent?.toPlainString().orEmpty()) }
    var payment by remember { mutableStateOf(when (oldTerms) {
        is ObligationTerms.Loan -> oldTerms.scheduledPayment
        is ObligationTerms.CreditCard -> oldTerms.minimumPayment
        is ObligationTerms.Installment -> oldTerms.installmentAmount
        else -> null
    }?.raw().orEmpty()) }
    var limit by remember { mutableStateOf((oldTerms as? ObligationTerms.CreditCard)?.creditLimit?.raw().orEmpty()) }
    var error by remember { mutableStateOf(false) }
    Text(stringResource(if (existing == null) R.string.add_obligation else R.string.edit_obligation), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(R.string.field_type), style = MaterialTheme.typography.titleMedium)
    Column(Modifier.selectableGroup()) {
        ObligationType.entries.forEach { choice ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = type == choice,
                enabled = existing == null, role = Role.RadioButton, onClick = { if (type != choice) { type = choice; rate = ""; payment = ""; limit = "" } }),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = type == choice, onClick = null, enabled = existing == null)
                Text(stringResource(typeLabel(choice)))
            }
        }
    }
    OutlinedTextField(title, { title = it.take(120) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_title)) }, singleLine = true)
    OutlinedTextField(lender, { lender = it.take(120) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_lender)) }, singleLine = true)
    Text(stringResource(R.string.field_currency))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        currencies.forEach { code ->
            FilterChip(selected = currency == code, onClick = { if (currency != code) { currency = code; balance = ""; payment = ""; limit = "" } },
                label = { Text(code) }, enabled = existing == null)
        }
    }
    AmountField(balance, { balance = it }, R.string.field_balance)
    if (type != ObligationType.OTHER) {
        AmountField(rate, { rate = it }, R.string.field_rate)
        AmountField(payment, { payment = it }, if (type == ObligationType.CREDIT_CARD) R.string.field_minimum else R.string.field_payment)
    }
    if (type == ObligationType.CREDIT_CARD) AmountField(limit, { limit = it }, R.string.field_limit)
    Text(stringResource(R.string.field_amount_hint), style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.preview_bank_zone), style = MaterialTheme.typography.bodySmall)
    if (error) Text(stringResource(R.string.invalid_fields), color = MaterialTheme.colorScheme.error)
    Button(modifier = Modifier.fillMaxWidth(), onClick = {
        val result = runCatching {
            val c = Currency.getInstance(currency)
            val zone = existing?.bankZone ?: ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val amount = parsePreviewMoney(balance, c)
            val p = parsePreviewMoney(payment, c)
            val annual = parsePreviewRate(rate)?.let { AnnualRate(it) }
            val terms = when (type) {
                ObligationType.LOAN -> ObligationTerms.Loan(annualRate = annual, scheduledPayment = p)
                ObligationType.CREDIT_CARD -> ObligationTerms.CreditCard(creditLimit = parsePreviewMoney(limit, c), annualRate = annual, minimumPayment = p)
                ObligationType.INSTALLMENT -> ObligationTerms.Installment(installmentAmount = p, annualRate = annual)
                ObligationType.OTHER -> ObligationTerms.Other()
            }
            val value = Obligation(existing?.id ?: UUID.randomUUID(), title.trim(), lender.trim().takeIf { it.isNotEmpty() }, c, zone,
                terms = terms, balance = amount?.let { if (it == existing?.balance?.totalDebt) existing?.balance else BalanceSnapshot(today, totalDebt = it) })
            require(ObligationValidator.validate(value, today).isEmpty())
            value
        }.getOrNull()
        if (result == null) error = true else onSave(result)
    }) { Text(stringResource(R.string.save)) }
    TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
}

@Composable
private fun AmountField(value: String, update: (String) -> Unit, label: Int) {
    OutlinedTextField(value, { update(it.take(32)) }, Modifier.fillMaxWidth(), label = { Text(stringResource(label)) },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}
