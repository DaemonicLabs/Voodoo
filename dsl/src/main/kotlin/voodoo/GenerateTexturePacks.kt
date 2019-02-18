package voodoo

@Repeatable
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateTexturePacks(
    val name: String,
    val mc: String = ""
//    val mc: Array<String>
//    val mcVersions: Array<String>,
//    val categoryIds: IntArray = [],
//    val section: Section
)