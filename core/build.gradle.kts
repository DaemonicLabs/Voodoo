dependencies {
    compile(project(":util"))
    compile(group = "org.apache.commons", name = "commons-compress", version = "1.18")

    // now included
//    compile( group= "com.github.aballano", name= "MnemoniK", version= "+")

    // apply(from = rootProject.file("cmd.gradle.kts"))
    compile(group = "com.xenomachina", name = "kotlin-argparser", version = Versions.argparser)
}
