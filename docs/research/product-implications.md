# What the research means for Cycle

The synthesis of this folder: principles for every feature, the candidate features and screens in a
sensible order, defaults, copy rules, and the questions only she can answer. It proposes; she
decides, through tickets. When a ticket settles one of these, update this page.

## Principles

1. **Her log is the truth; everything else is an estimate.** Logged periods are facts. Predicted
   periods come with a range; ovulation and the fertile window are always labelled as estimates
   ([`predictions.md`](predictions.md)).
2. **Her own numbers beat averages.** Start from 28 days only until she has history, then use hers.
   The 28-day cycle with ovulation on day 14 describes a minority of cycles
   ([`cycle-physiology.md`](cycle-physiology.md)).
3. **Not contraception, not diagnosis.** No "safe days", no condition names as causes. The app shows
   patterns and says when a doctor would want to hear about them
   ([`health-signals.md`](health-signals.md)). It records the contraception she uses so its words
   and estimates fit, but never acts as contraception and never advises on a method
   ([`contraception.md`](contraception.md)).
4. **Nothing leaves the phone** unless a decision record says otherwise. No SDKs that phone home
   ([`apps-and-privacy.md`](apps-and-privacy.md), [`0001-stack.md`](../decisions/0001-stack.md)).
5. **Fast to log, nothing required.** One tap for a period; every other category optional and
   hideable; no nagging about empty days ([`tracking-data.md`](tracking-data.md)).
6. **Calm.** No alarms for a late period, no red banners, no telling her how to feel. The visual
   direction already says this ([`visual-directions.md`](../design/visual-directions.md)).
7. **Life changes the cycle.** Contraception, pregnancy, birth, breastfeeding and perimenopause are
   modes that change or pause predictions, not reasons to start over. Each contraceptive method
   changes them differently ([`contraception.md`](contraception.md)).
8. **Discreet.** Notifications, widgets and the recent-apps preview reveal only what she allows.

## Candidate features, in order

Each step is usable on its own. The order follows dependencies: predictions need logged periods,
health signals need several cycles, a doctor summary needs the signals' data.

### 1. Track periods (the first release)

- **Today screen**: cycle day ("Cycle day 19"), the next period's expected date and range ("Period
  expected around 30 March, between 28 March and 2 April"), and one button to log a period starting
  today, or ending it while one is in progress.
- **Calendar**: month view with the existing `DayCell` states (period, predicted period, today) and
  the `CycleLegend`. Tap a day to log or edit it, including past days.
- **Day log sheet**: flow (light, medium, heavy) and spotting to start with, on the existing bottom
  sheet.
- **Cycle history**: a list of past cycles with their length and period length, and her typical
  values (median cycle, median period, shortest and longest).
- **Setup**: optionally her usual cycle and period length and the date of her last period, so the
  first prediction is not blind. Skippable.
- **Data**: Room tables per [`tracking-data.md`](tracking-data.md), cycles derived from days,
  predictions computed and never stored. Export from the start.

### 2. Log how she feels

- More categories in the day log: pain with severity, mood, physical symptoms, energy, sleep, sex,
  notes. A settings screen to show or hide each.
