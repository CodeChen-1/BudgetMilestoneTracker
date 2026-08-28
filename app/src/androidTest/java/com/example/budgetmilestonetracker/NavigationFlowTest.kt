package com.example.budgetmilestonetracker

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        // Start fresh activity and wipe the database
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
        // Let Room finish its background work
        Thread.sleep(500)
    }

    @After
    fun tearDown() {
        // Always try to go back to the dashboard, then close the activity
        try { pressBack() } catch (_: Exception) {}
        scenario.close()
    }

    // ── Dashboard initial state ───────────────────────────────────

    @Test
    fun testDashboardShowsAllMainViews() {
        // Check that the four main UI pieces are visible on launch
        onView(withId(R.id.recyclerCategories)).check(matches(isDisplayed()))
        onView(withId(R.id.fabAddTransaction)).check(matches(isDisplayed()))
        onView(withId(R.id.btnManageCategories)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSavingsGoals)).check(matches(isDisplayed()))
        // The chart button should also be visible
        onView(withId(R.id.btnChart)).check(matches(isDisplayed()))
    }

    // ── Navigation ────────────────────────────────────────────────

    @Test
    fun testNavigateToCategoryManageAndBack() {
        // Tap Manage Categories, check we arrived, then press back
        onView(withId(R.id.btnManageCategories)).perform(click())
        onView(withId(R.id.fabAddCategory)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.recyclerCategories)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToSavingsGoalsAndBack() {
        // Tap Savings Goals – the list is empty, so we check the FAB instead
        onView(withId(R.id.btnSavingsGoals)).perform(click())
        onView(withId(R.id.fabAddGoal)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.recyclerCategories)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToTransactionFormFromFAB() {
        // Tap the FAB, verify the form opened, then go back
        onView(withId(R.id.fabAddTransaction)).perform(click())
        onView(withId(R.id.btnSave)).check(matches(isDisplayed()))
        onView(withId(R.id.spinnerCategory)).check(matches(isDisplayed()))
        onView(withId(R.id.etTitle)).check(matches(isDisplayed()))
        pressBack()
    }

    // ── NEW: Spending Chart navigation ────────────────────────────

    @Test
    fun testSpendingChartOpens() {
        // Tap the Spending Chart button on the dashboard
        onView(withId(R.id.btnChart)).perform(click())
        // The chart screen should show the pie chart view
        onView(withId(R.id.pieChart)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.recyclerCategories)).check(matches(isDisplayed()))
    }

    // ── Form Validation ───────────────────────────────────────────

    @Test
    fun testEmptyTitleShowsRequiredError() {
        // Open form, press Save immediately – must show "Required"
        onView(withId(R.id.fabAddTransaction)).perform(click())
        onView(withId(R.id.btnSave)).perform(click())
        onView(withId(R.id.tilTitle)).check(matches(hasDescendant(withText("Required"))))
        pressBack()
    }

    @Test
    fun testZeroAmountShowsAmountError() {
        // Type a title but no amount – must show "Must be > 0"
        onView(withId(R.id.fabAddTransaction)).perform(click())
        onView(withId(R.id.etTitle)).perform(typeText("Lunch"), closeSoftKeyboard())
        onView(withId(R.id.btnSave)).perform(click())
        onView(withId(R.id.tilAmount)).check(matches(hasDescendant(withText("Must be > 0"))))
        pressBack()
    }

    @Test
    fun testNoCategoryStaysOnForm() {
        // Fill in valid data but there are no categories – form stays open
        onView(withId(R.id.fabAddTransaction)).perform(click())
        onView(withId(R.id.etTitle)).perform(typeText("Lunch"), closeSoftKeyboard())
        onView(withId(R.id.etAmount)).perform(typeText("10"), closeSoftKeyboard())
        onView(withId(R.id.btnSave)).perform(click())
        onView(withId(R.id.btnSave)).check(matches(isDisplayed()))
        pressBack()
    }

    // ── Profile navigation ────────────────────────────────────────

    @Test
    fun testProfileScreenIsAccessible() {
        // Open overflow menu, tap Profile, verify we see the username field
        onView(withContentDescription("More options")).perform(click())
        onView(withText("Profile")).perform(click())
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()))
        pressBack()
    }
}