package com.psy.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "budgets", indices = [Index("ledgerId")])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val groupId: Long?,   // null = total budget; otherwise references a CategoryGroup
    val amountMinor: Long,
)
