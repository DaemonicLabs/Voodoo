apply(from = rootProject.file("cmd.gradle.kts"))
dependencies {
    compile(project(":builder")) {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.module")
        exclude(group = "com.fasterxml.jackson.dataformat")
    }
}
