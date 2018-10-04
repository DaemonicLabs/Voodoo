import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
}

apply {
    plugin("com.github.johnrengelman.shadow")
}

application {
    mainClassName = "voodoo.BootstrapKt"
}
version = project.version

dependencies {
    compile(project(":util"))
}

var moduleName = ""
var fileRegex = ""
var fileName = "unconfigured"

if (project.hasProperty("target")) {
    val target: String by project
    val (_moduleName, _fileRegex, _baseName) = when (target) {
        "voodoo" -> Triple(
                "voodoo",
                """^[Vv]oodoo(-[^-]*)?(?!-fat)\\.jar$""",
                "bootstrap-voodoo"
        )
        "multimc-installer" -> Triple(
                "multimc-installer",
                """^multimc-installer(-[^-]*)?(?!-fat)\\.jar$""",
                "bootstrap-multimc-installer"
        )
        else -> throw InvalidUserDataException("invalid target property")
    }
    moduleName = _moduleName
    fileRegex = _fileRegex
    fileName = _baseName
}

val buildNumber = System.getenv("BUILD_NUMBER")?.let { "-$it" } ?: ""
base {
    archivesBaseName = "$fileName$buildNumber"
}
val shadowJar by tasks.getting(ShadowJar::class) {
    classifier = ""
    archiveName = "$baseName.$extension"
}

val build by tasks.getting(Task::class) {
    dependsOn(shadowJar)
}

val compileKotlin by tasks.getting(KotlinCompile::class) {
    doFirst {
        copy {
            from("src/template/kotlin/voodoo/")
            into("$buildDir/generated-src/voodoo/")
            expand(
                    mapOf(
                            "JENKINS_URL" to "https://ci.elytradev.com",
                            "JENKINS_JOB" to "elytra/Voodoo/rewrite",
                            "MODULE_NAME" to moduleName,
                            "FILE_REGEX" to fileRegex
                    )
            )
        }
    }
}
