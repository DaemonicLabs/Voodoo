plugins {
    id("voodoo") version "0.4.2-dev"
}

voodoo {
//    generatedSource = project.file(".src")
//    packDirectory = project.file("packs")
}

// only required for plugin dev
repositories {
    mavenLocal()
}
