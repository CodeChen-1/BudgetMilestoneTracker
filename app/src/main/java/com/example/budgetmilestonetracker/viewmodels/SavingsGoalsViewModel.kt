package com.example.budgetmilestonetracker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetmilestonetracker.BudgetApplication
import com.example.budgetmilestonetracker.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavingsGoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as BudgetApplication).repository

    // Goals with forecast for the adapter
    private val _goalsWithForecast = MutableLiveData<List<SavingsGoalWithForecast>>()
    val goalsWithForecast: LiveData<List<SavingsGoalWithForecast>> = _goalsWithForecast

    init {
        loadGoals()
    }

    fun loadGoals() {
        viewModelScope.launch(Dispatchers.IO) {
            _goalsWithForecast.postValue(repo.getGoalsWithForecast())
        }
    }

    // Add goal – supports round‑up flag
    fun addGoal(label: String, target: Double, deadline: Long, roundUp: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val goalId = repo.insertGoal(SavingsGoal(label = label, targetAmount = target, deadline = deadline))
            if (roundUp) {
                repo.setRoundUpGoal(goalId)
            }
            loadGoals()  // refresh list
        }
    }

    // Update goal – re‑applies round‑up exclusivity if needed
    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateGoal(goal)
            if (goal.roundUpEnabled) {
                repo.setRoundUpGoal(goal.id)
            }
            loadGoals()
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteGoal(goal)
            loadGoals()
        }
    }
}