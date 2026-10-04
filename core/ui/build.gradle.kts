plugins {
    alias(libs.plugins.cycle.android.library)
    alias(libs.plugins.cycle.android.compose)
}

// Shared app-level UI built on core:designsystem. Empty until the first feature needs it.
android {
    namespace = "com.tonypine.cycle.core.ui"
}

dependencies {
    api(projects.core.designsystem)
}
