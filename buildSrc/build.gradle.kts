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
        this.register("voodoo-poet") {
            id = "voodoo-poet"
            implementationClass = "moe.nikky.voodoo.PoetPlugin"
        }
        this.register("const-generator") {
            id = "const-generator"
            implementationClass = "GeneratorPlugin"
        }
    }
}