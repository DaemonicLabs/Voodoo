package voodoo

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.changelog.ChangelogBuilder
import voodoo.changelog.PackDiff
import voodoo.data.lock.LockPack
import voodoo.util.Directories
import voodoo.util.ShellUtil
import voodoo.util.UnzipUtility
import voodoo.util.packToZip
import voodoo.util.unixPath
import java.io.File

object Diff : KLogging() {

    // TODO: differentiate creating a diff for current version (and zip)
    //  and between 2 old versions (unzip)

    private val directories = Directories.get(moduleName = "diff")
    fun createDiff(
        stopwatch: Stopwatch,
        docDir: File,
        rootDir: File,
        newPack: LockPack,
        changelogBuilder: ChangelogBuilder
    ): List<PackDiff> = stopwatch {

        addVersion(rootDir, newPack.id, newPack.version)
        val versions = readVersions(rootDir, newPack.id)
        logger.debug("versions: $versions")
        // get last version before the current one
//        val lastVersion = versions.lastOrNull().takeIf { it != newPack.version } ?: versions.getOrNull(versions.size-2)
//        logger.debug("lastVersion: $lastVersion")
        if (newPack.version != versions.lastOrNull() && newPack.version in versions) {
            throw IllegalArgumentException("version ${newPack.version} already exists and is not the last version, please do not try to break things")
        }

        val newMetaDataLocation = getMetaDataDefault(rootDir, newPack.id)
        val metaDataPointerFile = getMetaDataPointer(rootDir, newPack.id)

        // TODO: evaluate if filtering is necessary
        val validVersions = versions.filter { version ->
            val valid = newMetaDataLocation.resolve(version).run {
                logger.info("checking validity: $this")
                // these files can be generated later,
                // but not sure how it is for historic versions
                version == versions.last() ||
                        resolve("entry.meta.json").exists() &&
                        resolve("pack.meta.json").exists()
            }

            logger.info("version $version is valid? $valid")
            valid
        }
        logger.debug("validVersions: $validVersions")

        // copy new pack to .meta/packid/version/root
//        directories.cacheHome.resolve(newPack.id).resolve("source").let {tmpSource ->
//            newPack.sourceFolder.copyRecursively(tmpSource)
//            tmpSource.listFiles { file ->
//                when {
//                    file.endsWith(".entry.json") -> true
//                    file.endsWith(".entry.lock.json") -> true
//                    else -> false
//                }
//            }.forEach {
//                it.delete()
//            }
//        }
        val newPackSourceZip = newMetaDataLocation.resolve(newPack.version).resolve("source.zip")
        newPackSourceZip.parentFile.mkdirs()
        newPackSourceZip.deleteRecursively()
        // TODO: zip current pack
        packToZip(newPack.sourceFolder.toPath(), newPackSourceZip.toPath())

        val sourcesRoot = directories.cacheHome.resolve("sources").resolve(newPack.id)
        val sources = validVersions.associateWith { version ->
            val sourceZip = newMetaDataLocation.resolve(version).resolve("source.zip")
            sourcesRoot.resolve(version).also {
                it.deleteRecursively()
                it.mkdirs()
                UnzipUtility.unzip(sourceZip, it)
            }
        }

        val diffPairs = (listOf(null) + validVersions).zipWithNext()
        val diffs = diffPairs.map { (oldVersion, newVersion) ->
            logger.info("generating diff $oldVersion -> $newVersion")
            // load old version
            val oldSource = sources[oldVersion]
            logger.debug("old root dir: $oldSource")

            val oldLockPackFile = oldSource
                ?.resolve("${newPack.id}.lock.pack.json")

            val oldPack = try {
                logger.info("reading: $oldLockPackFile")
                if (oldLockPackFile != null && oldSource != null) {
                    LockPack.parse(oldLockPackFile, oldSource).also {
                        logger.info("oldPack: ${it.version}")
                    }
                } else null
            } catch (e: Exception) {
                logger.error("could not parse old pack")
                e.printStackTrace()
                null
            }

            val newSource = sources.getValue(newVersion!!)

            // TODO: create a diff object
            //   diff pack values
            //   diff entries
            //   diff files
            val diff = PackDiff(
                newPack = newPack,
                oldPack = oldPack
            )

            val oldMetaDataLocation = readMetaDataLocation(rootDir, oldPack?.id ?: newPack.id)
            logger.debug("docDir: $docDir")
            diff.writeChangelog(
                newMeta = newMetaDataLocation.resolve(newPack.version),
                oldMeta = oldVersion?.let { oldMetaDataLocation.resolve(it) },
                docDir = docDir,
                generator = changelogBuilder
            )
            val diffFile = if (oldSource != null) {
                writeDiff(sourcesRoot, oldSource, newSource, docDir)
            } else {
                val emptySource = sourcesRoot.resolve("_init")
                emptySource.deleteRecursively()
                emptySource.mkdirs()
                writeDiff(sourcesRoot, emptySource, newSource, docDir).also {
                    emptySource.deleteRecursively() // delete the useless empty folder afterwards
                }
            }
            diffFile?.copyTo(newMetaDataLocation.resolve(newPack.version).resolve(diffFile.name), overwrite = true)

            diff
        }

        metaDataPointerFile.writeText(newMetaDataLocation.relativeTo(rootDir).unixPath)

        PackDiff.writeFullChangelog(newMetaDataLocation, validVersions, docDir = docDir)

        return@stopwatch diffs
    }

