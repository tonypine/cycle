package com.tonypine.cycle

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tonypine.cycle.core.data.settings.LanguageRepository
import com.tonypine.cycle.core.model.Language
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Applies Cycle's language (`docs/decisions/0008-languages.md`), with no frame in the wrong one.
 *
 * On Android 13 and later Android applies it to the process before any activity starts, and recreates
 * the activity when it changes; Cycle hands its stored choice to Android once, and copies Android's
 * back at every start. Below 13 Cycle applies it: [wrap] gives the activity a context in her language
 * before it draws, and [attach] recreates it when she changes it, which keeps the back stack and the
 * selected tab as a rotation does.
 */
class AppLanguage(private val languages: LanguageRepository, private val scope: CoroutineScope) {
    // Below 13: her choice, read once per process, then kept in memory for each recreation.
    @Volatile
    private var stored: Deferred<Language?> = CompletableDeferred(null)

    // Below 13: the language the activity was last created in.
    @Volatile
    private var applied: Language? = null

    // Below 13: the process's default locale before Cycle set her language as the default.
    private var phoneDefault: Locale? = null

    /** From `Application.onCreate`, before any activity. */
    fun onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The first start on this install waits for the hand-over, so even it opens in her language.
            runBlocking { if (!languages.isHandedOver()) languages.syncWithPhone() }
        } else {
            // The settings file is a few hundred bytes: read while the activity is being created.
            stored = scope.async(Dispatchers.IO) { languages.language.first() }
        }
    }

    /** [base] in her language, from the activity's `attachBaseContext`. Android's own on 13 and later. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val language = runBlocking { stored.await() }
        applied = language
        if (language == null) {
            phoneDefault?.let(Locale::setDefault)
            return base
        }
        val locale = language.locale
        if (phoneDefault == null) phoneDefault = Locale.getDefault()
        Locale.setDefault(locale)
        return base.createConfigurationContext(Configuration().apply { setLocale(locale) })
    }

    /** From the activity's `onCreate`: follows a change of language from then on. */
    fun attach(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // A language chosen on Android's page is the one Cycle reports, and backs up.
            activity.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        scope.launch { languages.syncWithPhone() }
                    }
                }
            )
        } else {
            val createdIn = applied
            activity.lifecycleScope.launch {
                languages.language.collect { language ->
                    if (language != createdIn) {
                        stored = CompletableDeferred(language)
                        activity.recreate()
                    }
                }
            }
        }
    }
}
