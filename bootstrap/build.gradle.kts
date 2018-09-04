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
    compile(project(":base:cmd"))
    compile(project(":util"))
}

val shadowJar by tasks.getting(ShadowJar::class) {
    classifier = ""
}

val build by tasks.getting(Task::class) {
    dependsOn(shadowJar)
}

var moduleName = ""
var fileRegex = ""

if (project.hasProperty("target")) {
    val target: String by properties
    val (_moduleName, _fileRegex, baseName) = when (target) {
        "voodoo" -> Triple(
                "voodoo",
                """^[Vv]oodoo(-[^-]*)?(?!-fat)\\.jar$""",
                "bootstrap-voodoo"
        )
        "hex" -> Triple(
                "hex",
                """^hex(-[^-]*)?(?!-fat)\\.jar$""",
                "bootstrap-hex"
        )
        else -> throw InvalidUserDataException("invalid target property")
    }
    moduleName = _moduleName
    fileRegex = _fileRegex
    shadowJar.baseName = baseName
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
