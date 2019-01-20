// SPEK

repositories {
    maven(url = "https://dl.bintray.com/spekframework/spek-dev")
}

val cleanTest by tasks.getting(Delete::class)

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("spek2")
    }
    dependsOn(cleanTest)
}