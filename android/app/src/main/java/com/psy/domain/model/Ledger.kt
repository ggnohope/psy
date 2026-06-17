package com.psy.domain.model

data class Ledger(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val currency: String,
    val createdAt: Long,
)
