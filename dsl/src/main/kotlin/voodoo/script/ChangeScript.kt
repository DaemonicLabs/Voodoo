package voodoo.script


import voodoo.changelog.ChangelogBuilder
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Voodoo Changelog Configuration",
    fileExtension = "changelog.kts",
    compilationConfiguration = ChangeScriptConfiguration::class
)
open class ChangeScript {
     lateinit var builder : ChangelogBuilder

    fun getBuilderOrNull(): ChangelogBuilder? {
        return if(::builder.isInitialized) {
            builder
        } else {
            null
        }
    }
}