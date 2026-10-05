package com.tonypine.cycle.core.domain

/**
 * Every number the cycle logic uses, in one place, each with where it comes from. The reasoning is
 * in `docs/decisions/0003-cycle-estimates.md`; the sources in `docs/research/`.
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
}
