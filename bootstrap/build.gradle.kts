import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

version = project.version

var artifact = ""
var variant = ""
var fileName = "unconfigured"

if (project.hasProperty("target")) {
    val target: String by project
    val (_artifact, _variant, _baseName) = when (target) {
        "voodoo" -> Triple(
                "voodoo",
                "all",
                "bootstrap-voodoo"
        )
        "multimc-installer" -> Triple(
                "multimc-installer",
                "all",
                "bootstrap-multimc-installer"
        )
        else -> throw InvalidUserDataException("invalid target property")
    }
    artifact = _artifact
    variant = _variant
    fileName = _baseName
}

base {
    archivesBaseName = fileName
}
val shadowJar by tasks.getting(ShadowJar::class) {
    archiveClassifier.set("")
    archiveVersion.set("")
}

val build by tasks.getting(Task::class) {
    dependsOn(shadowJar)
}

configure<ConstantsExtension> {
    constantsObject(
        pkg = "voodoo.bootstrap",
        className = "Config"
    ) {
        field("MAVEN_ARTIFACT") value artifact
    }
}
