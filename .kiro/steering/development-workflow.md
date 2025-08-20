# Development Workflow

## Overview

This document outlines the development workflow for the Expense Tracker project, emphasizing Test-Driven Development (TDD) and quality assurance practices.

## Task Execution Workflow

### 1. Task Analysis
- [ ] Read task requirements from `tasks.md`
- [ ] Review related requirements in `requirements.md`
- [ ] Check design specifications in `design.md`
- [ ] Identify dependencies and prerequisites

### 2. Test-First Development (TDD)
- [ ] **RED**: Write failing tests that define expected behavior
- [ ] **GREEN**: Implement minimal code to make tests pass
- [ ] **REFACTOR**: Improve code quality while keeping tests green
- [ ] Repeat cycle for each piece of functionality

### 3. Implementation Guidelines
- [ ] Follow architecture patterns (MVVM, Repository)
- [ ] Use dependency injection (Hilt)
- [ ] Implement proper error handling
- [ ] Follow Material Design 3 principles
- [ ] Ensure accessibility compliance
- [ ] Apply code quality principles (KISS, High Cohesion/Low Coupling, POLA) - See [Coding Principles](coding-principles.md)

### 4. Quality Assurance
- [ ] Run unit tests: `./gradlew testDebugUnitTest`
- [ ] Run integration tests: `./gradlew connectedAndroidTest`
- [ ] Verify build: `./gradlew assembleDebug`
- [ ] Check code coverage meets requirements (80%+)

### 5. Task Completion
- [ ] Update task status to completed
- [ ] Verify all acceptance criteria met
- [ ] Document any architectural decisions
- [ ] Prepare for next task

## TDD Integration

### Before Writing Any Code
1. **Understand the requirement**
   ```kotlin
   // Example: "User should be able to add expense with validation"
   ```

2. **Write failing test first**
   ```kotlin
   @Test
   fun `should save expense when valid data provided`() {
       val expense = createValidExpense()
       val result = repository.saveExpense(expense)
       assertTrue(result.isSuccess)
   }
   ```

3. **Run test to confirm it fails**
   ```bash
   ./gradlew testDebugUnitTest --tests "ExpenseRepositoryTest"
   ```

4. **Implement minimal code**
   ```kotlin
   fun saveExpense(expense: Expense): Result<Expense> {
       return Result.success(expense) // Minimal implementation
   }
   ```

5. **Run test to confirm it passes**
   ```bash
   ./gradlew testDebugUnitTest --tests "ExpenseRepositoryTest"
   ```

6. **Refactor and improve**
   ```kotlin
   suspend fun saveExpense(expense: Expense): Result<Expense> {
       return try {
           validateExpense(expense)
           val saved = dao.insert(expense)
           Result.success(saved)
       } catch (e: Exception) {
           Result.failure(e)
       }
   }
   ```

## Code Review Checklist

### TDD Compliance
- [ ] Tests written before implementation
- [ ] All tests pass
- [ ] Test names are descriptive and use backticks
- [ ] Edge cases and error conditions tested
- [ ] No implementation details tested (test behavior, not internals)

### Code Quality
- [ ] Follows project architecture patterns
- [ ] Proper error handling implemented
- [ ] Dependencies injected correctly
- [ ] Code is readable and well-documented
- [ ] No hardcoded values or magic numbers

### Android Specific
- [ ] Compose components follow Material Design 3
- [ ] Proper lifecycle management
- [ ] Accessibility content descriptions provided
- [ ] Resources externalized (strings, dimensions, colors)
- [ ] Performance considerations addressed

## Testing Strategy by Component Type

### UI Components
```kotlin
// Unit test for business logic
@Test
fun `should validate amount input correctly`() {
    val validator = AmountValidator()
    val result = validator.validate(BigDecimal("25.50"))
    assertTrue(result.isValid)
}

// Integration test for component behavior
@Test
fun `should display error when invalid amount entered`() {
    composeTestRule.setContent {
        AmountInput(isError = true, errorMessage = "Invalid amount")
    }
    composeTestRule.onNodeWithText("Invalid amount").assertIsDisplayed()
}
```

### ViewModels
```kotlin
@Test
fun `should update UI state when expense added`() = runTest {
    val viewModel = ExpenseViewModel(mockRepository)
    
    viewModel.addExpense(createTestExpense())
    
    val state = viewModel.uiState.value
    assertTrue(state.isLoading.not())
    assertTrue(state.expenses.isNotEmpty())
}
```

### Repository Layer
```kotlin
@Test
fun `should persist expense to database`() = runTest {
    val expense = createTestExpense()
    
    repository.saveExpense(expense)
    
    val saved = repository.getExpense(expense.id)
    assertEquals(expense, saved)
}
```

## Continuous Integration

### Local Development
```bash
# Before committing
./gradlew testDebugUnitTest
./gradlew connectedAndroidTest
./gradlew assembleDebug
./gradlew lint
```

### Coverage Requirements
- **New code**: Minimum 80% test coverage
- **Critical business logic**: 95% coverage
- **UI components**: Focus on business logic, not visual rendering

## Common Patterns

### Test Data Builders
```kotlin
class ExpenseTestDataBuilder {
    private var amount = BigDecimal("10.00")
    private var description = "Test expense"
    
    fun withAmount(amount: BigDecimal) = apply { this.amount = amount }
    fun withDescription(desc: String) = apply { this.description = desc }
    
    fun build() = Expense(
        amount = amount,
        description = description,
        // ... other fields
    )
}
```

### Mock Setup
```kotlin
@Before
fun setup() {
    mockRepository = mockk<ExpenseRepository>()
    every { mockRepository.saveExpense(any()) } returns Result.success(mockExpense)
}
```

### Coroutine Testing
```kotlin
@Test
fun `should handle async operations`() = runTest {
    val result = repository.getExpensesAsync()
    
    result.test {
        val expenses = awaitItem()
        assertTrue(expenses.isNotEmpty())
    }
}
```

## Documentation References

- **Coding Principles**: [coding-principles.md](coding-principles.md) - Code quality principles (KISS, High Cohesion/Low Coupling, POLA)
- **TDD Guidelines**: [tdd.md](tdd.md) - Comprehensive TDD practices
- **Technology Stack**: [tech.md](tech.md) - Tools and libraries
- **Project Structure**: [structure.md](structure.md) - Code organization
- **Product Requirements**: [product.md](product.md) - Business context

## Success Metrics

### Development Quality
- All tests pass before task completion
- Code coverage meets minimum requirements
- No lint warnings or errors
- Build succeeds on first attempt

### TDD Adherence
- Tests written before implementation
- Red-Green-Refactor cycle followed
- Test names clearly describe behavior
- Edge cases and error conditions covered

This workflow ensures consistent, high-quality development while maintaining the benefits of Test-Driven Development throughout the project lifecycle.