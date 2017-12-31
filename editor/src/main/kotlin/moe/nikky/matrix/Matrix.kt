package moe.nikky.matrix

import java.util.Arrays
import java.util.Random

/**
 * Created by nikky on 14/05/2017.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class Matrix {
    /**
     *
     * @return number of rows
     */
    val rows: Int             // number of rows
    /**
     *
     * @return number of columns
     */
    val columns: Int             // number of columns
    protected val data: Array<DoubleArray>   // M-by-N array

    /**
     * create M-by-N matrix of 0's
     * @param M number of rows
     * @param N number of columns
     */
    constructor(M: Int, N: Int) {
        if (M < 0 || N < 0) throw IllegalArgumentException("dimensions cannot be negative")
        this.rows = M
        this.columns = N
        data = Array(M) { DoubleArray(N) }
    }

    /**
     * create matrix based on 2d array
     * @param data internal double array
     */
    constructor(data: Array<DoubleArray>) {
        rows = data.size
        columns = data[0].size
        this.data = Array(rows) { DoubleArray(columns) }
        for (i in 0..rows - 1)
            for (j in 0..columns - 1)
                this.data[i][j] = data[i][j]
    }

    /**
     * copy constructor
     * @param A other Matrix
     */
    constructor(A: Matrix) : this(A.data) {}

    fun resize(rows: Int, columns: Int): Matrix {
        val A = Matrix(this.rows + rows, this.columns + columns)
        A.copyFrom(this)
        return A
    }

    /**
     * copies the internal data of the other Matrix
     * @param A other Matrix
     */
    private fun copyFrom(A: Matrix) {
        val m = Math.min(rows, A.rows)
        val n = Math.min(columns, A.columns)
        for (i in 0..m - 1)
            System.arraycopy(A.data[i], 0, this.data[i], 0, n)
    }

    // swap rows i and j

    /**
     * swap rows i and j
     * @param i row i
     * @param j row j
     */
    protected fun swap(i: Int, j: Int) {
        val temp = data[i]
        data[i] = data[j]
        data[j] = temp
    }    // swap rows i and j

    /**
     * create and return the transpose of the invoking matrix
     * @return transposed matrix
     */
    fun transpose(): Matrix {
        val A = Matrix(columns, rows)
        for (i in 0..rows - 1)
            for (j in 0..columns - 1)
                A.data[j][i] = this.data[i][j]
        return A
    }

    /**
     * add two matrices
     * C = A + B
     * @param B other matrix
     * @return `this` + `B`
     */
    operator fun plus(B: Matrix): Matrix {
        val A = this
        if (B.rows != A.rows || B.columns != A.columns) throw RuntimeException("Illegal matrix dimensions.")
        val C = Matrix(rows, columns)
        for (i in 0..rows - 1)
            for (j in 0..columns - 1)
                C.data[i][j] = A.data[i][j] + B.data[i][j]
        return C
    }


    /**
     * subtracts the other matrix from `this`
     * C = A - B
     * @param B other matrix
     * @return `this` - `B`
     */
    operator fun minus(B: Matrix): Matrix {
        val A = this
        if (B.rows != A.rows || B.columns != A.columns) throw RuntimeException("Illegal matrix dimensions.")
        val C = Matrix(rows, columns)
        for (i in 0..rows - 1)
            for (j in 0..columns - 1)
                C.data[i][j] = A.data[i][j] - B.data[i][j]
        return C
    }

    /**
     * equality check
     * @param B other matrix
     * @return A === B
     */
    fun equals(B: Matrix): Boolean {
        val A = this
        if (B.rows != A.rows || B.columns != A.columns) throw RuntimeException("Illegal matrix dimensions.")
        for (i in 0..rows - 1)
            for (j in 0..columns - 1)
                if (A.data[i][j] != B.data[i][j]) return false
        return true
    }

    // return C = A * B

    /**
     * multiply `this` with the other matrix
     * C = A * B
     * @param B other matrix
     * @return `this` * `B`
     */
    operator fun times(B: Matrix): Matrix {
        val A = this
        if (A.columns != B.rows) throw RuntimeException("Illegal matrix dimensions.")
        val C = Matrix(A.rows, B.columns)
        for (i in 0..C.rows - 1)
            for (j in 0..C.columns - 1)
                for (k in 0..A.columns - 1)
                    C.data[i][j] += A.data[i][k] * B.data[k][j]
        return C
    }

    // return C = A * b
    /**
     * multiply `this` with scalar value `b`
     * C = A * b
     * @param b scalar value
     * @return `this` * `b`
     */
    operator fun times(b: Double): Matrix {
        val A = this
        //if (A.N != B.M) throw new RuntimeException("Illegal matrix dimensions.");
        val C = Matrix(A)
        for (i in 0..C.rows - 1)
            for (j in 0..C.columns - 1)
                C.data[i][j] = A.data[i][j] * b
        return C
    }

    /**
     * solves matrix using `rhs`
     * @param rhs other matrix
     * @return return x = A^-1 b, assuming A is square and has full rank
     */
    fun solve(rhs: Matrix): Matrix {
        if (rows != columns || rhs.rows != columns || rhs.columns != 1)
            throw RuntimeException("Illegal matrix dimensions.")

        if (rank() < rows)
            throw RuntimeException("Matrix does not have full rank")

        // create copies of the data
        val A = Matrix(this)
        val b = Matrix(rhs)

        // Gaussian elimination with partial pivoting
        for (i in 0..columns - 1) {

            // find pivot row and swap
            var max = i
            for (j in i + 1..columns - 1)
                if (Math.abs(A.data[j][i]) > Math.abs(A.data[max][i]))
                    max = j
            A.swap(i, max)
            b.swap(i, max)

            // singular
            if (A.data[i][i] == 0.0) throw RuntimeException("Matrix is singular.")

            // pivot within b
            for (j in i + 1..columns - 1)
                b.data[j][0] -= b.data[i][0] * A.data[j][i] / A.data[i][i]

            // pivot within A
            for (j in i + 1..columns - 1) {
                val m = A.data[j][i] / A.data[i][i]
                for (k in i + 1..columns - 1) {
                    A.data[j][k] -= A.data[i][k] * m
                }
                A.data[j][i] = 0.0
            }
        }

        // back substitution
        val x = Matrix(columns, 1)
        for (j in columns - 1 downTo 0) {
            var t = 0.0
            for (k in j + 1..columns - 1)
                t += A.data[j][k] * x.data[k][0]
            x.data[j][0] = (b.data[j][0] - t) / A.data[j][j]
        }
        return x

    }

    /**
     *
     * @return string representation
     */
    override fun toString(): String {
        val b = StringBuilder()
        for (i in 0..rows - 1) {
            for (j in 0..columns - 1)
                b.append(String.format("%9.4f ", data[i][j]))
            b.append("\n")
        }
        return b.toString()
    }

    fun shortDescription(): String {
        return String.format("%dx%d ", rows, columns)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Matrix)
            equals(other as Matrix?)
        else
            false
    }

    // print matrix to standard output
    fun show() {
        println(toString())
    }

    /**
     *
     * @param row row index
     * @param column column index
     * @return value at the row and column index
     */
    operator fun get(row: Int, column: Int): Double {
        return data[row][column]
    }

    /**
     * set value in matrix
     * @param row row index
     * @param column column index
     * @param value double value that will be set
     */
    operator fun set(row: Int, column: Int, value: Double) {
        if (row < 0 || row >= data.size || column < 0 || column >= data[row].size)
            throw IllegalArgumentException("row and/or column are out of bounds")
        data[row][column] = value
    }

    /**
     * calculate the number of linear independent rows in the matrix
     * @return rank of the matrix
     */
    fun rank(): Int {
        var rank = columns
        val R = rows
        val C = columns

        // create copies of the data
        val A = Matrix(this)

        var row = 0
        while (row < rank) {
            // Before we visit current row 'row', we make
            // sure that mat[row][0],....mat[row][row-1]
            // are 0.

            // Diagonal element is not zero
            if (A.data[row][row] != 0.0) {
                for (col in 0..R - 1) {
                    if (col != row) {
                        // This makes all entries of current
                        // column as 0 except entry 'mat[row][row]'
                        val mult = A.data[col][row].toDouble() / A.data[row][row]
                        for (i in 0..rank - 1)
                            A.data[col][i] -= mult * A.data[row][i]
                    }
                }
            } else {
                var reduce = true

                /* Find the non-zero element in current
                column  */
                for (i in row + 1..R - 1) {
                    // Swap the row with non-zero element
                    // with this row.
                    if (A.data[i][row] != 0.0) {
                        //swap(mat, row, i, rank);
                        A.swap(row, i)
                        reduce = false
                        break
                    }
                }

                // If we did not find any row with non-zero
                // element in current columnm, then all
                // values in this column are 0.
                if (reduce) {
                    // Reduce number of columns
                    rank--

                    // Copy the last column here
                    for (i in 0..R - 1)
                        A.data[i][row] = A.data[i][rank]
                }

                // Process this row again
                row--
            }// Diagonal element is already zero. Two cases
            // arise:
            // 1) If there is a row below it with non-zero
            //    entry, then swap this row with that row
            //    and process that row
            // 2) If all elements in current column below
            //    mat[r][row] are 0, then remvoe this column
            //    by swapping it with last column and
            //    reducing number of columns by 1.
            row++

            // Uncomment these lines to see intermediate results
            // display(mat, R, C);
            // printf("\n");
        }
        return rank
    }

    companion object {

        /**
         * create and return a random M-by-N matrix with values between a and b
         * @param M number of rows
         * @param N number of columns
         * @param a lower bound of random values
         * @param b upper bound of random values
         * @param round how much digits after the omma should be rounded
         * @return random Matrix
         */
        @JvmOverloads
        fun random(M: Int, N: Int, a: Int = 0, b: Int = 1, round: Int = 2): Matrix {
            val A = Matrix(M, N)
            val r = Random()
            val rounder = Math.pow(10.0, round.toDouble())
            for (i in 0..M - 1)
                for (j in 0..N - 1)
                    A.data[i][j] = Math.round((a + (r.nextDouble() * b - a)) * rounder) / rounder
            return A
        }

        // create and return the N-by-N identity matrix

        /**
         * create and return the N-by-N identity matrix
         * @param N number of rows and colums
         * @return identity matrix
         */
        fun identity(N: Int): Matrix {
            val I = Matrix(N, N)
            for (i in 0..N - 1)
                I.data[i][i] = 1.0
            return I
        }
    }
}
/**
 * create and return a random M-by-N matrix with values between a and b
 * @param M number of rows
 * @param N number of columns
 * @return random Matrix
 */
