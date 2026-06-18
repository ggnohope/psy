package com.psy.domain.model

data class Transaction(
    val id: Long = 0,
    val ledgerId: Long,
    val type: TxType,
    val amountMinor: Long,
    val categoryId: Long? = null,
    val accountId: Long,
    val toAccountId: Long? = null,
    val note: String,
    val date: Long, // epoch millis of the day the bill occurred
    val createdAt: Long,
    val updatedAt: Long,
    val photoUri: String? = null,
)
