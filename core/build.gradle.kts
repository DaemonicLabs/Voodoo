import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

apply {
    plugin("com.github.johnrengelman.shadow")
}

dependencies {
    compile(project(":util"))
    compile(project(":Jankson"))
    compile(group = "org.apache.commons", name = "commons-compress", version = "+")

    // now included TODO= use gradle substitution
//    compile( group= "com.github.aballano", name= "MnemoniK", version= "+")
}

val shadowJar by tasks.getting(ShadowJar::class) {
    classifier = ""
}
val build by tasks.getting(Task::class) {
    dependsOn(shadowJar)
}