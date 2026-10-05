package com.tonypine.cycle.feature.history

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * [date] in the phone's locale as day and month, "Feb 2" with [short] months and "February 2"
 * without, with the year when it is not [today]'s, since History reaches back into past years.
 */
@Composable
internal fun formatDate(date: LocalDate, today: LocalDate, short: Boolean = false): String {
    val month = if (short) "MMM" else "MMMM"
    val skeleton = if (date.year == today.year) "d$month" else "d${month}y"
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale, skeleton) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }
    return formatter.format(date)
}
