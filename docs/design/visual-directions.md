# Visual directions

Three directions for Cycle's look, to choose from before the design system is built (MOT-4). Each
one has a board in [`directions/`](directions/): a self-contained HTML file to open in any browser,
with fonts and icons embedded and no network requests. Each board shows the sample screens at phone
size (360 × 780) in light and dark, the palette with contrast ratios, the type scale, the components
and the day cell in every cycle state. Buttons, chips and the motion strip can be pressed, to feel
each direction's spring and shape changes.

All dates, symptoms and moods on the boards are synthetic: a made-up March 2027 with a period from
2 to 6 March, a fertile window from 13 to 18, ovulation on 17, today on 20 and the next period
predicted from 29 March.

## What all three share

These follow from [`0001-stack.md`](../decisions/0001-stack.md) and hold whichever direction we pick.

- **Our own palette, no dynamic color.** Roles borrow Material 3's vocabulary (accent, container,
  surface, on-surface, outline, error) plus cycle roles: `period`, `predicted` with
  `predictedEdge`, `fertile`, `ovulation` and `today`. Every role has a light and a dark value.
- **Contrast.** Body text and important UI meet WCAG AA: 4.5:1 for text, 3:1 for the fills,
  edges and rings that carry meaning. Each direction's table lists the ratios.
- **No cycle state relies on colour alone.** Logged period is a solid fill, predicted period a
  dashed edge on a pale fill, the fertile window a soft tint plus a marker dot, ovulation a strong
  fill (and in Zest a distinct shape), today a ring with a bolder number. Each cell also carries a
  content description for TalkBack (`20 March, today`).
- **Period red and error red sit close** in every direction, as they would in any period tracker.
  Errors therefore always come with an icon and a sentence, never colour alone.
- **Fonts are open-licensed** (SIL OFL 1.1) variable fonts, bundled in the app. Variable fonts
  work through `FontVariation` from API 26, below our minimum SDK of 29. Material Symbols (Apache
  2.0) ship as vector drawables of the icons we use, not as a font.
- **Reduce motion.** When the system's animator duration scale is 0, springs become `snap()` and
  shape morphs jump to their end state. The boards honour `prefers-reduced-motion` the same way.
- **Copy says "period".** No euphemisms, no flowers or hearts standing in for the body, nothing
  that tells her how she should feel.

## 1. Ember: warm, editorial, grounded

Board: [`directions/ember.html`](directions/ember.html)

A well-made paper journal: cream pages, ink, a terracotta accent and a serif with a soft, human voice. Calm confidence rather than cute.

**How it differs from the usual period tracker.** Typical trackers are pastel pink with bubbly sans-serifs and hearts. Ember is earthy (cream, rust, pomegranate, sage), uses a serif display face, and draws the period as a continuous ribbon across the calendar instead of pink bubbles.

### Colour

