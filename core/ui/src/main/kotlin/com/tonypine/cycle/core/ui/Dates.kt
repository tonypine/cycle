package com.tonypine.cycle.core.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Skeletons for [DateFormat.getBestDateTimePattern]: the locale picks the order and punctuation. */
const val DAY_AND_MONTH = "dMMMM"
const val DAY_AND_DATE = "EEEEdMMMM"
const val DAY_MONTH_AND_YEAR = "dMMMMy"
const val DAY_AND_SHORT_MONTH = "dMMM"
const val DAY_SHORT_MONTH_AND_YEAR = "dMMMy"

fun dateFormatter(locale: Locale, skeleton: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

/** [date] in the phone's locale, by default as day and month: "23 October". */
@Composable
fun formatDate(date: LocalDate, skeleton: String = DAY_AND_MONTH): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale, skeleton) { dateFormatter(locale, skeleton) }
    return formatter.format(date)
}
