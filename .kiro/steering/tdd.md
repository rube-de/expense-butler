# Test-Driven Development (TDD) Guidelines

## Overview

This project follows Test-Driven Development (TDD) principles to ensure high code quality, maintainable architecture, and reliable functionality. All new features and components must be developed using the Red-Green-Refactor cycle.

## TDD Cycle: Red-Green-Refactor

### 🔴 RED - Write Failing Test First
1. **Define behavior before implementation**
   - Write a test that describes the expected functionality
   - Test should fail initially (no implementation exists yet)
   - Focus on the interface and expected outcomes

2. **Test naming convention**
   ```kotlin
   @Test
   fun `should [expected behavior] when [condition]`() {
       // Test implementation
   }
   ```

3. **Example - Red Phase**
   ```kotlin
   @Test
   fun `should save expense to database when valid expense provided`() {
       // Arrange
       val expense = createValidExpense()
       
       // Act
       val result = repository.saveExpense(expense)
       
       // Assert
       assertTrue(result.isSuccess)
       assertEquals(expense.id, result.getOrNull()?.id)
   }
   ```

### 🟢 GREEN - Write Minimal Code to Pass
1. **Implement only what's needed**
   - Write the simplest code that makes the test pass
   - Don't over-engineer or add unnecessary features
   - Focus on making the test green, not perfect code

2. **Example - Green Phase**
   ```kotlin
   class ExpenseRepository {
       fun saveExpense(expense: Expense): Result<Expense> {
           // Minimal implementation to pass test
           return Result.success(expense)
       }
   }
   ```

### 🔄 REFACTOR - Improve Code Quality
1. **Enhance without breaking tests**
   - Improve code structure, readability, and performance
   - Extract common functionality
   - Apply design patterns where appropriate
   - All tests must remain green

2. **Example - Refactor Phase**
   ```kotlin
   class ExpenseRepository @Inject constructor(
       private val dao: ExpenseDao,
       private val validator: ExpenseValidator
   ) {
       suspend fun saveExpense(expense: Expense): Result<Expense> {
           return try {
               validator.validate(expense)
               val savedExpense = dao.insert(expense)
               Result.success(savedExpense)
           } catch (e: ValidationException) {
               Result.failure(e)
           }
       }
   }
   ```

## Testing Strategy by Layer

### UI Components (Compose)
- **Unit Tests**: Business logic, validation, formatting
- **Integration Tests**: Component interactions
- **UI Tests**: User interactions and visual behavior

