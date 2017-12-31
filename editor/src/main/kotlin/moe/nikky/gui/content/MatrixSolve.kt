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
class MatrixSolve(inputPanel: InputPanel) : ContentMatrixPanel(inputPanel) {

    init {

        val lblOutput_1 = MatrixPanel(false)

        // solve
        addComponent(Label("A^-1 B"))
        val btnAdd = Button("Solve!") {
            val m0 = inputPanel.matrix0.getMatrix()
            val m1 = inputPanel.matrix1.getMatrix()
            try {
                lblOutput_1.setMatrix(m0.solve(m1))
            } catch (e: RuntimeException) {
                LanternaGui.logger.warning(e.localizedMessage)
            }
        }.setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(2)).addTo(this)
        outputMatrixPanel(lblOutput_1, true)
    }
}
