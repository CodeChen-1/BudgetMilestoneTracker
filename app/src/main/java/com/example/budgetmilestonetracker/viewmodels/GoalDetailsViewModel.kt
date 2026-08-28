package com.example.budgetmilestonetracker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.budgetmilestonetracker.BudgetApplication
import com.example.budgetmilestonetracker.data.db.GoalContribution
import com.example.budgetmilestonetracker.data.db.SavingsGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoalDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as BudgetApplication).repository
    private val _goalId = MutableLiveData<Long>()
    val goal: LiveData<SavingsGoal?> = _goalId.switchMap { id -> repo.getGoalByIdLiveData(id) }
    val contributions: LiveData<List<GoalContribution>> = _goalId.switchMap { id ->
        repo.getContributionsForGoal(id)
    }

    // One‑shot event for when a goal is just achieved
    private val _goalAchieved = MutableLiveData<Boolean>()
    val goalAchieved: LiveData<Boolean> = _goalAchieved

    fun setGoalId(id: Long) { _goalId.value = id }

    fun addContribution(amount: Double, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val goalId = _goalId.value ?: return@launch
            repo.insertContribution(
                GoalContribution(goalId = goalId, amount = amount,
                    timestamp = System.currentTimeMillis(), note = note)
            )
            // Check if the goal is now achieved
            val wasAchieved = repo.getGoalById(goalId)?.isAchieved ?: false
            repo.updateGoalAchievedStatus(goalId)
            val nowAchieved = repo.getGoalById(goalId)?.isAchieved ?: false
            if (!wasAchieved && nowAchieved) {
                _goalAchieved.postValue(true)   // fire celebration event
            }
        }
    }

    fun deleteContribution(contribution: GoalContribution) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteContribution(contribution)
            repo.updateGoalAchievedStatus(contribution.goalId)
        }
    }

    // Call this after showing the celebration toast so it doesn’t fire again
    fun resetGoalAchieved() {
        _goalAchieved.value = false
    }
}