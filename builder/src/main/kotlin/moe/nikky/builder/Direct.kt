package moe.nikky.builder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class DirectProviderThing(override val entry: Entry) : ProviderThingy(entry) {
    override fun validate(): Boolean {
        return entry.url.isNotBlank()
    }

    override val name = "Direct provider"
    fun doDirectThingy() {
        println("doDirectThingy not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
