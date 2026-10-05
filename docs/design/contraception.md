# Contraception: stories, journeys and copy

She asked for Cycle to know whether she has an implant or takes the pill
([MOT-48](https://linear.app/tonypine/issue/MOT-48)). This page is the design: the user stories, then
every journey screen by screen with its copy and where it lives in the app, then the components.
The screens are drawn in [`journeys/contraception.html`](journeys/contraception.html) (open it in a
browser; one row of phones per journey). The rules behind them, method by method, are
[`0006-contraception.md`](../decisions/0006-contraception.md); the research is
[`docs/research/contraception.md`](../research/contraception.md)
([MOT-49](https://linear.app/tonypine/issue/MOT-49)).

Every date and number here is synthetic.

## Where it lives

| Place | What changes |
| -- | -- |
| Setup | A third, optional step after her usual lengths: the method, since when, and breaks on a combined method. |
| Settings › Your cycle › Contraception | A new row and page: her method now, "Mark as stopped", "Change method", every earlier method with its dates, and an edit page for each. |
| Today | On a hormonal method, the method's name instead of "Day 19", a calm line, the next bleed (combined, a break every month) or the last 90 days (every other hormonal method). After stopping, the days since and a wider estimate. On a copper IUD, a card about heavier periods. |
| Calendar | "Bleed" or "Bleeding" in the legend and for TalkBack; expected bleeds instead of predicted periods on a combined method with monthly breaks; nothing predicted on the others. |
| History | The time on each hormonal method as one card, marked and left out of her typical cycle, with its last 90 days; the cycle a method cut short, marked. |
| Day log | Same flow and categories. Its fill offer and clear dialog use the method's word. |

## User stories

Refined against MOT-49. "Hormonal method" means everything except the copper IUD.

1. **Tell Cycle at setup.** With an implant, I can say so during setup, as an optional step after my
   usual lengths. "Since when" is optional too ("I don't remember"), so Cycle does not predict
   periods my implant will not follow. Skipping it, or choosing None, leaves Cycle as it is.
2. **Add or change it later.** When I start the pill, get an implant fitted or switch, I set it in
   Settings › Your cycle › Contraception, with the date it started (required there). On a combined
   pill, patch or ring, I also say whether I take a break every month, every few packs, or never.
   Switching ends the old method the day before the new one starts.
3. **Know what Cycle does on my method.** On a hormonal method Cycle says in one calm line what it
   stops estimating and why, on Today and in Settings ([the calm line](#the-calm-line-per-method)).
   Never "safe days", never a condition name, never advice on the method.
4. **Keep logging the same way.** Bleeding is logged with the same one tap ("Bleeding started",
   "Bleed started" on a combined method) and the same day log. Only the words change.
5. **Combined pill, patch or ring.** With a break every month, Cycle shows when my next bleed is
   likely, in the break, called a bleed, never fertile days. If I take breaks only every few packs,
   or none, it stops expecting one.
6. **Copper IUD.** Predictions continue as before. For the first six months Cycle mentions that
   periods can be heavier or longer at first, until I tap Got it.
7. **Stopping.** I mark the end date. Estimates restart from my usual lengths with a wider range
   (±7 days for three cycles), and Cycle says cycles can take a few months to settle. After the
   injection, it waits for my first period before estimating, and says why.
8. **History keeps its meaning.** Time on a hormonal method stays in History as one card, marked,
   and is left out of my typical cycle. On a method without an estimate I see the days of bleeding
   or spotting in the last 90 days, in plain counts.
9. **Fix mistakes.** I can edit the dates of any method, or delete them, and everything recomputes.
   When a correction overlaps another method, Cycle offers to move that one's edge.
10. **Private.** My method stays on the phone. It appears only in my export and Android's encrypted
    backup, never in a notification or widget.

## The words

From [`product-implications.md`](../research/product-implications.md#on-contraception):
"period" with no method or a copper IUD; "bleed" for the scheduled bleed on a combined pill, patch or
ring with a break every month; "bleeding" on every other hormonal method. A day before she started a
method keeps "period".

| Where | Own cycle | Combined, a break every month | Every other hormonal method |
| -- | -- | -- | -- |
| Today's main button | My period started / My period ended | Bleed started / Bleed ended | Bleeding started / Bleeding stopped |
| Today's Undo card | Period started today | Bleed started today | Bleeding started today |
| Today's line while bleeding | Day 2 of your period | Day 2 of your bleed | Day 2 of bleeding |
| Calendar legend | Period, Predicted period | Bleed, Expected bleed | Bleeding |
| TalkBack, a logged day | 14 October, period | 14 October, bleed | 14 October, bleeding |
| Day log fill offer | Period started this day: fill in 5 days | Bleed started this day: fill in 4 days (once she has logged an ended bleed on the method) | Not offered |
| Day log clear dialog | will no longer count as a period day | will no longer count as a bleed day | will no longer count as a bleeding day |
| "Still going?" | Still going? | Still bleeding? | Still bleeding? |

## The method list

Setup and Settings show the same list, one `RadioRow` each, nothing chosen at first.

| Title | Line under it |
| -- | -- |
| None | Or a barrier method, such as condoms |
| Combined pill | Two hormones, often with a break |
| Progestogen-only pill | Also called the mini pill |
| Patch | A new patch every week |
| Vaginal ring | Kept in for three weeks at a time |
| Implant | A rod in the arm, such as Nexplanon |
| Hormonal IUD | Hormonal coil or IUS, such as Mirena |
| Copper IUD | Copper coil, with no hormones |
| Injection | Such as Depo-Provera |

Under the list in Settings, after choosing a pill: "Not sure which pill you take? The leaflet in the
pack says."

Short names, for Today's display and History's card label: Pill, Mini pill, Patch, Ring, Implant,
Hormonal IUD, Copper IUD, Injection.

### Since when

| Method | Title (Setup and Settings) | When she stops |
| -- | -- | -- |
| Combined pill | When did you start the pill? | When did you take your last pill? |
| Progestogen-only pill | When did you start the mini pill? | When did you take your last pill? |
| Patch | When did you start the patch? | When did you take off your last patch? |
| Vaginal ring | When did you start the ring? | When did you take out your last ring? |
| Implant | When was your implant fitted? | When was your implant taken out? |
| Hormonal IUD, Copper IUD | When was your IUD fitted? | When was your IUD taken out? |
| Injection | When was your first injection? | When was your last injection? (body: "Cycle counts 13 weeks from it.") |

Body: "Roughly is fine." A `MonthCalendar` with days up to today; days before the method's start are
not tappable when stopping.

### Breaks (combined pill, patch, ring)

Title: "Do you take a break between packs?" (patch: "Do you have a patch-free week?", ring: "Do you
have a ring-free week?").

| Title | Line under it (pill; patch and ring in brackets) |
| -- | -- |
| Every month | A week off, or dummy pills, in every pack (A patch-free week after three patches; A ring-free week after three weeks) |
| Every few packs | Two or more packs in a row, then a break |
| No breaks | One pack straight after another |

## The calm line per method

One sentence that says what Cycle stops estimating and why. It is the body of the method's card in
Settings, and Today's line under the method's name on a method without an estimate.

| Method | The calm line |
| -- | -- |
| None | Cycle estimates your periods from your own cycle. |
| Copper IUD | Your cycle stays your own, so Cycle keeps estimating your periods. They can be heavier or longer at first. |
| Combined pill, a break every month | The pill sets your bleeds, so Cycle expects one in each break and calls it a bleed, not a period. (Patch: "The patch sets your bleeds, so Cycle expects one in each patch-free week…"; ring likewise.) |
| Combined, a break every few packs | You choose when your breaks come, so Cycle doesn't estimate your next bleed. |
| Combined, no breaks | With no breaks there's no bleed to expect, so Cycle doesn't estimate one. |
| Progestogen-only pill | Bleeding on the mini pill doesn't follow a cycle, so Cycle doesn't estimate it. |
| Implant | Bleeding on the implant can come at any time, so Cycle doesn't estimate it. |
| Hormonal IUD | Bleeding with a hormonal IUD doesn't follow a cycle you can count on, so Cycle doesn't estimate it. |
| Injection | Bleeding on the injection doesn't follow a cycle, so Cycle doesn't estimate it. |

### The first-months line

Added to Today's Last 90 days card (or the Next bleed card) for 3 months after the start date, 6 on
a hormonal IUD. Expected changes read as expected, not as a warning.

| Method | Line |
| -- | -- |
| Combined, a break every month | Bleeding between breaks is common in the first three months, and usually settles. |
| Combined, every few packs or no breaks | Bleeding between breaks is common in the first months, and usually settles. |
| Progestogen-only pill | Bleeding on the mini pill often changes in the first months. |
| Implant | In the first months on the implant, bleeding is often irregular or long. |
| Hormonal IUD | In the first months with a hormonal IUD, bleeding is often frequent or long. It usually gets lighter over the first year. |
| Injection | In the first months on the injection, bleeding is often irregular or long. It usually lessens with time. |

### What changes (Today's sheet)

Opened from "What changes on the implant?" (the method's name in each). Title "Bleeding on the
implant". Three paragraphs: what the method does to bleeding (from the research), what Cycle does
about it, and where to go if it bothers her. The implant's:

> The implant changes bleeding for most people. It can be lighter, longer, come and go, or stop, and
> the pattern can change at any time.
>
> So Cycle doesn't estimate when you'll bleed, count cycle days or say anything is late. It shows the
> days you log, and how much you bled in the last 90 days.
>
> If the bleeding becomes a problem for you, a GP or sexual health clinic can help.

Button: "Got it". On the combined pill with monthly breaks, "How is this estimated?" opens the
estimate sheet instead:

> On the combined pill, the bleed in your break comes because you stop the hormones for a few days.
> It's a bleed set by the pill, not a period.
>
> Cycle expects it 28 days after your last one started, give or take 2 days. Before you've logged
> one, it expects it in your first break, 22 to 28 days after you started.
>
> Bleeding between breaks is common in the first months. It shows on your calendar but doesn't move
> this estimate.
>
> These are estimates, not promises.

## Journeys

Each step names its screen, where it lives, the copy on it, and the components. The letters and
numbers match the phones in [`journeys/contraception.html`](journeys/contraception.html).

### A. Setup with an implant

Today is Saturday 20 March 2027. Her implant was fitted on 9 November 2026.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| A1 | Welcome | Setup | Unchanged: "Hi! Let's get your cycle going", "Log your period and Cycle estimates the next one. Everything stays on this phone: no account, no ads, no tracking." Get started · Restore from a Cycle export · Skip for now |
| A2 | Last period | Setup, step 1 of 3 | "Step 1 of 3" (was "of 2"). "When did your last period start?" "Roughly is fine." Calendar, 2 March chosen. Next · I don't remember |
| A3 | Usual lengths | Setup, step 2 of 3 | "How long do they usually last?" "Cycle estimates from these until you have logged periods of your own." Cycle length 28, period length 5, with their hints. The button now reads **Next**, not Done. |
| A4 | Contraception | Setup, step 3 of 3 (new) | "Are you using contraception?" "Some methods change bleeding, so Cycle changes what it estimates. You can change this later in Settings." The method list, Implant chosen. **Next** (off until she chooses) · **Skip**. Choosing None makes the button **Done**. |
| A5 | Since when | Setup, step 3 of 3 | "When was your implant fitted?" "Roughly is fine." Calendar, 9 November 2026 chosen. **Done** · **I don't remember** (no start date: every day logged before counts as on the implant). On a combined method, Done becomes Next and the breaks page follows, still step 3 of 3. |
| A6 | Today on the implant | Today | "SATURDAY 20 MARCH", display "Implant", "Bleeding on the implant can come at any time, so Cycle doesn't estimate it." The week, no bleeding. **Bleeding started** · Log how you feel. Card "Last 90 days": "You logged bleeding or spotting on 5 days, in 1 episode. The longest lasted 5 days." (the period from setup, now bleeding). What changes on the implant? |

### B. Starting the combined pill from Settings

Today is Monday 10 May 2027. She started the pill on 3 May.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| B1 | Settings | Settings | Your cycle gains a third row: **Contraception** · "None". |
| B2 | Contraception | Settings › Your cycle › Contraception | Top bar "Contraception", Back. "Now": card "None", "Cycle estimates your periods from your own cycle." **Add your method**. Note: "Cycle isn't a contraceptive and doesn't advise on methods. Questions about yours are for your pharmacist, nurse or doctor. Your method stays on this phone." |
| B3 | Method | Settings › Contraception › Add | "Which method?" "Some methods change bleeding, so Cycle changes what it estimates." The method list, Combined pill chosen. "Not sure which pill you take? The leaflet in the pack says." **Next** |
| B4 | Since when | Settings › Contraception › Add | "When did you start the pill?" "Roughly is fine." Calendar, 3 May chosen. **Next** |
| B5 | Breaks or not | Settings › Contraception › Add | "Do you take a break between packs?" Every month (chosen) · Every few packs · No breaks. **Save**: back to Contraception, which shows the pill (as in E1), then Today. |
| B6 | Today: the next bleed | Today | "MONDAY 10 MAY", display "Pill", "Your first bleed is expected in about 14 days". **Bleed started** · Log how you feel. Card "Next bleed": "24 to 30 May", "In your first pill break. Estimated from the day you started the pill." "Bleeding between breaks is common in the first three months, and usually settles." How is this estimated? |
| B7 | Calendar | Calendar | Expected bleed on 24 to 30 May, dashed like a predicted period. Legend: Bleed, Expected bleed, Today. "Expected bleeds are estimates. Tap any day up to today to log or change it." |

Later on the pill, once she has logged a bleed: "Your next bleed is expected in about 6 days"; card
"Around 21 June", "Between 19 and 23 June", "In your pill break. Estimated from your last bleed on
the pill." If a break passes with no bleed logged, the status card says "No bleed logged this
break" and "Some breaks pass without one. The next is expected in your next break."

With "Every few packs" or "No breaks", Today looks like C1 with display "Pill" and that regimen's
calm line.

### C. Living on the implant

Today is Thursday 14 October 2027. Implant since 9 November 2026.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| C1 | Today | Today | "THURSDAY 14 OCTOBER", "Implant", the calm line. **Bleeding started** · Log how you feel. "Last 90 days": "You logged bleeding or spotting on 11 days, in 3 episodes. The longest lasted 6 days." What changes on the implant? |
| C2 | Log a bleed: one tap | Today | After one tap: "Bleeding started today"; the day turns red in the week; card "Bleeding started today" · Undo. **Bleeding stopped** · Log flow and how you feel. The 90-day card updates: "12 days, in 4 episodes". |
| C3 | What changes | Today › sheet | "Bleeding on the implant" (copy above). Got it |
| C4 | Calendar: nothing predicted | Calendar | Logged days 2, 3 and 14 October as bleeding; no predicted days. Legend: Bleeding, Today. "Cycle doesn't estimate bleeding on the implant. Tap any day up to today to log or change it." |
| C5 | History: the time on it, marked | History | "Your typical cycle": cycle 29 days (27 to 31), period 5 days (4 to 6), "The middle length of your last 6 cycles. Time on hormonal contraception is left out." Under Cycles: card **IMPLANT**, "Since 9 November 2026", "Not part of your typical cycle.", "Last 90 days: you logged bleeding or spotting on 12 days, in 4 episodes. The longest lasted 6 days." See it in the calendar. Then "8 Oct to 8 Nov 2026", "32 days, cut short when the implant was fitted. Not counted." Then her earlier cycles, as now. |

When she has no natural cycles at all (on a method since before she installed Cycle), the typical
cycle card is left out and History starts with the method's card.

### D. Implant removed

Today is Monday 15 November 2027. The implant came out on 3 November.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| D1 | Settings | Settings | Contraception · "Implant, since 9 Nov 2026" |
| D2 | Contraception | Settings › Your cycle › Contraception | "Now": "Implant", "Since 9 November 2026", the calm line. **Mark as stopped** · Change method. "Your methods": Implant · Since 9 Nov 2026 › |
| D3 | Stopped on | Settings › Contraception › Mark as stopped | "When was your implant taken out?" "Roughly is fine." Calendar, 3 November chosen. **Save** |
| D4 | Contraception: none now | Settings › Your cycle › Contraception | "Now": "None", "Cycle estimates your periods from your own cycle." Add your method. "Your methods": Implant · 9 Nov 2026 to 3 Nov 2027 › |
| D5 | Today: fresh estimates | Today | "MONDAY 15 NOVEMBER", display "12 days", "since your implant came out". **My period started** · Log how you feel. "Next period": "Around 2 December", "Between 25 November and 9 December", "Estimated from your usual 29-day cycle. Cycles can take a few months to settle after the implant, so the range is wider." How is this estimated? |
| D6 | History | History | The implant's card now reads "9 Nov 2026 to 3 Nov 2027", "Not part of your typical cycle.", "In its last 90 days you logged bleeding or spotting on 10 days, in 3 episodes. The longest lasted 5 days." |

Once she logs her first period after it, Today is "Day 1" again, with the ±7-day range until she has
logged three cycles. "Missed a period?" is not asked before that first period. Other methods:
"since you stopped the pill", "since your IUD came out". After the injection, Today shows no next
period: "Periods can take several months to come back after the injection. Cycle will estimate again
once you log one."

### E. Switching from the pill to a hormonal IUD, then correcting the start date

Today is Friday 17 September 2027. Pill since 3 May. The IUD was fitted on 6 September; she first
enters 13 September by mistake.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| E1 | Contraception: on the pill | Settings › Your cycle › Contraception | "Now": "Combined pill", "Since 3 May 2027 · a break every month", the calm line. Mark as stopped · **Change method** |
| E2 | Change method | Settings › Contraception › Change method | "Which method?" The list, Hormonal IUD chosen. **Next** |
| E3 | Since when (wrong day) | Settings › Contraception › Change method | "When was your IUD fitted?" "Roughly is fine." 13 September chosen. **Save**. The pill now ends on 12 September. A day on or before 3 May is refused: "That's before you started the pill on 3 May. Pick a later day, or change the pill's dates first." |
| E4 | Contraception: switched | Settings › Your cycle › Contraception | "Now": "Hormonal IUD", "Since 13 September 2027", its calm line. "Your methods": Hormonal IUD · Since 13 Sep 2027 ›, Combined pill · 3 May to 12 Sep 2027 › |
| E5 | Edit its dates | Settings › Contraception › Hormonal IUD | Top bar "Hormonal IUD". Rows: Fitted · 13 September 2027; Taken out · Still in; Delete these dates · "Cycle forgets this method for these dates. What you logged stays." (Combined methods add Breaks · Every month.) |
| E6 | Correct the start | Settings › Contraception › Hormonal IUD › Fitted | Sheet "When was your IUD fitted?", 6 September chosen. **Save** |
| E7 | Overlap | Same, dialog | "Move the end of your pill?" "Your combined pill would end on 5 September instead of 12 September, so the two don't overlap. Cycle works out its estimates again." Cancel · **Move it** |
| E8 | Today on the IUD | Today | "FRIDAY 17 SEPTEMBER", "Hormonal IUD", the calm line. **Bleeding started** · Log how you feel. Card "Since 6 September": "You logged bleeding or spotting on 7 days, in 2 episodes. The longest lasted 4 days." "In the first months with a hormonal IUD, bleeding is often frequent or long. It usually gets lighter over the first year." What changes with a hormonal IUD? |

Deleting dates asks first: "Delete these dates?" "Cycle forgets you used the combined pill from 3 May
to 5 September 2027. The days you logged stay, and count as your own cycle again." Keep them ·
**Delete**. A correction that would swallow another method whole is refused: "That would cover all
of your time on the pill. Delete the pill's dates first, or pick a later day."

### F. Copper IUD

Today is Tuesday 22 June 2027. Her last period started on 14 June; the IUD was fitted on 15 June.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| F1 | Contraception | Settings › Your cycle › Contraception | "None". **Add your method** |
| F2 | Choose copper IUD | Settings › Contraception › Add | The list, Copper IUD chosen. **Next** |
| F3 | Since when | Settings › Contraception › Add | "When was your IUD fitted?" 15 June chosen, during her period. **Save** |
| F4 | Contraception: copper IUD | Settings › Your cycle › Contraception | "Copper IUD", "Since 15 June 2027", "Your cycle stays your own, so Cycle keeps estimating your periods. They can be heavier or longer at first." |
| F5 | Today still predicting | Today | "TUESDAY 22 JUNE", "Day 9", "Your next period is expected in about 20 days". Card "Periods can be heavier at first": "With a copper IUD, periods are often heavier, longer or more painful for the first 3 to 6 months. Cycle keeps estimating them as before." Got it. My period started · Log how you feel. "Next period": "Around 12 July", "Between 9 and 15 July", "Estimated from your last 6 cycles". |
| F6 | Calendar: periods predicted | Calendar | Unchanged: predicted period 12 to 16 July, legend Period, Predicted period, Today. |

The heavier-periods card shows for six months after the fitting date, until she taps Got it. Her
expected period length switches to periods since the fitting once one has ended.

## Components

Every screen uses `core:designsystem` and `core:ui` as they are, except the three additions below,
which the build adds first with their catalog entries, previews and tests
([`design-system.md`](design-system.md#adding-a-component)).

| Component | Used for | Status |
| -- | -- | -- |
| `TopAppBar`, `NavigationBar` | Every screen | Exists |
| `EmptyState` | A1 | Exists |
| `MonthCalendar`, `WeekRow`, `CycleLegend`, `DayCell` | A2, A5, B4, B7, C4, D3, E3, E6, F3, F6; the week on Today | Exist; gain `BleedingWords` |
| `UsualLengthFields` (`core:ui`) | A3 | Exists |
| `Card`, `ClickableCard` | Today's cards, History, Contraception's Now card | Exist |
| `FilledButton`, `TonalButton`, `TextButton` | Throughout | Exist |
| `CycleBottomSheet` | C3 and the estimate sheet, E6 | Exists |
| `CycleAlertDialog`, `CycleDestructiveDialog` | E7; deleting dates | Exist |
| Settings' `SettingsRow` (feature code, a `ClickableCard`) | B1, D2, E4, E5 | Exists in `feature:settings` |
| **`RadioRow`** | The method list (A4, B3, E2, F2) and breaks (B5) | **To add.** A single-choice row like `SwitchRow`: title in `titleSmall`, an optional one-line body in `bodySmall`, a radio at the end; `accentContainer` when selected; 48dp at least; `Role.RadioButton` in a `selectableGroup`, read as "Implant, A rod in the arm, such as Nexplanon, radio button, selected, 6 of 9". `ButtonGroup` does not fit: nine options with a line each. |
| **`BleedingWords`** | Calendar legend, day cells, week row | **To add.** An enum, `Period` (default), `Bleed`, `Bleeding`, passed to `DayCell`, `MonthCalendar`, `WeekRow` and `CycleLegend`. It changes the legend labels ("Bleed", "Expected bleed", "Bleeding") and TalkBack ("14 October, bleeding"); the shapes and colours stay. A calendar spanning a method's start takes the word per day. |
| **`CycleIcons.Medication`** | The Contraception row in Settings | **To add.** Material Symbols Rounded "medication", weight 600, like the others. The journeys page draws a stand-in capsule. |

## Copy checks

Against [`product-implications.md`](../research/product-implications.md#copy-rules) and MOT-49:

- No "safe day", "low chance", "protected", "you can't get pregnant", "fertile" or "ovulation" on a
  hormonal method; no pregnancy prompts, including when a break passes with no bleed.
- No advice on the method: no "consider", "should", "stop", "switch", missed-pill rules. Questions go
  to "your pharmacist, nurse or doctor"; bleeding that bothers her, to "a GP or sexual health
  clinic", the NHS's words.
- No condition names and no clinical labels ("amenorrhoea", "prolonged", "infrequent"); counts
  instead.
- Every estimate reads as one: "expected", "around", "between", "estimated from", and the
  calendar's "are estimates".
- No cycle language without a cycle: no cycle day ("Day 19"), "late" or "Missed a period?" on a
  method without an estimate.
- The method never appears in a notification, widget or the recent-apps preview.
