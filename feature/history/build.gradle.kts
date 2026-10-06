plugins {
    alias(libs.plugins.cycle.android.library)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

// History: her typical cycle, her cycles newest first, and each cycle's details.
android {
    namespace = "com.tonypine.cycle.feature.history"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.data)
    // The bleeding counts on each method's card.
    implementation(projects.core.domain)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // The state tests derive her overview from a synthetic log with the real calculator, and the
    // ViewModel tests run the real repositories on an in-memory database and a DataStore file.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.kotlinx.coroutines.test)
}
