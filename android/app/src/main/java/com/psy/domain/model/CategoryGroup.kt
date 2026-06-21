package com.psy.domain.model

data class CategoryGroup(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long, // ARGB packed
    val type: CategoryType,
    val sortOrder: Int,
)
