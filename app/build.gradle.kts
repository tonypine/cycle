plugins {
    alias(libs.plugins.cycle.android.application)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

android {
    namespace = "com.tonypine.cycle"

    // Version and release signing come from CI inputs through cycle.android.application; see
    // docs/decisions/0002-release-distribution.md.
    defaultConfig {
        applicationId = "com.tonypine.cycle"
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.data)
    implementation(projects.feature.history)
    implementation(projects.feature.today)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
}
