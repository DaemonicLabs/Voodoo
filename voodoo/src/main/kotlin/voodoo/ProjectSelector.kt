package voodoo

import mu.KLogging
import voodoo.util.Directories
import voodoo.util.asFile
import java.io.File
import javax.swing.JFileChooser
import kotlin.system.exitProcess

object ProjectSelector : KLogging() {
    private val directories = Directories.get(moduleName = "project-selector")
    val lastSelectedFile = directories.cacheHome.resolve("lastSelected.txt")
    fun select(): File {
        val lastSelected = lastSelectedFile.takeIf { it.exists() }
            ?.readText()
            ?.asFile
            ?.takeIf { it.exists() }
            ?.let { it.parentFile }
        val chooser = JFileChooser(lastSelected ?: File("."))
        chooser.dialogType = JFileChooser.OPEN_DIALOG
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isAcceptAllFileFilterUsed = false
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

        lastSelectedFile.writeText(file.absolutePath)

        return file
    }
}