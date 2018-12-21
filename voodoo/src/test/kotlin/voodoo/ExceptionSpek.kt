//package voodoo
//
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.runBlocking
//import org.spekframework.spek2.Spek
//import org.spekframework.spek2.style.specification.describe
//import kotlin.test.assertFailsWith
//
//object ExceptionSpek : Spek({
//    describe("throw exception") {
//        it("in runBlocking") {
//            assertFailsWith(IllegalStateException::class) {
//                suspend fun test() {
//                    throw IllegalStateException("Excpected Exception")
//                }
//                runBlocking {
//                    (0..100).forEach {
//                        launch {
//                            delay(1000)
//                            println(a"finished")
//                        }
//                    }
//                    val job = launch {
//                        test()
//                        assert(false)
//                    }
//
//                    job.join()
//                }
//            }
//        }
//    }
//})