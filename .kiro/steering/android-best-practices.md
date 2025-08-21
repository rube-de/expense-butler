# Android Best Practices

## Overview

This document codifies the critical lessons learned from the comprehensive code review and fixes implemented in this project. These practices ensure production-ready Android applications and prevent common pitfalls that can lead to technical debt and user experience issues.

**Source**: Lessons learned from review-fixes.md - 10 critical improvements that transformed this project from having serious issues to production-ready status.

## Critical Database Practices

### Database Migration Strategy (Critical Issue #1)

**Problem**: Using `.fallbackToDestructiveMigration()` destroys user data on schema changes.

**Solution**: Always implement proper Room migrations from day one.

```kotlin
// ❌ NEVER do this in production
Room.databaseBuilder(context, AppDatabase::class.java, "database")
    .fallbackToDestructiveMigration() // Destroys user data!
    .build()

// ✅ ALWAYS do this
Room.databaseBuilder(context, AppDatabase::class.java, "database")
    .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
    .build()

// Create comprehensive migration strategy
object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add performance indices
            database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_date ON expenses(date)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_categoryId ON expenses(categoryId)")
        }
    }
    
    val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
}
```

**Implementation Requirements**:
- Create `data/database/DatabaseMigrations.kt` from project start
- Include performance indices in migrations
- Test migrations with Room testing utilities
- Never use destructive migration in production builds

## State Management Architecture (Critical Issue #2)

### Event-Driven ViewModel Pattern

**Problem**: Multiple coroutine launches and direct state mutations cause race conditions.

**Solution**: Single event handler with sealed class events.

```kotlin
// ✅ Proper event-driven architecture
sealed class AddExpenseUiEvent {
    data class AmountChanged(val amount: BigDecimal?) : AddExpenseUiEvent()
    data class CategorySelected(val category: Category) : AddExpenseUiEvent()
    data class TagsChanged(val tags: List<String>) : AddExpenseUiEvent()
    object SaveExpense : AddExpenseUiEvent()
    object ResetForm : AddExpenseUiEvent()
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    // Single entry point for all state changes
    fun onUiEvent(event: AddExpenseUiEvent) {
        viewModelScope.launch {
            when (event) {
                is AddExpenseUiEvent.AmountChanged -> {
                    _uiState.update { it.copy(amount = event.amount) }
                }
                is AddExpenseUiEvent.SaveExpense -> {
                    handleSaveExpense()
                }
                // Handle all events consistently
            }
        }
    }
}
```

**Implementation Requirements**:
- Create sealed class for all UI events
- Single `onUiEvent()` function for all state updates
- Use `StateFlow` with `update()` for thread-safe mutations
- Never have multiple `viewModelScope.launch` blocks in ViewModels

## Memory Management (Critical Issue #3)

### Lifecycle-Aware Components

**Problem**: Commented or incomplete lifecycle-aware code suggests memory leaks.

**Solution**: Either implement properly or remove unused code.

```kotlin
// ❌ Dangerous: Commented lifecycle code
// val keyboardController = LocalSoftwareKeyboardController.current

// ✅ Option 1: Remove unused code completely
@Composable
fun AmountInput(
    amount: BigDecimal?,
    onAmountChanged: (BigDecimal?) -> Unit
) {
    // Clean implementation without unused references
}

// ✅ Option 2: Implement properly with lifecycle awareness
@Composable
fun AmountInput(
    amount: BigDecimal?,
    onAmountChanged: (BigDecimal?) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    LaunchedEffect(Unit) {
        // Proper lifecycle-aware implementation
        keyboardController?.hide()
    }
}
```

**Implementation Requirements**:
- Never leave commented lifecycle code
- Use `LaunchedEffect` for side effects
- Implement proper cleanup in `DisposableEffect`
- Test memory usage with LeakCanary

## Navigation Architecture (Important Issue #5)

### Deep Linking from Day One

**Problem**: Missing deep linking support limits navigation patterns.

**Solution**: Implement deep links for all primary screens.

```kotlin
// ✅ Comprehensive navigation with deep linking
composable(
    route = "${Screen.EditExpense.route}/{expenseId}",
    arguments = listOf(navArgument("expenseId") { type = NavType.LongType }),
    deepLinks = listOf(navDeepLink { uriPattern = "expensetracker://edit/{expenseId}" })
) { backStackEntry ->
    EditExpenseScreen(
        onNavigateBack = { navController.popBackStack() },
        onExpenseUpdated = { navController.popBackStack() }
    )
}
```

