package voodoo

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import voodoo.builder.Builder
import voodoo.builder.Importer
import voodoo.data.nested.NestedPack
import voodoo.dsl.DslConstants.FULL_VERSION
import java.io.File
import kotlin.system.exitProcess
import voodoo.data.lock.LockPack
import voodoo.script.MainScriptEnv

private val logger = KotlinLogging.logger {}

fun withDefaultMain(
    arguments: Array<out String>,
    root: File, // = File(System.getProperty("user.dir")),
    configureMain: MainScriptEnv.() -> NestedPack // = { throw IllegalStateException("no nested pack provided") }
) {
    // TODO: set system property "user.dir" to rootDir

    // classloader switching necessary for kscript
//    println("classloader is of type:" + Thread.currentThread().contextClassLoader)
//    println("classloader is of type:" + ClassLoader.getSystemClassLoader())
//    println("classloader is of type:" + XY::class.java.classLoader)
    Thread.currentThread().contextClassLoader = MainScriptEnv::class.java.classLoader

    if (arguments.firstOrNull() == "dump-srcRoot") {
        val nestedPack = MainScriptEnv(rootDir = root).configureMain()
        val srcRoot = root.resolve(nestedPack.sourceDir)
        println("srcRoot=$srcRoot")
        exitProcess(0)
    }

    val mainEnv = MainScriptEnv(rootDir = root)
    val nestedPack = mainEnv.configureMain()
    val id = nestedPack.id
    val packFileName = "$id.pack.hjson"
//    val packFile = rootDir.resolve(packFileName)
    val lockFileName = "$id.lock.hjson"
    val lockFile = root.resolve(lockFileName)

    val funcs = mapOf<String, suspend (Array<String>) -> Unit>(
        "import_debug" to { _ -> Importer.flatten(nestedPack, targetFileName = packFileName) },
//        "build_debug" to { args -> BuilderForDSL.build(packFile, rootDir, id, targetFileName = lockFileName, args = *args) },
        "build" to { args ->
            val modpack = Importer.flatten(nestedPack)
            val lockPack = Builder.build(modpack, name = id, /*targetFileName = lockFileName,*/ args = *args)
            Tome.generate(modpack, lockPack, mainEnv.tomeEnv)
            logger.info("finished")
        },
        "pack" to { args ->
            val modpack = LockPack.parse(lockFile.absoluteFile)
            Pack.pack(modpack, *args)
        },
        "test" to { args -> TesterForDSL.main(lockFile, args = *args) },
//        "idea" to Idea::main, //TODO: generate gradle/idea project ?
        "version" to { _ ->
            logger.info(FULL_VERSION)
        }
    )

    fun printCommands(cmd: String?) {
        if (cmd == null) {
            logger.error("no command specified")
        } else {
            logger.error("unknown command '$cmd'")
        }
        logger.warn("voodoo $FULL_VERSION")
        logger.warn("commands: ")
        funcs.keys.forEach { key ->
            logger.warn("> $key")
        }
    }

    val invocations = arguments.chunkBy(separator = "-")
    invocations.forEach { argChunk ->
        val command = argChunk.getOrNull(0) ?: run {
            printCommands(null)
            return
        }
        logger.info("executing command [${argChunk.joinToString()}]")
        val remainingArgs = argChunk.drop(1).toTypedArray()

        val function = funcs[command.toLowerCase()]
        if (function == null) {
            printCommands(command)
            return
        }

        runBlocking(CoroutineName("main")) {
            function(remainingArgs)
        }
    }
}

private fun Array<out String>.chunkBy(separator: String = "-"): List<Array<String>> {
    val result: MutableList<MutableList<String>> = mutableListOf(mutableListOf())
    this.forEach {
        if (it == separator)
            result += mutableListOf<String>()
        else
            result.last() += it
    }
    return result.map { it.toTypedArray() }
}
