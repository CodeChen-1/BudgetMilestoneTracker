package com.example.budgetmilestonetracker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetmilestonetracker.BudgetApplication
import com.example.budgetmilestonetracker.data.db.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryManageViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as BudgetApplication).repository
    val allCategories: LiveData<List<Category>> = repo.allCategories

    fun addCategory(name: String, limit: Double, iconRes: String) {
        val cat = Category(name = name, monthlyLimit = limit, iconResName = iconRes)
        viewModelScope.launch(Dispatchers.IO) { repo.insertCategory(cat) }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) { repo.updateCategory(category) }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteCategory(category) }
    }
}