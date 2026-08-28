package com.example.budgetmilestonetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val targetAmount: Double,
    val deadline: Long,
    val isAchieved: Boolean = false,
    val roundUpEnabled: Boolean = false
)