```kotlin
// Unit test for validation logic
@Test
fun `should validate amount correctly`() {
    val validator = AmountValidator()
    val error = validator.validate(BigDecimal.ZERO)
    assertEquals("Amount must be greater than 0", error)
}

// UI test for component behavior
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

### ViewModels
- **Test state management and business logic**
- **Mock dependencies (Repository, Services)**
- **Verify UI state updates**

```kotlin
@Test
fun `should update expense list when new expense added`() {
    // Arrange
    val mockRepository = mockk<ExpenseRepository>()
    val viewModel = ExpenseViewModel(mockRepository)
    
    // Act
    viewModel.addExpense(createTestExpense())
    
    // Assert
    verify { mockRepository.saveExpense(any()) }
    assertTrue(viewModel.uiState.value.expenses.isNotEmpty())
}
```

### Repository Layer
- **Test data operations with in-memory database**
- **Test error handling and edge cases**
- **Verify data transformations**

```kotlin
@Test
fun `should return expenses filtered by category`() = runTest {
    // Arrange
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository = ExpenseRepositoryImpl(database.expenseDao())
    
    // Act
    val expenses = repository.getExpensesByCategory(categoryId = 1L)
    
    // Assert
    expenses.test {
        val result = awaitItem()
        assertTrue(result.all { it.categoryId == 1L })
    }
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

### 1. Start with the Simplest Test
```kotlin
@Test
fun `should create expense with valid data`() {
    val expense = Expense(
        amount = BigDecimal("10.00"),
        currency = "USD",
        description = "Test expense",
        categoryId = 1L,
        tags = emptyList(),
        date = LocalDateTime.now()
    )
    
    assertNotNull(expense)
    assertEquals(BigDecimal("10.00"), expense.amount)
}
```

### 2. Test Edge Cases and Error Conditions
```kotlin
@Test
fun `should throw exception when amount is negative`() {
    assertThrows<IllegalArgumentException> {
        Expense(
            amount = BigDecimal("-10.00"),
            currency = "USD",
            description = "Invalid expense",
            categoryId = 1L,
            tags = emptyList(),
            date = LocalDateTime.now()
        )
    }
}
```

### 3. Use Test Data Builders
```kotlin
class ExpenseTestDataBuilder {
    private var amount = BigDecimal("10.00")
    private var currency = "USD"
    private var description = "Test expense"
    
    fun withAmount(amount: BigDecimal) = apply { this.amount = amount }
    fun withCurrency(currency: String) = apply { this.currency = currency }
    fun withDescription(description: String) = apply { this.description = description }
    
    fun build() = Expense(
        amount = amount,
        currency = currency,
        description = description,
        categoryId = 1L,
        tags = emptyList(),
        date = LocalDateTime.now()
    )
}

// Usage
val expense = ExpenseTestDataBuilder()
    .withAmount(BigDecimal("25.50"))
    .withDescription("Coffee")
    .build()
```

### 4. Keep Tests Independent
- Each test should be able to run in isolation
- Use `@Before` and `@After` for setup/cleanup
- Don't rely on test execution order

### 5. Test Behavior, Not Implementation
```kotlin
// ❌ Testing implementation details
@Test
fun `should call database insert method`() {
    repository.saveExpense(expense)
    verify { dao.insert(expense) }
}

// ✅ Testing behavior
@Test
fun `should persist expense and return success`() {
    val result = repository.saveExpense(expense)
    assertTrue(result.isSuccess)
    
    val savedExpense = repository.getExpense(expense.id)
    assertEquals(expense, savedExpense)
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

### Coverage Requirements (Enhanced from Review-Fixes Learnings)
- **Minimum coverage**: 80% for new code overall
- **Critical paths**: 95% coverage required for business logic
- **UI components**: Focus on business logic, not visual rendering
- **ViewModel Testing**: Minimum 40+ test cases for complex ViewModels
- **Validation Testing**: 100+ test cases covering all edge cases and boundaries
- **Production Readiness**: 300+ total tests for production deployment
- **Race Condition Testing**: Test concurrent state updates and event handling

## TDD Workflow Integration

### Before Starting Any Task
1. ✅ Read requirements and understand expected behavior
2. ✅ Write failing tests that define the behavior
3. ✅ Implement minimal code to pass tests
4. ✅ Refactor and improve code quality
5. ✅ Ensure all tests pass before committing

### Code Review Checklist (Enhanced with Review-Fixes Requirements)
- [ ] Tests written before implementation
- [ ] All tests pass
- [ ] Edge cases covered
- [ ] Mocks used appropriately
- [ ] Test names are descriptive
- [ ] No implementation details tested
- [ ] **ViewModel Coverage**: Complex ViewModels have 40+ test cases
- [ ] **Validation Coverage**: All validators have comprehensive test suites
- [ ] **Race Condition Coverage**: State management tested for concurrency issues
- [ ] **Event Handling**: All UI events tested with proper state verification
- [ ] **Error Scenarios**: Both success and failure paths tested thoroughly

## Comprehensive Testing Patterns (From Review-Fixes Success)

### ViewModel Testing Requirements

Based on the successful AddExpenseViewModel implementation with 43 test cases:

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

    // Organize tests with nested classes for clarity
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

    @Nested
    inner class ValidationIntegration {
        @Test
        fun `should validate all fields before saving`() = runTest {
            // Test comprehensive validation
        }
    }
}
```

**Requirements for ViewModel Testing**:
- Minimum 40+ test cases for complex ViewModels
- Use nested test classes for organization
- Test all UI event handling paths
- Test both success and failure scenarios
- Test race conditions and concurrent state updates
- Mock all external dependencies
- Use `runTest` for coroutine testing

### Validation Testing Requirements

Based on the comprehensive validator testing with 100+ test cases:

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
        
        @Test
        fun `should return Invalid for negative amount`() {
            val result = validator.validate(BigDecimal("-10.00"))
            assertTrue(result is ValidationResult.Invalid)
        }
        
        @Test
        fun `should return Invalid for amount exceeding maximum`() {
            val result = validator.validate(BigDecimal("1000000000.00"))
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

**Requirements for Validation Testing**:
- Test all boundary conditions (null, zero, negative, maximum)
- Test input filtering and sanitization
- Test error message generation
- Use nested classes for method grouping
- Achieve 100% coverage for validation logic

### Integration Testing Requirements

Based on successful repository testing:

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

**Requirements for Integration Testing**:
- Test database operations with in-memory Room database
- Test reactive data flows with Turbine
- Test data transformations and mapping
- Test error handling at integration boundaries

## Examples from Current Project

### ✅ Good TDD Example
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

// 3. REFACTOR - Handle multiple decimal points
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