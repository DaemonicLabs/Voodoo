package voodoo

import mu.KLogging
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter
import kotlin.system.exitProcess

object ProjectSelector : KLogging() {

    fun select(): File {
        val chooser = JFileChooser(File("."))
        chooser.dialogType = JFileChooser.OPEN_DIALOG
        chooser.choosableFileFilters.forEach {
            chooser.removeChoosableFileFilter(it)
        }
        val directoryFilter =  object : FileFilter() {
            override fun getDescription() = "Directories"
            override fun accept(f: File): Boolean = f.isDirectory
        }
        chooser.addChoosableFileFilter(directoryFilter)
        chooser.fileFilter = directoryFilter
        val option = chooser.showDialog(null, "Select project folder")
        val file = when (option) {
            JFileChooser.CANCEL_OPTION -> {
                logger.error("cancelled")
                exitProcess(option)
            }
            JFileChooser.ERROR_OPTION -> {
                logger.error("errored")
                exitProcess(option)
            }
            JFileChooser.APPROVE_OPTION -> {
                chooser.selectedFile
            }
            else -> {
                throw IllegalStateException("unhandled option: $option")
            }
        }

        return file
    }
}