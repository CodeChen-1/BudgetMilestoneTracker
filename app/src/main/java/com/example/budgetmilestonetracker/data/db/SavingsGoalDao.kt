package com.example.budgetmilestonetracker.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SavingsGoalDao {
    @Insert
    fun insert(goal: SavingsGoal): Long

    @Update
    fun update(goal: SavingsGoal)

    @Delete
    fun delete(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals ORDER BY deadline ASC")
    fun getAllGoals(): LiveData<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals")
    fun getAllGoalsList(): List<SavingsGoal>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getGoalById(id: Long): SavingsGoal?

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getGoalByIdLiveData(id: Long): LiveData<SavingsGoal?>

    @Query("""
        SELECT sg.id AS goal_id, sg.label, sg.targetAmount, sg.deadline, sg.isAchieved,
               COALESCE(SUM(gc.amount), 0) AS totalSaved
        FROM savings_goals sg
        LEFT JOIN goal_contributions gc ON sg.id = gc.goalId
        GROUP BY sg.id
        ORDER BY sg.deadline ASC
    """)
    fun getGoalsWithTotalLive(): LiveData<List<SavingsGoalWithTotal>>

    @Query("SELECT * FROM savings_goals WHERE roundUpEnabled = 1")
    fun getRoundUpEnabledGoals(): List<SavingsGoal>

    @Query("UPDATE savings_goals SET roundUpEnabled = 0")
    fun disableAllRoundUp()

    @Query("DELETE FROM savings_goals")
    fun deleteAll()
}