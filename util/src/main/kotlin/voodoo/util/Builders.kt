package voodoo.util

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext

// object ExceptionHelper : KLogging() {
//     val context = CoroutineExceptionHandler { context, exception ->
//         logger.error(exception.message)
//         logger.error { exception }
//         logger.error("Caught $exception")
//         exception.printStackTrace(System.out)
//         exception.printStackTrace()
//         context.cancel(exception)
//     }
// }

val VOODOO_MULTITHREADING =
    System.getenv("VOODOO_MULTITHREADING")?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors()
// val pool = newFixedThreadPoolContext(VOODOO_MULTITHREADING + 1, "pool")

@UseExperimental(ObsoleteCoroutinesApi::class)
inline fun <reified R> withPool(
    name: String = "pool",
    threads: Int = System.getenv("VOODOO_MULTITHREADING")?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors() + 1,
    execute: (pool: ExecutorCoroutineDispatcher) -> R
): R {
    val pool = newFixedThreadPoolContext(threads, name)
    val r = execute(pool)
    pool.close()
    return r
}