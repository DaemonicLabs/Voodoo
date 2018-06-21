package voodoo.gui.view

import tornadofx.*
import voodoo.gui.controller.TabController
import voodoo.gui.extensions.tabPane

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class TabbedEditorView : View("Entry Editor") {
    val controller: TabController by inject()

    override val root = tabPane {
        tab("Modpack") {
            add<ModpackEditorView>()
        }
        tab("Entry") {
            add<EntryEditorView>()
        }
        controller.selectionModel = selectionModel
    }
}
