package com.tonypine.cycle.core.model

import java.time.LocalDate

/**
 * A method of contraception she can record (`docs/decisions/0006-contraception.md`). This is the one
 * list of methods: setup, Settings, storage and the estimates all read it, so adding a method is an
 * entry here and its code in storage. "None" is no method: a day outside every stretch.
 *
 * @property isHormonal its bleeding is not a period, and its cycles leave her typical cycle.
 * @property isCombined a combined pill, patch or ring: it takes [Breaks], and a bleed in the 7 days
 *   after it stops is still its withdrawal bleed.
 */
enum class ContraceptionMethod(val isHormonal: Boolean, val isCombined: Boolean = false) {
    COMBINED_PILL(isHormonal = true, isCombined = true),
    PROGESTOGEN_PILL(isHormonal = true),
    PATCH(isHormonal = true, isCombined = true),
    RING(isHormonal = true, isCombined = true),
    IMPLANT(isHormonal = true),
    HORMONAL_IUD(isHormonal = true),
    COPPER_IUD(isHormonal = false),
    INJECTION(isHormonal = true)
}

/** How she takes a combined pill, patch or ring. */
enum class Breaks {
    /** A week off, or dummy pills, in every pack: a scheduled bleed every 28 days. */
    MONTHLY,

    /** Two or more packs in a row, then a break: she decides when. */
    EVERY_FEW_PACKS,

    /** One pack straight after another. */
    NONE
}

/** What a method does to her estimates, derived from the method and its breaks (`0006`). */
enum class MethodBehaviour(val word: BleedingWord) {
    /** No method, or a copper IUD: her own cycle, estimated as in `0003`. */
    OWN_CYCLE(BleedingWord.PERIOD),

    /** A combined method with a break every month: a bleed expected in each break. */
    SCHEDULED_BLEED(BleedingWord.BLEED),

    /** Every other hormonal method: bleeding with no estimate. */
    NO_ESTIMATE(BleedingWord.BLEEDING)
}

/** What her bleeding is called on a day: "period", "bleed" or "bleeding" (`0006`, Words). */
enum class BleedingWord { PERIOD, BLEED, BLEEDING }

/**
 * One stretch of time on one method, as she recorded it. Stretches never overlap.
 *
 * @property started the first day on the method, or null when she skipped "since when" in setup:
 *   the stretch then covers every day before [stopped].
 * @property stopped the last day on the method, included, or null while she is still on it. Only an
 *   injection's can be after today: 13 weeks after her last injection.
 * @property breaks for a combined method only, and always given for one.
 * @property id the stored row, or 0 before it is saved.
 */
data class ContraceptionStretch(
    val method: ContraceptionMethod,
    val started: LocalDate?,
    val stopped: LocalDate? = null,
    val breaks: Breaks? = null,
    val id: Long = 0
) {
    init {
        require((breaks != null) == method.isCombined) { "$method takes breaks only if combined, got $breaks" }
    }

    val behaviour: MethodBehaviour
        get() = when {
            !method.isHormonal -> MethodBehaviour.OWN_CYCLE
            breaks == Breaks.MONTHLY -> MethodBehaviour.SCHEDULED_BLEED
            else -> MethodBehaviour.NO_ESTIMATE
        }

    /** [started] for comparing: an unknown start is earlier than any date. */
    val startKey: LocalDate
        get() = started ?: LocalDate.MIN

    /** [stopped] for comparing: no stop yet is later than any date. */
    val stopKey: LocalDate
        get() = stopped ?: LocalDate.MAX

    /** She was on the method on [date]. */
    operator fun contains(date: LocalDate): Boolean = date in startKey..stopKey

    /** The two stretches share a day. */
    fun overlaps(other: ContraceptionStretch): Boolean = startKey <= other.stopKey && other.startKey <= stopKey

    /** Every day of [other] lies in this stretch. */
    fun coversWhole(other: ContraceptionStretch): Boolean = startKey <= other.startKey && other.stopKey <= stopKey
}

/** Which estimate applies on a day (`0006`, Three behaviours). */
enum class EstimateKind {
    /** Her next period, as in `0003`: no method, a copper IUD, or after stopping one. */
    PERIOD,

    /** The next scheduled bleed, on a combined method with a break every month. */
    NEXT_BLEED,

    /** None: a method whose bleeding has no estimate, or after the injection until a period. */
    NONE
}

