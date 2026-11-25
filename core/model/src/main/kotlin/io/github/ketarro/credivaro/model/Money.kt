package io.github.ketarro.credivaro.model

/** Exact minor-unit amounts. Currency scale/metadata is a separate future domain decision. */
data class Money(val currency: String, val minorUnits: Long) {
    init { require(currency.matches(Regex("[A-Z]{3}"))) { "Expected uppercase currency code" } }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return copy(minorUnits = Math.subtractExact(minorUnits, other.minorUnits))
    }
}
