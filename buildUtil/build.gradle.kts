plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    idea
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(group = "com.squareup", name = "kotlinpoet", version = "1.6.0")
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("constantsGenerator") {
            id = "moe.nikky.plugin.constants"
            implementationClass = "plugin.GeneratorPlugin"
        }
    }
}