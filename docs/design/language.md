# Language: stories, journeys and copy

Cycle will speak English, Brazilian Portuguese, Spanish and German
([MOT-87](https://linear.app/tonypine/issue/MOT-87)). This page is the design of how she sees and
changes the language: the user stories, every new string in the four languages, then each journey
screen by screen with its copy and where it lives, then the components and the copy checks. The
screens are drawn in [`journeys/language.html`](journeys/language.html) (open it in a browser; one
row of phones per journey). The rules behind them are
[`0008-languages.md`](../decisions/0008-languages.md): which languages, how the choice is applied and
stored, what follows the phone, and the copy rules in each language.

Every date, note and number here is synthetic. Copy of existing screens shown in another language
(Today, setup, the calendar) is a draft for the drawings: the translations themselves are
[MOT-93](https://linear.app/tonypine/issue/MOT-93)'s, reviewed as 0008 says.

## Where it lives

| Place | What changes |
| -- | -- |
| Settings | A new section, **App**, between Your data and About, with one row: **Language**. Its icon is a globe; its second line says what Cycle follows and the language it shows ("Phone's language, English", or "Deutsch"). |
| Settings › App › Language | A new page: a top bar with Back, one intro line, then five options in one group: Phone's language, then the four languages under their own names. Choosing one applies it at once. |
| Android Settings › Apps › Cycle › Language (Android 13 and later) | Android's own page, also reached from System › Languages › App languages. It lists System default and Cycle's four languages, from the list Cycle declares. It is the same choice as Cycle's page: a change in one shows in the other. Not drawn by Cycle. |
| Every screen | Only their words, dates, month and weekday names change: welcome and setup, Today and its sheets, the calendar, History, the day log, Settings and its pages, dialogs, import problems, the lock screen and the text Cycle gives the phone's unlock prompt. Layouts, colours and what each screen shows stay. |
| Nowhere | Setup gets no language step: the welcome already follows the phone, and the Language page is two taps from any tab. The export file, what she logged, her notes and the app's name ("Cycle") never change with the language. |

## User stories

Refined from the plan on MOT-87. Stories 7 and 11 are new or reworded; the rest keep its meaning.

1. **Cycle speaks my phone's language.** If my phone is set to English, Portuguese (Brazil), Spanish
   or German, Cycle opens in that language with nothing to set up, from the welcome screen on.
2. **Other languages fall back.** Cycle uses the first language in my phone's list that it has. If my
   phone says French and then Spanish, Cycle opens in Spanish. If my phone has none of the four,
   Cycle opens in English, and Settings › Language says why ("French isn't in Cycle yet, so this
   option uses English.").
3. **I can choose Cycle's language myself.** Settings › Language lists every language under its own
   name (Deutsch, English, Español, Português (Brasil)), so I can find mine even when I can't read the
   screen, and a "Phone's language" option. Choosing one switches Cycle at once: I stay on the same
   page, my tab and the screens behind it stay, and I don't redo setup or lose anything.
4. **One choice, two places.** On Android 13 and later, Android's Settings › Apps › Cycle › Language
   shows the same choice. Whichever place I change it, the other one shows the change.
5. **Back to my phone's language.** With "Phone's language", Cycle follows the phone again,
   including when I change the phone's language later.
6. **Dates read in my language.** Month and weekday names, and lines like "Last exported 2 March
   2027", use the language Cycle is in. The week starts on the day my phone starts it.
7. **My words stay mine.** Switching languages never changes what I logged. What I tick (flow, pain,
   mood) shows in Cycle's new language, because those are Cycle's words; my notes stay in the words I
   wrote them in.
8. **Exports work in any language.** A file exported while Cycle was in German imports into Cycle in
   English, and the other way round. The file doesn't depend on the language.
9. **The same care in every language.** The copy rules hold in every language: estimates read as
   estimates, Cycle never names a condition or calls a day free of risk, and Settings always says it
   is not a contraceptive and not a diagnosis. Each language has its own list of words the app never
   says, and tests check it ([Copy checks](#copy-checks)).
10. **Private.** The choice stays on the phone and goes into Android's encrypted backup with the
    rest of the settings. It is not part of the export.
11. **Readable at any size, and with TalkBack.** German at 200% font size wraps and never cuts a word
    off. TalkBack reads each language's name in that language's own voice, and reads the options as
    one group of five.

| Story | Steps |
| -- | -- |
| 1 | A1, A2, A3, A4 |
| 2 | B1, B2 |
| 3 | B2, B3, B4, C1, C2, C3 |
| 4 | E1, E2, E3 |
| 5 | D1, D2, D3, D4 |
| 6 | A2, A4, B5, C4, F1 |
| 7 | C5, F4 |
| 8 | F1, F2, F3 |
| 9 | A4, B5, C4 |
| 10 | F2 |
| 11 | B2, C6 |

## The words

Every new string, in the four languages. The names in the first column are proposals for the build
([MOT-94](https://linear.app/tonypine/issue/MOT-94)), all in `feature:settings`. Placeholders keep
their numbers in every language.

| String | English | Português (Brasil) | Español | Deutsch |
| -- | -- | -- | -- | -- |
| `settings_app`: the section | App | App | Aplicación | App |
| `language_title`: the row and the page | Language | Idioma | Idioma | Sprache |
| `settings_language_phone`: the row, following the phone | Phone's language, %1$s | Idioma do celular, %1$s | Idioma del teléfono, %1$s | Sprache des Telefons, %1$s |
| `language_intro`: the page's intro | Cycle's words and dates change. Everything you logged stays, and your notes keep your own words. | As palavras e as datas do Cycle mudam. Tudo o que você registrou continua aqui, e suas anotações ficam com as suas palavras. | Cambian las palabras y las fechas de Cycle. Tus registros se quedan como están, y tus notas conservan tus propias palabras. | Texte und Datumsangaben in Cycle ändern sich. Alles, was du eingetragen hast, bleibt, und deine Notizen behalten deine eigenen Worte. |
| `language_phone`: the first option | Phone's language | Idioma do celular | Idioma del teléfono | Sprache des Telefons |
| `language_phone_not_in_cycle`: its second line, when the phone's first language isn't one of the four | %1$s isn't in Cycle yet, so this option uses %2$s. | %1$s ainda não está no Cycle, então esta opção usa %2$s. | %1$s aún no está en Cycle, así que esta opción usa %2$s. | %1$s gibt es in Cycle noch nicht, deshalb nutzt diese Option %2$s. |
| `language_name_en` | English | Inglês | Inglés | Englisch |
| `language_name_pt_br` | Portuguese (Brazil) | Português (Brasil) | Portugués (Brasil) | Portugiesisch (Brasilien) |
| `language_name_es` | Spanish | Espanhol | Español | Spanisch |
| `language_name_de` | German | Alemão | Alemán | Deutsch |

The four titles are `translatable="false"`, in `values/` only: English, Português (Brasil),
Español, Deutsch.

### The Settings row's second line

| Cycle | in English | in Português (Brasil) | in Español | in Deutsch |
| -- | -- | -- | -- | -- |
| Follows the phone | Phone's language, English | Idioma do celular, Português (Brasil) | Idioma del teléfono, Español | Sprache des Telefons, Deutsch |
| A language she chose | English | Português (Brasil) | Español | Deutsch |

The language named is always the one Cycle is in, so its name in Cycle's language is its own name.
A phone whose first language isn't one of the four reads the same: on a phone in French, "Phone's
language, English"; the Language page says why. The row is one button; TalkBack reads its title,
then this line.

### The "isn't in Cycle yet" line

The second line of Phone's language when the phone's first language is none of the four. It says
what that option does, not what Cycle is doing, so it stays true whichever option is selected: with
Español chosen, the line still names the language Phone's language would give. Both names come from
Android (`Locale.getDisplayLanguage` of the phone's first language, and the display name of the
language this option uses), in Cycle's current language, so the whole line reads in one language
and one TalkBack voice. The first name is capitalized as the start of a sentence; the second is
written as the language writes a name mid-sentence (lower case in Portuguese and Spanish).

| Phone's languages | Cycle in | Line |
| -- | -- | -- |
| Français | English (following the phone) | French isn't in Cycle yet, so this option uses English. |
| Français | Español (chosen) | Francés aún no está en Cycle, así que esta opción usa inglés. |
| Français, Español | Deutsch (chosen) | Französisch gibt es in Cycle noch nicht, deshalb nutzt diese Option Spanisch. |
| Français, Español | Português (Brasil) (chosen) | Francês ainda não está no Cycle, então esta opção usa espanhol. |

When the phone's first language is one of the four (any region: Português (Portugal) counts as
Portuguese), the second line is that language's name from the table above, such as "English" or
"Englisch".

### TalkBack

Cycle supplies the words; TalkBack adds the role, the state and the position in its own language,
which is the phone's. Each option's title is marked with its own language, so TalkBack reads
"Deutsch" in a German voice on an English phone. Shown with TalkBack in the same language as Cycle:

| | English | Português (Brasil) | Español | Deutsch |
| -- | -- | -- | -- | -- |
| Settings row | Language, Phone's language, English, button | Idioma, Idioma do celular, Português (Brasil), botão | Idioma, Idioma del teléfono, Español, botón | Sprache, Sprache des Telefons, Deutsch, Schaltfläche |
| Page title | Language, heading | Idioma, título | Idioma, encabezado | Sprache, Überschrift |
| First option | Phone's language, English, radio button, selected, 1 of 5 | Idioma do celular, Português (Brasil), botão de opção, selecionado, 1 de 5 | Idioma del teléfono, Español, botón de opción, seleccionado, 1 de 5 | Sprache des Telefons, Deutsch, Optionsfeld, ausgewählt, 1 von 5 |
| Another option | Deutsch, German, radio button, not selected, 2 of 5 | Deutsch, Alemão, botão de opção, não selecionado, 2 de 5 | Deutsch, Alemán, botón de opción, no seleccionado, 2 de 5 | English, Englisch, Optionsfeld, nicht ausgewählt, 3 von 5 |

The globe icon is decorative and not read. Back is the existing `settings_back`.

## The language list

Phone's language first, then the four languages in alphabetical order of their own names, the order
Android's own language lists use. The order is the same whichever language Cycle is in, so a row
never moves when she switches.

| Position | Title (never translated) | Second line in English | Português (Brasil) | Español | Deutsch |
| -- | -- | -- | -- | -- | -- |
| 1 | Phone's language (translated: `language_phone`) | The language the phone gives, or the "isn't in Cycle yet" line | the same | the same | the same |
| 2 | Deutsch | German | Alemão | Alemán | none |
| 3 | English | none | Inglês | Inglés | Englisch |
| 4 | Español | Spanish | Espanhol | none | Spanisch |
| 5 | Português (Brasil) | Portuguese (Brazil) | none | Portugués (Brasil) | Portugiesisch (Brasilien) |

"None": the option for the language Cycle is in has no second line, which would repeat its title.
Nothing is chosen for her: with no choice stored, Phone's language is selected.

## Journeys

Each step names its screen, where it lives, the copy on it, and the components. The letters and
numbers match the phones in [`journeys/language.html`](journeys/language.html).

### A. First open on a phone in Portuguese (Brazil)

Her phone's languages: Português (Brasil). Today is Saturday 20 March 2027; her last period started
on 2 March.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| A1 | Welcome | Setup | "Oi! Vamos acompanhar seu ciclo", "Registre sua menstruação e o Cycle estima a próxima. Tudo fica neste celular: sem conta, sem anúncios, sem rastreamento." Começar · Restaurar de uma exportação do Cycle · Pular por enquanto. No language step before or after it. |
| A2 | Last period | Setup, step 1 of 2 | "Passo 1 de 2". "Quando começou sua última menstruação?" "Pode ser aproximado." Calendar "Março de 2027", weekday letters D S T Q Q S S (Brazil starts the week on Sunday), 2 March chosen. Não lembro · **Próximo** |
| A3 | Usual lengths | Setup, step 2 of 2 | "Passo 2 de 2". "Quanto tempo elas costumam durar?" "O Cycle faz estimativas com estes números até você registrar menstruações suas." "Duração do ciclo" "28 dias", "Duração da menstruação" "5 dias", each with its hint. **Concluir** |
| A4 | Today | Today | "SÁBADO, 20 DE MARÇO", display "Dia 19", "Sua próxima menstruação está prevista para daqui a cerca de 10 dias". Week from Sunday 14 to Saturday 20. **Minha menstruação começou** · Registrar como você se sente. Card "Próxima menstruação": "Por volta de 30 de março", "Entre 26 de março e 3 de abril", "Estimada a partir das durações que você informou". Como isso é estimado? Tabs: Hoje, Calendário, Histórico, Ajustes. |

### B. A phone in a language Cycle doesn't have

Her phone's languages: Français (France). Cycle is in English. Today is Monday 10 May 2027; her last
period was 3 to 7 May.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| B1 | Settings | Settings, scrolled to its end | …Export my data · Import from a file · Delete everything; **App**: Language · "Phone's language, English" ›; **About**: Open-source notices ›; "Not a contraceptive, and not a diagnosis."; "Version 0.1.80". |
| B2 | Language | Settings › App › Language | Top bar "Language", Back. "Cycle's words and dates change. Everything you logged stays, and your notes keep your own words." Phone's language (selected) · "French isn't in Cycle yet, so this option uses English."; Deutsch · German; English; Español · Spanish; Português (Brasil) · Portuguese (Brazil). |
| B3 | Español chosen | Same page, redrawn in Spanish | She taps Español. At once: top bar "Idioma", "Cambian las palabras y las fechas de Cycle. Tus registros se quedan como están, y tus notas conservan tus propias palabras." Idioma del teléfono · "Francés aún no está en Cycle, así que esta opción usa inglés."; Deutsch · Alemán; English · Inglés; Español (selected); Português (Brasil) · Portugués (Brasil). |
| B4 | Settings | Settings, in Spanish | Back. Top bar "Ajustes", scrolled as she left it: Exportar mis datos · Importar desde un archivo · Borrar todo; **Aplicación**: Idioma · "Español" ›; **Acerca de**; "No es un anticonceptivo ni un diagnóstico." Tabs: Hoy, Calendario, Historial, Ajustes. |
| B5 | Calendar | Calendar, in Spanish | "Mayo de 2027", weekday letters L M X J V S D (the week starts on Monday, as in France), her period on 3 to 7 May, today ringed, the predicted period from 31 May. Legend: Periodo, Periodo previsto, Hoy. "Los periodos previstos son estimaciones. Toca cualquier día hasta hoy para registrarlo o cambiarlo." |

### C. Switching to German, at its longest

Her phone's languages: English (United States). Today is Thursday 14 October 2027; her last period
started on 1 October, and she has logged six cycles of 28 days.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| C1 | Language | Settings › App › Language | As B2, with Phone's language · "English" (selected). She taps Deutsch. |
| C2 | Deutsch chosen | Same page, redrawn in German | Top bar "Sprache". "Texte und Datumsangaben in Cycle ändern sich. Alles, was du eingetragen hast, bleibt, und deine Notizen behalten deine eigenen Worte." Sprache des Telefons · Englisch; Deutsch (selected); English · Englisch; Español · Spanisch; Português (Brasil) · Portugiesisch (Brasilien). |
| C3 | Settings | Settings, in German | Top bar "Einstellungen", scrolled to its end: Meine Daten exportieren · Aus einer Datei importieren · Alles löschen; **App**: Sprache · "Deutsch" ›; **Info**: Open-Source-Hinweise ›; "Kein Verhütungsmittel und keine Diagnose." Tabs: Heute, Kalender, Verlauf, Einstellungen, each at 11sp ([components](#components)). |
| C4 | Today | Today, in German | "DONNERSTAG, 14. OKTOBER", display "Tag 14", "Deine nächste Periode wird in etwa 15 Tagen erwartet". The week from Sunday 10 to Saturday 16, letters S M D M D F S: the phone's region starts it on Sunday. **Meine Periode hat begonnen** · Eintragen, wie du dich fühlst. Card "Nächste Periode": "Um den 29. Oktober", "Zwischen dem 26. Oktober und dem 1. November", "Geschätzt aus deinen letzten 6 Zyklen". Wie wird das geschätzt? |
| C5 | Her note stays hers | Today › day log for 13 October | Sheet "Mittwoch, 13. Oktober": Blutung · Keine (chosen), Schmierblutung, Leicht, Mittel, Stark; Notizen · "Deine Notiz": "Long walk, then an early night." (her words, in English, as she wrote them), "Nur auf diesem Telefon." Jetzt nicht · **Eintragen** |
| C6 | At 200% font size | Settings › App › Language | C2 at 200%: the intro fills most of the first screen; "Sprache des Telefons" wraps in its row; the rows grow, nothing is cut, and the page scrolls to the rest of the list. |

### D. Back to the phone's language

Her phone's languages: English (United Kingdom). Cycle is in German, as at the end of C.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| D1 | Language | Settings › App › Language, in German | As C2. She taps Sprache des Telefons. |
| D2 | Phone's language | Same page, redrawn in English | As C1: Phone's language · "English" (selected). From now on Cycle follows the phone. |
| D3 | The phone's languages | Android Settings › System › Languages | Android's own screen. Later, she puts Español (España) first: 1 Español (España), 2 English (United Kingdom). |
| D4 | Cycle follows | Settings, in Spanish | Next time she opens Cycle it is in Spanish. The row: Idioma · "Idioma del teléfono, Español". |

If Cycle is open when she changes the phone's language, it redraws in the new one when she comes
back to it, as any Android app does.

### E. Android 13 and later: the same choice in Android's settings

Her phone's languages: English (United Kingdom), on Android 14. Cycle follows the phone.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| E1 | Android's App language | Android Settings › Apps › Cycle › Language | Android's own screen: Suggested · System default (English (United Kingdom)); All languages · Deutsch, English, Español, Português (Brasil). Only Cycle's four. She picks Español. |
| E2 | Cycle in Spanish | Settings, in Spanish | When she comes back, Cycle is in Spanish. The row: Idioma · "Español". |
| E3 | The same choice | Settings › App › Language | Idioma del teléfono · "Inglés"; Español (selected). |

The other way round: a language chosen on Cycle's page shows as selected on Android's (E1), and
Phone's language shows as System default.

### F. An export in German, imported in English

Cycle is in German on her phone. Today is Tuesday 2 March 2027.

| Step | Screen | Where | Copy |
| -- | -- | -- | -- |
| F1 | Export | Settings › Your data, in German | Meine Daten exportieren · "Zuletzt exportiert am 2. März 2027". The file name Android's save screen suggests stays `cycle-export-2027-03-02.csv`. |
| F2 | The file | Not Cycle: the file in a text viewer | The columns (`date,flow,period_started,…,note`), the values (`medium`, `yes`, `moderate`, `cramps;lower_back`) and the `setting,value` lines are the same in every language. Her note reads "Langer Spaziergang, früh geschlafen." as she wrote it. No line names the language. |
| F3 | Restore on an English phone | Setup › Restore from a Cycle export | On a new phone in English: "Import 3 days?" "Days already logged on this phone stay as they are." Cancel · **Import** |
| F4 | The day comes back | Calendar › day log for 24 February | "Wednesday 24 February": Flow · Medium; Pain · Moderate, Cramps (Cycle's words, now in English); Your note · "Langer Spaziergang, früh geschlafen." Nah · **Log it** |

## Behaviour

- **Applying a choice.** One tap applies it; there is no Save. The page redraws in the new language
  with that option selected and its scroll position kept. Back returns to Settings in the new
  language; the selected tab and the screens behind stay. Nothing else is asked or reset.
- **Cold start.** Cycle opens in the stored language from its first frame, the lock screen
  included. It never shows a frame in another language first ([`0008`](../decisions/0008-languages.md)).
- **Delete everything** forgets the choice with every other setting: Cycle starts over following the
  phone, as on a fresh install.
- **Restore.** The choice comes back with Android's backup, not with a Cycle export.
- **Android's own parts** follow the phone: the unlock prompt's buttons, the file picker for export
  and import, Android's per-app language page, TalkBack's own words.

## Components

Every screen uses `core:designsystem` and `core:ui` as they are, except the additions below, which the
build adds first with their catalog entries, previews and tests
([`design-system.md`](design-system.md#adding-a-component)).

| Component | Used for | Status |
| -- | -- | -- |
| `TopAppBar`, `NavigationBar` | Every screen | Exist. `NavigationBar` changes, below. |
| Settings' `SettingsRow` and `SectionTitle` (feature code) | The App section and its Language row | Exist in `feature:settings` |
| `RadioRow` | The five options | **To add**, as specified in [`contraception.md`](contraception.md#components), with the two changes below. |
| `CycleIcons.Language` | The Language row in Settings | **To add.** Material Symbols Rounded "language", weight 600, like the others. The journeys page draws a stand-in globe. |
| `MonthCalendar`, `WeekRow`, `CycleLegend`, `DayCell` | A2, A4, B5, C4, C5, F4 | Exist. Their first day of the week comes from the phone, not Cycle's language ([`0008`](../decisions/0008-languages.md)). |
| `Card`, `FilledButton`, `TonalButton`, `TextButton`, `EmptyState`, `CycleBottomSheet`, `CycleAlertDialog`, `CycleTextField`, chips, `UsualLengthSliders` | The screens that only change their words | Exist |

**`RadioRow`: two changes to the contraception spec.**

1. **The body wraps.** The spec gives "an optional one-line body". Here the second line can be a
   sentence ("Französisch gibt es in Cycle noch nicht, deshalb nutzt diese Option Englisch."), and
   at 200% font size every line wraps. Title and body wrap to as many lines as they need, with no ellipsis,
   and the row grows; the radio stays vertically centred. This applies to the method list too.
2. **A language for the title.** An optional `titleLocale` (a `LocaleList`), applied to the title as
   a `SpanStyle(localeList = …)`. TalkBack then reads "Deutsch" with a German voice on an English
   phone, and line breaking follows that language. The language options set it; the method list
   doesn't.

Otherwise as specified: title in `titleSmall`, the body in `bodySmall`, a radio at the end,
`accentContainer` when selected, at least 48dp tall, `Role.RadioButton` in a `selectableGroup`, read
as "Deutsch, German, radio button, not selected, 2 of 5". `ButtonGroup` doesn't fit five options with
a line each.

**`NavigationBar`: an 11sp floor for its labels.** Its labels shrink, all to the same size, when the
widest does not fit at a large font size, but never below their size at 100%, and a label still too
wide ends in an ellipsis. In DM Sans at 12sp bold, "Einstellungen" is 85dp wide; a tab on a phone 360dp
wide has 80dp. So the floor becomes 11sp (78dp) instead of 12sp, and the German labels stay whole on
the narrowest phones Cycle runs on. English, Portuguese and Spanish fit at 12sp ("Calendário", the
longest, is 68dp), so their bars are unchanged. Measured from `dm_sans.ttf` at weight 700, 12sp, with
the label's 0.4sp letter spacing:

| Tab | English | Português (Brasil) | Español | Deutsch |
| -- | -- | -- | -- | -- |
| Today | Today | Hoje | Hoy | Heute |
| Calendar | Calendar | Calendário (68dp) | Calendario (68dp) | Kalender |
| History | History | Histórico | Historial | Verlauf |
| Settings | Settings (53dp) | Ajustes (47dp) | Ajustes (47dp) | Einstellungen (85dp) |

Portuguese uses "Ajustes", as Brazilians know it from their phones' settings; "Configurações" would
be 92dp.

**Dates that start a line.** Portuguese and Spanish write month and weekday names in lower case, and
Android formats them so: "março de 2027". Where a formatted date starts a title or a line (the
calendar's month title, Today's date line, a sheet's title), Cycle capitalizes its first letter with
the language's rules: "Março de 2027", "Mayo de 2027". German and English are unchanged.

## Copy checks

Against [`product-implications.md`](../research/product-implications.md#copy-rules) and
[`0008`](../decisions/0008-languages.md#copy-checks-in-every-language), which holds each language's
list of words the app never says and the tests that check them:

- English: no "safe", "fertile", "pregnan", "you should feel"; in History, no "normal" or "regular"
  either.
- Português (Brasil): no "seguro", "segura", "fértil", "fertil", "grávid", "gravidez", "engravid",
  "deveria se sentir", "deve se sentir"; in History, no "normal" or "regular" (which also catch
  "anormal", "normalmente" and "irregular").
- Español: no "seguro", "segura", "fértil", "fertil", "embaraz", "deberías sentir", "debes sentir";
  in History, no "normal" or "regular".
- Deutsch: no "sichere", "fruchtbar", "schwanger", "solltest dich", "fühlen solltest"; in History, no
  "normal" or "regelmäßig" (which also catches "unregelmäßig").
- No new string here names a condition, gives an estimate or speaks of fertility. None says the
  language change is risky or that anything is lost: "Everything you logged stays".
- In German, "Daten" means both "dates" and "data", so the intro says "Datumsangaben": "Daten ändern
  sich" would read as "your data changes". German copy also avoids "in der Regel" ("as a rule"),
  which reads as "during your period".
- Each language name is the one Android uses, so it matches the phone's own language lists.
