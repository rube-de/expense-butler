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

## Testing Stack
- **Unit Tests**: JUnit 4, Mockito, Coroutines Test
- **Android Tests**: Espresso, Compose UI Testing
- **Room Testing**: Room testing library
- **Architecture Testing**: Core testing library

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
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run all tests
./gradlew check
```

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