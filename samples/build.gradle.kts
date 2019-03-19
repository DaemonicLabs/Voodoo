import voodoo.LocalVoodooJarTask

plugins {
    id("voodoo") version "0.4.6-dev"
}

voodoo {
    local = true
    addTask(name = "build", parameters = listOf("build"))
    addTask(name = "import_debug", parameters = listOf("import_debug"))
    addTask(name = "pack_sk", parameters = listOf("pack sk"))
    addTask(name = "pack_mmc", parameters = listOf("pack mmc"))
    addTask(name = "pack_mmc-static", parameters = listOf("pack mmc-static"))
    addTask(name = "pack_mmc-fat", parameters = listOf("pack mmc-fat"))
    addTask(name = "pack_server", parameters = listOf("pack server"))
    addTask(name = "pack_curse", parameters = listOf("pack curse"))
    addTask(name = "test_mmc", parameters = listOf("test mmc"))
    addTask(name = "buildAndPackAll", parameters = listOf("build", "pack sk", "pack server", "pack mmc"))
}

repositories {
    mavenLocal()
//    maven(url = "http://maven.modmuss50.me/") {
//        name = "modmuss50"
//    }
    maven(url = "https://kotlin.bintray.com/kotlinx") {
        name = "kotlinx"
    }
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(group = "moe.nikky.voodoo", name = "dsl", version = "0.4.6-dev")
    implementation(group = "moe.nikky.voodoo", name = "voodoo", version = "0.4.6-dev")
}
