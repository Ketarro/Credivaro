package io.github.ketarro.credivaro.model

import java.util.UUID

/** Cancels a recorded fact, optionally replacing it with another fact in one transaction. */
data class PaymentCorrection(
    val operationId: UUID,
    val obligationId: UUID,
    val targetPaymentId: UUID,
    val reason: String,
    val replacement: RecordedPayment? = null
) {
    init {
        require(reason.isNotBlank() && reason.length <= 500)
        replacement?.let {
            require(it.obligationId == obligationId)
            require(it.id != targetPaymentId)
            require(it.operationId != operationId)
        }
    }
}

enum class PaymentHistoryStatus { ACTIVE, CANCELLED, REPLACED }

data class PaymentHistoryItem(
    val payment: RecordedPayment,
    val status: PaymentHistoryStatus,
    val correctionReason: String? = null,
    val replacementId: UUID? = null
)
