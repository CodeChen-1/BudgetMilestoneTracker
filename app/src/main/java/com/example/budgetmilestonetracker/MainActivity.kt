package com.example.budgetmilestonetracker

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.budgetmilestonetracker.data.db.User
import com.example.budgetmilestonetracker.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.*
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var navController: NavController? = null

    companion object {
        private const val PREFS_NAME = "budget_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LOCALE = "locale"

        var lastDestinationId: Int? = null

        fun isDarkMode(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false)

        fun getLocale(context: Context): String =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LOCALE, "en") ?: "en"

        fun savePreferences(context: Context, darkMode: Boolean, locale: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putBoolean(KEY_DARK_MODE, darkMode)
                    .putString(KEY_LOCALE, locale)
            }
        }
    }

    // Make sure every string resolves to the stored language before the activity is created
    override fun attachBaseContext(newBase: Context) {
        val locale = getLocale(newBase)
        super.attachBaseContext(applyLocale(newBase, locale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply dark mode early so the theme is set before inflation
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Make sure there is always a user in the database
        ensureDefaultUser()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.dashboardFragment)
        )
        // Let the navigation component handle the toolbar title automatically
        setupActionBarWithNavController(navController!!, appBarConfiguration)

        // If we came back from a language change, jump straight to the screen we were on
        if (savedInstanceState == null && Companion.lastDestinationId != null) {
            val destId = Companion.lastDestinationId!!
            Companion.lastDestinationId = null
            navController?.navigate(
                destId, null,
                NavOptions.Builder()
                    .setPopUpTo(navController!!.graph.startDestinationId, true)
                    .build()
            )
        }
    }

    // Creates a default user if none exists — runs off the main thread
    private fun ensureDefaultUser() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = (application as BudgetApplication).database
            if (db.userDao().getUser() == null) {
                db.userDao().insert(User(username = "User", darkMode = false, language = "en"))
            }
        }
    }

    // Keeps the Room user entity synchronised with the current preferences
    private fun syncUserPreferences(darkMode: Boolean, language: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = (application as BudgetApplication).database
            val user = db.userDao().getUser()
            if (user != null) {
                db.userDao().update(user.copy(darkMode = darkMode, language = language))
            } else {
                db.userDao().insert(User(username = "User", darkMode = darkMode, language = language))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu?.findItem(R.id.action_dark_mode)?.isChecked = isDarkMode(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onSupportNavigateUp()
                return true
            }
            R.id.action_home -> {
                navController?.popBackStack(R.id.dashboardFragment, false)
                return true
            }
            R.id.action_profile -> {
                navController?.navigate(R.id.profileFragment)
                return true
            }
            R.id.action_dark_mode -> {
                val newDarkMode = !isDarkMode(this)
                savePreferences(this, newDarkMode, getLocale(this))
                syncUserPreferences(newDarkMode, getLocale(this))
                AppCompatDelegate.setDefaultNightMode(
                    if (newDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
                recreate()
                return true
            }
            R.id.action_lang_en -> {
                savePreferences(this, isDarkMode(this), "en")
                syncUserPreferences(isDarkMode(this), "en")
                saveLanguageAndRecreate()
                return true
            }
            R.id.action_lang_ms -> {
                savePreferences(this, isDarkMode(this), "ms")
                syncUserPreferences(isDarkMode(this), "ms")
                saveLanguageAndRecreate()
                return true
            }
            R.id.action_lang_zh -> {
                savePreferences(this, isDarkMode(this), "zh")
                syncUserPreferences(isDarkMode(this), "zh")
                saveLanguageAndRecreate()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Remembers which screen we're on, then recreates the activity so the new locale takes effect
    private fun saveLanguageAndRecreate() {
        Companion.lastDestinationId = navController?.currentDestination?.id
        recreate()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() ?: false || super.onSupportNavigateUp()
    }

    // Wraps a context so that all resources use the chosen language
    private fun applyLocale(context: Context, lang: String): Context {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}