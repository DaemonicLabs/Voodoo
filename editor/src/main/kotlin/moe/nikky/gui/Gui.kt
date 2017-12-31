package moe.nikky.gui

import moe.nikky.gui.content.*

import java.io.IOException
import java.util.Random

import java.awt.SystemColor.info

/**
 * Created by nikky on 01/11/16.
 *
 * @author Niklas Bloedorn [s0557375@htw-berlin.de](mailto:s0557375@htw-berlin.de)
 * @version 1.0
 */
class Gui @Throws(IOException::class)
constructor() : LanternaGui() {

    override fun buildNavigation(navigation: ActionList): ActionList {
        val input = InputPanel(this)
        val base = BaseMatrixPanel(input)
        navigation.addItem("addition", base, MatrixAddition(input))
        navigation.addItem("subtraction", base, MatrixSubtraction(input))
        navigation.addItem("multiplication", base, MatrixMultiplication(input))
        navigation.addItem("multiplication scalar", base, MatrixMultiplicationScalar(input))
        navigation.addItem("transpose", base, MatrixTranspose(input))
        navigation.addItem("solve", base, MatrixSolve(input))
        navigation.addItem("rank", base, MatrixRank(input))

        //        ActionList settingsNav = new ActionList(centerPanel);
        //        ActionList settingsActions = buildSettings(settingsNav);
        val settingsActions = buildSettings(navigation)
        //        navigation.sublist(settingsNav, sidePanel, "settings");

        navigation.addAction("logTest", Runnable {
            logger.info("log test")
            val j = Random().nextInt(10)
            for (i in 0..j - 1)
                logger.info(String.format("test %d", i))
        })

        navigation.addAction("exit", Runnable { System.exit(0) })

        return navigation
    }

}
