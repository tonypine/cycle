# Contraception: stories, journeys and copy

She asked for Cycle to know whether she has an implant or takes the pill
([MOT-48](https://linear.app/tonypine/issue/MOT-48)). This page is the design: the user stories, then
every journey screen by screen with its copy and where it lives in the app, then the components.
The screens are drawn in [`journeys/contraception.html`](journeys/contraception.html) (open it in a
browser; one row of phones per journey). The rules behind them, method by method, are
[`0006-contraception.md`](../decisions/0006-contraception.md), and the words of "When to get help" are
[`0007`](../decisions/0007-urgent-symptoms-on-a-method.md); the research is
[`docs/research/contraception.md`](../research/contraception.md)
([MOT-49](https://linear.app/tonypine/issue/MOT-49)).

Every date and number here is synthetic.

## Where it lives

| Place | What changes |
| -- | -- |
| Setup | A third, optional step after her usual lengths: the method, since when, and breaks on a combined method. |
| Settings › Your cycle › Contraception | A new row and page: her method now, "Mark as stopped", "Change method", every earlier method with its dates, and an edit page for each. On the combined pill, patch or ring or an IUD, "When to get help" under her method. |
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
   Switching ends the old method the day before the new one starts, also when I skipped "since when"
   for it.
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
   (±7 days for three cycles), and Cycle says cycles can take a few months to settle. After either
   injection, it waits for my first period before estimating, and says why.
8. **History keeps its meaning.** Time on a hormonal method stays in History as one card, marked,
   and is left out of my typical cycle. On a method without an estimate I see the days of bleeding
   or spotting in the last 90 days, in plain counts.
9. **Fix mistakes.** I can edit the dates of any method, or delete them, and everything recomputes.
   When a correction overlaps another method, Cycle offers to move that one's edge.
10. **Private.** My method stays on the phone. It appears only in my export and Android's encrypted
    backup, never in a notification or widget.
11. **Know when to get help.** On the combined pill, patch or ring, or with an IUD, my method's page
    lists the signs clinics tell everyone on it to get help for, and what to do. It is the same for
    everyone on the method, it is there whenever I look, and it says Cycle does not check my log for
    them ([When to get help](#when-to-get-help)).
12. **Monthly combined injection.** If I get an injection every 4 weeks, I can choose it apart from
    the 8- or 13-week injection. Cycle calls everything I log bleeding, shows my last 90 days and
    doesn't estimate my next bleed, because it doesn't know when I have my injections. When I mark
    it stopped with the date of my last injection, Cycle counts 4 more weeks, then waits for my
    first period before estimating ([MOT-85](https://linear.app/tonypine/issue/MOT-85)).

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
| Monthly combined injection | Two hormones, every 4 weeks, such as Cyclofem |

Under the list in Settings, after choosing a pill: "Not sure which pill you take? The leaflet in the
pack says."

Short names, for Today's display and History's card label: Pill, Mini pill, Patch, Ring, Implant,
Hormonal IUD, Copper IUD, Injection, Monthly injection. In a sentence: "monthly injection" ("Move
the end of your monthly injection?"); in full: "monthly combined injection".

"Two hormones" and "every 4 weeks" keep it apart from the progestogen-only injection: Mesigyna has
the same progestogen as Noristerat, and the list must not let one pass for the other
([research](../research/contraception.md#monthly-combined-injection)).

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
| Monthly combined injection | When was your first monthly injection? | When was your last injection? (body: "Cycle counts 4 weeks from it.") |

Body: "Roughly is fine." A `MonthCalendar` with days up to today; days before the method's start are
not tappable when stopping.

When she adds a method while on none, every day up to today is tappable. A day inside a stopped
method's dates asks to move that method's end, as in E7 ("Move the end of your implant?"); a day on
or before a stopped method's start is refused, as after E8 ("That would cover all of your time on
the implant. Delete the implant's dates first, or pick a later day."). When the day reaches more
than one method, any method covered whole refuses it, with no move dialog, and the refusal names
the latest method it would cover. Journey D shows all three.

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
| Monthly combined injection | Bleeding on the monthly injection follows your injections, not a cycle, and it varies, so Cycle doesn't estimate it. |

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
| Monthly combined injection | In the first months on the monthly injection, bleeding is often irregular or long. It usually settles. |

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

The monthly combined injection's, titled "Bleeding on the monthly injection":

> Most people bleed once between injections, about two to three weeks after each one. Bleeding can
> also come at other times, last longer, or not come at all, most often in the first months.
>
> Cycle doesn't know when you have your injections, so it doesn't estimate when you'll bleed, count
> cycle days or say anything is late. It shows the days you log, and how much you bled in the last
> 90 days.
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

## When to get help

The fixed text decided in [`0007`](../decisions/0007-urgent-symptoms-on-a-method.md): the signs
clinics give everyone on the method, under the action to take. It is reference text, the same for
every user of the method, and nothing she logs shows it, hides it or changes it.

| | |
| -- | -- |
| Methods | Combined pill (with any breaks), patch and ring; copper IUD and hormonal IUD. None for the progestogen-only pill, the implant, the injection or no method. None for the monthly combined injection either, until `0007` has a source for its signs and adds it ([`0006`](../decisions/0006-contraception.md#setup-and-settings)). |
| Where | Settings › Your cycle › Contraception, under the Now card, whose calm line says what the method does to bleeding, and above "Your methods". Shown in full for as long as the method is in force, also with "Start not known". Once she marks it as stopped, or changes to a method without it, the section goes. |
| Never | On Today or its sheets, in setup, on a method's edit page, in History, a notification or a widget. |
| Look | A section title "When to get help", like "Now" and "Your methods" (Settings' `SectionTitle`, a heading). Under it one `Card`: the intro line in `bodySmall` `onSurfaceVariant`; each action line in `titleSmall` `onSurface`, a heading; the signs in `body` `onSurface`, one per line, each after a bullet TalkBack skips; the last line in `bodySmall` `onSurfaceVariant`. No icon, no error colours, no button: nothing on it can be tapped or dismissed. |
| TalkBack | In reading order: "When to get help, heading", the intro, then each action line with "heading" ("Get emergency help now if you have:, heading") followed by its signs one by one, then "Cycle does not check your log for these signs." |

The words, line by line with their sources, are in
[`0007`](../decisions/0007-urgent-symptoms-on-a-method.md), and change only there. Only the intro
line differs between methods:

| Method | Intro line | Then |
| -- | -- | -- |
| Combined pill | Clinics give these signs to everyone who uses the combined pill, patch or ring. | Emergency help now (2 signs), urgent advice today (1 sign) |
| Patch | Clinics give these signs to everyone who uses the patch. | The same |
| Vaginal ring | Clinics give these signs to everyone who uses the ring. | The same |
| Copper IUD, hormonal IUD | Clinics give these signs to everyone who has an IUD. | Urgent advice today (5 signs) |

## Journeys

Each step names its screen, where it lives, the copy on it, and the components. The letters and
numbers match the phones in [`journeys/contraception.html`](journeys/contraception.html).

### A. Setup with an implant

Today is Saturday 20 March 2027. Her implant was fitted on 9 November 2026.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| A1 | Welcome | Setup | Unchanged: "Hi! Let's get your cycle going", "Log your period and Cycle estimates the next one. Everything stays on this phone: no account, no ads, no tracking." Get started · Restore from a Cycle export · Skip for now |
| A2 | Last period | Setup, step 1 of 3 | "Step 1 of 3" (was "of 2"). "When did your last period start?" "Roughly is fine." Calendar, 2 March chosen. Next · I don't remember |
| A3 | Usual lengths | Setup, step 2 of 3 | "How long do they usually last?" "Cycle estimates from these until you have logged periods of your own." Two slider fields: "Cycle length" "28 days" and "Period length" "5 days", each with − and + a day at a time and its hint ("From the first day of one period to the first day of the next. Often between 21 and 35 days." · "The days you bleed. Often between 2 and 7 days."). The button now reads **Next**, not Done. |
| A4 | Contraception | Setup, step 3 of 3 (new) | "Are you using contraception?" "Some methods change bleeding, so Cycle changes what it estimates. You can change this later in Settings." The method list, Implant chosen. **Next** (off until she chooses) · **Skip**. Choosing None makes the button **Done**. |
| A5 | Since when | Setup, step 3 of 3 | "When was your implant fitted?" "Roughly is fine." Calendar, 9 November 2026 chosen. **Done** · **I don't remember** (no start date: every day logged before counts as on the implant). On a combined method, Done becomes Next and the breaks page follows, still step 3 of 3. |
| A6 | Today on the implant | Today | "SATURDAY 20 MARCH", display "Implant", "Bleeding on the implant can come at any time, so Cycle doesn't estimate it." The week, no bleeding. **Bleeding started** · Log how you feel. Card "Last 90 days": "You logged bleeding or spotting on 5 days, in 1 episode. The longest lasted 5 days." (the period from setup, now bleeding). What changes on the implant? |

**With no start date** (I don't remember in A5), every place that prints the start says it is not
known:

- Settings' row: Contraception · "Implant, start not known".
- Contraception's Now card: "Implant", "Start not known", the calm line.
- "Your methods" and History's card: "Start not known" while she is on it; "Until 3 Nov 2027" once
  it has stopped, in place of "9 Nov 2026 to 3 Nov 2027".
- The edit page: Fitted · "Not known", which she can tap to set a date. Deleting asks "Cycle forgets
  you use the implant." (after it stopped: "Cycle forgets you used the implant until 3 November
  2027.")
- Today's bleeding card is always "Last 90 days", never "Since …"; the first-months line and the
  copper IUD's heavier-periods card never show; no cycle is marked as cut short.
- On a combined method with a break every month, there is no first break to estimate. The period
  from setup (A2) falls inside the stretch and counts as her last bleed, so Today shows the next
  bleed from it as in B. With no bleed logged, Today shows "Pill" and "Cycle will estimate your next
  bleed once you log one.", with no Next bleed card, and the calendar expects nothing, until she
  logs one.

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

A bleed logged before 24 May is bleeding between breaks: it shows on the calendar, and the card
still says "24 to 30 May". Later on the pill, once a bleed has counted (21 days or more after the
start, or after the last bleed that counted): "Your next bleed is expected in about 6 days"; card
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

On the monthly combined injection, Today looks like C1 with display "Monthly injection", its calm
line ("Bleeding on the monthly injection follows your injections, not a cycle, and it varies, so
Cycle doesn't estimate it."), and "What changes on the monthly injection?"; the calendar's hint is
"Cycle doesn't estimate bleeding on the monthly injection. Tap any day up to today to log or change
it."; History's card is **MONTHLY INJECTION**, with its last 90 days.

The rest of History's copy for a method ([MOT-55](https://linear.app/tonypine/issue/MOT-55)):

- A combined pill, patch or ring with a break every month counts its bleeds instead of the 90 days:
  "You logged 6 bleeds on it." ("You logged no bleeds on it."), the withdrawal bleed after it
  stopped included.
- A method younger than 90 days counts since its start, as on Today: "Since September 6: you logged
  bleeding or spotting on 4 days, in 2 episodes. The longest lasted 3 days."
  Once stopped, one shorter than 90 days counts all of it: "In that time you logged …". With nothing
  logged: "… you logged no bleeding or spotting."
- The cycle a method cut short says how: "cut short when you started the pill" (the mini pill, the
  patch, the ring), "when the implant was fitted", "when the IUD was fitted", "when you had your
  first injection" (either injection). Its detail adds "Cut short when the implant was fitted. Not
  part of your typical cycle." under its dates.
- "See it in the calendar" opens the month of the method's last day so far: this month while she is
  on it, else the month it stopped.

### D. Implant removed

Today is Monday 15 November 2027. The implant came out on 3 November.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| D1 | Settings | Settings | Contraception · "Implant, since 9 Nov 2026" |
| D2 | Contraception | Settings › Your cycle › Contraception | "Now": "Implant", "Since 9 November 2026", the calm line. **Mark as stopped** · Change method. No "When to get help" on the implant. "Your methods": Implant · Since 9 Nov 2026 › |
| D3 | Stopped on | Settings › Contraception › Mark as stopped | "When was your implant taken out?" "Roughly is fine." Calendar, 3 November chosen. **Save** |
| D4 | Contraception: none now | Settings › Your cycle › Contraception | "Now": "None", "Cycle estimates your periods from your own cycle." Add your method (a start inside the implant's dates: below). "Your methods": Implant · 9 Nov 2026 to 3 Nov 2027 › |
| D5 | Today: fresh estimates | Today | "MONDAY 15 NOVEMBER", display "12 days", "since your implant came out". **My period started** · Log how you feel. "Next period": "Around 2 December", "Between 25 November and 9 December", "Estimated from your usual 29-day cycle. Cycles can take a few months to settle after the implant, so the range is wider." How is this estimated? |
| D6 | History | History | The implant's card now reads "9 Nov 2026 to 3 Nov 2027", "Not part of your typical cycle.", "In its last 90 days you logged bleeding or spotting on 10 days, in 3 episodes. The longest lasted 5 days." |

The stop date is her last day on the implant. Had she marked it stopped with today's date, Settings
would show "None" at once, but Today would keep the implant's words until tomorrow, and D5's
estimates would start then.

Once she logs her first period after it, Today is "Day 1" again, with the ±7-day range until she has
logged three cycles. "Missed a period?" is not asked before that first period. Other methods:
"since you stopped the pill", "since your IUD came out".

**Adding a method after it** (from D4's Add your method, the same steps as F1 to F3). On 15
November she adds the pill from 20 October, inside the implant's dates: the dialog asks "Move the
end of your implant?" "Your implant would end on 19 October instead of 3 November, so the two don't
overlap. Cycle works out its estimates again." Cancel · **Move it**. Cancel goes back to the
calendar. A day on or before 9 November 2026 would cover all of the implant and is refused: "That
would cover all of your time on the implant. Delete the implant's dates first, or pick a later
day." A day after 3 November needs no dialog.

**After two methods.** Say she used the ring after the implant, from 20 November 2027 to 10
January 2028, and on 15 February 2028 adds the pill from 1 November 2027. That day is inside the
implant's dates but would cover all of the ring, so it is refused, with no dialog for the implant:
"That would cover all of your time on the ring. Delete the ring's dates first, or pick a later
day." Any day up to 20 November is refused the same way. From 21 November to 10 January the dialog
offers to move the ring's end ("Move the end of your ring?"); from 11 January nothing overlaps.

**After the pill.** A bleed in the 7 days after a combined method stops is the withdrawal bleed,
part of the time on it, not a period. Her last pill was on 3 November 2027 and she logs a bleed on 6
November:

- From 4 to 10 November, Today shows "N days", "since you stopped the pill", with **Bleed started**
  ("Bleeding started" after a break every few packs or none). From 11 November it is **My period
  started**.
- On 8 November, during the bleed: "5 days", "since you stopped the pill", **Bleed ended**. The
  calendar and TalkBack call its days "bleed", to its last day, even past 10 November.
- The bleed does not end the days-since display and does not count as her first period. "Next
  period" still counts from the stop date: "Around 2 December", "Between 25 November and 9
  December", "Estimated from your usual 29-day cycle. Cycles can take a few months to settle after
  the pill, so the range is wider." "Missed a period?" still waits for her first period.
- A bleed that starts on 11 November or later is her first period: "Day 1", as above.

**After the injection.** Her last injection was on 3 August 2027; she marks it as stopped on 20
August with that date ("When was your last injection?", "Cycle counts 13 weeks from it."). The
stretch now ends on 2 November, and until then she is still on the injection:

- Contraception's "Now" card stays "Injection", "Since 9 November 2026", the calm line, then
  "Cycle counts it until 2 November 2027, 13 weeks after your last injection." **Change method**
  stays; "Mark as stopped" is gone until the stop date has passed. Its edit page reads First
  injection · 9 November 2026; Counted until · 2 November 2027 (any day up to 13 weeks from today,
  for an 8-week injection); Delete these dates.
- Today, the calendar and History are unchanged: "Injection", the calm line, the Last 90 days card,
  "bleeding".
- Changing method in those weeks ends the injection the day before: an implant fitted on 31 August
  makes it "Injection · 9 Nov 2026 to 30 Aug 2027".

From 3 November, Contraception shows "None" and the injection under "Your methods" as "9 Nov 2026
to 2 Nov 2027". Today shows "12 days", "since you stopped the injection" (on 14 November), and no
next period: "Periods can take several months to come back after the injection. Cycle will estimate
again once you log one."

**After the monthly combined injection.** The same, with 4 weeks. Monthly injection since 9 November
2026, last injection on 3 August 2027; she marks it as stopped on 10 August with that date ("When
was your last injection?", "Cycle counts 4 weeks from it."):

- Until 31 August, Contraception's "Now" card stays "Monthly combined injection", "Since 9 November
  2026", the calm line, then "Cycle counts it until 31 August 2027, 4 weeks after your last
  injection." Its edit page reads First injection · 9 November 2026; Counted until · 31 August
  2027 (any day up to 4 weeks from today); Delete these dates. Today, the calendar and History are
  unchanged; a bleed she logs on 19 August is "bleeding", part of the time on it.
- From 1 September, Contraception shows "None" and "Monthly injection · 9 Nov 2026 to 31 Aug 2027"
  under "Your methods". On 12 September Today shows "12 days", "since you stopped the monthly
  injection", and no next period: "Periods can take a few months to come back after the monthly
  injection. Cycle will estimate again once you log one."

### E. Switching from the pill to a hormonal IUD, then correcting the start date

Today is Friday 17 September 2027. Pill since 3 May. The IUD was fitted on 6 September; she first
enters 13 September by mistake.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| E1 | Contraception: on the pill | Settings › Your cycle › Contraception | "Now": "Combined pill", "Since 3 May 2027 · a break every month", the calm line. Mark as stopped · **Change method**. Then "When to get help" for the combined pill, in full ([When to get help](#when-to-get-help)). "Your methods" and the note follow, below the fold. |
| E2 | Change method | Settings › Contraception › Change method | "Which method?" The list, Hormonal IUD chosen. **Next** |
| E3 | Since when (wrong day) | Settings › Contraception › Change method | "When was your IUD fitted?" "Roughly is fine." 13 September chosen. **Save**. The pill now ends on 12 September. A day on or before 3 May is refused: "That's before you started the pill on 3 May. Pick a later day, or change the pill's dates first." |
| E4 | Contraception: switched | Settings › Your cycle › Contraception | "Now": "Hormonal IUD", "Since 13 September 2027", its calm line. Then "When to get help" for an IUD, as in F4; the phone shows the page scrolled to its end. "Your methods": Hormonal IUD · Since 13 Sep 2027 ›, Combined pill · 3 May to 12 Sep 2027 › |
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
| F1 | Contraception | Settings › Your cycle › Contraception | "None". **Add your method**. With an earlier method under "Your methods", a start inside its dates offers to move its end, and one on or before its start, or one that would cover a later method whole, is refused ([Since when](#since-when), journey D). |
| F2 | Choose copper IUD | Settings › Contraception › Add | The list, Copper IUD chosen. **Next** |
| F3 | Since when | Settings › Contraception › Add | "When was your IUD fitted?" 15 June chosen, during her period. **Save** |
| F4 | Contraception: copper IUD | Settings › Your cycle › Contraception | "Copper IUD", "Since 15 June 2027", "Your cycle stays your own, so Cycle keeps estimating your periods. They can be heavier or longer at first." Mark as stopped · Change method. Then "When to get help" for an IUD, in full ([When to get help](#when-to-get-help)). |
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
| `UsualLengthSliders` (`core:ui`, two `SliderField`s) | A3 | Exists |
| `Card`, `ClickableCard` | Today's cards, History, Contraception's Now card and When to get help | Exist |
| `FilledButton`, `TonalButton`, `TextButton` | Throughout | Exist |
| `CycleBottomSheet` | C3 and the estimate sheet, E6 | Exists |
| `CycleAlertDialog`, `CycleDestructiveDialog` | E7; deleting dates | Exist |
| Settings' `SettingsRow` (feature code, a `ClickableCard`) | B1, D2, E4, E5 | Exists in `feature:settings` |
| Settings' `SectionTitle` (feature code, a heading) | Contraception's "Now", "When to get help" and "Your methods" | Exists in `feature:settings` |
| **`RadioRow`** | The method list (A4, B3, E2, F2) and breaks (B5) | Added ([MOT-52](https://linear.app/tonypine/issue/MOT-52)), with `RadioGroup`; the method and breaks lists are `MethodList` and `BreaksList` in `core:ui`. A single-choice row like `SwitchRow`: title in `titleSmall`, an optional one-line body in `bodySmall`, a radio at the end; `accentContainer` when selected; 48dp at least; `Role.RadioButton` in a `selectableGroup`, read as "Implant, A rod in the arm, such as Nexplanon, radio button, selected, 6 of 10". `ButtonGroup` does not fit: ten options with a line each. |
| **`BleedingWords`** | Calendar legend, day cells, week row | Added ([MOT-54](https://linear.app/tonypine/issue/MOT-54)). An enum, `Period` (default), `Bleed`, `Bleeding`, passed to `DayCell`, `MonthCalendar`, `WeekRow` and `CycleLegend`. It changes the legend labels ("Bleed", "Expected bleed", "Bleeding") and TalkBack ("14 October, bleeding"); the shapes and colours stay. A calendar spanning a method's start takes the word per day; its legend names the logged days and the predicted ones each in their own words. |
| **`CycleIcons.Medication`** | The Contraception row in Settings | Added ([MOT-52](https://linear.app/tonypine/issue/MOT-52)). Material Symbols Rounded "medication", weight 600, like the others. The journeys page draws a stand-in capsule. |

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
- The only words about urgent symptoms are "When to get help", in the words of
  [`0007`](../decisions/0007-urgent-symptoms-on-a-method.md): no condition names, no phone numbers,
  no step about the method, and never set off by what she logs.
