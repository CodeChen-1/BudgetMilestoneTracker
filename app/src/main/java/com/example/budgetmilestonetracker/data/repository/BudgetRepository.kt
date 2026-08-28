package com.example.budgetmilestonetracker.data.repository

import androidx.lifecycle.LiveData
import com.example.budgetmilestonetracker.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.ceil

class BudgetRepository(private val db: AppDatabase) {

    // ---- Categories ----
    val allCategories: LiveData<List<Category>> = db.categoryDao().getAllCategories()
    suspend fun insertCategory(category: Category) =
        withContext(Dispatchers.IO) { db.categoryDao().insert(category) }
    suspend fun updateCategory(category: Category) =
        withContext(Dispatchers.IO) { db.categoryDao().update(category) }
    suspend fun deleteCategory(category: Category) =
        withContext(Dispatchers.IO) { db.categoryDao().delete(category) }
    fun getCategoriesWithSpent(start: Long, end: Long): LiveData<List<CategoryWithSpent>> =
        db.categoryDao().getCategoriesWithSpent(start, end)
    suspend fun getCategoryById(categoryId: Long): Category? =
        withContext(Dispatchers.IO) { db.categoryDao().getCategoryById(categoryId) }
    suspend fun getCategorySpent(categoryId: Long, start: Long, end: Long): Double =
        withContext(Dispatchers.IO) { db.transactionDao().getCategorySpent(categoryId, start, end) }

    // ---- Transactions ----
    fun getTransactionsByCategory(categoryId: Long): LiveData<List<ExpenseTransaction>> =
        db.transactionDao().getTransactionsByCategory(categoryId)
    suspend fun insertTransaction(transaction: ExpenseTransaction) =
        withContext(Dispatchers.IO) { db.transactionDao().insert(transaction) }
    suspend fun updateTransaction(transaction: ExpenseTransaction) =
        withContext(Dispatchers.IO) { db.transactionDao().update(transaction) }
    suspend fun deleteTransaction(transaction: ExpenseTransaction) =
        withContext(Dispatchers.IO) { db.transactionDao().delete(transaction) }
    suspend fun getTransactionById(id: Long): ExpenseTransaction? =
        withContext(Dispatchers.IO) { db.transactionDao().getTransactionById(id) }
    fun getTotalSpentThisMonth(start: Long, end: Long): LiveData<Double> =
        db.transactionDao().getTotalSpentThisMonth(start, end)

    // ---- Goals ----
    val allGoals: LiveData<List<SavingsGoal>> = db.savingsGoalDao().getAllGoals()
    suspend fun insertGoal(goal: SavingsGoal) =
        withContext(Dispatchers.IO) { db.savingsGoalDao().insert(goal) }
    suspend fun updateGoal(goal: SavingsGoal) =
        withContext(Dispatchers.IO) { db.savingsGoalDao().update(goal) }
    suspend fun deleteGoal(goal: SavingsGoal) =
        withContext(Dispatchers.IO) { db.savingsGoalDao().delete(goal) }
    fun getGoalByIdLiveData(goalId: Long): LiveData<SavingsGoal?> =
        db.savingsGoalDao().getGoalByIdLiveData(goalId)
    fun getGoalsWithTotalLive(): LiveData<List<SavingsGoalWithTotal>> =
        db.savingsGoalDao().getGoalsWithTotalLive()
    // Plain goal getter – used inside coroutines
    suspend fun getGoalById(goalId: Long): SavingsGoal? =
        withContext(Dispatchers.IO) { db.savingsGoalDao().getGoalById(goalId) }

    // ---- Contributions ----
    fun getContributionsForGoal(goalId: Long): LiveData<List<GoalContribution>> =
        db.goalContributionDao().getContributionsForGoal(goalId)
    suspend fun insertContribution(contribution: GoalContribution) =
        withContext(Dispatchers.IO) { db.goalContributionDao().insert(contribution) }
    suspend fun deleteContribution(contribution: GoalContribution) =
        withContext(Dispatchers.IO) { db.goalContributionDao().delete(contribution) }
    suspend fun getTotalSavedForGoal(goalId: Long): Double =
        withContext(Dispatchers.IO) { db.goalContributionDao().getTotalSavedForGoal(goalId) }

