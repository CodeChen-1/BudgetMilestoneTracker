package com.example.budgetmilestonetracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_contributions",
    foreignKeys = [ForeignKey(
        entity = SavingsGoal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("goalId")]
)
data class GoalContribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goalId: Long,
    val amount: Double,
    val timestamp: Long,
    val note: String? = null
)