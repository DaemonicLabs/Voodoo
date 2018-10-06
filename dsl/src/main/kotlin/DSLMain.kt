import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import voodoo.Builder
import voodoo.Importer
import voodoo.Pack
import voodoo.TesterForDSL
import voodoo.data.nested.NestedPack
import voodoo.dsl.DslConstants.FULL_VERSION
import java.io.File
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class MainEnv(
    val root: File
)

fun withDefaultMain(
    arguments: Array<String>,
    root: File = File(System.getProperty("user.dir")),
    block: MainEnv.() -> NestedPack = { throw IllegalStateException("no nested pack provided") }
) {

    // classloader switching necessary for kscript
    class XY
//    println("classloader is of type:" + Thread.currentThread().contextClassLoader)
//    println("classloader is of type:" + ClassLoader.getSystemClassLoader())
//    println("classloader is of type:" + XY::class.java.classLoader)
    Thread.currentThread().contextClassLoader = XY::class.java.classLoader

    if (arguments.first() == "dump-root") {
        val nestedPack = MainEnv(root = root).block()
        val srcRoot = root.resolve(nestedPack.sourceDir)
        println("root=$srcRoot")
        exitProcess(0)
    }

    val nestedPack = MainEnv(root = root).block()
    val id = nestedPack.id
    val packFileName = "$id.pack.hjson"
//    val packFile = root.resolve(packFileName)
    val lockFileName = "$id.lock.hjson"
    val lockFile = root.resolve(lockFileName)

    val funcs = mapOf<String, suspend (Array<String>) -> Unit>(
        "import_debug" to { _ -> Importer.flatten(nestedPack, root, targetFileName = packFileName) },
//        "build_debug" to { args -> BuilderForDSL.build(packFile, root, id, targetFileName = lockFileName, args = *args) },
        "build" to { args ->
            val modpack = Importer.flatten(nestedPack, root)
            Builder.build(modpack, root, id, targetFileName = lockFileName, args = *args)
        },
        "pack" to { args -> Pack.pack(lockFile, root, args = *args) },
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

private fun Array<String>.chunkBy(separator: String = "-"): List<Array<String>> {
    val result: MutableList<MutableList<String>> = mutableListOf(mutableListOf())
    this.forEach {
        if (it == separator)
            result += mutableListOf<String>()
        else
            result.last() += it
    }
    return result.map { it.toTypedArray() }
}
