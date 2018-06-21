package voodoo.gui.view

import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.input.MouseButton
import javafx.scene.layout.Priority
import tornadofx.*
import voodoo.gui.controller.NestedEntryController
import voodoo.gui.controller.NestedModpackController
import voodoo.gui.model.NestedEntryWrapper
import voodoo.provider.Provider

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class VisualEntryList : View("Fancy NestedEntryWrapper TreeView") {
    val modpackController: NestedModpackController by inject()
    val controller: NestedEntryController by inject()
    //    val rootItem: TreeItem<NestedEntryWrapper> = TreeItem(modpackController.selectedModpack.root.value)
    override val root = stackpane {

        hgrow = Priority.ALWAYS
//        this += label(modpackController.selectedModpack.name)
        treeview<NestedEntryWrapper> {
            bindSelected(controller.selectedEntry)
            root = TreeItem(modpackController.selectedRootEntry.item)
            populate { item -> item.value.entries }
            root.isExpanded = true

            modpackController.selectedRootEntry.itemProperty.addListener(ChangeListener { observable, oldValue, newValue ->
                log.info("switch to ${newValue.hashCode()}")
                root = TreeItem(newValue)
                populate { item -> item.value.entries }
                root.isExpanded = true
            })

            cellFormat {
                if (it.entries.size > 0) {
                    textProperty().unbind()
                    text = "SubEntries"
                } else {
                    textProperty().bind(it.nameProperty)
                }

                    if (it.provider == Provider.CURSE.name && it.entries.size == 0 && it.thumbnail.isNotBlank())
                        graphic = imageview(it.thumbnailProperty, true) {
                            removeWhen(it.entriesOverrideProperty)
                            //                            removeWhen {
//                                it.providerProperty.booleanBinding { it == Provider.CURSE.name }
//                                        .and(it.thumbnailProperty.booleanBinding { !it.isNullOrBlank() })
//                                        .not()
//                            }
                            fitHeight = 64.0
                            fitWidth = 64.0
                            isPreserveRatio = true

                            //required to update the picture
                            it.nameProperty.addListener { observable, oldValue, newValue ->
                                refresh()
                            }
                        }

                setOnMouseReleased { event ->
                    if (event.button == MouseButton.PRIMARY) {
                        log.fine("CELL release detected $event")
                        val pickNode = event.pickResult.intersectedNode
                        log.fine("dragging: ${it.name}")

                        var node: Node = pickNode
                        while (node !is SmartTreeCell<*>) {
                            if (node.parent == null) {
                                return@setOnMouseReleased
                            }
                            node = node.parent as Node
                        }
                        val target = node.item as NestedEntryWrapper
                        log.info("target: ${target.name}")
                        log.info("node: $node")
                        if (it != target && target.name.isEmpty()) {
                            log.info("dragged: ${it.name}")
                            it.parent?.let { parent ->
                                if(parent != target) {
                                    parent.entries.remove(it)
                                    it.parent = target
                                    target.entries.add(0, it)
//                                    target.rebuildProvider()
                                }
                            }
                        }
                    }
                }
            }
//
//
//                setOnDragOver {
//                    log.info("drag over $it")
//                }
//                setOnMouseDragOver {
//                    log.info("mouse drag over $it")
//                }
        }

//        setOnDragDropped {
//            log.info("drag dropped $it")
//        }
//
//        setOnDragDone {
//            log.info("drag done $it")
//        }
//
//        setOnDragExited {
//            log.info("drag exited $it")
//        }
//        setOnMouseReleased { event ->
//            if (event.button == MouseButton.PRIMARY) {
//                val dragged = modpackController.draggedEntry.item
//                if (dragged != null) {
//                    log.info("dragged: ${dragged.name}")
//                    log.info("drag detected $event")
//                    if (event.target is SmartTreeCell<*>) {
//                        val cell = event.target as SmartTreeCell<NestedEntryWrapper>
//                        val entry = cell.item
//
//
////                            dragged.parent?.let { parent ->
////                                entry.entries.add(dragged)
////                                parent.entries.remove(dragged)
////                                dragged.parent = entry
////                            }
//                    }
//                }
//                modpackController.draggedEntry.item = null
//            }
//        }


    }
}

//            jfxlistview(modpackController.selectedModpack.entries) {
//                bindSelected(controller.selectedEntry)
//                cellFragment(EntryListFragment::class)
//            }
//            tornadofx.insets(insets.top, 10, 10, insets.left)
//            alignment = Pos.BOTTOM_RIGHT
//            jfxnodeslist {
//                rotate = 180.0
//                mainButton(FontAwesomeIcon.PLUS)
//                animatedOptionButton(FontAwesomeIcon.CODEPEN)
//                animatedOptionButton(FontAwesomeIcon.CODE)
//                animatedOptionButton(FontAwesomeIcon.FIRE)
//            }


//class EntryListFragment : ListCellFragment<FlatEntryWrapper>() {
//    val entry = EntryModel().bindTo(this)
//
//    override val root = vbox {
//        //        style {
////            fillWidth = true
////            spacing = 0.px
////        }
//
//        //cached thumbnail info in FlatEntryWrapper
////        fieldset {
////            field("Icon") {
////            }
////        enableWhen { entry.enabled }
//
//        // Common
////            field("Name") {
//        label(entry.name) {
//            style {
//                //                        fontSize = 18.px
//                fontWeight = FontWeight.BOLD
//            }
//        }
//
//        label(entry.comment) {
//            isWrapText = true
//            removeWhen {
//                entry.comment.isNotBlank().not()
//            }
//        }
//        imageview(entry.thumbnail, true) {
//            removeWhen {
//                entry.provider.booleanBinding { it == Provider.CURSE.name }
//                        .and(entry.thumbnail.booleanBinding { !it.isNullOrBlank() })
//                        .not()
//            }
//            fitHeight = 64.0
//            fitWidth = 64.0
//            isPreserveRatio = true
//        }
//
//        hbox {
//            label("Provider: ") {
//                style {
//                    fontWeight = FontWeight.BOLD
//                }
//            }
//            label(entry.provider) {
//                //    alignment = Pos.CENTER_RIGHT
////                    style {
////                        fontSize = 22.px
////                        fontWeight = FontWeight.BOLD
////                    }
//            }
//        }
//        hbox {
//            removeWhen { entry.side.booleanBinding { it != Side.BOTH }.not() }
//            label("Side: ") {
//                style {
//                    fontWeight = FontWeight.BOLD
//                }
//            }
//            label(entry.side) {
//                alignment = Pos.CENTER_RIGHT
//            }
//
//        }
//
//    }
//}
