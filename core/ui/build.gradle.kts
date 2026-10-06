plugins {
    alias(libs.plugins.cycle.android.library)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

// Shared app-level UI built on core:designsystem: what more than one feature shows, such as the day
// log sheet that Today and the calendar both open, and the usual-length fields of setup and Settings.
android {
    namespace = "com.tonypine.cycle.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    implementation(projects.core.domain)
    // The copy check in every language, and rendering in one (core:testing).
    testImplementation(projects.core.testing)
}
