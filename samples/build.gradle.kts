plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.scripting") version "1.4.10"
//    id("moe.nikky.voodoo") version "0.6.0-local"
}

//voodoo {
//    gitRoot {
//        rootDir.parentFile
//    }
//
////    buildLocal = true
////    localVoodooProjectLocation = rootDir.parentFile
//    addTask("build") {
//        build()
//    }
//    addTask("changelog") {
//        changelog()
//    }
//    addTask(name = "pack_voodoo") {
//        pack().voodoo()
//    }
//    addTask(name = "pack_mmc-voodoo") {
//        pack().multimcVoodoo()
//    }
//    addTask(name = "pack_mmc-fat") {
//        pack().multimcFat()
//    }
//    addTask(name = "pack_server") {
//        pack().server()
//    }
//    addTask(name = "pack_curse") {
//        pack().curse()
//    }
//    addTask(name = "test_mmc") {
//        test().multimc()
//    }
//    addTask(name = "buildAndPackAll") {
//        build()
//        pack().server()
//        pack().multimcFat()
//        pack().curse()
//        pack().voodoo()
//        pack().multimcVoodoo()
//    }
//
//    generateCurseforgeMods("Mod", "1.12", "1.12.1", "1.12.2")
//    generateCurseforgeMods("FabricMod", "1.15", "1.15.1", "1.15.2", categories = listOf("Fabric"))
//    generateCurseforgeResourcepacks("TexturePack", "1.12", "1.12.1", "1.12.2")
//    generateForge("Forge_12_2", "1.12.2")
//    generateForge("Forge_15_2", "1.15.2")
//    generateFabric("Fabric", true)
//}

repositories {
    mavenLocal()
//    maven(url = "http://maven.modmuss50.me/") {
//        name = "modmuss50"
//    }
//    maven(url = "https://kotlin.bintray.com/kotlinx") {
//        name = "kotlinx"
//    }
//    mavenCentral()
//    jcenter()
}

dependencies {
    kotlinScriptDef(group = "moe.nikky.voodoo", name = "voodoo", version = "0.6.0-local")
    kotlinScriptDef(group = "moe.nikky.voodoo", name = "dsl", version = "0.6.0-local")
//    implementation(group = "moe.nikky.voodoo", name = "voodoo", version = "0.6.0-local")
//    implementation(group = "moe.nikky.voodoo", name = "dsl", version = "0.6.0-local")

}