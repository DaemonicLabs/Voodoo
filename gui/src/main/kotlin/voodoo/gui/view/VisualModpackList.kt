package voodoo.gui.view

import com.jfoenix.controls.JFXDialog
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.control.Label
import voodoo.gui.model.FlatModpackWrapper
import voodoo.gui.model.FlatModpackModel
import tornadofx.*
import voodoo.gui.controller.NestedModpackController
import voodoo.gui.extensions.*
import voodoo.gui.model.NestedModpackModel
import voodoo.gui.model.NestedModpackWrapper

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class VisualModpackList : View("Fancy FlatModpackWrapper List") {
    val controller: NestedModpackController by inject()
    override val root = stackpane {
        jfxlistview(controller.modpacks) {
            bindSelected(controller.selectedModpack)
            cellFragment(ModpackListFragment::class)
        }

        tornadofx.insets(insets.top, 10, 10, insets.left)
        alignment = Pos.BOTTOM_RIGHT
        val dialog = jfxdialog {
            transitionType = JFXDialog.DialogTransition.CENTER
            content = jfxdialoglayout {
                prefWidth = 1400.0
                prefHeight = 800.0
                setHeading(Label("test"))
                setBody(Label("lorem ipsum"))
                setActions(jfxbutton("ok") {
                    action {
                        this@jfxdialog.close()
                    }
                })
            }
        }

        jfxnodeslist {
            rotate = 180.0
            mainButton(FontAwesomeIcon.PLUS)
            animatedOptionButton(FontAwesomeIcon.CODEPEN) {
                action {
                    dialog.show(MainView.root)
                    this@jfxnodeslist.animateList(false)
                }
            }
            animatedOptionButton(FontAwesomeIcon.CODE)
            animatedOptionButton(FontAwesomeIcon.FIRE)
        }
    }

    init {

    }
}

class ModpackListFragment : ListCellFragment<NestedModpackWrapper>() {
    val modpack = NestedModpackModel().bindTo(this)
    override val root = form {
        fieldset {
            field("Title") {
                label(modpack.title)
            }
            field("Name") {
                label(modpack.name)
            }
            field("Version") {
                label(modpack.version)
            }
            field("Version") {
                label(modpack.version)
            }
            field("Entries") {
                label(modpack.rootEntry.integerBinding { it?.flatCount()?: 0 })
            }
        }
    }
}