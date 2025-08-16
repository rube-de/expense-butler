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

### Coverage Requirements
- **Minimum coverage**: 80% for new code
- **Critical paths**: 95% coverage required
- **UI components**: Focus on business logic, not visual rendering

## TDD Workflow Integration

### Before Starting Any Task
1. ✅ Read requirements and understand expected behavior
2. ✅ Write failing tests that define the behavior
3. ✅ Implement minimal code to pass tests
4. ✅ Refactor and improve code quality
5. ✅ Ensure all tests pass before committing

### Code Review Checklist
- [ ] Tests written before implementation
- [ ] All tests pass
- [ ] Edge cases covered
- [ ] Mocks used appropriately
- [ ] Test names are descriptive
- [ ] No implementation details tested

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