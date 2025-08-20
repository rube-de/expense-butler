# Code Quality Principles

## Overview

This document outlines the core principles that guide code quality and maintainability throughout the Expense Tracker project. These principles should be applied consistently across all development work to ensure clean, understandable, and maintainable code.

## KISS (Keep It Simple, Stupid)

### Core Principle
- Prefer simple, straightforward solutions over complex ones
- Remove unnecessary abstractions (e.g., abstract base classes with single implementation)
- Delete unused code rather than keeping it "just in case"
- Choose clarity over cleverness in implementation

### Android/Kotlin Application
```kotlin
// ❌ Over-engineered solution
abstract class BaseRepository<T> {
    abstract fun getData(): Flow<List<T>>
}
class ExpenseRepository : BaseRepository<Expense>() {
    override fun getData(): Flow<List<Expense>> = expenseDao.getAllExpenses()
}

// ✅ Simple, direct solution
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
}
```

### Practical Guidelines
- If you can't easily explain your solution, it's probably too complex
- Avoid creating abstractions until you have at least 3 concrete implementations
- Prefer composition over inheritance
- Use standard library functions instead of custom implementations when possible

## High Cohesion / Low Coupling

### Core Principle
- **High Cohesion**: Keep related functionality together in the same module
- **Low Coupling**: Minimize dependencies between modules
- Each module should have a single, well-defined purpose
- Avoid circular dependencies between modules
- Use dependency injection rather than hard-coded dependencies

### Android/Kotlin Application
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

### Module Organization
- **Data layer**: Contains entities, DAOs, database, and repositories
- **UI layer**: Contains ViewModels, Composables, and navigation
- **Domain layer**: Contains business logic and use cases (when needed)
- Avoid direct dependencies between UI and Data layers (use Repository pattern)

## POLA (Principle of Least Astonishment)

### Core Principle
- Code should behave as developers expect
- Follow established conventions and patterns
- Name functions and variables clearly to indicate their purpose
- Avoid surprising side effects in functions
- Make error conditions explicit and handle them predictably

### Android/Kotlin Application
```kotlin
// ❌ Surprising behavior
fun updateExpense(expense: Expense) {
    // Surprise! This also deletes old categories
    categoryDao.deleteUnusedCategories()
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

// ✅ Clear naming and purpose
suspend fun updateExpenseAndCleanupCategories(expense: Expense): Result<Unit> {
    return try {
        expenseDao.update(expense)
        categoryDao.deleteUnusedCategories()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

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

## File Size Limits

### Core Principle
- **Maximum 300-400 lines per file** - Split larger files into focused modules
- If a file exceeds this limit, refactor into smaller, cohesive components
- Each file should have a single, clear responsibility
- Use module organization to maintain readability and maintainability

### Refactoring Guidelines

When a file becomes too large:

1. **Identify related functions** and group them into separate files
2. **Extract utility functions** into dedicated utility files
3. **Split UI components** into smaller, focused Composables
4. **Separate concerns** (e.g., validation logic from UI logic)

### Example Refactoring
```kotlin
// ❌ Large AddExpenseScreen.kt (500+ lines)
@Composable
fun AddExpenseScreen() {
    // 200 lines of state management
    // 150 lines of validation logic
    // 200 lines of UI components
}

// ✅ Refactored structure
// AddExpenseScreen.kt (100 lines) - Main screen composition
// AddExpenseViewModel.kt (150 lines) - State management
// ExpenseValidator.kt (80 lines) - Validation logic
// ExpenseFormComponents.kt (120 lines) - Reusable form components
```

### File Organization Strategy
- **One primary class per file** (with related helper classes)
- **Group related Composables** in component files
- **Separate business logic** from UI logic
- **Use meaningful file names** that reflect their single responsibility

## Enforcement and Code Reviews

### During Development
- Apply these principles during the TDD Red-Green-Refactor cycle
- Refactor code that violates these principles during the Refactor phase
- Consider principle adherence when designing new features

### Code Review Checklist
- [ ] Is the solution as simple as possible?
- [ ] Are related functions grouped together (high cohesion)?
- [ ] Are dependencies minimal and well-defined (low coupling)?
- [ ] Would the code behavior surprise a new developer (POLA)?
- [ ] Is the file under 400 lines and focused on a single responsibility?

### Continuous Improvement
- Regularly review and refactor code that violates these principles
- Update principles based on project experience and team feedback
- Use these principles to guide architectural decisions
- Include principle adherence in definition of done for features

## Integration with Project Workflow

These principles integrate with our existing development practices:

- **TDD**: Apply principles during the Refactor phase
- **Code Reviews**: Use principles as review criteria
- **Architecture**: Guide architectural decisions with these principles
- **Documentation**: Ensure code is self-documenting through clear naming and structure

By following these principles consistently, we maintain a codebase that is easy to understand, modify, and extend while supporting the long-term success of the Expense Tracker project.