package moe.nikky.gui.content

import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.gui2.dialogs.ActionListDialogBuilder
import moe.nikky.gui.LanternaGui
import moe.nikky.matrix.Matrix
import moe.nikky.matrix.MatrixPanel
import moe.nikky.util.FormatUtil
import moe.nikky.util.ParseUtil
import java.util.*
import java.util.regex.Pattern


/**
 * Created by nikky on 01/11/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class InputPanel(protected val gui: LanternaGui) : Panel(GridLayout(3)) {
    private val matrices = HashMap<String, Matrix>()
    private val scalars = HashMap<String, Double>()

    val matrix0 = MatrixPanel()
    val matrix1 = MatrixPanel()
    val scalar0 = TextBox()
    private var matrixKey: Char = 'A'
    private var scalarKey: Char = 'a'

    init {

        addComponent(Label("Matrix 1"))
        addComponent(Label(""), GridLayout.createHorizontallyEndAlignedLayoutData(2))

        matrix0.setMatrix(Matrix.random(4, 4, -10, 10, 0))

        textBoxButtonsMatrix(matrix0, true, true)

        addComponent(Label("Matrix 2"))
        addComponent(Label(""), GridLayout.createHorizontallyEndAlignedLayoutData(2))

        matrix1.setMatrix(Matrix.random(4, 4, -10, 10, 0))

        textBoxButtonsMatrix(matrix1, true, true)

        scalar0.setValidationPattern(scalar)
        textBoxButtonsScalar(scalar0, true, "Scalar")
        val r = Random()
        val initialValue = Math.round(r.nextDouble() * 20 - 10).toDouble()
        scalar0.text = FormatUtil.fmt(initialValue)

        addComponent(Label(""), GridLayout.createHorizontallyFilledLayoutData(3))
    }

    //TODO: Save and load Matrix
    protected fun textBoxButtonsMatrix(matrixPanel: MatrixPanel, load: Boolean, sizes: Boolean) {
        matrixPanel.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING, true, false, 2, 1))
        addComponent(matrixPanel)
        val p = Panel(GridLayout(2))
        if (load) {
            Button("Load", Runnable {
                val actionBuilder = ActionListDialogBuilder()
                        .setTitle("Polynomial Buffer")
                        .setDescription("Choose an item")
                if (matrices.isEmpty())
                    return@Runnable
                val keys = matrices.keys;
                for (k: String in keys) {
                    actionBuilder.addAction(k, Runnable {
                        matrixPanel.setMatrix(matrices[k]!!);
                    });
                }
//                matrices.forEach((k,v) -> {
//                    actionBuilder.addAction(k, () -> {
//                        matrixPanel.setMatrix(v);
//                    });
//                });
                    actionBuilder.build().showDialog(gui.gui);
            }).addTo(p)
            Button ("Save") {
                addMatrix(matrixPanel.getMatrix())
                LanternaGui.logger.info(matrices.toString())
            }.addTo(p)
        }

        if (sizes) {
            Button("x++") {
                val matrix = matrixPanel.getMatrix().resize(0, 1)
                matrixPanel.setMatrix(matrix)
            }.addTo(p)
            Button("x--") {
                val matrix = matrixPanel.getMatrix().resize(0, -1)
                matrixPanel.setMatrix(matrix)
            }.addTo(p)

            Button("y++") {
                val matrix = matrixPanel.getMatrix().resize(1, 0)
                matrixPanel.setMatrix(matrix)
            }.addTo(p)
            Button("y--") {
                val matrix = matrixPanel.getMatrix().resize(-1, 0)
                matrixPanel.setMatrix(matrix)
            }.addTo(p)
        }

        addComponent(p, GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.BEGINNING, false, false, 1, 1))
    }

    protected fun textBoxButtonsScalar(txtBox: TextBox, load: Boolean, label: String) {
        txtBox.layoutData = GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.BEGINNING, false, false, 1, 1)
        addComponent(txtBox)
        addComponent(Label(label))
        val p = Panel(LinearLayout(Direction.HORIZONTAL))
        if (load) {
            Button("Load", Runnable {
                val actionBuilder = ActionListDialogBuilder()
                        .setTitle("Scalar Buffer")
                        .setDescription("Choose an item")
                if (scalars.isEmpty())
                    return@Runnable
                scalars.forEach { k, v ->
                    run {
                        actionBuilder.addAction(k, Runnable {
                            txtBox.setText(FormatUtil.fmt(v));
                        })
                    }
                }
                actionBuilder.build().showDialog(gui.gui);
            }).setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(1))
                .addTo(p)
        }

        Button("Save") {
            val scalar = ParseUtil.parseDoubleOr(txtBox.text, 0.0)
            addScalar(scalar)
            LanternaGui.logger.info(FormatUtil.fmt(scalar))
        }.setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(1))
                .addTo(p)
        addComponent(p, GridLayout.createHorizontallyEndAlignedLayoutData(1))
    }

    fun addMatrix(matrix: Matrix) {
        matrices.put(matrixKey + ": " + matrix.shortDescription(), matrix)
        LanternaGui.logger.info(String.format("saved matrix as %c", matrixKey))
        matrixKey++
    }

    fun addScalar(scalar: Double) {
        scalars.put(scalarKey + ": " + FormatUtil.fmt(scalar), scalar)
        LanternaGui.logger.info(String.format("saved scalar as %c", scalarKey))
        scalarKey++
    }

    companion object {
        protected val scalar = Pattern.compile("[+-]?(\\d+([\\.,](\\d+)?)?)?")
    }
}
