package com.example.budgetmilestonetracker.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GoalContributionDao {
    @Insert
    fun insert(contribution: GoalContribution): Long

    @Delete
    fun delete(contribution: GoalContribution)

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getContributionsForGoal(goalId: Long): LiveData<List<GoalContribution>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE goalId = :goalId")
    fun getTotalSavedForGoal(goalId: Long): Double

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getContributionsForGoalList(goalId: Long): List<GoalContribution>

    @Query("DELETE FROM goal_contributions")
    fun deleteAll()
}