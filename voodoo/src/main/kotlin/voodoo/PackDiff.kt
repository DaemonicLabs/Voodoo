package voodoo

import mu.KLogging
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.util.ShellUtil
import java.io.File

data class PackDiff(
    val oldpack: LockPack,
    val newPack: LockPack,
    val oldEntries: Map<String, LockEntry>,
    val newEntries: Map<String, LockEntry>,
    val oldSource: File,
    val newSource: File,
    val oldTag: String
): KLogging() {
    fun write(docDir: File) {
        docDir.mkdirs()
        val changelogFile = docDir.resolve("changelog.md")

        logger.info("writing changelog to $changelogFile")

        if (ShellUtil.isInPath("git")) {
            val diffFile = docDir.resolve("changes.diff")
            val diffResult = ShellUtil.runProcess("git", "diff", oldTag, "--", newPack.sourceDir, ":(exclude)*.lock.hjson", ":(exclude)*.lock.pack.hjson")
            logger.info("writing '$diffFile'")
            diffFile.writeText(diffResult.stdout)
        } else {
            logger.error("please install `git`")
        }

//        changelogFile.bufferedWriter().use {
//            it.write(diffText)
//        }
    }
}