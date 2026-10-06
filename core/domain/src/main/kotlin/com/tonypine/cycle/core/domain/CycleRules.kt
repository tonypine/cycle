package com.tonypine.cycle.core.domain

/**
 * Every number the cycle logic uses, in one place, each with where it comes from. The reasoning is
 * in `docs/decisions/0003-cycle-estimates.md` and, on contraception, `0006-contraception.md`; the
 * sources in `docs/research/`.
 */
object CycleRules {
    /**
     * The usual cycle length before setup: the conventional 28 days. The real mean is 29.3
     * (Bull 2019), but 28 is what people expect to see; her own cycles replace it as soon as she
     * logs any (`cycle-physiology.md`, "Use her numbers, not 28/14").
     */
    const val DEFAULT_CYCLE_LENGTH = 28

    /** The usual period length before setup: "usually about 5" days (NHS, `cycle-physiology.md`). */
    const val DEFAULT_PERIOD_LENGTH = 5

    /**
     * The usual cycle lengths she can give at setup. Anything in between is accepted, unusual or not
     * (`cycle-physiology.md`, "Bound inputs ... without rejecting her data"): a 19-day or a 50-day
     * cycle happens. 15 days is already shorter than the shortest common follicular phase (10 days,
     * period included) and luteal phase (7 days) together (Bull 2019), so anything shorter is not a
     * cycle; at 90 days without a period it is time to see a doctor (ACOG, `health-signals.md`), and
     * there is no rhythm left to estimate from.
     */
    val USUAL_CYCLE_LENGTHS = 15..90

    /**
     * The usual period lengths she can give at setup. More than 8 days is "prolonged" (FIGO 2018,
     * `cycle-physiology.md`) but still accepted; a period of more than two weeks is not a usual one.
     * The longest stays shorter than the shortest cycle, so the two always fit together.
     */
    val USUAL_PERIOD_LENGTHS = 1..14

    /**
     * Days without a period log that a period may contain and still be one period
     * (`predictions.md`, "Inputs": "allowing a gap of a day without a log inside it").
     */
    const val MAX_GAP_DAYS = 1

    /**
     * The complete cycles (and ended periods) her estimates and typical lengths are drawn from:
     * the last six, as the calendar rhythm rule uses (WHO FP Handbook, `predictions.md`).
     */
    const val RECENT_CYCLES = 6

    /**
     * With no complete cycle, the range is the expected start ± this many days: about half of
     * FIGO's 7–9-day regularity spread (FIGO 2018, `predictions.md`, "Little data").
     */
    const val NO_CYCLES_SPREAD_DAYS = 4

    /** With up to [FEW_CYCLES] complete cycles, the range is at least ± this many days (`predictions.md`). */
    const val FEW_CYCLES_MIN_SPREAD_DAYS = 3

    /** "One or two cycles" (`predictions.md`, "Little data"). */
    const val FEW_CYCLES = 2

    /** How many periods ahead to estimate (`predictions.md`, "Further ahead": "at most the next three"). */
    const val CYCLES_AHEAD = 3

    /**
     * Ask "still going?" once an open period has lasted her usual length plus this many days: it
     * catches a forgotten "ended" before the period runs on in the log (MOT-30's plan).
     */
    const val STILL_GOING_EXTRA_DAYS = 2

    /**
     * Ask "missed a period?" once the current cycle reaches this many times her usual cycle: a cycle
     * "about twice her usual length" may be a missed log (`cycle-physiology.md`, `predictions.md`).
     * 1.8 times asks a little before twice, while the missed period is still easy to remember. In
     * percent, so the day it is asked on is exact integer arithmetic.
     */
    const val MISSED_PERIOD_PERCENT = 180

    /**
     * A pack, patch cycle or ring cycle of a combined method, its break or dummy pills at its end:
     * 21/7, 24/4 and 26/2 packs all last 28 days (NHS, `contraception.md`, "Combined pill").
     */
    const val PACK_DAYS = 28

    /**
     * A bleed on a combined method counts toward the next estimate only this many days after the
     * start date or the last bleed that counted: the active pills before the break (21/7 pack,
     * `contraception.md`). An earlier one is bleeding between breaks.
     */
    const val DAYS_BEFORE_BREAK = 21

    /** The next bleed after one that counted is expected within ± this many days (`0006`). */
    const val BLEED_SPREAD_DAYS = 2

    /**
     * Bleeding that starts within this many days after a combined method stops is its withdrawal
     * bleed, not a period: the length of a pill break (FSRH CHC 2019, `contraception.md`).
     */
    const val WITHDRAWAL_BLEED_DAYS = 7

    /**
     * After stopping a hormonal method, the next period's range is at least ± this many days, wider
     * than the little-data rule: the first cycles after a method vary more (NHS, "1 to 3 months",
     * `contraception.md`, "Patch and ring" and "Stopping a method").
     */
    const val SETTLING_SPREAD_DAYS = 7

    /** The wider range after stopping lasts until she has logged this many complete cycles since (`0006`). */
    const val SETTLING_CYCLES = 3

    /** An injection counts this many weeks after the last one: the DMPA interval (`contraception.md`, "Injection"). */
    const val INJECTION_WEEKS = 13L

    /**
     * Bleeding on a no-estimate method is described over this many days: the WHO's reference period
     * (`contraception.md`, "Describing bleeding that does not follow a cycle").
     */
    const val BLEEDING_SUMMARY_DAYS = 90

    /**
     * The first months on a hormonal method, when bleeding is often unsettled (FSRH, "The first
     * three months are different", `contraception.md`), and the longer ones on a hormonal IUD, whose
     * bleeding takes longer to settle (`contraception.md`, "Hormonal IUD").
     */
    const val FIRST_MONTHS = 3L
    const val HORMONAL_IUD_FIRST_MONTHS = 6L

    /**
     * How long after a copper IUD is fitted Cycle says periods can be heavier at first: "especially
     * in the first three to six months" (WHO FP Handbook, `contraception.md`, "Copper IUD").
     */
    const val COPPER_IUD_NOTE_MONTHS = 6L
}
