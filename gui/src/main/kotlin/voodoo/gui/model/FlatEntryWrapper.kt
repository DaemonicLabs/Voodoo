package voodoo.gui.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.*
import tornadofx.*
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.provider.UpdateChannel
import voodoo.gui.extensions.json

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */

class FlatEntryWrapper(entry: Entry, val modpack: ModPack) {
//    @JsonIgnore
//    val enabledProperty = SimpleBooleanProperty(enabled)
//    var enabled by enabledProperty

    @JsonIgnore
    val providerProperty = SimpleStringProperty(entry.provider)
    var provider by providerProperty

    @JsonIgnore
    val nameProperty = SimpleStringProperty(entry.name)
    var name by nameProperty

    @JsonIgnore
    val folderProperty = SimpleStringProperty(entry.folder)
    var folder by folderProperty

    @JsonIgnore
    val commentProperty = SimpleStringProperty(entry.comment)
    var comment by commentProperty

    @JsonIgnore
    val descriptionProperty = SimpleStringProperty(entry.description)
    var description by descriptionProperty

    //TODO: use a FeatureWrapper for EntryFeature
    @JsonIgnore
    val featureProperty = SimpleObjectProperty(entry.feature)
    var feature by featureProperty

    @JsonIgnore
    val sideProperty = SimpleObjectProperty(entry.side)
    var side by sideProperty

    @JsonIgnore
    val websiteUrlProperty = SimpleStringProperty(entry.websiteUrl)
    var websiteUrl by websiteUrlProperty

    @JsonIgnore
    val dependenciesProperty = SimpleMapProperty(entry.dependencies.observable())
    var dependencies by dependenciesProperty

    @JsonIgnore
    val packageTypeProperty = SimpleObjectProperty<PackageType>(entry.packageType)
    var packageType by packageTypeProperty

    @JsonIgnore
    val versionProperty = SimpleStringProperty(entry.version)
    var version by versionProperty

    @JsonIgnore
    val fileNameProperty = SimpleStringProperty(entry.fileName)
    var fileName: String? by fileNameProperty

    @JsonIgnore
    val fileNameRegexProperty = SimpleStringProperty(entry.fileNameRegex)
    var fileNameRegex by fileNameRegexProperty

    @JsonIgnore
    val validMCVersionsProperty = SimpleSetProperty<String>(entry.validMcVersions.observable())
    var validMcVersions by validMCVersionsProperty

    @JsonIgnore
    val curseMetaUrlProperty = SimpleStringProperty(entry.curseMetaUrl)
    var curseMetaUrl by curseMetaUrlProperty

    @JsonIgnore
    val curseReleaseTypesProperty = SimpleSetProperty<FileType>(entry.curseReleaseTypes.observable())
    var curseReleaseTypes by curseReleaseTypesProperty

    @JsonIgnore
    val curseOptionalDependenciesProperty = SimpleBooleanProperty(entry.curseOptionalDependencies)
    var curseOptionalDependencies by curseOptionalDependenciesProperty

    @JsonIgnore
    val urlProperty = SimpleStringProperty(entry.url)
    var url by urlProperty

    @JsonIgnore
    val useUrlTxtProperty = SimpleBooleanProperty(entry.useUrlTxt)
    var useUrlTxt by useUrlTxtProperty

    @JsonIgnore
    val jenkinsUrlProperty = SimpleStringProperty(entry.jenkinsUrl)
    var jenkinsUrl by jenkinsUrlProperty

    @JsonIgnore
    val jobProperty = SimpleStringProperty(entry.job)
    var job by jobProperty

    @JsonIgnore
    val buildNumberProperty = SimpleIntegerProperty(entry.buildNumber)
    var buildNumber by buildNumberProperty

    @JsonIgnore
    val fileSrcProperty = SimpleStringProperty(entry.fileSrc)
    var fileSrc by fileSrcProperty

    @JsonIgnore
    val updateJsonProperty = SimpleStringProperty(entry.updateJson)
    var updateJson by updateJsonProperty

    @JsonIgnore
    val updateChannelProperty = SimpleObjectProperty<UpdateChannel>(entry.updateChannel)
    var updateChannel by updateChannelProperty

    @JsonIgnore
    val templateProperty = SimpleStringProperty(entry.template)
    var template by templateProperty


