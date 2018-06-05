package voodoo.mmc

import com.sun.jna.Platform
import mu.KLogging
import voodoo.util.Directories
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.TimeUnit

object MMCUtil : KLogging() {
    private val directories = Directories.get(moduleName = "multimc")
    private val cacheHome = directories.cacheHome

    fun startInstance(name: String) {
        val workingDir = cacheHome.resolve(name).apply { mkdirs() }

        ProcessBuilder("multimc", "--launch", name)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

        logger.info("started multimc instance $name")
    }

    fun findDir(): File {
       val cmd =  when {
            Platform.isWindows() -> "where multimc"
            else -> "which multimc"
        }
        val location = cmd.runCommandToString() ?: throw FileNotFoundException("cannot find multimc on path")
        val multimcFile = File(location)
        return multimcFile.parentFile
    }

    private val path = System.getProperty("user.dir")
    fun String.runCommand(workingDir: File = cacheHome) {
        logger.info("running '$this' in $workingDir")
        ProcessBuilder(*split(" ").toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .apply { logger.info { directory() } }
                .start()
//                .waitFor()
    }
    fun String.runCommandToString(workingDir: File = cacheHome): String? {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            return proc.inputStream.bufferedReader().readText()
        } catch(e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}
