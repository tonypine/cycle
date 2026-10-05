package com.tonypine.cycle.core.domain

/**
 * The usual cycle and period lengths she can give, in days, as setup and Settings offer them: a
 * slider over [CycleRules.USUAL_CYCLE_LENGTHS] and one over [CycleRules.USUAL_PERIOD_LENGTHS].
 */
object UsualLengths {
    /** [days] as a cycle length she can give: the nearest end of the range when it lies outside. */
    fun cycle(days: Int): Int = days.coerceIn(CycleRules.USUAL_CYCLE_LENGTHS)

    /** [days] as a period length she can give: the nearest end of the range when it lies outside. */
    fun period(days: Int): Int = days.coerceIn(CycleRules.USUAL_PERIOD_LENGTHS)
}
