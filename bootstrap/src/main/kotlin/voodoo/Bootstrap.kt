///*
// * SK's Minecraft Launcher
// * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
// * Please see LICENSE.txt for license information.
// */
//
//package voodoo
//
//import java.io.File
//import java.io.FileOutputStream
//import java.io.IOException
//import java.net.URL
//import java.security.MessageDigest
//import java.util.*
//import java.util.stream.Collectors
//import javax.xml.parsers.DocumentBuilderFactory
//import javax.xml.xpath.XPathConstants
//import javax.xml.xpath.XPathFactory
//import kotlin.system.exitProcess
//
//
//@Throws(Throwable::class)
//fun main(vararg args: String) {
//    try {
//        Bootstrap.cleanup()
//        Bootstrap.launch(*args)
//    } catch (t: Throwable) {
//        t.printStackTrace()
//        System.err.println("Error")
//        exitProcess(-1)
//    }
//}
//
//object Bootstrap {
//
//    val props = Properties().apply {
//        javaClass.getResourceAsStream("/maven.properties").bufferedReader().use {
//            load(it)
//        }
//    }
//
//    private val mavenUrl = props.getProperty("url") as String
//    private val group = props.getProperty("group") as String
//    private val artifact = props.getProperty("name") as String
//    private val classifier = props.getProperty("classifier") as String
//
////    private val directories: Directories = Directories.get(moduleName = "$artifact-bootstrap")
//    private val binariesDir: File = File(".voodoo") // directories.cacheHome
//    private val lastFile: File = binariesDir.resolve("newest.jar")
//
//    fun cleanup() {
//        val files = binariesDir.listFiles { pathname -> pathname.name.endsWith(".tmp") }
//
//        if (files != null) {
//            for (file in files) {
//                file.delete()
//            }
//        }
//    }
//
//    private fun download(): File {
//
//        val groupPath = group.replace('.','/')
//        val mavenMetadataUrl = "$mavenUrl/$groupPath/$artifact/maven-metadata.xml"
//
//        val dbFactory = DocumentBuilderFactory.newInstance()
//        val dBuilder = dbFactory.newDocumentBuilder()
////        val xmlInput = InputSource(StringReader(xmlText))
//        val doc = dBuilder.parse(URL(mavenMetadataUrl).openStream())
//        val xpFactory = XPathFactory.newInstance()
//        val xPath = xpFactory.newXPath()
//
//
//        val xpath = "/metadata/versioning/release/text()"
//        val releaseVersion = xPath.evaluate(xpath, doc, XPathConstants.STRING) as String
//
//        // TODO: select latest build within major or minor version ?
//
//        val artifactFile = downloadArtifact(
//            mavenUrl = mavenUrl,
//            group = group,
//            artifactId = artifact,
//            version = releaseVersion,
//            classifier = classifier,
//            extension = "jar",
//            outputDir = binariesDir
//        )
//
//        return artifactFile
//    }
//
//    @Throws(Throwable::class)
//    fun launch(vararg originalArgs: String) {
//        println("Downloading the $artifact binary...")
//        val file = try {
//            download().apply {
//                require(exists()) { "downloaded files does not seem to exist" }
//                copyTo(lastFile, overwrite = true)
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//            System.err.println("cannot download $artifact from $mavenUrl, trying to reuse last binary")
//            lastFile
//        }
//
//        require(file.exists()) { "binary $file does not exist" }
//
//        println("Loaded " + file.path)
//        val java = arrayOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
//        val workingDir = File(System.getProperty("user.dir"))
//
//        val debugArgs = System.getProperty("kotlinx.coroutines.debug")?.let {
//            arrayOf("-Dkotlinx.coroutines.debug")
//        } ?: emptyArray()
//
//        val args = arrayOf(java, *debugArgs, "-jar", file.path, *originalArgs)
//        println("executing [${args.joinToString { "'$it'" }}]")
//        val exitStatus = ProcessBuilder(*args)
//            .directory(workingDir)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//            .redirectError(ProcessBuilder.Redirect.INHERIT)
//            .start()
//            .waitFor()
//        exitProcess(exitStatus)
//    }
//
//    fun downloadArtifact(
//        mavenUrl: String,
//        group: String,
//        artifactId: String,
//        version: String,
//        classifier: String? = null,
//        extension: String = "jar",
//        outputDir: File,
//        outputFile: File? = null
//    ): File {
//        val groupPath = group.replace('.', '/')
//
//        val classifierSuffix = classifier?.let { "-$it"} ?: ""
//        val artifactUrl = "$mavenUrl/$groupPath/$artifactId/$version/$artifactId-$version$classifierSuffix.$extension"
//        val tmpFile = File(outputDir, "$artifactId-$version$classifierSuffix.$extension.tmp")
//        val targetFile = outputFile ?: File(outputDir, "$artifactId-$version$classifierSuffix.$extension")
//
//        val md5 = URL("$artifactUrl.md5").openStream().bufferedReader().use { bis ->
//            bis.lines().collect(Collectors.joining(System.lineSeparator()))
//        }
//
//        if(targetFile.exists()) {
//            val md = MessageDigest.getInstance("MD5")
//            md.update(targetFile.readBytes())
//            val digest = md.digest()
//            val fileMd5 = digest.toHexString() // DigestUtils.md5Hex(tmpFile.inputStream())
//            if(fileMd5.toLowerCase() == md5.toLowerCase()) {
//                println("cached file matched md5 hash")
//                return targetFile
//            }
//        }
//
//        run {
//            val url = URL(artifactUrl)
//            url.openStream().buffered().use { bis ->
//                FileOutputStream(tmpFile).use { fis ->
//                    val buffer = ByteArray(1024)
//                    var count = 0
//                    while (bis.read(buffer, 0, 1024).also { count = it } != -1) {
//                        fis.write(buffer, 0, count)
//                    }
//                }
//            }
//        }
//
//
//        run {
//            val md = MessageDigest.getInstance("MD5")
//            md.update(tmpFile.readBytes())
//            val digest = md.digest()
//            val fileMd5 = digest.toHexString() // DigestUtils.md5Hex(tmpFile.inputStream())
//            require(fileMd5.toLowerCase() == md5.toLowerCase()) { "$artifactUrl did not match md5 hash: '$md5' file: $fileMd5" }
//        }
//
//        tmpFile.copyTo(targetFile, overwrite = true)
//        tmpFile.delete()
//        return targetFile
//    }
//    fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })
//}
