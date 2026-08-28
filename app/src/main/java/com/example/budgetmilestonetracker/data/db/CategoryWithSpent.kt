package com.example.budgetmilestonetracker.data.db

data class CategoryWithSpent(
    val categoryId: Long,
    val name: String,
    val monthlyLimit: Double,
    val iconResName: String,
    val totalSpent: Double
)