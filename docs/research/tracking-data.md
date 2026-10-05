# What to track

The things she could log each day, why each one matters, the scale to log it on, and how to store
it. Grouped by how much value each adds for how much effort: the period is the whole point, the rest
earns its place only if she wants it.

## Tiers

| Tier | Items | Why |
| -- | -- | -- |
| Core | Period days and flow, spotting | Everything else (cycles, predictions, health signals) derives from them. |
| Common | Pain, mood, physical symptoms, energy, sleep, sex, notes | What most trackers offer and what most users look at: patterns across the cycle, and the record a doctor asks for. |
| Fertility signs | Cervical mucus, basal body temperature, ovulation (LH) tests, pregnancy tests | Only useful if she wants to know when she ovulates, and only if logged daily. |
| Context | Contraception, medication, life stage (pregnant, after birth, breastfeeding, perimenopause), cycles to exclude | Changes what the app can predict ([`predictions.md`](predictions.md)). Set rarely, not logged daily. |

Clue offers more than 30 categories ([Clue](https://en.wikipedia.org/wiki/Clue_(mobile_app))); drip
covers bleeding, fertility signs, sex, mood and pain
([drip](https://gitlab.com/bloodyhealth/drip)). Cycle should start from the core and add categories
she asks for, each one optional and hideable.

## Items and scales

| Item | Scale | Notes |
| -- | -- | -- |
| Period flow | Light, medium, heavy | Per day. The first day of light or heavier flow is cycle day 1. Matches Health Connect's `MenstruationFlowRecord` (light, medium, heavy). |
| Spotting | Yes/no, outside a period | Separate from flow: spotting before a period does not start a cycle, and spotting between periods is a health signal ([FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666)). Health Connect calls it `IntermenstrualBleedingRecord`. |
| Heavy-bleeding details | Products changed that day; soaked through within 1 to 2 hours; clots bigger than about 2.5 cm; leaked onto clothes or bedding; changed overnight | Optional, shown only on heavy days. These are the signs the NHS and CDC use to tell heavy bleeding apart ([NHS](https://www.nhs.uk/conditions/heavy-periods/), [CDC](https://www.cdc.gov/female-blood-disorders/about/heavy-menstrual-bleeding.html)), because nobody measures millilitres at home. |
| Pain | None, mild, moderate, severe, per kind: cramps, headache or migraine, lower back, breasts, ovulation pain, pain during sex | Severity matters more than presence: severe period pain, pain outside periods and pain during sex are health signals ([WHO](https://www.who.int/news-room/fact-sheets/detail/endometriosis)). |
| Mood | Any of: calm, happy, sensitive, sad, anxious, irritable, angry, low, mood swings | Multi-select, no score. Mood that recurs before each period and lifts after it is the PMS/PMDD pattern. |
| Physical symptoms | Any of: bloating, tender breasts, acne, cravings, nausea, constipation, diarrhoea, fatigue, hot flushes, night sweats | The last two matter for perimenopause ([WHO](https://www.who.int/news-room/fact-sheets/detail/menopause)). |
| Energy, sleep | Low, normal, high; slept well or badly | Cheap to log, useful in patterns. |
| Sex | Yes; protected or unprotected; optional libido (low, normal, high) | Health Connect's `SexualActivityRecord` stores protected or unprotected. Sensitive: never in notifications, never in a widget. |
| Cervical mucus | Dry, sticky, creamy, watery, egg white, unusual | Health Connect's `CervicalMucusRecord` uses these appearances, plus a sensation (light, medium, heavy). |
| Basal body temperature | °C or °F, two decimals; time taken; a "disturbed" flag (fever, alcohol, short sleep, different time) | Same time every morning before getting up ([WHO FP Handbook](https://fphandbook.org/sites/default/files/Chapter_18_Eng.pdf)). Disturbed readings are kept but left out of ovulation detection. Health Connect: `BasalBodyTemperatureRecord`. |
| Ovulation test | Negative, high, positive, inconclusive | Health Connect's `OvulationTestRecord` results. |
| Pregnancy test | Negative, positive | Only if she wants it; a positive one can offer the pregnancy mode. |
| Medication | Free text or a short list she builds | |
| Contraception | A setting, not a daily log: the method (combined pill, patch, ring, progestogen-only pill by kind, implant, hormonal IUD, copper IUD, injection), the date she started and stopped it, and for combined methods optionally the regimen and the current pack's start date | Changes the words for what she logs, which estimates appear, and which health signals apply; hormonal methods switch fertility estimates off ([`contraception.md`](contraception.md)). |
| Notes | Free text per day | Everything else. |

## For the premenstrual diary

Clinicians confirm PMS and PMDD from prospective daily ratings over at least two cycles, not from
memory ([AAFP 2011](https://www.aafp.org/pubs/afp/issues/2011/1015/p918.html)), typically with the
Daily Record of Severity of Problems: each symptom rated daily on a 1 (not at all) to 6 (extreme)
scale. If she wants a diary to take to a doctor, the mood and symptom lists above need a daily
severity, not just presence, and a two-cycle summary she can show.

## Storage notes

These follow [`0001-stack.md`](../decisions/0001-stack.md) (Room, `java.time`, on the device).

- **Days, not instants.** Every entry belongs to a `LocalDate`, the day she means, not a timestamp: a
  period logged at 23:30 in one time zone must not move to the next day after travel. Only BBT keeps
  a time of day, as a `LocalTime`.
- **Derive, don't store, cycles and periods.** Periods and cycles are computed from the day entries,
  so editing a past day re-computes everything after it. Cache them if needed, never as the source.
- **Predictions are never stored as data.** They are recomputed from the log, and never written into
  the same tables as what she logged.
- **One row per day per category**, each optional, so adding a category is a migration that adds a
  table or a column, not a rewrite.
- **Life stages and settings are date ranges** (on the pill from, to; pregnant from, to), so past
  cycles keep their meaning when a setting changes. Bleeding is stored the same way on every method:
  a bleeding day on the implant and a period day are the same row, and the method in force on that
  date decides how it is labelled and counted ([`contraception.md`](contraception.md)).
- **Line up with Health Connect.** Using the same categories and values as Health Connect's cycle
  tracking records ([Android](https://developer.android.com/health-and-fitness/guides/health-connect/plan/data-types))
  keeps a later import or export a mapping, not a redesign. Health Connect itself stays out until
  she asks for it.
- **Export and import from the start.** A plain CSV or JSON she can save, so her history survives a
  lost phone or a move to another app. drip and Clue both export.
- **Synthetic only in tests and screenshots**, as everywhere in the repo.

## Logging experience

- **Fast path first.** Logging a period start should take one tap from the home screen; ending it,
  one more. The calendar should let her fill in past days, because people forget and catch up later.
- **Every category optional and hideable.** An empty day is normal; the app should never nag about
  missing logs except for the period itself, and only if she turns that reminder on.
- **Edit anything, any time.** Past days stay editable; there is no "locked" history.
- **Discreet by default.** Notifications and any widget say only what she chose to show, with
  neutral wording, because phones are seen by other people.
