object Deps {
    const val placeholder = ""
}

// fun Project.pin() {
//    configurations.all {
//        resolutionStrategy.eachDependency {
//            if (requested.group == "org.jetbrains.kotlin") {
//                useVersion(Versions.kotlin)
//            }
//        }
//    }
// }