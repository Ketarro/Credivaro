package io.github.ketarro.credivaro.database

import io.github.ketarro.credivaro.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** Versioned payload. Minor units are decimal integer strings inside JSON, never floating point. */
internal object TermsCodec {
    fun encode(value: ObligationTerms): String = JSONObject().apply {
        put("v", 1)
        fun money(k: String, m: Money?) { if (m != null) put(k, m.minorUnits.toString()) }
        fun date(k: String, d: LocalDate?) { if (d != null) put(k, d.toString()) }
        fun rate(r: AnnualRate?) { if (r != null) put("rate", r.percent.toPlainString()) }
        fun day(k: String, d: MonthlyDay?) {
            if (d != null) put(k, JSONObject().put("day", d.day).put("policy", d.missingDayPolicy.name))
        }
        when (value) {
            is ObligationTerms.Loan -> {
                money("originalPrincipal", value.originalPrincipal); rate(value.annualRate)
                put("termMonths", value.termMonths); put("scheme", value.paymentScheme?.name)
                money("payment", value.scheduledPayment); date("due", value.firstDueDate)
                day("paymentDay", value.paymentDay); put("frequency", value.paymentFrequency?.name)
            }
            is ObligationTerms.CreditCard -> {
                money("limit", value.creditLimit); rate(value.annualRate)
                day("statementDay", value.statementDay); money("minimum", value.minimumPayment)
                date("due", value.minimumPaymentDueDate); money("cashLimit", value.cashFeeFreeLimit)
                day("cashResetDay", value.cashResetDay)
                put("grace", JSONArray().apply {
                    value.graceGroups.forEach { g ->
                        put(JSONObject().put("id", g.id.toString()).put("category", g.category)
                            .put("statement", g.statementDate?.toString()).put("deadline", g.deadline?.toString())
                            .put("amount", g.amountToPreserveGrace?.minorUnits?.toString()))
                    }
                })
            }
            is ObligationTerms.Installment -> {
                money("original", value.originalAmount); money("down", value.downPayment)
                put("count", value.installmentCount); money("payment", value.installmentAmount)
                date("due", value.firstDueDate); rate(value.annualRate); money("fees", value.fees)
                put("frequency", value.paymentFrequency?.name); day("paymentDay", value.paymentDay)
            }
            is ObligationTerms.Other -> put("description", value.description)
        }
    }.toString()

    fun decode(type: String, payload: String, currency: String): ObligationTerms {
        val j = JSONObject(payload)
        require(j.getInt("v") == 1) { "Unsupported terms payload" }
        fun JSONObject.string(k: String): String? = if (isNull(k)) null else getString(k)
        fun JSONObject.money(k: String): Money? = string(k)?.let { Money(currency, it.toLong()) }
        fun JSONObject.date(k: String): LocalDate? = string(k)?.let(LocalDate::parse)
        fun integer(k: String): Int? = if (j.isNull(k)) null else j.getInt(k)
        fun rate(): AnnualRate? = j.string("rate")?.let { AnnualRate(BigDecimal(it)) }
        fun day(k: String): MonthlyDay? = if (j.isNull(k)) null else j.getJSONObject(k).let {
            MonthlyDay(it.getInt("day"), MissingDayPolicy.valueOf(it.getString("policy")))
        }
        return when (ObligationType.valueOf(type)) {
            ObligationType.LOAN -> ObligationTerms.Loan(
                j.money("originalPrincipal"), rate(), integer("termMonths"),
                j.string("scheme")?.let(PaymentScheme::valueOf), j.money("payment"), j.date("due"), day("paymentDay"), j.string("frequency")?.let(PaymentFrequency::valueOf))
            ObligationType.CREDIT_CARD -> ObligationTerms.CreditCard(
                j.money("limit"), rate(), day("statementDay"), j.money("minimum"), j.date("due"),
                j.money("cashLimit"), day("cashResetDay"), j.getJSONArray("grace").let { array ->
                    List(array.length()) { index -> array.getJSONObject(index).let { g ->
                        GraceGroup(UUID.fromString(g.getString("id")), g.getString("category"),
                            g.date("statement"), g.date("deadline"), g.money("amount"))
                    } }
                })
            ObligationType.INSTALLMENT -> ObligationTerms.Installment(
                j.money("original"), j.money("down"), integer("count"), j.money("payment"),
                j.date("due"), rate(), j.money("fees"), j.string("frequency")?.let(PaymentFrequency::valueOf), day("paymentDay"))
            ObligationType.OTHER -> ObligationTerms.Other(j.string("description"))
        }
    }
}
