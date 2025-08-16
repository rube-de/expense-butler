# Project Structure

## Root Level
- **app/**: Main application module
- **gradle/**: Gradle wrapper files
- **build.gradle.kts**: Root build configuration
- **settings.gradle.kts**: Project settings and module inclusion

## Application Structure (`app/src/main/java/com/expensetracker/`)

### Core Application
- **ExpenseTrackerApplication.kt**: Application class with Hilt setup
- **MainActivity.kt**: Single activity hosting Compose UI

### Data Layer (`data/`)
- **model/**: Entity classes (Expense, Category, RecurringExpense)
- **dao/**: Room DAO interfaces for database operations
- **database/**: Database configuration and initialization
- **converter/**: Room type converters for complex data types

### Dependency Injection (`di/`)
- **DatabaseModule.kt**: Hilt modules for database dependencies

### UI Layer (`ui/`)
- **theme/**: Compose theme configuration (Color, Theme, Type)

## Package Naming Convention
- Base package: `com.expensetracker`
- Follow standard Android package structure
- Group by feature/layer, not by type

## File Organization Patterns
- **Entities**: Data classes with Room annotations and validation
- **DAOs**: Suspend functions for async database operations
- **Database**: Singleton pattern with Room database setup
- **Converters**: Type converters for List<String>, BigDecimal, LocalDateTime

## Testing Structure
- **Unit tests**: `app/src/test/` - Mirror main package structure
- **Integration tests**: `app/src/androidTest/` - Focus on database and UI testing
- **Test naming**: `ClassNameTest.kt` convention

## Resource Organization (`app/src/main/res/`)
- **drawable/**: Vector drawables and icons
- **mipmap-*/**: App launcher icons
- **values/**: Strings, themes, and configuration
- **xml/**: Backup rules and data extraction rules

## Architecture Layers
1. **Presentation**: Compose UI components and ViewModels
2. **Domain**: Business logic and use cases (to be implemented)
3. **Data**: Repository pattern with Room database and DAOs