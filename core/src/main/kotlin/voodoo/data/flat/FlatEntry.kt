package voodoo.data.flat

import kotlinx.serialization.Transient
import mu.KotlinLogging
import voodoo.data.components.*
import voodoo.data.lock.CommonLockComponent

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */


sealed class FlatEntry: CommonMutable {
    data class Common(
        val common: CommonComponent = CommonComponent()
    ) : FlatEntry(), CommonMutable by common {
        init {
            optional = optionalData != null
        }
    }

    data class Curse(
        private val common: CommonComponent = CommonComponent(),
        private val curse: CurseComponent = CurseComponent()
    ) : FlatEntry(), CommonMutable by common, CurseMutable by curse {
        init {
            optional = optionalData != null
        }
    }

    data class Direct(
        private val common: CommonComponent = CommonComponent(),
        private val direct: DirectComponent = DirectComponent()
    ) : FlatEntry(), CommonMutable by common, DirectMutable by direct {
        init {
            optional = optionalData != null
        }
    }

    data class Jenkins(
        private val common: CommonComponent = CommonComponent(),
        private val jenkins: JenkinsComponent = JenkinsComponent()
    ) : FlatEntry(), CommonMutable by common, JenkinsMutable by jenkins {
        init {
            optional = optionalData != null
        }
    }

    data class Local(
        private val common: CommonComponent = CommonComponent(),
        private val local: LocalComponent = LocalComponent()
    ) : FlatEntry(), CommonMutable by common, LocalMutable by local {
        init {
            optional = optionalData != null
        }
    }

    data class Noop(
        private val common: CommonComponent = CommonComponent()
    ) : FlatEntry(), CommonMutable by common {
        init {
            optional = optionalData != null
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

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

    fun lockCommon(): CommonLockComponent {
        if(optionalData != null) {
            logger.warn { "[$id] optionalData: $optionalData" }
        }
        return CommonLockComponent(
            id = id,
            path = folder ?: "mods",
            name = name,
            fileName = fileName,
            side = side,
            description = description,
            optionalData = optionalData,
            dependencies = dependencies.toMap()
        )
    }
}
