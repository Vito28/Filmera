---
name: kotlin-native-ui-ux
description: Design, implement, review, and improve production-quality native Android interfaces using Kotlin, Jetpack Compose, Material 3, adaptive layouts, accessibility, and robust UI state handling.
---

# Kotlin Native UI/UX Skill

> **Scope:** Native Android UI built with Kotlin and Jetpack Compose. This does not refer to Kotlin/Native UI for iOS.

## Quick Navigation

- [Purpose](#purpose)
- [Design foundation](#design-direction)
- [Design system and tokens](#design-system)
- [Screen and navigation patterns](#screen-structure)
- [Adaptive layouts](#adaptive-layout)
- [Component rules](#component-rules)
- [State and feedback](#state-design)
- [Accessibility and dark theme](#accessibility)
- [Compose implementation](#compose-implementation-rules)
- [Review checklist](#design-review-checklist)

## Purpose

Use this skill when designing, implementing, reviewing, or improving native Android user interfaces with Kotlin and Jetpack Compose.

**This skill focuses on**

- Modern and elegant Android UI
- Native Android interaction patterns
- Jetpack Compose implementation
- Material 3 as a foundation, not a visual limitation
- Design systems and reusable components
- Responsive and adaptive layouts
- Motion and interaction feedback

## Accessibility

Dark theme

Loading, empty, error, and success states

UI performance

Visual consistency

Production-quality UX

Use this skill together with kotlin-development when the task also involves architecture, data, networking, business logic, or testing.

## Primary Stack

**Prefer**

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Adaptive
- Window Size Classes
- Navigation Compose
- Compose Animation
- Coil
- AndroidX Lifecycle
- ViewModel
- StateFlow
- UI tests with Compose Test

Do not introduce XML layouts into a Compose-first project unless required by an existing legacy component.

## Design Direction

**Create interfaces that feel**

- Native to Android
- Clean and intentional
- Modern without being decorative
- Elegant without sacrificing usability
- Spacious but not wasteful
- Consistent across screens
- Fast and responsive
- Accessible in light and dark themes

Do not copy iOS patterns blindly.

Do not make every screen look like a generic Material demo.

Use Material 3 behavior, semantics, elevation, motion, and component principles while allowing the product to have its own visual identity.

## Core UX Principles

- Make the primary action obvious.
- Reduce unnecessary decisions.
- Show users what is happening.
- Prevent errors before displaying errors.
- Preserve user input whenever possible.
- Keep navigation predictable.
- Use familiar Android interaction patterns.
- Make touch targets comfortable.
- Provide clear feedback for every important action.
- Design for loading, empty, error, offline, and success states.
- Prefer clarity over visual novelty.
- Keep important actions reachable with one hand where practical.
- Avoid hiding essential actions behind gestures only.
- Use progressive disclosure for advanced options.
- Keep destructive actions deliberate and reversible when possible.

## Visual Hierarchy

**Every screen should have a clear hierarchy**

- Screen purpose
- Primary content
- Primary action
- Supporting information
- Secondary actions
- Metadata

**Use hierarchy through**

### Typography

### Spacing

Alignment

### Contrast

Surface grouping

Content order

Limited emphasis colors

Do not rely only on font size.

Avoid giving equal visual weight to every element.

## Design System

Create a centralized design system.

Recommended structure:

```text
core/
  designsystem/
    component/
      AppButton.kt
      AppCard.kt
      AppTextField.kt
      AppTopBar.kt
      AppDialog.kt
      AppSnackbar.kt
      AppEmptyState.kt
    icon/
      AppIcons.kt
    theme/
      Color.kt
      Shape.kt
      Spacing.kt
      Theme.kt
      Typography.kt
    token/
      AppElevation.kt
      AppMotion.kt
      AppSize.kt
```

Feature-specific components should remain inside their feature.

Do not move a component into the global design system until it is genuinely shared.

## Design Tokens

Use tokens instead of unexplained values.

### Spacing

Prefer a consistent 4 dp base scale.

```kotlin
object AppSpacing {
  val xxs = 4.dp
  val xs = 8.dp
  val sm = 12.dp
  val md = 16.dp
  val lg = 24.dp
  val xl = 32.dp
  val xxl = 48.dp
}
```

**Typical usage**

- 4 dp: icon or label micro-gap
- 8 dp: tightly related elements
- 12 dp: compact internal spacing
- 16 dp: default screen and card spacing
- 24 dp: separation between groups
- 32–48 dp: major section separation

Avoid random values such as 13.dp, 19.dp, or 27.dp unless required by a precise visual specification.

### Screen Padding

**Use approximately**

- Compact phone: 16 dp
- Large phone: 20–24 dp
- Tablet content region: 24–32 dp
- Desktop-like Android window: constrained content width with larger outer margins

Do not stretch text forms and reading content across the full width of a tablet.

### Touch Targets

Interactive elements should have a minimum touch target of approximately 48 × 48 dp.

An icon may be visually smaller while its clickable area remains large.

### Shapes

Use a small, intentional shape scale.

```kotlin
object AppShapes {
  val small = RoundedCornerShape(8.dp)
  val medium = RoundedCornerShape(12.dp)
  val large = RoundedCornerShape(20.dp)
  val pill = RoundedCornerShape(50)
}
```

Do not give every element an oversized rounded rectangle.

Use shapes to indicate grouping, hierarchy, and component type.

### Elevation

Prefer subtle elevation and tonal surfaces.

**Use elevation only when it communicates**

- Layering
- Floating actions
- Temporary surfaces
- Navigation hierarchy
- Dragging or active movement

Avoid heavy shadows around every card.

### Color System

Define semantic colors, not feature-specific hardcoded colors.

**Examples**

- primary
- onPrimary
- surface
- surfaceContainer
- surfaceVariant
- onSurface
- onSurfaceVariant
- success
- warning
- error
- info
- outline

Never use raw colors directly inside feature composables when a semantic token is appropriate.

**Bad**

```kotlin
Text(
  color = Color(0xFF6B7280),
)
```

**Better**

```kotlin
Text(
  color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

Use the brand color sparingly.

Do not use the primary color for every icon, title, border, and button.

### Contrast

Ensure text and controls remain readable in both light and dark themes.

Do not communicate state using color alone.

**Combine color with**

## Icons

Labels

### Shapes

Supporting text

Semantic descriptions

### Typography

Use a deliberate typography hierarchy.

**Recommended roles**

- Display: rare, promotional or hero content
- Headline: major screen or section title
- Title: card and dialog title
- Body: main readable content
- Label: controls, tabs, chips, metadata

Prefer no more than three or four visibly distinct text sizes on one screen.

**Use font weight carefully**

- Regular: body content
- Medium: labels and supporting emphasis
- Semi-bold: titles and primary emphasis
- Bold: rare, strong emphasis

Avoid excessive bold text.

Use proper line height for readability.

Do not center-align long paragraphs.

## Screen Structure

**A production screen should normally include**

```kotlin
@Composable
fun FeatureRoute(
  viewModel: FeatureViewModel = hiltViewModel(),
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  FeatureScreen(
    uiState = uiState,
    onAction = viewModel::onAction,
    onNavigateBack = onNavigateBack,
  )
}

@Composable
fun FeatureScreen(
  uiState: FeatureUiState,
  onAction: (FeatureAction) -> Unit,
  onNavigateBack: () -> Unit,
) {
  // Pure UI rendering.
}
```

Keep route-level state collection separate from previewable screen content.

Make the screen composable stateless whenever practical.

### Scaffold Usage

**Use Scaffold intentionally for**

- Top app bars
- Bottom navigation
- Floating action buttons

### Snackbars

System inset handling

Always apply scaffold padding correctly.

```kotlin
Scaffold(
  topBar = {
    FeatureTopBar(
      onNavigateBack = onNavigateBack,
    )
  },
) { innerPadding ->
  FeatureContent(
    modifier = Modifier
      .fillMaxSize()
      .padding(innerPadding),
  )
}
```

Avoid nested scaffolds unless each level has a clear structural purpose.

## Edge-to-Edge

Support edge-to-edge layouts.

Account for:

Status bar

Navigation bar

Display cutouts

Gesture navigation

IME or software keyboard

Bottom sheets

Floating buttons

Use WindowInsets rather than fixed padding guesses.

Do not allow important controls to sit underneath system bars.

## Navigation Patterns

Use navigation based on screen size and hierarchy.

### Compact Width

**Prefer**

- Bottom navigation for 3–5 top-level destinations
- Top app bar for screen-level navigation
- Full-screen detail pages
- Modal or standard bottom sheets for short contextual tasks

### Medium Width

Consider:

Navigation rail

Supporting pane

Wider modal

Two-column forms

### Expanded Width

Consider:

Permanent navigation drawer

List-detail layout

Supporting pane

Constrained reading width

Persistent filters or secondary panels

Do not use bottom navigation with more than five primary destinations.

Do not place secondary actions in primary navigation.

## Adaptive Layout

Design for more than one phone size.

**Support**

- Small phones
- Large phones
- Landscape
- Tablets
- Foldables
- Resizable Android windows

Prefer adaptive decisions based on available width rather than device names.

**Conceptual width classes**

- Compact: less than 600 dp
- Medium: 600–839 dp
- Expanded: 840 dp and above

**Example**

```kotlin
@Composable
fun AdaptiveFeatureScreen(
  windowSizeClass: WindowSizeClass,
) {
  when (windowSizeClass.windowWidthSizeClass) {
    WindowWidthSizeClass.Compact -> CompactFeatureScreen()
    WindowWidthSizeClass.Medium -> MediumFeatureScreen()
    WindowWidthSizeClass.Expanded -> ExpandedFeatureScreen()
  }
}
```

Use a maximum width for forms and readable text.

```kotlin
Box(
  modifier = Modifier.fillMaxSize(),
  contentAlignment = Alignment.TopCenter,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 720.dp)
      .padding(horizontal = AppSpacing.md),
  ) {
    // Content
  }
}
```

## Component Rules

### Buttons

**Use**

- Filled button for the primary action
- Filled tonal button for important secondary actions
- Outlined button for lower-emphasis actions
- Text button for lightweight actions
- Icon button for universally understood compact actions

Normally show only one dominant primary action per section.

**Button labels should describe the action**

- Save changes
- Continue
- Create account
- Try again

**Avoid vague labels**

- OK
- Yes
- Submit

Show loading without changing the button width unexpectedly.

Disable the button only when the reason is obvious. Otherwise allow the action and show validation feedback.

### Cards

Use cards when content forms a meaningful group or has an action boundary.

Do not put every list row inside a card.

Avoid card-inside-card layouts.

Prefer flat lists with separators when content is repetitive.

### Text Fields

**Every field should have**

- Visible label
- Correct keyboard type
- Correct IME action
- Supporting or error text when needed
- Clear focus behavior
- Autofill semantics when applicable
- Password visibility control when applicable

Do not use placeholder text as the only label.

Validate near the field.

Preserve entered text after errors.

### Dialogs

**Use dialogs for**

- Important confirmations
- Short decisions
- Small focused forms
- Destructive action confirmation

Do not use a dialog for long content or complex multi-step flows.

Prefer a full screen or bottom sheet when more space is required.

### Bottom Sheets

**Use bottom sheets for**

- Contextual actions
- Filters
- Short selection tasks
- Supplementary details
- Lightweight forms

Ensure important content is visible above the keyboard.

Avoid placing a second bottom sheet over an existing bottom sheet.

### Snackbars

**Use snackbars for transient feedback such as**

- Item saved
- Item deleted with undo
- Network action failed
- Background action completed

Do not use snackbars for information that users must remember.

### Lists

Use stable keys in lazy lists.

```kotlin
LazyColumn {
  items(
    items = uiState.items,
    key = { item -> item.id },
  ) { item ->
    ItemRow(item = item)
  }
}
```

Use separators and spacing consistently.

Make the entire row clickable when it represents one destination.

Do not place multiple ambiguous tap targets inside a small row.

## State Design

Every data-driven screen must define relevant states.

```kotlin
sealed interface ContentState<out T> {
  data object Loading : ContentState<Nothing>
  data class Success<T>(val data: T) : ContentState<T>
  data object Empty : ContentState<Nothing>
  data class Error(
    val message: String,
    val canRetry: Boolean,
  ) : ContentState<Nothing>
}
```

**At minimum consider**

- Initial
- Loading
- Refreshing
- Content
- Empty
- Partial content
- Error
- Offline
- Submitting
- Success
- Permission denied

Do not replace existing content with a full loading screen during a refresh.

Use pull-to-refresh or inline progress when content already exists.

## Loading UX

Use the least disruptive loading pattern.

**Prefer**

- Inline progress for local actions
- Button progress for form submission
- Skeleton layout for predictable content
- Pull-to-refresh for user-requested refresh
- Full-screen progress only for first load when no content exists

Avoid indefinite spinners without context.

Do not show a skeleton that looks unrelated to the final layout.

## Empty States

**An empty state should explain**

- What is empty
- Why it may be empty
- What the user can do next

**Example**

> **No saved places yet**
>
> Save places you want to visit and they will appear here.
>
> **Explore places**

Avoid decorative empty-state illustrations that consume most of the screen without adding meaning.

## Error States

**Error messages should be**

- Human-readable
- Specific enough to help
- Close to the failed interaction
- Recoverable when possible

**Bad**

> Something went wrong.

**Better**

> We could not load your bookings.  
> Check your connection and try again.

Provide Retry only when retrying can actually help.

Do not expose raw exceptions, HTTP status codes, database errors, or stack traces to users.

## Form UX

Group related fields.

Use logical input order.

Move focus with IME actions.

Scroll focused fields above the keyboard.

**Show inline validation after**

- The field loses focus
- The user submits
- A value becomes clearly invalid

Avoid showing errors before users have interacted.

**For long forms**

- Divide into meaningful sections
- Preserve drafts
- Show progress when multi-step
- Avoid resetting completed fields
- Confirm before discarding meaningful changes

## Motion

Use motion to explain change, not decorate the screen.

**Appropriate uses**

- Screen transition hierarchy
- Expanding content
- Shared element continuity
- Visibility change
- Item insertion or removal
- Selection feedback
- Loading-to-content transition

Keep common transitions subtle and fast.

**Suggested duration ranges**

- Micro interaction: 100–180 ms
- Component transition: 180–300 ms
- Screen or major layout transition: 250–450 ms

Respect reduced-motion preferences where supported.

**Avoid**

- Bouncy motion on every component
- Long decorative entrance animations
- Animating large lists on every recomposition
- Motion that delays interaction
- Continuous animations with no functional purpose

## Interaction Feedback

Every interactive element should provide suitable feedback.

**Use**

- Ripple or indication
- Pressed state
- Selected state
- Loading state
- Disabled state
- Success or failure feedback
- Haptic feedback for meaningful events only

Do not add haptics to every tap.

Use haptics for actions such as:

Confirmed selection

Reorder snap

Long press

Destructive confirmation

Successful scan

## Accessibility

Accessibility is required, not optional.

Ensure:

Minimum comfortable touch targets

Sufficient contrast

Logical focus order

Screen reader labels

Content descriptions for meaningful images

Decorative images excluded from semantics

State announced when necessary

Text remains usable with larger font scales

Actions are not color-only

Motion is not required to understand content

**Example**

```kotlin
IconButton(
  onClick = onClose,
  modifier = Modifier.semantics {
    contentDescription = "Close"
  },
) {
  Icon(
    imageVector = Icons.Default.Close,
    contentDescription = null,
  )
}
```

Do not assign content descriptions to decorative icons that repeat nearby text.

Test with large font sizes.

Avoid clipping text at 1.3×–2.0× font scale.

Do not force one-line text unless truncation is genuinely acceptable.

## Dark Theme

Dark theme should be designed, not produced by simply inverting colors.

**Check**

- Surface hierarchy
- Text contrast
- Image brightness
- Divider visibility
- Disabled states
- Error and success colors
- Brand color saturation
- Navigation and system bar colors

Avoid pure black everywhere unless the product deliberately uses an AMOLED-black theme.

Prefer layered dark surfaces for hierarchy.

## Images and Media

Use meaningful aspect ratios.

Avoid stretching images.

**Use content scaling intentionally**

- ContentScale.Crop for visual thumbnails
- ContentScale.Fit for complete artwork or product display
- ContentScale.Inside for bounded icons or logos

Provide placeholders and error states.

Do not load full-resolution images into small thumbnails.

Avoid placing important text directly over unpredictable images without a contrast treatment.

## Icons

Use one consistent icon family.

Prefer standard Material icons for familiar Android actions.

**Use custom icons only when**

- The action is domain-specific
- The brand requires it
- No standard icon communicates the meaning

Icons should not replace labels for unfamiliar actions.

Do not mix filled, outlined, rounded, and sharp icon styles randomly.

## Content Writing

UI text should be concise and direct.

Use sentence case.

**Good**

- Create account
- Save changes
- No upcoming bookings
- Try again

**Avoid**

- ALL CAPS
- Technical terminology
- Unnecessary punctuation
- Overly playful text in serious flows
- Generic labels without context

Confirmation messages should describe the result.

Destructive actions should name what will be deleted.

## Compose Implementation Rules

### Modifier Order

Use modifier order intentionally.

**A common order is**

- Layout size
- Layout positioning
- Visual decoration
- Interaction
- Semantics
- Padding

**Example**

```kotlin
Modifier
  .fillMaxWidth()
  .clip(MaterialTheme.shapes.medium)
  .background(MaterialTheme.colorScheme.surfaceContainer)
  .clickable(onClick = onClick)
  .padding(AppSpacing.md)
```

Modifier order changes behavior. Review it carefully.

### State Hoisting

Hoist state to the lowest common owner.

```kotlin
@Composable
fun SearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
)
```

Do not hide business state inside reusable composables.

Local UI-only state may remain inside a composable.

### Recomposition

Keep composables stable and focused.

Prefer immutable models.

Avoid performing expensive work directly during composition.

**Use**

- remember
- rememberSaveable
- derivedStateOf
- LaunchedEffect
- snapshotFlow

only when their lifecycle behavior is understood.

Do not use LaunchedEffect(Unit) as a universal solution.

### Previews

**Add previews for**

- Light theme
- Dark theme
- Loading state
- Empty state
- Error state
- Typical content
- Large font or long text when relevant
- Compact and expanded widths for adaptive screens

Use sample data that resembles real content.

## UI Performance

**Avoid**

- Deep unnecessary component trees
- Unstable parameters
- Recreating collections during composition
- Loading oversized images
- Excessive blur and shadow
- Animating large layout changes continuously
- Nested scrolling without a clear need
- Unbounded layout measurements

Use stable keys in lazy layouts.

Move data transformation out of composables when possible.

Profile before making complex optimizations.

## Native Android Behavior

Respect Android conventions:

System back behavior

Predictive back where supported

Share sheet

Permission flow

Notification settings

Keyboard behavior

App links

System bars

Selection controls

Date and time patterns

Accessibility services

Activity recreation

Process restoration

Do not build custom replacements for standard system behavior without a strong reason.

## Permission UX

Ask for permissions only when the user starts a feature that needs them.

**Before requesting, explain**

- What permission is needed
- Why it is needed
- What the user gains

**After denial**

- Continue with reduced functionality when possible
- Explain how to enable permission later
- Do not repeatedly request on every screen entry

## Destructive Actions

Use clear labels:

Delete booking

Remove account

Discard changes

Avoid vague labels such as Confirm.

For reversible actions, prefer an immediate action with Undo.

For irreversible or high-impact actions, require explicit confirmation.

Do not use red for ordinary cancel actions.

## Design Review Checklist

Before considering a UI complete, verify:

### Hierarchy

- [ ] The main purpose is immediately clear.
- [ ] There is one obvious primary action.
- [ ] Supporting actions have lower emphasis.
- [ ] Related content is grouped correctly.

### Consistency

- [ ] Spacing follows tokens.
- [ ] Typography follows defined roles.
- [ ] Shapes and icon styles are consistent.
- [ ] Colors use semantic tokens.
- [ ] Similar components behave the same way.

### States

- [ ] Loading is designed.
- [ ] Empty state is useful.
- [ ] Errors are recoverable.
- [ ] Offline behavior is considered.
- [ ] Submission feedback is visible.
- [ ] Disabled states are understandable.

### Responsiveness

- [ ] Small phone layout works.
- [ ] Large phone layout works.
- [ ] Landscape is usable.
- [ ] Tablet layout does not look stretched.
- [ ] Long text and localization do not break the UI.
- [ ] Keyboard does not cover important fields.

### Accessibility

- [ ] Touch targets are comfortable.
- [ ] Contrast is sufficient.
- [ ] TalkBack labels are meaningful.
- [ ] Large font scaling works.
- [ ] Color is not the only state indicator.
- [ ] Focus order is logical.

### Native Behavior

- [ ] Back navigation is correct.
- [ ] System insets are handled.
- [ ] Permission requests are contextual.
- [ ] System components are used where appropriate.
- [ ] Screen state survives recreation when needed.

### Polish

- [ ] Motion supports understanding.
- [ ] Loading does not cause layout jumps.
- [ ] Buttons do not resize unexpectedly.
- [ ] Content alignment is precise.
- [ ] Empty decorative space is intentional.
- [ ] Visual effects do not reduce performance.

## Anti-Patterns

Do not:

- Use random spacing values throughout the app.
- Hardcode colors in feature screens.
- Put every item inside a card.
- Add gradients without a design purpose.
- Use glassmorphism on every surface.
- Use excessive shadows.
- Overuse rounded containers.
- Use icons without labels for unfamiliar actions.
- Hide important actions behind long press or swipe only.
- Make entire screens scroll horizontally.
- Use a full-screen spinner for every operation.
- Clear form input after an error.
- Stretch mobile layouts across tablet screens.
- Force a single layout for every window size.
- Ignore dark theme.
- Ignore accessibility semantics.
- Animate every screen element.
- Use Spacer values as random visual fixes.
- Build a custom component when a native component already solves the problem well.
- Copy a Dribbble design without validating real usability.

## Agent Workflow

When asked to create or redesign a screen:

1. Identify the screen goal and primary action.
2. Identify user states and failure states.
3. Determine compact, medium, and expanded behavior.
4. Establish content hierarchy.
5. Reuse existing design tokens and components.
6. Create missing reusable components only when justified.
7. Implement stateless screen composables.
8. Connect state through a route composable.
9. Add previews for important states.
10. Review accessibility and system inset behavior.
11. Check light and dark themes.
12. Check loading, empty, error, and long-content cases.
13. Avoid unrelated architectural refactoring.
14. Explain any deliberate UX tradeoff briefly.

## Expected Output

**When generating UI code, include**

- Relevant UI state models
- Screen action definitions
- Route composable
- Stateless screen composable
- Reusable components where justified
- Preview data
- Light and dark previews when practical
- Adaptive behavior when relevant
- Accessibility semantics
- Loading, empty, content, and error handling
- Clear file placement suggestions

Do not return only a visually attractive composable that ignores state, navigation, accessibility, or device variation.

## Definition of Done

A Kotlin native UI/UX task is complete when:

- [ ] The interface has a clear visual hierarchy.
- [ ] The screen feels consistent with Android behavior.
- [ ] The main action is obvious.
- [ ] Design tokens are used consistently.
- [ ] Compact and larger layouts are considered.
- [ ] Light and dark themes remain usable.
- [ ] Loading, empty, error, and content states exist.
- [ ] Touch targets and semantics are accessible.
- [ ] Keyboard and system insets are handled.
- [ ] Components remain previewable and testable.
- [ ] Motion is subtle and purposeful.
- [ ] The UI remains readable with realistic and localized content.
- [ ] No unnecessary visual effects reduce clarity or performance.
