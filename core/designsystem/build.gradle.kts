plugins {
    alias(libs.plugins.cycle.android.library)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

android {
    namespace = "com.tonypine.cycle.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    // RoundedPolygon and Morph, for shape morphing. Compose Foundation has no equivalent, and the
    // library depends only on Kotlin, collection and core, never Material.
    api(libs.androidx.graphics.shapes)
    // PredictiveBackHandler, for the bottom sheet's predictive back. Compose UI has no back API of
    // its own, and activity-compose brings no Material.
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.roborazzi.accessibility.check)
    // The copy check in every language, and rendering in one (core:testing).
    testImplementation(projects.core.testing)
}
