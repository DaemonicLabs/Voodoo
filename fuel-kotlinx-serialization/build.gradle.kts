val fuel_version: String by project
val serialization_version: String by project
dependencies {
    compile(Fuel.dependency)
    compile(Serialization.dependency)
}