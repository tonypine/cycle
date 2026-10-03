plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

group = "com.tonypine.cycle.buildlogic"

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    // compileOnly: the root build script puts these plugins on the classpath with `apply false`.
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ktlint.gradle.plugin)
    compileOnly(libs.roborazzi.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.cycle.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.cycle.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.cycle.android.compose.get().pluginId
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("screenshotTests") {
            id = libs.plugins.cycle.screenshot.tests.get().pluginId
            implementationClass = "ScreenshotTestsConventionPlugin"
        }
        register("ktlint") {
            id = libs.plugins.cycle.ktlint.get().pluginId
            implementationClass = "KtlintConventionPlugin"
        }
    }
}

ktlint {
    version.set(libs.versions.ktlint)
}
