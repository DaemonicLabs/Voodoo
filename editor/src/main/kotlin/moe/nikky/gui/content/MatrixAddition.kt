package moe.nikky.gui.content

import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.Label
import moe.nikky.gui.LanternaGui
import moe.nikky.matrix.Matrix
import moe.nikky.matrix.MatrixPanel

/**
 * Created by nikky on 01/11/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class MatrixAddition(inputPanel: InputPanel) : ContentMatrixPanel(inputPanel) {

    init {

        val lblOutput_add = MatrixPanel(false)

        // add
        addComponent(Label("A + B"))
        val btnAdd = Button("Add!") {
            val m0 = inputPanel.matrix0.getMatrix()
            val m1 = inputPanel.matrix1.getMatrix()
            try {
                lblOutput_add.setMatrix(m0.plus(m1)) //TODO set matrix into a matrix output field
            } catch (e: RuntimeException) {
                LanternaGui.logger.warning(e.localizedMessage)
            }
        }.setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(2)).addTo(this)
        outputMatrixPanel(lblOutput_add, true)
    }
}
