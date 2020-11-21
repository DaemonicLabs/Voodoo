package voodoo.dsl.annotations

@Repeatable
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateForge(
    val name: String,
    val mc: String
//    val mcVersions: Array<String> = []
)