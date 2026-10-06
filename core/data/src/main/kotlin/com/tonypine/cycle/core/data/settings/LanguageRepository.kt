package com.tonypine.cycle.core.data.settings

import com.tonypine.cycle.core.model.Language
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Cycle's language (`docs/decisions/0008-languages.md`): the phone's, or one she chose.
 *
 * Below Android 13 her choice is Cycle's own, in the settings, and the app applies it. On 13 and later
 * Android applies it and shows it on its own per-app page, which is then where it lives; the settings
 * keep a copy, so a backup restored on an older phone keeps it. [syncWithPhone] keeps the two the same
 * value, whichever side changed it.
 *
 * @param handedOver a file outside every backup, there once Cycle has handed its stored choice to
 *   Android on this install (Android 13 and later).
 */
class LanguageRepository(
    private val settings: SettingsRepository,
    private val phone: PhoneLanguages,
    private val handedOver: File,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    /** Her choice: one of [Language.Supported], or null for the phone's language. */
    val language: Flow<Language?> = settings.language

    /**
     * Chooses [language], or the phone's language with null. On Android 13 and later Android applies
     * it and recreates the activity; below, the app does.
     */
    suspend fun setLanguage(language: Language?) {
        settings.setLanguage(language)
        phone.appLanguage = language
    }

    /** What "the phone's language" is in Cycle now, among the languages it has: for the Settings row. */
    fun phoneLanguage(): Language = Language.ofPhone(phone.locales)

    /**
     * Whether [syncWithPhone] can run without the app waiting for it: always below Android 13, and on
     * 13 and later once Cycle has handed its choice to Android on this install.
     */
    suspend fun isHandedOver(): Boolean = !phone.hasAppLanguage || withContext(io) { handedOver.exists() }

    /**
     * Android 13 and later, at every start and return to the front: copies Android's per-app language
     * into the settings, so a choice made on Android's page is the one Cycle reports.
     *
     * The first time on this install it hands Cycle's stored choice to Android instead, when Android
     * has none: a phone updated from Android 12, or a backup from Android 10 to 12 restored on 13,
     * starts with an empty list on Android's side and her choice in the settings. Does nothing below
     * Android 13.
     */
    suspend fun syncWithPhone() {
        if (!phone.hasAppLanguage) return
        val android = phone.appLanguage
        val stored = settings.language.first()
        val handOver = !isHandedOver()
        when {
            android == stored -> Unit

            handOver && android == null -> phone.appLanguage = stored

            // Android restored its own setting, or she chose on Android's page: Android's wins.
            else -> settings.setLanguage(android)
        }
        if (handOver) {
            withContext(io) {
                handedOver.parentFile?.mkdirs()
                handedOver.createNewFile()
            }
        }
    }

    /**
     * "Delete everything": on Android 13 and later Android's per-app language goes too, so Cycle
     * follows the phone again. The settings, with Cycle's copy, are cleared with the rest.
     */
    fun forget() {
        phone.appLanguage = null
    }
}
