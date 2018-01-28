/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package voodoo.bootstrap

import mu.KLogging
import voodoo.util.Directories
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.MalformedURLException
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.JFileChooser

@Throws(Throwable::class)
fun main(args: Array<String>) {
    val bootstrap = Bootstrap(args)
    try {
        bootstrap.cleanup()
        bootstrap.launch()
    } catch (t: Throwable) {
        Bootstrap.logger.warn("Error", t)
    }
}

fun String.runCommand(workingDir: File) {
    ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(60, TimeUnit.MINUTES)
}

class Bootstrap @Throws(IOException::class)
constructor(private val originalArgs: Array<String>) {

    val binariesDir: File
    val properties: Properties
    val directories: Directories = Directories.get(moduleNam = "bootstrap")

    init {
        properties = BootstrapUtils.loadProperties("bootstrap.properties")

        binariesDir = directories.cacheHome
    }

    fun cleanup() {
        val files = binariesDir.listFiles { pathname -> pathname.name.endsWith(".tmp") }

        if (files != null) {
            for (file in files) {
                file.delete()
            }
        }
    }

    @Throws(Throwable::class)
    fun launch() {
        download()

        val files = binariesDir.listFiles(LauncherBinary.Filter())
        val binaries = ArrayList<LauncherBinary>()

        if (files != null) {
            for (file in files) {
                logger.info("Found " + file.absolutePath + "...")
                binaries.add(LauncherBinary(file))
            }
        }


        if (!binaries.isEmpty()) {
            launchExisting(binaries)
        } else {
            throw FileNotFoundException("no valid files found in ${directories.cacheHome}")
        }
    }

    @Throws(Exception::class)
    fun download() {
        logger.info("Downloading the launcher...")
        val file = Downloader.download(directories.cacheHome)

        logger.info("Downloaded " + file.absolutePath + "...")
        launchExisting(listOf(LauncherBinary(file)))
    }

    @Throws(Exception::class)
    fun launchExisting(binaries: List<LauncherBinary>) {
        Collections.sort(binaries)
        var working: LauncherBinary? = null
        var clazz: Class<*>? = null

        for (binary in binaries) {
            var testFile = binary.path

            // FIXME: temporary hack because jackson-kotlin throws a fit
            val java_home = System.getProperty("java.home")
            val java = arrayOf(java_home, "bin", "java").joinToString(File.separator)
            "$java_home/bin/java -jar $testFile ${originalArgs.joinToString(" ")}".runCommand(File(System.getProperty("user.dir")))
            System.exit(0)

            try {
                testFile = binary.executableJar
                logger.info("Trying " + testFile.getAbsolutePath() + "...")
                clazz = load(testFile)
                logger.info("Launcher loaded successfully.")
                working = binary
                break
            } catch (t: Throwable) {
                logger.warn("Failed to load " + testFile.getAbsoluteFile(), t)
            }

        }

        if (working != null) {
            for (binary in binaries) {
                if (working != binary) {
                    logger.info("Removing " + binary.path + "...")
                    binary.remove()
                }
            }

            execute(clazz!!)
        } else {
            throw IOException("Failed to find launchable .jar")
        }
    }

    @Throws(InvocationTargetException::class, IllegalAccessException::class, NoSuchMethodException::class)
    fun execute(clazz: Class<*>) {
        val method = clazz.getDeclaredMethod("main", Array<String>::class.java)
        val launcherArgs: Array<String>

        if (portable) {
            launcherArgs = arrayOf()
        } else {
            launcherArgs = arrayOf()
        }

        val args = arrayOfNulls<String>(originalArgs.size + launcherArgs.size)
        System.arraycopy(launcherArgs, 0, args, 0, launcherArgs.size)
        System.arraycopy(originalArgs, 0, args, launcherArgs.size, originalArgs.size)

        logger.info("Launching with arguments " + Arrays.toString(args))

        method.invoke(null, args)
    }

    @Throws(MalformedURLException::class, ClassNotFoundException::class)
    fun load(jarFile: File): Class<*> {
        val urls = arrayOf(jarFile.toURI().toURL())
        val child = URLClassLoader(urls, this.javaClass.classLoader)
        return Class.forName(properties.getProperty("launcherClass"), true, child)
    }

    companion object : KLogging() {
        private val BOOTSTRAP_VERSION = 1
        val fileChooseDefaultDir: File

        val portable: Boolean

        init {
            val chooser = JFileChooser()
            val fsv = chooser.fileSystemView
            fileChooseDefaultDir = fsv.defaultDirectory

            portable = File("portable.txt").exists()
        }

    }


}
