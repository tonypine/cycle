# Other apps, and privacy

How the best-known cycle trackers approach the problem, what goes wrong with them, and the privacy
record of the category. Cycle has one user and no business model, which frees it from most of the
reasons these apps go wrong.

## The landscape

| App | Approach | Worth copying | Worth avoiding |
| -- | -- | -- | -- |
| [Clue](https://helloclue.com/) | Berlin-based, science-led tracker with 30+ categories, GDPR-governed, EU servers. | Neutral, inclusive, non-pink tone ([Clue](https://helloclue.com/articles/cycle-a-z/talking-about-periods-beyond-gender)); categories you can turn off; its research partnerships produced some of the cycle data used here. | Data on servers and an account; Mozilla still gave it a privacy warning in 2022 ([Mozilla](https://www.theregister.com/2022/08/17/mozilla_pregnancy_app/)). |
| [Flo](https://flo.health/) | The largest tracker; predictions, symptom logging, content and a premium tier. | Breadth of logging; its anonymised data underpins large studies ([Grieger 2020](https://www.jmir.org/2020/6/e17109/)). | Shared users' health data with Facebook, Google and others after promising not to, which led to an FTC order in 2021 ([FTC](https://www.ftc.gov/news-events/news/press-releases/2021/06/ftc-finalizes-order-flo-health-fertility-tracking-app-shared-sensitive-health-data-facebook-google)) and, in August 2025, a jury finding Meta liable for collecting Flo users' reproductive data through its SDK ([CNBC](https://www.cnbc.com/2025/08/07/jury-rules-meta-violated-law-in-period-tracking-app-data-case.html)). |
| [Natural Cycles](https://www.naturalcycles.com/) | Temperature-based fertility app, FDA-cleared as contraception. | Shows how much rigor a real fertility claim needs: daily temperature, an algorithm validated in studies, regulatory clearance ([classification](https://www.prnewswire.com/news-releases/fda-releases-final-classification-for-natural-cycles-the-first-and-only-birth-control-app-in-the-us-300808657.html)). | Not a model for us: we make no fertility claims. |
| [Apple Cycle Tracking](https://support.apple.com/en-us/120356) | Built into the Health app; predictions, symptoms, optional wrist temperature. | Cycle deviation alerts over six months of logs; "factors" (pregnancy, lactation, contraception) that change predictions; perimenopause mode; a six-day fertile window labelled as an estimate. | iOS only. |
| [drip](https://gitlab.com/bloodyhealth/drip) | Open-source, non-commercial, Android first, data only on the phone; period-only or symptothermal mode. | The closest model to Cycle: local data, password protection, import and export, reminders, charts ([drip](https://bloodyhealth.gitlab.io/)). | Little; its scope (symptothermal charting) is wider than we need now. |
| [Euki](https://eukiapp.org/) | Privacy-first tracker that collects no personal data and stores everything on the device. | Mozilla's only "Best Of" among period trackers in 2022 ([Mozilla](https://www.theregister.com/2022/08/17/mozilla_pregnancy_app/)). Discreet design for people at risk. | Narrow feature set. |

## What users say

- **Predictions are the main reason to open the app, and wrong ones hurt.** Over half of surveyed
  users had seen a period arrive earlier than predicted and nearly three quarters later; wrong
  predictions caused anxiety, frustration and fear of pregnancy
  ([Broad 2022](https://journals.sagepub.com/doi/10.1177/17455057221095246)).
- **Fertility estimates are often wrong**, and apps rarely say so: most predicted ovulation days were
  2 to 9 days early in one test ([Worsfold 2021](https://journals.sagepub.com/doi/10.1177/17455065211049905)).
- **Privacy is a worry.** In a UK poll for the Information Commissioner's Office, 59% of women named
  transparency over data use and 57% security as concerns about these apps
  ([ICO coverage](https://www.femtechworld.co.uk/news/uk-regulator-to-review-period-and-fertility-tracking-apps/)).

## The privacy record

- **The leaks came from SDKs.** Flo embedded marketing and analytics SDKs (Facebook, Google,
  Fabric, AppsFlyer, Flurry) and named its analytics events after what users entered, such as
  `R_PREGNANCY_WEEK_CHOSEN`, so the event names alone carried health data to those companies for
  years ([FTC complaint](https://www.ftc.gov/system/files/documents/cases/flo_health_complaint.pdf)).
  Any third-party SDK is a channel off the phone, whatever the privacy policy says.
- **Most reproductive health apps fall short.** Mozilla reviewed 25 period, pregnancy and fertility
  apps and wearables in 2022 and gave 18 its warning label, including 8 of 10 period trackers. Most
  were vague about sharing data with law enforcement
  ([The Register](https://www.theregister.com/2022/08/17/mozilla_pregnancy_app/)).
- **Cycle data can be legal evidence.** After abortion restrictions in parts of the US in 2022,
  period data became a concern in law, not just in advertising. Data that never leaves the phone
  cannot be subpoenaed from a company.
- **Backups leave the phone too.** Android's backup to Google Drive is end-to-end encrypted with the
  device's PIN, pattern or password on Android 9 and later, but only when a screen lock is set. An
  app can require that encryption with `requireFlags="clientSideEncryption"` in its backup rules, and
  each app gets 25 MB ([Android](https://developer.android.com/identity/data/autobackup)). Cycle
  currently sets `allowBackup="true"` with no rules, so its database is backed up whether or not the
  backup is end-to-end encrypted.

## Implications for Cycle

- **Already decided, and right:** data stays on the device, no backend, no analytics, no crash
  reporting, no third-party SDK that sends data ([`0001-stack.md`](../decisions/0001-stack.md),
  `AGENTS.md`). This alone puts Cycle in the same group as drip and Euki.
- **Backup needs its own decision.** Requiring client-side encryption keeps unencrypted copies off
  Google's servers, at the cost of no backup at all on a phone without a screen lock. Export and
  import give her a second way to keep her history either way.
- **An app lock** (PIN or biometric) and a quick "delete everything" are cheap and expected in this
  category.
- **Discretion is a feature.** Neutral notification text, no sensitive data in widgets or the recent
  apps screenshot (`FLAG_SECURE` is an option for her to choose), a plain app name and icon if she
  wants one.
- **Honesty is the other half of trust.** The tone in
  [`visual-directions.md`](../design/visual-directions.md)
  (say "period", no euphemisms, nothing that tells her how to feel) matches what users praise in
  Clue. Predictions with ranges and estimates labelled as estimates avoid what users resent in the
  others.
