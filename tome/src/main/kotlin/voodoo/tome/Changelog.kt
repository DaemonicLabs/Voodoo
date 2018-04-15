package voodoo.tome

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.Provider
import voodoo.util.readJson
import java.io.File

/**
 * Created by nikky on 15/04/18.
 * @author Nikky
 * @version 1.0
 */

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 * @version 1.0
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
            val ca =a[a.length-i]
            val cb =b[b.length-i]
//            logger.info("${-i} $ca $cb")
            if (ca != cb || exclude.contains(ca)) {
                return a.substring(a.length-i+1, a.length)
            }
        }
        return a.substring(a.length-minLength, a.length)
    }

    fun cut(first: String, second: String): Pair<String, String> {
        logger.debug("a: $first b: $second")
        val prefix = commonPrefix(first, second)
        logger.debug("prefix: $prefix")
        val suffix = commonSuffix(first, second)
        logger.debug("suffix: $suffix")

        return Pair(first.removePrefix(prefix).removeSuffix(suffix), second.removePrefix(prefix).removeSuffix(suffix))
    }

    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val arguments = Arguments(ArgParser(args))

        arguments.run {
            logger.info("sources: $sources")
//            logger.info("templateFile: $templateFile")
            val builder = StringBuilder()
            val modpacks = sources.map { it.readJson<LockPack>() }

            // first version
            val first = modpacks.first()
            builder.append("## ${first.title} ${first.version}  \n")
            builder.append("**name:** `${first.name}`  \n")
            builder.append("**title:** `${first.title}`  \n")
            builder.append("**version:** `${first.version}`  \n")
            builder.append("**forge:** `${first.forge}`  \n")
            builder.append("\n")
            for (entry in first.entries) {
                logger.info("add ${entry.name}")
                val provider = Provider.valueOf(entry.provider).base
                builder.append(TEMPLATE_ADD
                        .replace("[modName]", entry.name)
                        .replace("[modVersion]", provider.getVersion(entry, first))
                )
            }
            builder.append("\n\n")

            val pairs = modpacks.dropLast(1).zip(modpacks.drop(1))

            for ((old, next) in pairs) {
                logger.info("previous: ${old.version} next: ${next.version}")

                builder.append("## ${next.title} ${next.version}  \n")
                if (old.name != next.name)
                    builder.append("**name:** `${old.name}` -> `${next.name}`  \n")
                if (old.title != next.title)
                    builder.append("**title:** `${old.title}` -> `${next.title}`  \n")
                if (old.version != next.version)
                    builder.append("**version:** `${old.version}` -> `${next.version}`  \n")
                if (old.forge != next.forge)
                    builder.append("**forge:** `${old.forge}` -> `${next.forge}`  \n")
                builder.append("\n")

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
                        builder.append(TEMPLATE_ADD
                                .replace("[modName]", entry.name)
                                .replace("[modVersion]", provider.getVersion(entry, next))
                        )
                        added = true
                    }
                }
                if (!added) {
                    builder.append("\nno mods added  \n\n")
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
                        val oldVersionLong = provider.getVersion(entry, old)
                        val nextVersionLong = nextProvider.getVersion(nextEntry, next)
                        if (oldVersionLong != nextVersionLong) {
                            val (oldVersion, nextVersion) = cut(oldVersionLong, nextVersionLong)
                            builder.append(TEMPLATE_UPDATE
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
                    builder.append("\nno mods updated  \n\n")
                }

                logger.info("scanning removed entries")
                for (entry in removed) {
//                    logger.info("remove ${entry.name}")
                    val provider = Provider.valueOf(entry.provider).base
                    builder.append(TEMPLATE_REMOVED
                            .replace("[modName]", entry.name)
                            .replace("[modVersion]", provider.getVersion(entry, next))
                    )
                }
                if (removed.isEmpty()) {
                    builder.append("\nno mods removed  \n")
                }
                builder.append("\n\n")
            }

            if (stdout) {
                print(builder.toString())
            } else {
                var target = targetArg ?: "${modpacks.last().name}.changelog.md"
                if (!target.endsWith(".md")) target += ".md"
                val targetFile = File(target)
                logger.info("Writing changelog file... $targetFile")
                targetFile.writeText(builder.toString())
            }
        }

    }

    private class Arguments(parser: ArgParser) {
//        val templateFile by parser.positional("TEMPLATE",
//                help = "template header") { File(this) }

        val sources by parser.positionalList("SOURCE", sizeRange = 2..Int.MAX_VALUE,
                help = "...") { File(this) }

        val headerFile by parser.storing("--header",
                help = "template header") { File(this) }
                .default<File?>(null)

        val footerFile by parser.storing("--footer",
                help = "template header") { File(this) }
                .default<File?>(null)

        val sort by parser.flagging("--sort",
                help = "template header")

        val targetArg by parser.storing("--output", "-o",
                help = "output file json")
                .default<String?>(null)

        val stdout by parser.flagging("--stdout", "-s",
                help = "print output")
                .default(false)
    }
}