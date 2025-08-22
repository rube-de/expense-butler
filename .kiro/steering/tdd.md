# Test-Driven Development (TDD) Guidelines

## Overview

This project follows Test-Driven Development (TDD) principles to ensure high code quality, maintainable architecture, and reliable functionality. All new features must be developed using the Red-Green-Refactor cycle.

## Table of Contents

1. [TDD Cycle: Red-Green-Refactor](#tdd-cycle-red-green-refactor)
2. [Testing Strategy by Layer](#testing-strategy-by-layer)
3. [Coverage Requirements](#coverage-requirements)
4. [Testing Tools and Dependencies](#testing-tools-and-dependencies)
5. [TDD Best Practices](#tdd-best-practices)
6. [Code Review Checklist](#code-review-checklist)
7. [TDD Workflow](#tdd-workflow)

## TDD Cycle: Red-Green-Refactor

### 🔴 RED - Write Failing Test First
- Write a test that describes the expected functionality
- Test should fail initially (no implementation exists yet)
- Focus on the interface and expected outcomes
- Use naming convention: `fun should [expected behavior] when [condition]`()

### 🟢 GREEN - Write Minimal Code to Pass
- Write the simplest code that makes the test pass
- Don't over-engineer or add unnecessary features
- Focus on making the test green, not perfect code

### 🔄 REFACTOR - Improve Code Quality
- Improve code structure, readability, and performance
- Extract common functionality and apply design patterns
- All tests must remain green throughout refactoring

## Testing Strategy by Layer

### ViewModel Testing
Test state management, business logic, and UI event handling with comprehensive coverage.

**Requirements:**
- Minimum 40+ test cases for complex ViewModels
- Use nested test classes for organization
- Test all UI event handling paths and race conditions
- Mock all external dependencies
- Use `runTest` for coroutine testing

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var mockRepository: ExpenseRepository
    private lateinit var viewModel: AddExpenseViewModel

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        viewModel = AddExpenseViewModel(
            repository = mockRepository,
            amountValidator = AmountValidator(),
            descriptionValidator = DescriptionValidator(),
            categoryValidator = CategoryValidator(),
            tagValidator = TagValidator()
        )
    }

    @Nested
    inner class AmountHandling {
        @Test
        fun `should update amount when valid amount provided`() = runTest {
            viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("25.50")))
            assertEquals(BigDecimal("25.50"), viewModel.uiState.value.amount)
        }
        
        @Test
        fun `should clear amount error when new amount provided`() = runTest {
            // Test error clearing behavior
        }
    }

    @Nested
    inner class StateManagement {
        @Test
        fun `should handle concurrent events without race conditions`() = runTest {
            // Test multiple simultaneous events
        }
    }
}
```

### Validation Testing
Test all boundary conditions and edge cases with comprehensive coverage.

**Requirements:**
- 100+ test cases covering all edge cases and boundaries
- Test input filtering and sanitization
- Use nested classes for method grouping
- Achieve 100% coverage for validation logic

```kotlin
class AmountValidatorTest {
    private val validator = AmountValidator()

    @Nested
    inner class ValidateMethod {
        @Test
        fun `should return Valid for positive amount`() {
            val result = validator.validate(BigDecimal("25.50"))
            assertEquals(ValidationResult.Valid, result)
        }
        
        @Test
        fun `should return Invalid for null amount`() {
            val result = validator.validate(null)
            assertTrue(result is ValidationResult.Invalid)
        }
        
        @Test
        fun `should return Invalid for zero amount`() {
            val result = validator.validate(BigDecimal.ZERO)
            assertTrue(result is ValidationResult.Invalid)
        }
    }

    @Nested
    inner class FilterAmountInputMethod {
        @Test
        fun `should filter out non-numeric characters`() {
            val result = validator.filterAmountInput("abc123.45def")
            assertEquals("123.45", result)
        }
    }
}
```

### Repository and Integration Testing
Test database operations with in-memory database and reactive data flows.

**Requirements:**
- Test database operations with in-memory Room database
- Test reactive data flows with Turbine
- Test error handling at integration boundaries

```kotlin
@Test
fun `should persist expense and update state reactively`() = runTest {
    // Arrange
    val expense = createTestExpense()
    
    // Act
    repository.saveExpense(expense)
    
    // Assert
    repository.getAllExpenses().test {
        val expenses = awaitItem()
        assertTrue(expenses.contains(expense))
    }
}
```

### UI Component Testing
Test user interactions and visual behavior with Compose testing framework.

```kotlin
@Test
fun `should display error when invalid amount entered`() {
    composeTestRule.setContent {
        AmountInput(
            amount = BigDecimal.ZERO,
            onAmountChanged = { },
            isError = true,
            errorMessage = "Amount must be greater than 0"
        )
    }
    
    composeTestRule
        .onNodeWithText("Amount must be greater than 0")
        .assertIsDisplayed()
}
```

## Test Organization

### Directory Structure
```
app/src/
├── test/java/                          # Unit tests
│   └── com/expensetracker/
│       ├── ui/components/              # Component logic tests
│       ├── data/repository/            # Repository tests
│       └── domain/usecase/             # Use case tests
├── androidTest/java/                   # Integration & UI tests
│   └── com/expensetracker/
│       ├── ui/                         # Compose UI tests
│       ├── data/database/              # Database tests
│       └── integration/                # End-to-end tests
```

### Test Naming Conventions
- **Test classes**: `[ClassName]Test.kt`
- **Test methods**: Use backticks for descriptive names
- **Test data**: `create[Entity]()` helper functions
- **Mocks**: `mock[Dependency]` naming

## Coverage Requirements

### Overall Coverage Targets
- **Minimum coverage**: 80% for new code overall
- **Critical paths**: 95% coverage required for business logic
- **Production Readiness**: 300+ total tests for production deployment

### Component-Specific Requirements
| Component Type | Minimum Tests | Coverage Target | Key Requirements |
|---------------|---------------|-----------------|------------------|
| **ViewModel** | 40+ test cases | 95% | Event handling, state management, race conditions |
| **Validation** | 100+ test cases | 100% | All boundary conditions, edge cases, sanitization |
| **Repository** | 20+ test cases | 90% | Database operations, reactive flows, error handling |
| **UI Components** | 10+ test cases | 80% | User interactions, visual behavior (not rendering) |

## Testing Tools and Dependencies

### Unit Testing
```kotlin
// JUnit 5
testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

// Mockk for mocking
testImplementation("io.mockk:mockk:1.13.4")

// Coroutines testing
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

// Turbine for Flow testing
testImplementation("app.cash.turbine:turbine:0.12.1")
```

### Android Testing
```kotlin
// Compose UI testing
androidTestImplementation("androidx.compose.ui:ui-test-junit4")

// Room testing
androidTestImplementation("androidx.room:room-testing:2.6.1")

// Hilt testing
androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
```

## TDD Best Practices

### Essential Principles
1. **Start with simplest tests** - Begin with basic functionality before complex edge cases
2. **Test behavior, not implementation** - Verify outcomes, not internal method calls
3. **Keep tests independent** - Each test runs in isolation with proper setup/cleanup
4. **Use descriptive names** - Test names should clearly describe expected behavior
5. **Test edge cases** - Include boundary conditions, null values, and error scenarios

### Test Organization
- Use **nested classes** to group related tests by functionality
- Create **test data builders** for complex object construction
- Apply **Arrange-Act-Assert** pattern consistently
- Use **`@Before` and `@After`** for setup and cleanup

### Example: Test Data Builder Pattern
```kotlin
class ExpenseTestDataBuilder {
    private var amount = BigDecimal("10.00")
    private var currency = "USD"
    
    fun withAmount(amount: BigDecimal) = apply { this.amount = amount }
    fun withCurrency(currency: String) = apply { this.currency = currency }
    
    fun build() = Expense(amount = amount, currency = currency, ...)
}
```

## Continuous Integration

### Test Execution Commands
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run all instrumented tests
./gradlew connectedAndroidTest

# Run all tests with coverage
./gradlew testDebugUnitTestCoverage

# Run specific test class
./gradlew testDebugUnitTest --tests "ExpenseRepositoryTest"
```


## Code Review Checklist

### TDD Compliance
- [ ] Tests written before implementation (Red-Green-Refactor cycle followed)
- [ ] All tests pass and provide meaningful coverage
- [ ] Edge cases and error conditions covered
- [ ] Test names clearly describe expected behavior
- [ ] Tests verify behavior, not implementation details

### Component-Specific Requirements
- [ ] **ViewModel**: Event-driven pattern with 40+ test cases for complex ViewModels
- [ ] **Validation**: Comprehensive test suites with 100+ test cases for validators
- [ ] **Repository**: Database operations tested with in-memory Room database
- [ ] **UI Components**: User interactions and visual behavior tested

### Quality Assurance
- [ ] Mocks used appropriately (external dependencies only)
- [ ] Test data builders used for complex object construction
- [ ] Tests organized with nested classes for clarity
- [ ] Race conditions and concurrent state updates tested

## TDD Workflow

### Development Process
1. **Read requirements** and understand expected behavior
2. **Write failing test** that defines the behavior (RED)
3. **Implement minimal code** to pass the test (GREEN)
4. **Refactor and improve** code quality (REFACTOR)
5. **Ensure all tests pass** before committing

### Example: Red-Green-Refactor Cycle
```kotlin
// 1. RED - Test written first
@Test
fun `should filter amount input to remove invalid characters`() {
    val filtered = filterAmountInput("abc123.45def")
    assertEquals("123.45", filtered)
}

// 2. GREEN - Minimal implementation
private fun filterAmountInput(input: String): String {
    return input.filter { it.isDigit() || it == '.' }
}

// 3. REFACTOR - Handle edge cases
private fun filterAmountInput(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val decimalCount = filtered.count { it == '.' }
    return if (decimalCount <= 1) {
        filtered
    } else {
        val firstDecimalIndex = filtered.indexOf('.')
        filtered.substring(0, firstDecimalIndex + 1) + 
        filtered.substring(firstDecimalIndex + 1).replace(".", "")
    }
}
```

This TDD approach ensures reliable, maintainable code and catches issues early in the development process.