    fun writeDiff(rootFolder: File, oldFolder: File, newMetaDataLocation: File, docDir: File): File? {
        if (ShellUtil.isInPath("diff")) {
            newMetaDataLocation.mkdirs()
            val diffFile = newMetaDataLocation.resolve("changes.diff")
            val diffResult = ShellUtil.runProcess(
                "diff",
                "-x", "*.lock.json",
                "-x", "*.lock.pack.json",
                "--",
                oldFolder.toRelativeString(rootFolder),
                newMetaDataLocation.toRelativeString(rootFolder),
                wd = rootFolder
            )
            logger.info("writing '$diffFile'")
            diffResult.stdout.trim().takeIf { it.isNotBlank() }?.let {
                diffFile.writeText(it)
            } ?: run {
                logger.info("diff result is empty")
                diffFile.delete()
                return null
            }

            return diffFile.copyTo(docDir.resolve(diffFile.name), overwrite = true)
        } else {
            logger.error("please install `diff`")
            return null
        }
    }

    fun getMetaDataDefault(rootDir: File, id: String) = rootDir.resolve(".meta").resolve(id.toLowerCase())
    fun getMetaDataPointer(rootDir: File, id: String) = rootDir.resolve(".meta").resolve(id.toLowerCase() + ".txt")

    fun readVersions(rootDir: File, id: String): List<String> {
        val versionsFile = getMetaDataDefault(rootDir, id).resolve("versions.txt")
        logger.debug("reading versions: .($versionsFile)")
        return versionsFile.takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() } ?: listOf()
    }

    fun addVersion(rootDir: File, id: String, version: String) {
        val versionsFile = getMetaDataDefault(rootDir, id).resolve("versions.txt")
        versionsFile.parentFile.mkdirs()
        val versions =
            (versionsFile.takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() } ?: listOf()).toSet() + version
        versionsFile.writeText(versions.joinToString("\n"))
    }

    fun readMetaDataLocation(
        rootDir: File,
        id: String,
        defaultLocation: File = getMetaDataDefault(rootDir, id)
    ): File {
        // TODO: where will the file be stored?
        // rootDir/.meta/$id.txt

        val metaDataPointerFile = getMetaDataPointer(rootDir, id)
        return metaDataPointerFile
            .takeIf { it.exists() }
            ?.run { rootDir.resolve(readText()) }
            ?: run {
                metaDataPointerFile.parentFile.mkdirs()
                metaDataPointerFile.writeText(defaultLocation.relativeTo(rootDir).path.replace('\\', '/'))
                defaultLocation
            }
    }
}
