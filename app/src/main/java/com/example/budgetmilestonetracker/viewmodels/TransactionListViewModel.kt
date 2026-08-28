package com.example.budgetmilestonetracker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.budgetmilestonetracker.BudgetApplication
import com.example.budgetmilestonetracker.data.db.ExpenseTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as BudgetApplication).repository

    private val _categoryId = MutableLiveData<Long>()
    private val _searchQuery = MutableLiveData("")
    private val _sortMode = MutableLiveData(0)

    private val rawTransactions: LiveData<List<ExpenseTransaction>> = _categoryId.switchMap { id ->
        repo.getTransactionsByCategory(id)
    }

    // MediatorLiveData observes all three sources — updates whenever any one changes
    val transactions: MediatorLiveData<List<ExpenseTransaction>> = MediatorLiveData<List<ExpenseTransaction>>().apply {
        fun recompute() {
            val list = rawTransactions.value ?: return
            val query = _searchQuery.value ?: ""
            val sort = _sortMode.value ?: 0
            value = list
                .filter {
                    it.title.contains(query, ignoreCase = true) ||
                            (it.notes?.contains(query, ignoreCase = true) == true)
                }
                .let { filtered ->
                    when (sort) {
                        0 -> filtered.sortedByDescending { it.timestamp }
                        1 -> filtered.sortedBy { it.timestamp }
                        2 -> filtered.sortedByDescending { it.amount }
                        3 -> filtered.sortedBy { it.amount }
                        else -> filtered
                    }
                }
        }
        addSource(rawTransactions) { recompute() }
        addSource(_searchQuery) { recompute() }
        addSource(_sortMode) { recompute() }
    }

    fun setCategoryId(id: Long) { _categoryId.value = id }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortMode(mode: Int) { _sortMode.value = mode }

    fun deleteTransaction(transaction: ExpenseTransaction) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteTransaction(transaction) }
    }

    fun insertTransaction(transaction: ExpenseTransaction) {
        viewModelScope.launch(Dispatchers.IO) { repo.insertTransaction(transaction) }
    }
}