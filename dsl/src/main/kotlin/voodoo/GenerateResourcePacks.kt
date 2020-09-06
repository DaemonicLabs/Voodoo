package voodoo

@Repeatable
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateResourcePacks(
    val name: String,
    val mc: String = ""
//    val mc: Array<String>
//    val mcVersions: Array<String>,
//    val ids: IntArray = [],
//    val section: Section
)