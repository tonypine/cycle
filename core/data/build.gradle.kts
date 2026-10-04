import com.android.build.api.dsl.CommonExtension

plugins {
    alias(libs.plugins.cycle.android.library)
    alias(libs.plugins.cycle.robolectric.tests)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Her log and settings on the phone: Room for the day log, DataStore for settings, and the
// repositories that combine them with core:domain. Nothing here talks to the network.
android {
    namespace = "com.tonypine.cycle.core.data"
}

// MigrationTestHelper reads the exported schemas from the test assets.
extensions.configure<CommonExtension> {
    sourceSets.getByName("test").assets.directories.add("$projectDir/schemas")
}

// Every schema version is exported and committed: the migration tests open each one.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.domain)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
