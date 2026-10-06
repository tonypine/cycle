# 0006: Contraception: what Cycle records, says and estimates on each method

**Status:** accepted, 2026-10-05.
[MOT-51](https://linear.app/tonypine/issue/MOT-51) stores the stretches and computes the estimates;
the screens follow this record and [`contraception.md`](../design/contraception.md).

## Context

She asked for Cycle to know whether she has an implant or takes the pill
([MOT-48](https://linear.app/tonypine/issue/MOT-48)). Each method changes bleeding differently, and
most of what Cycle shows today (cycle day, the next period, "late", "Missed a period?") assumes a
natural cycle. The research is [`contraception.md`](../research/contraception.md)
([MOT-49](https://linear.app/tonypine/issue/MOT-49)); the user stories, journeys and copy are in
[`docs/design/contraception.md`](../design/contraception.md), with screens in
[`journeys/contraception.html`](../design/journeys/contraception.html). This record turns them into
rules, on top of [`0003`](0003-cycle-estimates.md), whose consequences left life stages for later.

Cycle records the method she uses so its words and estimates fit her. It never acts as
contraception, never says whether she is protected, never calls a day safe, and never suggests
choosing, starting, changing or stopping a method ([`product-implications.md`](../research/product-implications.md)).

## Decision

### Three behaviours

Every method falls in one of three behaviours. The code keeps the method she chose, and derives the
behaviour from it.

| Behaviour | Methods | Her bleeding is called | Next-bleed estimate | Cycle day, "late", "Missed a period?" | In her typical cycle |
| -- | -- | -- | -- | -- | -- |
| **Own cycle** | None; copper IUD | Period | As in `0003` | As in `0003` | Yes |
| **Scheduled bleed** | Combined pill, patch or ring, with a break every month | Bleed | In the break, from the pack (below) | No; "Day N of your bleed" while bleeding | No |
| **No estimate** | Progestogen-only pill, implant, hormonal IUD, injection; combined pill, patch or ring with a break every few packs or no breaks | Bleeding | None | No; "Day N of bleeding" while bleeding | No |

Fertile window and ovulation estimates never show on a hormonal method, even after they exist for
natural cycles. On a copper IUD they follow her answer to the first open question in
`product-implications.md`, as without a method.

### Per method

| Method | Asked when she sets it | Today | Calendar | History |
| -- | -- | -- | -- | -- |
| None | Nothing | As now: "Day 19", next period | Periods, predicted periods | Cycles, typical cycle |
| Copper IUD | Date fitted | As now, plus "Periods can be heavier at first" for 6 months after fitting, until she taps Got it | As now | Cycles count. Typical period length from periods since fitting, once one has ended |
| Combined pill, patch, ring: a break every month | Start date; breaks | "Pill" (or "Patch", "Ring"); "Your next bleed is expected in about 6 days"; Next bleed card | Bleeds; expected bleeds, three ahead | One card for the time on it, its bleeds counted; left out of her typical cycle |
| Combined, a break every few packs, or no breaks | Start date; breaks | "Pill"; the calm line; Last 90 days card | Bleeding; nothing expected | One card for the time on it, with its last 90 days |
| Progestogen-only pill | Start date | "Mini pill"; the calm line; Last 90 days card | Bleeding; nothing expected | As above |
| Implant | Date fitted | "Implant"; the calm line; Last 90 days card | Bleeding; nothing expected | As above |
| Hormonal IUD | Date fitted | "Hormonal IUD"; the calm line; Last 90 days card, with the first-months line for 6 months | Bleeding; nothing expected | As above |
| Injection | Date of the first injection | "Injection"; the calm line; Last 90 days card | Bleeding; nothing expected | As above |

"The calm line" is the one sentence per method in [`contraception.md`](../design/contraception.md#the-calm-line-per-method)
that says what Cycle stops estimating and why, such as "Bleeding on the implant can come at any time,
so Cycle doesn't estimate it." On every method it is the body of the method's card in Settings; on
a no-estimate method it is also Today's line under the method's name while she is not bleeding.

The hormonal IUD gets no next-bleed estimate, not even later when her own bleeds look regular: the
research leaves it open, and offering it would need a rule for when bleeding counts as regular and a
second mode to switch to. Worth revisiting if she has a hormonal IUD and asks.

### Words

- "Period" with no method or a copper IUD; "bleed" for the scheduled bleed on a combined method with
  a break every month; "bleeding" everywhere else ([`product-implications.md`](../research/product-implications.md#on-contraception)).
- Every place that says "period" takes the word of the method on that day: Today's buttons ("Bleed
  started", "Bleeding started"), its cards and sheets, the day log's fill offer and clear dialog, the
  calendar legend and hint, History's labels, and TalkBack's day descriptions ("14 October,
  bleeding"). A day before she started a method keeps "period". A bleed that starts in the 7 days
  after a combined method stops keeps the method's word to its last day ([Stopping](#stopping)).
- No cycle language where there is no cycle: no cycle-day counter ("Day 19"), phases, "late",
  "Missed a period?" or "Still going?" on a no-estimate method. Counting the days of the bleeding
  itself ("Day 2 of bleeding") is fine, and "Still going?" becomes "Still bleeding?".

### What she logs

Nothing changes: one tap to start and end, the same day log, the same `day_log` rows. Only the words
and what is computed from the days differ. The day log's "fill in N days" offer appears on a
scheduled-bleed method only once she has logged an ended bleed on it (N is the median of those), and
never on a no-estimate method, where bleeding has no usual length.

### Stored: dated stretches

A Room table `contraception`, one row per stretch of time on one method, added by a migration
through `CycleMigrations.ALL` and `CycleDatabaseMigrationTest` like every schema change:

| Column | Type | Meaning |
| -- | -- | -- |
| `id` | integer, primary key | |
| `method` | text | `combined_pill`, `patch`, `ring`, `progestogen_pill`, `implant`, `hormonal_iud`, `copper_iud`, `injection`, written by an explicit converter as in `0003`. |
| `started` | ISO date, nullable | First day on the method. Null only when she skipped "since when" in setup: the stretch then covers every day before `stopped`. |
| `stopped` | ISO date, nullable | Last day on the method, included. Null while she is still on it. Only the injection's can be after today (below). |
| `breaks` | text, nullable | Combined pill, patch and ring only: `monthly`, `every_few_packs` or `none`. |

- **The current stretch.** At most one stretch covers today: the one with no stop date, or with a
  stop date today or later. That stretch is her method "Now" in Settings, on Today and everywhere
  else, until its stop date has passed.
- **No overlaps.** Starting or changing a method ends the current stretch the day before the new one
  starts, moving its stop date earlier if it has one. A new start can only be today or earlier, so
  it always falls before the current stretch ends. A new start on or before the current stretch's
  start is refused: "That's before you started the pill on 3 May. Pick a later day, or change the
  pill's dates first."
- **Adding a method while she is on none.** The new stretch starts on the day she picks, today or
  earlier, with no stop date. A start inside a stopped stretch would overlap it, so a dialog offers
  to move that stretch's end to the day before, as in Edits below. Implant 9 November 2026 to 3
  November 2027; on 15 November she adds the pill from 20 October: "Move the end of your implant?"
  "Your implant would end on 19 October instead of 3 November, so the two don't overlap. Cycle works
  out its estimates again." Cancel · **Move it**. Cancel goes back to the calendar. A start on or
  before a stopped stretch's start would cover all of it and is refused with the dates to use:
  "That would cover all of your time on the implant. Delete the implant's dates first, or pick a
  later day."
- **A start that reaches more than one stopped stretch.** The new stretch runs to today, so only the
  stretch holding its start can be cut short; every stretch after it would be covered whole. Any
  stretch covered whole refuses the start, with no move dialog, even when the start also lies inside
  another. The refusal names the latest stretch it would cover, because a day after that one's start
  is the earliest that can be saved. After the implant above, the ring from 20 November 2027 to 10
  January 2028; on 15 February 2028 she adds the pill from 1 November 2027: "That would cover all of
  your time on the ring. Delete the ring's dates first, or pick a later day." From 21 November to 10
  January, the dialog offers to move the ring's end; from 11 January, nothing overlaps.
- **Unknown start.** In every date comparison, here, in edits and in import, a null start counts as
  earlier than any date and a null stop as later than any. So a new start ends a stretch with an
  unknown start the day before (or, when she is on none, offers to, unless it would cover a later
  stretch whole, as above), and is never refused because of the unknown start itself.
- **Unknown start, in what she reads.** Where the copy prints the start date, a stretch with no
  start says so instead: Settings' row "Implant, start not known"; the Now card "Start not known"
  under the method's name (in place of "Since 9 November 2026"); "Your methods" and History's card
  "Start not known" while she is on it and "Until 3 Nov 2027" once it has stopped (in place of "9
  Nov 2026 to 3 Nov 2027"); the edit page "Fitted · Not known", which she can tap to set a date; the
  delete dialog "Cycle forgets you used the implant until 3 November 2027." (while on it, "Cycle
  forgets you use the implant."). Everything counted from the start date leaves it out: Today's
  bleeding card is always titled "Last 90 days", the first-months line and the copper IUD's
  heavier-periods card never show, no cycle is marked as cut short, and a combined method's first
  bleed is not estimated (below).
- **Edits.** Changing a stretch's start or stop recomputes everything, like editing a day. When the
  change would overlap a neighbour, a dialog offers to move the neighbour's edge ("Move the end of
  your pill?"); a change that would swallow a neighbour entirely is refused with the dates to use.
  A stopped date before the started date is refused.
- **Delete.** Deleting a stretch forgets the method for those dates. The days she logged stay and
  count as her own cycle again.
- **None** is no row: Cycle is on "none" on any day outside every stretch.
- **Injection.** Stopping asks for the date of her last injection; the stretch ends 13 weeks after
  it, the DMPA interval ([`contraception.md`](../research/contraception.md#injection)), so its stop
  date is usually still ahead. Until then she is still on the injection: Today, the calendar, History
  and Settings show it as her current method, with its words and its calm line, and Settings adds
  when Cycle counts it until. Starting or changing a method in those weeks moves the stop date to
  the day before the new start: last injection 3 August, stop date 2 November, implant fitted 31
  August, so the injection now ends on 30 August. She can edit the end, to any day up to 13 weeks
  after today, for an 8-week injection.

### Leaving her estimates

`CycleCalculator` takes the stretches with the log, as `0003` foresaw:

- **Cycles.** A complete cycle counts toward her typical lengths and ranges only if every day of it
  lies outside every hormonal stretch. The cycle running when she starts a hormonal method ends the
  day before the start date: it is shown, marked as cut short, and never counts. The last six
  natural cycles are the six most recent that count, from before a stretch as well as after it.
- **Periods.** A period that starts inside a hormonal stretch is a bleed or bleeding, not a period:
  it never counts toward her typical period length.
- **Combined methods, the bleed after stopping.** Bleeding that starts within 7 days after she stops
  a combined pill, patch or ring is still a withdrawal bleed and belongs to the stretch
  ([`contraception.md`](../research/contraception.md#combined-pill)): it is never a period and
  never her first period after stopping ([Stopping](#stopping)). The first period after it starts
  her first natural cycle.
- **Copper IUD.** Cycles count. Her expected period length uses periods that started on or after the
  date fitted, once one has ended; before that, her usual length.
- **Unknown start** (skipped in setup): the stretch covers every day logged before its stop, so none
  of those cycles count.

### The next bleed, on a combined method with a break every month

Every pack, patch cycle or ring cycle is 28 days, with the break or dummy pills at its end. The bleed
comes in the break, so:

- **Which bleeds count.** A bleed counts only if it starts at least 21 days after the start date
  (for the first one; with no start date, the first bleed she logs on the method counts) or at
  least 21 days after the first day of the last bleed that counted. Each break that passes with no
  bleed logged (below) moves that day 28 days later. Any other bleed is bleeding between breaks: it
  shows and counts in her logs, but does not move the estimate and does not restart the 21 days.
  Started 3 May, bleed logged 8 May: the bleed is still expected 24 to 30 May, not 3 to 7 June.
- **Before a bleed has counted:** the bleed is expected in the first break, from the start date + 21
  days to the start date + 27 (days 22 to 28 of the first pack, which covers 21/7, 24/4 and 26/2
  packs). Today shows it as a range only: "24 to 30 May", "In your first pill break. Estimated from
  the day you started the pill."
- **With no start date** ("I don't remember" in setup), there is no first break to place, so there
  is no estimate until a bleed counts. Usually that is the period she gave in setup, which falls
  inside the stretch; with none logged, Today shows "Pill" and "Cycle will estimate your next bleed
  once you log one." with no Next bleed card, the calendar expects nothing, and no break can pass
  "with no bleed logged". The first bleed she logs counts, and the estimate runs from it as below.
- **After that:** around the first day of the last bleed that counted + 28 days, between 2 days
  before and 2 days after.
- **Three ahead** on the calendar, each 28 days after the one before, as in `0003`.
- **No bleed in a break:** once the range has passed with nothing logged, Today says "No bleed
  logged this break. Some breaks pass without one." and the next one is expected 28 days after the
  missed one would have started. No "late" and no pregnancy wording.
- **Basis**, as in `0003`: "Estimated from the day you started the pill" or "Estimated from your last
  bleed on the pill". The "How is this estimated?" sheet says it is a bleed set by the pill, not a
  period.

With a break every few packs or no breaks, there is no estimate: she decides when a break comes, or
has none. Asking for her planned break dates was left out until she asks.

### The last 90 days, on a no-estimate method

Today and History describe bleeding the way clinicians do on contraception, over 90 days
([`contraception.md`](../research/contraception.md#describing-bleeding-that-does-not-follow-a-cycle-90-day-reference-periods)),
in plain counts, never with the clinical labels:

- **A bleeding day** is a period day by `0003`'s rules (flow light or heavier, or filled between
  markers) or a spotting day.
- **An episode** is a run of bleeding days in a row.
- **Today's card** is titled "Last 90 days", or "Since 6 September" while the stretch is younger
  than 90 days: "You logged bleeding or spotting on 11 days, in 3 episodes. The longest lasted 6
  days." With nothing logged: "You logged no bleeding or spotting."
- **History** shows the same counts over a stretch's last 90 days (the current 90 days while she is
  on it).
- **The first months:** for 3 months after the start date (6 on a hormonal IUD), the card adds one
  line that says the bleeding is expected to be unsettled, per method (in the design doc). It goes
  without a tap; it is not a warning.

### Stopping

The stretch ends on the stop date and the next day is "none", except for a combined method's
withdrawal bleed (below). Everything below starts the day after the stop date, never while it is
still ahead (the injection's weeks, above):

- **Today** shows the days since the stop date ("12 days", "since your implant came out") instead of
  a cycle day, until her first period after stopping, and "My period started" as its button.
- **The 7 days after a combined method.** A bleed that starts in the 7 days after the stop date is
  the withdrawal bleed and belongs to the stretch (above). In those days Today's button is the
  method's "Bleed started" ("Bleeding started" after a break every few packs or none), and "My
  period started" from the 8th day. That bleed's days are "bleed" (or "bleeding") on Today, the
  calendar, TalkBack and the day log, to its last day, even past the 7th; while it lasts, the button
  is "Bleed ended" ("Bleeding stopped"). It does not end the days-since display, is not her first
  period after stopping, and does not anchor the estimate: the next period is still expected at the
  stop date + her usual cycle, ±7 days, and "Missed a period?" still waits for her first period.
  Last pill 3 November, bleed logged on 6 November: on 8 November Today reads "5 days", "since you
  stopped the pill", **Bleed ended**, next period around 2 December. A bleed that starts on 11
  November or later is her first period: "Day 1".
- **The next period** is expected at the stop date + her usual cycle (the median of her natural
  cycles, or her setup length, or 28), between 7 days before and 7 days after. Once she logs a
  period, cycles count from it as in `0003`, but the range stays at least ±7 days until she has
  logged three complete cycles since stopping. The basis says why: "Estimated from your usual 29-day
  cycle. Cycles can take a few months to settle after the implant, so the range is wider."
- **"Missed a period?"** is not asked before her first period after stopping.
- **After the injection**, no next-period estimate until she logs a period, because periods can take
  months, up to a year, to come back ([`contraception.md`](../research/contraception.md#injection)).
  Today says so: "Periods can take several months to come back after the injection. Cycle will
  estimate again once you log one."

### Setup and Settings

- **Setup** gains an optional third step after her usual lengths: "Are you using contraception?",
  then "since when" (with "I don't remember") and, on a combined method, its breaks. Skipping it, or
  choosing None, leaves Cycle as it is now.
- **Settings › Your cycle › Contraception** shows the current method with its calm line, "Mark as
  stopped" and "Change method", the earlier stretches, and each stretch's edit page (dates, breaks,
  delete). The flows and copy are in the design doc.
- **When to get help.** On the combined pill, patch or ring, or either IUD, the same page shows
  "When to get help" under the current method and above the earlier stretches: fixed text with the
  signs clinics give every user of the method, in the words of
  [`0007`](0007-urgent-symptoms-on-a-method.md). It follows the method in force, never what she
  logs, and appears nowhere else. Its layout is in the design doc.

### Privacy

The method is health data and stays on the phone with the rest:

- It travels only in her export and in Android's encrypted backup (the Room database,
  [`0004`](0004-backup-encryption.md)). The export gains a third section after the settings, with
  the columns `method,started,stopped,breaks`, and import adds the stretches that do not overlap
  ones already on the phone, refusing the file with a line number otherwise, like its other
  problems. A null start overlaps every stretch that starts on or before its stop, as above.
- Never in a notification, a widget or any text outside the app. "Delete everything" deletes it.

### Thresholds

All new numbers live in `CycleRules` with their source in a comment, as in `0003`: the 28-day pack,
21 days between scheduled bleeds, ±2 days around the next bleed, 7 days for the withdrawal bleed
after stopping, ±7 days and three cycles after stopping, 13 weeks after the last injection, 90 days
for the bleeding summary, 3 months (6 on a hormonal IUD) for the first-months line, and 6 months for
the copper IUD note.

## Options considered

- **One switch, "on hormonal contraception".** Simplest, but the combined pill's bleed can be
  estimated and the implant's cannot, and the copper IUD keeps her cycle. Three behaviours cover the
  research's table with no more settings for her.
- **A list of every method, including the injection and the patch.** Chosen over only the implant and
  the pill she asked about: the research covers them, the extra rows cost nothing, and a later switch
  needs no new screen. The diaphragm and condoms have no row: they leave her cycle as it is, which
  is "None". The monthly combined injection has no row yet either: it stops ovulation like the other
  hormonal methods, but it is sold mainly outside the UK and US, and whether her list needs it is
  still hers to answer ([`product-implications.md`](../research/product-implications.md), open
  question 2).
- **Storing the method in DataStore with the settings.** Easy for the current method, but History
  needs the dates of every past method, and `0003` already says life stages are date ranges in their
  own table.
- **Deleting or moving every stretch a new start covers, in one dialog.** Fewer steps when a start
  reaches back over several methods, but one tap on "Move it" would forget whole stretches she
  logged. Deleting dates keeps its own page and its own question; the refusal names the stretch to
  delete or the day to pick after.
- **Asking for the start date of her current pack** to place the break exactly. More precise, but one
  more date to keep right, and wrong as soon as she starts a pack a day late. Her logged bleeds
  follow the pack anyway, so the estimate anchors on them after the first break.
- **Counting the next bleed from any bleed she logs.** Breakthrough bleeding is common in the first
  months; letting it move the estimate would put the next bleed in the middle of a pack. Only a
  bleed at least 21 days after the start date, or after the last bleed that counted, counts.
- **Planned breaks for extended and flexible regimens** ("Next planned break: 3 to 6 June"). Needs
  her break dates for every pack; left out until she asks.
- **A next-bleed estimate on the hormonal IUD once her bleeding looks regular.** See above: no rule
  for "regular" yet, and a second mode to explain.
- **Keeping the cycle-day counter on hormonal methods.** It counts from a bleed that is not a period,
  and invites "late" readings. The method's name and a 90-day count describe her bleeding without
  pretending there is a cycle.
- **Showing the clinical labels** ("infrequent", "prolonged", "amenorrhoea"). Clinicians' words read
  like a diagnosis; plain counts say the same thing.
- **Asking about contraception first in setup**, so the last-period step could say "bleed". The order
  welcome, last period, lengths, contraception keeps the first two steps the same for everyone, and
  the stretch re-labels the logged day afterwards.
- **A required start date in setup.** She may not remember when an implant went in; "I don't
  remember" is better than a guess, and covers the few days setup logs.
- **The same ±4-day little-data range after stopping.** The first cycles after a hormonal method vary
  more, and she asked for Cycle to say so; ±7 days for three cycles says it in the estimate itself.

## Consequences

- `core:model` gains `ContraceptionMethod` (with its behaviour), `Breaks` and `ContraceptionStretch`;
  `core:data` the table, its DAO, a `ContraceptionRepository`, the migration and the export section;
  `core:domain` the rules above in `CycleCalculator` and `CycleRules`. The overview screens read
  carries the method in force today, its behaviour and the stretches, so no screen decides which
  words to use on its own.
- `core:designsystem` gains, each with its catalog entry, previews and tests
  ([`contraception.md`](../design/contraception.md#components)): `RadioRow`, a single-choice row like
  `SwitchRow`; a `BleedingWords` parameter (`Period`, `Bleed`, `Bleeding`) on `DayCell`,
  `MonthCalendar`, `WeekRow` and `CycleLegend`, for the legend labels and TalkBack; and
  `CycleIcons.Medication` for the Settings row.
- Health signals, when they arrive, follow the per-method table in
  [`contraception.md`](../research/contraception.md#worth-mentioning-to-a-doctor-on-a-method), using
  the stretches' start and stop dates.
- Reminders (a pill or injection reminder) are not part of this record. If she asks for one, it
  needs its own ticket and its own discreet wording.
