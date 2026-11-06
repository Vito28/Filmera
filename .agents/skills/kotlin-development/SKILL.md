---
name: kotlin-development
description: "Use when designing, implementing, reviewing, testing, debugging, securing, or refactoring Kotlin applications, especially modern Android apps built with Jetpack Compose."
version: 1.0.0
---

# Kotlin Development

> Production-oriented guidance for Kotlin and modern Android development with Jetpack Compose.

Use this skill together with `kotlin-native-ui-ux` when a task requires deeper visual design, interaction design, adaptive layout, or accessibility decisions. This skill remains responsible for architecture, state, data, security, testing, and implementation quality.

## Table of Contents

- [Purpose](#purpose)
- [Primary Stack](#primary-stack)
- [Core Principles](#core-principles)
- [Project Structure](#project-structure)
- [Layer Responsibilities](#layer-responsibilities)
- [Kotlin Style](#kotlin-style)
- [Compose Architecture](#compose-architecture)
- [Coroutines](#coroutines)
- [Compose Best Practices](#compose-best-practices)
- [Design System](#design-system)
- [Responsive Design](#responsive-design)
- [Navigation](#navigation)
- [Dependency Injection](#dependency-injection)
- [Networking](#networking)
- [Repository Pattern](#repository-pattern)
- [Room Database](#room-database)
- [Offline-First Data](#offline-first-data)
- [DataStore](#datastore)
- [Error Handling](#error-handling)
- [Validation](#validation)
- [Authentication](#authentication)
- [Security](#security)
- [Performance](#performance)
- [Accessibility](#accessibility)
- [Localization](#localization)
- [Testing Strategy](#testing-strategy)
- [Logging](#logging)
- [Build Configuration](#build-configuration)
- [Gradle Practices](#gradle-practices)
- [API Design](#api-design)
- [Naming Conventions](#naming-conventions)
- [Documentation](#documentation)
- [Code Review Checklist](#code-review-checklist)
- [Refactoring Rules](#refactoring-rules)
- [Agent Behavior](#agent-behavior)
- [Implementation Workflow](#implementation-workflow)
- [Definition of Done](#definition-of-done)

## Purpose

Use this skill when working on Kotlin applications, especially modern Android applications built with Jetpack Compose.

This skill covers:

- Kotlin language best practices
- Android application architecture
- Jetpack Compose UI development
- State management
- Coroutines and Flow
- Dependency injection with Hilt
- Networking and local persistence
- Navigation
- Testing
- Security
- Performance
- Accessibility
- Code review and refactoring

## Primary Stack

Prefer the following technologies unless the existing project already uses another valid stack:

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Lifecycle
- ViewModel
- Kotlin Coroutines
- StateFlow and SharedFlow
- Hilt
- Room
- Retrofit or Ktor Client
- Kotlin Serialization or Moshi
- Navigation Compose
- DataStore
- WorkManager
- Coil
- JUnit
- MockK or Mockito
- Turbine
- Compose UI Test

Do not replace an existing project stack without a clear technical reason.

## Core Principles

- Write simple, readable, and idiomatic Kotlin.
- Prefer immutable state and explicit data flow.
- Keep business logic outside composables, activities, and fragments.
- Use dependency inversion between presentation, domain, and data layers.
- Model loading, success, empty, and error states explicitly.
- Avoid premature abstraction.
- Reuse components only when they represent a real shared concept.
- Make invalid states difficult to represent.
- Prefer compile-time safety over runtime checks.
- Follow the existing project conventions when they are reasonable.

## Project Structure

Prefer feature-first organization with clear internal layers.

```text
app/
  src/main/java/com/example/app/
    core/
      common/
      designsystem/
      navigation/
      network/
      database/
      model/
      ui/
    feature/
      authentication/
        data/
        domain/
        presentation/
      home/
        data/
        domain/
        presentation/
      profile/
        data/
        domain/
        presentation/
    di/
    MainActivity.kt
    App.kt
```

For smaller applications, simplify the structure.

```text
feature/
  home/
    HomeScreen.kt
    HomeViewModel.kt
    HomeUiState.kt
    HomeRepository.kt
```

Do not create empty layers or interfaces that provide no real value.

## Layer Responsibilities

### Presentation

Contains:

- Compose screens
- Reusable UI components
- ViewModels
- UI state
- UI events
- UI effects
- Presentation mappers

Presentation must not directly access databases, Retrofit services, or Android storage APIs.

### Domain

Contains:

- Business models
- Repository contracts
- Use cases
- Business validation
- Domain errors

The domain layer should avoid Android framework dependencies where practical.

Use cases are optional. Add them when they contain reusable business rules, combine repositories, or simplify ViewModels.

### Data

Contains:

- Repository implementations
- Remote data sources
- Local data sources
- DTOs
- Database entities
- Data mappers
- Cache policies

Do not expose Retrofit DTOs or Room entities directly to the presentation layer.

## Kotlin Style

### Formatting

Use 2-space indentation when the project follows that convention.

Use trailing commas for multiline declarations.

Keep functions focused and reasonably short.

Prefer expression bodies for simple functions.

Avoid deeply nested control flow.

Use meaningful names instead of comments that repeat the code.

```kotlin
fun formatUserName(user: User): String =
  listOfNotNull(user.firstName, user.lastName)
    .joinToString(" ")
    .ifBlank { "Unknown user" }
```

### Null Safety

Avoid !!.

Prefer:

- Safe calls
- Elvis operators
- Early returns
- requireNotNull
- Explicit nullable models

```kotlin
val user = repository.getUser(id) ?: return Result.failure(UserNotFoundException())
```

Use lateinit only when lifecycle or dependency injection guarantees initialization.

### Data Classes

Use data classes for immutable state and value models.

```kotlin
data class User(
  val id: String,
  val name: String,
  val email: String,
)
```

Do not use data classes for objects whose identity or mutable lifecycle is more important than their values.

### Sealed Types

Use sealed interfaces or sealed classes for finite state and event sets.

```kotlin
sealed interface LoginResult {
  data class Success(val user: User) : LoginResult
  data class Failure(val error: LoginError) : LoginResult
}
```

### Scope Functions

Use scope functions intentionally.

- let: transform or safely use a nullable value
- run: compute a result in an object scope
- apply: configure an object
- also: perform side effects
- with: group operations on a non-null object

Avoid chaining several scope functions when normal statements are clearer.

### Collections

Prefer standard collection operators when they improve clarity.

Avoid creating unnecessary intermediate lists in performance-sensitive code.

```kotlin
val activeNames = users
  .asSequence()
  .filter(User::isActive)
  .map(User::name)
  .toList()
```

## Compose Architecture

Use state hoisting and unidirectional data flow.

```text
User action
  -> ViewModel event
  -> business logic
  -> state update
  -> Compose recomposition
```

A route composable may obtain the ViewModel. A screen composable should receive state and callbacks.

```kotlin
@Composable
fun ProfileRoute(
  onBack: () -> Unit,
  viewModel: ProfileViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  ProfileScreen(
    state = state,
    onAction = viewModel::onAction,
    onBack = onBack,
  )
}

@Composable
fun ProfileScreen(
  state: ProfileUiState,
  onAction: (ProfileAction) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Stateless UI rendering.
}
```

### UI State

Prefer one immutable UI state object per screen.

```kotlin
data class ProfileUiState(
  val isLoading: Boolean = false,
  val user: UserUi? = null,
  val errorMessage: String? = null,
  val isSaving: Boolean = false,
)
```

For mutually exclusive states, use a sealed interface.

```kotlin
sealed interface ProductUiState {
  data object Loading : ProductUiState
  data object Empty : ProductUiState
  data class Success(val products: List<ProductUi>) : ProductUiState
  data class Error(val message: String) : ProductUiState
}
```

Do not store Compose State, Context, NavController, or mutable UI objects inside ViewModels.

### Events and Effects

Use events for user intentions.

```kotlin
sealed interface LoginAction {
  data class EmailChanged(val value: String) : LoginAction
  data class PasswordChanged(val value: String) : LoginAction
  data object SubmitClicked : LoginAction
}
```

Use state for persistent screen information.

Use one-time effects only for actions such as:

- Navigation
- Snackbar messages
- Opening external pages
- Requesting focus

```kotlin
sealed interface LoginEffect {
  data object NavigateHome : LoginEffect
  data class ShowMessage(val message: String) : LoginEffect
}
```

Do not use one-time events as a substitute for state that must survive recomposition or configuration changes.

### ViewModel Rules

- Expose immutable StateFlow.
- Keep mutable flows private.
- Use viewModelScope.
- Handle exceptions explicitly.
- Prevent duplicate submissions.
- Keep Android framework objects out of ViewModels.
- Move reusable business rules into domain code.

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
  private val loginUseCase: LoginUseCase,
) : ViewModel() {

  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState = _uiState.asStateFlow()

  private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
  val effects = _effects.receiveAsFlow()

  fun onAction(action: LoginAction) {
    when (action) {
      is LoginAction.EmailChanged -> updateEmail(action.value)
      is LoginAction.PasswordChanged -> updatePassword(action.value)
      LoginAction.SubmitClicked -> submit()
    }
  }

  private fun submit() {
    if (_uiState.value.isSubmitting) return

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

      loginUseCase(
        email = _uiState.value.email,
        password = _uiState.value.password,
      ).fold(
        onSuccess = {
          _uiState.update { state -> state.copy(isSubmitting = false) }
          _effects.send(LoginEffect.NavigateHome)
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = error.toUserMessage(),
            )
          }
        },
      )
    }
  }
}
```

## Coroutines

### Dispatcher Rules

- Main-safe suspend functions should move blocking work internally.
- Use Dispatchers.IO for blocking I/O when the library does not already handle dispatching.
- Use Dispatchers.Default for CPU-intensive work.
- Inject dispatchers when deterministic testing is needed.

```kotlin
data class AppDispatchers(
  val io: CoroutineDispatcher,
  val default: CoroutineDispatcher,
  val main: CoroutineDispatcher,
)
```

### Structured Concurrency

- Prefer structured concurrency.
- Avoid creating unmanaged CoroutineScope instances.
- Do not use GlobalScope.
- Use supervisorScope only when sibling failures should not cancel each other.

### Cancellation

Do not swallow CancellationException.

```kotlin
try {
  repository.sync()
} catch (error: CancellationException) {
  throw error
} catch (error: Exception) {
  logger.error(error)
}
```

### Flow

- Use StateFlow for observable state with a current value.
- Use SharedFlow for broadcast events that may have multiple collectors.
- Use Channel for one-consumer effects.
- Prefer repository streams for data that can change over time.

```kotlin
interface UserRepository {
  fun observeUser(id: String): Flow<User?>
}
```

Use stateIn or shareIn when sharing upstream work.

```kotlin
val uiState = repository.observeProducts()
  .map<List<Product>, ProductUiState> { products ->
    if (products.isEmpty()) ProductUiState.Empty
    else ProductUiState.Success(products.map(Product::toUi))
  }
  .catch { emit(ProductUiState.Error("Unable to load products")) }
  .stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = ProductUiState.Loading,
  )
```

Avoid collecting a Flow inside another collection when operators such as flatMapLatest, combine, or transformLatest are clearer.

## Compose Best Practices

### Recomposition

- Pass stable and immutable parameters.
- Avoid heavy calculations inside composables.
- Use remember for local computed values.
- Use derivedStateOf for derived state that changes more frequently than needed.
- Use keys correctly in lazy lists.
- Do not optimize blindly without measuring.

```kotlin
LazyColumn {
  items(
    items = products,
    key = ProductUi::id,
  ) { product ->
    ProductCard(product = product)
  }
}
```

### Side Effects

Use the correct effect API:

- LaunchedEffect: launch suspend work tied to composition
- DisposableEffect: register and remove listeners
- SideEffect: publish Compose state to non-Compose code
- rememberUpdatedState: access the latest callback inside a long-lived effect

Do not call network, database, or navigation operations directly during composition.

### Modifiers

- Accept modifier: Modifier = Modifier on reusable UI components.
- Apply the passed modifier to the root element.
- Put modifier parameters before callback parameters when practical.
- Avoid exposing internal layout modifiers unnecessarily.

### Previews

Provide previews for reusable components and important states.

```kotlin
@Preview(showBackground = true)
@Composable
private fun ProductCardPreview() {
  AppTheme {
    ProductCard(product = ProductUi.preview())
  }
}
```

Do not make previews depend on real repositories, Hilt, databases, or network access.

## Design System

Place shared visual foundations in a design system.

```text
core/designsystem/
  component/
    AppButton.kt
    AppTextField.kt
    AppTopBar.kt
  icon/
  theme/
    Color.kt
    Shape.kt
    Theme.kt
    Type.kt
  token/
    Spacing.kt
```

Prefer semantic component names over visual names.

**Good**

- PrimaryActionButton
- ErrorBanner
- UserAvatar

**Avoid**

- BlueButton
- RoundedBox
- BigText

Do not make every composable part of the global design system. Feature-specific components should remain inside the feature.

## Responsive Design

Support compact, medium, and expanded layouts.

Do not hardcode one phone width for every screen.

Consider:

- Phones
- Foldables
- Tablets
- Landscape orientation
- Window resizing
- Large font scale

Use window size classes or adaptive layout APIs.

```kotlin
when (windowSizeClass.widthSizeClass) {
  WindowWidthSizeClass.Compact -> CompactHomeScreen(state)
  WindowWidthSizeClass.Medium -> MediumHomeScreen(state)
  WindowWidthSizeClass.Expanded -> ExpandedHomeScreen(state)
}
```

Guidelines:

- Compact: single-column layout
- Medium: wider content or list-detail when useful
- Expanded: navigation rail, permanent drawer, or two-pane layout

Constrain extremely wide content instead of stretching text across the whole screen.

## Navigation

Keep route definitions centralized and type-safe where possible.

```kotlin
sealed interface AppRoute {
  @Serializable
  data object Home : AppRoute

  @Serializable
  data class ProductDetail(val productId: String) : AppRoute
}
```

Rules:

- Pass identifiers instead of full domain objects.
- Load destination data from repositories.
- Keep NavController out of ViewModels.
- Handle deep links explicitly.
- Avoid navigation logic scattered across UI components.
- Prevent duplicate navigation caused by repeated effects.

## Dependency Injection

Use constructor injection by default.

```kotlin
class ProductRepositoryImpl @Inject constructor(
  private val service: ProductService,
  private val dao: ProductDao,
) : ProductRepository
```

Use Hilt modules for bindings and external object creation.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

  @Binds
  @Singleton
  abstract fun bindProductRepository(
    implementation: ProductRepositoryImpl,
  ): ProductRepository
}
```

Avoid service locator patterns and manual dependency access from composables.

## Networking

Separate API DTOs from domain models.

```kotlin
@Serializable
data class ProductDto(
  val id: String,
  val name: String,
  val price: Long,
)

fun ProductDto.toDomain(): Product = Product(
  id = id,
  name = name,
  price = price,
)
```

Handle:

- Timeouts
- Offline state
- Authentication failures
- Rate limiting
- Server validation errors
- Malformed responses
- Retry policies

Do not expose raw exception messages to users.

Do not log access tokens, passwords, personal data, or complete request bodies containing secrets.

## Repository Pattern

Repository interfaces should express application needs rather than mirror API endpoints.

**Good**

```kotlin
interface ProductRepository {
  fun observeProducts(): Flow<List<Product>>
  suspend fun refreshProducts(): Result<Unit>
  suspend fun getProduct(id: String): Result<Product>
}
```

**Avoid**

```kotlin
interface ProductRepository {
  suspend fun getProductsApi(): ProductResponseDto
}
```

## Room Database

- Keep entities separate from domain models.
- Use database transactions for related writes.
- Add indexes for frequent filters and joins.
- Define migrations explicitly.
- Avoid destructive migrations in production.
- Return Flow for observable queries.

```kotlin
@Entity(
  tableName = "products",
  indices = [Index(value = ["category_id"])],
)
data class ProductEntity(
  @PrimaryKey val id: String,
  @ColumnInfo(name = "category_id") val categoryId: String,
  val name: String,
)

@Dao
interface ProductDao {

  @Query("SELECT * FROM products ORDER BY name")
  fun observeAll(): Flow<List<ProductEntity>>

  @Upsert
  suspend fun upsertAll(products: List<ProductEntity>)

  @Query("DELETE FROM products")
  suspend fun deleteAll()
}
```

## Offline-First Data

For data that should work offline:

1. Observe local database data.
2. Refresh from the network.
3. Persist successful network results.
4. Let Room emit the updated data.

Do not maintain separate unsynchronized network and database state inside the UI.

Use timestamps or version fields when cache freshness matters.

## DataStore

Use DataStore for small preferences such as:

- Theme
- Language
- Notification settings
- Onboarding completion

Do not use DataStore as a relational database.

Do not store highly sensitive secrets in plain Preferences DataStore.

## Error Handling

Represent domain failures explicitly.

```kotlin
sealed interface AppError {
  data object NetworkUnavailable : AppError
  data object Unauthorized : AppError
  data object NotFound : AppError
  data class Validation(val fields: Map<String, String>) : AppError
  data class Unknown(val cause: Throwable) : AppError
}
```

Map technical errors into user-facing messages at the presentation boundary.

Do not display stack traces or backend implementation details.

Use Result when it improves clarity. Use a custom result type when typed errors are important.

## Validation

Keep validation deterministic and testable.

```kotlin
class ValidateEmailUseCase {
  operator fun invoke(value: String): EmailValidationResult {
    if (value.isBlank()) return EmailValidationResult.Empty
    if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
      return EmailValidationResult.Invalid
    }
    return EmailValidationResult.Valid
  }
}
```

For domain-only code, prefer validation that does not depend on Android APIs.

Validate both client-side and server-side. Client validation is not a security boundary.

## Authentication

- Store short-lived access tokens securely.
- Prefer platform-backed secure storage where appropriate.
- Never hardcode secrets in the application.
- Treat refresh tokens as sensitive credentials.
- Clear authentication state on logout.
- Handle token expiration centrally.
- Avoid exposing authorization rules only in the UI.
- Enforce authorization on the backend.
- Do not place private API secrets in BuildConfig, resources, or client code. Anything shipped in the APK can potentially be extracted.

## Security

- Required Checks
- Validate all external input.
- Use HTTPS.
- Avoid cleartext traffic.
- Disable sensitive logging in release builds.
- Sanitize data rendered in WebView.
- Restrict exported Android components.
- Use explicit intents for sensitive operations.
- Verify deep-link parameters.
- Protect local sensitive data.
- Apply least-privilege permissions.
- Review third-party dependencies.

### Manifest

Explicitly define exported state for components with intent filters.

```xml
<activity
  android:name=".MainActivity"
  android:exported="true">
  ...
</activity>
```

Set non-public components to android:exported="false".

### WebView

Avoid WebView when native UI or Custom Tabs are sufficient.

When WebView is required:

- Disable unnecessary JavaScript.
- Do not expose unsafe JavaScript interfaces.
- Restrict navigation domains.
- Disable file access when unnecessary.
- Validate URLs before loading them.

### Permissions

- Request permissions only when required by a user action.
- Explain why the permission is needed.
- Handle denial and permanent denial states.
- Do not request broad storage or location permissions when narrower APIs are available.

### Background Work

Use WorkManager for guaranteed deferrable work.

Examples:

- Data synchronization
- Upload retries
- Periodic refresh
- Log cleanup

Do not use WorkManager for immediate UI tasks.

Use foreground services only when the work is user-visible and meets Android requirements.

## Performance

- Measure before optimizing.

Review:

- Startup time
- Main-thread blocking
- Unnecessary recomposition
- Excessive database queries
- Large image loading
- Memory leaks
- Repeated network calls
- Lazy list key stability
- Flow collection lifecycle

Never perform blocking disk or network work on the main thread.

Use paging for large datasets.

Use image resizing and caching.

Avoid decoding full-resolution images when only thumbnails are shown.

## Accessibility

- All important UI must be accessible.
- Provide content descriptions for meaningful images.
- Use null content descriptions for decorative images.
- Support font scaling.
- Maintain sufficient contrast.
- Provide minimum touch targets.
- Do not rely only on color to communicate state.
- Use clear labels for text fields.
- Group semantics when appropriate.
- Test with TalkBack.

```kotlin
Icon(
  imageVector = Icons.Default.Delete,
  contentDescription = stringResource(R.string.delete_item),
)
```

## Localization

- Store user-facing text in string resources.
- Avoid string concatenation.
- Use formatted resources and plurals.
- Support longer translated text.
- Avoid hardcoded date, number, and currency formats.
- Consider right-to-left layouts when relevant.

```kotlin
Text(
  text = stringResource(
    R.string.product_count,
    productCount,
  ),
)
```

## Testing Strategy

### Unit Tests

Test:

- Use cases
- ViewModels
- Validators
- Mappers
- Repository behavior
- Error mapping

Use coroutine test utilities and virtual time.

```kotlin
@Test
fun `submit success emits navigation effect`() = runTest {
  // Arrange
  // Act
  // Assert
}
```

### Flow Tests

Use Turbine for Flow assertions.

```kotlin
viewModel.uiState.test {
  assertEquals(LoginUiState(), awaitItem())
}
```

### Repository Tests

Test:

- Cache-first behavior
- Network refresh
- Offline failures
- Database updates
- Mapping errors

Prefer fakes for simple deterministic dependencies.

Use mocks only when interaction verification is important.

### Compose Tests

Test user-visible behavior instead of implementation details.

```kotlin
composeTestRule
  .onNodeWithText("Sign in")
  .performClick()
```

Use semantic tags sparingly when accessible labels cannot identify a node reliably.

## Logging

Use structured logging.

Log useful context without exposing secrets.

**Good**

```text
Product refresh failed: categoryId=abc123, error=timeout
```

**Avoid**

```text
Login request password=myPassword123 token=eyJ...
```

Release logging should be minimized and sanitized.

## Build Configuration

Keep environment-specific configuration outside source code when possible.

Use build types and product flavors for:

- API base URLs
- Feature flags
- Logging behavior
- Development tools

Do not treat client-side build configuration as secret storage.

Enable release optimization carefully and test release builds.

Maintain ProGuard or R8 rules for libraries that require them.

## Gradle Practices

- Prefer Kotlin DSL.
- Use a version catalog.
- Keep plugin and dependency definitions centralized.
- Avoid dynamic dependency versions.
- Remove unused dependencies.
- Keep modules meaningful.
- Do not create excessive modules for a small application.

```toml
[versions]
kotlin = "..."
compose-bom = "..."

[libraries]
androidx-compose-bom = {
  module = "androidx.compose:compose-bom",
  version.ref = "compose-bom",
}
```

Do not invent dependency versions. Check the existing version catalog or official documentation before changing versions.

## API Design

Prefer small and explicit APIs.

**Good**

```kotlin
suspend fun updateProfile(command: UpdateProfileCommand): Result<User>
```

Avoid methods with many unrelated primitive arguments.

```kotlin
suspend fun updateProfile(
  name: String,
  email: String,
  phone: String,
  city: String,
  country: String,
  avatar: String,
)
```

Use value classes for identifiers when they improve type safety.

```kotlin
@JvmInline
value class UserId(val value: String)
```

## Naming Conventions

### Classes

- HomeScreen
- HomeRoute
- HomeViewModel
- HomeUiState
- HomeAction
- HomeEffect
- GetProductsUseCase
- ProductRepository
- ProductRepositoryImpl
- ProductDto
- ProductEntity
- ProductUi

### Functions

Use verbs:

- loadProducts
- observeUser
- validateEmail
- mapToDomain

Boolean properties should read naturally:

- isLoading
- hasPermission
- canSubmit
- shouldRefresh

Avoid vague names such as:

- Manager
- Helper
- Util
- Data
- Process

Use those only when the responsibility is genuinely clear and narrow.

## Documentation

Document public APIs and non-obvious decisions.

Use English KDoc for reusable public functions and classes.

```kotlin
/**
 * Returns a stream of products stored in the local database.
 *
 * The stream emits again whenever the product table changes.
 */
fun observeProducts(): Flow<List<Product>>
```

Do not add comments that merely restate the code.

## Code Review Checklist

**Before completing a Kotlin task, verify**

#### Correctness

- [ ] Does the code satisfy the requested behavior?
- [ ] Are loading, empty, success, and error states handled?
- [ ] Are lifecycle and cancellation handled correctly?
- [ ] Are race conditions possible?
- [ ] Can duplicate actions occur?

#### Architecture

- [ ] Is business logic outside UI components?
- [ ] Are layer dependencies pointing inward?
- [ ] Are DTOs and entities kept out of presentation?
- [ ] Are abstractions justified?

#### Compose

- [ ] Is state hoisted correctly?
- [ ] Are side effects using the correct API?
- [ ] Are lazy list keys stable?
- [ ] Could expensive work run during recomposition?
- [ ] Is the component previewable?

#### Coroutines and Flow

- [ ] Is structured concurrency preserved?
- [ ] Is cancellation propagated?
- [ ] Are dispatchers appropriate?
- [ ] Are flows collected with lifecycle awareness?
- [ ] Is shared upstream work configured correctly?

#### Security

- [ ] Are secrets or personal data logged?
- [ ] Is sensitive data stored securely?
- [ ] Are permissions minimized?
- [ ] Are deep links and external inputs validated?
- [ ] Are exported components intentional?

#### Quality

- [ ] Are names clear?
- [ ] Is nullability handled safely?
- [ ] Are errors meaningful?
- [ ] Is duplicate code worth extracting?
- [ ] Are tests included for important logic?

## Refactoring Rules

When refactoring:

- Preserve externally observable behavior unless change is requested.
- Add or update tests before risky changes.
- Make small, reviewable changes.
- Remove dead code.
- Avoid introducing abstractions with only one trivial implementation.
- Do not refactor unrelated areas without a clear benefit.
- Explain architectural changes that affect multiple files.

## Agent Behavior

When using this skill as an AI coding agent:

- Inspect the project structure before generating code.
- Read existing Gradle files, version catalogs, architecture, and naming conventions.
- Reuse existing components and utilities where appropriate.
- Do not invent files, routes, APIs, or database fields without evidence.
- Do not change dependency versions unless required.
- Do not remove existing behavior silently.
- Provide complete imports for new Kotlin files.
- Ensure generated code compiles conceptually.
- Mention assumptions when project context is incomplete.
- Prioritize the smallest correct change.
- Add tests for new business logic where practical.
- Check security, accessibility, and error states before finishing.

## Implementation Workflow

For new features, follow this sequence:

1. Understand the user flow and acceptance criteria.
2. Inspect related existing files.
3. Define domain models and repository contracts.
4. Implement or update data sources and mappers.
5. Implement repository behavior.
6. Add use cases only when justified.
7. Define UI state, actions, and effects.
8. Implement ViewModel logic.
9. Build stateless Compose UI.
10. Connect navigation and dependency injection.
11. Add tests.
12. Run formatting, lint, tests, and build checks.

## Definition of Done

A Kotlin task is complete when:

- [ ] The requested behavior is implemented.
- [ ] Code follows project conventions.
- [ ] Important states and errors are handled.
- [ ] No obvious security issue is introduced.
- [ ] UI is responsive and accessible where relevant.
- [ ] Tests cover important logic.
- [ ] There are no unnecessary dependencies or abstractions.
- [ ] The code is ready for review.
