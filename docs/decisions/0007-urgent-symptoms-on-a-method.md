# 0007: Urgent symptoms on a contraceptive method

**Status:** accepted, 2026-10-05

## Context

Cycle will record her contraceptive method and explain what it does to her bleeding
([`contraception.md`](../research/contraception.md)). The clinical sources behind that research also
list symptoms that need urgent help on some methods: calf pain, chest pain, sudden severe pain low in
the tummy. The research left them out of the app until a ticket decided the wording and a person
reviewed it, because Cycle is not a triage tool
([`health-signals.md`](../research/health-signals.md)). This is that decision
([MOT-66](https://linear.app/tonypine/issue/MOT-66)). Until now Cycle says nothing about them.

The FSRH asks clinics to tell every user of a combined method which symptoms should send them for
urgent review ([FSRH CHC 2019, 12.5 and Box 4](https://www.cosrh.org/Common/Uploaded%20files/documents/fsrh-guideline-combined-hormonal-contraception-october-2023.pdf)),
and the NHS lists the urgent ones for both IUDs
([copper IUD](https://www.nhs.uk/contraception/methods-of-contraception/iud-coil/side-effects/),
[hormonal IUD](https://www.nhs.uk/contraception/methods-of-contraception/ius-hormonal-coil/side-effects-and-risks/)).
A screen that explains her method and leaves these out would leave out the part that matters most.

## Decision

A short "When to get help" section, the same every time, on the screen that shows her method, for
the methods whose sources list urgent symptoms. Cycle never decides, from what she logs, that she
needs help.

| Area | Decision |
| -- | -- |
| Where | The method screen designed in [MOT-50](https://linear.app/tonypine/issue/MOT-50) (Settings › Your cycle › Contraception), below what the method does to bleeding: under the Now card and its calm line, above her earlier methods ([design](../design/contraception.md#when-to-get-help)). Shown in full, not folded away, for as long as the method is set. |
| Which methods | Combined pill, patch and ring; copper IUD and hormonal IUD. No section for the progestogen-only pill, implant or injection, and none without a method: the research found no urgent-symptom list for them, and adding one needs a source and a change to this record. |
| What it is | Reference text: one optional intro line, then the signs under the action to take, in the words below. Every user of the method sees the same text. |
| What it never is | Not a notification, a signal card, a banner, a pop-up or a step in setting the method. Never shown because of something she logged, and never hidden because of it. No symptom checker and no questions. |
| Look | The design system's normal body text and headings, in the calm treatment of the signal cards. No error red, no warning icon. The two action lines are headings for TalkBack, which reads the same words. |
| Numbers | No phone numbers. Cycle does not know her country, and a wrong emergency number is worse than none. "Emergency help" and "urgent medical advice" are words she can act on anywhere. |
| What Cycle does not watch | The section ends by saying so, so its silence elsewhere is never read as "all clear". |
| In code | The strings live in one place, each with its source in a comment, like the signal thresholds. A test pins each method's text. Changing a word means changing this record in the same PR. |

### Combined pill, patch and ring

> **When to get help**
>
> Clinics give these signs to everyone who uses the combined pill, patch or ring.
>
> **Get emergency help now if you have:**
>
> - chest pain, or you feel short of breath, or you cough up blood
> - sudden weakness or numbness in your face, an arm or a leg, or trouble speaking
>
> **Get urgent medical advice today if you have:**
>
> - pain, swelling or redness in one leg, usually the calf
>
> Cycle does not check your log for these signs.

On the patch or ring screen, the intro line names that method only.

| Line | Source |
| -- | -- |
| Chest pain, breathlessness, coughing up blood: emergency help | FSRH Box 4, urgent review: "Chest pain and/or breathlessness and/or coughing up blood". The NHS says to call 999 or go to A&E with symptoms of a clot in the leg and shortness of breath or chest pain ([NHS, DVT](https://www.nhs.uk/conditions/blood-clots/)). |
| Weakness, numbness, trouble speaking: emergency help | FSRH Box 4, urgent review: "Loss of motor or sensory function". The NHS's stroke signs (face, arm, speech) and its "Call 999 now" ([NHS, stroke](https://www.nhs.uk/conditions/stroke/symptoms/)). |
| Pain, swelling or redness in one leg: urgent advice today | FSRH Box 4, urgent review: "Calf pain, swelling and/or redness". The NHS: pain or swelling in one leg, usually in the calf or thigh, and red or darkened skin; ask for an urgent GP appointment or call 111 ([NHS, DVT](https://www.nhs.uk/conditions/blood-clots/)). |

### Copper IUD and hormonal IUD

> **When to get help**
>
> Clinics give these signs to everyone who has an IUD.
>
> **Get urgent medical advice today if you have:**
>
> - pain low in your tummy that painkillers do not help
> - sudden pain low in your tummy that gets worse or does not go away
> - a high temperature
> - unusual or smelly discharge
> - very heavy bleeding
>
> Cycle does not check your log for these signs.

Each line is the NHS's, under its "ask for an urgent GP appointment or get help from NHS 111" for
both the copper IUD and the hormonal IUD
([copper](https://www.nhs.uk/contraception/methods-of-contraception/iud-coil/side-effects/),
[hormonal](https://www.nhs.uk/contraception/methods-of-contraception/ius-hormonal-coil/side-effects-and-risks/)),
in second person and without the medical words.

### Translations

The English above is the source. Each language follows it line by line
([`0008`](0008-languages.md#copy-rules-in-every-language)): the same two actions, each sign with the
same meaning in everyday words, the intro and the closing line, with no condition names, no phone
numbers and no country's own service named. Each line has its back-translation into English beside
it. They are drafts until a native speaker has read them (0008, "Review of translations"), and
`WhenToGetHelpTest` pins each language as it pins the English. Changing a word in any language
changes this record in the same PR.

**Português (Brasil)**

| Line | Translation | Back-translation |
| -- | -- | -- |
| Title | Quando procurar ajuda | When to look for help |
| Intro, combined pill | As clínicas passam estes sinais a todas as pessoas que usam a pílula combinada, o adesivo ou o anel. | Clinics pass these signs on to everyone who uses the combined pill, the patch or the ring. |
| Intro, patch | As clínicas passam estes sinais a todas as pessoas que usam o adesivo. | Clinics pass these signs on to everyone who uses the patch. |
| Intro, ring | As clínicas passam estes sinais a todas as pessoas que usam o anel. | Clinics pass these signs on to everyone who uses the ring. |
| Intro, IUD | As clínicas passam estes sinais a todas as pessoas que usam um DIU. | Clinics pass these signs on to everyone who uses an IUD. |
| Emergency help | Procure atendimento de emergência agora se você tiver: | Seek emergency care now if you have: |
| Chest | dor no peito, ou falta de ar, ou tosse com sangue | chest pain, or shortness of breath, or a cough with blood |
| Weakness | fraqueza ou dormência repentina no rosto, em um braço ou em uma perna, ou dificuldade para falar | sudden weakness or numbness in the face, in an arm or in a leg, or difficulty speaking |
| Urgent advice | Procure orientação médica urgente hoje se você tiver: | Seek urgent medical advice today if you have: |
| Leg | dor, inchaço ou vermelhidão em uma perna, geralmente na panturrilha | pain, swelling or redness in one leg, usually in the calf |
| Painkillers | dor no pé da barriga que não passa com analgésicos | pain low in the belly that painkillers do not take away |
| Sudden pain | dor repentina no pé da barriga que piora ou não passa | sudden pain low in the belly that gets worse or does not go away |
| Temperature | febre alta | a high fever |
| Discharge | corrimento fora do comum ou com mau cheiro | unusual or bad-smelling discharge |
| Bleeding | sangramento muito intenso | very heavy bleeding |
| Closing | O Cycle não verifica seu registro em busca destes sinais. | Cycle does not check your log for these signs. |

**Español**

| Line | Translation | Back-translation |
| -- | -- | -- |
| Title | Cuándo buscar ayuda | When to seek help |
| Intro, combined pill | Las clínicas dan estas señales a todas las personas que usan la píldora combinada, el parche o el anillo. | Clinics give these signs to everyone who uses the combined pill, the patch or the ring. |
| Intro, patch | Las clínicas dan estas señales a todas las personas que usan el parche. | Clinics give these signs to everyone who uses the patch. |
| Intro, ring | Las clínicas dan estas señales a todas las personas que usan el anillo. | Clinics give these signs to everyone who uses the ring. |
| Intro, IUD | Las clínicas dan estas señales a todas las personas que llevan un DIU. | Clinics give these signs to everyone who has an IUD. |
| Emergency help | Busca ayuda de emergencia ahora si tienes: | Get emergency help now if you have: |
| Chest | dolor en el pecho, o te falta el aire, o toses sangre | pain in the chest, or you are short of breath, or you cough up blood |
| Weakness | debilidad o entumecimiento repentinos en la cara, un brazo o una pierna, o dificultad para hablar | sudden weakness or numbness in the face, an arm or a leg, or difficulty speaking |
| Urgent advice | Busca atención médica urgente hoy si tienes: | Get urgent medical attention today if you have: |
| Leg | dolor, hinchazón o enrojecimiento en una pierna, sobre todo en la pantorrilla | pain, swelling or redness in one leg, mostly in the calf |
| Painkillers | dolor en la parte baja de la barriga que no se calma con analgésicos | pain in the lower part of the belly that painkillers do not ease |
| Sudden pain | dolor repentino en la parte baja de la barriga que empeora o no se va | sudden pain in the lower part of the belly that gets worse or does not go away |
| Temperature | fiebre alta | a high fever |
| Discharge | flujo vaginal raro o con mal olor | strange or bad-smelling vaginal discharge |
| Bleeding | un sangrado muy abundante | very heavy bleeding |
| Closing | Cycle no revisa tu registro en busca de estas señales. | Cycle does not check your log for these signs. |

**Deutsch**

| Line | Translation | Back-translation |
| -- | -- | -- |
| Title | Wann du Hilfe brauchst | When you need help |
| Intro, combined pill | Praxen geben diese Anzeichen allen mit, die die Kombinationspille, das Pflaster oder den Ring verwenden. | Doctors' practices give these signs to everyone who uses the combined pill, the patch or the ring. |
| Intro, patch | Praxen geben diese Anzeichen allen mit, die das Pflaster verwenden. | Doctors' practices give these signs to everyone who uses the patch. |
| Intro, ring | Praxen geben diese Anzeichen allen mit, die den Ring verwenden. | Doctors' practices give these signs to everyone who uses the ring. |
| Intro, IUD | Praxen geben diese Anzeichen allen mit, die eine Spirale haben. | Doctors' practices give these signs to everyone who has an IUD. |
| Emergency help | Hol sofort Notfallhilfe, wenn du Folgendes hast: | Get emergency help at once if you have the following: |
| Chest | Schmerzen in der Brust, Atemnot oder du hustest Blut | pain in the chest, shortness of breath, or you cough up blood |
| Weakness | plötzliche Schwäche oder Taubheit im Gesicht, in einem Arm oder einem Bein, oder Schwierigkeiten beim Sprechen | sudden weakness or numbness in the face, in an arm or a leg, or difficulty speaking |
| Urgent advice | Hol dir heute noch dringend ärztlichen Rat, wenn du Folgendes hast: | Get urgent medical advice today if you have the following: |
| Leg | Schmerzen, Schwellung oder Rötung in einem Bein, meist in der Wade | pain, swelling or redness in one leg, mostly in the calf |
| Painkillers | Schmerzen im Unterbauch, gegen die Schmerzmittel nicht helfen | pain in the lower belly that painkillers do not help |
| Sudden pain | plötzliche Schmerzen im Unterbauch, die schlimmer werden oder nicht weggehen | sudden pain in the lower belly that gets worse or does not go away |
| Temperature | hohes Fieber | a high fever |
| Discharge | ungewöhnlichen oder übel riechenden Ausfluss | unusual or foul-smelling discharge |
| Bleeding | sehr starke Blutungen | very heavy bleeding |
| Closing | Cycle prüft deine Einträge nicht auf diese Anzeichen. | Cycle does not check your entries for these signs. |

### Left out on purpose

- **"You think you might be pregnant" and "you cannot feel the threads"**, from the same NHS IUD
  lists. Both are about whether the method still works, which is a question for her clinic, and
  Cycle does not prompt pregnancy tests ([`contraception.md`](../research/contraception.md),
  [`product-implications.md`](../research/product-implications.md)). The urgent sign behind the
  first, sudden pain low in the tummy, is in the list.
- **The FSRH's non-urgent review list** for combined methods: breast changes, a new migraine,
  persistent unscheduled bleeding. Not urgent, so outside this record. Persistent unscheduled
  bleeding is already a signal card ([`contraception.md`](../research/contraception.md)).
- **The FSRH's new diagnoses that call for a review of the method** (high blood pressure, a past
  clot, migraine with aura and so on). Acting on them means advising on her method, which Cycle never
  does.
- **"Stop taking the pill"** or any other step about the method itself. Where to get help is the
  whole message.
- **The names of the conditions** (a blood clot, a stroke, an infection). The signs and the action
  are what she needs, and the copy rules keep condition names out
  ([`product-implications.md`](../research/product-implications.md)).

## Options considered

- **A fixed section on the method screen (chosen).** The sources treat this as information every
  user is given, not something to work out from her data. It is there whenever she opens her
  method, works offline, and promises nothing about watching her log.
- **Say nothing (as now).** Keeps Cycle furthest from triage, but the screen that explains her method
  would leave out what clinics most want users to know.
- **An alert when she logs a matching symptom**, such as severe pain on an IUD. She often logs hours
  later, so it would come late, and it would teach her that Cycle is watching: the day it stays
  quiet would read as "all clear". That is triage, which the research rules out.
- **A card or notification when she sets the method.** Alarming at the wrong moment, against the
  "calm and once" rule, and gone once dismissed, when she may need it months later.
- **A link to the NHS pages instead of text.** Needs the network, opens a UK service wherever she
  is, and may move or change without anyone checking.

## Consequences

- Nothing ships with this record. The section is built with the method screen
  ([MOT-50](https://linear.app/tonypine/issue/MOT-50)).
- Another method gets a section only with a source and a change to this record, as does any change
  to the wording.
- If she lives in one country and would like its numbers (999 and 111 in the UK), that is a one-line
  change and her call.
