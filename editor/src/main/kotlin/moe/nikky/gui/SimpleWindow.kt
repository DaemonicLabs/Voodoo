package moe.nikky.gui

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.gui2.AbstractWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.Interactable
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

/**
 * Created by nikky on 01/11/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class SimpleWindow : AbstractWindow {
    constructor() {}

    constructor(title: String) : super(title) {}

    /**
     * little trick to make clicking on buttons activate them
     * by sending Enter
     * @param position mouse position in terminal
     * @return true if clicked on something
     */
    fun select(position: TerminalPosition): Boolean {
        val interactable = interactableLookupMap.getInteractableAt(position)
        if (interactable != null && interactable is Button) {
            interactable.handleKeyStroke(KeyStroke(KeyType.Enter))
            return true
        }
        //        }
        return false
    }

    fun find(position: TerminalPosition): Interactable {
        return interactableLookupMap.getInteractableAt(position)
    }
}
