plugins {
    kotlin("jvm") version "1.3.20-eap-100"
    id("voodoo") version "0.4.5-dev"
    idea
}

voodoo {
    rootDir { project.rootDir.resolve("run") }
    packDirectory { project.rootDir.resolve("packs") }
//    docDirectory { project.rootDir.resolve("docs") }
//    generatedSource { rootDir -> rootDir.resolve(".voodoo") }

    addTask(name = "build", parameters = listOf("build"))
    addTask(name = "pack_sk", parameters = listOf("pack sk"))
    addTask(name = "pack_mmc", parameters = listOf("pack mmc"))
    addTask(name = "pack_mmc-static", parameters = listOf("pack mmc-static"))
    addTask(name = "pack_mmc-fat", parameters = listOf("pack mmc-fat"))
    addTask(name = "pack_server", parameters = listOf("pack server"))
    addTask(name = "pack_curse", parameters = listOf("pack curse"))
    addTask(name = "test_mmc", parameters = listOf("test mmc"))
    addTask(name = "buildAndPackAll", parameters = listOf("build", "pack sk", "pack server", "pack mmc"))
}

// only required for plugin dev
repositories {
    mavenLocal()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap") {
        name = "Kotlin EAP"
    }
    maven(url = "https://kotlin.bintray.com/kotlinx") {
        name = "kotlinx"
    }
    maven(url = "https://jitpack.io") {
        name = "jitpack"
    }
    mavenCentral()
}

dependencies {
    implementation(group = "moe.nikky.voodoo", name = "voodoo", version = "0.4.5-dev")
}
