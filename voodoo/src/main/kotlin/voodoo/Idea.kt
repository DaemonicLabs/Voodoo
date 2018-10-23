package voodoo

import mu.KLogging
import voodoo.ShellUtils.requireInPath
import voodoo.data.nested.NestedPack
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.function.Consumer
import kotlin.system.exitProcess

val logger = KLogging().logger
val KSCRIPT_CACHE_DIR = File(System.getProperty("user.home")!!, ".kscript")

data class ProcessResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String) {
    override fun toString(): String {
        return """
            Exit Code   : ${exitCode}Comand      : $command
            Stdout      : $stdout
            Stderr      : """.trimIndent() + "\n" + stderr
    }
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

fun runProcess(
    vararg cmd: String,
    wd: File? = null,
    stdoutConsumer: Consumer<String> = StringBuilderConsumer(),
    stderrConsumer: Consumer<String> = StringBuilderConsumer()
): ProcessResult {

    try {
        // simplify with https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
        val proc = ProcessBuilder(cmd.asList()).directory(wd)
            // see https://youtrack.jetbrains.com/issue/KT-20785
            .apply { environment()["KOTLIN_RUNNER"] = "" }.start()

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

fun errorIf(value: Boolean, lazyMessage: () -> Any) {
    if (value) {
        logger.error(lazyMessage().toString())
        quit(1)
    }
}

fun quit(status: Int): Nothing {
    print(if (status == 0) "true" else "false")
    exitProcess(status)
}

internal class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: Consumer<String>) :
    Thread() {

    override fun run() {
        BufferedReader(InputStreamReader(inputStream)).lines().forEach(consumeInputLine)
    }
}

internal open class StringBuilderConsumer : Consumer<String> {
    val sb = StringBuilder()

    override fun accept(t: String) {
        sb.appendln(t)
    }

    override fun toString(): String {
        return sb.toString()
    }
}

object ShellUtils {
    fun isInPath(tool: String) = evalBash("which $tool").stdout.trim().isNotBlank()
    fun requireInPath(tool: String, msg: String = "$tool is not in PATH") = errorIf(!isInPath(tool)) { msg }
}

fun launchIdeaWithKscriptlet(scriptFile: File, libs: List<File>): File {
    requireInPath(
        "idea",
        "Could not find 'idea' in your PATH. It can be created in IntelliJ under `Tools -> Create Command-line Launcher`"
    )

    logger.info("Setting up idea project from $scriptFile")

    val tmpProjectDir = scriptFile.run {
        absoluteFile.parentFile
    }.apply { mkdirs() }
    val gradleScript = """
importer org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3-M2")
    }
}
plugins {
    java
}
apply {
    plugin("kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val compileKotlin by tasks.getting(KotlinCompile::class) {
    // Customise the “compileKotlin” task.
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
    }
}
repositories {
    mavenCentral()
    jcenter()
    maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
}
val kotlin_version: String  = "1.3-M2"
dependencies {
//    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8", version = kotlin_version)
//    compile("org.jetbrains.kotlin:kotlin-stdlib")
    compile("org.jetbrains.kotlin:kotlin-script-runtime")
    compile(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}
val wrapper by tasks.getting(Wrapper::class) {
    gradleVersion = "4.9"
    distributionType = Wrapper.DistributionType.ALL
}
kotlin.sourceSets.maybeCreate("main").kotlin.srcDir("src")
    """.trimIndent()

    File(tmpProjectDir, "build.gradle.kts").writeText(gradleScript)

    // also copy/symlink script resource in
    tmpProjectDir.resolve("src").run {
        mkdirs()

        // https://stackoverflow.com/questions/17926459/creating-a-symbolic-link-with-java
        createSymLink(File(this, scriptFile.name), scriptFile)

        for (libJar in libs) {
            val target = tmpProjectDir.resolve("libs")
            target.mkdirs()
            libJar.copyTo(target.resolve(libJar.name), overwrite = true)
        }
//        // also symlink all includes
//        includeURLs.distinctBy { it.fileName() }
//                .forEach {
//
//                    val includeFile = when {
//                        it.protocol == "file" -> File(it.toURI())
//                        else -> fetchFromURL(it.toString())
//                    }
//
//                    createSymLink(File(this, it.fileName()), includeFile)
//                }
    }

    return tmpProjectDir
}

private fun createSymLink(link: File, target: File, overwrite: Boolean = false) {
    try {
        if (overwrite) link.deleteRecursively()
        Files.createSymbolicLink(link.toPath(), target.absoluteFile.toPath())
    } catch (e: IOException) {
        logger.error("Failed to create symbolic link to script. Copying instead...", e)
        target.copyTo(link)
    }
}

object Idea {
    suspend fun main(vararg args: String) {
        logger.info(args.joinToString())
        val script = File(args[0])
        if (!script.isFile) {
            logger.error { "$script is not a file" }
            exitProcess(-1)
        }
        val jar = File(NestedPack::class.java.protectionDomain.codeSource.location.path)
        logger.info("jarfile: $jar")
        val projectDir = launchIdeaWithKscriptlet(script, libs = listOf(jar))
//        runProcess("gradle", "idea", wd = projectDir)
        runProcess("idea", projectDir.absolutePath, wd = projectDir)
    }
}
