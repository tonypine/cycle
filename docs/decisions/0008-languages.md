# 0008: Languages: which ones, how Cycle applies them, and the copy rules in each

**Status:** accepted, 2026-10-06. Tony approved it with
[MOT-91](https://linear.app/tonypine/issue/MOT-91) before
[MOT-92](https://linear.app/tonypine/issue/MOT-92) (applying and storing the language),
[MOT-93](https://linear.app/tonypine/issue/MOT-93) (the translations) and
[MOT-94](https://linear.app/tonypine/issue/MOT-94) (the Language page). Her four questions below stay
open; the build follows each "Until she answers".

## Context

She asked for Cycle in English, Brazilian Portuguese, Spanish and German
([MOT-87](https://linear.app/tonypine/issue/MOT-87)). The stories, the new words in four languages
and the journeys are in [`docs/design/language.md`](../design/language.md), drawn in
[`journeys/language.html`](../design/journeys/language.html)
([MOT-91](https://linear.app/tonypine/issue/MOT-91)). This record decides what the build needs
underneath.

What there is today:

- Every string the app shows is in `res/values/strings.xml`, about 520 lines across `app`,
  `core:designsystem`, `core:ui` and the five features, plurals included. No screen hard-codes copy.
- Dates, month names and weekday letters are formatted with `LocalConfiguration.current.locales[0]`,
  read in six places: `Dates.kt` and `DaySummary.kt` in `core:ui`, `Calendar.kt` and `DayCell.kt` in
  `core:designsystem`, `TodaySheets.kt` in `feature:today` and `HistoryDates.kt` in
  `feature:history`. The first day of the week is `WeekFields.of` that locale, in `Calendar.kt` and
  in `TodaySheets.kt`'s `weeksOf`.
- Each module's `*StringsTest` checks its English strings against the words the app never says.
- `MainActivity` is a `FragmentActivity`, which the unlock prompt needs ([`0005`](0005-app-lock.md)).
  The minimum SDK is 29; Android's own per-app language setting starts at 13 (API 33).
- Settings live in one DataStore file, backed up only end-to-end encrypted
  ([`0004`](0004-backup-encryption.md)).
- The export writes fixed codes (`date,flow,…`, `medium`, `yes`), and import reads only those.

## Decision

### The four languages

| Language | Tag (the stored choice, `localeConfig`) | Resource folder | Its own name |
| -- | -- | -- | -- |
| English | `en` | `values` (the default) | English |
| Portuguese (Brazil) | `pt-BR` | `values-pt-rBR` | Português (Brasil) |
| Spanish | `es` | `values-es` | Español |
| German | `de` | `values-de` | Deutsch |

- **English stays in `values`**, so it is what a phone with none of the four gets. Android Lint's
  `MissingTranslation` stays an error, so no string ever falls back to English silently.
- **`values-pt-rBR`, not `values-b+pt+BR`.** Both mean the same to Android from API 21. The `r` form
  is the one Android Studio's Translations Editor, lint's messages and the AndroidX libraries use;
  the `b+` form is only needed for a script or a three-letter code. No `values-pt`: the strings are
  Brazilian, and Android already gives every Portuguese phone the closest Portuguese (below).
- **One Spanish, in `values-es`**, neutral enough for Spain and Latin America: "tú", never
  "vosotros", and words both sides use ("periodo", "teléfono"). **One German, in `values-de`.**

What a phone gets, with Cycle following it. Android 7 and later go down the phone's list of
languages, and take a language's closest region when the exact one is missing:

| Phone's languages | Cycle's words | Dates and numbers | First day of the week |
| -- | -- | -- | -- |
| Português (Portugal) | Brazilian Portuguese | Portugal's: "20 de março de 2027" | Sunday |
| Español (México) | Spanish | Mexico's | Sunday |
| Deutsch (Österreich) | German | Austria's ("Jänner" for January) | Monday |
| Deutsch (Schweiz) | German, with the ß that Swiss German writes as ss | Switzerland's | Monday |
| English (United States) | English | US: "March 20, 2027" | Sunday |
| Français, then Español | Spanish | Spanish | Monday (France) |
| Français only | English | English, for France's region where Android has it (`en-FR`) | Monday |

### Cycle's locale, and what follows the phone

| Follows Cycle's language | Follows the phone |
| -- | -- |
| Every word, plural and TalkBack description Cycle supplies. Dates, month and weekday names and weekday letters. Numbers. | The first day of the week. Android's own screens and dialogs (the unlock prompt's buttons, the file picker, the per-app language page), TalkBack's own words and its default voice. |

**Cycle's locale** formats every date and number. It is the language of the strings Android picked,
with a region:

- Following the phone: the phone's first locale in that language, region and all (`es-MX`, `de-AT`,
  `pt-PT`). With none of the four on the phone: English, with the region of the phone's first locale
  (`en-FR`).
- A chosen language: that language, with the region of the phone's first locale where the tag has
  none (Deutsch on a US phone is `de-US`, English on a German phone `en-DE`). Where Android has no
  data for that pair, the language's own formats apply. Português (Brasil) keeps Brazil.

One helper in `core:designsystem` gives it to every formatter. It cannot be
`Configuration.locales[0]`: on a phone with French then Spanish, Android shows the Spanish strings
while `locales[0]` stays French, and the dates would come out in French. Each `values` folder carries
its own tag in a string (`en`, `pt-BR`, `es`, `de`), so the resources themselves say which language
Android picked. MOT-92 replaces every `LocalConfiguration.current.locales[0]` with the helper, the
six above included, and a unit test greps the main sources of every module for `locales[0]` and
`locales.get(0)` and fails on any it finds outside the helper, so no formatter is left on the
phone's first locale.

**The first day of the week** comes from the phone: Android 14's "First day of week" regional
preference when she set one, otherwise the region of the phone's first locale, read with
`WeekFields.of` on the phone's locale, not Cycle's. Cycle's calendar then starts its weeks where the
phone's own calendar does (question 1).

### Applying and storing the choice

| | Android 13 and later (API 33+) | Android 10 to 12 (API 29–32) |
| -- | -- | -- |
| Applied by | Android: `LocaleManager.setApplicationLocales`, with `android:localeConfig` listing the languages Cycle has. Android stores it, applies it to Cycle's process before any activity starts, recreates the activity when it changes, and shows it on its own per-app page. | Cycle: `MainActivity.attachBaseContext` wraps its context in a configuration with the chosen locale (`createConfigurationContext`), and sets `Locale.setDefault`. A change calls `recreate()`. |
| Source of truth | Android's per-app setting, so a change made on Android's page wins, once Cycle has handed its stored choice to Android on this install (below). | Cycle's stored choice. |
| Cycle's own copy | The same value, written to the settings DataStore at every start and return to the front, so a backup restored on an older phone keeps it. | `SettingsRepository` in `core:data` stores it in the settings DataStore: no value for the phone's language, otherwise a tag. |
| "Phone's language" | An empty locale list, which Android's page shows as System default. | No stored tag: the context stays as Android gives it. |

- **No wrong language on a cold start.** On Android 13 and later, Android applies the language before
  Cycle draws anything. Below 13, `attachBaseContext` runs before the first frame and needs the
  choice at once, while DataStore reads asynchronously: the `Application` starts reading the
  settings file in `onCreate`, and `attachBaseContext` waits for that one read, of a file of a few
  hundred bytes, once per process. Later recreations use the value already in memory. A Robolectric
  test at SDK 29 checks that the first composition already uses the stored language.
- **A choice made below 13 reaches Android once.** A phone updated from Android 12 to 13, or a
  backup from an Android 10–12 phone restored onto a 13+ phone, starts with an empty per-app list
  and a tag in Cycle's DataStore. Copying Android's empty list over it would erase her choice. So
  the first start on 13 or later hands the stored choice to Android before mirroring anything
  back:
  - A marker file in `noBackupFilesDir`, written only on 13 and later, says Cycle has already handed
    its choice to Android on this install. A phone updated from 12 has none, and the file is outside
    every backup, so it never comes back with a restore.
  - With no marker, the `Application`'s `onCreate` waits for the same one settings read as below 13.
    If Android's list is empty and the DataStore holds a tag, it calls
    `LocaleManager.setApplicationLocales` with that tag; if Android's list is not empty (Android
    restored its own setting, or she chose on Android's page), Android's value wins and is written
    to the DataStore. Then it writes the marker. This runs before the first activity, so even this
    start shows no frame in the phone's language.
  - With the marker, Android's value is mirrored to the DataStore as in the table, and nothing
    waits.

  Robolectric tests at SDK 33 check each case: a stored `de` with an empty list and no marker ends
  with Android's list `de` and the DataStore still `de`; the same with the marker present ends with
  both empty (she chose System default on Android's page); a non-empty list with no marker is
  mirrored into the DataStore.
- **The switch keeps her place.** A language change recreates the activity like a rotation:
  ViewModels, the navigation back stack, the selected tab and saved scroll positions survive, as they
  must already. The Language page redraws with the new choice selected.
- **Backup.** The choice is in the settings DataStore file, which both of `0004`'s rule files back
  up only end-to-end encrypted, and which `BackupRulesTest` already covers. Android 13 and later also
  keep the per-app language in the system's own settings. Restored onto 13 or later from any phone,
  the DataStore's choice is handed to Android once, as above, when Android has not restored one of
  its own; restored onto 10 to 12, it applies as stored. It is never in the export.
- **Delete everything** forgets it with the other settings (on 13 and later, an empty locale list),
  so Cycle starts over following the phone.
- **Text comes from the activity.** Below 13, the `Application`'s own context keeps the phone's
  language. User-visible text is read in Compose (`stringResource`) or from the activity's context,
  never from `Application.getString`. Today every string already is.

### The languages packed into the app

- **`androidResources.localeFilters`** in the `cycle.android.application` convention lists the
  languages Cycle has, for `app` and `app-catalog`. Libraries bring their own strings in about 80
  languages (AndroidX Biometric's prompt, Activity, Compose's accessibility words), and Android
  resolves each string on its own. On a phone in French, with Cycle in English, any library string
  that has a French version would show in French in the middle of English screens, such as the
  fallback text of Lock Cycle's prompt. The filter drops every other language from the APK, so all
  of a screen comes from one language.
- **`android:localeConfig`** points to a hand-written `res/xml/locales_config.xml` in `app`.
- MOT-92 sets both to `en`; MOT-93 adds `pt-BR`, `es` and `de` in the PR that adds their strings. A
  unit test checks that `localeConfig`, `localeFilters` and the `values-*` folders of every module
  name the same languages, so a fifth language cannot be half added.
- `app-catalog` keeps its own few strings in English: it is a review tool. It gets the same filter
  and no `localeConfig`.

### What never changes with the language

- **The export file**: its column names, its values (Cycle's codes, such as `medium`,
  `cramps;lower_back`, `implant`), its `setting,value` keys, ISO dates, and the file name
  `cycle-export-<date>.csv`. Import reads only those, whatever language either phone is in. Import
  problems are translated sentences that quote the column names and values unchanged ("…in the flow
  column…").
- **Stored data**: the database holds codes and dates, never words.
- **Her notes**: shown as she typed them, never translated.
- **The app's name**, "Cycle", `translatable="false"`. Like a person's name it takes the language's
  grammar around it ("o Cycle" in Portuguese).

### Copy rules in every language

The rules in [`product-implications.md`](../research/product-implications.md#copy-rules) hold in each
language, worded for it.

- **Speak to her directly**: "você" in Brazilian Portuguese, "tú" in Spanish, "du" (lower case) in
  German, as the English does (question 3).
- **The same words for the same thing**, from this glossary. MOT-93 proposes the rest of the
  vocabulary in its PR, and a native speaker reviews it there.

  | English | Português (Brasil) | Español | Deutsch |
  | -- | -- | -- | -- |
  | period | menstruação | periodo | Periode |
  | bleed (the scheduled one, on a combined method) | sangramento da pausa | sangrado de la pausa | Abbruchblutung |
  | bleeding | sangramento | sangrado | Blutung |
  | spotting | escape | manchado | Schmierblutung |
  | cycle | ciclo | ciclo | Zyklus |
  | estimate, estimated | estimativa, estimada | estimación, estimado | Schätzung, geschätzt |
  | log (to) | registrar | registrar | eintragen |
  | note | anotação | nota | Notiz |
  | phone | celular | teléfono | Telefon |
  | Settings | Ajustes | Ajustes | Einstellungen |
  | Not a contraceptive, and not a diagnosis. | Não é um contraceptivo nem um diagnóstico. | No es un anticonceptivo ni un diagnóstico. | Kein Verhütungsmittel und keine Diagnose. |

  "Period", "bleed" and "bleeding" stay three different words in every language, because
  [`0006`](0006-contraception.md) depends on them.
- **Estimates read as estimates.** Never a bare date or a future tense ("vai começar", "empezará",
  "beginnt am") for anything she did not log:

  | English | Português (Brasil) | Español | Deutsch |
  | -- | -- | -- | -- |
  | Your next period is expected in about 10 days | Sua próxima menstruação está prevista para daqui a cerca de 10 dias | Tu próximo periodo se espera en unos 10 días | Deine nächste Periode wird in etwa 10 Tagen erwartet |
  | Around 30 March | Por volta de 30 de março | Alrededor del 30 de marzo | Um den 30. März |
  | Between 26 March and 3 April | Entre 26 de março e 3 de abril | Entre el 26 de marzo y el 3 de abril | Zwischen dem 26. März und dem 3. April |
  | Estimated from your last 6 cycles | Estimada a partir dos seus últimos 6 ciclos | Estimado a partir de tus últimos 6 ciclos | Geschätzt aus deinen letzten 6 Zyklen |
  | These are estimates, not promises. | São estimativas, não promessas. | Son estimaciones, no promesas. | Das sind Schätzungen, keine Versprechen. |

- **Each language's typography**: “…” in Portuguese and Spanish, „…“ in German; ¿…? and ¡…! in
  Spanish; German nouns capitalized. A formatted date that starts a title or a line starts with a
  capital in every language ("Março de 2027"). In German, "Datumsangaben" for dates, never "Daten",
  which also means data.
- **Grammar the code must not assemble**: placeholders are numbered (`%1$s`), so each language orders
  them as it needs; no sentence is built from fragments, and no date from pieces. Plurals have every
  quantity Android asks for (`one`, `many` and `other` in Portuguese and Spanish; `one` and `other` in
  German).
- **Length**: German runs about a third longer than English. Body text wraps, never ends in an
  ellipsis. Where a component cannot fit, it is fixed in the component, with its catalog entry and
  tests, never per screen; `NavigationBar`'s 11sp floor is the first such fix
  ([design](../design/language.md#components)).
- **"When to get help"** ([`0007`](0007-urgent-symptoms-on-a-method.md)). The English in `0007` is
  the source. Each translation follows it line by line: the same two actions (emergency help now,
  urgent medical advice today), each sign with the same meaning in everyday words, the intro and the
  closing line, with no condition names, no phone numbers and no country's own service named. It is
  checked twice before it merges: a back-translation into English beside each line in the PR,
  compared with `0007`, and a native speaker's review. The translations are added to `0007`, under
  the English, in the PR that adds them (MOT-93 if the section is on `main` by then, otherwise the PR
  that builds it), and a test pins each language's text as it pins the English. Changing a word in
  any language changes `0007` in the same PR.

### Copy checks in every language

Each language has its own list of words the app never says. The shared check (MOT-92) reads every
string and plural of a module in each language it has, and looks for that language's list, ignoring
case, as each `*StringsTest` does today for English.

| Language | Every module | History, also |
| -- | -- | -- |
| English | safe, fertile, you should feel, pregnan | normal, regular |
| Português (Brasil) | seguro, segura, fértil, fertil, férteis, grávid, gravidez, engravid, deveria se sentir, deve se sentir | normal, regular |
| Español | seguro, segura, fértil, fertil, embaraz, deberías sentir, debes sentir | normal, regular |
| Deutsch | sichere, fruchtbar, schwanger, solltest dich, fühlen solltest | normal, regelmäßig |

- Each list covers the English list's meaning: the "safe day" wording, fertility, pregnancy, telling
  her how to feel, and, in History, judging a cycle. Stems catch the other forms ("fertil" catches
  "fertilidade", "embaraz" catches "embarazo", "normal" catches "anormal" and "normalmente",
  "regelmäßig" catches "unregelmäßig"). A plural no stem catches is listed too: "férteis", as in
  "dias férteis" (added by MOT-92, whose check found the gap).
- German has "sichere", the form in "sichere Tage", rather than "sicher": "Sicherung" (backup) must
  stay usable. Portuguese and Spanish copy finds other words for "sure" ("Tem certeza?", "¿Quieres…?").
- Where a word is caught on purpose, the copy changes, not the list: "usually" is "costuma",
  "suele" and "meist", never a form of "normal".
- A list changes only with this record.

### Review of translations

- **A draft is marked.** A translation not written by a native speaker (MOT-93's are drafted by an
  agent) starts its `strings.xml` with `<!-- Translation: draft, awaiting a native speaker's review.
  -->`. Once reviewed, the line becomes `<!-- Translation: reviewed by a native speaker on <date>.
  -->`. No reviewer's name goes in the repository, which is public.
- **Reviewed before it ships.** Every merge to `main` is a release to her
  ([`0002`](0002-release-distribution.md)), so the review happens on the PR. Its description lists
  the strings to read first: the disclaimer, estimates, a late or missed period, import problems,
  "When to get help", the contraception words. Tony merges a language once its reader has passed it;
  a language without a reader yet waits in a PR of its own.
- **Who reads** is question 2.

### Adding a string later

- Every new user-visible string goes into `values` and every `values-xx` in the same PR, and into
  each language's copy check. Lint's `MissingTranslation` fails the build otherwise.
- The PR lists its new translations for a native speaker, as above; a draft stays marked as one.
- Text that is not language (the app's name, the languages' own names) is `translatable="false"`.
- MOT-92 adds this rule to `AGENTS.md` and `docs/design/design-system.md`.

## Questions only she can answer

1. **Which day do her weeks start on?** With Cycle in German on a phone set to US English, the phone
   starts weeks on Sunday and German calendars on Monday. Until she answers: the phone's day, so
   Cycle's calendar matches the phone's own, and Android 14's "First day of week" setting changes
   both.
2. **Who reads each translation?** Which of the three languages does she read as a native, and who
   could read the others? Until she answers: MOT-93 drafts all three and marks them as drafts, and
   each language merges only after a native speaker Tony asks has read it on the PR.
3. **Which Spanish, and how familiar?** Until she answers: one neutral Spanish with "tú",
   Brazilian Portuguese with "você", German with "du". Spain's Spanish ("vosotros", "móvil", "regla")
   or "Sie" in German would change only the strings.
4. **A language step at the welcome?** Until she answers: none. The welcome follows the phone, and
   Settings › Language is findable by its globe and the languages' own names.

## Options considered

**Applying the language**

- **Android's `LocaleManager` on 13 and later, Cycle's own stored choice below (chosen).** No new
  dependency, `MainActivity` stays as it is, and the choice and Android's page are one setting on
  13 and later. Below 13 it costs Cycle about a screen of code: wrap the context, recreate, store.
- **AndroidX AppCompat's `AppCompatDelegate.setApplicationLocales`.** One call on every API level,
  and AppCompat stores the choice below 13 itself. But below 13 it applies only to an
  `AppCompatActivity`, so `MainActivity` would have to become one (it still is a `FragmentActivity`,
  so the unlock prompt keeps working) with a `Theme.AppCompat` parent theme, which
  `AppCompatActivity` requires. It is a new dependency with its own resources and widgets, beside a
  UI built on Compose Foundation. Its store is a file of its own, outside the paths `0004`'s rules
  back up, so a restored phone would lose the choice unless both rule files and `BackupRulesTest`
  change; and it needs a service entry in the manifest. Most of what it saves is the few lines
  below 13.
- **Only Android 13 and later.** No code below 13, but the minimum SDK is 29, so a phone on Android
  10 to 12 could never choose, and on 13 and later the choice would live only in Android's settings.
- **A translated context inside Compose only** (`LocalContext` and `LocalConfiguration` replaced
  under `setContent`), switching without recreating. Text read outside Compose (the unlock prompt's
  title) would keep the old language, Android 13's per-app setting recreates the activity anyway, and
  every `LocalConfiguration` reader would need care.
- **Cycle's own setting with no `localeConfig`.** On 13 and later Android would not list Cycle on its
  per-app page, and the phone's setting and Cycle's could disagree.

**Storing it below 13**

- **The settings DataStore (chosen).** Already backed up as `0004` requires, and where the other
  settings live. The cost is the one waited-for read at a cold start.
- **SharedPreferences.** Reads at once, but `shared_prefs/` is outside both backup rule files; adding
  it means changing both rules and `BackupRulesTest`, and a second settings store.

**Resources and formats**

- **`values-b+pt+BR`.** Same meaning; less familiar to the tools.
- **Brazilian Portuguese in `values-pt`.** The same result on a phone, but mislabelled, and
  `localeConfig` would list plain Portuguese on Android's page.
- **`values-b+es+419` (Latin American Spanish).** Android would still give it to a phone in Spain, so
  one neutral Spanish under `es` says the truth.
- **No resource filter (today).** Library strings in other languages would mix into Cycle's screens.
- **AGP's `generateLocaleConfig`.** Writes `localeConfig` from the resource folders, but needs a
  `resources.properties` and hides the list in the build; a written file and a test catch the same
  mistakes and are easier to read.
- **The first day of the week from Cycle's language.** A German calendar would start on Monday on a
  US phone, unlike the phone's own calendar beside it. Her call (question 1).
- **Dates from `Configuration.locales[0]` (today).** Wrong whenever Android picks a language further
  down the phone's list, as above.

**Translations**

- **Translating in the app** (a translation service or an on-phone model). Needs the network or a
  large model, and nobody reviews what she reads. Ruled out.
- **One ticket per language.** Smaller reviews, three times the plumbing; MOT-93 may split a language
  out if its review waits.

## Consequences

- **MOT-92**: the language in `SettingsRepository`, applied on both API ranges as above, with the
  one-time hand-over to Android on 13 and later (a phone updated from 12 to 13, a backup from 10–12
  restored on 13+) and its tests;
  `localeFilters` and `localeConfig` for `en`, with the test that keeps them in step with the
  folders; Cycle's locale in `core:designsystem` for every date and number, replacing all six readers of
  `locales[0]`, with the grep test that keeps any from coming back; the first day of the week from
  the phone; a Robolectric helper to render under a language; the shared copy check with
  the lists above (English behaves exactly as today); the "same PR" rule in `AGENTS.md` and
  `design-system.md`. No screen changes.
- **MOT-93**: every string in the three languages with the glossary and the rules above, marked as
  drafts until reviewed; the three tags in `localeConfig` and `localeFilters`; capitalized dates at
  the start of a line; `NavigationBar`'s 11sp floor; German screenshots at 1× and 200%.
- **MOT-94**: the App section, the Language row and page, `RadioRow` with the two changes in the
  design, and `CycleIcons.Language`.
- **Adding a fifth language** means its tag in `localeConfig` and `localeFilters`, its folders, its
  copy-check list and glossary column here, and a reader. The test of the three lists catches a miss.
- If her phone is on Android 13 or later, she will mostly meet the choice there too; the code below
  13 stays small and covers a phone change.
