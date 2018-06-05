package voodoo

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import mu.KLogging
import org.apache.commons.codec.digest.DigestUtils
import voodoo.data.Recommendation
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.sk.data.Feature
import voodoo.sk.data.IfTask
import voodoo.sk.data.Pack
import voodoo.util.*
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dialog
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.*
import javax.swing.BoxLayout


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
        val url = urlFile.readText()
        val (_, _, result) = url.httpGet()
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

        logger.info("loaded ${modpack.name}")

        val versionFile = instanceDir.resolve("voodoo.version.txt")
        if (versionFile.exists()) {
            val version = versionFile.readText()
            if (version == modpack.version) {
                logger.info("no update required ?")
//                return
            }
        }

        val forgePrefix = "net.minecraftforge:forge:"
        val (_, _, forgeVersion) = modpack.versionManifest.libraries.find {
            it.name.startsWith(forgePrefix)
        }?.name.let { it ?: "::" }.split(':')

        logger.info("forge version is $forgeVersion")

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if(featureJson.exists()) {
            featureJson.readJson()
        } else {
            mapOf<String, Boolean>()
        }
        val features = selectFeatures(modpack.features, defaults)
        featureJson.writeJson(features)

        val cacheFolder = directories.cacheHome

        val objectsUrl = url.substringBeforeLast('/') + "/" + modpack.objectsLocation

        val modsFolder = minecraftDir.resolve("mods")
        modsFolder.deleteRecursively()

        for (task in modpack.tasks) {
            val whenTask = task.`when`
            if (whenTask != null) {
                val download = when (whenTask.`if`) {
                    IfTask.requireAny -> {
                        whenTask.features.any { features[it] ?: false }
                    }
                }
                if (!download) {
                    logger.info("${modpack.name} is disabled, skipping download")
                    continue
                }
            }

            val url = if (task.location.startsWith("http")) {
                task.location
            } else {
                "$objectsUrl/${task.location}"
            }
            val target = minecraftDir.resolve(task.to)
            target.parentFile.mkdirs()
            if (target.exists()) {
                val sha1 = target.sha1Hex()
                if (sha1 == task.hash) {
                    logger.info("no changes to ${task.to}")
                    continue
                }
                if (task.userFile) {
                    logger.info("userfile: ${task.to}")
                    continue
                }
            }

            val path = task.hash.chunked(6).joinToString("/")
            target.download(url, cacheFolder.resolve(path))

            if (target.exists()) {
                val sha1 = target.sha1Hex()
                if (sha1 != task.hash) {
                    logger.error("hashes do not match")
                    logger.error(sha1)
                    logger.error(task.hash)
                }
            }
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

        versionFile.writeText(modpack.version)
    }

    fun selectFeatures(features: List<Feature>, defaults: Map<String, Boolean>): Map<String, Boolean> {
        if (features.isEmpty()) {
            logger.info("no selectable features")
            return mapOf()
        }

        val checkPane = JPanel()
        checkPane.layout = BoxLayout(checkPane, BoxLayout.Y_AXIS)

        val checkBoxes = features.associateBy({
            it.name
        }, {
            JCheckBox("", defaults[it.name] ?: it.selected)
        })

        val adapter = object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                logger.info("closing dialog")
            }
        }

        val dialog = object : JDialog(null as Dialog?, "Features", true), ActionListener {
            init {
                modalityType = Dialog.ModalityType.APPLICATION_MODAL
                val messagePane = JPanel()

                val listPanel = JPanel()
                listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)

                for (feature in features) {

                    val panel = JPanel()
                    val check = checkBoxes[feature.name]
                    if (check != null) {
                        panel.add(check)
                    }
                    val name = JLabel(feature.name).apply { panel.add(this) }
                    when (feature.recommendation) {
                        Recommendation.starred -> {
                            panel.add(JLabel("★").apply { foreground = Color.YELLOW })
                        }
                        Recommendation.avoid -> {
                            panel.add(JLabel("avoid"))
                            name.foreground = Color.RED
                        }

                    }
                    if (!feature.description.isNullOrBlank()) {
                        panel.add(JLabel(feature.description))
                    }
                    panel.alignmentX = Component.LEFT_ALIGNMENT
                    listPanel.add(panel)
                }
                messagePane.add(listPanel)

                add(messagePane, BorderLayout.CENTER)
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