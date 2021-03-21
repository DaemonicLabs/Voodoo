import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContextMap
import mu.KotlinLogging
import org.slf4j.MDC
import java.lang.IllegalArgumentException
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

fun main() {
    runBlocking(MutableMDCContext()) {
        MDC.put("a", "a")
        storeMDC()

        logger.info { "before context switch" }
        launch(MutableMDCContext() + Dispatchers.IO + exceptionHandler()) {
            logger.info { "withContext" }
            withTimeout(1000) {
                delay(10)
//                throw IllegalArgumentException("test")

                logger.info { "withTimeout" }
            }
//            throwsError()
        }
//        val exc = CoroutineExceptionHandler { context, e ->
//            logger.error { "handling exception" }
////            GlobalScope.launch(MutableMDCContext(context[MutableMDCContext.Key]?.contextMap) + Dispatchers.Default) {
////                logger.error(e) { "unhandled exception" }
////            }
//        }
//        launch(Dispatchers.IO + exc) {
//            launch(Dispatchers.Unconfined) {
//
//                delay(100)
//                logger.info { "withException handler" }
//                require(false) { "test exception" }
//            }
//        }
        delay(100)
        logger.info { "after context switch" }

        logger.info { "before call foo" }
        foo()

        delay(1000)
    }

    logger.info { "after runBlocking" }
}

suspend fun throwsError() {

    delay(10)

}


suspend fun foo() {
    logger.info { "before enter foo" }
    MDC.put("fun", "foo")
    withContext(MutableMDCContext()) {

        logger.info { "entered foo" }

        bar()
    }

}

suspend fun bar() {
    MDC.put("fun", "bar")

    logger.info { "entered bar" }

}

fun CoroutineScope.storeMDC() {
    logger.info { "storeMDC" }
    coroutineContext[MutableMDCContext.Key]?.apply {
        contextMap = MDC.getCopyOfContextMap()
    }
}

private fun exceptionHandler() = CoroutineExceptionHandler { context, e ->
    logger.error { "handling exception" }
    runBlocking(MutableMDCContext(context[MutableMDCContext.Key]?.contextMap) + Dispatchers.Default) {
        logger.error(e) { "unhandled exception" }
    }
}

class MutableMDCContext(
    /**
     * The value of [MDC] context map.
     */
    public var contextMap: MDCContextMap = MDC.getCopyOfContextMap()
) : ThreadContextElement<MDCContextMap>, AbstractCoroutineContextElement(Key) {
    /**
     * Key of [MDCContext] in [CoroutineContext].
     */
    companion object Key : CoroutineContext.Key<MutableMDCContext> {
        private val logger = KotlinLogging.logger {}
    }
    /** @suppress */
    override fun updateThreadContext(context: CoroutineContext): MDCContextMap {
        val oldState = MDC.getCopyOfContextMap()
        logger.info { "updateThreadContext oldState=$oldState setCurrent($contextMap)" }
        setCurrent(contextMap)
        return oldState
    }
    /** @suppress */
    override fun restoreThreadContext(context: CoroutineContext, oldState: MDCContextMap) {
        logger.info { "restoreThreadContext setCurrent($oldState)" }
        setCurrent(oldState)
    }
    private fun setCurrent(contextMap: MDCContextMap) {
        if (contextMap == null) {
            MDC.clear()
        } else {
            MDC.setContextMap(contextMap)
        }
    }
}