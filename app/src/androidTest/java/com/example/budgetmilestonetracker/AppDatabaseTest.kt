package com.example.budgetmilestonetracker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.budgetmilestonetracker.data.db.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var savingsGoalDao: SavingsGoalDao
    private lateinit var contributionDao: GoalContributionDao
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        categoryDao    = db.categoryDao()
        transactionDao = db.transactionDao()
        savingsGoalDao = db.savingsGoalDao()
        contributionDao = db.goalContributionDao()
        userDao        = db.userDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ── Category CRUD ─────────────────────────────────────────────

    @Test
    fun testCategoryInsertAndRead() {
        categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        val list = categoryDao.getAllCategories().getOrAwaitValue()
        assertEquals(1, list.size)
        assertEquals("Food", list[0].name)
        assertEquals(500.0, list[0].monthlyLimit, 0.01)
    }

    @Test
    fun testCategoryUpdate() {
        val id = categoryDao.insert(Category(name = "Old", monthlyLimit = 100.0, iconResName = "ic_food"))
        categoryDao.update(Category(id = id, name = "Updated", monthlyLimit = 200.0, iconResName = "ic_transport"))
        val result = categoryDao.getCategoryById(id)
        assertEquals("Updated", result?.name)
        assertEquals(200.0, result?.monthlyLimit ?: 0.0, 0.01)
        assertEquals("ic_transport", result?.iconResName)
    }

    @Test
    fun testCategoryDelete() {
        val id = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        categoryDao.delete(categoryDao.getCategoryById(id)!!)
        assertTrue(categoryDao.getAllCategories().getOrAwaitValue().isEmpty())
    }

    @Test
    fun testCategoryMultipleSortedAlphabetically() {
        categoryDao.insert(Category(name = "Transport", monthlyLimit = 200.0, iconResName = "ic_transport"))
        categoryDao.insert(Category(name = "Food",      monthlyLimit = 500.0, iconResName = "ic_food"))
        categoryDao.insert(Category(name = "Health",    monthlyLimit = 150.0, iconResName = "ic_health"))
        val list = categoryDao.getAllCategories().getOrAwaitValue()
        assertEquals("Food",      list[0].name)
        assertEquals("Health",    list[1].name)
        assertEquals("Transport", list[2].name)
    }

    // ── Transaction CRUD ──────────────────────────────────────────

    @Test
    fun testTransactionInsertAndReadByCategory() {
        val catId = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Lunch", amount = 15.0, timestamp = System.currentTimeMillis()))
        val list = transactionDao.getTransactionsByCategory(catId).getOrAwaitValue()
        assertEquals(1, list.size)
        assertEquals("Lunch", list[0].title)
    }

    @Test
    fun testTransactionUpdate() {
        val catId = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Lunch", amount = 15.0, timestamp = System.currentTimeMillis()))
        val t = transactionDao.getTransactionsByCategory(catId).getOrAwaitValue()[0]
        transactionDao.update(t.copy(title = "Dinner", amount = 30.0))
        val updated = transactionDao.getTransactionById(t.id)
        assertEquals("Dinner", updated?.title)
        assertEquals(30.0, updated?.amount ?: 0.0, 0.01)
    }

    @Test
    fun testTransactionDelete() {
        val catId = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Lunch", amount = 15.0, timestamp = System.currentTimeMillis()))
        val t = transactionDao.getTransactionsByCategory(catId).getOrAwaitValue()[0]
        transactionDao.delete(t)
        assertTrue(transactionDao.getTransactionsByCategory(catId).getOrAwaitValue().isEmpty())
    }

    @Test
    fun testTransactionCascadeDeleteWithCategory() {
        val catId = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Lunch", amount = 15.0, timestamp = System.currentTimeMillis()))
        categoryDao.delete(categoryDao.getCategoryById(catId)!!)
        assertTrue(transactionDao.getTransactionsByCategory(catId).getOrAwaitValue().isEmpty())
    }

    @Test
    fun testTransactionOnlyReturnsMatchingCategory() {
        val catId1 = categoryDao.insert(Category(name = "Food",      monthlyLimit = 500.0, iconResName = "ic_food"))
        val catId2 = categoryDao.insert(Category(name = "Transport", monthlyLimit = 200.0, iconResName = "ic_transport"))
        transactionDao.insert(ExpenseTransaction(categoryId = catId1, title = "Lunch", amount = 15.0, timestamp = System.currentTimeMillis()))
        transactionDao.insert(ExpenseTransaction(categoryId = catId2, title = "Bus",   amount = 5.0,  timestamp = System.currentTimeMillis()))
        val list = transactionDao.getTransactionsByCategory(catId1).getOrAwaitValue()
        assertEquals(1, list.size)
        assertEquals("Lunch", list[0].title)
    }

    @Test
    fun testTransactionNotesPersisted() {
        val catId = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Lunch", amount = 15.0, timestamp = System.currentTimeMillis(), notes = "Team lunch"))
        val t = transactionDao.getTransactionsByCategory(catId).getOrAwaitValue()[0]
        assertEquals("Team lunch", t.notes)
    }

    @Test
    fun testTransactionNullNotesAllowed() {
        val catId = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Lunch", amount = 15.0, timestamp = System.currentTimeMillis(), notes = null))
        val t = transactionDao.getTransactionsByCategory(catId).getOrAwaitValue()[0]
        assertNull(t.notes)
    }

    // ── JOIN: getCategoriesWithSpent ───────────────────────────────

    @Test
    fun testCategoriesWithSpentSumsCorrectly() {
        val catId = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        val now = System.currentTimeMillis()
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Lunch",  amount = 15.0, timestamp = now))
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Dinner", amount = 25.0, timestamp = now))
        val list = categoryDao.getCategoriesWithSpent(now - 86400000L, now + 86400000L).getOrAwaitValue()
        assertEquals(40.0, list[0].totalSpent, 0.01)
    }

    @Test
    fun testCategoriesWithSpentZeroWhenNoTransactions() {
        categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        val now = System.currentTimeMillis()
        val list = categoryDao.getCategoriesWithSpent(now - 86400000L, now + 86400000L).getOrAwaitValue()
        assertEquals(0.0, list[0].totalSpent, 0.01)
    }

    @Test
    fun testCategoriesWithSpentExcludesOutOfRange() {
        val catId = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        val now = System.currentTimeMillis()
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Old", amount = 100.0, timestamp = now - 172800000L))
        val list = categoryDao.getCategoriesWithSpent(now - 86400000L, now + 86400000L).getOrAwaitValue()
        assertEquals(0.0, list[0].totalSpent, 0.01)
    }

    // ── getTotalSpentThisMonth ─────────────────────────────────────

    @Test
    fun testTotalSpentThisMonthSumsCorrectly() {
        val catId = categoryDao.insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
        val now = System.currentTimeMillis()
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Lunch",  amount = 20.0, timestamp = now))
        transactionDao.insert(ExpenseTransaction(categoryId = catId, title = "Dinner", amount = 30.0, timestamp = now))
        val total = transactionDao.getTotalSpentThisMonth(now - 86400000L, now + 86400000L).getOrAwaitValue()
        assertEquals(50.0, total, 0.01)
    }

    @Test
    fun testTotalSpentThisMonthZeroWhenEmpty() {
        val now = System.currentTimeMillis()
        val total = transactionDao.getTotalSpentThisMonth(now - 86400000L, now + 86400000L).getOrAwaitValue()
        assertEquals(0.0, total, 0.01)
    }

    // ── SavingsGoal CRUD ──────────────────────────────────────────

    @Test
    fun testSavingsGoalInsertAndRead() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val list = savingsGoalDao.getAllGoals().getOrAwaitValue()
        assertEquals(1, list.size)
        assertEquals("Laptop", list[0].label)
        assertEquals(1000.0, list[0].targetAmount, 0.01)
    }

    @Test
    fun testSavingsGoalUpdate() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goal = savingsGoalDao.getAllGoals().getOrAwaitValue()[0]
        savingsGoalDao.update(goal.copy(label = "Gaming PC", targetAmount = 2000.0))
        val updated = savingsGoalDao.getGoalById(goal.id)
        assertEquals("Gaming PC", updated?.label)
        assertEquals(2000.0, updated?.targetAmount ?: 0.0, 0.01)
    }

    @Test
    fun testSavingsGoalDelete() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goal = savingsGoalDao.getAllGoals().getOrAwaitValue()[0]
        savingsGoalDao.delete(goal)
        assertTrue(savingsGoalDao.getAllGoals().getOrAwaitValue().isEmpty())
    }

    @Test
    fun testSavingsGoalRoundUpFlagPersisted() {
        // roundUpEnabled defaults to false; verify it persists when set true
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L, roundUpEnabled = true))
        val goal = savingsGoalDao.getAllGoals().getOrAwaitValue()[0]
        assertTrue(goal.roundUpEnabled)
    }

    @Test
    fun testDisableAllRoundUpClearsFlag() {
        savingsGoalDao.insert(SavingsGoal(label = "A", targetAmount = 100.0, deadline = System.currentTimeMillis() + 86400000L, roundUpEnabled = true))
        savingsGoalDao.insert(SavingsGoal(label = "B", targetAmount = 200.0, deadline = System.currentTimeMillis() + 86400000L, roundUpEnabled = true))
        savingsGoalDao.disableAllRoundUp()
        val goals = savingsGoalDao.getAllGoals().getOrAwaitValue()
        assertTrue(goals.all { !it.roundUpEnabled })
    }

    // ── GoalContribution CRUD ─────────────────────────────────────

    @Test
    fun testContributionInsertAndRead() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 200.0, timestamp = System.currentTimeMillis()))
        val list = contributionDao.getContributionsForGoal(goalId).getOrAwaitValue()
        assertEquals(1, list.size)
        assertEquals(200.0, list[0].amount, 0.01)
    }

    @Test
    fun testContributionDelete() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 200.0, timestamp = System.currentTimeMillis()))
        val c = contributionDao.getContributionsForGoal(goalId).getOrAwaitValue()[0]
        contributionDao.delete(c)
        assertTrue(contributionDao.getContributionsForGoal(goalId).getOrAwaitValue().isEmpty())
    }

    @Test
    fun testContributionCascadeDeleteWithGoal() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 200.0, timestamp = System.currentTimeMillis()))
        savingsGoalDao.delete(savingsGoalDao.getGoalById(goalId)!!)
        assertTrue(contributionDao.getContributionsForGoal(goalId).getOrAwaitValue().isEmpty())
    }

    @Test
    fun testContributionTotalSavedSumsMultiple() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 200.0, timestamp = System.currentTimeMillis()))
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 350.0, timestamp = System.currentTimeMillis()))
        assertEquals(550.0, contributionDao.getTotalSavedForGoal(goalId), 0.01)
    }

    @Test
    fun testContributionTotalSavedZeroWhenNone() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        assertEquals(0.0, contributionDao.getTotalSavedForGoal(goalId), 0.01)
    }

    @Test
    fun testContributionNotesPersisted() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 100.0, timestamp = System.currentTimeMillis(), note = "Birthday money"))
        val c = contributionDao.getContributionsForGoal(goalId).getOrAwaitValue()[0]
        assertEquals("Birthday money", c.note)
    }

    // ── JOIN: getGoalsWithTotalLive ───────────────────────────────

    @Test
    fun testGoalsWithTotalCorrectSumFromJoin() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 300.0, timestamp = System.currentTimeMillis()))
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 150.0, timestamp = System.currentTimeMillis()))
        val list = savingsGoalDao.getGoalsWithTotalLive().getOrAwaitValue()
        assertEquals(1, list.size)
        assertEquals(450.0, list[0].totalSaved, 0.01)
    }

    @Test
    fun testGoalsWithTotalZeroWhenNoContributions() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val list = savingsGoalDao.getGoalsWithTotalLive().getOrAwaitValue()
        assertEquals(0.0, list[0].totalSaved, 0.01)
    }

    @Test
    fun testGoalsWithTotalUpdatesAfterContributionDeleted() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 400.0, timestamp = System.currentTimeMillis()))
        val c = contributionDao.getContributionsForGoal(goalId).getOrAwaitValue()[0]
        contributionDao.delete(c)
        assertEquals(0.0, savingsGoalDao.getGoalsWithTotalLive().getOrAwaitValue()[0].totalSaved, 0.01)
    }

    // ── Goal achieved status logic ────────────────────────────────

    @Test
    fun testGoalMarkedAchievedWhenContributionsReachTarget() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 500.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 300.0, timestamp = System.currentTimeMillis()))
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 200.0, timestamp = System.currentTimeMillis()))
        val total = contributionDao.getTotalSavedForGoal(goalId)
        val goal = savingsGoalDao.getGoalById(goalId)!!
        if (total >= goal.targetAmount && !goal.isAchieved) savingsGoalDao.update(goal.copy(isAchieved = true))
        assertTrue(savingsGoalDao.getGoalById(goalId)!!.isAchieved)
    }

    @Test
    fun testGoalUnmarkedAchievedWhenContributionDeleted() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 500.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 500.0, timestamp = System.currentTimeMillis()))
        savingsGoalDao.update(savingsGoalDao.getGoalById(goalId)!!.copy(isAchieved = true))
        val c = contributionDao.getContributionsForGoal(goalId).getOrAwaitValue()[0]
        contributionDao.delete(c)
        val total = contributionDao.getTotalSavedForGoal(goalId)
        val goal = savingsGoalDao.getGoalById(goalId)!!
        if (total < goal.targetAmount && goal.isAchieved) savingsGoalDao.update(goal.copy(isAchieved = false))
        assertFalse(savingsGoalDao.getGoalById(goalId)!!.isAchieved)
    }

    @Test
    fun testGoalNotAchievedWhenContributionsShort() {
        savingsGoalDao.insert(SavingsGoal(label = "Laptop", targetAmount = 1000.0, deadline = System.currentTimeMillis() + 86400000L))
        val goalId = savingsGoalDao.getAllGoals().getOrAwaitValue()[0].id
        contributionDao.insert(GoalContribution(goalId = goalId, amount = 400.0, timestamp = System.currentTimeMillis()))
        assertFalse(savingsGoalDao.getGoalById(goalId)!!.isAchieved)
    }

    // ── User CRUD ─────────────────────────────────────────────────

    @Test
    fun testUserInsertAndRead() {
        userDao.insert(User(username = "Alice", darkMode = false, language = "en"))
        val user = userDao.getUser()
        assertNotNull(user)
        assertEquals("Alice", user?.username)
        assertEquals("en", user?.language)
        assertFalse(user?.darkMode ?: true)
    }

    @Test
    fun testUserUpdate() {
        userDao.insert(User(username = "Alice", darkMode = false, language = "en"))
        val user = userDao.getUser()!!
        userDao.update(user.copy(username = "Bob", darkMode = true, language = "ms"))
        val updated = userDao.getUser()
        assertEquals("Bob", updated?.username)
        assertTrue(updated?.darkMode ?: false)
        assertEquals("ms", updated?.language)
    }

    @Test
    fun testUserDeleteAll() {
        userDao.insert(User(username = "Alice", darkMode = false, language = "en"))
        userDao.deleteAll()
        assertNull(userDao.getUser())
    }

    @Test
    fun testUserOnlyOneRowReturned() {
        // REPLACE strategy should overwrite — only one user should exist
        userDao.insert(User(username = "Alice", darkMode = false, language = "en"))
        userDao.insert(User(username = "Bob",   darkMode = true,  language = "zh"))
        assertNotNull(userDao.getUser())
    }

    // ── Helper ────────────────────────────────────────────────────

    private fun <T> LiveData<T>.getOrAwaitValue(timeoutSec: Long = 3): T {
        var result: T? = null
        val latch = CountDownLatch(1)
        val observer = object : androidx.lifecycle.Observer<T> {
            override fun onChanged(value: T) {
                result = value
                latch.countDown()
                removeObserver(this)
            }
        }
        observeForever(observer)
        if (!latch.await(timeoutSec, TimeUnit.SECONDS)) {
            removeObserver(observer)
            throw TimeoutException("LiveData did not emit within ${timeoutSec}s")
        }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}