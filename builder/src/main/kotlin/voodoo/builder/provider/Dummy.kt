package voodoo.builder.provider

import voodoo.builder.ProviderThingy
import mu.KLogging
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class DummyProviderThing : ProviderThingy() {
    companion object: KLogging()
    override val name = "Dummy provider"

    //    override fun validate(): Boolean {
//        return entry.url.isNotBlank()
//    }
    init {
        register("finalze", //download
                {
                    it.resolvedOptionals
                },
                { entry, m ->
                    entry.done = true
                }
        )
    }
}
