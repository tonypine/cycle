import com.android.build.api.dsl.Lint
import com.tonypine.cycle.buildlogic.library
import com.tonypine.cycle.buildlogic.libs
import com.tonypine.cycle.buildlogic.versionOf
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * A pure Kotlin module with no Android dependency, such as `core:model`: the compiler keeps Android
 * out of it, and its JUnit tests run without Robolectric.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "com.android.lint")
        apply(plugin = "cycle.ktlint")

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(libs.versionOf("jvmToolchain").toInt())
        }
        extensions.configure<Lint> {
            abortOnError = true
            warningsAsErrors = false
        }

        // The repo-wide checks (`AGENTS.md`, CI) run `testDebugUnitTest`, which a JVM module does not
        // have: without this, they would skip its tests without a word.
        val test = tasks.named("test")
        tasks.register("testDebugUnitTest") {
            group = "verification"
            description = "Runs the unit tests, under the name the Android modules use."
            dependsOn(test)
        }

        dependencies {
            add("testImplementation", libs.library("junit"))
        }
    }
}
