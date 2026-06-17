package com.psy.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index("ledgerId"), Index("date"), Index("categoryId"), Index("accountId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val type: String,   // TxType.name
    val amountMinor: Long,
    val categoryId: Long,
    val accountId: Long,
    val note: String,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
