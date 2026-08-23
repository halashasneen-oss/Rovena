package com.rovena.garage.domain.usecase

/**
 * Sums monetary amounts without ever mixing currencies (spec: multi-currency
 * analytics must never sum e.g. 100 JOD + 100 USD = 200). Every call site
 * that would otherwise do `list.sumOf { it.amount }` on a currency-tagged
 * list should go through [aggregate] instead.
 */
object CurrencyAggregator {

    sealed class CurrencyTotal {
        /** No entries to total. */
        data object Empty : CurrencyTotal()

        /** Every entry shared the same currency - a single trustworthy sum. */
        data class Single(val amount: Double, val currencyCode: String) : CurrencyTotal()

        /** Entries spanned more than one currency - never combined, always shown grouped. */
        data class Mixed(val byCurrency: Map<String, Double>) : CurrencyTotal()
    }

    fun aggregate(amounts: List<Pair<Double, String>>): CurrencyTotal {
        if (amounts.isEmpty()) return CurrencyTotal.Empty
        val grouped = amounts.groupBy({ it.second }, { it.first }).mapValues { it.value.sum() }
        val first = grouped.entries.first()
        return if (grouped.size == 1) CurrencyTotal.Single(first.value, first.key) else CurrencyTotal.Mixed(grouped)
    }

    /** True only when every entry shares one currency - the sole condition under which a derived ratio (e.g. cost/km) is meaningful. */
    fun isSingleCurrency(amounts: List<Pair<Double, String>>): Boolean = aggregate(amounts) is CurrencyTotal.Single
}
