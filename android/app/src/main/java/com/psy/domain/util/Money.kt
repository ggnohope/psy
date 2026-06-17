package com.psy.domain.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/** Formatting helpers for amounts stored as minor units (e.g. cents, đồng). */
object Money {

    /**
     * Renders a minor-unit amount as a grouped decimal string with a currency suffix.
     * Always shows exactly [fractionDigits] decimal places.
     *
     * @param amountMinor amount in minor units (e.g. 1234 with fractionDigits=2 == 12.34)
     * @param fractionDigits decimal places for the currency (0 for VND, 2 for USD)
     * @param suffix currency symbol appended after a space (e.g. "đ", "$")
     */
    fun formatMinor(amountMinor: Long, fractionDigits: Int, suffix: String): String {
        val divisor = Math.pow(10.0, fractionDigits.toDouble())
        val value = abs(amountMinor) / divisor

        val symbols = DecimalFormatSymbols(Locale.US) // ',' grouping, '.' decimal
        val pattern = buildString {
            append("#,##0")
            if (fractionDigits > 0) {
                append('.')
                repeat(fractionDigits) { append('0') }
            }
        }
        val formatted = DecimalFormat(pattern, symbols).format(value)
        val sign = if (amountMinor < 0) "-" else ""
        return "$sign$formatted $suffix"
    }
}
