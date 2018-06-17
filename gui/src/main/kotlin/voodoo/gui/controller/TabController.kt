package voodoo.gui.controller

import javafx.beans.property.SimpleListProperty
import javafx.scene.control.SingleSelectionModel
import javafx.scene.control.Tab
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
class TabController : Controller() {
    lateinit var selectionModel : SingleSelectionModel<Tab>

    init {

    }
}