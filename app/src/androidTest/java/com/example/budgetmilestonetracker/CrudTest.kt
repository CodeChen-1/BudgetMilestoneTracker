package com.example.budgetmilestonetracker

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.budgetmilestonetracker.data.db.Category
import com.example.budgetmilestonetracker.data.db.SavingsGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrudTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        // Start fresh and clean the database
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val db = (activity.application as BudgetApplication).database
            runBlocking(Dispatchers.IO) {
                db.goalContributionDao().deleteAll()
                db.savingsGoalDao().deleteAll()
                db.transactionDao().deleteAll()
                db.categoryDao().deleteAll()
            }
        }
        Thread.sleep(500)
    }

    @After
    fun tearDown() {
        try { pressBack() } catch (_: Exception) {}
        scenario.close()
    }

    // ── Category CRUD ─────────────────────────────────────────────

    @Test
    fun testAddCategoryAppearsInList() {
        // Add a category "Groceries" and check it appears
        onView(withId(R.id.btnManageCategories)).perform(click())
        onView(withId(R.id.fabAddCategory)).perform(click())
        onView(withId(R.id.etCategoryName)).perform(typeText("Groceries"), closeSoftKeyboard())
        onView(withId(R.id.etCategoryLimit)).perform(typeText("300"), closeSoftKeyboard())
        onView(withText("Save")).perform(click())
        onView(withText("Groceries")).check(matches(isDisplayed()))
        pressBack()
    }

    @Test
    fun testEditCategoryUpdatesNameInList() {
        // Seed a category, then edit it
        scenario.onActivity { activity ->
            val db = (activity.application as BudgetApplication).database
            runBlocking(Dispatchers.IO) {
                db.categoryDao().insert(Category(name = "OldName", monthlyLimit = 100.0, iconResName = "ic_food"))
            }
        }
        Thread.sleep(300); scenario.close(); Thread.sleep(500)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnManageCategories)).perform(click())
        onView(withText("OldName")).perform(click())
        onView(withText("Edit")).perform(click())
        onView(withId(R.id.etCategoryName)).perform(click(), typeText(" Updated"), closeSoftKeyboard())
        onView(withText("Update")).perform(click())
        onView(withText("OldName Updated")).check(matches(isDisplayed()))
        pressBack()
    }

    @Test
    fun testDeleteCategoryRemovesFromList() {
        // Seed a category, delete it, then verify empty state
        scenario.onActivity { activity ->
            val db = (activity.application as BudgetApplication).database
            runBlocking(Dispatchers.IO) {
                db.categoryDao().insert(Category(name = "ToDelete", monthlyLimit = 100.0, iconResName = "ic_food"))
            }
        }
        Thread.sleep(300); scenario.close(); Thread.sleep(500)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnManageCategories)).perform(click())
        onView(withText("ToDelete")).perform(click())
        onView(withText("Delete")).perform(click())          // popup menu

        Thread.sleep(500)
        onView(allOf(withText("Delete"), isDisplayed())).perform(click()) // confirmation dialog

        // Go back to Dashboard and re‑enter to force a fresh list
        pressBack()
        onView(withId(R.id.btnManageCategories)).perform(click())
        Thread.sleep(1000)

        // Empty‑state text should now be visible
        onView(withText(R.string.empty_categories)).check(matches(isDisplayed()))
        pressBack()
    }

    // ── Savings Goal CRUD ─────────────────────────────────────────

    @Test
    fun testAddSavingsGoalAppearsInList() {
        onView(withId(R.id.btnSavingsGoals)).perform(click())
        onView(withId(R.id.fabAddGoal)).perform(click())
        onView(withId(R.id.etGoalLabel)).perform(typeText("New Laptop"), closeSoftKeyboard())
        onView(withId(R.id.etTargetAmount)).perform(typeText("2500"), closeSoftKeyboard())
        onView(withText("Save")).perform(click())
        onView(withText("New Laptop")).check(matches(isDisplayed()))
        pressBack()
    }

    @Test
    fun testDeleteSavingsGoalRemovesFromList() {
        // Seed a goal, delete it, verify empty state
        scenario.onActivity { activity ->
            val db = (activity.application as BudgetApplication).database
            runBlocking(Dispatchers.IO) {
                db.savingsGoalDao().insert(SavingsGoal(label = "ToDelete", targetAmount = 500.0,
                    deadline = System.currentTimeMillis() + 86400000L))
            }
        }
        Thread.sleep(300); scenario.close(); Thread.sleep(500)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnSavingsGoals)).perform(click())
        onView(withText("ToDelete")).check(matches(isDisplayed()))
        onView(withId(R.id.btnDelete)).perform(click())

        Thread.sleep(500)
        onView(allOf(withText("Delete"), isDisplayed())).perform(click()) // confirm dialog

        // Return to Dashboard and re‑enter Savings Goals
        pressBack()
        onView(withId(R.id.btnSavingsGoals)).perform(click())
        Thread.sleep(1000)

        // Empty‑state should appear
        onView(withText(R.string.empty_goals)).check(matches(isDisplayed()))
        pressBack()
    }

    // ── Transaction Flow ──────────────────────────────────────────

    @Test
    fun testAddTransactionAppearsInCategoryList() {
        // Seed a category "Food"
        scenario.onActivity { activity ->
            val db = (activity.application as BudgetApplication).database
            runBlocking(Dispatchers.IO) {
                db.categoryDao().insert(Category(name = "Food", monthlyLimit = 500.0, iconResName = "ic_food"))
            }
        }
        Thread.sleep(300); scenario.close(); Thread.sleep(500)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        // Allow the new activity to fully settle and any stale views to disappear
        Thread.sleep(1000)

        // Open the transaction form
        onView(withId(R.id.fabAddTransaction)).perform(click())
        onView(withId(R.id.etTitle)).perform(typeText("Lunch"), closeSoftKeyboard())
        onView(withId(R.id.etAmount)).perform(typeText("12.50"), closeSoftKeyboard())
        onView(withId(R.id.btnSave)).perform(click())

        // Tap the visible "Food" row – extra delay to avoid stale views
        Thread.sleep(500)
        onView(allOf(withText("Food"), isDisplayed())).perform(click())
        onView(withText("Lunch")).check(matches(isDisplayed()))
        pressBack()
    }

    // ── Goal Details Flow ─────────────────────────────────────────

    @Test
    fun testAddContributionAppearsInGoalDetails() {
        scenario.onActivity { activity ->
            val db = (activity.application as BudgetApplication).database
            runBlocking(Dispatchers.IO) {
                db.savingsGoalDao().insert(SavingsGoal(label = "Holiday", targetAmount = 1000.0,
                    deadline = System.currentTimeMillis() + 86400000L))
            }
        }
        Thread.sleep(300); scenario.close(); Thread.sleep(500)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnSavingsGoals)).perform(click())
        onView(withText("Holiday")).perform(click())
        onView(withId(R.id.fabAddContribution)).perform(click())
        onView(withId(R.id.etContributionAmount)).perform(typeText("200"), closeSoftKeyboard())
        onView(withText("Save")).perform(click())
        onView(withText("+ RM 200.00")).check(matches(isDisplayed()))
        pressBack()
    }

    @Test
    fun testDeleteContributionRemovesFromGoalDetails() {
        scenario.onActivity { activity ->
            val db = (activity.application as BudgetApplication).database
            runBlocking(Dispatchers.IO) {
                db.savingsGoalDao().insert(SavingsGoal(label = "Holiday", targetAmount = 1000.0,
                    deadline = System.currentTimeMillis() + 86400000L))
            }
        }
        Thread.sleep(300); scenario.close(); Thread.sleep(500)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.btnSavingsGoals)).perform(click())
        onView(withText("Holiday")).perform(click())
        onView(withId(R.id.fabAddContribution)).perform(click())
        onView(withId(R.id.etContributionAmount)).perform(typeText("150"), closeSoftKeyboard())
        onView(withText("Save")).perform(click())

        onView(withText("+ RM 150.00")).check(matches(isDisplayed()))
        onView(withId(R.id.btnDelete)).perform(click())

        Thread.sleep(800)
        onView(withText("+ RM 150.00")).check(doesNotExist())
        pressBack()
    }
}