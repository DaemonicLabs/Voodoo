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
    compile(group = "com.squareup", name = "kotlinpoet", version = "1.0.0-RC1")
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("constGenerator") {
            id = "constantsGenerator"
            implementationClass = "plugin.GeneratorPlugin"
        }
    }
}