# Technology Stack

## Build System
- **Gradle** with Kotlin DSL (`.gradle.kts` files)
- **Android Gradle Plugin** 8.11.1
- **Kotlin** 2.0.21

## Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Room with SQLite
- **Navigation**: Navigation Compose
- **Async**: Kotlin Coroutines

## Key Libraries
- **Jetpack Compose BOM**: 2023.10.01
- **Room**: 2.6.1 (database ORM)
- **Hilt**: 2.52 (dependency injection)
- **Navigation Compose**: 2.7.5
- **Kotlinx DateTime**: 0.5.0 (date/time handling)
- **Kotlinx Serialization**: 1.6.2 (JSON serialization)
- **Vico**: 1.13.1 (charts and data visualization)

## Architecture Requirements (Based on Review-Fixes Learnings)

### Database Architecture
- **Migration Strategy**: Always implement proper Room migrations using `DatabaseMigrations` object
- **Performance**: Include database indices in migrations for frequently queried columns
- **NEVER**: Use `.fallbackToDestructiveMigration()` in production builds
- **Testing**: Use in-memory Room database for integration tests

### State Management Architecture  
- **Pattern**: Event-driven architecture with sealed class events
- **ViewModel Structure**: Single `onUiEvent()` function for all state updates
- **State Flow**: Use `StateFlow` with `update()` for thread-safe mutations
- **AVOID**: Multiple `viewModelScope.launch` blocks in ViewModels

### Validation Architecture
- **Pattern**: Centralized validation with dedicated validator classes
- **Structure**: `domain/validation/` package with `ValidationResult` sealed class
- **Dependency Injection**: Provide validators through Hilt modules
- **AVOID**: Inline validation logic in UI components

### Performance Architecture
- **Compose Optimization**: Use `remember`, `derivedStateOf`, and stable callbacks
- **Monitoring**: Include `PerformanceUtils` with composition logging
- **Memoization**: Pre-calculate expensive operations in `remember` blocks
- **Recomposition**: Monitor and minimize unnecessary recompositions

### Internationalization Architecture
- **String Resources**: Organize by category (navigation, forms, errors, status)
- **Error Handling**: Type-safe error classes with `UserFacingError` sealed class
- **Localization**: Use `stringResource()` throughout UI components
- **Configuration**: Set up `res/xml/locales_config.xml` for language support

## Testing Stack

### Core Testing Frameworks
- **JUnit 5**: Modern testing framework with JUnit 4 compatibility via Vintage Engine
- **JUnit 4**: Maintained for Android instrumented tests and legacy compatibility
- **Coroutines Test**: `kotlinx-coroutines-test` for testing suspend functions and Flows
- **Turbine**: Flow testing library for clean async assertions

### Assertion Libraries
- **Google Truth**: Fluent assertions with better error messages (`assertThat(value).isEqualTo(expected)`)
- **Kotest Assertions**: Kotlin-first assertion library with property testing support
- **JUnit Assertions**: Basic assertions for simple test cases

### Mocking Frameworks
- **MockK**: Kotlin-first mocking library with coroutine support (primary)
- **Mockito**: Java mocking library (maintained for legacy tests)
- **Fake Implementations**: Custom test doubles in `test/fakes/` directory

### Android Testing
- **Espresso**: UI testing framework for Android views
- **Compose UI Testing**: Testing framework for Jetpack Compose components
- **Room Testing**: In-memory database testing
- **Hilt Testing**: Dependency injection testing support
- **Architecture Components Testing**: LiveData and ViewModel testing utilities

### Test Infrastructure
- **Test Fixtures**: Builders and factories in `test/fixtures/` for consistent test data
- **Test Rules**: Custom JUnit rules in `test/rules/` for common setup
  - `CoroutineTestRule`: Manages test dispatchers for coroutine testing
  - `InstantTaskExecutorRule`: Synchronous execution for Architecture Components
  - `MockKRule`: Automatic mock cleanup between tests
- **Fake Repository**: `FakeExpenseRepository` for controlled testing scenarios

### Testing Best Practices
- **Test-Driven Development (TDD)**: Red-Green-Refactor cycle - See [TDD Guidelines](tdd.md)
- **Testing Pyramid**: More unit tests, fewer integration tests, minimal UI tests
- **Test Naming**: Descriptive names using backticks or `@DisplayName`
- **Test Isolation**: Each test runs independently with proper setup/teardown
- **Parameterized Tests**: JUnit 5's `@ParameterizedTest` for data-driven testing
- **Critical Requirements (From review-fixes.md)**:
  - **ViewModel Testing**: Minimum 40+ test cases for complex ViewModels
  - **Validation Testing**: 100+ test cases covering all edge cases and boundaries
  - **Race Condition Testing**: Test concurrent state updates and event handling
  - **Integration Testing**: 323+ total tests for production readiness

## Common Commands

### Build & Run
```bash
# Build debug APK
./gradlew assembleDebug

# Install and run on device/emulator
./gradlew installDebug

# Build release APK
./gradlew assembleRelease
```

### Testing
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests
./gradlew connectedAndroidTest

# Run all tests with coverage
./gradlew testDebugUnitTestCoverage

# Run specific test class
./gradlew testDebugUnitTest --tests "ExpenseRepositoryTest"

# Run all tests
./gradlew check
```

**TDD Workflow**: Always write tests first following Red-Green-Refactor cycle. See [TDD Guidelines](tdd.md) for detailed practices.

### Code Quality
```bash
# Clean build
./gradlew clean

# Lint check
./gradlew lint
```

## Development Requirements
- **Java Version**: 1.8 (source/target compatibility)
- **Compile SDK**: 34
- **Kotlin Compiler Extension**: 1.5.15

## Documentation Access
When working with Android libraries or frameworks, use the MCP documentation tools for up-to-date information:

### Getting Library Documentation
1. **Resolve Library ID**: Use `mcp_docker_resolve_library_id` to find the correct library identifier
   - Example: Search for "jetpack compose", "room database", "hilt", etc.
2. **Get Documentation**: Use `mcp_docker_get_library_docs` with the resolved library ID
   - Provides current API documentation, examples, and best practices

### Common Library Searches
- Android Jetpack libraries (Compose, Room, Navigation, etc.)
- Kotlin coroutines and serialization
- Hilt dependency injection
- Material Design 3 components
- Testing frameworks (JUnit, Espresso, Mockito)

This ensures access to the most current documentation and examples for all project dependencies.