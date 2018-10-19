plugins {
    wrapper
    id("voodoo") version "0.4.2-dev"
}

voodoo {
//    rootDir =
//    generatedSource = project.file(".src")
//    packDirectory = project.file("packs")
}

// only required for plugin dev
repositories {
    mavenLocal()
}

tasks.withType<Wrapper> {
    gradleVersion = "5.0-milestone-1"
    distributionType = Wrapper.DistributionType.ALL
}