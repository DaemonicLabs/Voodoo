package moe.nikky.gui.content

import com.googlecode.lanterna.gui2.*

/**
 * Created by nikky on 11/11/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class BaseMatrixPanel(protected var inputPanel: InputPanel) : Panel(BorderLayout()) {
    protected var contentPanel: ContentMatrixPanel? = null

    init {
        addComponent(inputPanel, BorderLayout.Location.TOP)
    }

    fun setContent(content: ContentMatrixPanel) {
        if (contentPanel != null)
            removeComponent(contentPanel!!)
        contentPanel = content
        addComponent(contentPanel, BorderLayout.Location.CENTER)
    }
}
