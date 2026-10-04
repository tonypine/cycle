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
| `CycleTheme.stateAlpha` | `CycleStateAlpha` | State layer opacities (`hovered` 8%, `focused` 10%, `pressed` 10%) and disabled ones (`disabledContainer` 12%, `disabledContent` 45%, both on `onSurface`). |

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
  `CyclePolygons` holds Zest's `circle` and eight-point `sun`. Keep morphs for moments that mean
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
component file has its own previews at the bottom, in light and dark.

## Testing components

`core/designsystem/src/test/.../testing/ComponentStateMatrix.kt` is the shared harness every
component test uses. `ComponentStateMatrix.cases(states, layoutStates)` lists each state
(default, pressed, focused, disabled, error, selected, and optionally hovered) in light and dark,
plus each of `layoutStates` (the default state unless you pass more) at 200% font scale and
right-to-left. `PressableStates` suits buttons, `SelectableStates` adds selected for chips and
toggles, and `InteractiveStates` adds error for fields. `ComponentStateMatrixRule.capture(name, case) { ... }`
renders the component in `CycleTheme`, drives the interaction state through the `interactionSource`
it hands you, along with `enabled`, `isError` and `selected` for its parameters, records `src/test/screenshots/<name>_<state>_<appearance>.png`, and checks accessibility:

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
prove shapes spring normally and snap under `CycleTheme(reduceMotion = true)`.

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
