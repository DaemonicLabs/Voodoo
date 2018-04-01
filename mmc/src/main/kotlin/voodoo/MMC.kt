package voodoo

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import org.apache.commons.codec.digest.DigestUtils
import voodoo.data.Recommendation
import voodoo.mmc.Feature
import voodoo.mmc.IfTask
import voodoo.mmc.Pack
import voodoo.util.Directories
import voodoo.util.download
import voodoo.util.jsonMapper
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

object MMC : KLogging() {
    val directories = Directories.get(moduleName = "mmc")

    @JvmStatic
    fun main(vararg args: String) {
//
//        val input = File(args[0])
//
//        val pack = input.readJson<Pack>()
        load(args[0])


    }

    fun load(url: String) {
        val (_, _, result) = url.httpGet()
//                .header("User-Agent" to useragent)
                .responseString()
        val pack: Pack = when (result) {
            is Result.Success -> {
                jsonMapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error("${result.error} could not retrieve pack")
                return
            }
        }

        logger.info("loaded ${pack.name}")

        //TODO: cache pack version


        val forgePrefix = "net.minecraftforge:forge:"
        val (_, _, forgeVersion) = pack.versionManifest.libraries.find {
            it.name.startsWith(forgePrefix)
        }?.name.let { it ?: "::" }.split(':')

        logger.info("forge version is $forgeVersion")

//        val featues = mapOf(
//                *pack.features.map { it.name to true }.toTypedArray()
//        )
        val features = selectFeatures(pack.features)
        //TODO: read user input
//
//        val dialog = object : JDialog() {
//            fun show() : Map<String, Bool> {
//                isVisible = true
//
//
//            }
//        }
//
//        val result = dialog.


        val folder = File("mmc", pack.name)
        folder.mkdirs()

        val cacheFolder = directories.cacheHome

        val objectsUrl = url.substringBeforeLast('/') + "/" + pack.objectsLocation

        val modsFolder = folder.resolve("mods")
        modsFolder.deleteRecursively()

        for (task in pack.tasks) {
            val whenTask = task.`when`
            if (whenTask != null) {
                val download = when (whenTask.`if`) {
                    IfTask.requireAny -> {
                        whenTask.features.any { features[it] ?: false }
                    }
                }
                if (!download) {
                    logger.info("${pack.name} is disabled, skipping download")
                    continue
                }
            }

            val url = if (task.location.startsWith("http")) {
                task.location
            } else {
                "$objectsUrl/${task.location}"
            }
            val target = folder.resolve(task.to)
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


    }

    fun File.sha1Hex(): String? {
        return DigestUtils.sha1Hex(this.inputStream())
    }

    fun selectFeatures(features: List<Feature>): Map<String, Boolean> {
        if (features.isEmpty()) {
            logger.info("no selectable features")
            return mapOf()
        }

        val checkPane = JPanel()
        checkPane.layout = BoxLayout(checkPane, BoxLayout.Y_AXIS)


//        logger.info("${checkPane.layout}")

        val checkBoxes = features.associateBy({
            it.name
        }, {
            JCheckBox("", it.selected)
        })

        val adapter = object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                throw Exception("cancelled")
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
                    (parent as JFrame).dispose()
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