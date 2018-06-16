package voodoo.gui.controller

import javafx.beans.property.SimpleListProperty
import tornadofx.*
import voodoo.data.flat.ModPack
import voodoo.gui.model.ModpackModel
import voodoo.gui.model.ModpackWrapper
import voodoo.util.readJson
import java.io.File

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class ModpackController : Controller() {
    val modpacks = SimpleListProperty<ModpackWrapper>(observableList())

    val selectedModpack = ModpackModel()

    init {
        // iterate though json files and try to load them
        val path = System.getProperty("user.dir")
        val files = File(path).listFiles({ dir, name ->
                    name.endsWith(".json")
                            && !name.endsWith(".lock.json")
                            && !name.endsWith(".features.json")
                            && !name.endsWith(".versions.json")
        })
        files.forEach {
            log.info("try loading $it")
            try {
                val modpack = it.readJson<ModPack>()
                modpacks.add(ModpackWrapper(modpack))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        modpacks.firstOrNull()?.apply {
            selectedModpack.item = this
        }


    }
}