- **Patterns**: for each symptom, which cycle days it tends to fall on (for example "Headaches: most
  often on days 26 to 2"), once she has logged two or three cycles.

### 3. Estimates and reminders

- **Fertile window and ovulation** on the calendar, labelled as estimates, if she wants them (see the
  questions below). The `Fertile` and `Ovulation` day states already exist.
- **Reminders**, all off by default: period expected in N days, log today's period, take the pill.
  Neutral wording she can change.
- **Contraception**: she asked for Cycle to know her method
  ([MOT-48](https://linear.app/tonypine/issue/MOT-48)), so this can come before the rest of this
  step. Her method and the dates she started and stopped it, then per method: what to estimate
  (the next bleed follows the pack on a combined pill, patch or ring; nothing on the implant,
  injection or progestogen-only pill), what to hide (fertile window and ovulation on every hormonal
  method), the words ("period", "bleed" or "bleeding"), a 90-day bleeding summary on
  progestogen-only methods, and the signals that fit the method
  ([`contraception.md`](contraception.md)). The design is
  [MOT-50](https://linear.app/tonypine/issue/MOT-50).
- **Other life-stage modes**: pregnant (predictions paused), after birth or breastfeeding
  (predictions paused until three regular cycles), perimenopause (wider ranges, no fertility
  estimates by default).

### 4. Health signals and a doctor summary

- **Signal cards** from [`health-signals.md`](health-signals.md), over the last six cycles, each
  dismissible, all switchable off.
- **Doctor summary**: a screen and a shareable file (PDF or CSV, created on the phone, shared only
  when she taps share) with cycle and period lengths, flow, pain and the symptoms she picks, for a
  chosen range.
- **Heavy-bleeding details and a daily symptom severity scale**, which the signals and a PMS diary
  need.

### 5. Privacy and data controls

Can move earlier if she wants them sooner; none depend on the steps above.

- App lock (PIN or biometric) and hidden content in the recent-apps preview: done, as "Lock Cycle"
  with the phone's own lock ([`0005`](../decisions/0005-app-lock.md),
  [MOT-47](https://linear.app/tonypine/issue/MOT-47)). Delete everything: done
  ([MOT-40](https://linear.app/tonypine/issue/MOT-40)).
- Import from a CSV export of another tracker, if she has history elsewhere.

### 6. Only if she asks

- **Fertility signs** (temperature charts, LH and mucus logging), with ovulation confirmed from
  temperature as in [`predictions.md`](predictions.md).
- **Health Connect** import or export, which [`0001-stack.md`](../decisions/0001-stack.md) leaves for
  later.
- A home-screen widget (with the discretion rules above).

### Not planned

- Cycle acting as contraception: "avoid pregnancy" modes, safe or green days, "low chance"
  wording, protection or missed-pill advice, and advice on choosing, starting or stopping a method.
  Recording the contraception she uses, and adapting estimates to it, is planned (step 3).
- Accounts, sync, a backend, analytics, ads, community or content feeds.
- Diagnoses, risk scores or AI-generated health advice.

## Defaults

| Setting | Default | Why |
| -- | -- | -- |
| Usual cycle and period length, before any history | 28 and 5 days | Common convention; replaced by her data after the first complete cycle. |
| Cycles used for predictions | The last 6 complete cycles, median | Matches the calendar rhythm rule and Apple's six-month window; a median resists one odd month. |
| Prediction range with little data | ±4 days with no cycles, at least ±3 with one or two | About half of FIGO's 7–9-day regularity spread ([FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666)). |
| Cycles shown ahead on the calendar | 3 | Further out, the range grows too wide to help. |
| Fertile window and ovulation | Depends on her answer to the first question below; always off on a hormonal method | Meaningless or unwanted for some goals, and meaningless on hormonal contraception ([`contraception.md`](contraception.md)). |
| Contraception | None set | She sets her method and its start date; nothing is assumed. |
| Health signal cards | On, dismissible, switchable off; to confirm with her | Useful and calm when worded as in [`health-signals.md`](health-signals.md). |
| Notifications | All off | She opts in to each. |
| Temperature unit, first day of the week, date format | From the phone's locale | No setting needed until she asks. |

## Copy rules

- **Say "period"**, not "that time", flowers or euphemisms
  ([`visual-directions.md`](../design/visual-directions.md)).
- **Estimates read as estimates**: "expected around", "between", "estimated ovulation". Never a bare
  date for anything she did not log.
- **Late is neutral**: "Your period is 3 days later than expected." No "Are you pregnant?" prompts.
- **Signals describe, then suggest**: what she logged, then that it is worth mentioning to a doctor.
  No condition names as causes, no "abnormal".
- **Never**: "safe day", "low chance", "you can't get pregnant", "you should feel".
- **TalkBack reads the same words**, including "estimated" ("18 March, estimated ovulation").
- **Discreet surfaces** (notifications, widget) use neutral text by default: "Reminder from Cycle".

### On contraception

From [`contraception.md`](contraception.md):

- **"Period" only when she has her own cycle**: with no method or a copper IUD.
- **"Bleed" for the scheduled bleed on a combined pill, patch or ring**: it is a withdrawal bleed,
  not a period. "Bleed expected in your pill break", not "period expected".
- **"Bleeding" on progestogen-only methods** (pill, implant, injection, hormonal IUD) and for
  unscheduled bleeding on a combined method. These are the accurate words, not euphemisms.
- **No cycle language where there is no cycle**: no "cycle day", phases or "late" on the implant,
  injection or progestogen-only pill. Describe bleeding over the last 90 days in plain counts.
- **Never advise on her method**: no "consider switching", "you may want to stop", "you are
  protected" or missed-pill guidance. Questions about her method go to her pharmacist, nurse or
  doctor.
- **Expected changes read as expected**: in the first months on a method, unpredictable bleeding is
  common, and the copy says so calmly rather than flagging it.

## Open questions for her

The answers change features, defaults and copy. Each is worth one short ticket or a comment on the
next planning ticket.

1. **What does she want the app for?** Knowing when her period comes; understanding symptoms or
   moods; trying to conceive, now or later; or noticing changes with age. This decides whether
   fertility estimates appear at all, and whether the narrow six-day window or the wider calendar
   range fits ([`predictions.md`](predictions.md)).
2. **Does she use contraception?** Being taken up: she wants Cycle to record whether she has an
   implant or takes the pill ([MOT-48](https://linear.app/tonypine/issue/MOT-48)). The research is
   in [`contraception.md`](contraception.md); the screens and the decision come from
   [MOT-50](https://linear.app/tonypine/issue/MOT-50). Still hers to answer there: whether she wants
   a next-bleed estimate on the pill, a 90-day summary, and a pill or injection reminder.
3. **Does she have history in another app** she wants to bring over? Which app, and can it export?
4. **What does she want to log** beyond her period? The list in
   [`tracking-data.md`](tracking-data.md) is a menu, not a requirement.
5. **Does she want health signal cards**, and a doctor summary?
6. **Which reminders, if any**, and how discreet should they be?
7. **Does she want an app lock?** Answered: yes, optional and off by default, with a 30-second grace
   period ([`0005`](../decisions/0005-app-lock.md)). Backups already require end-to-end encryption,
   so a phone without a screen lock gets none ([`0004`](../decisions/0004-backup-encryption.md)); if
   she would rather have an unencrypted backup than none, that record changes.

## Effects on what already exists

- The `CycleLegend` labels ("Estimated fertile window", "Estimated ovulation") and the day cell's
  TalkBack descriptions ("18 March, estimated ovulation") say ovulation and the fertile window are
  estimates ([MOT-29](https://linear.app/tonypine/issue/MOT-29)).
- The `Predicted period` day state (dashed edge on a pale fill) already reads as uncertain; the range
  of a predicted period could use the same treatment, with the most likely start emphasised.
- The Today screen ("Your period could start any day now", "Missed a period?", the late message and
  the next-period estimate), the calendar's predicted periods and the period labels all assume a
  natural cycle. On a hormonal method they change as [`contraception.md`](contraception.md)
  describes; [MOT-50](https://linear.app/tonypine/issue/MOT-50) designs how.
- Backups to Google Drive carry the database and settings only when end-to-end encrypted with the
  phone's screen lock ([`0004`](../decisions/0004-backup-encryption.md),
  [MOT-28](https://linear.app/tonypine/issue/MOT-28)).
