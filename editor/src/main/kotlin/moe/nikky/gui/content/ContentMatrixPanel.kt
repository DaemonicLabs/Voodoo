package moe.nikky.gui.content

import com.googlecode.lanterna.gui2.*
import moe.nikky.matrix.Matrix
import moe.nikky.matrix.MatrixPanel
import moe.nikky.util.ParseUtil

import java.util.HashSet
import java.util.regex.Pattern

/**
 * Created by nikky on 11/11/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
open class ContentMatrixPanel(protected var inputPanel: InputPanel) : Panel(GridLayout(3)) {

    protected fun outputMatrixPanel(matrixPanel: MatrixPanel, save: Boolean) {
        matrixPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING, true, false, 2, 1))
        val p = Panel(GridLayout(2))
        addComponent(matrixPanel)

        if (save) {
            Button("Save") {
                val matrix = matrixPanel.getMatrix()
                inputPanel.addMatrix(matrix)
            }.setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(1)).addTo(p)
        }

        addComponent(p, GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.CENTER, true, false, 1, 1))
    }

    protected fun addLabelButtonsScalar(description: String, lbl: Label) {
        addComponent(Label(description))
        addComponent(lbl, GridLayout.createHorizontallyEndAlignedLayoutData(1))
        Button("Save") {
            val scalar = ParseUtil.parseDoubleOr(lbl.text, 0.0)
            inputPanel.addScalar(scalar)
        }.setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(1))
                .addTo(this)
    }
}
