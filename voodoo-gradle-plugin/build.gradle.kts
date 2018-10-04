plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("myPlugin") {
            id = "my-plugin"
            implementationClass = "plugin.MyPlugin"
        }
    }
}