/**
 * Her contraception on the overview's day, so no screen decides on its own which words and
 * estimates apply.
 *
 * @property stretches every stretch she recorded that has started, oldest first.
 * @property current the stretch in force today, her method "Now"; null on none.
 * @property stopped the hormonal method she stopped, from the day after its stop date until her
 *   first period after it.
 * @property estimates which estimate applies today.
 * @property nextBleed on a combined method with a break every month; null there only while there
 *   is no start date and no bleed logged yet.
 * @property bleeding on a no-estimate method: her bleeding over the last 90 days, or since its start.
 * @property firstMonths in the current method's first months since its start date (never with an
 *   unknown start): the line that bleeding is often unsettled, or on a copper IUD the card that
 *   periods can be heavier at first.
 */
data class ContraceptionOverview(
    val stretches: List<ContraceptionStretch> = emptyList(),
    val current: ContraceptionStretch? = null,
    val stopped: StoppedMethod? = null,
    val estimates: EstimateKind = EstimateKind.PERIOD,
    val nextBleed: BleedEstimate? = null,
    val bleeding: BleedingSummary? = null,
    val firstMonths: Boolean = false
) {
    /** What the current method does, or her own cycle on none. */
    val behaviour: MethodBehaviour
        get() = current?.behaviour ?: MethodBehaviour.OWN_CYCLE
}

/**
 * A hormonal method she stopped and has had no period since.
 *
 * @property days since its stop date: "12 days", "since your implant came out".
 * @property withdrawalWindow within the 7 days after a combined method stopped, when a bleed that
 *   starts is still the method's withdrawal bleed.
 */
data class StoppedMethod(val stretch: ContraceptionStretch, val days: Int, val withdrawalWindow: Boolean)

/** Where a next-bleed estimate comes from. */
enum class BleedBasis {
    /** The first break after the start date: shown as a range only. */
    START_DATE,

    /** The last bleed on the method that counted. */
    LAST_BLEED
}

/**
 * The next bleeds on a combined method with a break every month.
 *
 * @property bleeds the next first, then the two after it, 28 days apart. Never in the past. With
 *   [BleedBasis.START_DATE] each one's expected start is the first day of its range.
 * @property missedBreak the last break passed with no bleed logged.
 */
data class BleedEstimate(val bleeds: List<EstimatedPeriod>, val basis: BleedBasis, val missedBreak: Boolean) {
    val next: EstimatedPeriod
        get() = bleeds.first()
}

/**
 * Bleeding over a stretch of days, in plain counts (`0006`, The last 90 days).
 *
 * @property from the first day counted: 90 days back, or the method's start when that is later.
 * @property to the last day counted: today, or the method's stop date.
 * @property sinceStart [from] is the method's start date: "Since 6 September", not "Last 90 days".
 * @property days bleeding or spotting days.
 * @property episodes runs of bleeding days in a row.
 * @property longest the longest run, in days; 0 with none.
 */
data class BleedingSummary(
    val from: LocalDate,
    val to: LocalDate,
    val sinceStart: Boolean,
    val days: Int,
    val episodes: Int,
    val longest: Int
)

/** What saving a stretch did, or why it did not. */
sealed interface StretchChange {
    data object Saved : StretchChange

    /**
     * Nothing saved yet: the dates overlap other stretches, and saving again with the moves
     * confirmed moves each one's edge as [moves] says ("Move the end of your implant?").
     */
    data class ConfirmMoves(val moves: List<StretchMove>) : StretchChange

    data class Refused(val reason: StretchRefusal) : StretchChange
}

/** A stored stretch [from] and how it would change to make room: [to], with one edge moved. */
data class StretchMove(val from: ContraceptionStretch, val to: ContraceptionStretch)

/** Why a stretch's dates cannot be saved. */
sealed interface StretchRefusal {
    /** A start after today. */
    data object StartAfterToday : StretchRefusal

    /** A new start on or before the [current] method's start: "That's before you started the pill on 3 May." */
    data class NotAfterCurrentStart(val current: ContraceptionStretch) : StretchRefusal

    /** The dates would cover all of [stretch], the latest one they cover whole. */
    data class CoversWhole(val stretch: ContraceptionStretch) : StretchRefusal

    /** A stop date before the start date. */
    data object StopBeforeStart : StretchRefusal

    /** A stop date after [latest]: today, or 13 weeks after today on the injection. */
    data class StopTooLate(val latest: LocalDate) : StretchRefusal

    /** The stretch is no longer stored. */
    data object Gone : StretchRefusal
}
