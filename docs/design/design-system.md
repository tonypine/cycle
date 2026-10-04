# Design system

Cycle's UI is built from `core:designsystem`: the Zest tokens from
[`visual-directions.md`](visual-directions.md) (section 3, board
[`directions/zest.html`](directions/zest.html)), the `CycleTheme` that provides them, and the
components built on them. It sits on Compose Foundation only; the build fails if
`androidx.compose.material` or `material3` reaches a classpath (`checkNoMaterialDependencies`).

The `app-catalog` app shows every token and component, one section per page. Run it while building
UI, and use its screenshots in review.

## Reading tokens

Wrap every screen in `CycleTheme` (the activities already do) and read tokens through its object
accessors. Each one is a `CompositionLocal`, so previews and tests can provide other values.

| Accessor | Type | Holds |
| -- | -- | -- |
| `CycleTheme.colors` | `CycleColors` | Every Zest colour role: `accent`, `surface`, `onSurface`, the error roles and the cycle roles (`period`, `predicted`, `predictedEdge`, `fertile`, `ovulation`, `today`, each with its `on*` role). Light or dark, from `darkTheme`. No dynamic colour. |
| `CycleTheme.typography` | `CycleTypography` | `display`, `headline`, `title`, `titleSmall`, `body`, `bodySmall`, `label`, `labelSmall`, `dayNumber`, and emphasized variants (`headlineEmphasized`, `titleEmphasized`, `dayNumberEmphasized`) for key numbers. |
| `CycleTheme.shapes` | `CycleShapes` | Corner scale: `extraSmall` 8dp, `small` 12dp, `medium` 16dp, `large` 24dp, `extraLarge` 32dp, `full` (pill). |
| `CycleTheme.spacing` | `CycleSpacing` | 4dp grid: `extraSmall` 4, `small` 8, `medium` 12, `large` 16 (screen margin), `extraLarge` 24, `extraExtraLarge` 32, `huge` 48. |
| `CycleTheme.elevation` | `CycleElevation` | Shadow scale `level0` to `level3` (0, 1, 3, 6dp). Zest is flat: cards sit on `surfaceContainer` with no shadow; only floating bars, sheets and dialogs cast one. |
| `CycleTheme.motion` | `CycleMotion` | Spatial and effects springs in fast, default and slow speeds, as `FiniteAnimationSpec`s. All `snap()` under reduce motion. See [Motion](#motion). |
| `CycleTheme.stateAlpha` | `CycleStateAlpha` | State layer opacities (`hovered` 8%, `focused` 10%, `pressed` 10%), disabled ones (`disabledContainer` 12%, `disabledContent` 45%, both on `onSurface`), and the text `selection` highlight (40% `accent`, which `CycleTheme` installs as `LocalTextSelectionColors`). |

```kotlin
BasicText(
    text = "Cycle day 19",
    modifier = Modifier.padding(CycleTheme.spacing.large),
    style = CycleTheme.typography.titleEmphasized.copy(color = CycleTheme.colors.onSurface)
)
```

Pair every background role with its `on*` role for content on it (`onAccent` on `accent`,
`onSurfaceVariant` for secondary text on any surface). Cycle states never rely on colour alone, and
errors always come with an icon and a sentence, because period red and error red sit close.

### Fonts

Bricolage Grotesque (display, headline, title) and DM Sans (everything smaller) are bundled as
variable fonts in `core/designsystem/src/main/res/font`. Each weight is an instance of the one file,
made with `FontVariation.Settings`; Bricolage Grotesque always uses width 75 and optical size 96.
Their SIL OFL 1.1 text and copyright notice sit next to them in `res/raw/license_*.txt`, ship in the
APK, and are listed in `OpenSourceNotices`. Any new bundled asset with a licence adds its notice
there too.

## Interaction

Nothing uses Material's ripple. `CycleTheme` installs `CycleIndication`, our own
`IndicationNodeFactory`, as `LocalIndication`, so every `clickable`, `selectable` and `toggleable`
gets it. It gives:

- a **state layer** in the content colour while hovered (8%), focused (10%) or pressed (10%), faded
  on the fast effects spring;
- a **press scale** to 94% on the default spatial spring;
- a **focus ring** for keyboard and D-pad focus: 3dp of `accent`, 2dp outside the component. Touch
  never shows it, because Compose clickables do not take focus in touch mode.

The theme's default draws the layer and ring as a rectangle in `onSurface`. A component with its own
shape or content colour passes `cycleIndication(shape, color)`, and puts `clickable` **before** its
`background`, so the scale moves the whole component:

```kotlin
val interactionSource = remember { MutableInteractionSource() }
val pressed by interactionSource.collectIsPressedAsState()
val shape = animatedCornerShape(active = pressed) // pill → 14dp while pressed
Row(
    Modifier
        .clickable(interactionSource, cycleIndication(shape, CycleTheme.colors.onAccent), enabled, role = Role.Button, onClick = onClick)
        .background(if (enabled) CycleTheme.colors.accent else CycleTheme.colors.onSurface.copy(alpha = CycleTheme.stateAlpha.disabledContainer), shape)
        .heightIn(min = 48.dp)
) { /* icon and label */ }
```

A disabled component draws its container in `onSurface` at `stateAlpha.disabledContainer` and its
content in `onSurface` at `stateAlpha.disabledContent`.

That is how the [buttons, icon buttons and chips](#buttons) are built; use them rather than
repeating it. A new control that draws smaller than 48dp puts `Modifier.minimumTouchTarget()` right
after its `clickable`, `selectable` or `toggleable` (with `indication = null`), and draws the
indication on its visible container with `Modifier.indication(interactionSource, cycleIndication(shape, color))`,
so the touch target is 48dp while the state layer, press scale and focus ring follow the container.

### Shape change

- `animatedCornerShape(active, restingCorner, activeCorner)` springs a rounded rectangle's corners
  between two `CornerSize`s, for press and selection (buttons: pill → 14dp pressed; chips: 12dp →
  pill selected). It returns one shape object that reads its progress when drawn, so pass the same
  shape to `background` and `cycleIndication`.
- `MorphShape(morph, progress)` and `animatedMorphShape(start, end, atEnd)` turn a `graphics-shapes`
  `Morph` between two `RoundedPolygon`s into a Compose `Shape` (`Morph.toPath`, `Outline.Generic`).
  `CyclePolygons` holds Zest's `circle`, period `squircle` and eight-point `sun`. Keep morphs for moments that mean
  something: a day being logged, the ovulation day.

`androidx.graphics:graphics-shapes` is the one extra dependency: Compose Foundation has no polygon
rounding or morphing, and the library depends only on Kotlin and AndroidX core, never Material.

## Motion

`CycleTheme.motion` holds Zest's springs. Spatial specs (position, size, shape) bounce, effects specs
(colour, opacity) never overshoot.

| Spec | Damping | Stiffness | For |
| -- | -- | -- | -- |
| `fastSpatialSpec()` | 0.6 | 800 | Small moves |
| `defaultSpatialSpec()` | 0.6 | 500 | Most moves, press squash, shape change |
| `slowSpatialSpec()` | 0.6 | 200 | Sheets, whole-screen moves |
| `fastEffectsSpec()` | 1.0 | 3800 | State layers |
| `defaultEffectsSpec()` | 1.0 | 1600 | Most colour and opacity changes |
| `slowEffectsSpec()` | 1.0 | 800 | Content fading in and out |

```kotlin
val offset by animateDpAsState(target, CycleTheme.motion.defaultSpatialSpec())
val tint by animateColorAsState(target, CycleTheme.motion.defaultEffectsSpec())
```

Always take specs from `CycleTheme.motion`, never `spring()` or `tween()` directly: that is how
reduce motion works. When the system animator duration scale is 0 ("Remove animations"),
`CycleTheme` sets `reduceMotion`, every spec becomes `snap()`, and shapes jump to their end state.
`isReduceMotionEnabled()` reads the setting and follows it while the app runs. Tests and previews
can pass `CycleTheme(reduceMotion = true)`.

## Icons

`CycleIcons` lists every icon the app ships: Material Symbols Rounded at weight 600, filled, grade 0
and optical size 24, as vector drawables in `core/designsystem/src/main/res/drawable/ic_symbol_*.xml`.
Draw one with `CycleIcon(CycleIcons.Calendar, contentDescription = "Calendar")`; pass `null` when a
visible label already says the same. An icon is not a touch target on its own: put it in a 48dp
clickable. `Back`, `ChevronStart` and `ChevronEnd` are `autoMirrored`, so they flip in right-to-left.

To add an icon, download its SVG from
`https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/<name>/materialsymbolsrounded/<name>_wght600fill1_24px.svg`,
convert it the same way as the existing files (960 viewport, `translateY="960"` group, black fill,
the header comment), and add an entry to `CycleIcons`. The Apache 2.0 text sits next to the assets in
`res/raw/license_material_symbols.txt`, ships in the APK, and is listed in `OpenSourceNotices`.

## Buttons

`Buttons.kt`. One action, with a short verb label ("Log it", "Save", "Nah") and an optional leading
`icon: CycleIcons`.

| Component | Looks | For |
| -- | -- | -- |
| `FilledButton` | `accent` pill, `onAccent` label | The screen's main action. One per screen at most. |
| `TonalButton` | `accentContainer` pill, `onAccentContainer` label | A secondary action that still needs weight. |
| `OutlinedButton` | `accent` label in a 1dp `outline` border | The other action next to a filled button ("Cancel" beside "Save"). |
| `TextButton` | `accent` label, no container | The lightest action: dialogs, "Show all" under a list. |

```kotlin
FilledButton(text = "Log it", onClick = onLog, icon = CycleIcons.Add)
TextButton(text = "Nah", onClick = onDismiss, enabled = canDismiss)
```

Parameters: `text`, `onClick`, `modifier`, `icon` (null), `enabled` (true) and `interactionSource`.
Each is a 40dp pill in a 48dp touch target (`Modifier.fillMaxWidth()` stretches it). Pressing squashes
its corners from the pill to 14dp on the default spatial spring, on top of the theme's state layer
and 94% press scale; under reduce motion the corners jump. TalkBack reads the label and "button";
the icon is decoration and is not read. Disabled buttons use the disabled alphas (outlined and text
buttons keep no container).

Don't use a button for an option that stays on (use a `FilterChip`), or for navigation inside a list
row (make the row clickable).

## Icon buttons

`IconButtons.kt`. An action shown as an icon alone, where a label would not fit: month arrows,
"Add" in a bar.

| Component | Looks | For |
| -- | -- | -- |
| `IconButton` | `onSurface` icon, no container | Most icon actions: previous and next month, close. |
| `FilledIconButton` | `accent` circle, `onAccent` icon | The main action as an icon. |
| `TonalIconButton` | `accentContainer` circle, `onAccentContainer` icon | A secondary icon action that needs weight. |
| `IconToggleButton` | `onSurfaceVariant` icon; `accentContainer` circle when `checked` | A setting turned on and off in place. |

```kotlin
IconButton(CycleIcons.ChevronEnd, contentDescription = "Next month", onClick = onNextMonth)
IconToggleButton(CycleIcons.Calendar, "Show the calendar", checked = showCalendar, onCheckedChange = { showCalendar = it })
```

`contentDescription` is required: name the action ("Next month"), not the icon ("Chevron"). For a
toggle, describe the setting, not its state: TalkBack adds "checkbox" and "checked" or "not checked"
from its toggleable semantics (`Role.Checkbox`). Each is a 40dp circle in a 48dp touch target and
squashes to 14dp corners while pressed, like a button. Prefer a labelled button whenever there is
room: an icon alone is harder to understand.

## Chips

`Chips.kt`. Small options and suggestions that sit with content, 36dp tall in a 48dp touch target,
with an `onSurfaceVariant` label in a 1dp `outline` border and 12dp corners
(`CycleTheme.shapes.small`). Lay them out in a `FlowRow` with `CycleTheme.spacing.small` between.

- `FilterChip(label, selected, onClick)`: one option that stays on, such as a symptom. Selected, it
  fills with `accentContainer`, shows a check before the label, and its corners grow into a pill:
  the shape on the fast spatial spring and the colours on the default effects spring, all jumping
  under reduce motion. The selection never relies on colour alone. TalkBack reads the label,
  "checkbox" and "selected" or "not selected" (`selectable` with `Role.Checkbox`).
- `AssistChip(label, onClick, icon = null)`: a suggested next step that acts once, such as "Add a
  note" under a logged day. It has no selected state; TalkBack reads it as a button.

```kotlin
FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
    symptoms.forEach { symptom ->
        FilterChip(symptom.label, selected = symptom in logged, onClick = { onToggle(symptom) })
    }
}
```

Don't use chips for the screen's main action (use a `FilledButton`), or for choosing exactly one of
a few options (that is a connected button group, still to come).

## Day cell

`DayCell` draws one day of the calendar in Zest's shape for its cycle state, so no state relies on
colour alone:

| `CycleDayState` | Shape | Colours |
| -- | -- | -- |
| `Plain` | The number on its own | `onSurface` |
| `Period` | Solid squircle (14dp corners at 36dp) | `period`, number `onPeriod` |
| `PredictedPeriod` | Pale squircle with a 2dp dashed edge | `predicted`, edge `predictedEdge`, number `onSurface` |
| `Fertile` | Tinted circle with a marker dot under the number | `fertile`, dot `ovulation`, number `onFertile` |
| `Ovulation` | Soft eight-point sun (`CyclePolygons.sun`) | `ovulation`, number `onOvulation` |

Three flags combine with every state:

- `isToday` adds a 3dp `today` ring and the bolder `dayNumberEmphasized`. The shape shrinks to 85% to
  sit inside the ring, so the ring always lies on `surface` and never hides a corner, edge or point.
- `selected` adds a 2dp `accent` rounded-square frame (`shapes.medium`) on the cell's edge. It is
  square where the today ring is round, so the two read apart without colour, and the cell exposes
  `selected` to TalkBack.
- `enabled = false`, for a day outside the month or in the future, draws the whole cell at
  `stateAlpha.disabledContent` and ignores taps. The state's shape stays visible.

Pressed and focused come from `cycleIndication` in the `shapes.medium` tile: the state layer, the
press scale and the focus ring.

```kotlin
DayCell(
    date = date,
    state = CycleDayState.Period,
    onClick = { onDayClick(date) },
    isToday = date == today,
    selected = date == selectedDate,
    enabled = date.month == shownMonth && !date.isAfter(today)
)
```

When `state` changes to `Period`, the fill morphs from the plain circle to the period squircle on
the default spatial spring (`animatedMorphShape(CyclePolygons.circle, CyclePolygons.squircle, ...)`),
a small celebration of logging the day. Under reduce motion it jumps to the squircle. A cell that
starts as a period day shows the squircle straight away.

The cell is at least 48dp square and grows with large text, so the shape always holds the number (58dp
at 200%). Its geometry scales with the cell's smaller side. It is a `Role.Button` whose content
description is the date in the locale's day-and-month form, then "today" and the state: "20 March,
today, period", "14 March, predicted period". The visible number is hidden from TalkBack, so it is
not read twice. The words live in `core:designsystem`'s `strings.xml`.

Do not use it for a date picker that has no cycle meaning, and do not tint a cell with other colours:
a new cycle state needs a new `CycleDayState` with its own shape.

## Cycle legend

`CycleLegend()` is the calendar's key: one swatch per state (period, predicted period, fertile
window, ovulation, today), each drawn by the same code as the cells at 32dp, with its label in
`bodySmall` `onSurfaceVariant`. It is a `FlowRow`, so the entries wrap when the text is large, and it
exposes `CollectionInfo` with one `CollectionItemInfo` per entry, so TalkBack reads it as a list of
five. The swatches are decorative; the label says what each one is. Put it under the calendar it
explains.

## Cards

`Cards.kt`. Content containers on `surfaceContainer` with 32dp corners (`CycleTheme.shapes.extraLarge`)
and `spacing.large` padding. The content slot is a column with `spacing.small` between children; put
text in `onSurface` and `onSurfaceVariant`, which keep their contrast on `surfaceContainer`.

- `Card { ... }`: a panel that does nothing on tap, such as a cycle summary. It is a traversal group,
  so TalkBack reads its children in order before moving on.
- `ClickableCard(onClick, enabled = true, onClickLabel = null) { ... }`: a card that opens something,
  such as last cycle's details. The whole card is one `Role.Button`: TalkBack reads its content as one
  item, then "button" and the `onClickLabel` ("Open"). Pressed, focused and hovered come from
  `cycleIndication` in the 32dp shape: the state layer, the 94% press scale and the focus ring. A
  disabled card draws at `stateAlpha.disabledContent` and ignores taps. It is at least 48dp in both
  directions.

```kotlin
ClickableCard(onClick = { onOpenCycle(cycle) }, onClickLabel = "Open") {
    BasicText("Last cycle", style = CycleTheme.typography.titleSmall.copy(color = CycleTheme.colors.onSurface))
    BasicText("29 days", style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant))
}
```

Don't put a button, chip or other clickable inside a `ClickableCard`: its content merges into one
button, so TalkBack could not reach the inner control. Use a `Card` with the controls in it instead.

## Empty state

`EmptyState.kt`. What a screen or list shows when it has nothing yet: an illustration, a title, one
sentence of body and an optional action, centred one above the other.

```kotlin
EmptyState(
    title = "No periods logged yet",
    body = "Log the first day of your last period and Cycle will start predicting the next one.",
    modifier = Modifier.fillMaxSize(),
    illustration = { EmptyStateIcon(CycleIcons.Calendar) },
    action = EmptyStateAction("Log a period", onClick = onLogPeriod, icon = CycleIcons.Add)
)
```

| Parameter | What it does |
| -- | -- |
| `title` | `title` style in `onSurface`. Marked as a heading. |
| `body` | One sentence in `body` `onSurfaceVariant`: what will appear here, or how to start. |
| `illustration` | Any composable above the title. `EmptyStateIcon(icon)` draws the icon at 48dp in `onAccentContainer` in a 96dp `accentContainer` circle. It is decoration and is not read. |
| `action` | `EmptyStateAction(label, onClick, icon = null)`, shown as a `FilledButton`. Leave it out when there is nothing to do yet. |

Given a bounded height (a screen, or a box with a size) it fills it, centres its content and scrolls
when the content is taller, as at 200% font scale, so it never clips. In a column that already
scrolls, it takes its content's height and leaves scrolling to the column. TalkBack reads the
illustration, title and body as one item marked as a heading, then the action as a button.

Don't use it for an error (say what went wrong and how to fix it, next to where it happened), or
while content is loading (use `LoadingState`).

## Loading

`LoadingIndicator.kt`. Built on graphics-shapes: an `accent` shape that morphs from the sun to a
clover, a pentagon and a 9-point cookie and back, turning a quarter at each step, on
`CycleTheme.motion`'s slow spatial spring. Under reduce motion it does not animate: it holds a static
frame of the sun.

- `LoadingIndicator(contentDescription = "Loading")`: 48dp (the shape fills 38dp of it), in line
  with other content, such as inside a card.
- `LoadingState(message = null)`: fills the space it is given and centres the indicator, with an
  optional `message` under it in `body` `onSurfaceVariant`. Use it for a screen, or the part of one,
  that has nothing to show until something loads.

```kotlin
if (cycles == null) LoadingState(message = "Loading your cycle") else CycleList(cycles)
```

Both read to TalkBack as an indeterminate progress bar (`ProgressBarRangeInfo.Indeterminate`)
described by `contentDescription`, the `message`, or "Loading", and are a polite live region so they
are announced. `LoadingState` reads its message once, as the description. Say what is loading when
you can. The "Loading" string lives in `core:designsystem`'s `strings.xml`.

The loop runs through the coroutine's `InfiniteAnimationPolicy`, so a Compose test whose clock
advances on its own holds the first frame instead of waiting forever; screenshots show the sun.
`LoadingMotionTest` drives the clock by hand to check that it animates normally and holds still
under `CycleTheme(reduceMotion = true)`.

Don't use it for a wait under a second (show nothing), or when you know how far along the work is
(that needs a determinate progress component, still to come).

## Previews

`@Preview` works in every Compose module. The `cycle.android.compose` convention plugin adds
`ui-tooling-preview` to `implementation` and `ui-tooling`, which Android Studio needs to render
previews, to `debugImplementation` only, so release builds never contain it.

`ui-tooling` declares a dependency on `material3` (and through it `material-ripple`), although none
of its classes use Material. A plain `exclude` on the declaration does not remove it, because Gradle
reaches `ui-tooling-android` through a variant redirect. So the plugin registers a component
metadata rule, `DropToolingMaterial`, that removes those two groups from `ui-tooling-android`'s own
dependencies and nothing else. `checkNoMaterialDependencies` has **no exception**: it still checks every
debug, release and unit test classpath, and fails if anything else brings Material in.

The interaction previews live in `core/designsystem/.../InteractionPreviews.kt`, with
`PreviewSurface` and `rememberInteractionSourceIn` for holding a pressed or focused state still. Each
component keeps its previews next to it, at the bottom of its own file (`Buttons.kt`, `DayCell.kt`,
`CycleLegend.kt`, `Cards.kt`, `EmptyState.kt`, `LoadingIndicator.kt`), in light and dark.

## Testing components

`core/designsystem/src/test/.../testing/ComponentStateMatrix.kt` is the shared harness every
component test uses. `ComponentStateMatrix.cases(states, layoutStates)` lists each state
(default, pressed, focused, disabled, error, selected, and optionally hovered) in light and dark,
plus each of `layoutStates` (the default state unless you pass more) at 200% font scale and
right-to-left. `PressableStates` suits buttons, `SelectableStates` adds selected for chips and
toggles, `InteractiveStates` adds error for fields, and `InputStates` swaps pressed for filled, for
text inputs. `ComponentStateMatrixRule.capture(name, case) { ... }` renders the component in
`CycleTheme`, drives the interaction state through the `interactionSource` it hands you, along with
`enabled`, `isError` and `selected` for its parameters, records
`src/test/screenshots/<name>_<state>_<appearance>.png`, and checks accessibility:

- every clickable must be laid out at least 48dp in both directions (a JVM check: Compose stretches
  a small clickable's touch bounds to 48dp and reports those to accessibility, so the Accessibility
  Test Framework cannot see it);
- the Accessibility Test Framework's latest checks then run on the view and its screenshot through
  Roborazzi, failing on any error or warning: nothing for TalkBack to read, text or icon contrast
  below WCAG, and the rest of the preset. Contrast is skipped in the disabled state, which WCAG
  exempts.

```kotlin
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChipScreenshotTest(private val case: MatrixCase) {
    @get:Rule val matrix = ComponentStateMatrixRule()

    @Test fun chip() = matrix.capture("chip", case) {
        FilterChip("Cramps", selected = selected, onClick = {}, enabled = enabled, interactionSource = interactionSource)
    }

    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(
            ComponentStateMatrix.SelectableStates,
            layoutStates = listOf(ComponentState.Default, ComponentState.Selected)
        )
    }
}
```

`AccessibilityHarnessTest` keeps deliberately broken samples (a 24dp target, a clickable with no
label, low-contrast text) to prove each check fails. Use `matrix.checkAccessibility()` on its own for
a screen that needs no screenshot.

Next to the matrix, each component has behaviour tests with the Compose test rule:
`ControlSemanticsTest` asserts roles, toggle and selected state, content descriptions, the 48dp
target and the TalkBack order, and `ControlMotionTest` reads the corner radius frame by frame to
prove shapes spring normally and snap under `CycleTheme(reduceMotion = true)`. `ContainerSemanticsTest`
does the same for cards, the empty state (one heading group, scrolling at 200%) and the loading
state (progress semantics), and `LoadingMotionTest` compares frames of the loading indicator.

## Text field

`CycleTextField` is the outlined text field (Zest `.field`), on Foundation's `BasicTextField` with a
`TextFieldState`. Use it for anything typed: notes, names, numbers. Do not use it for picking from a
fixed set or a date; those get their own components.

```kotlin
val notes = rememberTextFieldState()
CycleTextField(
    state = notes,
    label = "Notes",
    placeholder = "Anything else? Spill it here.",
    supportingText = "Only on this phone.",
    maxLength = 200,
    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3)
)
```

| Parameter | What it does |
| -- | -- |
| `label` | Always shown, inside the field above the value, and part of the field's semantics: TalkBack reads the label, then the value. |
| `placeholder` | Shown in `onSurfaceVariant` while the field is empty. |
| `supportingText` | A hint below the field. |
| `errorMessage` | Puts the field in its error state: a 2dp `error` border, the label in `error`, and the `CycleIcons.Error` icon with the sentence below the field, in place of the supporting text. The field announces it to TalkBack through `error()` semantics. Write a sentence that says how to fix the value: "Pick a day up to today." |
| `leadingIcon` | A decorative `CycleIcons` icon before the value. |
| `trailingAction` | A `TextFieldAction(icon, contentDescription, onClick)`: a 48dp icon button at the end, such as "Clear". |
| `maxLength` | Rejects longer text and shows a "12/200" counter, which TalkBack reads as "12 of 200 characters". |
| `lineLimits` | `TextFieldLineLimits.SingleLine` (the default) or `MultiLine(...)`. |
| `enabled`, `readOnly` | Disabled draws in `onSurface` at the disabled alphas. Read-only keeps the value readable, selectable and focusable, with an `outlineVariant` border. |
| `keyboardOptions`, `onKeyboardAction`, `inputTransformation` | Passed to `BasicTextField`. Without a handler, `ImeAction.Next` moves focus to the next field and `ImeAction.Done` closes the keyboard. |
| `interactionSource` | Receives the field's interactions. Emitting into it (a held focus in a preview or test) only restyles the field. |

The border is `outline` at rest, a 2dp `accent` border while focused, and a 2dp `error` border in
the error state. The border and label colours change on `CycleTheme.motion`'s default effects spring,
so they snap under reduce motion.

**Keyboard.** Activities that show text fields draw edge to edge (`enableEdgeToEdge()`) with
`android:windowSoftInputMode="adjustResize"`. Put the fields in a `verticalScroll` column with
`imePadding()`, or `safeDrawingPadding()` which includes it. When the keyboard opens, the column
shrinks above it, and Foundation scrolls the focused field, with its supporting text, back into view.
`CycleTextFieldImeTest` checks this with Robolectric by dispatching keyboard insets.

## Adding a token

1. Add the value to the token class in `core:designsystem` (`CycleColors`, `CycleTypography`,
   `CycleShapes`, `CycleSpacing` or `CycleElevation`), with a KDoc line saying what it is for. A
   colour needs a light and a dark value.
2. Take values from the chosen direction in `visual-directions.md`. If the doc has no value for
   it, add it to the doc in the same PR.
3. A colour that carries text or meaning adds its pair to `CycleContrastPairs`. `CycleContrastTest`
   then checks it against 4.5:1 (text) or 3:1 (fills, edges, rings) in both palettes.
4. Show it in the matching catalog section and re-record the screenshots
   (`./gradlew recordRoborazziDebug`).

A whole new token set (motion, for example) gets its own `Immutable` class, a `Local*`
`staticCompositionLocalOf`, a line in the `CycleTheme` provider and an accessor on `CycleTheme`.

## Adding a component

1. Write it in `core:designsystem` on Compose Foundation, reading only `CycleTheme` tokens: no
   hard-coded colours, sizes or text styles. Give it a `modifier: Modifier = Modifier` parameter,
   semantics (role, content description, state) and a minimum 48dp touch target.
2. Add previews for its states in light and dark.
3. Add tests under `core/designsystem/src/test`: behaviour and semantics with the Compose test rule,
   and a `ComponentStateMatrixRule` test for its screenshots and accessibility checks (see
   [Testing components](#testing-components)).
4. Add a catalog entry: one `CatalogSection` in `app-catalog`'s `CatalogSections` showing every
   state. `CatalogScreenshotTest` captures each section in light, dark, 200% font scale and
   right-to-left; give the new section a page height there.
5. Add a section for the component to this file: what it is for, its parameters, and when not to
   use it.

## Feature code

Feature code uses design system components and tokens only. It never styles Foundation code ad hoc
(`Modifier.background(Color(...))`, `16.dp` padding, a hand-made `TextStyle`). If a screen needs
something the design system lacks, add the token or component here first.
