# Design system

Cycle's UI is built from `core:designsystem`: the Zest tokens from
[`visual-directions.md`](visual-directions.md) (section 3, board
[`directions/zest.html`](directions/zest.html)), the `CycleTheme` that provides them, and the
components built on them. It sits on Compose Foundation only; the build fails if
`androidx.compose.material` or `material3` reaches a classpath (`checkNoMaterialDependencies`).

The `app-catalog` app shows every token and component, one section per page. Run it while building
UI, and use its screenshots in review. Every GitHub Release also carries it as
`cycle-catalog-v<versionName>.apk`, for reviewing the UI on a phone.

## Reading tokens

Wrap every screen in `CycleTheme` (the activities already do) and read tokens through its object
accessors. Each one is a `CompositionLocal`, so previews and tests can provide other values.

| Accessor | Type | Holds |
| -- | -- | -- |
| `CycleTheme.colors` | `CycleColors` | Every Zest colour role: `accent`, `surface`, `onSurface`, the error roles and the cycle roles (`period`, `predicted`, `predictedEdge`, `fertile`, `ovulation`, `today`, each with its `on*` role), and the translucent `scrim` behind sheets and dialogs. Light or dark, from `darkTheme`. No dynamic colour. |
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
a few options (use a [`ButtonGroup`](#button-group)).

## Button group

`ButtonGroup.kt`. A connected, single-choice group: a few short options side by side, of which one
or none is chosen, such as period flow (None, Spotting, Light, Medium, Heavy) or pain (None, Mild,
Moderate, Severe).

```kotlin
var flow by rememberSaveable { mutableStateOf<Int?>(null) }
ButtonGroup(
    label = "Flow",
    options = listOf("None", "Spotting", "Light", "Medium", "Heavy"),
    selectedIndex = flow,
    onSelectedChange = { flow = it }
)
```

| Parameter | What it does |
| -- | -- |
| `label` | Names what the group chooses. It is not drawn: show it as a title above the group. TalkBack reads it after each option ("Medium, Flow"), so two groups that both offer "None" still sound different. |
| `options` | Two or more short labels, one word each where you can. |
| `selectedIndex` | The chosen option, or null while nothing is chosen. |
| `onSelectedChange` | Called with the tapped option's index, or with null when the selected option is tapped again, so a choice can be cleared. |
| `modifier`, `enabled` | A disabled group draws at the disabled alphas, keeps its selection and ignores taps. |
| `interactionSources` | One per option, for previews and tests to hold a pressed or focused segment. |

- **Looks.** Segments are 48dp tall and 2dp apart, with a pill at the group's two outer ends and 8dp
  corners (`shapes.extraSmall`) inside. Unselected, a segment is an `onSurfaceVariant` label in a 1dp
  `outline` border. The selected one fills with `accent`, shows a check before its `onAccent` label
  and turns into a pill, so the choice never relies on colour alone.
- **Motion.** The corners and the check's width move on the default spatial spring, so the chosen
  segment grows and its neighbours make room; colours change on the default effects spring. Under
  reduce motion all of it jumps. Pressed, focused and hovered come from `cycleIndication` in the
  segment's shape: the state layer, the 94% press squash and the focus ring.
- **Width.** The group fills the width it is given and shares the space left over between the
  segments. When the labels do not fit, the segments' padding shrinks to 4dp a side, and then the
  group scrolls sideways, as at 200% font scale. Labels stay on one line and never wrap.
- **TalkBack.** Each segment is a radio button (`Role.RadioButton`) with its selected state, in a
  `selectableGroup`, read in order: "Medium, Flow, radio button, selected".

Don't use it for options that can be on together (use `FilterChip`s), for more than five options or
labels longer than a word or two (use a list on its own screen or in a sheet), or for an action (use
buttons).

## Switch

`Switch.kt`. A setting that is on or off and takes effect at once, such as whether a category shows
in the day log.

```kotlin
SwitchRow(
    title = "Sleep",
    body = "How long you slept and how well.",
    icon = CycleIcons.Bedtime,
    checked = logSleep,
    onCheckedChange = { logSleep = it }
)
Switch(checked = logSleep, onCheckedChange = { logSleep = it }, contentDescription = "Log sleep")
```

- `SwitchRow(title, checked, onCheckedChange, modifier, body = null, icon = null, enabled = true, interactionSource)`
  is the one to reach for: a full-width row on `surfaceContainer` with 24dp corners
  (`shapes.large`), an optional decorative `icon` in `onSurfaceVariant`, the `title` in `titleSmall`
  `onSurface`, an optional one-sentence `body` in `bodySmall` `onSurfaceVariant`, and the switch at
  the end. The whole row is the target (`toggleable`, `Role.Switch`), at least 56dp tall, and
  TalkBack reads it once: the title, the body, "switch" and "on" or "off". Pressed, focused and
  hovered come from `cycleIndication` in the row's shape.
- `Switch(checked, onCheckedChange, contentDescription, modifier, enabled = true, interactionSource)`
  is the switch alone, for a place that already has its label beside it. A 52 by 32dp pill track in
  a 48dp target. `contentDescription` names the setting ("Log sleep"), not its state.

On, the track is `accent` and a 24dp `onAccent` thumb with an `accent` check sits at the end. Off,
the track is a 2dp `outline` border with a 16dp `outline` thumb at the start: the thumb's size and
the check say the state without colour. The thumb slides and grows on the default spatial spring and
the colours change on the default effects spring; under reduce motion they jump. Disabled, the text,
icon and switch draw at the disabled alphas, the state stays visible and taps are ignored.

Don't use a switch for choosing between options (use a `ButtonGroup`), for picking several items
such as symptoms (use `FilterChip`s), or for an action that happens once (use a button).

## Radio row

`RadioRow.kt`. One choice of several, each with a line under it, such as her contraception method or
whether she takes breaks between packs.

```kotlin
RadioGroup(
    options = methods,
    selected = method,
    onSelect = { method = it },
    title = { methodTitle(it) },
    body = { methodLine(it) }
)
RadioRow(title = "Implant", selected = true, onClick = {}, body = "A rod in the arm, such as Nexplanon")
```

- `RadioGroup(options, selected, onSelect, title, modifier, body = { null }, enabled = true)` is the
  one to reach for: a `RadioRow` per option, `spacing.small` apart, with nothing selected while
  `selected` is null. The rows are one `selectableGroup` and a collection, so TalkBack reads each
  row's place in it: "Implant, A rod in the arm, such as Nexplanon, radio button, selected, 6 of 9".
- `RadioRow(title, selected, onClick, modifier, body = null, enabled = true, interactionSource)` is
  one row: a full-width row on `surfaceContainer` with 24dp corners (`shapes.large`), the `title` in
  `titleSmall` `onSurface`, an optional one-line `body` in `bodySmall` `onSurfaceVariant`, and the
  radio at the end. The whole row is the target (`selectable`, `Role.RadioButton`), at least 56dp
  tall. Pressed, focused and hovered come from `cycleIndication` in the row's shape.

Selected, the row is `accentContainer` with `onAccentContainer` text and the radio is a 10dp `accent`
dot in a 20dp `accent` ring; otherwise the radio is a 2dp `outline` ring, so the dot says the state
without colour. The colours change on the default effects spring and jump under reduce motion.
Disabled, the text and radio draw at the disabled alphas and taps are ignored.

Don't use a radio row for two to five short options side by side (use a `ButtonGroup`), for a
setting that is on or off (use a `SwitchRow`), or for picking several items (use `FilterChip`s).

## Slider

`Slider.kt`. A whole number picked along a track, such as her usual cycle length in days. Built on
Compose Foundation's pointer input and semantics: there is no Foundation slider, and Material's is
off limits.

```kotlin
var cycle by rememberSaveable { mutableIntStateOf(28) }
SliderField(
    label = "Cycle length",
    value = cycle,
    onValueChange = { cycle = it },
    valueRange = 15..90,
    valueText = "$cycle days",
    decreaseDescription = "Cycle one day shorter",
    increaseDescription = "Cycle one day longer",
    supportingText = "Often between 21 and 35 days."
)
Slider(cycle, { cycle = it }, 15..90, contentDescription = "Cycle length", stateDescription = "$cycle days")
```

- `SliderField(label, value, onValueChange, valueRange, valueText, decreaseDescription, increaseDescription, modifier, supportingText = null, enabled = true)`
  is the one to reach for: the `label` in `titleSmall` and the value as large text
  (`valueText` in `headlineEmphasized`, "28 days"), `−` and `+` `TonalIconButton`s for one step at a
  time, the slider under them for big moves, and an optional one-sentence `supportingText` in
  `bodySmall` `onSurfaceVariant`, on `surfaceContainer` with 24dp corners (`shapes.large`). On a
  wide range such as 15 to 90, one step is a millimetre of track, so the buttons are how she lands
  on an exact value. `−` turns off at the start of the range and `+` at the end.
- `Slider(value, onValueChange, valueRange, contentDescription, stateDescription, modifier, enabled = true, interactionSource)`
  is the track alone, for a place that already shows the value. One step per whole number.

**Looks.** A 16dp pill track: `accent` up to the value, `accentContainer` after it, with a 4dp
`accent` dot at the far end. At the value, a 4 by 44dp `accent` handle with a 6dp gap on each side
and 2dp corners on the track next to it. The slider is 48dp tall and fills the width it is given;
it runs right to left in right-to-left layouts. Disabled, the active part and handle draw in
`onSurface` at `stateAlpha.disabledContent` and the rest at `disabledContainer`.

**Touch and motion.** Tapping the track moves to that value and the handle springs there on the fast
spatial spring; dragging sideways follows the finger directly. A vertical swipe that starts on the
track still scrolls the screen. The handle narrows to 2dp while held. Pressed and focused come from
`cycleIndication` on the handle: the focus ring and press scale. Under reduce motion it all jumps.

**Keys and TalkBack.** Arrow keys move one step (right is forward, or back in right-to-left), Home and
End go to the ends. TalkBack reads the slider's `contentDescription` and `stateDescription` ("Cycle
length, 28 days") and adjusts it one step at a time (`ProgressBarRangeInfo` with a step per whole
number, and `setProgress`). In a `SliderField` the label and value read together as one item, a polite
live region, so the new value is announced when `−` or `+` change it; the buttons read their
descriptions, which name what they do ("Cycle one day shorter").

Don't use a slider for a value she would type more easily, such as a year, or for a few named options
(use a [`ButtonGroup`](#button-group)).

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
- `enabled = false` draws the whole cell at `stateAlpha.disabledContent` and ignores taps. The
  state's shape stays visible.

A day that shows but cannot be tapped, such as a future day in the calendar, passes
`onClick = null` instead of `enabled = false`: it keeps its full colour, so a predicted period stays
readable, and ignores taps. TalkBack reads its date and state without "button" and with no "double
tap to activate"; it still exposes `selected`. Calendars do this for every day their caller marks
as not enabled.

Pressed and focused come from `cycleIndication` in the `shapes.medium` tile: the state layer, the
press scale and the focus ring.

```kotlin
DayCell(
    date = date,
    state = CycleDayState.Period,
    onClick = if (date.isAfter(today)) null else ({ onDayClick(date) }),
    isToday = date == today,
    selected = date == selectedDate
)
```

When `state` changes to `Period`, the fill morphs from the plain circle to the period squircle on
the default spatial spring (`animatedMorphShape(CyclePolygons.circle, CyclePolygons.squircle, ...)`),
a small celebration of logging the day. Under reduce motion it jumps to the squircle. A cell that
starts as a period day shows the squircle straight away.

The cell is at least 48dp square and grows with large text, so the shape always holds the number (58dp
at 200%). Its geometry scales with the cell's smaller side. It is a `Role.Button` whose content
description is the date in the locale's day-and-month form, then "today" and the state: "20 March,
today, period", "14 March, predicted period", "17 March, estimated ovulation". Ovulation and the
fertile window are estimates from period dates, so they always say "estimated". The visible number is
hidden from TalkBack, so it is not read twice. The words live in `core:designsystem`'s `strings.xml`.
On a method, `words: BleedingWords` (`Period` by default, `Bleed`, `Bleeding`) names a period day
"bleed" or "bleeding" and a predicted one "expected bleed" ("14 October, bleeding"); the shapes and
colours stay ([`contraception.md`](contraception.md#components)).

Do not use it for a date picker that has no cycle meaning, and do not tint a cell with other colours:
a new cycle state needs a new `CycleDayState` with its own shape.

## Month calendar and week row

`Calendar.kt`. Feature code never lays out `DayCell`s itself: a month goes in a `MonthCalendar`, a
week in a `WeekRow`.

- `MonthCalendar(month, stateOf, onDayClick, today, onPreviousMonth, onNextMonth, selected = null,
  isEnabled = { true }, wordsOf = { BleedingWords.Period })`: a header with the previous and next `IconButton`s ("Previous month", "Next
  month", `ChevronStart` and `ChevronEnd`, so they mirror in right-to-left) around the month name in
  `title`; the narrow weekday names in `labelSmall` `onSurfaceVariant`; then a seven-column grid of
  `DayCell`s. The days outside `month` are left blank, so a month takes four to six rows.
- `WeekRow(weekOf, stateOf, onDayClick, today, selected = null, isEnabled = { true }, wordsOf = {
  BleedingWords.Period })`: the weekday
  names and the seven days of the week around `weekOf`, for Today and for picking a recent day. Days
  of the previous or next month show like any other.

`stateOf` gives each day's `CycleDayState`, `today` gets the ring, `selected` the frame. A day where
`isEnabled` returns false keeps its full colour and ignores taps (`DayCell` with `onClick = null`), so
pass `{ !it.isAfter(today) }` to keep a predicted period readable while only past days can be
logged. The week starts on the phone's first day (`firstDayOfWeek()`), whatever Cycle's language:
Monday in the UK, Sunday in the US, and the columns run right to left in right-to-left layouts. Each cell is keyed by its date,
so moving to another month never replays the logging morph. `wordsOf` gives each day's
`BleedingWords`, so a month spanning the start of a method reads "period" before it and "bleeding"
after.

```kotlin
var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
MonthCalendar(
    month = month,
    stateOf = { date -> dayStates[date] ?: CycleDayState.Plain },
    onDayClick = onOpenDay,
    today = today,
    onPreviousMonth = { month = month.minusMonths(1) },
    onNextMonth = { month = month.plusMonths(1) },
    modifier = Modifier.padding(horizontal = CycleTheme.spacing.medium),
    selected = selectedDay,
    isEnabled = { !it.isAfter(today) }
)
CycleLegend(entries = CycleLegendEntry.WithoutFertility)
```

Layout: every cell is the size of the largest and each column is at least that wide; the columns
share the width left over. Lay a calendar out with `spacing.medium` (12dp) margins, not the 16dp
screen margin: seven 48dp cells need 336dp, exactly what a 360dp screen leaves inside 12dp margins.
With large text the cells grow (58dp at 200%), and seven no longer fit even a 412dp screen, so the
weekday names and the days **scroll sideways together** while the header stays put. Shrinking the
margins would not be enough (seven 58dp cells are 406dp). When it scrolls, the grid starts with
today's column in view, since today is often at the weekend end of the week.

TalkBack: the month name is a heading and a polite live region, so it is read again when the month
changes. The grid exposes `CollectionInfo` (as many rows as the month has weeks, seven columns; one
row for a `WeekRow`) and each day a `CollectionItemInfo`, and reads as in `DayCell` ("20 March,
today"). Each weekday initial reads as the day's full name ("Monday").

## Cycle legend

`CycleLegend(entries = CycleLegendEntry.entries)` is the calendar's key: one swatch per entry
(`Period`, `PredictedPeriod`, `Fertile`, `Ovulation`, `Today`), each drawn by the same code as the
cells at 32dp, with its label in `bodySmall` `onSurfaceVariant`: "Period", "Predicted period",
"Estimated fertile window", "Estimated ovulation", "Today". Show only the entries the calendar
above it can draw: `CycleLegendEntry.WithoutFertility` (period, predicted period, today) while
fertility estimates are off, as in the MVP. The default is all five. It is a `FlowRow`, so the entries
wrap when the text is large, and it exposes `CollectionInfo` with one `CollectionItemInfo` per entry,
so TalkBack reads it as a list of as many items as it shows. The swatches are decorative; the label
says what each one is. Put it under the calendar it explains.

`words` lists the `BleedingWords` of the logged days on screen, in order: the `Period` entry shows
once per word ("Period", then "Bleeding" in a month where she started the implant). `predictedWords`
names the predicted days on their own, so `PredictedPeriod` reads "Expected bleed" on a combined pill
with a break every month even when the only logged days on screen are a period before it ("Period",
"Expected bleed"). Leave `PredictedPeriod` out when nothing is predicted, as on the implant.

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
sentence of body, a main action and any secondary ones, centred one above the other.

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
| `illustration` | Any composable above the title. `EmptyStateIcon(icon)` draws the icon at 48dp in `onAccentContainer` in a 96dp `accentContainer` circle; `EmptyStateIcon(painter)` does the same for a single-colour drawing outside `CycleIcons`, such as the app's mark on the lock screen. It is decoration and is not read. |
| `action` | `EmptyStateAction(label, onClick, icon = null)`, shown as a `FilledButton`. Leave it out when there is nothing to do yet. |
| `secondaryActions` | `EmptyStateAction`s shown as `TextButton`s under `action`, in order, such as "Restore from a Cycle export" and the way out ("Skip for now"). |

Given a bounded height (a screen, or a box with a size) it fills it, centres its content and scrolls
when the content is taller, as at 200% font scale, so it never clips. In a column that already
scrolls, it takes its content's height and leaves scrolling to the column. TalkBack reads the
illustration, title and body as one item marked as a heading, then the actions as buttons.

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

## Top app bar

`TopAppBar.kt`. The bar at the top of a screen: an optional navigation icon button, the screen's
title, and up to two action icon buttons, on `surface`, at least 64dp tall.

```kotlin
TopAppBar(
    title = "Calendar",
    navigation = AppBarAction(CycleIcons.Back, "Back", onBack),
    actions = listOf(
        AppBarAction(CycleIcons.Today, "Go to today", onToday),
        AppBarAction(CycleIcons.Settings, "Settings", onSettings)
    )
)
```

- `AppBarAction(icon, contentDescription, onClick, interactionSource = null)` draws an `IconButton`:
  name the action, not the icon. `actions` holds at most two (`MAX_TOP_APP_BAR_ACTIONS`); anything
  more belongs on the screen.
- The title is `title` type in `onSurface`, with `heading()` semantics so TalkBack users can jump to
  it. TalkBack reads the navigation button, then the title, then the actions.
- The title stays on one line and ends in an ellipsis when it runs out of room, at any font scale, so
  the actions never leave the screen.
- Edge to edge: put it at the top of a screen drawn with `enableEdgeToEdge()`. It pads itself by
  `WindowInsets.statusBars`, plus the display cutout and navigation bar on either side, and fills the
  space behind the status bar with `surface`.

Don't use it inside a page or a card: it is the screen's header, one per screen. Don't add a third
action; move it onto the screen.

## Navigation bar

`NavigationBar.kt`. The app's top-level navigation: three to five destinations in a floating,
rounded `surfaceContainer` bar (Zest floats it, with `CycleTheme.elevation.level1` and 32dp
corners, 12dp in from the screen edges).

```kotlin
val destinations = listOf(
    NavigationDestination("Today", CycleIcons.Today),
    NavigationDestination("Calendar", CycleIcons.Calendar),
    NavigationDestination("Settings", CycleIcons.Settings)
)
NavigationBar(destinations, selectedIndex = selected, onSelect = { selected = it })
```

- Each destination shows its icon over a one-word label in `labelSmall`. The selected one sits on an
  `accentContainer` pill with an `onAccentContainer` icon and an `onSurface` label; the others are
  `onSurfaceVariant` (the "Secondary text on card" contrast pair).
- When the selection changes, the pill slides to the new destination and stretches on the way: its
  leading edge moves on the fast spatial spring and its trailing edge on the default spatial spring.
  Icon and label colours change on the default effects spring. Under reduce motion all of it jumps.
- Each destination is a tab (`Role.Tab`, in a `selectableGroup`) at least 48dp tall, with its selected
  state and its position (`CollectionInfo` on the bar, `CollectionItemInfo` on each tab), so TalkBack
  reads "Calendar, selected, tab, 2 of 4". The icon is decoration.
- At large font scales, labels that do not fit shrink together, never below their size at 100%, and
  only then end in an ellipsis. Keep labels to one short word.
- Edge to edge: put it at the bottom of the screen. It pads itself by `WindowInsets.navigationBars`.
- `interactionSources` takes one source per destination, for previews and tests to hold a pressed or
  focused state.

The catalog's App bars page has a full-screen demo with both bars on an edge-to-edge screen, the
system bars shaded. `AppBarSemanticsTest` checks the heading, tab roles, selected state, positions
and TalkBack order, `NavigationBarMotionTest` measures the pill mid-spring and under reduce motion,
`AppBarInsetsTest` and the catalog's `AppBarsDemoScreenshotTest` dispatch system bar insets and check
both bars stay clear of them.

Don't use the navigation bar for actions (use buttons), for fewer than three destinations (use the
top app bar's navigation), or for switching views inside one screen (use a
[`ButtonGroup`](#button-group)).

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
component keeps its previews next to it, at the bottom of its own file (`Buttons.kt`,
`ButtonGroup.kt`, `Switch.kt`, `DayCell.kt`, `Calendar.kt`, `CycleLegend.kt`, `Cards.kt`,
`EmptyState.kt`, `LoadingIndicator.kt`), in light and dark.

## Testing components

`core/designsystem/src/test/.../testing/ComponentStateMatrix.kt` is the shared harness every
component test uses. `ComponentStateMatrix.cases(states, layoutStates)` lists each state
(default, pressed, focused, disabled, error, selected, and optionally hovered) in light and dark,
plus each of `layoutStates` (the default state unless you pass more) at 200% font scale and
right-to-left. `PressableStates` suits buttons, `SelectableStates` adds selected for chips and
toggles, `InteractiveStates` adds error for fields, and `InputStates` swaps pressed for filled, for
text inputs. `ComponentStateMatrixRule.capture(name, case) { ... }` renders the component in
`CycleTheme`, drives the interaction state through the `interactionSource` it hands you, along with
`enabled`, `isError` and `selected` for its parameters, pads it by `spacing.large` (or the `margin`
you pass, such as a calendar's 12dp), records
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

A component with a window of its own, such as the [bottom sheet](#bottom-sheet), passes that window's
root to `checkAccessibility(root = composeRule.onNode(isDialog()))` and captures the whole screen with
Roborazzi's `captureScreenRoboImage`. `suppress` drops a framework result that cannot apply, with the
reason next to the matcher.

`AccessibilityHarnessTest` keeps deliberately broken samples (a 24dp target, a clickable with no
label, low-contrast text) to prove each check fails. Use `matrix.checkAccessibility()` on its own for
a screen that needs no screenshot.

Next to the matrix, each component has behaviour tests with the Compose test rule:
`ControlSemanticsTest` asserts roles, toggle and selected state, content descriptions, the 48dp
target and the TalkBack order (and that a button group's labels stay on one line and scroll at
200%), and `ControlMotionTest` reads the corner radius and the switch thumb's position frame by frame
to prove they spring normally and snap under `CycleTheme(reduceMotion = true)`. `ContainerSemanticsTest`
does the same for cards, the empty state (one heading group, scrolling at 200%) and the loading
state (progress semantics), and `LoadingMotionTest` compares frames of the loading indicator.
`SliderTest` drags, taps, scrolls past, presses keys on and sets through TalkBack the slider and the
slider field, and checks that `−` and `+` stop at the ends.

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

## Dialogs

`Dialogs.kt`. A dialog interrupts to ask one question or collect one value. It opens in its own
window from Compose UI's `Dialog`, edge to edge: the `scrim` dims the whole screen, system bars
included, and the dialog sits on `surfaceContainer` with 32dp corners (`shapes.extraLarge`) and a
level 3 shadow, inside the safe drawing area, so it never sits under a bar, a cut-out or the keyboard.

| Component | Has | For |
| -- | -- | -- |
| `CycleAlertDialog` | Optional icon, title, a sentence or two, confirm and optional dismiss text buttons | A question with a clear answer: "Turn on reminders?" |
| `CycleDestructiveDialog` | Error icon (by default), title, a sentence saying what is lost, a required dismiss button and an `error` confirm button | An action that loses data: "Delete this day?" |
| `CycleDialog` | Optional icon, title and sentence, then a content slot, confirm and optional dismiss | Collecting a value, such as a note in a `CycleTextField`. `confirmEnabled` holds the confirm action off until it is valid. |

```kotlin
var confirmDelete by rememberSaveable { mutableStateOf(false) }
CycleDestructiveDialog(
    visible = confirmDelete,
    onDismissRequest = { confirmDelete = false },
    title = "Delete this day?",
    text = "This removes the period and notes logged for 14 March. You can't undo it.",
    confirmText = "Delete",
    onConfirm = { onDelete(); confirmDelete = false },
    dismissText = "Keep it"
)
```

- **Showing and hiding.** Keep the dialog in composition and flip `visible`; set it to false in
  `onDismissRequest` and in the actions. The scrim fades on the effects springs and the dialog fades
  and scales from 90% on the spatial springs, faster on the way out. The window stays until the exit
  ends. Under reduce motion both snap.
- **Dismissal.** Back (and Escape) and a tap on the scrim call `onDismissRequest`. Turn either off
  with `dismissOnBackPress = false` or `dismissOnScrimTap = false`, for a choice that must be made.
  A tap on the dialog itself never dismisses it. The dismiss button calls `onDismiss`, which defaults
  to `onDismissRequest`.
- **Actions.** They sit in a row at the end, confirm last. When the row does not fit, such as at
  200% font scale, they stack at the end with confirm on top. Alert and custom dialogs use text
  buttons; the destructive one pairs a text button with an `error` pill (`onError` label).
- **Destructive.** The danger never relies on colour alone: the error icon, a sentence that says what
  will be lost and the red confirm button all carry it, and there is always a dismiss button.
- **Focus.** When the dialog opens, focus moves into it, onto its first focusable element: the
  dismiss button for a keyboard or D-pad user, or the first text field in a `CycleDialog`, which
  opens the keyboard. When it closes, focus goes back to whatever had it, such as the button that
  opened it. TalkBack announces the title as the window's title.
- **Keyboard.** The dialog pads itself above the keyboard and its content scrolls, so the focused
  field scrolls into view and the actions stay above the keyboard.
- **TalkBack.** The title is a heading, and TalkBack reads the title, the text and the actions in
  that order. The icon is decorative.
- **Density and direction.** The dialog window uses the caller's `LocalDensity` and
  `LocalLayoutDirection`, so it matches the screen that opened it.

Text and icons on the dialog use pairs checked against `surfaceContainer`: `onSurface` (title),
`onSurfaceVariant` (text), `accent` (icon and text buttons) and `error` (the destructive icon).

Don't use a dialog for a message that needs no answer (that is a snackbar, still to come), for a
choice that can be undone in place, or for a long form (use a screen).

`DialogTest` covers dismissal, focus, the action layout and motion, `DialogImeTest` the keyboard,
and `DialogScreenshotTest` captures each dialog over a sample screen with `captureScreenRoboImage`,
then runs the accessibility checks on the dialog's window
(`matrix.checkAccessibility(root = composeRule.onNode(isDialog()))`).

## Bottom sheet

`CycleBottomSheet` is the modal bottom sheet (`BottomSheet.kt`): a `surfaceContainer` panel with 32dp
top corners (`shapes.extraLarge`) and `elevation.level2`, sliding up over the `scrim` in a window of
its own. It shows a drag handle, the title as a heading, and the content in a column that scrolls when
the sheet is full. Use it for a short task on top of a screen: logging a day, picking an option, a
note. Don't use it for a whole flow (that is a screen) or for a message that needs an answer (a
dialog).

```kotlin
val sheet = rememberCycleBottomSheetState()
val scope = rememberCoroutineScope()
FilledButton("Log today", onClick = { scope.launch { sheet.show() } })
CycleBottomSheet(sheet, title = "Log today", onDismiss = { /* closed, however it closed */ }) {
    CycleTextField(notes, label = "Notes")
    FilledButton("Log it", onClick = { scope.launch { sheet.hide() } }, modifier = Modifier.fillMaxWidth())
}
```

Call `CycleBottomSheet` wherever the screen's content is; it shows nothing until the state opens it.
`CycleBottomSheetState` is the state holder:

| Member | What it does |
| -- | -- |
| `show()` | Opens the sheet partially expanded (its top at half the screen) when it is taller than that, expanded otherwise. |
| `expand()`, `partialExpand()` | Move an open sheet, or open a closed one, to that anchor. |
| `hide()` | Slides the sheet away, then closes its window. `onDismiss` runs. |
| `isVisible`, `currentValue`, `targetValue` | Whether the window is up, and the `CycleSheetValue` (`Hidden`, `PartiallyExpanded`, `Expanded`) it rests at or moves to. |

`rememberCycleBottomSheetState(skipPartiallyExpanded = true)` makes a sheet that only opens expanded.
The state survives configuration changes.

**Closing.** People close the sheet by dragging it down, tapping the scrim, going back, or with the
handle's Dismiss accessibility action. Predictive back narrows the sheet and moves it down as the back
gesture progresses; letting go closes it and cancelling springs it back. `onDismiss` runs once the
sheet has closed, whichever way it closed, including `hide()`.

**Motion.** Opening, closing and settling after a drag or fling use the slow spatial spring, so the
sheet bounces a little past its anchor (it fills the gap that opens below it). Under reduce motion
the spring is `snap()` and the sheet jumps.

**Drag handle.** A 32 by 4dp `onSurfaceVariant` pill in a 48dp target. Tapping it expands a partially
expanded sheet and collapses an expanded one (or closes it, when there is no partial anchor). TalkBack
reads "Drag handle", "button" and the state ("Partially expanded", "Expanded"), and offers Expand or
Collapse, and Dismiss, as custom actions. It shows the focus ring when focused from a keyboard.

**Insets and the keyboard.** The sheet's window draws edge to edge. The sheet stops below the status
bar, pads its content above the navigation bar, and sits above the keyboard (`imePadding()`). When
the keyboard opens, a partially expanded sheet expands, and the focused field scrolls into view.
While the keyboard is up, the handle and title scroll with the content, so a short window (a phone
on its side) keeps its room for the field. `BottomSheetImeTest` checks this by dispatching keyboard
insets to the sheet's window.

**Focus and TalkBack.** Opening moves focus into the sheet: onto the handle for someone using a
keyboard, and to the sheet itself for touch. The opener in the screen behind keeps its focus, so it is
focused again when the sheet closes. The sheet is a pane titled with `title` (`paneTitle`), which
TalkBack announces when it opens, and it reads the handle, the title, then the content. The scrim is
hidden from TalkBack: use the handle's Dismiss action or back.

A partially expanded sheet runs off the bottom of the screen, so a control there can show only a
sliver. `BottomSheetScreenshotTest` suppresses the framework's touch target result for an element cut
by the bottom of the screen; the 48dp layout check still covers it.

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
   state. A component that needs the whole screen, such as the app bars, also passes a `demo`, which
   the page opens full screen. `CatalogScreenshotTest` captures each section in light, dark, 200% font scale and
   right-to-left; give the new section a page height there.
5. Add a section for the component to this file: what it is for, its parameters, and when not to
   use it.

## Feature code

Feature code uses design system components and tokens only. It never styles Foundation code ad hoc
(`Modifier.background(Color(...))`, `16.dp` padding, a hand-made `TextStyle`). If a screen needs
something the design system lacks, add the token or component here first.

## Strings and languages

Cycle speaks every language in `Language.Supported` (English only for now; Brazilian Portuguese,
Spanish and German come with MOT-93), as
[`0008-languages.md`](../decisions/0008-languages.md) decides. Every word a screen shows or TalkBack
reads is a string resource in its module's `res/values/strings.xml`, never a literal in code.

- **Every new user-visible string goes into every language in the same PR**: into `values` (English)
  and each `values-xx` folder (`values-pt-rBR`, `values-es`, `values-de`). Android Lint's
  `MissingTranslation` is an error, so a missing one fails the build. A translation not written by a
  native speaker is marked as a draft at the top of its file, and the PR lists the new strings for a
  native speaker to read (0008's "Review of translations").
- **The copy rules hold in each language.** The words the app never says, per language, are in
  `NeverSaid` in `core:testing` (`core/testing/src/main/kotlin/.../CopyRules.kt`), copied from 0008's
  "Copy checks in every language", with the glossary and typography rules beside them in 0008. Each
  module's `*StringsTest` runs `assertNeverSaid` over its strings and plurals in every language. A
  list changes only with 0008.
- **Text that is not language**, such as the app's name, is `translatable="false"`.
- **Dates and numbers** are formatted in `cycleLocale()`, the language of the strings Android picked,
  never in `LocalConfiguration.current.locales[0]` (`LocaleReadersTest` fails on it). The first day
  of the week is `firstDayOfWeek()`, from the phone. A date is formatted whole, with
  `DateFormat.getBestDateTimePattern`, never assembled from pieces, and placeholders are numbered
  (`%1$s`) so each language orders them its own way.
- **Screenshots in another language**: `@Config(qualifiers = "+de")` on a test, or `renderIn(language)`
  from `core:testing` before `setContent`.
- **A new language** is added to `Language.Supported`, `localeFilters` (`CycleLanguages.kt` in
  `build-logic`), `app`'s `res/xml/locales_config.xml`, every module's strings and `NeverSaid`, in one
  PR; `LanguagesTest` in `app` fails until all of them name it.
