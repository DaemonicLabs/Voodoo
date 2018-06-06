package voodoo.mmc

import com.sun.jna.Platform
import mu.KLogging
import voodoo.data.Recommendation
import voodoo.data.SKFeature
import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.forge.Forge
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.provider.Provider
import voodoo.util.Directories
import voodoo.util.readJson
import voodoo.util.writeJson
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.*
import kotlin.system.exitProcess

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

    fun findDir(): File = when {
        Platform.isWindows() -> {
            val location = "where multimc".runCommandToString() ?: throw FileNotFoundException("cannot find multimc on path")
            val multimcFile = File(location)
            multimcFile.parentFile
        }
        Platform.isLinux() -> File(System.getProperty("user.home")+"/.local/share/multimc")
        else -> throw Exception("unsupported platform")
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
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun readCfg(cfgFile: File): SortedMap<String, String> =
            cfgFile.bufferedReader().useLines { lines ->
                lines.map { Pair(it.substringBefore('='), it.substringAfter('=')) }.toMap().toSortedMap()
            }

    fun writeCfg(cfgFile: File, properties: Map<String, String>) {
        cfgFile.createNewFile()
        cfgFile.bufferedWriter().use { bw ->
            properties.forEach { key, value ->
                bw.write(key)
                bw.write("=")
                bw.write(value)
                bw.newLine()
            }
        }
    }

    fun install(name: String, folder: String, modpack: LockPack) {
        val cacheDir = directories.cacheHome
        val multimcDir = MMCUtil.findDir()
        val instanceDir = multimcDir.resolve("instances").resolve(folder)
        instanceDir.mkdirs()

        val minecraftDir = instanceDir.resolve(".minecraft")
        minecraftDir.mkdirs()

        val modsDir = minecraftDir.resolve("mods")
        modsDir.deleteRecursively()

        val minecraftSrcDir = File(modpack.minecraftDir)
        if (minecraftSrcDir.exists()) {
            minecraftSrcDir.copyRecursively(minecraftDir, overwrite = true)
        }

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if (featureJson.exists()) {
            featureJson.readJson()
        } else {
            mapOf<String, Boolean>()
        }
        val features = selectFeatures(modpack.features.map { it.properties }, defaults)
        if(!features.isEmpty()){
            featureJson.createNewFile()
            featureJson.writeJson(features)
        }

        for (entry in modpack.entries) {
            if (entry.side == Side.SERVER) continue
            val matchedFeatureList = modpack.features.filter { it.entries.contains(entry.name) }
            if (!matchedFeatureList.isEmpty()) {
                val download = matchedFeatureList.any { features[it.properties.name] ?: false }
                if (!download) {
                    logger.info("${matchedFeatureList.map { it.properties.name }} is disabled, skipping download")
                    continue
                }
            }
            val provider = Provider.valueOf(entry.provider).base
            val targetFolder = minecraftDir.resolve(entry.folder)
            val (url, file) = provider.download(entry, modpack, targetFolder, cacheDir)
        }

        // set minecraft and forge versions
        val mmcPackPath = instanceDir.resolve("mmc-pack.json")
        val mmcPack = if (mmcPackPath.exists()) {
            mmcPackPath.readJson()
        } else MultiMCPack()
        logger.info("forge version for build ${modpack.forge}")
        val (_, _, _, forgeVersion) = Forge.getForgeUrl(modpack.forge.toString(), modpack.mcVersion)
        logger.info("forge version : $forgeVersion")
        mmcPack.components = listOf(
                PackComponent(
                        uid = "net.minecraft",
                        version = modpack.mcVersion,
                        important = true
                ),
                PackComponent(
                        uid = "net.minecraftforge",
                        version = forgeVersion,
                        important = true
                )
        ) + mmcPack.components
        mmcPackPath.writeJson(mmcPack)

        val cfgFile = instanceDir.resolve("instance.cfg")
        val cfgMap = if (cfgFile.exists())
            readCfg(cfgFile)
        else
            sortedMapOf()

        cfgMap["InstanceType"] = "OneSix"
        if (!cfgMap.containsKey("iconKey"))
            cfgMap["iconKey"] = "default"
        if (!cfgMap.containsKey("iconKey"))
            cfgMap["iconKey"] = "default"
        cfgMap["name"] = name
        writeCfg(cfgFile, cfgMap)

    }

    fun selectFeatures(features: List<SKFeature>, defaults: Map<String, Boolean>): Map<String, Boolean> {
        if (features.isEmpty()) {
            logger.info("no selectable features")
            return mapOf()
        }

        UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()
        )

        val checkBoxes = features.associateBy({
            it.name
        }, {
            JCheckBox("", defaults[it.name] ?: it.selected)
        })

        var success = false

        val adapter = object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                logger.info("closing dialog")
                if (!success)
                    exitProcess(1)
            }
        }

        val dialog = object : JDialog(null as Dialog?, "Features", true), ActionListener {
            init {
                modalityType = Dialog.ModalityType.APPLICATION_MODAL

                val panel = JPanel()
                panel.layout = GridBagLayout()

                for ((row, feature) in features.withIndex()) {

                    val check = checkBoxes[feature.name]
                    if (check != null) {
                        check.foreground = Color.LIGHT_GRAY
                        check.alignmentX = Component.LEFT_ALIGNMENT
                        panel.add(check,
                                GridBagConstraints().apply {
                                    gridx = 0
                                    gridy = row
                                    weightx = 0.001
                                    anchor = GridBagConstraints.LINE_START
                                    fill = GridBagConstraints.BOTH
                                }
                        )
                    }

                    val name = JLabel(feature.name)
                    panel.add(name,
                            GridBagConstraints().apply {
                                gridx = 1
                                gridy = row
                                weightx = 0.001
                                anchor = GridBagConstraints.LINE_END
                            }
                    )

                    val recommendation = when (feature.recommendation) {
                        Recommendation.starred -> {
                            val orange = Color(0xFFd09b0d.toInt())
                            name.foreground = orange
                            JLabel("★").apply { foreground = orange }
                        }
                        Recommendation.avoid -> {
                            name.foreground = Color.RED
                            JLabel("⚠️").apply { foreground = Color.RED }
                        }
                        else -> {
                            JLabel("")
                        }
                    }

                    panel.add(recommendation,
                            GridBagConstraints().apply {
                                gridx = 2
                                gridy = row
                                weightx = 0.001
                                insets = Insets(0, 8, 0, 8)
                            }
                    )

                    val description = JLabel("<html>${feature.description}</html>")

                    if (!feature.description.isNullOrBlank()) {
                        panel.add(description,
                                GridBagConstraints().apply {
                                    gridx = 3
                                    gridy = row
                                    weightx = 1.0
                                    anchor = GridBagConstraints.LINE_START
                                    fill = GridBagConstraints.BOTH
                                    ipady = 8
                                }
                        )
                    }
                }

                add(panel, BorderLayout.CENTER)
                val buttonPane = JPanel()
                val button = JButton("OK")
                button.addActionListener(this)
                buttonPane.add(button)
                add(buttonPane, BorderLayout.SOUTH)
                defaultCloseOperation = DISPOSE_ON_CLOSE
                addWindowListener(adapter)
                pack()
                setLocationRelativeTo(null)
            }

            override fun actionPerformed(e: ActionEvent) {
                isVisible = false
                success = true
                dispose()
            }

            override fun setVisible(visible: Boolean) {
                super.setVisible(visible)
                if (!visible) {
                    (parent as? JFrame)?.dispose()
                }
            }

            fun getValue(): Map<String, Boolean> {
                isVisible = true
                dispose()
                return features.associateBy({ it.name }, { checkBoxes[it.name]!!.isSelected })
            }
        }
        logger.info("created dialog")
        return dialog.getValue()
    }
}
