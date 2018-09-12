package voodoo.util

import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import mu.KLogging
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.coroutineContext
import kotlin.system.exitProcess

object ExceptionHelper : KLogging() {
    val context = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
        logger.error(exception.message)
        logger.error("Caught $exception")
    }
}

val pool = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() + 1, "pool")

fun <T, R> T.runBlockingWith(context: CoroutineContext = EmptyCoroutineContext, block: suspend T.(CoroutineContext) -> R): R {
    return kotlinx.coroutines.experimental.runBlocking(context = context + ExceptionHelper.context) {
        try {
        block(coroutineContext)
        } catch (e: Exception) {
            e.printStackTrace()
            coroutineContext.cancel(e)
            exitProcess(1)
        }
    }
}
