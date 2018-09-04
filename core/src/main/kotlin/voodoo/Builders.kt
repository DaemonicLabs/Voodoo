package voodoo

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.system.exitProcess

val exceptionHandler = CoroutineExceptionHandler { _, exception ->
    exception.printStackTrace()
    println("Caught $exception")
}

val pool = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() + 1, "pool")

fun <T, R> T.runBlockingWith(context: CoroutineContext = EmptyCoroutineContext, block: suspend T.(CoroutineContext) -> R): R {
    return kotlinx.coroutines.runBlocking(context = context + exceptionHandler) {
        try {
        block(coroutineContext)
        } catch (e: Exception) {
            e.printStackTrace()
            coroutineContext.cancel(e)
            exitProcess(1)
        }
    }
}
