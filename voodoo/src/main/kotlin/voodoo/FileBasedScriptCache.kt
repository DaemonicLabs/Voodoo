package voodoo

import voodoo.util.toHexString
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.MessageDigest
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvmhost.CompiledJvmScriptsCache

class FileBasedScriptCache(val baseDir: File) : CompiledJvmScriptsCache {
    internal fun uniqueHash(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
        val digestWrapper = MessageDigest.getInstance("MD5")
        digestWrapper.update(script.text.toByteArray())
        scriptCompilationConfiguration.entries().sortedBy { it.key.name }.forEach { (key, value) ->
            digestWrapper.update(key.name.toByteArray())
            digestWrapper.update(value.toString().toByteArray())
        }
        return digestWrapper.digest().toHexString()
    }

    override fun get(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*>? {
        val prefix = if (script is FileScriptSource) {
            "${script.file.name}-"
        } else ""
        val file = File(baseDir, prefix + uniqueHash(script, scriptCompilationConfiguration))
//        val file = File(baseDir, uniqueHash(script, scriptCompilationConfiguration))
        return if (file.exists()) {
            file.readCompiledScript(scriptCompilationConfiguration)
        } else {
            null
        }
    }

    override fun store(
        compiledScript: CompiledScript<*>,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        val prefix = if (script is FileScriptSource) {
            "${script.file.name}-"
        } else ""
        val file = File(baseDir, prefix + uniqueHash(script, scriptCompilationConfiguration))
//        val file = File(baseDir, uniqueHash(script, scriptCompilationConfiguration))
        file.outputStream().use { fs ->
            ObjectOutputStream(fs).use { os ->
                os.writeObject(compiledScript)
            }
        }
    }

    companion object {
        private fun File.readCompiledScript(scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*> {
            return inputStream().use { fs ->
                ObjectInputStream(fs).use { os ->
                    (os.readObject() as KJvmCompiledScript<*>).apply {
                        // TODO: figure out if i need this ?
//                        setCompilationConfiguration(scriptCompilationConfiguration)
                    }
                }
            }
        }
    }
}