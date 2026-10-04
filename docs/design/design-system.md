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
   and Roborazzi screenshots of its states.
4. Add a catalog entry: one `CatalogSection` in `app-catalog`'s `CatalogSections` showing every
   state. `CatalogScreenshotTest` captures each section in light, dark, 200% font scale and
   right-to-left; give the new section a page height there.
5. Add a section for the component to this file: what it is for, its parameters, and when not to
   use it.

## Feature code

Feature code uses design system components and tokens only. It never styles Foundation code ad hoc
(`Modifier.background(Color(...))`, `16.dp` padding, a hand-made `TextStyle`). If a screen needs
something the design system lacks, add the token or component here first.
