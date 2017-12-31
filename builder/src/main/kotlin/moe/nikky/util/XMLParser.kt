package moe.nikky.util;

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.lang.reflect.Field
import java.net.URL
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Created by kongsin on 26/4/2559.
 */
class XMLParser<T>(var obj: Class<T>) {

    fun fromXML(url : URL) : T {
        val scanner = Scanner(url.openStream())
        var str = ""
        while (scanner.hasNext()){
            str+=scanner.nextLine()
        }
        return fromXML(str)
    }

    fun fromXML(xml: String): T {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val strBuilder = StringBuilder()
        strBuilder.append(xml)
        val byteStream = ByteArrayInputStream(strBuilder.toString().toByteArray())
        val doc = builder.parse(byteStream)
        doc.documentElement.normalize()
        return getNodeObject(doc.documentElement, obj)
    }

    private fun <A>getNodeObject(element: Element, myObj : Class<A>): A {
        val _obj = myObj.newInstance()
        val fields = myObj.fields
        for (f in fields) {
            if (isNativeObject(f)) {
                putValue(f, _obj, element)
            } else {
                if (f.type.isArray) {
                    val elm = getData(element.getElementsByTagName(f.name))
                    elm.size.let {
                        val tmpObject = java.lang.reflect.Array.newInstance(f.type.componentType, elm.size) as Array<Any>
                        for (i in 0..tmpObject.size - 1) {
                            tmpObject[i] = getNodeObject(elm[i] as Element, f.type.componentType)
                        }
                        f.set(_obj, tmpObject)
                    }
                } else {
                    val tmpObject = getNodeObject(element, f.type)
                    f.set(_obj, tmpObject)
                }
            }
        }
        return _obj
    }

    private fun getData(list: NodeList): Array<Element?> {
        val e: Array<Element?> = arrayOfNulls(list.length)
        e.let {
            for (i in 0..list.length - 1) {
                e[i] = list.item(i) as Element?
            }
        }
        return e
    }

    private fun <A>putValue(f: Field, obj: A, value: Element): A {
        val type = f.type
        val v = getData(value.getElementsByTagName(f.name))
        when (type) {
            Char::class.java ->
                if (f.type.isArray) {
                    val ch = arrayOfNulls<Char?>(v.size)
                    for (i in 0..ch.size - 1) {
                        ch[i] = v[i]!!.textContent.toCharArray()[i]
                    }
                    f.set(obj, ch)
                } else {
                    for (e in v) {
                        f.setChar(obj, e!!.textContent[0])
                    }
                }
            Int :: class.java ->
                if (f.type.isArray) {
                    val ch = arrayOfNulls<Int?>(v.size)
                    for (i in 0..ch.size - 1) {
                        ch[i] = v[i]!!.textContent.toInt()
                    }
                    f.set(obj, ch)
                } else {
                    for (e in v) {
                        f.setInt(obj, e!!.textContent.trim().toInt())
                    }
                }
            Short :: class.java ->
                if (f.type.isArray) {
                    val ch = arrayOfNulls<Short?>(v.size)
                    for (i in 0..ch.size - 1) {
                        ch[i] = v[i]!!.textContent.toShort()
                    }
                    f.set(obj, ch)
                } else {
                    for (e in v) {
                        f.setShort(obj, e!!.textContent.toShort())
                    }
                }
            Long :: class.java ->
                if (f.type.isArray) {
                    val ch = arrayOfNulls<Long?>(v.size)
                    for (i in 0..ch.size - 1) {
                        ch[i] = v[i]!!.textContent.toLong()
                    }
                    f.set(obj, ch)
                } else {
                    for (e in v) {
                        f.setLong(obj, e!!.textContent.toLong())
                    }
                }
            Boolean :: class.java ->
                if (f.type.isArray) {
                    val ch = arrayOfNulls<Boolean?>(v.size)
                    for (i in 0..ch.size - 1) {
                        ch[i] = v[i]!!.textContent.toBoolean()
                    }
                    f.set(obj, ch)
                } else {
                    for (e in v) {
                        f.setBoolean(obj, e!!.textContent.toBoolean())
                    }
                }
            Float :: class.java ->
                if (f.type.isArray) {
                    val ch = arrayOfNulls<Float?>(v.size)
                    for (i in 0..ch.size - 1) {
                        ch[i] = v[i]!!.textContent.toFloat()
                    }
                    f.set(obj, ch)
                } else {
                    for (e in v) {
                        f.setFloat(obj, e!!.textContent.toFloat())
                    }
                }
            Double :: class.java ->
                if (f.type.isArray) {
                    val ch = arrayOfNulls<Double?>(v.size)
                    for (i in 0..ch.size - 1) {
                        ch[i] = v[i]!!.textContent.toDouble()
                    }
                    f.set(obj, ch)
                } else {
                    for (e in v) {
                        f.setDouble(obj, e!!.textContent.toDouble())
                    }
                }
            String :: class.java ->
                if (f.type.isArray) {
                    val ch = arrayOfNulls<String?>(v.size)
                    for (i in 0..ch.size - 1) {
                        ch[i] = v[i]!!.textContent
                    }
                    f.set(obj, ch)
                } else {
                    for (e in v) {
                        f.set(obj, e!!.textContent)
                    }
                }
            }
        return obj
    }

    private fun isNativeObject(f: Field): Boolean {
        val type = f.type
        when (type) {
            Char :: class.java -> return true
            Int :: class.java -> return true
            Short :: class.java -> return true
            Long :: class.java -> return true
            Float :: class.java -> return true
            Double :: class.java -> return true
            Boolean  :: class.java -> return true
            else -> return String :: class.java == type
        }
    }

    fun toXML(obj: Any): String {
        val fields = obj.javaClass.declaredFields
        val builder = StringBuilder()
        builder.append("\n")
        builder.append(openTag(obj.javaClass.simpleName))
        builder.append("\n")
        for (_f in fields) {
            if (isNativeObject(_f)) {
                if (_f.type.isArray) {
                    val data = _f.get(obj) as Array<*>
                    for (o in data) {
                        builder.append("\n")
                        builder.append(openTag(_f.name))
                        builder.append(o.toString())
                        builder.append(closeTag(_f.name))
                        builder.append("\n")
                    }
                } else {
                    builder.append("\n")
                    builder.append(openTag(_f.name))
                    builder.append(_f.get(obj).toString())
                    builder.append(closeTag(_f.name))
                    builder.append("\n")
                }
            } else {
                if (_f.type.isArray) {
                    val data = _f.get(obj) as Array<*>
                    for (o in data) {
                        builder.append(toXML(o as Any))
                    }
                } else {
                    val tmpObject = _f.get(obj)
                    builder.append(toXML(tmpObject))
                }
            }
        }
        builder.append("\n")
        builder.append(closeTag(obj.javaClass.simpleName))
        return builder.toString().replace("\n\n", "\n")
    }

    private fun openTag(tagName: String): String {
        val b = StringBuilder()
        b.append("<")
        b.append(tagName)
        b.append(">")
        return b.toString()
    }

    private fun closeTag(tagName: String): String {
        val b = StringBuilder()
        b.append("</")
        b.append(tagName)
        b.append(">")
        return b.toString()
    }

}