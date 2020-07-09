package voodoo.data.flat

import kotlinx.serialization.Transient
import mu.KLogging
import voodoo.data.components.*
import voodoo.data.lock.CommonLockComponent
import voodoo.data.lock.LockEntry
import voodoo.provider.*
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

sealed class Entry: CommonMutable {
    data class Common(
        val common: CommonComponent = CommonComponent()
    ) : Entry(), CommonMutable by common {
        override val provider = ""
        init {
            optional = optionalData != null
        }
    }

    data class Curse(
        private val common: CommonComponent = CommonComponent(),
        private val curse: CurseComponent = CurseComponent()
    ) : Entry(), CommonMutable by common, CurseMutable by curse {
        override val provider = CurseProvider.id
        init {
            optional = optionalData != null
        }
    }

    data class Direct(
        private val common: CommonComponent = CommonComponent(),
        private val direct: DirectComponent = DirectComponent()
    ) : Entry(), CommonMutable by common, DirectMutable by direct {
        override val provider = DirectProvider.id
        init {
            optional = optionalData != null
        }
    }

    data class Jenkins(
        private val common: CommonComponent = CommonComponent(),
        private val jenkins: JenkinsComponent = JenkinsComponent()
    ) : Entry(), CommonMutable by common, JenkinsMutable by jenkins {
        override val provider = JenkinsProvider.id
        init {
            optional = optionalData != null
        }
    }

    data class Local(
        private val common: CommonComponent = CommonComponent(),
        private val local: LocalComponent = LocalComponent()
    ) : Entry(), CommonMutable by common, LocalMutable by local {
        override val provider = LocalProvider.id
        init {
            optional = optionalData != null
        }
    }

    data class Noop(
        private val common: CommonComponent = CommonComponent()
    ) : Entry(), CommonMutable by common {
        override val provider = NoopProvider.id
        init {
            optional = optionalData != null
        }
    }

    companion object : KLogging()

    @Transient
    var optional: Boolean = false// = optionalData != null


    @Transient
    val cleanId: String
        get() = id
            .replace('/', '-')
            .replace("[^\\w-]+".toRegex(), "")

    @Transient
    val serialFilename: String
        get() = "$cleanId.entry.json"

//    @Deprecated("looks suspect")
//    fun serialize(sourceFolder: File) {
//        val file = sourceFolder.resolve(folder).resolve("$cleanId.entry.json").absoluteFile
//        file.absoluteFile.parentFile.mkdirs()
//        file.writeText(json.stringify(Entry.serializer(), this))
//    }

    inline fun <reified E: LockEntry> lock(block: (CommonLockComponent) -> E): E {
        if(optionalData != null) {
            logger.warn { "[$id] optionalData: $optionalData" }
        }
        val commonComponent = CommonLockComponent(
            name = name,
            fileName = fileName,
            side = side,
            description = description,
            optionalData = optionalData,
            dependencies = dependencies.toMap()
        )
        // TODO: fix ugly hacks to make types match
        val lockEntry = block(commonComponent)
        lockEntry.changeId(id)
        lockEntry.folder = File(folder)
        return lockEntry
    }
}
