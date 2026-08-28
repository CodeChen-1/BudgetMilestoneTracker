package com.example.budgetmilestonetracker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetmilestonetracker.BudgetApplication
import com.example.budgetmilestonetracker.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class TransactionFormViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as BudgetApplication).repository
    val allCategories: LiveData<List<Category>> = repo.allCategories

    fun saveTransaction(
        transactionId: Long?,
        categoryId: Long,
        title: String,
        amount: Double,
        timestamp: Long,
        notes: String?
    ) {
        val t = ExpenseTransaction(
            id = transactionId ?: 0L,
            categoryId = categoryId,
            title = title,
            amount = amount,
            timestamp = timestamp,
            notes = notes
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (transactionId == null) repo.insertTransaction(t)
            else repo.updateTransaction(t)
        }
    }

    // Check if adding this amount would exceed the category's monthly limit
    suspend fun wouldExceedLimit(categoryId: Long, amount: Double): Boolean {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.clear(Calendar.MINUTE); cal.clear(Calendar.SECOND)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis

        // Use the repository's plain method for current spending
        val spent = repo.getCategorySpent(categoryId, start, end)
        val category = repo.getCategoryById(categoryId)   // we'll add this method to the repository
        val limit = category?.monthlyLimit ?: return false
        return (spent + amount) > limit
    }

    // Smart round‑up after saving a transaction
    fun performRoundUp(transaction: ExpenseTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<BudgetApplication>()
            app.repository.performRoundUp(transaction)
        }
    }

    suspend fun getTransactionById(id: Long): ExpenseTransaction? = repo.getTransactionById(id)
}