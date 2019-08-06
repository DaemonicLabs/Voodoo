import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

plugins {
    `maven-publish`
    constantsGenerator
    application
}

configure<ConstantsExtension> {
    constantsObject(
        pkg = "voodoo.bootstrap",
        className = "Config"
    ) {
        field("MAVEN_ARTIFACT") value "unset"
    }
}

val shadowJar by tasks.getting(ShadowJar::class) {
    archiveClassifier.set("")
    archiveVersion.set("")
}

//application {
//    mainClassName = "voodoo.BootstrapKt"
//}

val generateConstants by tasks.getting

val shadowJarVoodoo by tasks.creating(ShadowJar::class) {
    group = "shadow"
    dependsOn(generateConstants)
    doFirst {
        generateBootstrapConfig(
            fields = mapOf("MAVEN_ARTIFACT" to "voodooo"),
            outputFolder = buildDir.resolve("generated-src")
        )
    }
    val jar = tasks.getByName<Jar>("jar")
    manifest.inheritFrom(jar.manifest)
    from(sourceSets.getAt("main").output)
    val runtimeClasspath = project.configurations.findByName("runtimeClasspath")
    configurations = listOf(if(runtimeClasspath != null) project.configurations.runtimeClasspath.get() else project.configurations.runtime.get())
    exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")

    manifest {
        attributes("MainClass" to application.mainClassName)
    }

    archiveBaseName.set("bootstrap")
    archiveClassifier.set("voodoo")
//    archiveVersion.set("")
}
val shadowJarMultimcInstaller by tasks.creating(ShadowJar::class) {
    group = "shadow"
    dependsOn(generateConstants)
    doFirst {
        generateBootstrapConfig(
            fields = mapOf("MAVEN_ARTIFACT" to "multimc-installer"),
            outputFolder = buildDir.resolve("generated-src")
        )
    }
    val jar = tasks.getByName<Jar>("jar")
    manifest.inheritFrom(jar.manifest)
    from(sourceSets.getAt("main").output)
    val runtimeClasspath = project.configurations.findByName("runtimeClasspath")
    configurations = listOf(if(runtimeClasspath != null) project.configurations.runtimeClasspath.get() else project.configurations.runtime.get())
    exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")

    manifest {
        attributes("MainClass" to application.mainClassName)
    }

    archiveBaseName.set("bootstrap")
    archiveClassifier.set("multimc-installer")
//    archiveVersion.set("")
}

val build by tasks.getting(Task::class) {
    dependsOn(shadowJarVoodoo)
    dependsOn(shadowJarMultimcInstaller)
}

publishing {
    publications {
        create("bootstrapShadow", MavenPublication::class.java) {
            artifact(shadowJarVoodoo)
            artifact(shadowJarMultimcInstaller)
        }
    }
}

fun generateBootstrapConfig(
    fields: Map<String, Any>,
    pkg: String = "voodoo.bootstrap",
    outputFolder: File
) {
    val constantBuilder = TypeSpec.objectBuilder(ClassName(pkg, "Config"))

    fields.forEach { (key, value) ->
        when (value) {
            is String -> {
                constantBuilder.addProperty(
                    PropertySpec.builder(
                        key,
                        String::class,
                        KModifier.CONST
                    )
                        .initializer("%S", value)
                        .build()
                )
            }
            is Int -> {
                constantBuilder.addProperty(
                    PropertySpec.builder(
                        key,
                        Int::class,
                        KModifier.CONST
                    )
                        .initializer("%L", value)
                        .build()
                )
            }
        }
    }

    val source = FileSpec.get(pkg, constantBuilder.build())
    source.writeTo(outputFolder)
}
