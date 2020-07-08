package voodoo.data.components

import kotlinx.serialization.Transient
import voodoo.data.DependencyType
import voodoo.data.OptionalData
import voodoo.data.Side
import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.curse.ProjectID
import voodoo.data.provider.UpdateChannel


interface CommonImmutable {
    @Transient val provider: String
    val id: String
    val name: String?
    val folder: String
    val description: String?
    val optionalData: OptionalData?
    val side: Side
    val websiteUrl: String
    val dependencies: MutableMap<String, DependencyType>
    val replaceDependencies: Map<ProjectID, ProjectID>
    val packageType: PackageType
    val transient: Boolean // this entry got added as dependency for somethin
    val version: String // TODO: use regex only ?
    val fileName: String?
    val fileNameRegex: String
    val validMcVersions: Set<String>
    val invalidMcVersions: Set<String>
    val enabled: Boolean
}
interface CommonMutable : CommonImmutable {
    override var id: String
    override var name: String?
    override var folder: String
    override var description: String?
    override var optionalData: OptionalData?
    override var side: Side
    override var websiteUrl: String
    override var dependencies: MutableMap<String, DependencyType>
    override var replaceDependencies: Map<ProjectID, ProjectID>
    override var packageType: PackageType
    override var transient: Boolean // this entry got added as dependency for somethin
    override var version: String // TODO: use regex only ?
    override var fileName: String?
    override var fileNameRegex: String
    override var validMcVersions: Set<String>
    override var invalidMcVersions: Set<String>
    override var enabled: Boolean
}

interface CurseImmutable {
    val releaseTypes: Set<FileType>
    val projectID: ProjectID
    val fileID: FileID
    val useUrlTxt: Boolean
    val skipFingerprintCheck: Boolean
}
interface CurseMutable: CurseImmutable {
    override var releaseTypes: Set<FileType>
    override var projectID: ProjectID
    override var fileID: FileID
    override var useUrlTxt: Boolean
    override var skipFingerprintCheck: Boolean
}

interface DirectImmutable {
    val url: String
    val useUrlTxt: Boolean
}
interface DirectMutable: DirectImmutable {
    override var url: String
    override var useUrlTxt: Boolean
}

interface JenkinsImmutable {
    val jenkinsUrl: String
    val job: String
    val buildNumber: Int
}
interface JenkinsMutable: JenkinsImmutable {
    override var jenkinsUrl: String
    override var job: String
    override var buildNumber: Int
}


interface LocalImmutable {
    val fileSrc: String
}
interface LocalMutable: LocalImmutable {
    override var fileSrc: String
}

interface UpdateJsonImmutable {
    val updateJson: String
    val updateChannel: UpdateChannel
    val template: String
    val useUrlTxt: Boolean
}
interface UpdateJsonMutable: UpdateJsonImmutable {
    override var updateJson: String
    override var updateChannel: UpdateChannel
    override var template: String
    override var useUrlTxt: Boolean
}