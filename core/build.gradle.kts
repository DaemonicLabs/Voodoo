dependencies {
    compile(project(":util"))
    compile(project(":Jankson"))
    compile(group = "org.apache.commons", name = "commons-compress", version = "+")

    // now included TODO= use gradle substitution
//    compile( group= "com.github.aballano", name= "MnemoniK", version= "+")
}
