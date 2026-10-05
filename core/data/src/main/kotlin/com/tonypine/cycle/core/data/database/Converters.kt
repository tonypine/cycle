package com.tonypine.cycle.core.data.database

import androidx.room.TypeConverter
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate

/**
 * How dates and flow are written in the database. Both are stored as fixed text, so renaming a
 * Kotlin constant cannot change what is on her phone.
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

    companion object {
        /** The text each flow is stored and exported as. Never change a code that has shipped. */
        val FLOW = FeelingCodes.Codes(
            FlowLevel.NONE to "none",
            FlowLevel.SPOTTING to "spotting",
            FlowLevel.LIGHT to "light",
            FlowLevel.MEDIUM to "medium",
            FlowLevel.HEAVY to "heavy"
        )
    }
}
