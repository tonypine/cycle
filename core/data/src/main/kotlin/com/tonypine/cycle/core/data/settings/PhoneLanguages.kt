package com.tonypine.cycle.core.data.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import com.tonypine.cycle.core.model.Language
import java.util.Locale

/**
 * The phone's side of Cycle's language: the languages she set for the whole phone, and on Android 13
 * and later the language Android keeps for Cycle on its own per-app page.
 */
interface PhoneLanguages {
    /** The phone's languages in her order, as Android Settings › System › Languages lists them. */
    val locales: List<Locale>

    /** Android 13 and later: Android keeps a language for Cycle itself, [appLanguage]. */
    val hasAppLanguage: Boolean

    /** Android's per-app language for Cycle: null for System default, and always null below Android 13. */
    var appLanguage: Language?
}

/** [PhoneLanguages] from Android: `LocaleManager` on Android 13 and later, the system's resources below. */
class AndroidPhoneLanguages(context: Context) : PhoneLanguages {
    private val context = context.applicationContext

    override val locales: List<Locale>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PerAppLanguage.systemLocales(context)
        } else {
            Resources.getSystem().configuration.locales.toList()
        }

    override val hasAppLanguage: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override var appLanguage: Language?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) PerAppLanguage.get(context) else null
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) PerAppLanguage.set(context, value)
        }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object PerAppLanguage {
    // The phone's own languages: with a per-app language set, the app's configuration starts with it.
    // A context without the service falls back to the system's resources.
    fun systemLocales(context: Context): List<Locale> =
        localeManager(context)?.systemLocales?.toList() ?: Resources.getSystem().configuration.locales.toList()

    // One language, as Android's page and Cycle set it.
    fun get(context: Context): Language? =
        localeManager(context)?.applicationLocales?.toList()?.firstOrNull()?.let { Language(it.toLanguageTag()) }

    fun set(context: Context, language: Language?) {
        localeManager(context)?.applicationLocales =
            if (language == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language.tag)
    }

    private fun localeManager(context: Context): LocaleManager? = context.getSystemService(LocaleManager::class.java)
}

private fun LocaleList.toList(): List<Locale> = List(size()) { get(it) }
