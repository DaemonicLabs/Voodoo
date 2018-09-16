apply(from = rootProject.file("cmd.gradle.kts"))
dependencies {
    compile(project(":core")) {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.module")
        exclude(group = "com.fasterxml.jackson.dataformat")
    }
    compile(group = "commons-lang", name = "commons-lang", version = "2.6")
    compile(group = "commons-io", name = "commons-io", version = "2.6")
}