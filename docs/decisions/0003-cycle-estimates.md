# 0003: Cycle data and estimates

**Status:** accepted, 2026-10-04

## Context

Every MVP screen (MOT-30) reads the same things: what she logged each day, her periods and cycles,
and when her next period should come. [`predictions.md`](../research/predictions.md) recommends an
approach, [`tracking-data.md`](../research/tracking-data.md) says how to store the log and
[`0001`](0001-stack.md) fixes Room, DataStore and `java.time`. This record turns those into rules
the code follows. Releases already ship to her phone, so the storage choices here are permanent
unless a migration changes them.

## Decisions

| Area | Decision |
| -- | -- |
| Modules | `core:model` (plain Kotlin types), `core:domain` (the cycle logic, plain Kotlin) and `core:data` (Room, DataStore, repositories). The first two are JVM modules, so the compiler keeps Android out of the logic and its tests run without Robolectric. `cycle.jvm.library` gives them a `testDebugUnitTest` task, so the repo-wide checks run their tests too. |
| What is stored | Only what she logged. Room table `day_log`, one row per day: `date` (primary key), `flow` (`none`, `spotting`, `light`, `medium`, `heavy`, or null when she logged nothing about it), `period_started` and `period_ended`. A day with nothing logged has no row. |
| What is derived | Periods, cycles, typical lengths, estimates and prompts, recomputed from the log on every change by `CycleCalculator` and never stored. Editing any past day recomputes every cycle after it. |
| Dates | `LocalDate` everywhere, stored as ISO text (`2027-03-02`): a day, with no time and no zone, which sorts in date order. A log made at 23:30 stays on that day after the phone changes time zone. The caller passes "today" from its clock in the phone's current zone. Logs after today are ignored until that day comes. |
| Stored values | Dates and flow are written as fixed text by explicit converters, not enum names or ordinals, so renaming a Kotlin constant cannot change what is on her phone. |
| Feelings, later | The "how she feels" categories (MOT-30 J10) arrive as their own tables keyed by the same `date`, so each is a migration that adds a table. |
| Schema changes | Every version is exported to `core/data/schemas/` and committed. A change raises the version, adds a migration to `CycleMigrations.ALL` and goes through `CycleDatabaseMigrationTest`, which migrates every earlier version to the current one, validates the result and reads her log back. The test also pins the hash of each released schema, so a shipped version cannot be edited in place. There is no destructive fallback: a missing migration fails rather than erasing her history. |
| Settings | DataStore: usual cycle length (default 28), usual period length (default 5), whether setup is done, whether she is past the first-run welcome (set by setup's Done and by Skip, so skipping keeps the typical lengths), and the prompts she dismissed, each set keyed by the first day of its cycle. Setup accepts a usual cycle of 15 to 90 days and a usual period of 1 to 14 (`CycleRules`): unusual lengths are hers to give, impossible ones are refused with the range to use. |
| A period | A run of period days, allowing a gap of one day; spotting alone never starts one. A period day is a day with light or heavier flow, or with either marker. A gap day may be unlogged, `none` or spotting. |
| Started and ended | "Ended" marks a period's last day: the next period day starts a new period. "Ended" alone, with no light or heavier flow and no "started", never starts a period: it closes the period before it if that one has not ended yet, which then runs up to that day however many days lie between, the way "started" and "ended" fill the days between them. When the period before it has already ended, or there is none, the day is ignored. "Started" without "ended" keeps the period open: it takes in every later day, and the last period runs up to today. A new "started" after more than a day's gap starts a new period, and the open one before it ends on its last logged day. |
| A cycle | From one period's first day to the day before the next. The last cycle is open until the next period is logged. |
| Her typical lengths | The median, shortest and longest of her last 6 complete cycles, and of her last 6 periods that are over (marked ended, followed by another period, or too long ago for another day to join them). An even count's median is the mean of the two middle values, a half day rounded up. |
| Expected start | Last period's first day + her median cycle. |
| Range | Last start + her shortest to longest cycle. No complete cycle: her setup length ± 4 days (28 before setup). One or two cycles: at least ± 3 days around the expected start, wider where she was. |
| Expected length | The median of her periods, or her setup length (5 before setup). |
| Basis | Every estimate says where its numbers come from: `Typical` (no setup, 28 and 5), `Setup` (her setup lengths) or `Logged(n)` (her last n cycles, or periods for the length). The cycle and the period length each carry their own, because she can have ended periods before any complete cycle. |
| Late | Never in the past. Once the expected start has passed with no new period, the next period is expected today, and `daysLate` counts the days since the expected start. The range is clamped to start no earlier than today. |
| Further ahead | Three periods: the next, then each one a median cycle after the one before, with the same range around it. Once late, they follow from today. |
| "Still going?" | Asked when an open period reaches her usual period length + 2 days: her median period, or her setup length. |
| "Missed a period?" | Asked when her cycle reaches 1.8 times her usual cycle (her median, or her setup length), outside an open period. With a 28-day median, that is cycle day 51. |
| Asked once | A dismissed prompt is stored under the first day of its cycle and not asked again in that cycle. |
| Thresholds | All in `CycleRules` in `core:domain`, each with its source in a comment. |
| Privacy | Everything stays on the phone: no network permission and no SDK beyond Room, DataStore and Kotlin coroutines. Tests use synthetic dates only. |

## Options considered

- **Storing periods (start and end dates) instead of days.** Simpler queries, but the period and
  the days could disagree, and editing one day would need to fix the period by hand.
  [`tracking-data.md`](../research/tracking-data.md) asks for days as the source.
- **Storing dates as epoch days or timestamps.** Epoch days are compact, but a timestamp drifts with
  the time zone, and ISO text reads the same in an export, a database browser and a bug report.
- **Mean instead of median.** One forgotten period doubles a cycle and moves a mean by days; the
  median barely moves ([`predictions.md`](../research/predictions.md)).
- **All her cycles instead of the last six.** Cycles drift with age and life stage
  ([`cycle-physiology.md`](../research/cycle-physiology.md)); six recent ones follow her as she is,
  and match the calendar rhythm rule.
- **Leaving out a cycle about twice her usual length until she confirms it.** `predictions.md`
  suggests asking first. For now the "missed a period?" prompt asks while that cycle is still open,
  and a long cycle she did not log a period in still counts: the median resists one such cycle, but
  her range widens until it drops out of the last six. Leaving it out needs a stored "confirmed"
  answer per cycle; worth doing if the wide range bothers her.
- **Ranges that widen for each cycle further ahead.** More honest about compounding uncertainty,
  but three ever-wider ranges crowd the calendar. Each later period keeps the next period's range.
- **Moving the whole range forward once late.** It would invent a new window; clamping keeps the
  range she was shown, cut at today.
- **"Still going?" at her setup length only.** Her own periods are a better guide once she has
  them, as with every other estimate.
- **Spotting or an explicit "none" ending a period.** A period with one lighter day is still one
  period; only "ended" ends it early.
- **Ignoring "ended" alone after an unlogged gap.** Safer against a stray tap, but it throws away
  what she said: that the period ran until that day. Filling the days up to it keeps her period
  length right, and a mistaken tap is fixed by removing it, which recomputes everything after it.

## Consequences

- Screens read `CycleRepository.observeOverview(today)` and write through `DayLogRepository` and
  `SettingsRepository`; none of them computes a cycle itself. A screen that also shows what she
  logged on the day, such as Today's Undo, reads `CycleRepository.observeDay(today)`: the overview
  and the day's log from one read, so they never disagree for a moment after a write.
- Screens that show and edit any day, the calendar and the day log sheet, read
  `CycleRepository.observeLog(today)`: the overview with every logged day. Edits that touch more than
  one day go through `DayLogEdits` in `core:domain` and are written in one transaction
  (`DayLogRepository.fillPeriod`, `clearDay`), so no screen sees half an edit:
  - "Period started this day: fill in N days" (N her usual period length, as the estimates use it)
    is offered on a day up to today with no period within a day's gap of the days it would fill. It
    marks the day started and the Nth day ended, as Today's "Log a period" does; a fill that reaches
    today is still going. A stray "ended" on a day before the Nth, left by earlier clears, is removed
    so it cannot close the period early.
  - "Clear this day" removes what she logged and takes the day off its period while the rest stays:
    on the first day, "started" moves to the next day that still draws as a period day; on the last,
    the nearest such day before is marked ended, which also closes a period still going today. Days
    she cleared before are skipped, so a marker never puts them back on the period. A day in the
    middle is logged as no flow, a gap day: splitting the period instead would invent a cycle of a
    few days and skew every estimate. When that gap day would still cut the period short (a second
    cleared day next to it in a period logged as flow alone, or the day held its "started"), the
    period's first day is marked started and, unless it is still going, its last day ended, so it
    keeps the same days.
  - The calendar draws a day logged with no period flow and no marker (none or spotting) as plain,
    even inside a period, so a cleared middle day shows as cleared while the period stays one.
- Life stages are date ranges in their own table, and `CycleCalculator` leaves the cycles inside them
  out of the last six. Hormonal contraception is the first, decided in [`0006`](0006-contraception.md):
  the `contraception` table holds one row per stretch on a method, written through
  `ContraceptionRepository`, and `CycleCalculator.overview` takes the stretches with the log, so
  editing or deleting a stretch recomputes every cycle after it like editing a day. The overview
  carries the method in force, which estimate applies and what each method shows instead. Pregnancy,
  after birth and other cycles to exclude are not modelled yet.
- Fertile window and ovulation estimates are not part of this record.
