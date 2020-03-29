plugins {
//    kotlin("plugin.scripting") version "1.3.70"
    id("voodoo") version "0.5.0-dev"
}

voodoo {
    local = true
    addTask("build") {
        build()
    }
    addTask(name = "import_debug") {
        importDebug()
    }
    addTask(name = "pack_sk") {
        pack().sklauncher()
    }
    addTask(name = "pack_mmc") {
        pack().multimc()
    }
    addTask(name = "pack_mmc-static") {
        pack().multimcStatic()
    }
    addTask(name = "pack_mmc-fat") {
        pack().multimcFat()
    }
    addTask(name = "pack_server") {
        pack().server()
    }
    addTask(name = "pack_curse") {
        pack().curse()
    }
    addTask(name = "test_mmc") {
        test().multimc()
    }
    addTask(name = "buildAndPackSome") {
        build()
        pack().sklauncher()
        pack().server()
        pack().multimcFat()
        pack().curse()
    }
    addTask(name = "buildAndPackAll") {
        build()
        pack().sklauncher()
        pack().server()
        test().multimc()
        pack().multimcFat()
        pack().curse()
    }

    generateCurseforgeMods("Mod", "1.12", "1.12.1", "1.12.2")
    generateCurseforgeTexturepacks("TexturePack", "1.12", "1.12.1", "1.12.2")
    generateForge("Forge_12_2", "1.12.2")
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
    implementation(group = "moe.nikky.voodoo", name = "dsl", version = "0.5.0-dev")
    implementation(group = "moe.nikky.voodoo", name = "voodoo", version = "0.5.0-dev")
    kotlinScriptDef(group = "moe.nikky.voodoo", name = "dsl", version = "0.5.0-dev")
//    kotlinScriptDef(group = "moe.nikky.voodoo", name = "voodoo", version = "0.5.0-dev")
}
