import com.android.build.api.dsl.ApplicationExtension
import com.tonypine.cycle.buildlogic.configureKotlinAndroid
import com.tonypine.cycle.buildlogic.configureNoMaterialDependencies
import com.tonypine.cycle.buildlogic.libs
import com.tonypine.cycle.buildlogic.versionOf
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "com.android.application")
        apply(plugin = "cycle.ktlint")

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = libs.versionOf("targetSdk").toInt()
        }
        configureNoMaterialDependencies()
    }
}
