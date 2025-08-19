# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Expense Butler is an Android expense tracking application built with Kotlin and Jetpack Compose. It uses modern Android development practices including MVVM architecture, Hilt for dependency injection, Room for database management, and Compose for UI.

For build commands, testing instructions, and technology stack details, see:
- **Commands & Tools**: @.kiro/steering/tech.md
- **Development Process**: @.kiro/steering/development-workflow.md

## Architecture

### MVVM + Clean Architecture Pattern

The app follows MVVM with Repository pattern:

1. **UI Layer** (`ui/`)
   - **Screens**: Composable screens that observe ViewModels
   - **ViewModels**: Manage UI state and business logic, use Hilt injection
   - **Components**: Reusable Compose UI components
   - **Navigation**: Single Activity with Compose Navigation

2. **Data Layer** (`data/`)
   - **Repository**: Single source of truth, abstracts data sources
   - **Database**: Room database with DAOs for local storage
   - **Models**: Data classes for expenses, categories, recurring expenses
   - **Converters**: Type converters for Room database

3. **Dependency Injection** (`di/`)
   - **DatabaseModule**: Provides Room database and DAOs
   - **RepositoryModule**: Binds repository implementations

### Key Architectural Decisions

- **Single Activity Architecture**: MainActivity hosts all screens via Compose Navigation
- **Reactive Data Flow**: Kotlin Flows from Room → Repository → ViewModel → UI
- **State Management**: ViewModels hold UI state as StateFlow/MutableStateFlow
- **Dependency Injection**: Hilt for compile-time safe DI
- **Database**: Room with migrations disabled (dev mode)

### Navigation Structure

Bottom navigation with 5 main screens:
- ExpenseList (start destination)
- AddExpense
- Analytics
- AIChat (placeholder)
- RecurringExpenses (placeholder)

Additional screens:
- EditExpense (with expenseId argument)
- Settings (placeholder)

## Database Schema

### Tables
1. **expenses**: Main expense records
2. **categories**: Expense categories
3. **recurring_expenses**: Recurring expense templates

### Type Converters
- Date/Time conversion (LocalDateTime)
- List<String> for tags
- Custom enum conversions

## Testing Strategy

### Android Test Runner Agent
When running Android tests, Claude Code will automatically use the `android-test-runner` agent which provides:
- Specialized Android testing expertise
- Proper Gradle command execution for unit and instrumented tests
- ADB integration for device/emulator testing
- Test result analysis and debugging assistance
- Coverage report generation and analysis

The agent will be triggered automatically when:
- Running `./gradlew test` or similar test commands
- Implementing new Android features that require testing
- Debugging test failures or analyzing test results
- Setting up continuous integration for Android tests

### Unit Tests (`test/`)
- ViewModels: Test state management and business logic
- Repository: Mock DAO interactions
- Converters: Data transformation logic
- Validation: Form validation and data integrity

### Instrumented Tests (`androidTest/`)
- DAOs: Database operations
- UI Tests: Screen interactions and navigation
- Integration: Full flow testing

## Important Notes

- Database uses `fallbackToDestructiveMigration()` - implement proper migrations for production
- MinSDK is 33 (Android 13) - relatively high, consider lowering for wider device support
- Placeholder screens exist for AI Chat, Recurring Expenses, and Settings
- Charts use Vico library - refer to documentation for customization

## Steering Documents

### Product Vision and Strategy
@.kiro/steering/product.md

### Technical Architecture
@.kiro/steering/tech.md

### Project Structure
@.kiro/steering/structure.md

### Test-Driven Development Guidelines
@.kiro/steering/tdd.md

### Development Workflow
@.kiro/steering/development-workflow.md

## Project Specifications

### Requirements
@.kiro/specs/expense-tracker/requirements.md

### Design Documentation
@.kiro/specs/expense-tracker/design.md

### Task Planning
@.kiro/specs/expense-tracker/tasks.md
## Best Practices
- Use simple straigtforward approaches, don't over-engineer things