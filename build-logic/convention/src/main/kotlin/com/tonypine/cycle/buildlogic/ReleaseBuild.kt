package com.tonypine.cycle.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project

private val releaseSigningInputs =
    listOf("RELEASE_KEYSTORE_PATH", "RELEASE_KEYSTORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")

/**
 * Every app ships from the same release: the same version and the same release key, both from CI
 * inputs. See docs/decisions/0002-release-distribution.md.
 *
 * - Version: Gradle properties `cycle.versionCode` and `cycle.versionName`. Without
 *   `cycle.versionName`, it is `<cycle.versionMajorMinor>.<versionCode>`. Without either, local
 *   and PR builds use `1` and `<cycle.versionMajorMinor>.0`.
 * - Signing: the `release` build type signs with the keystore from the `RELEASE_*` environment
 *   variables. With none set, the release APK stays unsigned; with only some set, the build fails,
 *   so a missing secret cannot ship an unsigned APK.
 */
internal fun Project.configureReleaseBuild(application: ApplicationExtension) {
    val versionMajorMinor = providers.gradleProperty("cycle.versionMajorMinor").get()
    val ciVersionCode =
        providers.gradleProperty("cycle.versionCode").map {
            requireNotNull(it.toIntOrNull()?.takeIf { code -> code > 0 }) {
                "cycle.versionCode must be a positive integer, got '$it'"
            }
        }
    val ciVersionName =
        providers.gradleProperty("cycle.versionName").orElse(ciVersionCode.map { "$versionMajorMinor.$it" })

    val signingInputs =
        releaseSigningInputs.associateWith { providers.environmentVariable(it).orNull?.takeIf(String::isNotBlank) }
    val missingSigningInputs = signingInputs.filterValues { it == null }.keys
    check(missingSigningInputs.size in setOf(0, signingInputs.size)) {
        "Release signing needs all of ${signingInputs.keys}; missing $missingSigningInputs"
    }

    application.apply {
        defaultConfig.versionCode = ciVersionCode.getOrElse(1)
        defaultConfig.versionName = ciVersionName.getOrElse("$versionMajorMinor.0")

        if (missingSigningInputs.isEmpty()) {
            signingConfigs.create("release") {
                storeFile = file(signingInputs.getValue("RELEASE_KEYSTORE_PATH")!!)
                storePassword = signingInputs.getValue("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = signingInputs.getValue("RELEASE_KEY_ALIAS")
                keyPassword = signingInputs.getValue("RELEASE_KEY_PASSWORD")
            }
        }

        buildTypes.getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
}