**Implementation Requirements**:
- Define deep links for all major screens
- Use consistent URI patterns
- Handle navigation arguments properly
- Test deep linking with integration tests

## Validation Architecture (Important Issue #7)

### Centralized Validation Framework

**Problem**: Inline validation logic in UI components reduces testability.

**Solution**: Dedicated validation classes with comprehensive testing.

```kotlin
// ✅ Centralized validation architecture
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

interface Validator<T> {
    fun validate(input: T): ValidationResult
}

class AmountValidator @Inject constructor() : Validator<BigDecimal?> {
    override fun validate(input: BigDecimal?): ValidationResult {
        return when {
            input == null -> ValidationResult.Invalid("Amount is required")
            input <= BigDecimal.ZERO -> ValidationResult.Invalid("Amount must be greater than 0")
            input > MAX_AMOUNT -> ValidationResult.Invalid("Amount too large")
            else -> ValidationResult.Valid
        }
    }
    
    companion object {
        private val MAX_AMOUNT = BigDecimal("999999999.99")
    }
}

// Hilt module for validators
@Module
@InstallIn(SingletonComponent::class)
object ValidationModule {
    @Provides
    @Singleton
    fun provideAmountValidator(): AmountValidator = AmountValidator()
}
```

**Implementation Requirements**:
- Create `domain/validation/` package structure
- Implement `ValidationResult` sealed class
- Create dedicated validator for each input type
- Provide validators through Hilt dependency injection
- Write comprehensive tests for all validators (100+ test cases)

## Performance Optimization (Minor Issue #10)

### Compose Performance Best Practices

**Problem**: Expensive calculations and unnecessary recompositions.

**Solution**: Memoization and stable callbacks.

```kotlin
// ✅ Performance optimization utilities
object PerformanceUtils {
    @Composable
    fun LogCompositions(tag: String) {
        val ref = remember { Ref(0) }
        SideEffect { ref.value++ }
        Log.d(tag, "Compositions: ${ref.value}")
    }
    
    @Composable
    fun <T> stableCallback(dependency: T, block: (T) -> Unit): (T) -> Unit {
        return remember(dependency) { block }
    }
}

// ✅ Memoized expensive calculations
@Composable
fun AnalyticsScreen(
    data: Map<Category, BigDecimal>
) {
    // Memoize chart calculations
    val chartSegments by remember(data) {
        derivedStateOf {
            calculatePieChartSegments(data)
        }
    }
    
    // Stable callbacks prevent recomposition
    val stableOnCategoryClick = stableCallback(viewModel) { category ->
        viewModel.onCategorySelected(category)
    }
    
    PieChart(
        segments = chartSegments,
        onCategoryClick = stableOnCategoryClick
    )
}
```

**Implementation Requirements**:
- Create `ui/util/PerformanceUtils.kt`
- Use `remember` and `derivedStateOf` for expensive calculations
- Implement stable callbacks for event handlers
- Add composition logging during development
- Monitor recomposition counts in complex screens

## Internationalization (Minor Issue #8)

### I18n Infrastructure from Day One

**Problem**: Hardcoded strings prevent internationalization.

**Solution**: Comprehensive string resources and i18n configuration.

```kotlin
// ✅ Comprehensive string resources organization
// res/values/strings.xml
<resources>
    <!-- Navigation labels -->
    <string name="nav_expenses">Expenses</string>
    <string name="nav_add">Add</string>
    
    <!-- Form labels -->
    <string name="label_amount">Amount</string>
    <string name="label_description">Description</string>
    
    <!-- Error messages -->
    <string name="error_amount_required">Amount is required</string>
    <string name="error_amount_too_large">Amount exceeds maximum limit</string>
    
    <!-- Status messages -->
    <string name="status_saving">Saving...</string>
    <string name="status_loading">Loading...</string>
</resources>

// ✅ Error handling with user-facing messages
sealed class UserFacingError {
    data class ValidationError(val field: String, val messageRes: Int) : UserFacingError()
    data class SaveFailed(val messageRes: Int) : UserFacingError()
    data class LoadFailed(val messageRes: Int) : UserFacingError()
}

@Composable
fun ErrorDisplay(error: UserFacingError) {
    when (error) {
        is UserFacingError.ValidationError -> {
            Text(stringResource(error.messageRes))
        }
        is UserFacingError.SaveFailed -> {
            Text(stringResource(error.messageRes))
        }
    }
}
```

