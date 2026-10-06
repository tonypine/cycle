plugins {
    alias(libs.plugins.cycle.android.library)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

// Settings: her usual lengths, "What to log", her data (Lock Cycle, export, import, delete everything) and
// about. Files go only where she picks, through Android's save screen and file picker.
android {
    namespace = "com.tonypine.cycle.feature.settings"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // The ViewModel tests run the real repositories on an in-memory database and a DataStore file.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.kotlinx.coroutines.test)
    // The copy check in every language, and rendering in one (core:testing).
    testImplementation(projects.core.testing)
}
