package com.tonypine.cycle.core.data.database

import androidx.room.TypeConverter
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate

/**
 * How dates, flow and contraception are written in the database. All are stored as fixed text, so
 * renaming a Kotlin constant cannot change what is on her phone.
 */
internal class Converters {
    /** ISO-8601 (`2027-03-02`): a calendar day with no time and no zone, sorting in date order. */
    @TypeConverter
    fun dateToText(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun textToDate(text: String?): LocalDate? = text?.let(LocalDate::parse)

    @TypeConverter
    fun flowToText(flow: FlowLevel?): String? = flow?.let(FLOW::encode)

    @TypeConverter
    fun textToFlow(text: String?): FlowLevel? = text?.let { value ->
        checkNotNull(FLOW.decode(value)) { "Unknown flow \"$value\"" }
    }

    @TypeConverter
    fun methodToText(method: ContraceptionMethod?): String? = method?.let(METHOD::encode)

    @TypeConverter
    fun textToMethod(text: String?): ContraceptionMethod? = text?.let { value ->
        checkNotNull(METHOD.decode(value)) { "Unknown method \"$value\"" }
    }

    @TypeConverter
    fun breaksToText(breaks: Breaks?): String? = breaks?.let(BREAKS::encode)

    @TypeConverter
    fun textToBreaks(text: String?): Breaks? = text?.let { value ->
        checkNotNull(BREAKS.decode(value)) { "Unknown breaks \"$value\"" }
    }

    companion object {
        /** The text each flow is stored and exported as. Never change a code that has shipped. */
        val FLOW = FeelingCodes.Codes(
            FlowLevel.NONE to "none",
            FlowLevel.SPOTTING to "spotting",
            FlowLevel.LIGHT to "light",
            FlowLevel.MEDIUM to "medium",
            FlowLevel.HEAVY to "heavy"
        )

        /** The text each method is stored and exported as (`0006`). Never change a code that has shipped. */
        val METHOD = FeelingCodes.Codes(
            ContraceptionMethod.COMBINED_PILL to "combined_pill",
            ContraceptionMethod.PROGESTOGEN_PILL to "progestogen_pill",
            ContraceptionMethod.PATCH to "patch",
            ContraceptionMethod.RING to "ring",
            ContraceptionMethod.IMPLANT to "implant",
            ContraceptionMethod.HORMONAL_IUD to "hormonal_iud",
            ContraceptionMethod.COPPER_IUD to "copper_iud",
            ContraceptionMethod.INJECTION to "injection"
        )

        /** The text each kind of breaks is stored and exported as (`0006`). */
        val BREAKS = FeelingCodes.Codes(
            Breaks.MONTHLY to "monthly",
            Breaks.EVERY_FEW_PACKS to "every_few_packs",
            Breaks.NONE to "none"
        )
    }
}
