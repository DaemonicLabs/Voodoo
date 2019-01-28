package voodoo.mcupdater

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object ServerPackGenerator {
    fun generate(output: File) {
        require(output.name.endsWith(".xml")) { "filename must have 'xml' extension" }
        output.absoluteFile.parentFile.mkdirs()

        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()

        // root elements
        val doc = docBuilder.newDocument()
        doc.child(doc, "ServerPack") {
            attr["version"] = "3.4"
            attr["xmlns"] = "http://www.mcupdater.com"
            attr["xmlns:xsi"] = "http://www.w3.org/2001/XMLSchema-instance"
            attr["xsi:schemaLocation"] = "http://www.mcupdater.com http://files.mcupdater.com/ServerPackv2.xsd"

            val server = child(doc, "Server") {
                attr["id"] = "mcu-intro"
                attr["name"] = "MCUpdater - Introduction to Modded"
                attr["newsUrl"] = "http://files.mcupdater.com/example/SamplePack.xml"
                attr["version"] = "1.7.10"
                attr["mainClass"] = "net.minecraft.launchwrapper.Launch"
                attr["revision"] = "1"
                attr["autoConnect"] = "true"

                (0..4).forEach { number ->
                    child(doc, "Module") {
                        attr["name"] = "Module $number"
                        attr["id"] = "mod$number"
                        child(doc, tagName = "URL", nodeValue = "http://files.minecraftforge.net/maven/cpw/mods/ironchest/1.7.10-6.0.62.742/ironchest-1.7.10-6.0.62.742-universal.jar") {
                            attr["priority"] = "0"
                        }
                        child(doc, "Required", "false") {
                            attr["isDefault"] = "true"
                        }
                        // TODO: ModType
                        //  md5

                        child(doc, "ModPath", nodeValue = "mods/module$number")
                        child(doc, "Size", nodeValue = "$number")

                        // TODO: SubModule

                    }
                }

                // add one module with all configfile entries
                child(doc, "Module") {
                    attr["name"] = "Configurations"
                    attr["id"] = "packId-configurations"
                    (0..3).forEach { number ->
                        child(doc, "ConfigFile") {
                            child(doc, tagName = "URL", nodeValue = "http://files.minecraftforge.net/maven/cpw/mods/ironchest/1.7.10-6.0.62.742/ironchest-1.7.10-6.0.62.742-universal.jar") {
                                attr["priority"] = "0"
                            }
                            child(doc, "Path", nodeValue = "config/sample$number.cfg")

                            // Whether or not this config should be preserved if an older version exists already.
                            child(doc, "NoOverwrite", nodeValue = "true")

                            child(doc, "MD5", nodeValue = "generate-me")
                        }
                    }
                }
            }
        }

        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        val source = DOMSource(doc)
        val result = StreamResult(output)

        // Output to console for testing
        // StreamResult result = new StreamResult(System.out);

        transformer.transform(source, result)

        println("File saved!")
    }
}

interface MapLike {
    operator fun set(key: String, value: String)
    operator fun get(key: String): String?
}

private val Element.attr: MapLike
    get() {
        val map by lazy {
            object : MapLike {
                override fun set(key: String, value: String) {
                    this@attr.setAttribute(key, value)
                }

                override fun get(key: String): String? {
                    return this@attr.getAttribute(key)
                }
            }
        }
        return map
    }

private fun Element.setAttributes(vararg pairs: Pair<String, String>) {
    for ((name, value) in pairs) {
        setAttribute(name, value)
    }
}

private fun Node.child(doc: Document, tagName: String, nodeValue: String? = null, configureElement: Element.() -> Unit = {}): Element {
    val childElement = doc.createElement(tagName)!!
    if(nodeValue != null) childElement.nodeValue = nodeValue
    childElement.configureElement()
    appendChild(childElement)
    return childElement
}

