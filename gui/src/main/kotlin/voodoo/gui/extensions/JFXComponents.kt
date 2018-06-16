package voodoo.gui.extensions

import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXDialogLayout
import javafx.event.EventTarget
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import tornadofx.*

/**
 * Created by nikky on 20/03/18.
 * @author Nikky
 * @version 1.0
 */

fun EventTarget.jfxdialog(
        dialogContainer: StackPane,
        content: Region,
        transitionType: JFXDialog.DialogTransition,
        op: JFXDialog.() -> Unit
): JFXDialog {
    val dialog = JFXDialog(dialogContainer, content, transitionType)
    op(dialog)
    return dialog
//    return opcr(this, dialog, op)
}
fun EventTarget.jfxdialog(
        op: JFXDialog.() -> Unit
): JFXDialog {
    val dialog = JFXDialog()
    op(dialog)
    return dialog
//    return opcr(this, dialog, op)
}
fun EventTarget.jfxdialoglayout(
        op: JFXDialogLayout.() -> Unit
): JFXDialogLayout {
    val layout = JFXDialogLayout()
    op(layout)
    return layout
}