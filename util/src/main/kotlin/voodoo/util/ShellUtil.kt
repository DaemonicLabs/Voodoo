package voodoo.util

import mu.KLogging
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.nio.file.Files
import java.util.function.Consumer

object ShellUtil : KLogging() {
    fun isInPath(tool: String) = when {
        Platform.isLinux || Platform.isMac -> evalBash("which $tool").stdout.trim().isNotBlank()
        Platform.isWindows -> runProcess("where", tool).stdout.trim().takeIf { !it.contains("INFO: Could not find files for the given pattern(s).") }?.isNotBlank() ?: false
        else -> throw IllegalStateException("unrecognized or unsupported platform")
    }

    fun evalBash(
        cmd: String,
        wd: File? = null,
        stdoutConsumer: Consumer<String> = StringBuilderConsumer(),
        stderrConsumer: Consumer<String> = StringBuilderConsumer()
    ): ProcessResult {
        return runProcess(
            "bash", "-c", cmd,
            wd = wd, stderrConsumer = stderrConsumer, stdoutConsumer = stdoutConsumer
        )
    }

    fun requireInPath(
        tool: String,
        msg: String =
            "$tool is not in PATH"
    ) = require(isInPath(tool)) { msg }

    fun createSymLink(link: File, target: File, overwrite: Boolean = false) {
        try {
            if (overwrite) link.deleteRecursively()
            Files.createSymbolicLink(link.toPath(), target.absoluteFile.toPath())
        } catch (e: IOException) {
            logger.error("Failed to create symbolic link to script. Copying instead...", e)
            target.copyTo(link)
        }
    }

    fun runProcess(
        vararg cmd: String,
        wd: File? = null,
        displayOut: Boolean = true,
        displayErr: Boolean = true,
        stdoutConsumer: Consumer<String> = StringBuilderConsumer(if (displayOut) System.out else null),
        stderrConsumer: Consumer<String> = StringBuilderConsumer(if (displayErr) System.err else null)
    ): ProcessResult {

        try {
            logger.debug { "running: ${cmd.joinToString("' '", "['", "']")}" }
            // simplify with https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
            val proc = ProcessBuilder(cmd.asList())
                .directory(wd)
                .start()

            // we need to gobble the streams to prevent that the internal pipes hit their respecitive buffer limits, which
            // would lock the sub-process execution (see see https://github.com/holgerbrandl/kscript/issues/55
            // https://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes-without-blocki
            val stdoutGobbler = StreamGobbler(proc.inputStream, stdoutConsumer).apply { start() }
            val stderrGobbler = StreamGobbler(proc.errorStream, stderrConsumer).apply { start() }

            val exitVal = proc.waitFor()

            // we need to wait for the gobbler threads or we may loose some output (e.g. in case of short-lived processes
            stderrGobbler.join()
            stdoutGobbler.join()

            return ProcessResult(cmd.joinToString(" "), exitVal, stdoutConsumer.toString(), stderrConsumer.toString())
        } catch (t: Throwable) {
            throw RuntimeException(t)
        }
    }

    internal class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: Consumer<String>) :
        Thread() {

        override fun run() {
            BufferedReader(InputStreamReader(inputStream)).lines().forEach(consumeInputLine)
        }
    }

    internal open class StringBuilderConsumer(val output: PrintStream? = null) : Consumer<String> {
        private val sb = StringBuilder()

        override fun accept(t: String) {
            output?.println(t)
            sb.appendLine(t)
        }

        override fun toString(): String {
            return sb.toString()
        }
    }

    data class ProcessResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String) {
        override fun toString(): String {
            return """
            Exit Code   : $exitCode
            Comand      : $command
            Stdout      : $stdout
            Stderr      : $stderr
            """.trimIndent()
        }
    }
}