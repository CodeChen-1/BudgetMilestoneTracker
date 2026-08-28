package com.example.budgetmilestonetracker.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(category: Category): Long

    @Update
    fun update(category: Category)

    @Delete
    fun delete(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories")
    fun getAllCategoriesList(): List<Category>

    @Query("""SELECT c.id as categoryId, c.name, c.monthlyLimit, c.iconResName,
               COALESCE(SUM(t.amount), 0) as totalSpent
        FROM categories c
        LEFT JOIN transactions t ON c.id = t.categoryId
              AND t.timestamp >= :startOfMonth AND t.timestamp < :endOfMonth
        GROUP BY c.id
        ORDER BY c.name ASC""")
    fun getCategoriesWithSpentList(startOfMonth: Long, endOfMonth: Long): List<CategoryWithSpent>

    @Query("""
        SELECT c.id as categoryId, c.name, c.monthlyLimit, c.iconResName,
               COALESCE(SUM(t.amount), 0) as totalSpent
        FROM categories c
        LEFT JOIN transactions t ON c.id = t.categoryId
              AND t.timestamp >= :startOfMonth AND t.timestamp < :endOfMonth
        GROUP BY c.id
        ORDER BY c.name ASC
    """)
    fun getCategoriesWithSpent(startOfMonth: Long, endOfMonth: Long): LiveData<List<CategoryWithSpent>>

    @Query("DELETE FROM categories")
    fun deleteAll()
}