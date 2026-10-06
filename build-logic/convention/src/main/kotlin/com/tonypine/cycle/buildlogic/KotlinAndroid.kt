package com.tonypine.cycle.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Shared Android and Kotlin settings for every Android module. */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    val jvmToolchain = libs.versionOf("jvmToolchain").toInt()

    commonExtension.apply {
        compileSdk = libs.versionOf("compileSdk").toInt()
        defaultConfig.minSdk = libs.versionOf("minSdk").toInt()

        compileOptions.sourceCompatibility = JavaVersion.toVersion(jvmToolchain)
        compileOptions.targetCompatibility = JavaVersion.toVersion(jvmToolchain)

        // Errors fail the build; warnings are reported but do not.
        lint.abortOnError = true
        lint.warningsAsErrors = false
        lint.checkReleaseBuilds = true
        // Every string in every language Cycle has, never a silent fallback to English (0008-languages.md).
        lint.error += "MissingTranslation"
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(jvmToolchain)
    }
}
