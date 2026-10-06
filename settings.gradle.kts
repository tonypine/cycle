pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "cycle"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":app-catalog")
include(":core:data")
include(":core:designsystem")
include(":core:domain")
include(":core:model")
include(":core:testing")
include(":core:ui")
include(":feature:calendar")
include(":feature:history")
include(":feature:onboarding")
include(":feature:settings")
include(":feature:today")
