package voodoo

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import mu.KLogging
import org.apache.commons.codec.digest.DigestUtils
import voodoo.data.Recommendation
import voodoo.data.SKFeature
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.data.IfTask
import voodoo.data.Pack
import voodoo.util.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.*
import kotlin.system.exitProcess


/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 * @version 1.0
 */

object Hex : KLogging() {
    private val directories = Directories.get(moduleName = "hex")

    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val arguments = Arguments(ArgParser(args))

        arguments.run {

            install(instanceId, instanceDir, minecraftDir)
        }

    }

    fun File.sha1Hex(): String? = DigestUtils.sha1Hex(this.inputStream())

    fun install(instanceId: String, instanceDir: File, minecraftDir: File) {

        val urlFile = instanceDir.resolve("voodoo.url.txt")
        val packUrl = urlFile.readText()
        val (_, _, result) = packUrl.httpGet()
//                .header("User-Agent" to useragent)
                .responseString()

        val modpack: Pack = when (result) {
            is Result.Success -> {
                jsonMapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error("${result.error} could not retrieve pack")
                return
            }
        }

        val oldpackFile = instanceDir.resolve("voodoo.modpack.json")
        val oldpack: Pack? = if (!oldpackFile.exists())
            null
        else {
            val pack: Pack = jsonMapper.readValue(oldpackFile)
            logger.info("loaded old pack ${pack.name} ${pack.version}")
            pack
        }

        if (oldpack != null) {
            if (oldpack.version == modpack.version) {
                logger.info("no update required ?")
//                return //TODO: make dialog close continue when no update is required ?
            }
        }

        val forgePrefix = "net.minecraftforge:forge:"
        val (_, _, forgeVersion) = modpack.versionManifest.libraries.find {
            it.name.startsWith(forgePrefix)
        }?.name.let { it ?: "::" }.split(':')

        logger.info("forge version is $forgeVersion")

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if (featureJson.exists()) {
            featureJson.readJson()
        } else {
            mapOf<String, Boolean>()
        }
        val features = selectFeatures(modpack.features, defaults)
        featureJson.writeJson(features)

        val cacheFolder = directories.cacheHome.resolve("hex")

        val objectsUrl = packUrl.substringBeforeLast('/') + "/" + modpack.objectsLocation

        val oldTasks = oldpack?.tasks?.toMutableList()

        // iterate new tasks
        for (task in modpack.tasks) {
            val whenTask = task.`when`
            if (whenTask != null) {
                val download = when (whenTask.`if`) {
                    IfTask.requireAny -> {
                        whenTask.features.any { features[it] ?: false }
                    }
                }
                if (!download) {
                    logger.info("${whenTask.features} is disabled, skipping download")
                    continue
                }
            }


            val url = if (task.location.startsWith("http")) {
                task.location
            } else {
                "$objectsUrl/${task.location}"
            }
            val target = minecraftDir.resolve(task.to)
            val chunkedHash = task.hash.chunked(6).joinToString("/")

            val oldTask = oldTasks?.find { it.location == task.location }
            if (target.exists()) {
                if (oldTask != null) {
                    // file exists already and existed in the last version

                    if (task.userFile) {
                        if (oldTask.userFile) {
                            logger.info("task ${task.location} is a userfile, will not be modified")
                            oldTasks.remove(oldTask)
                            continue
                        }
                    }
                    if (oldTask.hash == task.hash) {
                        if (target.isFile && target.sha1Hex() == task.hash) {
                            logger.info("task ${task.location} file did not change and sha1 hash matches")
                            oldTasks.remove(oldTask)
                            continue
                        }
                    } else {
                        // mismatching hash.. override file
                        oldTasks.remove(oldTask)
                        target.delete()
                        target.parentFile.mkdirs()
                        target.download(url, cacheFolder.resolve(chunkedHash))
                    }
                } else {
                    // file exists but was not in the last version.. reset to make sure
                    target.delete()
                    target.parentFile.mkdirs()
                    target.download(url, cacheFolder.resolve(chunkedHash))
                }
            } else {
                // new file
                target.parentFile.mkdirs()
                target.download(url, cacheFolder.resolve(chunkedHash))
            }

            if (target.exists()) {
                val sha1 = target.sha1Hex()
                if (sha1 != task.hash) {
                    logger.error("hashes do not match")
                    logger.error(sha1)
                    logger.error(task.hash)
                }
            }
        }

        // iterate old
        oldTasks?.forEach { task ->
            val target = minecraftDir.resolve(task.to)
            target.delete()
        }


        // set minecraft and forge versions
        val mmcPackPath = instanceDir.resolve("mmc-pack.json")
        val mmcPack = if (mmcPackPath.exists()) {
            mmcPackPath.readJson()
        } else MultiMCPack()
        mmcPack.components = listOf(
                PackComponent(
                        uid = "net.minecraft",
                        version = modpack.gameVersion,
                        important = true
                ),
                PackComponent(
                        uid = "net.minecraftforge",
                        version = forgeVersion.substringAfter("${modpack.gameVersion}-"),
                        important = true
                )
        ) + mmcPack.components
        mmcPackPath.writeJson(mmcPack)

        oldpackFile.writeJson(modpack)
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

    private class Arguments(parser: ArgParser) {
        val instanceId by parser.storing("--id",
                help = "\$INST_ID - ID of the instance")

        val instanceDir by parser.storing("--inst",
                help = "\$INST_DIR - absolute path of the instance") { File(this) }

        val minecraftDir by parser.storing("--mc",
                help = "\$INST_MC_DIR - absolute path of minecraft") { File(this) }
    }
}