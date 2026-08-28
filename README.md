# BudgetMilestoneTracker

A personal finance Android application for tracking expenses, setting savings goals, and visualizing spending patterns. Uses Malaysian Ringgit (RM) as the default currency.

## Features

- Expense tracking by category with configurable monthly limits
- Savings goals with target amounts and deadlines
- Predictive forecast: "On track" / "At risk" / "Off track"
- Smart round-up: spare change automatically contributed to savings
- Contextual nudges when overspending in a category
- Custom pie chart visualization for spending breakdown
- Dark mode toggle
- 3-language support: English, Bahasa Melayu, Chinese (Simplified)
- User profile with preferences

## Tech Stack

- **Language:** Kotlin
- **Platform:** Android (Min SDK 24, Target SDK 34)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Database:** Room (with migrations)
- **Navigation:** AndroidX Navigation Component with Safe Args
- **UI:** Android Views with View Binding, Material Design
- **Async:** Kotlin Coroutines + LiveData
- **Build:** Gradle 9.3.1 with Kotlin DSL

## Build & Run

1. Open the project in Android Studio
2. Sync Gradle dependencies
3. Run on an emulator or physical device (Android 7.0+)

## Testing

The project includes 30+ instrumented tests covering:
- Database CRUD operations for all entities
- JOIN queries and cascade deletes
- UI-level CRUD flows (categories, goals, transactions)
- Navigation flow tests
- Form validation

Run tests via Android Studio or:
```bash
./gradlew connectedAndroidTest
```
