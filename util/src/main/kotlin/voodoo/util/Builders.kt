package voodoo.util

import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import mu.KLogging

object ExceptionHelper : KLogging() {
    val context = CoroutineExceptionHandler { context, exception ->
        logger.error(exception.message)
        logger.error { exception }
        logger.error("Caught $exception")
        exception.printStackTrace(System.out)
        exception.printStackTrace()
        context.cancel(exception)
    }
}

val VOODOO_MULTITHREADING = System.getenv("VOODOO_MULTITHREADING")?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors()
val pool = newFixedThreadPoolContext(VOODOO_MULTITHREADING + 1, "pool")
