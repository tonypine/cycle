package com.tonypine.cycle.core.model

/**
 * How much she bled on a day. Light, medium and heavy match Health Connect's
 * `MenstruationFlowRecord`; spotting is its `IntermenstrualBleedingRecord`, kept apart because it
 * never starts a period (`docs/research/tracking-data.md`).
 */
enum class FlowLevel {
    /** She logged that she did not bleed. */
    NONE,
    SPOTTING,
    LIGHT,
    MEDIUM,
    HEAVY
    ;

    /** Light or heavier: a day that counts towards a period. */
    val isPeriodFlow: Boolean
        get() = this >= LIGHT
}
