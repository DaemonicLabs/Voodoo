package voodoo.gui.extensions

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXNodesList
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay
import tornadofx.*

/**
 * Created by nikky on 20/03/18.
 * @author Nikky
 * @version 1.0
 */

fun EventTarget.jfxnodeslist(spacing: Number? = 15, alignment: Pos? = null, op: JFXNodesList.() -> Unit = {}): JFXNodesList {
    val nodeslist = JFXNodesList()
    if (alignment != null) nodeslist.alignment = alignment
    if (spacing != null) nodeslist.spacing = spacing.toDouble()
    return opcr(this, nodeslist, op)
}

fun EventTarget.jfxbutton(text: String? = null, alignment: Pos? = null, styleClass: String? = null, op: JFXButton.() -> Unit = {}): JFXButton {
    val jfxbutton = if (text != null) JFXButton(text) else JFXButton()
    if (alignment != null) jfxbutton.alignment = alignment
    if (styleClass != null) jfxbutton.styleClass += styleClass
    return opcr(this, jfxbutton, op)
}

fun EventTarget.mainButton(icon: FontAwesomeIcon, op: JFXButton.() -> Unit = {}): JFXButton {
    return jfxbutton(null, Pos.CENTER, "main-button", op).apply {
        contentDisplay = ContentDisplay.GRAPHIC_ONLY
        graphic = mainIcon(icon)
    }
}

fun EventTarget.animatedOptionButton(icon: FontAwesomeIcon, op: JFXButton.() -> Unit = {}): JFXButton {
    return jfxbutton(null, Pos.CENTER, "animated-option-button", op).apply {
        contentDisplay = ContentDisplay.GRAPHIC_ONLY
        graphic = subIcon(icon)
    }
}

fun EventTarget.fontawesomeiconview(icon: FontAwesomeIcon = FontAwesomeIcon.ANCHOR, size: Number = 24, styleClass: String? = null, op: FontAwesomeIconView.() -> Unit = {}): FontAwesomeIconView {
    val iconview = FontAwesomeIconView(icon)
    iconview.size = size.toString()
    if (styleClass != null) iconview.styleClass += styleClass
    return opcr(this, iconview, op)
}

fun EventTarget.mainIcon(icon: FontAwesomeIcon, op: FontAwesomeIconView.() -> Unit = {}): FontAwesomeIconView {
    return fontawesomeiconview(icon, 24, "main-icon", op)
}

fun EventTarget.subIcon(icon: FontAwesomeIcon, op: FontAwesomeIconView.() -> Unit = {}): FontAwesomeIconView {
    return fontawesomeiconview(icon, 24, "sub-icon", op)
}
