val vcs = "https://github.com/DaemonicLabs/Voodoo"

configure<PublishingExtension> {
    publications.withType<MavenPublication> {
        logger.lifecycle("configuring publication '$name'")
        pom {
            name.set(project.name)
            description.set(project.description)
            url.set(vcs)
            licenses {
                license {
                    name.set("MIT License")
                    url.set("http://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("NikkyAi")
                    name.set("Nikky")
                }
            }
            scm {
                connection.set("$vcs.git")
                developerConnection.set("$vcs.git")
                url.set(vcs)
            }
        }
    }
}