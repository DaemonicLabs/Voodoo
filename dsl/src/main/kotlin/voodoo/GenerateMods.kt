package voodoo

@Repeatable
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateMods(
    val name: String,
    val mc: String
//    val mcVersions: Array<String>,
//    val ids: IntArray = [],
//    val section: Section
)