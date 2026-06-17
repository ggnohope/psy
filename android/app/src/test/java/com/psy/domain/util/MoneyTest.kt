package com.psy.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun `formats whole amount with grouping and suffix`() {
        assertEquals("2,450,000 đ", Money.formatMinor(amountMinor = 245_000_000, fractionDigits = 2, suffix = "đ"))
    }

    @Test
    fun `formats amount with fractional digits`() {
        assertEquals("12.34 $", Money.formatMinor(amountMinor = 1234, fractionDigits = 2, suffix = "$"))
    }

    @Test
    fun `formats zero-fraction currency without decimals`() {
        assertEquals("7,000 đ", Money.formatMinor(amountMinor = 7_000, fractionDigits = 0, suffix = "đ"))
    }

    @Test
    fun `formats negative amount with leading minus`() {
        assertEquals("-45.00 $", Money.formatMinor(amountMinor = -4500, fractionDigits = 2, suffix = "$"))
    }
}
