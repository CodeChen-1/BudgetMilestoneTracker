package com.example.budgetmilestonetracker.data.db

data class SavingsGoalWithForecast(
    val goal: SavingsGoal,
    val totalSaved: Double,
    val forecastLabel: String
)