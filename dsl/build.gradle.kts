dependencies {
    compile(project(":core:core-dsl"))
    compile(project(":builder"))
    compile(project(":pack"))
    compile(project(":pack:pack-tester"))
    compile(project(":importer"))
    compile(project(":poet"))
    compile(group = "com.squareup", name = "kotlinpoet", version = Versions.poet)
    compile(group = "com.github.holgerbrandl", name = "kscript-annotations", version = "1.+")
}
