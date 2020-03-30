package voodoo.data.components

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

data class CommonComponent(
    override var provider: String = "",
    override var id: String = "",
    override var name: String? = null,
    override var folder: String = "mods",
    override var description: String? = null,
    override var optionalData: OptionalData? = null,
    override var side: Side = Side.BOTH,
    override var websiteUrl: String = "",
    override var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
    override var replaceDependencies: Map<ProjectID, ProjectID> = mapOf(),
    override var packageType: PackageType = PackageType.MOD,
    override var transient: Boolean = false, // this entry got added as dependency for something else
    override var version: String = "", // TODO: use regex only ?
    override var fileName: String? = null,
    override var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
    override var validMcVersions: Set<String> = setOf(),
    override var enabled: Boolean = true
) : CommonMutable

data class CurseComponent(
    override var releaseTypes: Set<FileType> = setOf(
        FileType.Release,
        FileType.Beta
    ),
    override var projectID: ProjectID = ProjectID.INVALID,
    override var fileID: FileID = FileID.INVALID,
    override var useUrlTxt: Boolean = true,
    override var skipFingerprintCheck: Boolean = true
) : CurseMutable

data class DirectComponent(
    override var url: String = "",
    override var useUrlTxt: Boolean = true
) : DirectMutable

data class JenkinsComponent(
    override var jenkinsUrl: String = "",
    override var job: String = "",
    override var buildNumber: Int = -1
) : JenkinsMutable

data class LocalComponent(
    override var fileSrc: String = ""
) : LocalMutable

data class UpdateJsonComponent(
    override var updateJson: String = "",
    override var updateChannel: UpdateChannel = UpdateChannel.RECOMMENDED,
    override var template: String = "",
    override var useUrlTxt: Boolean = true
): UpdateJsonMutable