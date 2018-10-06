object Env {
    val branch = System.getenv("GIT_BRANCH")
        ?.takeUnless { it == "master" }
        ?.let { "-$it" }
        ?: ""

    val versionSuffix = System.getenv("BUILD_NUMBER") ?: "dev"
}