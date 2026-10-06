plugins {
    alias(libs.plugins.cycle.android.library)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

// Calendar: her logged and estimated periods month by month, and the day log for any day up to today.
android {
    namespace = "com.tonypine.cycle.feature.calendar"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.data)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // The ViewModel tests run the real repositories on an in-memory database and a DataStore file.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.kotlinx.coroutines.test)
    // The copy check in every language, and rendering in one (core:testing).
    testImplementation(projects.core.testing)
}
