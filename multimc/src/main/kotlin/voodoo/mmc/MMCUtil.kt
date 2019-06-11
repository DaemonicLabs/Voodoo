package voodoo.mmc

import com.skcraft.launcher.model.modpack.Recommendation
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KLogging
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.util.Directories
import voodoo.util.Platform
import voodoo.util.json
import voodoo.util.serializer.FileSerializer
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dialog
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.io.IOException
import java.util.SortedMap
import java.util.concurrent.TimeUnit
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JToggleButton
import javax.swing.SwingConstants
import javax.swing.UIManager
import kotlin.system.exitProcess

object MMCUtil : KLogging() {
    private val directories = Directories.get(moduleName = "multimc")
    private val cacheHome = directories.cacheHome
    private val configHome = Directories.get().configHome

    @Serializable
    data class MMCConfiguration(
        val binary: String = "multimc",
        @Serializable(with = FileSerializer::class) val path: File = File(System.getProperty("user.home") + "/.local/share/multimc")
    )

    val mmcConfig: MMCConfiguration

    init {
        val jsonWithDefaults = Json(JsonConfiguration(prettyPrint = true, unquoted = true, encodeDefaults = true))
        val mmcConfigurationFile = configHome.resolve("multimc.hjson")
        logger.info("loading multimc config $mmcConfigurationFile")
        mmcConfig = when {
            mmcConfigurationFile.exists() -> jsonWithDefaults.parse(
                MMCConfiguration.serializer(),
                mmcConfigurationFile.readText()
            )
            else -> MMCConfiguration()
        }
        logger.info("loaded config: $mmcConfig")

        mmcConfigurationFile.parentFile.mkdirs()
        mmcConfigurationFile.writeText(jsonWithDefaults.stringify(MMCConfiguration.serializer(), mmcConfig))
    }

    fun startInstance(name: String) {
        val process = ProcessBuilder(mmcConfig.binary, "--launch", name)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        logger.info("started multimc instance $name $process")
        val status = process.waitFor()
        logger.info("multimc instance exited with code $status")
    }

    var dir: File? = null
        private set

    /**
     * Finds the MultiMC data loccation
     */
    fun findDir(): File {
        logger.info("os.name: ${System.getProperty("os.name")}")
        dir = dir ?: when {
            Platform.isWindows -> {
                logger.debug("executing'where ${mmcConfig.binary}'")
                val location = "where ${mmcConfig.binary}".runCommand()
                logger.debug("output: $location")
                val multimcFile = File(location)
                multimcFile.parentFile ?: run {
                    logger.error("multimcFile: '$multimcFile'")
                    logger.error("Cannot find MultiMC on PATH")
                    logger.error("make sure to add the multimc install location to the PATH")
                    logger.error(
                        "go to `Control Panel\\All Control Panel Items\\System`" +
                                " >> Advanced system settings" +
                                " >> Environment Variables"
                    )
                    logger.info("once added restart the shell and try to execute `multimc`")
                    exitProcess(1)
                }
            }
            Platform.isLinux -> File(System.getProperty("user.home")).resolve(mmcConfig.path)
            else -> {
                logger.warn("unsupported platform, on OSX please contact NikkyAi to implement this OR make a PR")
                File(System.getProperty("user.home")).resolve(mmcConfig.path)
            }
        }
        return dir!!
    }

