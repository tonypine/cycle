import com.android.build.api.dsl.CommonExtension
import com.tonypine.cycle.buildlogic.gradleResolvedRobolectricRuntime
import com.tonypine.cycle.buildlogic.library
import com.tonypine.cycle.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** JVM tests that need the Android framework, such as Room DAO tests, run on Robolectric. */
class RobolectricTestsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val useGradleResolvedRuntime = gradleResolvedRobolectricRuntime()
        extensions.getByType(CommonExtension::class.java).apply {
            testOptions.unitTests.isIncludeAndroidResources = true
            testOptions.unitTests.all { test ->
                // Robolectric sets FileDescriptor internals through the JDK's SharedSecrets.
                test.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
                // No writes to $HOME or the system temp dir, so tests run in the agent sandbox.
                useGradleResolvedRuntime(test)
            }
            // One robolectric.properties for every module, so they all emulate the same SDK.
            sourceSets.getByName("test").resources.srcDir(rootDir.resolve("build-logic/robolectric"))
        }

        dependencies {
            add("testImplementation", libs.library("junit"))
            add("testImplementation", libs.library("robolectric"))
            add("testImplementation", libs.library("androidx-test-ext-junit"))
        }
    }
}
