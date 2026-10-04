plugins {
    alias(libs.plugins.cycle.android.application)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

// A standalone app that shows every design system component, for building and reviewing UI in isolation.
// Every release ships it next to the app, with the same version and release key (cycle.android.application).
android {
    namespace = "com.tonypine.cycle.catalog"

    defaultConfig {
        applicationId = "com.tonypine.cycle.catalog"
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(libs.androidx.activity.compose)
}
