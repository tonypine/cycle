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
