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

class EntryWrapper(entry: Entry, val modpack: ModPack) {
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
    val validMCVersionsProperty = SimpleListProperty<String>(entry.validMcVersions.observable())
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
        return "EntryWrapper(${this.json})"
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

class EntryModel : ItemViewModel<EntryWrapper>() {
    //    val enabled = bind(EntryWrapper::enabledProperty)
    val provider = bind(EntryWrapper::providerProperty)
    val name = bind(EntryWrapper::nameProperty)
    val folder = bind(EntryWrapper::folderProperty)
    val comment = bind(EntryWrapper::commentProperty)
    val description = bind(EntryWrapper::descriptionProperty)
    val feature = bind(EntryWrapper::featureProperty)
    val side = bind(EntryWrapper::sideProperty)
    val websiteUrl = bind(EntryWrapper::websiteUrlProperty)
    val dependencies = bind(EntryWrapper::dependenciesProperty)
    val packageType = bind(EntryWrapper::packageTypeProperty)
    val version = bind(EntryWrapper::versionProperty)
    val fileName = bind(EntryWrapper::fileNameProperty)
    val fileNameRegex = bind(EntryWrapper::fileNameRegexProperty)
    val validMcVersions = bind(EntryWrapper::validMCVersionsProperty)
    val curseMetaUrl = bind(EntryWrapper::curseMetaUrlProperty)
    val curseReleaseTypes = bind(EntryWrapper::curseReleaseTypesProperty)
    val curseOptionalDependencies = bind(EntryWrapper::curseOptionalDependenciesProperty)
    val url = bind(EntryWrapper::urlProperty)
    val useUrlTxt = bind(EntryWrapper::useUrlTxtProperty)
    val jenkinsUrl = bind(EntryWrapper::jenkinsUrlProperty)
    val job = bind(EntryWrapper::jobProperty)
    val buildNumber = bind(EntryWrapper::buildNumberProperty)
    val fileSrc = bind(EntryWrapper::fileSrcProperty)
    val updateJson = bind(EntryWrapper::updateJsonProperty)
    val updateChannel = bind(EntryWrapper::updateChannelProperty)
    val template = bind(EntryWrapper::templateProperty)

    val thumbnail = bind(EntryWrapper::thumbnailProperty)

//    val forceRebuild = bind(EntryWrapper::forceRebuildProperty)

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