package voodoo.gui.view

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import tornadofx.*
import voodoo.data.Side
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

        //cached thumbnail info in EntryWrapper
//        fieldset {
//            field("Icon") {
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

        label(entry.comment) {
            isWrapText = true
            removeWhen {
                entry.comment.isNotBlank().not()
            }
        }
        imageview(entry.thumbnail, true) {
            removeWhen {
                entry.provider.booleanBinding { it == Provider.CURSE.name }
                        .and(entry.thumbnail.booleanBinding { !it.isNullOrBlank() })
                        .not()
            }
            fitHeight = 64.0
            fitWidth = 64.0
            isPreserveRatio = true
        }

        hbox {
            label("Provider: ") {
                style {
                    fontWeight = FontWeight.BOLD
                }
            }
            label(entry.provider) {
                //    alignment = Pos.CENTER_RIGHT
//                    style {
//                        fontSize = 22.px
//                        fontWeight = FontWeight.BOLD
//                    }
            }
        }
        hbox {
            removeWhen { entry.side.booleanBinding { it != Side.BOTH }.not() }
            label("Side: ") {
                style {
                    fontWeight = FontWeight.BOLD
                }
            }
            label(entry.side) {
                alignment = Pos.CENTER_RIGHT
            }

        }

    }
}
