# Health signals

Patterns in a cycle log that clinicians would want to hear about, the conditions often behind them,
and how Cycle may point them out without diagnosing anything. ACOG calls the menstrual cycle a vital
sign ([ACOG CO 651](https://www.acog.org/clinical/clinical-guidance/committee-opinion/articles/2015/12/menstruation-in-girls-and-adolescents-using-the-menstrual-cycle-as-a-vital-sign)):
a good log is something she can bring to an appointment, and that is the app's job here.

## Signals the log can show

Thresholds are the ones clinical sources use. Where two sources differ, both are given; the app
should use the more cautious one only to suggest, never to label.

| Signal | Threshold | Source |
| -- | -- | -- |
| Cycles often short | Shorter than 21 days (NHS); shorter than 24 is "frequent" (FIGO). | [NHS](https://www.nhs.uk/conditions/irregular-periods/), [FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666) |
| Cycles often long | Longer than 35 days (NHS); longer than 38 is "infrequent" (FIGO). | [NHS](https://www.nhs.uk/conditions/irregular-periods/), [FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666) |
| Irregular cycles | Shortest and longest cycle in a year more than 7 to 9 days apart, depending on age (more than 9 at 18–25, 7 at 26–41, 9 at 42–45). | [FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666) |
| No period for a long time | 90 days without one, when not pregnant (ACOG, adolescents); the NHS says to see a GP after three missed periods. | [ACOG CO 651](https://www.acog.org/clinical/clinical-guidance/committee-opinion/articles/2015/12/menstruation-in-girls-and-adolescents-using-the-menstrual-cycle-as-a-vital-sign), [NHS](https://www.nhs.uk/conditions/periods/) |
| Long periods | More than 7 days (NHS, CDC); more than 8 is "prolonged" (FIGO). With a copper IUD, more than 8 days from three months after insertion (WHO; see [`contraception.md`](contraception.md)). | [NHS](https://www.nhs.uk/conditions/heavy-periods/), [CDC](https://www.cdc.gov/female-blood-disorders/about/heavy-menstrual-bleeding.html), [FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666), [WHO FP Handbook, copper IUD](https://fphandbook.org/managing-any-problems) |
| Heavy bleeding | Changing a pad or tampon every 1 to 2 hours; needing two products at once; clots larger than about 2.5 cm; bleeding through to clothes or bedding; changing overnight; missing activities or work; feeling tired or short of breath a lot. NICE defines heavy bleeding by its effect on her life, not by volume. | [NHS](https://www.nhs.uk/conditions/heavy-periods/), [CDC](https://www.cdc.gov/female-blood-disorders/about/heavy-menstrual-bleeding.html), [NICE NG88](https://www.nice.org.uk/guidance/ng88) |
| Bleeding between periods or after sex | Any. FIGO counts intermenstrual bleeding as always abnormal. | [NHS](https://www.nhs.uk/conditions/periods/), [FIGO 2018](https://obgyn.onlinelibrary.wiley.com/doi/10.1002/ijgo.12666) |
| Severe pain | Severe period pain, pain that stops daily life, pelvic pain outside periods, pain during sex or when peeing. | [NHS](https://www.nhs.uk/conditions/heavy-periods/), [WHO](https://www.who.int/news-room/fact-sheets/detail/endometriosis) |
| Premenstrual mood that disrupts life | Mood symptoms that come before each period, ease after it starts, and get in the way of work or relationships, across two or more cycles. | [AAFP 2011](https://www.aafp.org/pubs/afp/issues/2011/1015/p918.html) |
| Signs of the menopause transition | In her 40s: a persistent difference of 7 days or more between consecutive cycles, then gaps of 60 days or more. Not a problem, but worth knowing. | [STRAW+10](https://www.asrm.org/practice-guidance/practice-committee-documents/executive-summary-of-the-stages-of-reproductive-aging-workshopd10-addressing-the-unfinished-agenda-of-staging-reproductive-aging-2012/) |
| Bleeding after menopause | Any bleeding after 12 months without a period. | [NHS](https://www.nhs.uk/conditions/periods/), [WHO](https://www.who.int/news-room/fact-sheets/detail/menopause) |

## Conditions behind these patterns

Background for writing copy and choosing what to track, not for the app to name as a cause.

| Condition | How common | Cycle-related signs | Source |
| -- | -- | -- | -- |
| Polycystic ovary syndrome (PCOS) | 10 to 13% of women; up to 70% undiagnosed | Infrequent, unpredictable or absent periods (sometimes heavy, long or painful ones); excess hair, acne. | [WHO](https://www.who.int/news-room/fact-sheets/detail/polycystic-ovary-syndrome) |
| Endometriosis | About 10% of women of reproductive age | Severe period pain, heavy bleeding, pelvic pain between periods, pain during sex, bowel or bladder pain, trouble conceiving. Diagnosis takes 4 to 12 years on average. | [WHO](https://www.who.int/news-room/fact-sheets/detail/endometriosis) |
| Fibroids, adenomyosis, polyps | Common; often found because of heavy bleeding | Heavy or long periods, bleeding between periods. | [NHS](https://www.nhs.uk/conditions/heavy-periods/) |
| Bleeding disorders (such as von Willebrand disease) | Uncommon, often missed | Heavy periods since the first ones. | [NHS](https://www.nhs.uk/conditions/heavy-periods/), [CDC](https://www.cdc.gov/female-blood-disorders/about/heavy-menstrual-bleeding.html) |
| Thyroid problems, weight change, stress, intense exercise | Common | Irregular or missed periods. | [NHS](https://www.nhs.uk/conditions/irregular-periods/) |
| PMS and PMDD | PMS 20 to 32%; PMDD 3 to 8% | Mood and physical symptoms in the days before the period that ease once it starts. Confirmed from daily ratings over two cycles. | [AAFP 2011](https://www.aafp.org/pubs/afp/issues/2011/1015/p918.html) |
| Perimenopause | Everyone who reaches it, usually in the 40s | Changing cycle lengths and flow, hot flushes, night sweats, sleep and mood changes. | [WHO](https://www.who.int/news-room/fact-sheets/detail/menopause) |

## How the app should say it

Apple's Cycle Tracking is the reference: it looks at the last six months of logs and tells the user
when they show a pattern of irregular cycles, infrequent periods, prolonged periods or persistent
spotting, and suggests perimenopause can be a cause from 40 on
([Apple](https://support.apple.com/en-us/120356)).

- **Describe what she logged, then what people usually do.** "Your last three periods lasted 9, 10
  and 9 days. Periods longer than 7 days are worth mentioning to a doctor." Not "You may have
  menorrhagia" and never a condition's name as a cause.
- **Patterns, not single events.** One long cycle is noise. Flag a pattern over several cycles (the
  last six, as Apple does), except for bleeding between periods, after sex or after menopause, which
  are worth a mention each time.
- **Calm and once.** One dismissible card, not a notification and not a red banner. Period red and
  error red are already close in the palette ([`design-system.md`](../design/design-system.md)), so
  these cards need their own calm treatment. Do not repeat a dismissed signal until the pattern
  changes.
- **Her choice.** A setting turns these cards off entirely.
- **No emergencies.** The app is not a triage tool and should not pretend to be one: no card or
  alert ever says a symptom she logged is urgent. Wording about urgent symptoms comes from a clinical
  source and is reviewed; the only wording so far is the fixed "When to get help" text on some
  contraceptive methods ([`0006`](../decisions/0006-urgent-symptoms-on-a-method.md)).
- **The useful output is a summary.** A screen or file she can show a doctor: cycle lengths, period
  lengths, flow, pain and the symptoms she chose to share, over a chosen range. That shortens the
  "how long has this been going on?" part of an appointment, and for PMDD it is the two-cycle diary
  clinicians ask for.

## Implications for Cycle

- Every signal above can be computed from the [core and common items](tracking-data.md), on the
  device, with no model and no network.
- The thresholds belong in one place in code, each with its source in a comment, so a reviewer can
  check them against this document.
- Heavy bleeding can only be detected if the heavy-bleeding details are loggable. Pain only if
  severity is.
- A doctor summary (screen and export) is a stronger feature than any alert, and less risky.
- None of this ships without her saying she wants it: it is a candidate, not a default (see
  [`product-implications.md`](product-implications.md)).
