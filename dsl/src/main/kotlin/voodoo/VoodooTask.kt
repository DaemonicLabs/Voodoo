package voodoo

import com.eyeem.watchadoin.Stopwatch
import voodoo.builder.Builder
import voodoo.builder.Importer
import voodoo.changelog.ChangelogBuilder
import voodoo.data.lock.LockPack
import voodoo.data.nested.NestedPack
import voodoo.pack.AbstractPack
import voodoo.tome.TomeEnv
import voodoo.util.SharedFolders
import voodoo.dsl.GeneratedConstants
import java.io.File

sealed class VoodooTask(open val key: String) {
    object Build : VoodooTask("build") {
        suspend fun execute(
            stopwatch: Stopwatch,
            id: String,
            nestedPack: NestedPack,
            rootFolder: File,
            tomeEnv: TomeEnv?
        ) = stopwatch {
            "buildTask".watch {
                val modpack = "flatten".watch {
                    Importer.flatten(
                        stopwatch = this,
                        nestedPack = nestedPack,
                        id = id,
                        targetFolder = rootFolder
                    )
                }
                val lockPack = "build".watch {
                    Builder.build(this, modpack, id = id)
                }

                if (tomeEnv != null) {
                    val uploadDir = SharedFolders.UploadDir.get(id)
                    val rootDir = SharedFolders.RootDir.get().absoluteFile

                    // TODO: merge tome into core
                    "tome".watch {
                        Tome.generate(this, lockPack, tomeEnv, uploadDir)
                    }

                    // TODO: just generate meta info

                    Diff.writeMetaInfo(
                        stopwatch = "writeMetaInfo".watch,
                        rootDir = rootDir.absoluteFile,
                        newPack = lockPack
                    )
                }
            }
        }
    }

    object Changelog : VoodooTask("changelog") {
        suspend fun execute(
            stopwatch: Stopwatch,
            id: String,
            changelogBuilder: ChangelogBuilder,
            tomeEnv: TomeEnv
        ) = stopwatch {
                "changelogTask".watch {
                    val rootDir = SharedFolders.RootDir.get().absoluteFile
                    val uploadDir = SharedFolders.UploadDir.get(id)
                    val docDir = SharedFolders.DocDir.get(id)

                    val lockFileName = "$id.lock.pack.json"
                    val lockFile = rootDir.resolve(id).resolve(lockFileName)

                    val lockPack = LockPack.parse(lockFile.absoluteFile, rootDir)

                    "tome".watch {
                        Tome.generate(this, lockPack, tomeEnv, uploadDir)
                    }
                    Diff.createChangelog(
                        stopwatch = "createDiff".watch,
                        docDir = docDir,
                        rootDir = rootDir.absoluteFile,
                        currentPack = lockPack,
                        changelogBuilder = changelogBuilder
                    )
                }
            }
    }

    object Pack : VoodooTask("pack") {
        suspend fun execute(
            stopwatch: Stopwatch,
            id: String,
            packer: AbstractPack
        ) = stopwatch {
            "packTask".watch {
                val rootDir = SharedFolders.RootDir.get().absoluteFile
                val uploadDir = SharedFolders.UploadDir.get(id)
                val lockFileName = "$id.lock.pack.json"
                val lockFile = rootDir.resolve(id).resolve(lockFileName)

                val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                // TODO: pass pack method (enum / object)
                voodoo.Pack.pack("pack".watch, modpack, uploadDir, packer)
            }
        }
    }

    object Test : VoodooTask("test") {
        suspend fun execute(
            stopwatch: Stopwatch,
            id: String,
            method: TestMethod
        ) = stopwatch {
            "testTask".watch {
                val rootDir = SharedFolders.RootDir.get().absoluteFile
                val lockFileName = "$id.lock.pack.json"
                val lockFile = rootDir.resolve(id).resolve(lockFileName)

                val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)

                method.tester.execute(stopwatch = "${method.key}-test".watch, modpack = modpack, clean = method.clean)
            }
        }
    }

    object Version : VoodooTask("version") {
        val version = GeneratedConstants.FULL_VERSION
    }
}