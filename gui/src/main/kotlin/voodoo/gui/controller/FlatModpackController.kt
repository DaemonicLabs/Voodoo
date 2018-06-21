//package voodoo.gui.controller
//
//import javafx.beans.property.SimpleListProperty
//import tornadofx.*
//import voodoo.data.flat.ModPack
//import voodoo.gui.model.FlatModpackModel
//import voodoo.gui.model.FlatModpackWrapper
//import voodoo.util.readJson
//import java.io.File
//
///**
// * Created by nikky on 18/03/18.
// * @author Nikky
// * @version 1.0
// */
//class FlatModpackController : Controller() {
//    val tabController: TabController by inject()
//
//    val modpacks = SimpleListProperty<FlatModpackWrapper>(observableList())
//
//    val selectedModpack = FlatModpackModel()
//
//    init {
//        // iterate though json files and try to load them
//        val path = System.getProperty("user.dir")
//        val files = File(path).listFiles({ dir, name ->
//            name.endsWith(".json")
//                    && !name.endsWith(".lock.json")
//                    && !name.endsWith(".features.json")
//                    && !name.endsWith(".versions.json")
//        })
//        files.forEach {
//            log.info("try loading $it")
//            try {
//                val modpack = it.readJson<ModPack>()
//                modpacks.add(FlatModpackWrapper(modpack))
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//
//
//        modpacks.firstOrNull()?.apply {
//            selectedModpack.item = this
//        }
//
//        selectedModpack.itemProperty.addListener(ChangeListener { observable, oldValue, newValue ->
//            tabController.selectionModel.select(0)
//        })
//
//    }
//}