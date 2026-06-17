package com.psy.domain.model

/** Minimal currency metadata for formatting. v1 supports VND + USD. */
data class Currency(val code: String, val symbol: String, val fractionDigits: Int) {
    companion object {
        val VND = Currency("VND", "đ", 0)
        val USD = Currency("USD", "$", 2)
        fun of(code: String): Currency = when (code) {
            "USD" -> USD
            else -> VND
        }
    }
}
