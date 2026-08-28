package com.example.budgetmilestonetracker.data.db

data class SavingsGoalWithTotal(
    val goal_id: Long,
    val label: String,
    val targetAmount: Double,
    val deadline: Long,
    val isAchieved: Boolean,
    val totalSaved: Double
)