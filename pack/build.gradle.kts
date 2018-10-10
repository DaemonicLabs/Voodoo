dependencies {
    compile(project(":multimc"))
    compile(project(":builder"))
    compile(project(":skcraft"))

    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-html-jvm", version = Versions.html)
}
