# Predictions: the next period, the fertile window and ovulation

What a tracker can honestly predict from what she logs, how accurate each method is, and how Cycle
should compute and show its estimates. Read [`cycle-physiology.md`](cycle-physiology.md) first for
the phases and normal ranges.

## What can be known, and how well

| Estimate | From period dates alone | With more signs |
| -- | -- | -- |
| Next period | Reasonable. Most people's cycles move by a few days a month (mean 2.6 days, [Bull 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6710244/)), so a date with a range of a few days is honest. | A confirmed ovulation (temperature rise) sharpens it, because the luteal phase is steadier than the follicular phase. |
| Ovulation day | Poor. Calendar methods and apps hit the actual day no more than 21% of the time ([Johnson 2018](https://www.tandfonline.com/doi/pdf/10.1080/03007995.2018.1475348)); in one test, 3 of 36 app predictions were exactly right and two thirds were 2 to 9 days early ([Worsfold 2021](https://journals.sagepub.com/doi/10.1177/17455065211049905)). | A positive LH test anticipates it by one to two days; a sustained temperature rise confirms it afterwards. |
| Fertile window | Poor. Only about 30% of women have their fertile window entirely within days 10 to 17, and on every day from 6 to 21 at least 10% of women are in it, even with regular cycles ([Wilcox 2000](https://pubmed.ncbi.nlm.nih.gov/11082086/)). | Cervical mucus and temperature together (the symptothermal method) are the most reliable home approach. |

The fertile window itself is well established: about six days, the five before ovulation and the day
of ovulation, with the chance of conception rising from about 10% five days before to about 33% on
the day ([Wilcox 1995](https://www.nejm.org/doi/full/10.1056/NEJM199512073332301)).

## Methods

### Calendar methods (period dates only)

- **Average or median of past cycles.** What most apps do for the next period. A median of recent
  cycles resists a single odd month better than a mean.
- **Counting back for ovulation.** Ovulation is placed a fixed number of days before the predicted
  next period. Apps traditionally use 14; real luteal phases average 12.4 days (95% between 7 and 17,
  [Bull 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6710244/)), and in 28-day cycles the most
  likely ovulation day is day 16 ([Johnson 2018](https://www.tandfonline.com/doi/pdf/10.1080/03007995.2018.1475348)),
  which is 12 days before the next period.
- **Calendar rhythm rule.** Over at least the last six cycles: first fertile day = shortest cycle −
  18, last fertile day = longest cycle − 11 ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)).
  Wide by design, because it covers her whole range.
- **Standard Days Method.** For cycles between 26 and 32 days, treats days 8 to 19 as fertile
  ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)).

### Symptom-based methods (body signs)

- **LH tests.** Urine strips detect the LH surge, which starts about a day and a half before
  ovulation ([StatPearls](https://www.ncbi.nlm.nih.gov/books/NBK546686/)). The positive day and the
  day after are the most fertile.
- **Basal body temperature.** Rises 0.2 to 0.5 °C after ovulation; three days above her earlier level
  mean ovulation has passed ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)).
  Confirms, never predicts. Wearables now estimate the same rise from overnight skin temperature
  (Apple Watch does this; [Apple](https://support.apple.com/en-us/120356)).
- **Cervical mucus.** Wet, clear, stretchy secretions mark the fertile days; the TwoDay and ovulation
  (Billings) methods are built on it.
- **Symptothermal.** Mucus, temperature and calendar rules combined.

### Effectiveness for avoiding pregnancy

For context only: Cycle will not offer contraception (see below). Pregnancies per 100 women in the
first year ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)):

| Method | Consistent and correct use | As commonly used |
| -- | -- | -- |
| Standard Days | 5 | 12 |
| TwoDay | 4 | 14 |
| Ovulation (mucus) | 3 | 23 |
| Symptothermal | <1 | 2 |
| Natural Cycles (app with temperature; FDA-cleared in 2018) | about 2 | 7 |

The Natural Cycles row is from its own studies, which the FDA cleared it on: 93% effective with
typical use, about 98% with perfect use
([Natural Cycles, FDA classification](https://www.prnewswire.com/news-releases/fda-releases-final-classification-for-natural-cycles-the-first-and-only-birth-control-app-in-the-us-300808657.html)).
That clearance made it a regulated medical device (De Novo, class II): an app that tells someone when
they can have unprotected sex is regulated as one. Reliable effectiveness figures do not exist for
calendar rhythm or temperature alone, and as commonly used about 15 in 100 couples relying on
periodic abstinence (mostly calendar rhythm) get pregnant in a year
([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)).

## How wrong predictions feel

In a survey of tracker users, 54.9% had had a period arrive earlier than predicted and 72.1% later.
Wrong predictions brought frustration, anxiety, stress and fear of pregnancy; late ones caused the
most anxiety ([Broad 2022](https://journals.sagepub.com/doi/10.1177/17455057221095246)). A precise date
that turns out wrong costs more trust than an honest range.

## Recommended approach for Cycle

A starting point for the prediction ticket, not a decision. The next-period rules Cycle follows are
decided in [`0003-cycle-estimates.md`](../decisions/0003-cycle-estimates.md); the fertile window
and ovulation are still open.

### Inputs

- **Periods** come from logged bleeding days: a run of period days, allowing a gap of a day without
  a log inside it, is one period. Spotting alone does not start one.
- **Cycles** run from one period's first day to the day before the next. The current cycle is open
  until the next period is logged.
- **Recent cycles** are the last six complete cycles that are not excluded. Exclude cycles that span
  a pregnancy, the months after birth until three regular cycles return, or a stretch on hormonal
  contraception ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)), and
  ask about a cycle about twice her usual length before counting it: it may be a missed log. After
  a hormonal method, natural cycles count from the first period after stopping (after the
  injection, from the first one after the next injection would have been due); cycles with a copper
  IUD count as usual ([`contraception.md`](contraception.md)).

### Next period

- **Expected start** = last period start + the median of recent cycles.
- **Range** = last period start + her shortest to longest recent cycle.
- **Expected length** = the median of her recent periods' lengths.
- **Little data.** With no complete cycle, use the typical length she gives at setup (default 28
  days, 5-day period) and a ±4-day range, about half of FIGO's 7–9-day regularity spread (the most
  a regular cycle's shortest and longest lengths may differ)
  ([FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666)). With one or two
  cycles, keep at least ±3 days. Say the estimate will improve as she logs.
- **When it is late.** Never show a prediction in the past. Once the expected day passes without a
  log, keep the predicted period starting from today and show how many days later than expected it
  is, in neutral words. No alarms and no pregnancy prompts; a late period is mentioned only when it
  reaches a threshold in [`health-signals.md`](health-signals.md).
- **Further ahead.** Predict at most the next three cycles on the calendar, each with its range.

### Fertile window and ovulation (calendar only)

- **Estimated ovulation** = expected next start − 12 days, from Bull's 12.4-day mean luteal phase and
  Johnson's day 16 in 28-day cycles.
- **Fertile window** = the five days before the estimated ovulation and the day itself (Wilcox).
- **Labelled as estimates**, everywhere: on the calendar, in the legend, in TalkBack descriptions.
- Whether to show the narrow six-day window or her full range (the calendar rhythm rule) depends on
  what she wants it for. That is an open question in
  [`product-implications.md`](product-implications.md).

### With body signs, if she logs them

- **Positive LH test**: estimated ovulation moves to the day after the first positive result; the
  window ends there.
- **Temperature**: three readings above her earlier level confirm ovulation retrospectively. Then
  the window closes, the luteal phase is counted from the confirmed day, and the next period
  estimate becomes confirmed ovulation + her luteal length (or 12 days until she has her own).
- **Mucus**: wet or egg-white mucus marks a fertile day, whatever the calendar says.

### Worked example (synthetic)

Her last six cycles were 26, 27, 28, 28, 29 and 31 days, and her last period started on 2 March 2027.

| Estimate | Calculation | Result |
| -- | -- | -- |
| Expected next period | 2 March + 28 days (median) | 30 March |
| Range | 2 March + 26 to + 31 days | 28 March to 2 April |
| Estimated ovulation | 30 March − 12 days | 18 March (cycle day 17) |
| Fertile window | 5 days before ovulation to ovulation | 13 to 18 March |
| Calendar rhythm range, for comparison | days 26 − 18 = 8 to 31 − 11 = 20 | 9 to 21 March |

## What Cycle should never do

- **Never offer contraception.** No "safe days", "low chance" or "you can't get pregnant" wording, no
  green "go" days. Doing it reliably needs a regulated medical device, and the as-commonly-used
  numbers above show how often calendar estimates fail.
- **Never show an estimate as a fact.** Ovulation and the fertile window are always "estimated";
  the next period comes with its range.
- **Never compute fertility on hormonal contraception, in pregnancy, or after birth until cycles
  return.** Hide those estimates in those modes rather than show meaningless ones.
- **Never predict a period that is not one.** On a combined pill, patch or ring the bleed follows the
  pack and is predicted as a bleed in the break; on the implant, the injection, the
  progestogen-only pill and, by default, the hormonal IUD, bleeding follows no cycle and gets no
  next date. The bleed after a monthly combined injection follows the injection, not a cycle, and
  by default gets no next date either ([`contraception.md`](contraception.md)).
- **Never send prediction data off the device** to improve the algorithm. Everything above runs
  locally on a few numbers ([`0001-stack.md`](../decisions/0001-stack.md)).
