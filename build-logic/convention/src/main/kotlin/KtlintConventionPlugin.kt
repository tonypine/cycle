import com.tonypine.cycle.buildlogic.libs
import com.tonypine.cycle.buildlogic.versionOf
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** ktlint for Kotlin sources and build scripts. Rules come from the root `.editorconfig`. */
class KtlintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")

        extensions.configure<KtlintExtension> {
            version.set(libs.versionOf("ktlint"))
            ignoreFailures.set(false)
            filter {
                exclude { it.file.path.contains("/build/") }
            }
        }
    }
}
