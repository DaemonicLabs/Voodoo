package voodoo.gui.extensions

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import tornadofx.*

/**
 * Created by nikky on 20/03/18.
 * @author Nikky
 * @version 1.0
 */

fun EventTarget.jfxcheckbox(property: Property<Boolean>? = null, text: String? = null, op: JFXCheckBox.() -> Unit = {}): JFXCheckBox {
    val checkbox = JFXCheckBox(text)
    if (property != null) checkbox.bind(property)
    return opcr(this, checkbox, op)
}

fun EventTarget.jfxtextfield(value: String? = null, op: TextField.() -> Unit = {}): JFXTextField {
    val textfield = JFXTextField()
    if (value != null) textfield.text = value
    return opcr(this, textfield, op)
}

fun EventTarget.jfxtextfield(property: ObservableValue<String>, op: TextField.() -> Unit = {}) = jfxtextfield().apply {
    bind(property)
    op(this)
}

fun EventTarget.jfxbutton(text: String = "", type: JFXButton.ButtonType? = null, graphic: Node? = null, op: JFXButton.() -> Unit = {}): JFXButton {
    val button = JFXButton(text)
    if (graphic != null) button.graphic = graphic
    if (type != null) button.buttonType = type
    return opcr(this, button, op)
}

fun EventTarget.jfxtextarea(value: String? = null, op: JFXTextArea.() -> Unit = {}) = opcr(this, JFXTextArea().apply { if (value != null) text = value }, op)
fun EventTarget.jfxtextarea(property: ObservableValue<String>, op: TextArea.() -> Unit = {}) = jfxtextarea().apply {
    bind(property)
    op(this)
}