| Role | Use | Light | Dark |
| -- | -- | -- | -- |
| `accent` | Primary actions, links, selection | `#A63D1F` | `#FFB59A` |
| `onAccent` | Text and icons on accent | `#FFFFFF` | `#5A1A04` |
| `accentContainer` | Tonal buttons, selected chips, nav indicator | `#FFDBCC` | `#7C2B12` |
| `onAccentContainer` | Text on accent container | `#3D1003` | `#FFDBCC` |
| `surface` | Screen background | `#FBF5EE` | `#1B1411` |
| `surfaceContainer` | Cards, navigation bar | `#F3EAE0` | `#261D19` |
| `surfaceContainerHigh` | Raised or pressed surfaces | `#EADFD3` | `#322722` |
| `onSurface` | Body text and icons | `#2A1D17` | `#F3E6DC` |
| `onSurfaceVariant` | Secondary text, captions | `#62524A` | `#D3C2B6` |
| `outline` | Control borders (chips, fields) | `#877669` | `#A08D80` |
| `outlineVariant` | Dividers, decorative borders | `#D9CABD` | `#52433A` |
| `error` | Error text and borders | `#B3261E` | `#FFB4AB` |
| `onError` | Text on error | `#FFFFFF` | `#690005` |
| `errorContainer` | Error banners | `#F9DEDC` | `#93000A` |
| `onErrorContainer` | Text on error container | `#410E0B` | `#FFDAD6` |
| `period` | Logged period day (fill) | `#9C2342` | `#F28BA0` |
| `onPeriod` | Day number on period | `#FFFFFF` | `#3F0614` |
| `predicted` | Predicted period day (fill) | `#F6DDE2` | `#45222B` |
| `predictedEdge` | Predicted period dashed edge | `#9C2342` | `#F28BA0` |
| `fertile` | Fertile window (tint) | `#DDE8CF` | `#2D3B24` |
| `onFertile` | Day number in fertile window | `#1F3214` | `#D3E6C2` |
| `ovulation` | Ovulation day (fill), fertile marker dot | `#4C6A35` | `#A9CB8C` |
| `onOvulation` | Day number on ovulation | `#FFFFFF` | `#16290A` |
| `today` | Today ring | `#2A1D17` | `#F3E6DC` |

Contrast (WCAG 2.x ratios, computed from the hex values above):

| Pair | Needs | Light | Dark |
| -- | -- | -- | -- |
| Body text on surface (`onSurface` on `surface`) | 4.5:1 | 15.08 | 14.86 |
| Secondary text on surface (`onSurfaceVariant` on `surface`) | 4.5:1 | 6.87 | 10.53 |
| Text on card (`onSurface` on `surfaceContainer`) | 4.5:1 | 13.72 | 13.49 |
| Secondary text on raised surface (`onSurfaceVariant` on `surfaceContainerHigh`) | 4.5:1 | 5.66 | 8.39 |
| Text on accent (primary button) (`onAccent` on `accent`) | 4.5:1 | 6.35 | 7.78 |
| Text on accent container (selected chip) (`onAccentContainer` on `accentContainer`) | 4.5:1 | 12.76 | 7.35 |
| Accent as text/link on surface (`accent` on `surface`) | 4.5:1 | 5.86 | 10.68 |
| Outline vs surface (control border) (`outline` on `surface`) | 3:1 | 4.02 | 5.73 |
| Error text on surface (`error` on `surface`) | 4.5:1 | 6.04 | 10.71 |
| Text on error (`onError` on `error`) | 4.5:1 | 6.54 | 7.72 |
| Text on error container (`onErrorContainer` on `errorContainer`) | 4.5:1 | 12.77 | 7.24 |
| Day number on period (`onPeriod` on `period`) | 4.5:1 | 7.68 | 7.20 |
| Period fill vs surface (`period` on `surface`) | 3:1 | 7.10 | 7.77 |
| Day number on predicted period (`onSurface` on `predicted`) | 4.5:1 | 12.72 | 11.32 |
| Predicted dashed edge vs surface (`predictedEdge` on `surface`) | 3:1 | 7.10 | 7.77 |
| Day number on fertile window (`onFertile` on `fertile`) | 4.5:1 | 10.83 | 9.00 |
| Day number on ovulation (`onOvulation` on `ovulation`) | 4.5:1 | 6.14 | 8.56 |
| Ovulation fill vs surface (`ovulation` on `surface`) | 3:1 | 5.67 | 10.05 |
| Fertile marker dot on fertile fill (`ovulation` on `fertile`) | 3:1 | 4.83 | 6.59 |
| Today ring vs surface (`today` on `surface`) | 3:1 | 15.08 | 14.86 |

### Typography

