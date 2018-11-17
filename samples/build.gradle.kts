plugins {
    wrapper
    id("voodoo") version "0.4.3-dev"
}

voodoo {
    rootDir = project.rootDir.resolve("run")
    packDirectory { project.rootDir.resolve("packs") }
//    generatedSource = { rootDir -> rootDir.resolve(".voodoo") }

    addTask(name = "build", parameters = listOf("build"))
    addTask(name = "sk", parameters = listOf("pack sk"))
    addTask(name = "server", parameters = listOf("pack server"))
    addTask(name = "buildAndPackAll", parameters = listOf("build", "pack sk", "pack server", "pack mmc"))
}

// only required for plugin dev
repositories {
    mavenLocal()
}

tasks.withType<Wrapper> {
    gradleVersion = "5.0-rc-1"
    distributionType = Wrapper.DistributionType.ALL
}