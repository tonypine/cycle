package com.tonypine.cycle.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Shared Android and Kotlin settings for every Android module. */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    val jvmToolchain = libs.versionOf("jvmToolchain").toInt()

    commonExtension.apply {
        compileSdk = libs.versionOf("compileSdk").toInt()

        defaultConfig {
            minSdk = libs.versionOf("minSdk").toInt()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.toVersion(jvmToolchain)
            targetCompatibility = JavaVersion.toVersion(jvmToolchain)
        }

        lint {
            // Errors fail the build; warnings are reported but do not.
            abortOnError = true
            warningsAsErrors = false
            checkReleaseBuilds = true
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(jvmToolchain)
    }
}
