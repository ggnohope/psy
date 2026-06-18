package com.psy.domain.model

data class Budget(val id: Long = 0, val ledgerId: Long, val categoryId: Long?, val amountMinor: Long)
