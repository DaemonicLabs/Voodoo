package voodoo.util

import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.io.StringWriter
import mu.KLogging
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.coroutineContext
import kotlin.math.max
import kotlin.system.exitProcess

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

val pool = newFixedThreadPoolContext(max(8, Runtime.getRuntime().availableProcessors()) + 1, "pool")

