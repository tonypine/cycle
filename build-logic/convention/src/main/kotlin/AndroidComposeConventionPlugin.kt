import com.android.build.api.dsl.CommonExtension
import com.tonypine.cycle.buildlogic.library
import com.tonypine.cycle.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
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
            // Renders @Preview in Android Studio. Debug only, and without its Material dependency.
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
            components.withModule("androidx.compose.ui:ui-tooling-android", DropToolingMaterial::class.java)
        }
    }
}

/**
 * ui-tooling declares a dependency on material3 (which brings material-ripple), but none of its
 * classes reference Material: previews render through `ComposeViewAdapter`, which uses Compose UI
 * only. This rule removes that one declared dependency, so `checkNoMaterialDependencies` needs no
 * exception and still fails if anything else brings Material in.
 */
abstract class DropToolingMaterial : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        context.details.allVariants {
            withDependencies {
                removeAll { it.group == "androidx.compose.material3" || it.group == "androidx.compose.material" }
            }
        }
    }
}
