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
    implementation(group = "com.squareup", name = "kotlinpoet", version = "1.5.0")
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("constGenerator") {
            id = "constantsGenerator"
            implementationClass = "plugin.GeneratorPlugin"
        }
    }
}