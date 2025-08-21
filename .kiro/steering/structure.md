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
  - **DatabaseMigrations.kt**: Migration strategy (CRITICAL: Never use fallbackToDestructiveMigration)
- **converter/**: Room type converters for complex data types
- **repository/**: Repository implementations with business logic

### Domain Layer (`domain/`) - **Added from Review-Fixes Learnings**
- **validation/**: Centralized validation architecture
  - **ValidationResult.kt**: Sealed class for type-safe validation results
  - **Validator.kt**: Generic validation interface
  - **AmountValidator.kt**: Amount validation with business rules
  - **DescriptionValidator.kt**: Description validation and sanitization
  - **TagValidator.kt**: Tag validation and filtering
  - **CategoryValidator.kt**: Category validation and constraints
- **error/**: Type-safe error handling
  - **UserFacingError.kt**: Sealed class for user-facing error messages

### Dependency Injection (`di/`)
- **DatabaseModule.kt**: Hilt modules for database dependencies
- **RepositoryModule.kt**: Repository binding modules
- **ValidationModule.kt**: Validator dependency injection

### UI Layer (`ui/`)
- **theme/**: Compose theme configuration (Color, Theme, Type)
- **screens/**: Main application screens with ViewModels
- **components/**: Reusable UI components
- **navigation/**: Navigation configuration with deep linking
- **util/**: UI utilities and performance helpers
  - **PerformanceUtils.kt**: Compose performance monitoring and optimization

## Package Naming Convention
- Base package: `com.expensetracker`
- Follow standard Android package structure
- Group by feature/layer, not by type

## File Organization Patterns
- **Entities**: Data classes with Room annotations and validation
- **DAOs**: Suspend functions for async database operations
- **Database**: Singleton pattern with Room database setup
- **Converters**: Type converters for List<String>, BigDecimal, LocalDateTime

## Testing Structure (Enhanced from Review-Fixes Learnings)
- **Unit tests**: `app/src/test/` - Mirror main package structure
  - **domain/validation/**: Comprehensive validator testing (100+ test cases each)
  - **ui/screens/**: ViewModel testing (40+ test cases for complex ViewModels)
  - **data/repository/**: Repository testing with in-memory database
  - **test/rules/**: Custom JUnit rules for test infrastructure
    - **CoroutineTestRule.kt**: Test dispatcher management
  - **test/fixtures/**: Test data builders and factories
- **Integration tests**: `app/src/androidTest/` - Focus on database and UI testing
  - **data/database/**: Database migration testing
  - **ui/**: Compose UI testing with user interactions
- **Test naming**: `ClassNameTest.kt` convention
- **Test organization**: Use nested inner classes for method grouping
- **Production readiness**: Minimum 300+ total tests for deployment

## Resource Organization (`app/src/main/res/`) - **Enhanced for I18n**
- **drawable/**: Vector drawables and icons
- **mipmap-*/**: App launcher icons
- **values/**: Strings, themes, and configuration
  - **strings.xml**: Organized by category (navigation, forms, errors, status)
- **values-{lang}/**: Localization files for different languages
- **xml/**: Backup rules, data extraction rules, and localization configuration
  - **locales_config.xml**: Supported languages configuration

## Architecture Layers (Refined from Review-Fixes Experience)
1. **Presentation Layer**: 
   - Compose UI components with performance optimization
   - ViewModels with event-driven state management
   - Multiple preview states for comprehensive UI coverage
2. **Domain Layer**: 
   - Centralized validation architecture
   - Type-safe error handling
   - Business logic and use cases
3. **Data Layer**: 
   - Repository pattern with proper abstractions
   - Room database with migration strategy
   - Reactive data flows with Flow/StateFlow

## Implementation Guidelines (From Review-Fixes Success)

### Required Components for New Features
1. **Database Changes**: Always include DatabaseMigrations.kt updates
2. **Validation**: Create dedicated validator classes in domain/validation/
3. **State Management**: Use event-driven ViewModel pattern
4. **Error Handling**: Implement UserFacingError classes
5. **Testing**: Comprehensive test coverage (ViewModel 40+, Validation 100+)
6. **I18n**: Externalize all user-facing strings
7. **Performance**: Apply memoization for expensive calculations
8. **Navigation**: Include deep linking for primary screens

### Architectural Decisions Record
- **Database Migration**: Learned from destructive migration issue - always implement proper migrations
- **State Management**: Learned from race conditions - single event handler pattern prevents issues
- **Validation**: Learned from inline validation problems - centralized validation improves testability
- **Performance**: Learned from recomposition issues - memoization and stable callbacks essential
- **I18n**: Learned from hardcoded strings - externalization enables localization

This structure reflects the lessons learned from implementing and fixing 10 critical improvements that transformed the project from having serious architectural issues to being production-ready with 323 passing tests.