dependencies {
    compile(project(":util"))
//    compile(project(":Jankson"))
    compile(group = "com.github.Falkreon", name = "Jankson", version = "master-SNAPSHOT")
    compile(group = "org.apache.commons", name = "commons-compress", version = "1.18")

    // now included TODO= use gradle substitution
//    compile( group= "com.github.aballano", name= "MnemoniK", version= "+")
}
