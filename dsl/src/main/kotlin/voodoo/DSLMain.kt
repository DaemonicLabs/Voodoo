package voodoo

import kotlinx.coroutines.experimental.runBlocking
import voodoo.data.nested.NestedPack
import voodoo.dsl.DslConstants.FULL_VERSION
import java.io.File

fun withDefaultMain(
    arguments: Array<String>,
    root: File = File(System.getProperty("user.dir")),
    block: () -> NestedPack = { throw NotImplementedError() }
) {
    class XY
    println("classloader is of type:" + Thread.currentThread().contextClassLoader)
    println("classloader is of type:" + ClassLoader.getSystemClassLoader())
    println("classloader is of type:" + XY::class.java.classLoader)
    Thread.currentThread().contextClassLoader = XY::class.java.classLoader

    val nestedPack = block()
    val id = nestedPack.id
    val packFileName = "$id.pack.hjson"
    val packFile = root.resolve(packFileName)
    val lockFileName = "$id.pack.hjson"
    val lockFile = root.resolve(lockFileName)

    val funcs = mapOf<String, (Array<String>) -> Unit>(
        "import" to { _ -> runBlocking { Importer.flatten(nestedPack, root, targetFileName = packFileName) } },
        "build" to { args -> runBlocking { BuilderForDSL.build(packFile, root, id, targetFileName = lockFileName, args = *args) } },
        "quickbuild" to { args -> runBlocking {
            val modpack = Importer.flatten(nestedPack, root)
            BuilderForDSL.build(modpack, root, id, targetFileName = lockFileName, args = *args)
        } },
        "pack" to { args -> runBlocking { Pack.pack(lockFile, root, args = *args) } },
//        "test" to Tester::main,
//        "idea" to Idea::main,
        "version" to { _ ->
            logger.info(FULL_VERSION)
        }
    )

    fun printCommands(cmd: String?) {
        if (cmd == null) {
            logger.error("no command specified")
        } else {
            logger.error("unknown command $cmd")
        }
        logger.warn("voodoo $FULL_VERSION")
        logger.warn("commands: ")
        funcs.keys.forEach { key ->
            logger.warn("> $key")
        }
    }

    val invocations = arguments.split("--")
    invocations.forEach { argChunk ->
        val command = argChunk.getOrNull(0)
        logger.info(argChunk.joinToString())
        val remainingArgs = argChunk.drop(1).toTypedArray()

        if (command == null) {
            printCommands(null)
            return@forEach
        }

        val function = funcs[command.toLowerCase()]
        if (function == null) {
            printCommands(command)
            return
        }

        function(remainingArgs)
    }
}

private fun Array<String>.split(separator: String = "--"): List<Array<String>> {
    val result: MutableList<MutableList<String>> = mutableListOf(mutableListOf())
    this.forEach {
        if (it == separator)
            result += mutableListOf<String>()
        else
            result.last() += it
    }
    return result.map { it.toTypedArray() }
}
