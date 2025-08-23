# Code Quality Principles

## Overview

This document outlines the core principles that guide code quality and maintainability throughout the Expense Tracker project. These principles should be applied consistently across all development work to ensure clean, understandable, and maintainable code.

## KISS (Keep It Simple, Stupid)

### Core Principle
- Prefer simple, straightforward solutions over complex ones
- Remove unnecessary abstractions (e.g., abstract base classes with single implementation)
- Delete unused code rather than keeping it "just in case"
- Choose clarity over cleverness in implementation

### Practical Guidelines
- If you can't easily explain your solution, it's probably too complex
- Avoid creating abstractions until you have at least 3 concrete implementations
- Prefer composition over inheritance
- Use standard library functions instead of custom implementations when possible

### Example
```kotlin
// ❌ Over-engineered solution
abstract class BaseRepository<T> {
    abstract fun getData(): Flow<List<T>>
}
class ExpenseRepository : BaseRepository<Expense>()

// ✅ Simple, direct solution
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
}
```

## High Cohesion / Low Coupling

### Core Principle
- **High Cohesion**: Keep related functionality together in the same module
- **Low Coupling**: Minimize dependencies between modules
- Each module should have a single, well-defined purpose
- Avoid circular dependencies between modules
- Use dependency injection rather than hard-coded dependencies

### File and Module Organization
- **Maximum 300-400 lines per file** - Split larger files into focused modules
- **One primary class per file** (with related helper classes)
- **Group related functionality** in the same package/module
- **Data layer**: Contains entities, DAOs, database, and repositories
- **UI layer**: Contains ViewModels, Composables, and navigation
- **Domain layer**: Contains business logic and validation (when needed)
- Avoid direct dependencies between UI and Data layers (use Repository pattern)

### Refactoring Strategy
When a file becomes too large, refactor by:
1. **Identifying related functions** and grouping them into separate files
2. **Extracting utility functions** into dedicated utility files
3. **Splitting UI components** into smaller, focused Composables
4. **Separating concerns** (e.g., validation logic from UI logic)

### Example
```kotlin
// ✅ High cohesion: All expense-related operations in one place
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {
    fun getExpensesWithCategories(): Flow<List<ExpenseWithCategory>> = 
        expenseDao.getExpensesWithCategories()
    
    suspend fun saveExpense(expense: Expense): Result<Expense> = 
        // All expense saving logic here
}

// ✅ Low coupling: ViewModel depends on interface, not implementation
class AddExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository // Injected dependency
) : ViewModel() {
    // ViewModel doesn't know about DAO or database implementation
}
```

## POLA (Principle of Least Astonishment)

### Core Principle
- Code should behave as developers expect
- Follow established conventions and patterns
- Name functions and variables clearly to indicate their purpose
- Avoid surprising side effects in functions
- Make error conditions explicit and handle them predictably

### Naming Conventions
- Use descriptive names: `calculateMonthlyTotal()` not `calc()`
- Boolean variables: `isLoading`, `hasError`, `canEdit`
- Functions should be verbs: `saveExpense()`, `validateInput()`, `formatAmount()`
- Classes should be nouns: `ExpenseRepository`, `CategoryValidator`

### Error Handling
- Use `Result<T>` type for operations that can fail
- Make error states explicit in UI state classes
- Provide meaningful error messages to users
- Log technical details but show user-friendly messages

### Example
```kotlin
// ❌ Surprising behavior
fun updateExpense(expense: Expense) {
    categoryDao.deleteUnusedCategories() // Surprise!
    expenseDao.update(expense)
}

// ✅ Clear, expected behavior
suspend fun updateExpense(expense: Expense): Result<Unit> {
    return try {
        expenseDao.update(expense)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ✅ Clear naming when side effects are needed
suspend fun updateExpenseAndCleanupCategories(expense: Expense): Result<Unit>
```

## Android Implementation Guidelines

### ViewModel Architecture
**Event-Driven State Management**: Always use single event handler pattern to prevent race conditions.

```kotlin
// ✅ Good: Single event handler prevents race conditions
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {
    
    fun onUiEvent(event: ExpenseUiEvent) {
        viewModelScope.launch {
            when (event) {
                is ExpenseUiEvent.AmountChanged -> updateAmount(event.amount)
                is ExpenseUiEvent.SaveExpense -> saveExpense()
            }
        }
    }
}
```

### Compose Performance
**Memoization First**: Always consider expensive calculations for memoization.

```kotlin
// ✅ Good: Memoized expensive calculations
@Composable
fun ExpenseChart(expenses: List<Expense>) {
    val chartData by remember(expenses) {
        derivedStateOf { calculateChartData(expenses) }
    }
    val stableOnClick = remember { { expense: Expense -> /* handle click */ } }
    Chart(data = chartData, onClick = stableOnClick)
}
```

### Resource Management
**String Resources Always**: Never hardcode user-facing strings.

```kotlin
// ✅ Good: Externalized strings
@Composable
fun ErrorDisplay(error: UserFacingError) {
    Text(
        text = stringResource(
            when (error) {
                is UserFacingError.ValidationError -> R.string.error_validation
                is UserFacingError.SaveFailed -> R.string.error_save_failed
            }
        )
    )
}
```

### Preview Strategy
**Comprehensive Coverage**: Always create multiple preview states.

```kotlin
@Preview(showBackground = true, name = "Empty State")
@Composable
private fun ExpenseFormPreview_Empty() { /* empty form */ }

@Preview(showBackground = true, name = "Error State")  
@Composable
private fun ExpenseFormPreview_Errors() { /* validation errors */ }
```

## Code Review Checklist

### Core Principle Compliance
- [ ] Is the solution as simple as possible? (KISS)
- [ ] Are related functions grouped together? (High Cohesion)
- [ ] Are dependencies minimal and well-defined? (Low Coupling)
- [ ] Would the code behavior surprise a new developer? (POLA)
- [ ] Is the file under 400 lines and focused on a single responsibility?

### Android-Specific Quality
- [ ] ViewModel uses single event handler pattern?
- [ ] Compose components use memoization for expensive calculations?
- [ ] All user-facing strings use stringResource()?
- [ ] Multiple preview states implemented?
- [ ] Validation logic centralized in dedicated classes?

## Integration with Development Workflow

### During Development
- Apply these principles during the TDD Red-Green-Refactor cycle
- Refactor code that violates these principles during the Refactor phase
- Consider principle adherence when designing new features

### Continuous Improvement
- Regularly review and refactor code that violates these principles
- Update principles based on project experience and team feedback
- Use these principles to guide architectural decisions
- Include principle adherence in definition of done for features

By following these principles consistently, we maintain a codebase that is easy to understand, modify, and extend while supporting the long-term success of the Expense Tracker project.