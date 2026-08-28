package com.example.budgetmilestonetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val monthlyLimit: Double,
    val iconResName: String
)