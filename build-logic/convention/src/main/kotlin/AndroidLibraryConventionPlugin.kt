import com.android.build.api.dsl.LibraryExtension
import com.tonypine.cycle.buildlogic.configureKotlinAndroid
import com.tonypine.cycle.buildlogic.configureNoMaterialDependencies
import com.tonypine.cycle.buildlogic.libs
import com.tonypine.cycle.buildlogic.versionOf
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        apply(plugin = "com.android.library")
        apply(plugin = "org.jetbrains.kotlin.android")
        apply(plugin = "cycle.ktlint")

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            testOptions.targetSdk = libs.versionOf("targetSdk").toInt()
            lint.targetSdk = libs.versionOf("targetSdk").toInt()
        }
        configureNoMaterialDependencies()
    }
}
