package com.psy.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val icon: String,
    val color: Long, // ARGB packed
    val isFund: Boolean = false, // fund (quỹ): excluded from income/expense stats & budget
)
