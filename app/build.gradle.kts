plugins {
    alias(libs.plugins.cycle.android.application)
    alias(libs.plugins.cycle.android.compose)
    alias(libs.plugins.cycle.screenshot.tests)
}

// Release builds take their version and signing key from CI; see docs/decisions/0002-release-distribution.md.
// Without these inputs, local and PR builds keep the defaults below and the release APK stays unsigned.
val versionMajorMinor = "0.1"
val ciVersionCode =
    providers.gradleProperty("cycle.versionCode").map {
        requireNotNull(it.toIntOrNull()?.takeIf { code -> code > 0 }) {
            "cycle.versionCode must be a positive integer, got '$it'"
        }
    }
val ciVersionName =
    providers.gradleProperty("cycle.versionName").orElse(ciVersionCode.map { "$versionMajorMinor.$it" })

val releaseSigningInputs =
    listOf("RELEASE_KEYSTORE_PATH", "RELEASE_KEYSTORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
        .associateWith { providers.environmentVariable(it).orNull?.takeIf(String::isNotBlank) }
val missingReleaseSigningInputs = releaseSigningInputs.filterValues { it == null }.keys
check(missingReleaseSigningInputs.size in setOf(0, releaseSigningInputs.size)) {
    "Release signing needs all of ${releaseSigningInputs.keys}; missing $missingReleaseSigningInputs"
}

android {
    namespace = "com.tonypine.cycle"

    defaultConfig {
        applicationId = "com.tonypine.cycle"
        versionCode = ciVersionCode.getOrElse(1)
        versionName = ciVersionName.getOrElse("$versionMajorMinor.0")
    }

    signingConfigs {
        if (missingReleaseSigningInputs.isEmpty()) {
            create("release") {
                storeFile = file(releaseSigningInputs.getValue("RELEASE_KEYSTORE_PATH")!!)
                storePassword = releaseSigningInputs.getValue("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = releaseSigningInputs.getValue("RELEASE_KEY_ALIAS")
                keyPassword = releaseSigningInputs.getValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
