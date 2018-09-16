apply(from = rootProject.file("cmd.gradle.kts"))
dependencies {
    compile(project(":multimc")) {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.module")
        exclude(group = "com.fasterxml.jackson.dataformat")
    }
    compile(group = "commons-codec", name = "commons-codec", version = "+")
}
