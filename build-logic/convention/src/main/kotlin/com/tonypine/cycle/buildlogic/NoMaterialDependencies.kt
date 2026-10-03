package com.tonypine.cycle.buildlogic

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasUnitTest
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/**
 * Cycle draws its own UI on Compose Foundation (see `core:designsystem`). These groups must never
 * reach a classpath, not even transitively through another library.
 */
private val bannedGroups = setOf("androidx.compose.material", "androidx.compose.material3")

/**
 * Fails when a Material Compose artifact is on the compile, runtime or unit test classpath of any
 * variant. Each variant's `pre<Variant>Build` depends on the check, so every build runs it.
 */
internal fun Project.configureNoMaterialDependencies() {
    val androidComponents = extensions.getByType(AndroidComponentsExtension::class.java)
    val checkAll = tasks.register("checkNoMaterialDependencies") {
        group = "verification"
        description = "Fails if androidx.compose.material or material3 is on any classpath."
    }
    tasks.named("check") { dependsOn(checkAll) }

    androidComponents.onVariants { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val check = tasks.register<CheckNoMaterialDependenciesTask>(
            "check${variantName}NoMaterialDependencies"
        ) {
            group = "verification"
            description = "Fails if androidx.compose.material or material3 is on a ${variant.name} classpath."
            dependencyGraphs.add(variant.compileConfiguration.rootComponent())
            dependencyGraphs.add(variant.runtimeConfiguration.rootComponent())
            (variant as? HasUnitTest)?.unitTest?.let { unitTest ->
                dependencyGraphs.add(unitTest.compileConfiguration.rootComponent())
                dependencyGraphs.add(unitTest.runtimeConfiguration.rootComponent())
            }
        }
        checkAll.configure { dependsOn(check) }
        tasks.matching { it.name == "pre${variantName}Build" }.configureEach { dependsOn(check) }
    }
}

private fun Configuration.rootComponent() = incoming.resolutionResult.rootComponent

abstract class CheckNoMaterialDependenciesTask : DefaultTask() {
    @get:Input
    abstract val dependencyGraphs: ListProperty<ResolvedComponentResult>

    @TaskAction
    fun check() {
        val offenders = dependencyGraphs.get().flatMap(::bannedPaths).toSortedSet()
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Material Compose is not allowed; build UI on core:designsystem instead. Found:\n" +
                    offenders.joinToString("\n") { "  - $it" }
            )
        }
    }

    /** Breadth-first, so each banned module is reported with its shortest path from the root. */
    private fun bannedPaths(root: ResolvedComponentResult): List<String> {
        val paths = mutableListOf<String>()
        val pathTo = mutableMapOf(root to emptyList<String>())
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            val component = queue.removeFirst()
            val path = pathTo.getValue(component)
            val id = component.id
            if (id is ModuleComponentIdentifier && id.group in bannedGroups) {
                paths += path.joinToString(" -> ")
                continue
            }
            component.dependencies
                .filterIsInstance<ResolvedDependencyResult>()
                .filterNot { it.isConstraint }
                .map { it.selected }
                .filter { it !in pathTo }
                .forEach {
                    pathTo[it] = path + it.id.displayName
                    queue.addLast(it)
                }
        }
        return paths
    }
}
