package voodoo.gui.extensions

import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXListView
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.Node
import tornadofx.*

/**
 * Created by nikky on 20/03/18.
 * @author Nikky
 * @version 1.0
 */
internal inline fun <T : Node> T.attachTo(
        parent: EventTarget,
        after: T.() -> Unit,
        before: (T) -> Unit
) = this.also(before).attachTo(parent, after)

fun <T> EventTarget.jfxlistview(values: ObservableValue<ObservableList<T>>, op: JFXListView<T>.() -> Unit = {}) = JFXListView<T>().attachTo(this, op) {
    fun rebinder() {
        (it.items as? SortedFilteredList<T>)?.bindTo(it)
    }
    it.itemsProperty().bind(values)
    rebinder()
    it.itemsProperty().onChange {
        rebinder()
    }
}

fun <T> EventTarget.jfxcombobox(property: Property<T>? = null, values: List<T>? = null, op: JFXComboBox<T>.() -> Unit = {}) = JFXComboBox<T>().attachTo(this, op) {
    if (values != null) it.items = values as? ObservableList<T> ?: values.observable()
    if (property != null) it.bind(property)
}