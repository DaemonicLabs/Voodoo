package voodoo.util

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import mu.KotlinLogging

// object ExceptionHelper {
// private val logger = KotlinLogging.logger {}
//     val context = CoroutineExceptionHandler { context, exception ->
//         logger.error(exception.message)
//         logger.error { exception }
//         logger.error("Caught $exception")
//         exception.printStackTrace(System.out)
//         exception.printStackTrace()
//         context.cancel(exception)
//     }
// }

@OptIn(ObsoleteCoroutinesApi::class)
inline fun <reified R> withPool(
    name: String = "pool",
    threads: Int = System.getenv("VOODOO_MULTITHREADING")?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors() - 1,
    execute: (pool: ExecutorCoroutineDispatcher) -> R
): R {
    return newFixedThreadPoolContext(threads, name).use { pool ->
        execute(pool)
    }
}