package com.psy.domain.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/** Formatting helpers for amounts stored as minor units (e.g. cents, đồng). */
object Money {

    /**
     * Renders a minor-unit amount as a grouped decimal string with a currency suffix.
     * Always shows exactly [fractionDigits] decimal places. Uses integer arithmetic
     * throughout so precision is never lost (amounts are Long minor units).
     *
     * @param amountMinor amount in minor units (e.g. 1234 with fractionDigits=2 == 12.34)
     * @param fractionDigits decimal places for the currency (0 for VND, 2 for USD)
     * @param suffix currency symbol appended after a space (e.g. "đ", "$")
     */
    fun formatMinor(amountMinor: Long, fractionDigits: Int, suffix: String): String {
        var divisor = 1L
        repeat(fractionDigits) { divisor *= 10L }

        val absAmount = abs(amountMinor)
        val whole = absAmount / divisor
        val frac = absAmount % divisor

        val symbols = DecimalFormatSymbols(Locale.US) // ',' grouping
        val groupedWhole = DecimalFormat("#,##0", symbols).format(whole)
        val sign = if (amountMinor < 0) "-" else ""

        return if (fractionDigits > 0) {
            val fracStr = frac.toString().padStart(fractionDigits, '0')
            "$sign$groupedWhole.$fracStr $suffix"
        } else {
            "$sign$groupedWhole $suffix"
        }
    }
}
