# Research

What we know about the menstrual cycle, and what it means for Cycle. Read these before deciding on a
feature, a screen, a prediction, a notification or a piece of copy that touches the cycle itself.
They are research notes, not decisions: a decision that comes out of them goes in
[`docs/decisions/`](../decisions/) as a numbered record, and links back here.

| Document | Read it when you work on |
| -- | -- |
| [`cycle-physiology.md`](cycle-physiology.md) | Anything that names a phase, a day of the cycle or a "normal" range: the calendar, the cycle day counter, phase labels, onboarding defaults, life stages. |
| [`predictions.md`](predictions.md) | The next period, the fertile window and ovulation: how to compute them, how sure they can be, and how to show them. |
| [`tracking-data.md`](tracking-data.md) | The daily log: what she can record, the scales for each item, and the data model under it. |
| [`contraception.md`](contraception.md) | Anything that depends on her contraception: how each method changes bleeding, ovulation and what can be estimated, the words for what she logs (period, bleed, bleeding), stopping a method, and the patterns on a method worth a doctor's visit. |
| [`health-signals.md`](health-signals.md) | Patterns worth a doctor's visit, the conditions behind them, what the app may say about them and how. |
| [`apps-and-privacy.md`](apps-and-privacy.md) | How other trackers do it, what users complain about, and the privacy failures to avoid. |
| [`product-implications.md`](product-implications.md) | The synthesis: principles, candidate features and screens in order, defaults, copy rules and the open questions for her. Start here when planning. |

## How these were written

Researched in October 2026 for [MOT-26](https://linear.app/tonypine/issue/MOT-26);
[`contraception.md`](contraception.md) for [MOT-49](https://linear.app/tonypine/issue/MOT-49), and its
monthly combined injection section for [MOT-67](https://linear.app/tonypine/issue/MOT-67). Sources,
in order of preference:

1. Clinical bodies and their guidance: WHO, FIGO, ACOG, NICE, the NHS, the CDC, STRAW+10, the FSRH
   (the UK's Faculty of Sexual and Reproductive Healthcare, now the CoSRH).
2. Peer-reviewed studies, favouring large prospective ones (Wilcox's hormone-measured cycles) and
   large real-world app datasets (Natural Cycles, Flo, the Apple Women's Health Study).
3. Product labels (the Nexplanon and Lunelle prescribing information) for what a method's trials
   found.
4. Product documentation and regulators' actions (Apple, Android, the FTC, Mozilla's reviews) for
   how apps behave and fail.

Every number links to where it came from. App-data studies describe the people who use those apps,
who are not everyone: they skew young, logged their cycles well enough to be counted, and in some
studies measured their temperature every morning. Treat their averages as typical, not as limits.

## Rules for these documents

- **Nothing here is medical advice.** The app may show what she logged and say when a pattern is one
  a doctor would want to hear about. It never diagnoses, and never says a day is "safe".
- **No real health data.** Examples use made-up dates and numbers, like the rest of the repo.
- **Keep it current.** When a ticket learns something that changes a finding here (her own
  preferences included), update the document in the same PR and say what changed.
- **Add, don't scatter.** New research on the cycle goes in one of these files or a new one listed
  in the table above, each ending with what it means for Cycle and its sources.
