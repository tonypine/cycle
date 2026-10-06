package com.tonypine.cycle.buildlogic

import com.android.build.api.dsl.ApplicationExtension

/**
 * The languages Cycle has strings for, as resource qualifiers (`en`, `pt-rBR`). The APK keeps only
 * these, its own and every library's, so all of a screen comes from one language
 * (docs/decisions/0008-languages.md). A language is added here in the PR that adds its strings, with
 * `localeConfig` and `Language.Supported`; `LanguagesTest` in `app` keeps the lists in step.
 */
private val localeFilters = listOf("en")

internal fun configureLocaleFilters(application: ApplicationExtension) {
    application.androidResources.localeFilters += localeFilters
    // For LanguagesTest, which compares the filter with the other lists.
    application.testOptions.unitTests.all { test ->
        test.systemProperty("cycle.localeFilters", localeFilters.joinToString(","))
    }
}
