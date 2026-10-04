plugins {
    alias(libs.plugins.cycle.android.application)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

android {
    namespace = "com.tonypine.cycle"

    defaultConfig {
        applicationId = "com.tonypine.cycle"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
