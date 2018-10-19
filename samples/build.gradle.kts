plugins {
    wrapper
    id("voodoo") version "0.4.2-dev"
}

voodoo {
    rootDir = project.rootDir.resolve("run")
    packDirectory = { rootDir -> rootDir.resolve("packs") }
//    generatedSource = { rootDir -> rootDir.resolve(".voodoo") }
}

// only required for plugin dev
repositories {
    mavenLocal()
}

tasks.withType<Wrapper> {
    gradleVersion = "5.0-milestone-1"
    distributionType = Wrapper.DistributionType.ALL
}