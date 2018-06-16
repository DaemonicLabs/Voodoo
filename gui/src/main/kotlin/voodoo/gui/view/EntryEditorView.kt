package voodoo.gui.view

import com.jfoenix.controls.JFXButton
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import tornadofx.*
import voodoo.curse.CurseClient
import voodoo.gui.controller.EntryController
import voodoo.gui.extensions.jfxbutton
import voodoo.gui.extensions.jfxcombobox
import voodoo.gui.extensions.jfxtextfield
import voodoo.gui.model.EntryModel
import voodoo.provider.Provider

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class EntryEditorView : View("EntryWrapper Editor") {
    val controller: EntryController by inject()
    val model: EntryModel by inject()

    companion object {
        val validCurseNames = CurseClient.idMap.keys.toList()
    }

    override val root = form {
        val entry = controller.selectedEntry
        fieldset("Edit modpack") {
            vgrow = Priority.ALWAYS //important

            field("Name") {
                removeWhen { entry.provider.booleanBinding { it == Provider.CURSE.name } }
                jfxtextfield(entry.name) {
                    required()
                }
            }
            field("Name") {
                text("(?)").tooltip("select and start typing to filter mod names")
                removeWhen { entry.provider.booleanBinding { it != Provider.CURSE.name } }
                jfxcombobox(entry.name, validCurseNames.observable()) {
                    makeAutocompletable { text -> validCurseNames.filter { it.contains(text) } }
                    validator { input ->
                        if (validCurseNames.contains(input))
                            null
//                            ValidationMessage("", ValidationSeverity.Success)
                        else
                            ValidationMessage("not a valid curse mod", ValidationSeverity.Error)
                    }
                }
            }

            field("Provider") {
                jfxcombobox(entry.provider, Provider.values().map { it.name }.toList())
            }

//            field("Freeze EntryWrapper") {
//                jfxcheckbox(null, entry.freeze)
//            }
//            field("Force Rebuild") {
//                jfxcheckbox(null, entry.forceRebuild) {
//                    enableWhen(entry.dirty.not())
//                }
//            }

            jfxbutton("Save", type = JFXButton.ButtonType.RAISED) {
                enableWhen(entry.dirty)
                action {
                    entry.commit()
                }
                style {
                    backgroundColor += Color.WHITE
                }
                prefWidth = 80.0
                spacing = 5.0
            }
            jfxbutton("Reset", type = JFXButton.ButtonType.RAISED) {
                action {
                    entry.rollback()
                }
                style {
                    backgroundColor += Color.WHITE
                }
                prefWidth = 80.0
                spacing = 5.0
            }

//            text("\nResult:\n")
//            jfxtextarea(entry.resultJson) {
//                vgrow = Priority.SOMETIMES //important
//                isEditable = false
//            }
        }
    }

    private fun save() {
        model.validationContext.validate(true)
        model.commit()
        println("Saving ${model.item.name} / ${model.item.provider}")
    }
}