Display: [Fraunces](https://github.com/undercasetype/Fraunces). Text: [Figtree](https://github.com/erikdkennedy/figtree). Both SIL OFL 1.1 variable fonts, bundled as `res/font` files. Display uses the optical size axis at 144.

| Role | Font | Size / line (sp) | Weight | Tracking |
| -- | -- | -- | -- | -- |
| Display | Fraunces | 48 / 52 | 600 | -0.5sp |
| Headline | Fraunces | 30 / 36 | 500 | 0 |
| Title | Figtree | 20 / 26 | 650 | 0 |
| Title small | Figtree | 16 / 22 | 600 | 0 |
| Body | Figtree | 16 / 24 | 400 | 0 |
| Body small | Figtree | 14 / 20 | 400 | 0 |
| Label | Figtree | 14 / 20 | 600 | 0.1sp |
| Label small | Figtree | 12 / 16 | 600 | 0.4sp |
| Day number | Figtree | 15 / 20 | 600 | 0 |

### Shape

Radii: xs 4dp, s 8dp, m 12dp, l 20dp, xl 28dp. Buttons 12dp → 6dp pressed; chips 8dp → 16dp selected; cards 20dp.

Soft rectangles. Small radii on controls (8–12dp), larger on cards (20dp), nothing fully round except the ovulation dot. Period, fertile and predicted days join into ribbons across the week; pressing a button tightens its corners (12 → 6dp).

Day cells: period: ribbon joining consecutive days; predicted: ribbon joining consecutive days; fertile: ribbon joining consecutive days; ovulation: circle; today: rounded square (12dp).

### Motion

Settled. Springs with a hint of overshoot (damping 0.85, stiffness 380) for position and shape, no bounce on colour and opacity (damping 1.0). Things arrive and stop; nothing wobbles. Spatial spring: `spring(dampingRatio = 0.85f, stiffness = 380f)`; effects spring: `spring(dampingRatio = 1.0f, stiffness = 1600f)`.

### Icons

Material Symbols Outlined, weight 300, grade 0, optical size 24; filled only for the selected destination.

### Voice

Warm, plain and direct, like a friend who knows the facts. Full sentences, says “period”, never euphemisms. No exclamation marks.

| Moment | Copy |
| -- | -- |
| Today, phase line | Luteal phase · Your period is likely to start in about 9 days. |
| Cycle card | **Cycle day 19**. Next period around Mon 29 March. Fertile window ended 18 March. |
| Save button / dismiss | Save today / Not now |
| Notes placeholder | Anything else worth remembering? |
| Error (future date) | That date is in the future. Pick today or an earlier day. |

## 2. Tide: calm, lunar, quiet

Board: [`directions/tide.html`](directions/tide.html)

Night sky and moonlight: deep indigo, cool greys, a geometric grotesque and round everything. Few words, lots of air. Feels like checking the moon, not a medical chart.

**How it differs from the usual period tracker.** Instead of pink and florals, Tide is built on indigo and night blues, with coral reserved for the period so it reads as a signal rather than decoration. Shapes are pure circles and pills, like moon phases, and copy is as short as possible.

### Colour

| Role | Use | Light | Dark |
| -- | -- | -- | -- |
| `accent` | Primary actions, links, selection | `#3949C2` | `#BAC3FF` |
| `onAccent` | Text and icons on accent | `#FFFFFF` | `#15228A` |
| `accentContainer` | Tonal buttons, selected chips, nav indicator | `#DEE0FF` | `#2D3BA8` |
| `onAccentContainer` | Text on accent container | `#0B1667` | `#DEE0FF` |
| `surface` | Screen background | `#F6F7FC` | `#0E1120` |
| `surfaceContainer` | Cards, navigation bar | `#ECEEF7` | `#171B2D` |
| `surfaceContainerHigh` | Raised or pressed surfaces | `#E1E4F0` | `#21263B` |
| `onSurface` | Body text and icons | `#161A2B` | `#E3E5F4` |
| `onSurfaceVariant` | Secondary text, captions | `#484E66` | `#B3B8CF` |
| `outline` | Control borders (chips, fields) | `#737992` | `#888EA8` |
| `outlineVariant` | Dividers, decorative borders | `#C5C9DA` | `#383E56` |
| `error` | Error text and borders | `#B3261E` | `#FFB4AB` |
| `onError` | Text on error | `#FFFFFF` | `#690005` |
| `errorContainer` | Error banners | `#F9DEDC` | `#93000A` |
| `onErrorContainer` | Text on error container | `#410E0B` | `#FFDAD6` |
| `period` | Logged period day (fill) | `#C2304B` | `#FF8A9B` |
| `onPeriod` | Day number on period | `#FFFFFF` | `#3B0711` |
| `predicted` | Predicted period day (fill) | `#FBE2E6` | `#3A1D29` |
| `predictedEdge` | Predicted period dashed edge | `#C2304B` | `#FF8A9B` |
| `fertile` | Fertile window (tint) | `#CDEEEA` | `#11403B` |
| `onFertile` | Day number in fertile window | `#00352F` | `#A6F0E6` |
| `ovulation` | Ovulation day (fill), fertile marker dot | `#00796B` | `#5FD6C6` |
| `onOvulation` | Day number on ovulation | `#FFFFFF` | `#00201C` |
| `today` | Today ring | `#3949C2` | `#BAC3FF` |

Contrast (WCAG 2.x ratios, computed from the hex values above):

| Pair | Needs | Light | Dark |
| -- | -- | -- | -- |
| Body text on surface (`onSurface` on `surface`) | 4.5:1 | 16.13 | 14.99 |
| Secondary text on surface (`onSurfaceVariant` on `surface`) | 4.5:1 | 7.68 | 9.53 |
| Text on card (`onSurface` on `surfaceContainer`) | 4.5:1 | 14.91 | 13.63 |
| Secondary text on raised surface (`onSurfaceVariant` on `surfaceContainerHigh`) | 4.5:1 | 6.47 | 7.60 |
| Text on accent (primary button) (`onAccent` on `accent`) | 4.5:1 | 7.23 | 7.58 |
| Text on accent container (selected chip) (`onAccentContainer` on `accentContainer`) | 4.5:1 | 12.24 | 7.04 |
| Accent as text/link on surface (`accent` on `surface`) | 4.5:1 | 6.75 | 11.02 |
| Outline vs surface (control border) (`outline` on `surface`) | 3:1 | 4.02 | 5.79 |
| Error text on surface (`error` on `surface`) | 4.5:1 | 6.11 | 11.05 |
| Text on error (`onError` on `error`) | 4.5:1 | 6.54 | 7.72 |
| Text on error container (`onErrorContainer` on `errorContainer`) | 4.5:1 | 12.77 | 7.24 |
| Day number on period (`onPeriod` on `period`) | 4.5:1 | 5.50 | 7.64 |
| Period fill vs surface (`period` on `surface`) | 3:1 | 5.14 | 8.36 |
| Day number on predicted period (`onSurface` on `predicted`) | 4.5:1 | 14.07 | 12.09 |
| Predicted dashed edge vs surface (`predictedEdge` on `surface`) | 3:1 | 5.14 | 8.36 |
| Day number on fertile window (`onFertile` on `fertile`) | 4.5:1 | 10.97 | 8.91 |
| Day number on ovulation (`onOvulation` on `ovulation`) | 4.5:1 | 5.32 | 9.74 |
| Ovulation fill vs surface (`ovulation` on `surface`) | 3:1 | 4.97 | 10.65 |
| Fertile marker dot on fertile fill (`ovulation` on `fertile`) | 3:1 | 4.31 | 6.55 |
| Today ring vs surface (`today` on `surface`) | 3:1 | 6.75 | 11.02 |

### Typography

Display: [Space Grotesk](https://github.com/floriankarsten/space-grotesk). Text: [Inter](https://github.com/rsms/inter). Both SIL OFL 1.1 variable fonts, bundled as `res/font` files.

| Role | Font | Size / line (sp) | Weight | Tracking |
| -- | -- | -- | -- | -- |
| Display | Space Grotesk | 56 / 60 | 300 | -1sp |
| Headline | Space Grotesk | 28 / 34 | 400 | -0.25sp |
| Title | Space Grotesk | 20 / 26 | 500 | 0 |
| Title small | Inter | 16 / 22 | 600 | 0 |
| Body | Inter | 16 / 24 | 400 | 0 |
| Body small | Inter | 14 / 20 | 400 | 0 |
| Label | Inter | 14 / 20 | 500 | 0.1sp |
| Label small | Inter | 12 / 16 | 500 | 0.5sp |
| Day number | Inter | 15 / 20 | 500 | 0 |

### Shape

Radii: xs 8dp, s 12dp, m 20dp, l 28dp, xl full. Buttons full → 16dp pressed; chips full → full selected; cards 28dp.

Circles and pills only. Controls are fully rounded; cards 28dp. Day cells are circles; the fertile window is one capsule that runs under its days, like a phase of the moon. Pressing a pill button flattens it to 16dp.

Day cells: period: circle; predicted: circle; fertile: capsule running under its days; ovulation: circle; today: circle.

### Motion

Fluid and unhurried. Critically damped springs (damping 1.0) with low stiffness (200) for spatial moves, so nothing overshoots; long cross-fades between screens. Motion is the slowest of the three. Spatial spring: `spring(dampingRatio = 1.0f, stiffness = 200f)`; effects spring: `spring(dampingRatio = 1.0f, stiffness = 800f)`.

### Icons

Material Symbols Rounded, weight 300, grade 0, optical size 24; filled for the selected destination.

### Voice

Quiet and brief. Short phrases over sentences, numbers first, no exclamation marks, never tells her how to feel.

| Moment | Copy |
| -- | -- |
| Today, phase line | Luteal · Period in about 9 days. |
| Cycle card | **Day 19 of about 27**. Next period ~ Mon 29 Mar. Fertile window closed 18 Mar. |
| Save button / dismiss | Save / Later |
| Notes placeholder | Note |
| Error (future date) | Pick a day up to today. |

## 3. Zest: bold, playful, upbeat

Board: [`directions/zest.html`](directions/zest.html)

Sunshine and colour: grape violet, tomato red, sunflower amber and mint on warm white, with a chunky condensed grotesque. The most Material 3 Expressive of the three: shapes morph, things bounce a little.

**How it differs from the usual period tracker.** Typical trackers are soft and pastel; Zest is saturated and graphic, closer to a sticker sheet or a poster than a diary. It uses expressive shapes (a sun-shaped ovulation day, squircles) instead of hearts and flowers, and treats the body plainly while still being fun.

### Colour

| Role | Use | Light | Dark |
| -- | -- | -- | -- |
| `accent` | Primary actions, links, selection | `#6A2BD6` | `#D3BBFF` |
| `onAccent` | Text and icons on accent | `#FFFFFF` | `#3F008D` |
| `accentContainer` | Tonal buttons, selected chips, nav indicator | `#EBDDFF` | `#5418BE` |
| `onAccentContainer` | Text on accent container | `#25005A` | `#EBDDFF` |
| `surface` | Screen background | `#FFFCF4` | `#17130C` |
| `surfaceContainer` | Cards, navigation bar | `#FFF2D9` | `#221C12` |
| `surfaceContainerHigh` | Raised or pressed surfaces | `#FCE6BE` | `#2E271A` |
| `onSurface` | Body text and icons | `#1F1A10` | `#F2E8D5` |
| `onSurfaceVariant` | Secondary text, captions | `#584C33` | `#D2C4A5` |
| `outline` | Control borders (chips, fields) | `#827455` | `#9B8D6E` |
| `outlineVariant` | Dividers, decorative borders | `#E1D0AA` | `#4D4430` |
| `error` | Error text and borders | `#B3261E` | `#FFB4AB` |
| `onError` | Text on error | `#FFFFFF` | `#690005` |
| `errorContainer` | Error banners | `#F9DEDC` | `#93000A` |
| `onErrorContainer` | Text on error container | `#410E0B` | `#FFDAD6` |
| `period` | Logged period day (fill) | `#D1342A` | `#FF8B7B` |
| `onPeriod` | Day number on period | `#FFFFFF` | `#410200` |
| `predicted` | Predicted period day (fill) | `#FFDDD6` | `#4A1E18` |
| `predictedEdge` | Predicted period dashed edge | `#D1342A` | `#FF8B7B` |
| `fertile` | Fertile window (tint) | `#C4F0D6` | `#1D4A32` |
| `onFertile` | Day number in fertile window | `#00391F` | `#B4F2CE` |
| `ovulation` | Ovulation day (fill), fertile marker dot | `#B35A00` | `#FFC24A` |
| `onOvulation` | Day number on ovulation | `#FFFFFF` | `#2B1F00` |
| `today` | Today ring | `#1F1A10` | `#F2E8D5` |
| `scrim` | Dims the screen behind a sheet or dialog | `#1F1A10` at 32% | `#000000` at 60% |

Contrast (WCAG 2.x ratios, computed from the hex values above):

| Pair | Needs | Light | Dark |
| -- | -- | -- | -- |
| Body text on surface (`onSurface` on `surface`) | 4.5:1 | 16.88 | 15.23 |
| Secondary text on surface (`onSurfaceVariant` on `surface`) | 4.5:1 | 8.20 | 10.73 |
| Text on card (`onSurface` on `surfaceContainer`) | 4.5:1 | 15.62 | 13.90 |
| Secondary text on card, navigation label, sheet handle, dialog body (`onSurfaceVariant` on `surfaceContainer`) | 4.5:1 | 7.59 | 9.80 |
| Secondary text on raised surface (`onSurfaceVariant` on `surfaceContainerHigh`) | 4.5:1 | 6.89 | 8.57 |
| Text on accent (primary button) (`onAccent` on `accent`) | 4.5:1 | 7.17 | 7.72 |
| Text on accent container (selected chip) (`onAccentContainer` on `accentContainer`) | 4.5:1 | 13.31 | 7.25 |
| Accent as text/link on surface (`accent` on `surface`) | 4.5:1 | 6.99 | 10.86 |
| Accent as text on card, text button in a sheet or dialog (`accent` on `surfaceContainer`) | 4.5:1 | 6.47 | 9.91 |
| Outline vs surface (control border) (`outline` on `surface`) | 3:1 | 4.47 | 5.66 |
| Error text on surface (`error` on `surface`) | 4.5:1 | 6.38 | 10.90 |
| Error text on card (destructive dialog icon) (`error` on `surfaceContainer`) | 4.5:1 | 5.90 | 9.95 |
| Text on error (`onError` on `error`) | 4.5:1 | 6.54 | 7.72 |
| Text on error container (`onErrorContainer` on `errorContainer`) | 4.5:1 | 12.77 | 7.24 |
| Day number on period (`onPeriod` on `period`) | 4.5:1 | 4.96 | 7.48 |
| Period fill vs surface (`period` on `surface`) | 3:1 | 4.84 | 8.13 |
| Day number on predicted period (`onSurface` on `predicted`) | 4.5:1 | 13.65 | 11.60 |
| Predicted dashed edge vs surface (`predictedEdge` on `surface`) | 3:1 | 4.84 | 8.13 |
| Day number on fertile window (`onFertile` on `fertile`) | 4.5:1 | 10.46 | 7.96 |
| Day number on ovulation (`onOvulation` on `ovulation`) | 4.5:1 | 4.80 | 10.06 |
| Ovulation fill vs surface (`ovulation` on `surface`) | 3:1 | 4.68 | 11.51 |
| Fertile marker dot on fertile fill (`ovulation` on `fertile`) | 3:1 | 3.83 | 6.29 |
| Today ring vs surface (`today` on `surface`) | 3:1 | 16.88 | 15.23 |

### Typography

Display: [Bricolage Grotesque](https://github.com/ateliertriay/bricolage). Text: [DM Sans](https://github.com/googlefonts/dm-fonts). Both SIL OFL 1.1 variable fonts, bundled as `res/font` files. Display uses the width axis at 75 (condensed) and optical size 96.

| Role | Font | Size / line (sp) | Weight | Tracking |
| -- | -- | -- | -- | -- |
| Display | Bricolage Grotesque | 60 / 60 | 800 | -1sp |
| Headline | Bricolage Grotesque | 32 / 36 | 750 | -0.5sp |
| Title | Bricolage Grotesque | 22 / 28 | 700 | 0 |
| Title small | DM Sans | 16 / 22 | 700 | 0 |
| Body | DM Sans | 16 / 24 | 400 | 0 |
| Body small | DM Sans | 14 / 20 | 400 | 0 |
| Label | DM Sans | 14 / 20 | 700 | 0.1sp |
| Label small | DM Sans | 12 / 16 | 700 | 0.4sp |
| Day number | DM Sans | 15 / 20 | 700 | 0 |

### Shape

Radii: xs 8dp, s 12dp, m 16dp, l 24dp, xl 32dp. Buttons full → 14dp pressed; chips 12dp → full selected; cards 32dp.

Chunky and mixed, built with graphics-shapes in the spirit of Material’s shape set: squircles for period days, a soft eight-point sun for ovulation, pills for buttons, 32dp cards. Selection is a shape change: chips go from 12dp to a pill, buttons squash from pill to 14dp when pressed.

Day cells: period: squircle (14dp); predicted: squircle (14dp); fertile: circle; ovulation: soft 8-point sun (graphics-shapes star); today: circle.

### Motion

Bouncy but quick. Underdamped springs (damping 0.6, stiffness 500) for shape and position, so selections overshoot and settle; colour changes stay critically damped. Logging a day gets a small celebratory morph. Spatial spring: `spring(dampingRatio = 0.6f, stiffness = 500f)`; effects spring: `spring(dampingRatio = 1.0f, stiffness = 1600f)`.

Fast and slow speeds keep the damping and take Material 3 Expressive's stiffnesses: spatial 800 (fast) and 200 (slow), effects 3800 (fast) and 800 (slow). Pressed controls scale to 94%; focus shows a 3dp `accent` ring 2dp outside the control.

### Icons

Material Symbols Rounded, weight 600, filled, grade 0, optical size 24, matching the heavy type.

### Voice

Upbeat and a little cheeky, short and active. Plain words about the body, never cutesy names for it. Humour in labels, never in health information.

| Moment | Copy |
| -- | -- |
| Today, phase line | Luteal phase · Period’s due in about 9 days. |
| Cycle card | **Day 19 — nice and steady**. Next period around Mon 29 Mar. Fertile window wrapped up on 18 Mar. |
| Save button / dismiss | Log it / Nah |
| Notes placeholder | Anything else? Spill it here. |
| Error (future date) | Whoa, that’s the future. Pick today or earlier. |
## Recommendation

### Material 3 Expressive: what to adopt and how

We take the patterns, not the library. Everything below builds on Compose Foundation, Compose
animation and `androidx.graphics:graphics-shapes`, none of which depend on Material.

| Pattern | Adopt? | How it maps onto Compose Foundation |
| -- | -- | -- |
| Spring motion scheme (spatial and effects springs, in fast, default and slow speeds) | Yes | A `CycleMotion` token object in `core:designsystem`, provided through a `CompositionLocal`, holding `SpringSpec`s built with `spring(dampingRatio, stiffness)` from `androidx.compose.animation.core`. Components use them through `animate*AsState`, `Animatable`, `AnimatedContent` and `AnimatedVisibility`. Spatial springs (position, size, shape) may overshoot; effects springs (colour, opacity) never do. A reduce-motion check swaps every spec for `snap()`. |
| Shape morphing | Yes, where it means something | `RoundedPolygon` and `Morph` from `graphics-shapes`. Its builders (`circle`, `rectangle`, `star`, `pill`, `pillStar`) cover the shapes we need; Material's named shapes, such as Cookie, are only `star` recipes we can rebuild without `material3`. The morph becomes an Android `Path` with `Morph.toPath(progress)`, then a Compose path with `asComposePath()`, inside our own `Shape` whose `createOutline` returns `Outline.Generic`. Progress is an `Animatable` driven by the spatial spring. Used for the ovulation and today cells, the "day logged" confirmation, and the loading indicator if we need one. |
| Shape change on press and selection (buttons, chips, toggle groups) | Yes | Plain corner animation, cheaper than a full morph: animate the corner size of a Foundation `RoundedCornerShape` from `MutableInteractionSource.collectIsPressedAsState()` or the selected state. |
| Shape scale (corner tokens from extra small to full) | Yes | A `CycleShapes` token set of `RoundedCornerShape`s; each direction above lists its values. |
| Emphasized type styles | Yes | Each type role gets an emphasized variant (heavier weight, or tighter width in Zest), used for the cycle day and other key numbers. Built as our own `TextStyle` tokens with `FontVariation.Settings`. |
| Connected button groups | Yes, for flow intensity | A `Row` of `Modifier.selectable` items with `Role.RadioButton`. The pressed item widens a little and its neighbours make room, using the spatial spring. |
| Floating toolbar / floating navigation bar | Direction-dependent | Tide and Zest float the navigation bar as a rounded container, Ember docks it. Either way it's our own component, padded by `WindowInsets.navigationBars`. |
| State layers and ripple | Replace | Our own `IndicationNodeFactory` with a tint and, in Ember and Zest, a slight scale on press, as the stack decision already requires. |
| Expressive loading indicator (morphing shapes) | Later | Only once something loads slowly enough to need it. Built from the same `Morph` shape. |
| Dynamic color | No | Ruled out: the palette belongs to the app. |
| Tonal palettes generated from a seed colour (HCT) | No | The palettes above are hand-tuned and checked for contrast. Seed tools may help explore at design time, but they don't ship. |
| Wavy progress indicators, FAB menu, split button, carousel, flexible large app bars | No | Decorative or built for problems the app doesn't have: it has one main action per screen and no content feeds. Revisit if a feature needs one. |
| Material components themselves | No | No dependency on `material` or `material3`, as the stack decision says. |

### Pick: Ember

I'd choose **Ember**.

- **It has the most personality without shouting.** Cream, ink, terracotta and a soft serif look
  like nothing else in the category, yet still feel calm enough to open every day for years.
  Zest is fun but tiring to live with daily; Tide is lovely but sits close to every meditation and
  sleep app.
- **The period colour belongs to the palette.** In a warm palette, pomegranate red is part of
  the family instead of an alarm on a cool or pastel background, so the most important state
  looks good as well as clear.
- **The ribbon calendar reads at a glance.** Consecutive period, fertile and predicted days join
  into bands, so the phases show as shapes across the month, not as scattered dots.
- **It leaves the most headroom.** Its text pairs clear AA by the widest margin (the lowest is
  5.66:1), and its settled springs fit a tool used for a few seconds a day. Expressive moments
  (the logged-day morph, pressed buttons tightening) are kept for where they add meaning.

Caveats to settle in MOT-4: keep Fraunces to 20sp and up, so the small text is all Figtree, and
check the ribbon with right-to-left layout and at 200% font scale. If she wants more joy than
Ember gives, Zest's sun-shaped ovulation day and bouncier chips carry over without changing the
palette.

## Out of scope

Compose code, the logo, the app icon and illustrations, as the ticket says.
