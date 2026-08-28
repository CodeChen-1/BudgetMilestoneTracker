package com.example.budgetmilestonetracker.data.db

data class Nudge(
    val categoryName: String,
    val suggestedAmount: Double,
    val goalLabel: String,
    val goalId: Long
)