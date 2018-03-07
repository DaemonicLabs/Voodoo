package voodoo.builder.provider

import mu.KLogging

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class DummyProviderThing : ProviderBase("Dummy provider") {
    companion object: KLogging()

    //    override fun validate(): Boolean {
//        return entry.url.isNotBlank()
//    }
    init {
        register2("finalize", //download
                { it, m ->
                    m.tracker.isProcessed(it.name, "resolveOptional")
                },
                { entry, _ ->
                    entry.internal.done = true
                }
        )
    }
}
