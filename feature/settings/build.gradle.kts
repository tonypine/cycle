plugins {
    alias(libs.plugins.cycle.android.library)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

// Settings: for now the tab's placeholder with "What to log", where she shows or hides each day log
// category. The rest of Settings lands with MOT-40.
android {
    namespace = "com.tonypine.cycle.feature.settings"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // The ViewModel tests run the real settings on a DataStore file.
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.kotlinx.coroutines.test)
}
