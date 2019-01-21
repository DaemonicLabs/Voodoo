object Env {
    val branch = System.getenv("GIT_BRANCH")
        ?.takeUnless { it == "master" }
        ?.let { "-$it" }
        ?: ""

    val isCI = System.getenv("BUILD_NUMBER") != null
}