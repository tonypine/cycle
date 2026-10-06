package com.tonypine.cycle.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.DisableCachingByDefault

/**
 * Lets Robolectric tests run where `$HOME` is read-only, as in the agent sandbox, with no extra
 * environment.
 *
 * Left alone, Robolectric downloads its `android-all` runtime jar into `~/.m2` under a lock file in
 * `$HOME`, and the test JVM writes to the system temp dir. Instead, Gradle resolves the jar into its
 * own cache, Robolectric finds it through a `robolectric-deps.properties` file, and temp files go
 * under the module's `build/tmp`.
 *
 * Registers the resolution once per project and returns the step that points one test task at it.
 */
internal fun Project.gradleResolvedRobolectricRuntime(): (Test) -> Unit {
    // The SDK in robolectric.properties, and Android 10 for the tests that pin `sdk = [29]`. One
    // configuration each: in one, Gradle would keep only the newer version of the same module.
    val androidAll = listOf("robolectric-android-all-instrumented", "robolectric-android-all-instrumented-sdk29")
        .map { alias ->
            configurations.detachedConfiguration(dependencies.create(libs.library(alias).get()))
                .apply { isTransitive = false }
        }

    val writeDeps = tasks.register<WriteRobolectricDepsTask>("writeRobolectricDeps") {
        description = "Maps the android-all jars Gradle resolved for Robolectric to their files."
        androidAll.forEach { configuration ->
            jarsByCoordinates.putAll(
                configuration.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                    artifacts.associate { artifact ->
                        val id = artifact.id.componentIdentifier as ModuleComponentIdentifier
                        "${id.group}:${id.module}:${id.version}" to artifact.file.absolutePath
                    }
                }
            )
        }
        propertiesFile.set(layout.buildDirectory.file("robolectric/robolectric-deps.properties"))
    }

    return { test ->
        test.dependsOn(writeDeps)
        // The jar's content is the input, not its path, so the build cache works across checkouts.
        test.inputs.files(androidAll)
            .withPropertyName("robolectricAndroidAll")
            .withPathSensitivity(PathSensitivity.NONE)
        test.jvmArgumentProviders.add(
            objects.newInstance<RobolectricJvmArgs>().apply {
                depsProperties.set(writeDeps.flatMap { it.propertiesFile })
                tmpDir.set(layout.buildDirectory.dir("tmp/${test.name}"))
            }
        )
    }
}

@DisableCachingByDefault(because = "Writes absolute paths to the jars in the Gradle cache.")
abstract class WriteRobolectricDepsTask : DefaultTask() {
    /** `group:artifact:version`, the key Robolectric's `PropertiesDependencyResolver` looks up. */
    @get:Input
    abstract val jarsByCoordinates: MapProperty<String, String>

    @get:OutputFile
    abstract val propertiesFile: RegularFileProperty

    @TaskAction
    fun write() {
        propertiesFile.get().asFile.writeText(
            jarsByCoordinates.get().toSortedMap().entries.joinToString("") { (coordinates, jar) ->
                "${escape(coordinates)}=${escape(jar)}\n"
            }
        )
    }

    private fun escape(value: String) = value.replace("\\", "\\\\").replace(":", "\\:")
}

/** Absolute paths, so `@Internal`: the test task's real inputs are the jars themselves. */
abstract class RobolectricJvmArgs : CommandLineArgumentProvider {
    @get:Internal
    abstract val depsProperties: RegularFileProperty

    @get:Internal
    abstract val tmpDir: DirectoryProperty

    override fun asArguments(): List<String> {
        val tmp = tmpDir.get().asFile.apply { mkdirs() }
        return listOf(
            "-Drobolectric-deps.properties=${depsProperties.get().asFile.absolutePath}",
            "-Djava.io.tmpdir=${tmp.absolutePath}"
        )
    }
}
