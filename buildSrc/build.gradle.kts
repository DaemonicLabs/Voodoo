plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
//    id("org.gradle.kotlin.kotlin-dsl") version "1.0-rc-12"
    idea
}

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    jcenter()
    mavenCentral()
}

dependencies {
    compile(group = "com.squareup", name = "kotlinpoet", version = "1.0.0-RC1")
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
//        register("oodooPoet") {
//            id = "voodoo.poet"
//            implementationClass = "moe.nikky.voodoo.PoetPlugin"
//        }
//        this.create()
        create("constGenerator") {
            id = "constantsGenerator"
            implementationClass = "plugin.GeneratorPlugin"
        }
    }
}