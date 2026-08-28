package com.example.budgetmilestonetracker.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.budgetmilestonetracker.BudgetApplication
import com.example.budgetmilestonetracker.data.db.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as BudgetApplication).repository

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val u = repo.getUser()
            _user.postValue(u)
        }
    }

    fun saveProfile(username: String, darkMode: Boolean, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repo.getUser()
            if (existing == null) {
                repo.insertUser(User(username = username, darkMode = darkMode, language = language))
            } else {
                val updated = existing.copy(username = username, darkMode = darkMode, language = language)
                repo.updateUser(updated)
            }
            _user.postValue(repo.getUser())
        }
    }
}