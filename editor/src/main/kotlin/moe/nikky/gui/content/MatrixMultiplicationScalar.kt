package moe.nikky.gui.content

import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.Label
import moe.nikky.gui.LanternaGui
import moe.nikky.matrix.Matrix
import moe.nikky.matrix.MatrixPanel
import moe.nikky.util.ParseUtil


/**
 * Created by nikky on 01/11/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class MatrixMultiplicationScalar(inputPanel: InputPanel) : ContentMatrixPanel(inputPanel) {

    init {

        val lblOutput_1 = MatrixPanel(false)
        val lblOutput_2 = MatrixPanel(false)

        // multiply scalar
        addComponent(Label("A * x"))
        val btmMultiply = Button("Multiply Matrix!") {
            val m0 = inputPanel.matrix0.getMatrix()
            val m1 = inputPanel.matrix1.getMatrix()
            val x = ParseUtil.parseDoubleOr(inputPanel.scalar0.text, 0.0)
            try {
                lblOutput_1.setMatrix(m0.times(x))
                lblOutput_2.setMatrix(m1.times(x))
            } catch (e: RuntimeException) {
                LanternaGui.logger.warning(e.localizedMessage)
            }
        }.setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(2)).addTo(this)
        outputMatrixPanel(lblOutput_1, true)
        addComponent(Label("B * x"), GridLayout.createHorizontallyFilledLayoutData(3))
        outputMatrixPanel(lblOutput_2, true)
    }
}
