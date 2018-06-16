package voodoo.gui.view

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*
import javafx.scene.layout.StackPane
import tornadofx.*

class MainView : View("Hello Voodoo TornadoFX") {
    companion object {
        lateinit var root: StackPane
    }
    override val root = stackpane {
        borderpane {
            // addClass(Styles.wrapper)
            left = hbox {
                add<ViualModpackList>()
            }
            center = hbox {
                add<ViualEntryList>()
            }
            right = hbox {
                add<EntryEditorView>()
            }
//        top = stackpane {
//            tornadofx.insets(insets.top, 10, 10, insets.left)
//            alignment = Pos.BOTTOM_RIGHT
//            jfxnodeslist {
//                mainButton(PLUS)
//                animatedOptionButton(CODEPEN)
//                animatedOptionButton(CODE)
//                animatedOptionButton(FIRE)
//            }
//        }

        }
    }
    init {
        Companion.root = root
    }
}




