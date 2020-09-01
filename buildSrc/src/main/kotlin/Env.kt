object Env {
    // TODOL get branch
    val branch = System.getenv("GIT_BRANCH")
        ?.takeUnless { it == "master" }
        ?.let { "-$it" }
        ?: ""

    val isCI = System.getenv("CI") != null
}