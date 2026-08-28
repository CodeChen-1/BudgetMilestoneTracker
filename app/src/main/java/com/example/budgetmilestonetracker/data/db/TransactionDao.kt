package com.example.budgetmilestonetracker.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {
    @Insert
    fun insert(transaction: ExpenseTransaction): Long

    @Update
    fun update(transaction: ExpenseTransaction)

    @Delete
    fun delete(transaction: ExpenseTransaction)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE categoryId = :categoryId AND timestamp >= :start AND timestamp < :end")
    fun getCategorySpent(categoryId: Long, start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :startOfMonth AND timestamp < :endOfMonth")
    fun getTotalSpentThisMonthPlain(startOfMonth: Long, endOfMonth: Long): Double

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryId: Long): LiveData<List<ExpenseTransaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): ExpenseTransaction?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :startOfMonth AND timestamp < :endOfMonth")
    fun getTotalSpentThisMonth(startOfMonth: Long, endOfMonth: Long): LiveData<Double>

    @Query("DELETE FROM transactions")
    fun deleteAll()
}