    private fun String.runCommand(workingDir: File = cacheHome): String {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            return proc.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            throw Exception("cannot execute '$this'")
        }
    }

    private fun readCfg(cfgFile: File): SortedMap<String, String> =
        cfgFile.bufferedReader().useLines { lines ->
            lines.map { Pair(it.substringBefore('='), it.substringAfter('=')) }.toMap().toSortedMap()
        }

    private fun writeCfg(cfgFile: File, properties: Map<String, String>) {
        cfgFile.createNewFile()
        cfgFile.writeText(
            properties.map { (key, value) -> "$key=$value" }
                .joinToString("\n")
        )
    }

    /**
     * Prepares a MultiMC instance
     * @return Minecraft Directory
     */
    fun installEmptyPack(
        name: String?,
        folder: String,
        icon: File? = null,
        mcVersion: String? = null,
        forgeVersion: String? = null,
        instanceDir: File = with(findDir()) {
            this.resolve(
                readCfg(this.resolve("multimc.cfg"))["InstanceDir"] ?: "instances"
            ).resolve(folder)
        },
        preLaunchCommand: String? = null
    ): File {
        instanceDir.mkdirs()

        logger.info("instance dir: '$instanceDir'")

        val minecraftDir = instanceDir.resolve(".minecraft")
        minecraftDir.mkdirs()

        val iconKey = if (icon != null && icon.exists()) {
            val iconName = "icon_$folder"
            val iconsDir =
                with(MMCUtil.findDir()) { this.resolve(readCfg(this.resolve("multimc.cfg"))["IconsDir"] ?: "icons") }
            icon.copyTo(iconsDir.resolve("$iconName.png"), overwrite = true)
            icon.copyTo(instanceDir.resolve("$iconName.png"), overwrite = true)
            iconName
        } else {
            "default"
        }

        val json = Json(JsonConfiguration(prettyPrint = true))

        // set minecraft and forge versions
        val mmcPackPath = instanceDir.resolve("mmc-pack.json")
        val mmcPack = if (mmcPackPath.exists()) {
            json.parse(MultiMCPack.serializer(), mmcPackPath.readText())
        } else MultiMCPack()

        if (mcVersion != null) {
            if (forgeVersion != null) {
                logger.info("forge version : $forgeVersion")
                mmcPack.components = listOf(
                    PackComponent(
                        uid = "net.minecraftforge",
                        version = forgeVersion,
                        important = true
                    )
                ) + mmcPack.components
            }
            mmcPack.components = listOf(
                PackComponent(
                    uid = "net.minecraft",
                    version = mcVersion,
                    important = true
                )
            ) + mmcPack.components
        }
        mmcPackPath.writeText(json.stringify(MultiMCPack.serializer(), mmcPack))

        val cfgFile = instanceDir.resolve("instance.cfg")
        val cfg = if (cfgFile.exists())
            readCfg(cfgFile)
        else
            sortedMapOf<String, String>()

        cfg["InstanceType"] = "OneSix"
        if(name != null) cfg["name"] = name
        cfg["iconKey"] = iconKey

        if (preLaunchCommand != null) {
            cfg["OverrideCommands"] = "true"
            cfg["PreLaunchCommand"] = preLaunchCommand
        }

        writeCfg(cfgFile, cfg)

        return minecraftDir
    }

    fun selectFeatures(
        selectables: List<MMCSelectable>,
        previousSelection: Map<String, Boolean>,
        name: String,
        version: String,
        forceDisplay: Boolean,
        updating: Boolean
    ): Pair<Map<String, Boolean>, Boolean> {
        if (selectables.isEmpty() && !forceDisplay) {
            logger.info("no selectable features")
            return Pair(mapOf(), false)
        }

        UIManager.setLookAndFeel(
            UIManager.getSystemLookAndFeelClassName()
        )

        logger.debug { selectables }
        logger.debug { previousSelection }

        val toggleButtons = selectables.associateBy({
            it.id
        }, {
            JToggleButton(it.name, previousSelection[it.id] ?: it.selected)
                .apply {
                    horizontalAlignment = SwingConstants.RIGHT
                }
        })

        var success = false
        var reinstall = false

        val windowTitle =
            "Features" + if (name.isBlank()) "" else " - $name" + if (version.isBlank()) "" else " - $version"
        val dialog = object : JDialog(null as Dialog?, windowTitle, true) {
            init {
                modalityType = Dialog.ModalityType.APPLICATION_MODAL

                val panel = JPanel()
                panel.layout = GridBagLayout()

                val setter = selectables.asSequence().sortedBy { it.name }.mapIndexed { row, optionalEntry ->
                    val indicator = JCheckBox("", toggleButtons.getValue(optionalEntry.id).isSelected)
                    panel.add(indicator,
                        GridBagConstraints().apply {
                            gridx = 0
                            gridy = row
                            weightx = 0.001
                            weighty = 1.0
                            anchor = GridBagConstraints.LINE_START
                            fill = GridBagConstraints.BOTH
                            ipady = 4
                        }
                    )

                    val toggle = toggleButtons.getValue(optionalEntry.id).apply {
                        alignmentX = Component.RIGHT_ALIGNMENT
                        toolTipText = optionalEntry.name
                    }

                    fun select(selected: Boolean) {
                        toggle.isSelected = selected
                        indicator.isSelected = selected
                    }
                    toggle.addItemListener {
                        select(toggle.isSelected)
                    }
                    indicator.addItemListener {
                        select(indicator.isSelected)
                    }

                    panel.add(toggle,
                        GridBagConstraints().apply {
                            gridx = 1
                            gridy = row
                            weightx = 0.001
                            weighty = 1.0
                            anchor = GridBagConstraints.LINE_START
                            fill = GridBagConstraints.BOTH
                            ipady = 4
                        }
                    )

                    val recommendation = when (optionalEntry.skRecommendation) {
                        Recommendation.starred -> {
                            val orange = Color(0xFFd09b0d.toInt())
                            JLabel("★").apply {
                                foreground = orange
                                toolTipText = "Recommended"
                            }
                        }
                        Recommendation.avoid -> {
                            JLabel("⚠️").apply {
                                foreground = Color.RED
                                toolTipText = "Avoid"
                            }
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
                            fill = GridBagConstraints.BOTH
                            insets = Insets(0, 8, 0, 8)
                        }
                    )

                    if (optionalEntry.description?.isBlank() != true) {
                        val descriptionText = JLabel("<html>${optionalEntry.description}</html>")
                        panel.add(descriptionText,
                            GridBagConstraints().apply {
                                gridx = 3
                                gridy = row
                                weightx = 1.0
                                anchor = GridBagConstraints.LINE_START
                                fill = GridBagConstraints.BOTH
                                ipady = 8
                                insets = Insets(0, 8, 0, 8)
                            }
                        )
                    }
                    optionalEntry.id to ::select
                }.toList().toMap()

                val scrollPane = JScrollPane(panel)
                add(scrollPane, BorderLayout.CENTER)
                val buttonPane = JPanel(GridBagLayout())

                val buttonResetDefault = JButton("Reset to Default").apply {
                    addActionListener {
                        setter.forEach { (id, function) ->
                            val selected = selectables.find { optionalEntry ->
                                optionalEntry.id == id
                            }!!.selected
                            function(selected)
                        }
                    }
                }
                buttonPane.add(buttonResetDefault, GridBagConstraints().apply {
                    gridx = 0
                    weightx = 1.0
                    anchor = GridBagConstraints.LINE_START
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })
                val buttonResetLast = JButton("Reset to Last").apply {
                    isEnabled = previousSelection.isNotEmpty()
                    addActionListener {
                        setter.forEach { (id, function) ->
                            val selected = selectables.find { it.id == id }!!.selected
                            function(previousSelection[name] ?: selected)
                        }
                    }
                }
                buttonPane.add(buttonResetLast, GridBagConstraints().apply {
                    gridx = 1
                    weightx = 1.0
                    anchor = GridBagConstraints.LINE_START
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })
                val buttonForceReinstall = JButton("Force Reinstall").apply {
                    isEnabled = false
                    toolTipText = "enable with checkbox"
                    addActionListener {
                        reinstall = true
                        success = true
                        dispose()
                    }
                }
                buttonPane.add(buttonForceReinstall, GridBagConstraints().apply {
                    gridx = 2
                    weightx = 1.0
                    anchor = GridBagConstraints.LINE_START
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })
                val checkForceReinstall = JCheckBox().apply {
                    isEnabled = updating
                    addItemListener {
                        buttonForceReinstall.isEnabled = isSelected
                    }
                }
                buttonPane.add(checkForceReinstall, GridBagConstraints().apply {
                    gridx = 3
                    weightx = 0.1
                    anchor = GridBagConstraints.LINE_END
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })

                val okText = if (updating) "Update" else "Install"
                val button = JButton(okText).apply {
                    addActionListener {
                        isVisible = false
                        success = true
                        dispose()
                    }
                }

                buttonPane.add(button, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 1
//                    weightx = 6.0
                    gridwidth = 4
                    anchor = GridBagConstraints.LINE_START
                    fill = GridBagConstraints.HORIZONTAL
                    ipady = 4
                    insets = Insets(4, 0, 0, 0)
                })

                add(buttonPane, BorderLayout.SOUTH)
                defaultCloseOperation = DISPOSE_ON_CLOSE
                addWindowListener(
                    object : WindowAdapter() {
                        override fun windowClosed(e: WindowEvent) {
                            logger.info("closing dialog")
                            if (!success)
                                exitProcess(1)
                        }
                    }
                )
                pack()
                setLocationRelativeTo(null)
            }

            override fun setVisible(visible: Boolean) {
                super.setVisible(visible)
                if (!visible) {
                    (parent as? JFrame)?.dispose()
                }
            }
        }
        logger.info("created dialog")

        val mmcStateFile = configHome.resolve("mmc.state.json")
        println(mmcStateFile)
        val mmcState = mmcStateFile.takeIf { it.exists() }
            ?.let {
                json.parse(MMCState.serializer(), it.readText())
            }
        val bounds = mmcState?.bounds
        if (bounds != null) {
            dialog.bounds = Rectangle(bounds.x, bounds.y, bounds.width, bounds.height)
        } else {
            dialog.setLocationRelativeTo(null)
        }

        dialog.isVisible = true
        dialog.dispose()

        // save statw
        mmcStateFile.writeText(json.stringify(MMCState.serializer(), MMCState(bounds = Bounds(dialog.bounds))))

        return selectables.associateBy({ it.id }, { toggleButtons.getValue(it.id).isSelected }) to reinstall
    }
}
