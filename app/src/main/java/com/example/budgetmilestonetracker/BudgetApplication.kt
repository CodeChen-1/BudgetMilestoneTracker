package com.example.budgetmilestonetracker

import android.app.Application
import com.example.budgetmilestonetracker.data.db.AppDatabase
import com.example.budgetmilestonetracker.data.repository.BudgetRepository

class BudgetApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { BudgetRepository(database) }
}