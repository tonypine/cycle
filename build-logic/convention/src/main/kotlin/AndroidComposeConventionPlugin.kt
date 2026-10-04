import com.android.build.api.dsl.CommonExtension
import com.tonypine.cycle.buildlogic.library
import com.tonypine.cycle.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** Turns on Compose for an Android module that already applies an application or library plugin. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "org.jetbrains.kotlin.plugin.compose")

        extensions.getByType(CommonExtension::class.java).apply {
            buildFeatures.compose = true
        }

        dependencies {
            val bom = platform(libs.library("androidx-compose-bom"))
            add("implementation", bom)
            add("testImplementation", bom)
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            // Not ui-tooling: it depends on androidx.compose.material, which the build forbids.
        }
    }
}
