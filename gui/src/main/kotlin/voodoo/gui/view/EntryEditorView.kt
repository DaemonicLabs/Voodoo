package voodoo.gui.view

import com.jfoenix.controls.JFXButton
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import tornadofx.*
import voodoo.curse.CurseClient
import voodoo.gui.controller.NestedEntryController
import voodoo.gui.extensions.jfxbutton
import voodoo.gui.extensions.jfxcheckbox
import voodoo.gui.extensions.jfxcombobox
import voodoo.gui.extensions.jfxtextfield
import voodoo.gui.model.NestedEntryModel
import voodoo.provider.Provider

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
class EntryEditorView : View("Entry Editor") {
    val controller: NestedEntryController by inject()
    val model: NestedEntryModel by inject()

    companion object {
        val validCurseNames = CurseClient.nameIdMap.keys.toList()
    }

    override val root = form {
        val entry = controller.selectedEntry
        fieldset("Edit Entry") {
            vgrow = Priority.ALWAYS //important

            field("Provider") {
                jfxcheckbox(entry.providerOverride)
                jfxcombobox(entry.provider, Provider.values().map { it.name }.toList()) {
                    enableWhen(entry.providerOverride)
                }
            }

            field("Name") {
                removeWhen { entry.provider.booleanBinding { it != Provider.CURSE.name }.not() }

                jfxcheckbox(entry.nameOverride)
                jfxtextfield(entry.name) {
                    enableWhen(entry.nameOverride)
                    required()
                }
            }
            field("Name") {
                removeWhen { entry.provider.booleanBinding { it == Provider.CURSE.name }.not() }
                jfxcheckbox(entry.nameOverride)
                text("(?)").tooltip("select and start typing to filter mod names")
                jfxcombobox(entry.name, validCurseNames.observable()) {
                    enableWhen(entry.nameOverride)
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

            field("Folder") {
                jfxcheckbox(entry.folderOverride)
                jfxtextfield(entry.folder) {
                    enableWhen(entry.folderOverride)
                    required()
                }
            }


            field("has sub-entries") {
                jfxcheckbox(entry.entriesOverride, "entries")
            }

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
        }
    }

    private fun save() {
        model.validationContext.validate(true)
        model.commit()
        println("Saving ${model.item.name} / ${model.item.provider}")
    }
}
