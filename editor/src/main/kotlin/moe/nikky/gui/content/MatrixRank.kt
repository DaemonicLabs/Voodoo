package moe.nikky.gui.content

import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.Label
import moe.nikky.gui.LanternaGui
import moe.nikky.matrix.Matrix
import moe.nikky.matrix.MatrixPanel
import moe.nikky.util.FormatUtil
import moe.nikky.util.ParseUtil


/**
 * Created by nikky on 01/11/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class MatrixRank(inputPanel: InputPanel) : ContentMatrixPanel(inputPanel) {

    init {

        val lblOutput_1 = Label("")
        val lblOutput_2 = Label("")

        // multiply scalar
        //addComponent(new Label("rank(A)"));
        val btmMultiply = Button("Calculate Rank!") {
            val m0 = inputPanel.matrix0.getMatrix()
            val m1 = inputPanel.matrix1.getMatrix()
            val x = ParseUtil.parseDoubleOr(inputPanel.scalar0.text, 0.0)
            try {
                lblOutput_1.text = FormatUtil.fmt(m0.rank().toDouble())
            } catch (e: RuntimeException) {
                LanternaGui.logger.warning(e.toString())
                for (traceElement in e.stackTrace)
                    LanternaGui.logger.warning("\tat " + traceElement)
            }

            try {
                lblOutput_2.text = FormatUtil.fmt(m1.rank().toDouble())
            } catch (e: RuntimeException) {
                LanternaGui.logger.warning(e.localizedMessage)
                LanternaGui.logger.warning(e.toString())
            }
        }.setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(3)).addTo(this)
        addLabelButtonsScalar("rank(A)", lblOutput_1)
        addLabelButtonsScalar("rank(B)", lblOutput_2)
    }
}
