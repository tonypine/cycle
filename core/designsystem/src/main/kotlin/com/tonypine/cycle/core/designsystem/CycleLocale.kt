package com.tonypine.cycle.core.designsystem

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Cycle's locale, for every date, month and weekday name and number it formats
 * (`docs/decisions/0008-languages.md`): the language of the strings Android picked, with a region.
 *
 * Following the phone, it is the phone's first locale in that language, region and all ("es-MX"), or
 * with none of Cycle's languages on the phone, English with the region of the phone's first locale
 * ("en-FR"). With a language she chose, it is that language with the region of the phone's first
 * locale ("de-US" for German on a US phone), unless the language has its own ("pt-BR").
 *
 * Never `Configuration.locales[0]`: on a phone in French then Spanish, Android shows the Spanish
 * strings while the first locale stays French.
 */
@Composable
fun cycleLocale(): Locale {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val stringsTag = stringResource(R.string.cycle_language_tag)
    return remember(configuration, stringsTag) {
        cycleLocale(stringsTag, configuration.locales.toList(), phoneLocales(context))
    }
}

/**
 * [text], a formatted date that starts a title or a line, with its first letter capitalized by
 * [locale]'s rules: Portuguese and Spanish write month and weekday names in lower case ("março de
 * 2027"), but a line starts with a capital ("Março de 2027"). English and German are unchanged.
 */
fun startingLine(text: String, locale: Locale): String = text.replaceFirstChar { it.titlecase(locale) }

/**
 * The first day of the week, from the phone whatever Cycle's language is: Android 14's "First day of
 * week" when she set one, otherwise the region of the phone's first locale. Cycle's calendar starts
 * its weeks where the phone's own calendar does.
 */
@Composable
fun firstDayOfWeek(): DayOfWeek {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    return remember(configuration) {
        WeekFields.of(phoneLocales(context).firstOrNull() ?: Locale.getDefault()).firstDayOfWeek
    }
}

/**
 * [cycleLocale] for the strings in [stringsTag], in an app whose configuration has [appLocales], on a
 * phone set to [phoneLocales]. A choice of hers shows as an app locale other than the phone's first.
 */
internal fun cycleLocale(stringsTag: String, appLocales: List<Locale>, phoneLocales: List<Locale>): Locale {
    val strings = Locale.forLanguageTag(stringsTag)
    val phone = phoneLocales.ifEmpty { appLocales }
    val chosen = appLocales.firstOrNull() != phone.firstOrNull()
    if (!chosen) phone.firstOrNull { it.language == strings.language }?.let { return it }
    if (strings.country.isNotEmpty()) return strings
    val region = phone.firstOrNull()?.country.orEmpty()
    return Locale.Builder().setLocale(strings).setRegion(region).build()
}

/** The phone's own languages, in her order, whatever language Cycle is in. */
private fun phoneLocales(context: Context): List<Locale> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    SystemLocales.of(context)
} else {
    // Below Android 13 Cycle sets its language on the activity only, never on the system's resources.
    Resources.getSystem().configuration.locales.toList()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object SystemLocales {
    // With a per-app language, the process's configuration starts with it; this is the phone's own list.
    // A context without the service, such as a preview's, falls back to the system's resources.
    fun of(context: Context): List<Locale> =
        context.getSystemService(LocaleManager::class.java)?.systemLocales?.toList()
            ?: Resources.getSystem().configuration.locales.toList()
}

private fun LocaleList.toList(): List<Locale> = List(size()) { get(it) }
