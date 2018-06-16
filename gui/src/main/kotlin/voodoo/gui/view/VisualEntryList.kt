package voodoo.gui.view

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import tornadofx.*
import voodoo.gui.controller.EntryController
import voodoo.gui.controller.ModpackController
import voodoo.gui.extensions.animatedOptionButton
import voodoo.gui.extensions.jfxlistview
import voodoo.gui.extensions.jfxnodeslist
import voodoo.gui.extensions.mainButton
import voodoo.gui.model.EntryModel
import voodoo.gui.model.EntryWrapper
import voodoo.provider.Provider

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class ViualEntryList : View("Fancy EntryWrapper List") {
    val modpackController: ModpackController by inject()
    val controller: EntryController by inject()
    override val root = stackpane {
        hgrow = Priority.ALWAYS
        jfxlistview(modpackController.selectedModpack.entries) {
            bindSelected(controller.selectedEntry)
            cellFragment(EntryListFragment::class)
        }
        tornadofx.insets(insets.top, 10, 10, insets.left)
        alignment = Pos.BOTTOM_RIGHT
        jfxnodeslist {
            rotate = 180.0
            mainButton(FontAwesomeIcon.PLUS)
            animatedOptionButton(FontAwesomeIcon.CODEPEN)
            animatedOptionButton(FontAwesomeIcon.CODE)
            animatedOptionButton(FontAwesomeIcon.FIRE)
        }
    }
}

class EntryListFragment : ListCellFragment<EntryWrapper>() {
    val entry = EntryModel().bindTo(this)

    override val root = vbox {
        //        style {
//            fillWidth = true
//            spacing = 0.px
//        }
        println(style)
//        fieldset {
//            field("Icon") {
        label(entry.comment) {
            isWrapText = true
            removeWhen {
                entry.provider.booleanBinding { it != Provider.CURSE.name }
                        .or(entry.thumbnail.booleanBinding { it.isNullOrBlank()})
            }
            //cached thumbnail info in EntryWrapper
            graphic = imageview(entry.thumbnail, true) {
                fitHeight = 64.0
                fitWidth = 64.0
                isPreserveRatio = true
            }
        }
//            }
//        enableWhen { entry.enabled }

        // Common
//            field("Name") {
        label(entry.name) {
            style {
                //                        fontSize = 18.px
                fontWeight = FontWeight.BOLD
            }
        }
//            }
//            field("provider") {
        label(entry.provider) {
            //                    alignment = Pos.CENTER_RIGHT
//                    style {
//                        fontSize = 22.px
//                        fontWeight = FontWeight.BOLD
//                    }
        }
//            }
//            field("side") {
        label(entry.side) {
            //                    alignment = Pos.CENTER_RIGHT
//                    style {
//                        fontSize = 22.px
//                        fontWeight = FontWeight.BOLD
//                    }
        }

    }
}
