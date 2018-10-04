plugins {
    `kotlin-dsl`
    idea
    kotlin("jvm") version "1.2.61"
}

repositories {
    jcenter()
}

dependencies {
    compile(group = "com.squareup", name = "kotlinpoet", version = "1.0.0-RC1")
}