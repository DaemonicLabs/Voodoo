package moe.nikky.matrix

import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.Panel
import moe.nikky.gui.TextField
import moe.nikky.util.FormatUtil

import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.ArrayList
import java.util.regex.Pattern

import moe.nikky.gui.LanternaGui.Companion.logger

/**
 * Created by nikky on 14/05/2017.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class MatrixPanel : Panel {
    private var matrix: Matrix? = null
    private val list = ArrayList<ArrayList<TextField>>()
    private var enabled = true
    //private Table<TextField> table;
    //private TableModel<TextField> model;

    constructor() {
        setMatrix(Matrix(5, 5))
    }

    constructor(enabled: Boolean) {
        this.enabled = enabled

        setMatrix(Matrix(arrayOf(doubleArrayOf(0.0))))
    }

    fun getMatrix(): Matrix {
        return Matrix(matrix!!)
    }

    private fun addColumn() {
        //        int columns = model.getColumnCount() + 1;
        //        int rows = model.getRowCount();
        //        ArrayList<TextField> column = new ArrayList<>();
        //        for (int i = 0; i < rows; i++) {
        //            column.add(createTextField(columns, i));
        //        }
        //
        //        model.addColumn("", column.toArray(new TextField[rows]));
    }

    private fun addRow() {
        //        int columns = model.getColumnCount();
        //        int rows = model.getRowCount() + 1;
        //        ArrayList<TextField> row = new ArrayList<>();
        //        for (int i = 0; i < columns; i++) {
        //            row.add(createTextField(i, rows));
        //        }
        //
        //        model.addRow(row);
    }

    private fun createTextField(row: Int, column: Int, initialValue: Double): TextField {
        val df = DecimalFormat("##.####")
        df.roundingMode = RoundingMode.HALF_EVEN
        val txtfield = TextField(df.format(initialValue)).onChange(
            object: TextField.OnChange{
                override fun run(text: String, event: String) {
                    try {
                        val value = java.lang.Double.parseDouble(text.replace(',', '.'))
                        logger.info(String.format("%s: [%d,%d]set double: %s %n", event, row, column, FormatUtil.fmt(value, true)))
                        matrix!!.set(row, column, value)
                    } catch (e: NumberFormatException) {
                        logger.warning(e.localizedMessage)
                    }
                }
            })
        txtfield.setValidationPattern(pattern)
        txtfield.isEnabled = enabled
        return txtfield
    }

    fun setMatrix(mat: Matrix) {
        this.removeAllComponents()
        matrix = mat
        val numberRows = matrix!!.rows
        val numberColumns = matrix!!.columns

        layoutManager = GridLayout(numberColumns)

        for (i in 0..numberRows - 1) {
            for (j in 0..numberColumns - 1) {
                this.addComponent(createTextField(i, j, mat.get(i, j)))
            }
        }
        this.invalidate()
    }

    companion object {
        protected val pattern = Pattern.compile(
                "[+-]?(\\d+([.,](\\d+)?)?)?"
        )
    }

    //    @Override
    //    public TerminalSize calculatePreferredSize() {
    //        return new TerminalSize(matrix.getColumns(), matrix.getRows());
    //    }
}
