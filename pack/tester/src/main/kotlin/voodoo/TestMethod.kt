package voodoo

import voodoo.tester.AbstractTester
import voodoo.tester.MultiMCTester

abstract class TestMethod(val key: String) {
    open val clean: Boolean = false
    abstract val tester: AbstractTester

    class MultiMC(override val clean: Boolean = false) : TestMethod("mmc") {
        override val tester: AbstractTester = MultiMCTester
    }
}