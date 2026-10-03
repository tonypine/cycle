plugins {
    alias(libs.plugins.cycle.android.application)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

// A standalone app that shows every design system piece, for building and reviewing UI in isolation.
android {
    namespace = "com.tonypine.cycle.catalog"

    defaultConfig {
        applicationId = "com.tonypine.cycle.catalog"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(libs.androidx.activity.compose)
}
