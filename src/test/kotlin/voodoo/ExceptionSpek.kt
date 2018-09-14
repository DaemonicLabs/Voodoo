package voodoo

import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.util.ExceptionHelper
import kotlin.coroutines.experimental.coroutineContext
import kotlin.test.*

object ExceptionSpek : Spek({
    describe("throw exception") {
        it("in runBlocking") {
            assertFailsWith(IllegalStateException::class) {
                suspend fun test() {
                    throw IllegalStateException("Excpected Exception")
                }
                try {
                    runBlocking {
                        (0..100).forEach {
                            launch {
                                delay(1000)
                                println("finished")
                            }
                        }
                        val job = launch {
                            test()
                            assert(false)
                        }

                        job.join()
                    }
                }catch(e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }

        }
    }
})