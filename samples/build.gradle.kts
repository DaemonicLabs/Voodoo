plugins {
    id("voodoo") version "0.4.9-dev"
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
    kotlinScriptDef(group = "moe.nikky.voodoo", name = "dsl", version = "0.4.8-dev")
    kotlinScriptDef(group = "moe.nikky.voodoo", name = "voodoo", version = "0.4.8-dev")
}