    // ---- User ----
    suspend fun getUser(): User? = withContext(Dispatchers.IO) { db.userDao().getUser() }
    suspend fun insertUser(user: User) = withContext(Dispatchers.IO) { db.userDao().insert(user) }
    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) { db.userDao().update(user) }

    // ---- Achievement check ----
    suspend fun updateGoalAchievedStatus(goalId: Long) {
        val total = getTotalSavedForGoal(goalId)
        val goal = getGoalById(goalId) ?: return
        if (total >= goal.targetAmount && !goal.isAchieved) {
            updateGoal(goal.copy(isAchieved = true))
        } else if (total < goal.targetAmount && goal.isAchieved) {
            updateGoal(goal.copy(isAchieved = false))
        }
    }

    // ---- Smart round‑up ----
    suspend fun performRoundUp(transaction: ExpenseTransaction) {
        val goals = withContext(Dispatchers.IO) {
            db.savingsGoalDao().getRoundUpEnabledGoals()
        }
        if (goals.isEmpty()) return

        val goal = goals.first()
        val rounded = ceil(transaction.amount).toLong().toDouble()
        val spareChange = rounded - transaction.amount
        if (spareChange <= 0.0) return

        withContext(Dispatchers.IO) {
            db.goalContributionDao().insert(
                GoalContribution(
                    goalId = goal.id,
                    amount = spareChange,
                    timestamp = System.currentTimeMillis(),
                    note = "Smart round‑up from: ${transaction.title}"
                )
            )
        }
        updateGoalAchievedStatus(goal.id)
    }

    // ---- Set a goal as the only round‑up target ----
    suspend fun setRoundUpGoal(goalId: Long) {
        withContext(Dispatchers.IO) {
            db.savingsGoalDao().disableAllRoundUp()
            val goal = db.savingsGoalDao().getGoalById(goalId)
            if (goal != null) {
                db.savingsGoalDao().update(goal.copy(roundUpEnabled = true))
            }
        }
    }

    // ---- Contextual nudge ----
    suspend fun getCurrentNudge(): Nudge? {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.clear(Calendar.MINUTE); cal.clear(Calendar.SECOND)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis

        val spentList = withContext(Dispatchers.IO) {
            db.categoryDao().getCategoriesWithSpentList(start, end)
        }

        val overspent = spentList
            .filter { it.monthlyLimit > 0 && it.totalSpent > it.monthlyLimit * 0.8 }
            .maxByOrNull { it.totalSpent / it.monthlyLimit }
            ?: return null

        val goals = withContext(Dispatchers.IO) {
            db.savingsGoalDao().getAllGoalsList()
        }.filter { !it.isAchieved }
        val targetGoal = goals.minByOrNull { it.deadline } ?: return null

        val excess = overspent.totalSpent - overspent.monthlyLimit
        val suggestion = if (excess > 0) (excess * 0.1).toInt().toDouble() else 10.0

        return Nudge(
            categoryName = overspent.name,
            suggestedAmount = suggestion,
            goalLabel = targetGoal.label,
            goalId = targetGoal.id
        )
    }

    // ---- Predictive forecast ----
    suspend fun getGoalForecast(goalId: Long): String {
        val goal = getGoalById(goalId) ?: return "Unknown"
        val totalSaved = getTotalSavedForGoal(goalId)
        val remaining = goal.targetAmount - totalSaved
        if (remaining <= 0) return "Achieved"

        val now = System.currentTimeMillis()
        val threeMonthsAgo = now - 90L * 24 * 3600 * 1000
        val recentContribs = withContext(Dispatchers.IO) {
            db.goalContributionDao().getContributionsForGoalList(goalId)
        }.filter { it.timestamp >= threeMonthsAgo }

        val monthsPassed = maxOf(1.0, (now - threeMonthsAgo) / (30.44 * 24 * 3600 * 1000))
        val monthlyRate = recentContribs.sumOf { it.amount } / monthsPassed
        val monthsRemaining = maxOf(0.0, (goal.deadline - now) / (30.44 * 24 * 3600 * 1000))
        if (monthsRemaining <= 0) return if (totalSaved >= goal.targetAmount) "Achieved" else "Off track"

        val projectedTotal = totalSaved + (monthlyRate * monthsRemaining)
        val projectedPercent = projectedTotal / goal.targetAmount

        return when {
            projectedPercent >= 0.9 -> "On track"
            projectedPercent >= 0.7 -> "At risk"
            else -> "Off track"
        }
    }

    // ---- Goals with forecast (for the adapter) ----
    suspend fun getGoalsWithForecast(): List<SavingsGoalWithForecast> {
        val goals = withContext(Dispatchers.IO) {
            db.savingsGoalDao().getAllGoalsList()
        }
        return goals.map { goal ->
            val total = getTotalSavedForGoal(goal.id)
            val forecast = getGoalForecast(goal.id)
            SavingsGoalWithForecast(goal, total, forecast)
        }
    }
}