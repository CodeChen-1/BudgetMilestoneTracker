package com.example.budgetmilestonetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val darkMode: Boolean = false,
    val language: String = "en"
)