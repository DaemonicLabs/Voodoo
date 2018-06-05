package voodoo.sk

import java.io.File
import org.apache.commons.codec.digest.DigestUtils

fun File.sha1Hex(): String? = DigestUtils.sha1Hex(this.inputStream())