**Implementation Requirements**:
- Organize strings by category (navigation, forms, errors, status)
- Create `domain/error/UserFacingError.kt` for type-safe errors
- Configure `res/xml/locales_config.xml` for language support
- Use `stringResource()` throughout UI components
- Separate debug messages (English) from user messages (localizable)

## Preview Strategy (Minor Issue #9)

### Comprehensive Preview Coverage

**Problem**: Single preview doesn't cover all UI states.

**Solution**: Multiple previews for different states.

```kotlin
// ✅ Comprehensive preview strategy
@Preview(showBackground = true, name = "Empty Form")
@Composable
private fun AddExpenseScreenPreview_Empty() {
    ExpenseTrackerTheme {
        AddExpenseScreen(
            uiState = AddExpenseUiState(),
            onUiEvent = { },
            onNavigateBack = { },
            onExpenseSaved = { }
        )
    }
}

@Preview(showBackground = true, name = "Validation Errors")
@Composable
private fun AddExpenseScreenPreview_ValidationErrors() {
    ExpenseTrackerTheme {
        AddExpenseScreen(
            uiState = AddExpenseUiState(
                amountError = "Amount is required",
                descriptionError = "Description is required",
                categoryError = "Please select a category"
            ),
            onUiEvent = { },
            onNavigateBack = { },
            onExpenseSaved = { }
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun AddExpenseScreenPreview_Loading() {
    ExpenseTrackerTheme {
        AddExpenseScreen(
            uiState = AddExpenseUiState(isLoading = true),
            onUiEvent = { },
            onNavigateBack = { },
            onExpenseSaved = { }
        )
    }
}

@Preview(showBackground = true, name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddExpenseScreenPreview_Dark() {
    ExpenseTrackerTheme {
        AddExpenseScreen(
            uiState = sampleFilledState,
            onUiEvent = { },
            onNavigateBack = { },
            onExpenseSaved = { }
        )
    }
}
```

**Implementation Requirements**:
- Create minimum 4-6 previews per major screen
- Cover: empty, filled, error, loading, dark theme states
- Use descriptive preview names
- Share sample data between previews for consistency

## Testing Requirements (Important Issue #6)

### Comprehensive Test Coverage

**Problem**: Missing unit tests for critical ViewModels.

**Solution**: Minimum 40+ test cases for complex ViewModels.

```kotlin
// ✅ Comprehensive ViewModel testing structure
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
    inner class FormValidation {
        @Test
        fun `should show validation errors for invalid form`() = runTest {
            // Test validation scenarios
        }
    }

    @Nested
    inner class SaveFunctionality {
        @Test
        fun `should save expense when valid data provided`() = runTest {
            // Test successful save
        }
        
        @Test
        fun `should handle save failure gracefully`() = runTest {
            // Test failure scenarios
        }
    }
}
```

**Implementation Requirements**:
- Minimum 40+ test cases for complex ViewModels
- Use nested test classes for organization
- Test all UI event handling
- Test all validation scenarios
- Test success and failure paths
- Use MockK for repository mocking
- Achieve 95% test coverage for critical business logic

## Implementation Checklist

Before starting any new Android feature, ensure:

### Pre-Development
- [ ] Database migration strategy planned
- [ ] Validation architecture designed
- [ ] UI event structure defined
- [ ] Deep linking requirements identified
- [ ] String resources categorized
- [ ] Performance monitoring planned

### During Development
- [ ] Event-driven state management implemented
- [ ] Comprehensive validation classes created
- [ ] Multiple preview states implemented
- [ ] Performance optimization applied
- [ ] String resources used throughout
- [ ] Memory management verified

### Post-Development
- [ ] 40+ ViewModel tests written
- [ ] 100+ validation tests created
- [ ] Database migrations tested
- [ ] Deep linking verified
- [ ] Performance monitored
- [ ] I18n infrastructure tested

## Success Metrics

A feature is production-ready when:
- **Test Coverage**: 95%+ for business logic, 80%+ overall
- **Performance**: No unnecessary recompositions detected
- **I18n Ready**: All user-facing strings externalized
- **Navigation**: Deep linking implemented and tested
- **Validation**: Centralized and comprehensively tested
- **Database**: Proper migrations with performance indices
- **State Management**: Single event handler pattern used

These practices ensure that every new implementation starts with the quality standards achieved through the review-fixes process, preventing technical debt and ensuring maintainable, production-ready code.