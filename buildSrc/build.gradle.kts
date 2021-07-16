plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleKotlinDsl())
    implementation("com.squareup:kotlinpoet:_")
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("constantsGenerator") {
            id = "moe.nikky.plugin.constants"
            implementationClass = "plugin.GeneratorPlugin"
        }
    }
}