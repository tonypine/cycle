plugins {
    alias(libs.plugins.cycle.jvm.library)
}

// Pure cycle logic: periods and cycles from the day log, typical lengths, estimates and prompts.
dependencies {
    api(projects.core.model)
}
