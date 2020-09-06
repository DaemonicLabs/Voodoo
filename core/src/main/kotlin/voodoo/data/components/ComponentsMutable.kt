package voodoo.data.components

import kotlinx.serialization.Serializable
import voodoo.data.DependencyType
import voodoo.data.OptionalData
import voodoo.data.Side
import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.curse.ProjectID
import voodoo.data.provider.UpdateChannel

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@Serializable
data class CommonComponent(
    @Deprecated("use type checks instead of a stringtype thing, will be replaced with a constant in each specialized component")
    @Transient override var provider: String = "",
    override var id: String = "",
    override var name: String? = null,
    override var folder: String? = null,
    override var description: String? = null,
    override var optionalData: OptionalData? = null,
    override var side: Side = Side.BOTH,
    override var websiteUrl: String = "",
    override var dependencies: MutableMap<String, DependencyType> = mutableMapOf(),
    override var replaceDependencies: Map<ProjectID, ProjectID> = mapOf(),
    override var packageType: PackageType = PackageType.MOD,
    override var transient: Boolean = false, // this entry got added as dependency for something else
    override var version: String = "", // TODO: use regex only ?
    override var fileName: String? = null,
    override var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
    override var validMcVersions: Set<String> = setOf(),
    override var invalidMcVersions: Set<String> = setOf(),
    override var enabled: Boolean = true
) : CommonMutable

@Serializable
data class CurseComponent(
    override var releaseTypes: Set<FileType> = setOf(
        FileType.Release,
        FileType.Beta
    ),
    override var projectID: ProjectID = ProjectID.INVALID,
    override var fileID: FileID = FileID.INVALID,
    override var useUrlTxt: Boolean = true,
    override var skipFingerprintCheck: Boolean = false
) : CurseMutable

@Serializable
data class DirectComponent(
    override var url: String = "",
    override var useUrlTxt: Boolean = true
) : DirectMutable

@Serializable
data class JenkinsComponent(
    override var jenkinsUrl: String = "",
    override var job: String = "",
    override var buildNumber: Int = -1
) : JenkinsMutable

@Serializable
data class LocalComponent(
    override var fileSrc: String = ""
) : LocalMutable
