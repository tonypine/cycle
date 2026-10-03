import com.android.build.api.dsl.CommonExtension
import com.tonypine.cycle.buildlogic.library
import com.tonypine.cycle.buildlogic.libs
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * JVM-only Compose tests: Robolectric runs the UI and Roborazzi records or verifies screenshots.
 * Reference images live in `src/test/screenshots` and are committed.
 */
class ScreenshotTestsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "io.github.takahirom.roborazzi")

        extensions.getByType(CommonExtension::class.java).apply {
            testOptions.unitTests.isIncludeAndroidResources = true
            testOptions.unitTests.all { test ->
                test.systemProperty("robolectric.graphicsMode", "NATIVE")
            }
        }

        extensions.configure<RoborazziExtension> {
            outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
        }

        dependencies {
            add("testImplementation", libs.library("junit"))
            add("testImplementation", libs.library("robolectric"))
            add("testImplementation", libs.library("androidx-test-ext-junit"))
            add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
            add("testImplementation", libs.library("roborazzi"))
            add("testImplementation", libs.library("roborazzi-compose"))
            add("testImplementation", libs.library("roborazzi-junit-rule"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
        }
    }
}
