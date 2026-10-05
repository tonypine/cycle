# The cycle: phases, hormones and normal ranges

What happens in a menstrual cycle, how long each part lasts, how much that varies from person to
person and from month to month, and how it changes over a lifetime. The numbers here are the
defaults and bounds the rest of the app should use.

## The cycle in one paragraph

A cycle runs from the first day of one period (cycle day 1) to the day before the next period
starts. Its length is the number of days in between. During the first part, a follicle in the ovary
matures and the womb lining rebuilds; then the ovary releases an egg (ovulation); then the remains
of the follicle make progesterone, which holds the lining in place. If no pregnancy follows, that
progesterone falls after about two weeks, the lining sheds, and the next period starts.

## Phases

The phases overlap, and an app should name them only as well as it can know them. The period and
the cycle day are certain, because she logs them. Ovulation is only ever an estimate unless she
logs an ovulation test or her temperature (see [`predictions.md`](predictions.md)).

| Phase | When | What happens | How long |
| -- | -- | -- | -- |
| Period (menstruation) | From cycle day 1 | The womb lining sheds. | 2 to 7 days, usually about 5 ([NHS](https://www.nhs.uk/conditions/periods/)); up to 8 is within FIGO's normal limit ([FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666)). |
| Follicular phase | Cycle day 1 to ovulation (includes the period) | FSH makes follicles grow; the leading follicle makes rising oestrogen; the lining thickens; cervical mucus gets wetter and more slippery. | The variable part: mean 16.9 days, 95% of cycles between 10 and 30 ([Bull 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6710244/)). |
| Ovulation | One day, mid-cycle at best | High oestrogen triggers an LH surge; the egg is released roughly a day and a half after the surge starts ([StatPearls](https://www.ncbi.nlm.nih.gov/books/NBK546686/)). | The egg lives about a day. |
| Luteal phase | Day after ovulation to the day before the next period | The corpus luteum makes progesterone; resting body temperature rises 0.2 to 0.5 °C and stays up until the next period ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)); mucus dries up. Premenstrual symptoms, when she has them, fall at the end of this phase. | Steadier: mean 12.4 days, 95% between 7 and 17 ([Bull 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6710244/)). |

Two consequences follow, and the app should respect both:

- **Cycle length varies because the follicular phase varies.** A long cycle usually means a late
  ovulation, not a long luteal phase. Counting back from the next period is a better guess at
  ovulation than counting forward from the last one.
- **"Ovulation on day 14" is a myth for most cycles.** Only 13% of 612,613 cycles in Bull's dataset
  were exactly 28 days long, and even in 28-day cycles the mean follicular phase was 15.4 days, not
  14 ([Bull 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6710244/)).

## Body signs through the cycle

These are the signs she can observe and log. Each is useful for something different.

| Sign | Pattern | What it tells |
| -- | -- | -- |
| Bleeding | Period at the start of the cycle; spotting can occur at other times. | Marks cycle day 1. Bleeding between periods or after sex is never "normal" in FIGO's scheme and is worth a doctor's visit ([FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666), [NHS](https://www.nhs.uk/conditions/periods/)). |
| Cervical mucus | Dry or none after the period, then sticky or creamy, then wet, clear and stretchy (like raw egg white) near ovulation, then dry again. | The fertile window opens with the first wet secretions ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)). Needs daily checks and some practice. |
| Basal body temperature (BBT) | Lower before ovulation; rises 0.2 to 0.5 °C just after it and stays higher until the next period. Measured on waking, before getting up, at the same time each day. | Confirms ovulation after the fact: three days above the earlier level mean ovulation has passed ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)). It cannot warn in advance. Fever, alcohol, broken sleep and a changed wake time disturb it. |
| LH (ovulation) test | Urine strips turn positive on the LH surge, one to two days before ovulation. | The only home sign that looks ahead. |
| Premenstrual symptoms | Mood changes, irritability, breast tenderness, bloating, headache, fatigue, cravings in the days before the period; they ease once it starts. | Up to 80% notice some; 20 to 32% have PMS that affects daily life; 3 to 8% have PMDD ([AAFP 2011](https://www.aafp.org/pubs/afp/issues/2011/1015/p918.html)). |
| Mid-cycle pain | A one-sided twinge around ovulation (mittelschmerz) for some. | A hint, not a measurement. |

## What is normal

The ranges clinicians use, all for people who are not pregnant, not breastfeeding and not on
hormonal contraception.

| Measure | Normal | Source |
| -- | -- | -- |
| Cycle length, adults | 24 to 38 days (FIGO); the NHS uses 21 to 35. 91% of 1.6 million Flo users had a median cycle between 21 and 35 days. | [FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666), [NHS](https://www.nhs.uk/conditions/irregular-periods/), [Grieger 2020](https://www.jmir.org/2020/6/e17109/) |
| Cycle length, first years after the first period | 21 to 45 days | [ACOG CO 651](https://www.acog.org/clinical/clinical-guidance/committee-opinion/articles/2015/12/menstruation-in-girls-and-adolescents-using-the-menstrual-cycle-as-a-vital-sign) |
| Average cycle | 29.3 days (SD 5.2) across 612,613 cycles; the "28-day cycle" is a rounded convention. | [Bull 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6710244/) |
| Regularity | Shortest to longest cycle over a year within 7 to 9 days, depending on age (≤9 at 18–25, ≤7 at 26–41, ≤9 at 42–45). | [FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666) |
| Month-to-month variation | Mean 2.6 days per person (SD 2.5): most people's cycles shift by a few days every month. | [Bull 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6710244/) |
| Period length | 2 to 7 days, usually about 5; more than 8 days is prolonged (FIGO), more than 7 is a reason to see a GP (NHS, CDC). | [NHS](https://www.nhs.uk/conditions/periods/), [FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666), [CDC](https://www.cdc.gov/female-blood-disorders/about/heavy-menstrual-bleeding.html) |
| Blood loss | 20 to 90 ml (1 to 5 tablespoons) per period (NHS); 5 to 80 ml (FIGO). Nobody measures this at home, so heavy bleeding is judged by its signs and its effect on her life (see [`health-signals.md`](health-signals.md)). | [NHS](https://www.nhs.uk/conditions/periods/), [FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666) |

## How the cycle changes with age and life stage

Cycles are not fixed. A tracker that assumes they are will be wrong at both ends of life and around
every pregnancy.

- **Age.** Cycles shorten with age through the reproductive years: from a mean of 30.3 days at 18–24
  to 27.4 days at 40–45, about 0.18 days a year, almost all of it in the follicular phase
  ([Bull 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6710244/)). Variability is lowest at 35–39
  and about 45% higher under 20 and at 45–49; after 50, cycles lengthen again
  ([Li 2023, Apple Women's Health Study](https://pmc.ncbi.nlm.nih.gov/articles/PMC10226714/)).
- **Other factors.** In the Apple study, cycles were 1.6 days longer for Asian than white
  participants and 1.5 days longer at a BMI of 40 or more, with more variability in both groups
  ([Li 2023](https://pmc.ncbi.nlm.nih.gov/articles/PMC10226714/)). Stress, illness, weight change,
  intense exercise, thyroid problems and some medicines also lengthen or disturb cycles
  ([NHS](https://www.nhs.uk/conditions/irregular-periods/)). A personal baseline beats any
  population average.
- **First periods.** Periods usually start around 12 and settle by 16 to 18
  ([NHS](https://www.nhs.uk/conditions/periods/)); early cycles are often irregular
  ([ACOG CO 651](https://www.acog.org/clinical/clinical-guidance/committee-opinion/articles/2015/12/menstruation-in-girls-and-adolescents-using-the-menstrual-cycle-as-a-vital-sign)).
  Not our user, but it explains why "normal" ranges differ by age.
- **Contraception.** On the combined pill, patch or ring there is no ovulation, and the bleed in the
  break is a withdrawal bleed, not a period
  ([FSRH CHC 2019](https://www.cosrh.org/Common/Uploaded%20files/documents/fsrh-guideline-combined-hormonal-contraception-october-2023.pdf)).
  The implant, the injection and the desogestrel pill stop ovulation too, and bring bleeding that
  does not follow a cycle: anything from none to frequent or prolonged. Most users of a hormonal
  IUD, and many on a traditional progestogen-only pill, still ovulate, but their bleeding changes
  too. The copper IUD leaves the cycle alone and makes periods heavier and longer, especially at
  first. Fertile window and ovulation estimates mean nothing on hormonal methods. Each method, what
  she logs on it, and how fast cycles come back after stopping (weeks for most, months for the
  injection) are in [`contraception.md`](contraception.md).
- **Pregnancy, after birth and breastfeeding.** Periods stop in pregnancy. After birth, cycles
  return at different times, often later when breastfeeding, and the first ones are irregular.
  Calendar-based methods should wait until three regular cycles have returned
  ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)). After a
  miscarriage or abortion, the next bleed starts a new count.
- **Perimenopause.** The transition to menopause starts, typically in the 40s, with a persistent
  difference of 7 days or more between consecutive cycles (early transition), and moves on to gaps of
  60 days or more without a period (late transition)
  ([STRAW+10](https://www.asrm.org/practice-guidance/practice-committee-documents/executive-summary-of-the-stages-of-reproductive-aging-workshopd10-addressing-the-unfinished-agenda-of-staging-reproductive-aging-2012/)).
  Hot flushes, night sweats, sleep problems and mood changes come with it
  ([WHO](https://www.who.int/news-room/fact-sheets/detail/menopause)).
- **Menopause.** Confirmed after 12 months without a period, usually between 45 and 55
  ([WHO](https://www.who.int/news-room/fact-sheets/detail/menopause)). Any bleeding after that needs
  a doctor ([NHS](https://www.nhs.uk/conditions/periods/)).

## Implications for Cycle

- **Day 1 is the first day of real flow.** Spotting before it does not start a cycle. The data model
  should derive cycles from logged period days, not store them separately, so a corrected log fixes
  every cycle after it.
- **Use her numbers, not 28/14.** A new install with no history can start from 28 days and a 5-day
  period, but every estimate should switch to her own logged cycles as soon as there are any, and say
  which it is using.
- **Bound inputs with the ranges above, without rejecting her data.** A 19-day or 50-day cycle is
  unusual, not impossible: accept it, keep it in her history, and let
  [`health-signals.md`](health-signals.md) decide whether to mention it. Gaps that look like a missed
  log (a cycle about twice her usual length) need a gentle "did you miss a period?" rather than a
  silent average.
- **Name phases carefully.** "Period" and "cycle day 19" are facts. "Fertile window" and "ovulation"
  are estimates and should look and read like estimates (see [`predictions.md`](predictions.md)).
  The legend already shows predicted periods with a dashed edge; estimated ovulation deserves the
  same honesty.
- **Life stages are modes.** Pregnancy, after birth, breastfeeding, contraception and perimenopause
  each change what the app can predict. The app needs a way to say "pause or change predictions"
  without her deleting data. For contraception, what changes depends on the method
  ([`contraception.md`](contraception.md)).