    val providerObj = voodoo.provider.Provider.valueOf(provider)
    val thumbnailProperty = SimpleStringProperty(
            providerObj.base.getThumbnail(entry)
                    .takeUnless { it.isBlank() } ?: "https://edb-cdn2-prod-tqgiyve.stackpathdns.com/teams/logos/56b9239c-ca95-11e7-b012-0ec39221f676.png"
    )
    val thumbnail by thumbnailProperty

    init {
    }

    override fun toString(): String {
        return "FlatEntryWrapper(${this.json})"
    }

    val entry: Entry
        get() {
            return Entry(
                    provider = provider,
                    name = name,
                    folder = folder,
                    comment = comment,
                    description = description,
                    feature = feature,
                    side = side,
                    websiteUrl = websiteUrl,
                    dependencies = dependencies,
                    packageType = packageType,
                    version = version,
                    fileName = fileName,
                    fileNameRegex = fileNameRegex,
                    validMcVersions = validMcVersions,
                    curseMetaUrl = curseMetaUrl,
                    curseReleaseTypes = curseReleaseTypes,
                    curseOptionalDependencies = curseOptionalDependencies,
                    url = url,
                    useUrlTxt = useUrlTxt,
                    jenkinsUrl = jenkinsUrl,
                    job = job,
                    buildNumber = buildNumber,
                    fileSrc = fileSrc,
                    updateJson = updateJson,
                    updateChannel = updateChannel,
                    template = template
            )
        }
}

class EntryModel : ItemViewModel<FlatEntryWrapper>() {
    //    val enabled = bind(FlatEntryWrapper::enabledProperty)
    val provider = bind(FlatEntryWrapper::providerProperty)
    val name = bind(FlatEntryWrapper::nameProperty)
    val folder = bind(FlatEntryWrapper::folderProperty)
    val comment = bind(FlatEntryWrapper::commentProperty)
    val description = bind(FlatEntryWrapper::descriptionProperty)
    val feature = bind(FlatEntryWrapper::featureProperty)
    val side = bind(FlatEntryWrapper::sideProperty)
    val websiteUrl = bind(FlatEntryWrapper::websiteUrlProperty)
    val dependencies = bind(FlatEntryWrapper::dependenciesProperty)
    val packageType = bind(FlatEntryWrapper::packageTypeProperty)
    val version = bind(FlatEntryWrapper::versionProperty)
    val fileName = bind(FlatEntryWrapper::fileNameProperty)
    val fileNameRegex = bind(FlatEntryWrapper::fileNameRegexProperty)
    val validMcVersions = bind(FlatEntryWrapper::validMCVersionsProperty)
    val curseMetaUrl = bind(FlatEntryWrapper::curseMetaUrlProperty)
    val curseReleaseTypes = bind(FlatEntryWrapper::curseReleaseTypesProperty)
    val curseOptionalDependencies = bind(FlatEntryWrapper::curseOptionalDependenciesProperty)
    val url = bind(FlatEntryWrapper::urlProperty)
    val useUrlTxt = bind(FlatEntryWrapper::useUrlTxtProperty)
    val jenkinsUrl = bind(FlatEntryWrapper::jenkinsUrlProperty)
    val job = bind(FlatEntryWrapper::jobProperty)
    val buildNumber = bind(FlatEntryWrapper::buildNumberProperty)
    val fileSrc = bind(FlatEntryWrapper::fileSrcProperty)
    val updateJson = bind(FlatEntryWrapper::updateJsonProperty)
    val updateChannel = bind(FlatEntryWrapper::updateChannelProperty)
    val template = bind(FlatEntryWrapper::templateProperty)

    val thumbnail = bind(FlatEntryWrapper::thumbnailProperty)

//    val forceRebuild = bind(FlatEntryWrapper::forceRebuildProperty)

//    override fun onCommit(commits: List<Commit>) {
//        commits.findChanged(name)?.let { println("Name changed from ${it.first} to ${it.second}")}
//        commits.findChanged(provider)?.let { println("Provider changed from ${it.first} to ${it.second}")}
//        commits.findChanged(side)?.let { println("Side changed from ${it.first} to ${it.second}")}
//        commits.findChanged(id)?.let { println("ID changed from ${it.first} to ${it.second}")}
//
//        onCommit()
//    }

    private fun <T> List<Commit>.findChanged(ref: Property<T>): Pair<T, T>? {
        val commit = find { it.property == ref && it.changed }
        return commit?.let { (it.newValue as T) to (it.oldValue as T) }
    }


    override fun onCommit() {
        println("onCommit()")
//        item.compile()
    }

}