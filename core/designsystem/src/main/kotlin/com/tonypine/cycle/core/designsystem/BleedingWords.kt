package com.tonypine.cycle.core.designsystem

/**
 * What her bleeding is called on a day, which changes the calendar's labels and what TalkBack reads,
 * never the shapes or colours (`docs/design/contraception.md`, Components): "period" with no method
 * or a copper IUD, "bleed" for the scheduled bleed on a combined method with a break every month,
 * "bleeding" on every other hormonal method.
 */
enum class BleedingWords {
    /** "Period", "Predicted period", "14 October, period". The default. */
    Period,

    /** "Bleed", "Expected bleed", "14 October, bleed". */
    Bleed,

    /** "Bleeding", "14 October, bleeding". Nothing is expected on these methods. */
    Bleeding
}
