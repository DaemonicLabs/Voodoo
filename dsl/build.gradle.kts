dependencies {
    compile(project(":core:core-dsl"))
    compile(project(":builder"))
    compile(project(":pack:pack-tester"))
    compile(Poet.dependency)
  //  compile(group = "com.github.holgerbrandl", name = "kscript-annotations", version = "1.+")
}
