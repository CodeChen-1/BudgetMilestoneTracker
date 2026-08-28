package com.example.budgetmilestonetracker.data.db

import androidx.room.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Update
    fun update(user: User)

    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): User?

    @Query("DELETE FROM users")
    fun deleteAll()
}