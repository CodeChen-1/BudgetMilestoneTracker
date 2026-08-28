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
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as BudgetApplication).repository

    // Greeting
    private val _greeting = MutableLiveData<String>()
    val greeting: LiveData<String> = _greeting

    // Nudge for overspent categories
    private val _nudge = MutableLiveData<Nudge?>()
    val nudge: LiveData<Nudge?> = _nudge

    // Goals with forecast labels
    private val _goalsWithForecast = MutableLiveData<List<SavingsGoalWithForecast>>()
    val goalsWithForecast: LiveData<List<SavingsGoalWithForecast>> = _goalsWithForecast

    // Categories with spending (no need for mutable – lazy works)
    val categoriesWithSpent: LiveData<List<CategoryWithSpent>> by lazy {
        val (start, end) = getMonthBoundaries()
        repo.getCategoriesWithSpent(start, end)
    }

    // Total spent this month
    val totalSpentThisMonth: LiveData<Double> by lazy {
        val (start, end) = getMonthBoundaries()
        repo.getTotalSpentThisMonth(start, end)
    }

    init {
        // Load user name for greeting
        viewModelScope.launch(Dispatchers.IO) {
            val username = repo.getUser()?.username ?: "User"
            _greeting.postValue(username)
        }
        // Load nudge and forecasts
        refreshExtras()
    }

    fun refreshExtras() {
        viewModelScope.launch(Dispatchers.IO) {
            _nudge.postValue(repo.getCurrentNudge())
            _goalsWithForecast.postValue(repo.getGoalsWithForecast())
        }
    }

    fun acceptNudge(goalId: Long, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.insertContribution(
                GoalContribution(goalId = goalId, amount = amount,
                    timestamp = System.currentTimeMillis(),
                    note = "Nudge: saving from overspending")
            )
            repo.updateGoalAchievedStatus(goalId)
            _nudge.postValue(repo.getCurrentNudge())
        }
    }

    private fun getMonthBoundaries(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.clear(Calendar.MINUTE); cal.clear(Calendar.SECOND); cal.clear(Calendar.MILLISECOND)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }
}