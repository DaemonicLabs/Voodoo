package voodoo.tome

import com.fasterxml.jackson.databind.DeserializationFeature
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import com.xenomachina.text.clear
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.Provider
import voodoo.util.jsonMapper
import voodoo.util.readJson
import java.io.File

/**
 * Created by nikky on 15/04/18.
 * @author Nikky
 */

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

object Changelog : KLogging() {
    val TEMPLATE_ADD = "**added** `[modName]`  \n" +
            "**version**: `[modVersion]`  \n" +
            "\n"
    val TEMPLATE_REMOVED = "**removed** `[modName]`  \n" +
            "**version**: `[modVersion]`  \n" +
            "\n"
    val TEMPLATE_UPDATE = "**updated** `[modName]`  \n" +
            "**version**: `[oldModVersion]` -> `[newModVersion]`  \n" +
            "\n"

    fun commonPrefix(a: String, b: String, exclude: List<Char> = "0123456789".toList()): String {
        val minLength = Math.min(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[i] != b[i] || exclude.contains(a[i])) {
                return a.substring(0, i)
            }
        }
        return a.substring(0, minLength)
    }

    fun commonSuffix(a: String, b: String, exclude: List<Char> = "0123456789".toList()): String {
        val minLength = Math.min(a.length, b.length)
        for (i in 1 until minLength) {
            val ca = a[a.length - i]
            val cb = b[b.length - i]
//            logger.info("${-i} $ca $cb")
            if (ca != cb || exclude.contains(ca)) {
                return a.substring(a.length - i + 1, a.length)
            }
        }
        return a.substring(a.length - minLength, a.length)
    }

    fun cut(first: String, second: String): Pair<String, String> {
        logger.debug("a: $first b: $second")
        val prefix = commonPrefix(first, second)
        logger.debug("prefix: $prefix")
        val suffix = commonSuffix(first, second)
        logger.debug("suffix: $suffix")

        return Pair(first.removePrefix(prefix).removeSuffix(suffix), second.removePrefix(prefix).removeSuffix(suffix))
    }

    suspend fun String.replaceElse(oldValue: String, newValue: suspend () -> String, alternateiveValue: String = "[unavailable]", ignoreCase: Boolean = false): String {
        val newString = try {
            newValue()
        } catch (e: Exception) {
            e.printStackTrace()
            alternateiveValue
        }
        return this.replace(oldValue, newString, ignoreCase)
    }

    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val arguments = Arguments(ArgParser(args))

        arguments.run {
            runBlocking {
                logger.info("sources: $sources")
//            logger.info("templateFile: $templateFile")
                val builder = StringBuilder()
                val mapper = jsonMapper
                        .copy()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                val modpacks = sources.map { it.readJson<LockPack>(mapper) }

                val sections = mutableListOf<String>()

                // first version
                val first = modpacks.first()
                val section = StringBuilder()

                builder.append("# ${first.title}\n\n")

                section.append("## ${first.title} ${first.version}  \n\n")
                section.append("**name:** `${first.name}`  \n")
                section.append("**title:** `${first.title}`  \n")
                section.append("**version:** `${first.version}`  \n")
                section.append("**forge:** `${first.forge}`  \n")
                section.append("\n")
                for (entry in first.entries) {
                    logger.info("add ${entry.name}")
                    val provider = Provider.valueOf(entry.provider).base
                    section.append(TEMPLATE_ADD
                            .replace("[modName]", entry.name)
                            .replaceElse("[modVersion]", { provider.getVersion(entry) })
                    )
                }
                sections.add(0, section.toString())

                val pairs = modpacks.dropLast(1).zip(modpacks.drop(1))

                for ((old, next) in pairs) {
                    section.clear()
                    logger.info("previous: ${old.version} next: ${next.version}")

                    section.append("## ${next.title} ${next.version}  \n\n")
                    if (old.name != next.name)
                        section.append("**name:** `${old.name}` -> `${next.name}`  \n")
                    if (old.title != next.title)
                        section.append("**title:** `${old.title}` -> `${next.title}`  \n")
                    if (old.version != next.version)
                        section.append("**version:** `${old.version}` -> `${next.version}`  \n")
                    if (old.forge != next.forge)
                        section.append("**forge:** `${old.forge}` -> `${next.forge}`  \n")
                    section.append("\n")

                    val entries = if (sort) {
                        old.entries.sortedBy { it.name }
                    } else {
                        old.entries
                    }

                    logger.info("scanning added entries")
                    var added = false
                    for (entry in next.entries) {
//                    logger.info("add ${entry.name}")
                        val oldEntry = old.entries.find { it.name == entry.name }
                        if (oldEntry == null) {
                            val provider = Provider.valueOf(entry.provider).base
                            section.append(TEMPLATE_ADD
                                    .replace("[modName]", entry.name)
                                    .replaceElse("[modVersion]", { provider.getVersion(entry) })
                            )
                            added = true
                        }
                    }
                    if (!added) {
                        section.append("no mods added  \n\n")
                    }

                    logger.info("scanning changed entries")
                    val removed = mutableListOf<LockEntry>()
                    var updated = false
                    for (entry in entries) {
//                    logger.info("update ${entry.name}")
                        val nextEntry = next.entries.find { it.name == entry.name }
                        if (nextEntry != null) {
                            val provider = Provider.valueOf(entry.provider).base
                            val nextProvider = Provider.valueOf(nextEntry.provider).base
                            val oldVersionLong = try {
                                provider.getVersion(entry)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                "[unavailable]"
                            }

                            val nextVersionLong = try {
                                nextProvider.getVersion(nextEntry)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                "[unavailable]"
                            }

                            if (oldVersionLong != nextVersionLong) {
                                val (oldVersion, nextVersion) = cut(oldVersionLong, nextVersionLong)
                                section.append(TEMPLATE_UPDATE
                                        .replace("[modName]", entry.name)
                                        .replace("[oldModVersion]", oldVersion)
                                        .replace("[newModVersion]", nextVersion)
                                )
                                updated = true
                            }
                        } else {
                            removed += entry
                        }
                    }
                    if (!updated) {
                        section.append("\nno mods updated  \n\n")
                    }

                    logger.info("scanning removed entries")
                    for (entry in removed) {
//                    logger.info("remove ${entry.name}")
                        val provider = Provider.valueOf(entry.provider).base
                        section.append(TEMPLATE_REMOVED
                                .replace("[modName]", entry.name)
                                .replaceElse("[modVersion]", { provider.getVersion(entry) })
                        )
                    }
                    if (removed.isEmpty()) {
                        section.append("\nno mods removed  \n")
                    }

                    // append section to top
                    sections.add(0, section.toString())
                }

                builder.append(sections.joinToString("\n"))

                if (stdout) {
                    print(builder.toString())
                }
                if(!nofile){
                    var target = targetArg ?: "${modpacks.last().name}.changelog.md"
                    if (!target.endsWith(".md")) target += ".md"
                    val targetFile = File(target)
                    logger.info("Writing changelog file... $targetFile")
                    targetFile.writeText(builder.toString())
                }
            }

        }

    }

    private class Arguments(parser: ArgParser) {
//        val templateFile by parser.positional("TEMPLATE",
//                help = "template header") { File(this) }

        val sources by parser.positionalList("SOURCE", sizeRange = 2..Int.MAX_VALUE,
                help = "...") { File(this) }

        val sort by parser.flagging("--sort",
                help = "template header")

        val targetArg by parser.storing("--output", "-o",
                help = "output file json")
                .default<String?>(null)

        val stdout by parser.flagging("--stdout", "-s",
                help = "print output")
                .default(false)

        val nofile by parser.flagging("--nofile", "-n",
                help = "do not write output to a file")
                .default(false)
    }
}