package voodoo.gui.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.*
import tornadofx.*
import voodoo.data.Side
import voodoo.data.curse.DependencyType
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType
import voodoo.data.flat.Entry
import voodoo.data.flat.EntryFeature
import voodoo.data.provider.UpdateChannel
import voodoo.flatten.data.NestedEntry
import voodoo.gui.extensions.json
import voodoo.provider.Provider

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */

class NestedEntryWrapper(
        entry: NestedEntry,
        @JsonIgnore var parent: NestedEntryWrapper?
) {
//    @JsonIgnore
//    val enabledProperty = SimpleBooleanProperty(enabled)
//    var enabled by enabledProperty

    companion object {
        val DEFAULT = NestedEntry()
    }

    @JsonIgnore
    val providerOverrideProperty = SimpleBooleanProperty(entry.provider != DEFAULT.provider)
    var providerOverride by providerOverrideProperty

    @JsonIgnore
    val providerProperty = object : SimpleStringProperty(entry.provider) {
        override fun get() = if (providerOverride)
            super.get()
        else
            parent?.provider ?: DEFAULT.provider
    }
    var provider: String by providerProperty


    @JsonIgnore
    val nameOverrideProperty = SimpleBooleanProperty(entry.name != DEFAULT.name)
    var nameOverride by nameOverrideProperty

    @JsonIgnore
    val nameProperty = object : SimpleStringProperty(entry.name) {
        override fun get() = if (nameOverride)
            super.get()
        else
            parent?.name ?: DEFAULT.name
    }
    var name: String by nameProperty


    @JsonIgnore
    val folderOverrideProperty = SimpleBooleanProperty(entry.folder != DEFAULT.folder)
    var folderOverride by folderOverrideProperty

    @JsonIgnore
    val folderProperty = object : SimpleStringProperty(entry.folder) {
        override fun get() = if (folderOverride)
            super.get()
        else
            parent?.folder ?: DEFAULT.folder
    }
    var folder: String by folderProperty


    @JsonIgnore
    val commentOverrideProperty = SimpleBooleanProperty(entry.comment != DEFAULT.comment)
    var commentOverride by commentOverrideProperty

    @JsonIgnore
    val commentProperty = object : SimpleStringProperty(entry.comment) {
        override fun get() = if (commentOverride)
            super.get()
        else
            parent?.comment ?: DEFAULT.comment
    }
    var comment: String by commentProperty


    @JsonIgnore
    val descriptionOverrideProperty = SimpleBooleanProperty(entry.description != DEFAULT.description)
    var descriptionOverride by descriptionOverrideProperty

    @JsonIgnore
    val descriptionProperty = object : SimpleStringProperty(entry.description) {
        override fun get() = if (descriptionOverride)
            super.get()
        else
            parent?.description ?: DEFAULT.description
    }
    var description: String by descriptionProperty


    @JsonIgnore
    val featureOverrideProperty = SimpleBooleanProperty(entry.feature != DEFAULT.feature)
    var featureOverride by featureOverrideProperty


    //TODO: use a FeatureWrapper for EntryFeature
    @JsonIgnore
    val featureProperty = object : SimpleObjectProperty<EntryFeature?>(entry.feature) {
        override fun get() = if (featureOverride)
            super.get()
        else
            parent?.feature ?: DEFAULT.feature
    }
    var feature: EntryFeature? by featureProperty


    @JsonIgnore
    val sideOverrideProperty = SimpleBooleanProperty(entry.side != DEFAULT.side)
    var sideOverride by sideOverrideProperty

    @JsonIgnore
    val sideProperty = object : SimpleObjectProperty<Side>(entry.side) {
        override fun get() = if (sideOverride)
            super.get()
        else
            parent?.side ?: DEFAULT.side
    }
    var side: Side by sideProperty


    @JsonIgnore
    val websiteUrlOverrideProperty = SimpleBooleanProperty(entry.websiteUrl != DEFAULT.websiteUrl)
    var websiteUrlOverride by websiteUrlOverrideProperty

    @JsonIgnore
    val websiteUrlProperty = object : SimpleStringProperty(entry.websiteUrl) {
        override fun get() = if (websiteUrlOverride)
            super.get()
        else
            parent?.websiteUrl ?: DEFAULT.websiteUrl
    }
    var websiteUrl: String by websiteUrlProperty


    @JsonIgnore
    val dependenciesOverrideProperty = SimpleBooleanProperty(entry.dependencies != DEFAULT.dependencies)
    var dependenciesOverride by dependenciesOverrideProperty

    @JsonIgnore
    val dependenciesProperty = object : SimpleObjectProperty<MutableMap<DependencyType, List<String>>>(entry.dependencies.observable()) {
        override fun get() = if (dependenciesOverride)
            super.get()
        else
            parent?.dependencies ?: DEFAULT.dependencies.observable()
    }
    var dependencies: MutableMap<DependencyType, List<String>> by dependenciesProperty


    @JsonIgnore
    val packageTypeOverrideProperty = SimpleBooleanProperty(entry.packageType != DEFAULT.packageType)
    var packageTypeOverride by packageTypeOverrideProperty

    @JsonIgnore
    val packageTypeProperty = object : SimpleObjectProperty<PackageType>(entry.packageType) {
        override fun get() = if (packageTypeOverride)
            super.get()
        else
            parent?.packageType ?: DEFAULT.packageType
    }
    var packageType: PackageType by packageTypeProperty


    @JsonIgnore
    val versionOverrideProperty = SimpleBooleanProperty(entry.version != DEFAULT.version)
    var versionOverride by versionOverrideProperty

    @JsonIgnore
    val versionProperty = object : SimpleStringProperty(entry.version) {
        override fun get() = if (versionOverride)
            super.get()
        else
            parent?.version ?: DEFAULT.version
    }
    var version: String by versionProperty


    @JsonIgnore
    val fileNameOverrideProperty = SimpleBooleanProperty(entry.fileName != DEFAULT.fileName)
    var fileNameOverride by fileNameOverrideProperty

    @JsonIgnore
    val fileNameProperty = object : SimpleStringProperty(entry.fileName) {
        override fun get() = if (fileNameOverride)
            super.get()
        else
            parent?.fileName ?: DEFAULT.fileName
    }
    var fileName: String by fileNameProperty


    @JsonIgnore
    val fileNameRegexOverrideProperty = SimpleBooleanProperty(entry.fileNameRegex != DEFAULT.fileNameRegex)
    var fileNameRegexOverride by fileNameRegexOverrideProperty

    @JsonIgnore
    val fileNameRegexProperty = object : SimpleStringProperty(entry.fileNameRegex) {
        override fun get() = if (fileNameRegexOverride)
            super.get()
        else
            parent?.fileNameRegex ?: DEFAULT.fileNameRegex
    }
    var fileNameRegex: String by fileNameRegexProperty


    @JsonIgnore
    val validMcVersionsOverrideProperty = SimpleBooleanProperty(entry.validMcVersions != DEFAULT.validMcVersions)
    var validMcVersionsOverride by validMcVersionsOverrideProperty

    @JsonIgnore
    val validMcVersionsProperty = object : SimpleListProperty<String>(entry.validMcVersions.observable()) {
        override fun get() = if (validMcVersionsOverride)
            super.get()
        else
            parent?.validMcVersions?.observable() ?: DEFAULT.validMcVersions.observable()
    }
    var validMcVersions: MutableList<String> by validMcVersionsProperty


    @JsonIgnore
    val curseMetaUrlOverrideProperty = SimpleBooleanProperty(entry.curseMetaUrl != DEFAULT.curseMetaUrl)
    var curseMetaUrlOverride by curseMetaUrlOverrideProperty

    @JsonIgnore
    val curseMetaUrlProperty = object : SimpleStringProperty(entry.curseMetaUrl) {
        override fun get() = if (curseMetaUrlOverride)
            super.get()
        else
            parent?.curseMetaUrl ?: DEFAULT.curseMetaUrl
    }
    var curseMetaUrl: String by curseMetaUrlProperty

    @JsonIgnore
    val curseReleaseTypesOverrideProperty = SimpleBooleanProperty(entry.curseReleaseTypes != DEFAULT.curseReleaseTypes)
    var curseReleaseTypesOverride by curseReleaseTypesOverrideProperty

    @JsonIgnore
    val curseReleaseTypesProperty = object : SimpleSetProperty<FileType>(entry.curseReleaseTypes.observable()) {
        override fun get() = if (curseReleaseTypesOverride)
            super.get()
        else
            parent?.curseReleaseTypes?.observable() ?: DEFAULT.curseReleaseTypes.observable()
    }
    var curseReleaseTypes: Set<FileType> by curseReleaseTypesProperty


    @JsonIgnore
    val curseOptionalDependenciesOverrideProperty = SimpleBooleanProperty(entry.curseOptionalDependencies != DEFAULT.curseOptionalDependencies)
    var curseOptionalDependenciesOverride by curseOptionalDependenciesOverrideProperty

    @JsonIgnore
    val curseOptionalDependenciesProperty = object : SimpleBooleanProperty(entry.curseOptionalDependencies) {
        override fun get() = if (curseOptionalDependenciesOverride)
            super.get()
        else
            parent?.curseOptionalDependencies ?: DEFAULT.curseOptionalDependencies
    }
    var curseOptionalDependencies: Boolean by curseOptionalDependenciesProperty


    @JsonIgnore
    val urlOverrideProperty = SimpleBooleanProperty(entry.url != DEFAULT.url)
    var urlOverride by urlOverrideProperty

    @JsonIgnore
    val urlProperty = object : SimpleStringProperty(entry.url) {
        override fun get() = if (urlOverride)
            super.get()
        else
            parent?.url ?: DEFAULT.url
    }
    var url: String by urlProperty


    @JsonIgnore
    val useUrlTxtOverrideProperty = SimpleBooleanProperty(entry.useUrlTxt != DEFAULT.useUrlTxt)
    var useUrlTxtOverride by useUrlTxtOverrideProperty

    @JsonIgnore
    val useUrlTxtProperty = object : SimpleBooleanProperty(entry.useUrlTxt) {
        override fun get() = if (useUrlTxtOverride)
            super.get()
        else
            parent?.useUrlTxt ?: DEFAULT.useUrlTxt
    }
    var useUrlTxt: Boolean by useUrlTxtProperty


    @JsonIgnore
    val jenkinsUrlOverrideProperty = SimpleBooleanProperty(entry.jenkinsUrl != DEFAULT.jenkinsUrl)
    var jenkinsUrlOverride by jenkinsUrlOverrideProperty

    @JsonIgnore
    val jenkinsUrlProperty = object : SimpleStringProperty(entry.jenkinsUrl) {
        override fun get() = if (jenkinsUrlOverride)
            super.get()
        else
            parent?.jenkinsUrl ?: DEFAULT.jenkinsUrl
    }
    var jenkinsUrl: String by jenkinsUrlProperty


    @JsonIgnore
    val jobOverrideProperty = SimpleBooleanProperty(entry.job != DEFAULT.job)
    var jobOverride by jobOverrideProperty

    @JsonIgnore
    val jobProperty = object : SimpleStringProperty(entry.job) {
        override fun get() = if (jobOverride)
            super.get()
        else
            parent?.job ?: DEFAULT.job
    }
    var job: String by jobProperty


    @JsonIgnore
    val buildNumberOverrideProperty = SimpleBooleanProperty(entry.buildNumber != DEFAULT.buildNumber)
    var buildNumberOverride by buildNumberOverrideProperty

    @JsonIgnore
    val buildNumberProperty = object : SimpleIntegerProperty(entry.buildNumber) {
        override fun get() = if (buildNumberOverride)
            super.get()
        else
            parent?.buildNumber ?: DEFAULT.buildNumber
    }
    var buildNumber: Int by buildNumberProperty


    @JsonIgnore
    val fileSrcOverrideProperty = SimpleBooleanProperty(entry.fileSrc != DEFAULT.fileSrc)
    var fileSrcOverride by fileSrcOverrideProperty

    @JsonIgnore
    val fileSrcProperty = object : SimpleStringProperty(entry.fileSrc) {
        override fun get() = if (fileSrcOverride)
            super.get()
        else
            parent?.fileSrc ?: DEFAULT.fileSrc
    }
    var fileSrc: String by fileSrcProperty


    @JsonIgnore
    val updateJsonOverrideProperty = SimpleBooleanProperty(entry.updateJson != DEFAULT.updateJson)
    var updateJsonOverride by updateJsonOverrideProperty

    @JsonIgnore
    val updateJsonProperty = object : SimpleStringProperty(entry.updateJson) {
        override fun get() = if (updateJsonOverride)
            super.get()
        else
            parent?.updateJson ?: DEFAULT.updateJson
    }
    var updateJson: String by updateJsonProperty


    @JsonIgnore
    val updateChannelOverrideProperty = SimpleBooleanProperty(entry.updateChannel != DEFAULT.updateChannel)
    var updateChannelOverride by updateChannelOverrideProperty

    @JsonIgnore
    val updateChannelProperty = object : SimpleObjectProperty<UpdateChannel>(entry.updateChannel) {
        override fun get() = if (updateChannelOverride)
            super.get()
        else
            parent?.updateChannel ?: DEFAULT.updateChannel
    }
    var updateChannel: UpdateChannel by updateChannelProperty


    @JsonIgnore
    val templateOverrideProperty = SimpleBooleanProperty(entry.template != DEFAULT.template)
    var templateOverride by templateOverrideProperty

    @JsonIgnore
    val templateProperty = object : SimpleStringProperty(entry.template) {
        override fun get() = if (templateOverride)
            super.get()
        else
            parent?.template ?: DEFAULT.template
    }
    var template: String by templateProperty


    //TODO: maybe switch to check if non-empty
    @JsonIgnore
    val entriesOverrideProperty = SimpleBooleanProperty(entry.entries != DEFAULT.entries)
    var entriesOverride by entriesOverrideProperty

    @JsonIgnore
    val entriesProperty = SimpleListProperty<NestedEntryWrapper>(entry.entries.map { NestedEntryWrapper(it, this) }.observable())
    var entries: MutableList<NestedEntryWrapper> by entriesProperty


    // OLD CODE

//    @JsonIgnore
//    val nameProperty = SimpleStringProperty(entry.name)
//    var name by nameProperty
//
//    @JsonIgnore
//    val folderProperty = SimpleStringProperty(entry.folder)
//    var folder by folderProperty
//
//    @JsonIgnore
//    val commentProperty = SimpleStringProperty(entry.comment)
//    var comment by commentProperty
//
//    @JsonIgnore
//    val descriptionProperty = SimpleStringProperty(entry.description)
//    var description by descriptionProperty
//
//    //TODO: use a FeatureWrapper for EntryFeature
//    @JsonIgnore
//    val featureProperty = SimpleObjectProperty(entry.feature)
//    var feature by featureProperty
//
//    @JsonIgnore
//    val sideProperty = SimpleObjectProperty(entry.side)
//    var side by sideProperty
//
//    @JsonIgnore
//    val websiteUrlProperty = SimpleStringProperty(entry.websiteUrl)
//    var websiteUrl by websiteUrlProperty
//
//    @JsonIgnore
//    val dependenciesProperty = SimpleMapProperty(entry.dependencies.observable())
//    var dependencies by dependenciesProperty
//
//    @JsonIgnore
//    val packageTypeProperty = SimpleObjectProperty<PackageType>(entry.packageType)
//    var packageType by packageTypeProperty
//
//    @JsonIgnore
//    val versionProperty = SimpleStringProperty(entry.version)
//    var version by versionProperty
//
//    @JsonIgnore
//    val fileNameProperty = SimpleStringProperty(entry.fileName)
//    var fileName: String? by fileNameProperty
//
//    @JsonIgnore
//    val fileNameRegexProperty = SimpleStringProperty(entry.fileNameRegex)
//    var fileNameRegex by fileNameRegexProperty
//
//    @JsonIgnore
//    val validMcVersionsProperty = SimpleListProperty<String>(entry.validMcVersions.observable())
//    var validMcVersions by validMcVersionsProperty
//
//    @JsonIgnore
//    val curseMetaUrlProperty = SimpleStringProperty(entry.curseMetaUrl)
//    var curseMetaUrl by curseMetaUrlProperty
//
//    @JsonIgnore
//    val curseReleaseTypesProperty = SimpleSetProperty<FileType>(entry.curseReleaseTypes.observable())
//    var curseReleaseTypes by curseReleaseTypesProperty
//
//    @JsonIgnore
//    val curseOptionalDependenciesProperty = SimpleBooleanProperty(entry.curseOptionalDependencies)
//    var curseOptionalDependencies by curseOptionalDependenciesProperty
//
//    @JsonIgnore
//    val urlProperty = SimpleStringProperty(entry.url)
//    var url by urlProperty
//
//    @JsonIgnore
//    val useUrlTxtProperty = SimpleBooleanProperty(entry.useUrlTxt)
//    var useUrlTxt by useUrlTxtProperty
//
//    @JsonIgnore
//    val jenkinsUrlProperty = SimpleStringProperty(entry.jenkinsUrl)
//    var jenkinsUrl by jenkinsUrlProperty
//
//    @JsonIgnore
//    val jobProperty = SimpleStringProperty(entry.job)
//    var job by jobProperty
//
//    @JsonIgnore
//    val buildNumberProperty = SimpleIntegerProperty(entry.buildNumber)
//    var buildNumber by buildNumberProperty
//
//    @JsonIgnore
//    val fileSrcProperty = SimpleStringProperty(entry.fileSrc)
//    var fileSrc by fileSrcProperty
//
//    @JsonIgnore
//    val updateJsonProperty = SimpleStringProperty(entry.updateJson)
//    var updateJson by updateJsonProperty
//
//    @JsonIgnore
//    val updateChannelProperty = SimpleObjectProperty<UpdateChannel>(entry.updateChannel)
//    var updateChannel by updateChannelProperty
//
//    @JsonIgnore
//    val templateProperty = SimpleStringProperty(entry.template)
//    var template by templateProperty
//
//    // load NestedEntryWrapper
//    @JsonIgnore
//    val entriesProperty = SimpleListProperty<NestedEntryWrapper>(entry.entries.map { NestedEntryWrapper(it, this) }.observable())
//    var entries by entriesProperty

    // Extra properties

    @JsonIgnore
    val providerObjProperty = object : SimpleObjectProperty<Provider>() {
        override fun get() = voodoo.provider.Provider.valueOf(provider)
    }
    val providerObj: Provider by providerObjProperty

    @JsonIgnore
    val thumbnailProperty = object : SimpleStringProperty() {
        override fun get(): String = providerObj.base.getThumbnail(flatEntry)
    }
//                    .takeUnless { it.isBlank() }
//                    ?: "https://edb-cdn2-prod-tqgiyve.stackpathdns.com/teams/logos/56b9239c-ca95-11e7-b012-0ec39221f676.png"

    val thumbnail by thumbnailProperty


    fun flatCount(): Int {
        if (entries.size > 0) {
            return entries.sumBy { it.flatCount() }
        } else {
            return 1
        }
    }

    override fun toString(): String {
        return "NestedEntryWrapper(${this.json})"
    }

    val entry: NestedEntry
        get() {
            val wrapper = this
            return NestedEntry(
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
                    template = template,
                    entries = entries.map { it.entry }
            ).apply {
                if (providerOverride) provider = wrapper.provider
                if (nameOverride) name = wrapper.name
                if (folderOverride) folder = wrapper.folder
                if (commentOverride) comment = wrapper.comment
                if (descriptionOverride) description = wrapper.description
                if (featureOverride) feature = wrapper.feature
                if (sideOverride) side = wrapper.side
                if (websiteUrlOverride) websiteUrl = wrapper.websiteUrl
                if (dependenciesOverride) dependencies = wrapper.dependencies
                if (packageTypeOverride) packageType = wrapper.packageType
                if (versionOverride) version = wrapper.version
                if (fileNameOverride) fileName = wrapper.fileName
                if (fileNameRegexOverride) fileNameRegex = wrapper.fileNameRegex
                if (validMcVersionsOverride) validMcVersions = wrapper.validMcVersions
                if (curseMetaUrlOverride) curseMetaUrl = wrapper.curseMetaUrl
                if (curseReleaseTypesOverride) curseReleaseTypes = wrapper.curseReleaseTypes
                if (curseOptionalDependenciesOverride) curseOptionalDependencies = wrapper.curseOptionalDependencies
                if (urlOverride) url = wrapper.url
                if (useUrlTxtOverride) useUrlTxt = wrapper.useUrlTxt
                if (jenkinsUrlOverride) jenkinsUrl = wrapper.jenkinsUrl
                if (jobOverride) job = wrapper.job
                if (buildNumberOverride) buildNumber = wrapper.buildNumber
                if (fileSrcOverride) fileSrc = wrapper.fileSrc
                if (updateJsonOverride) updateJson = wrapper.updateJson
                if (updateChannelOverride) updateChannel = wrapper.updateChannel
                if (templateOverride) template = wrapper.template
                if (entriesOverride) entries = wrapper.entries.map { it.entry }
            }
        }

    val flatEntry: Entry
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

class NestedEntryModel : ItemViewModel<NestedEntryWrapper>() {
    //    val enabled = bind(NestedEntryWrapper::enabledProperty)
    val provider = bind(NestedEntryWrapper::providerProperty)
    val name = bind(NestedEntryWrapper::nameProperty)
    val folder = bind(NestedEntryWrapper::folderProperty)
    val comment = bind(NestedEntryWrapper::commentProperty)
    val description = bind(NestedEntryWrapper::descriptionProperty)
    val feature = bind(NestedEntryWrapper::featureProperty)
    val side = bind(NestedEntryWrapper::sideProperty)
    val websiteUrl = bind(NestedEntryWrapper::websiteUrlProperty)
    val dependencies = bind(NestedEntryWrapper::dependenciesProperty)
    val packageType = bind(NestedEntryWrapper::packageTypeProperty)
    val version = bind(NestedEntryWrapper::versionProperty)
    val fileName = bind(NestedEntryWrapper::fileNameProperty)
    val fileNameRegex = bind(NestedEntryWrapper::fileNameRegexProperty)
    val validMcVersions = bind(NestedEntryWrapper::validMcVersionsProperty)
    val curseMetaUrl = bind(NestedEntryWrapper::curseMetaUrlProperty)
    val curseReleaseTypes = bind(NestedEntryWrapper::curseReleaseTypesProperty)
    val curseOptionalDependencies = bind(NestedEntryWrapper::curseOptionalDependenciesProperty)
    val url = bind(NestedEntryWrapper::urlProperty)
    val useUrlTxt = bind(NestedEntryWrapper::useUrlTxtProperty)
    val jenkinsUrl = bind(NestedEntryWrapper::jenkinsUrlProperty)
    val job = bind(NestedEntryWrapper::jobProperty)
    val buildNumber = bind(NestedEntryWrapper::buildNumberProperty)
    val fileSrc = bind(NestedEntryWrapper::fileSrcProperty)
    val updateJson = bind(NestedEntryWrapper::updateJsonProperty)
    val updateChannel = bind(NestedEntryWrapper::updateChannelProperty)
    val template = bind(NestedEntryWrapper::templateProperty)
    val entries = bind(NestedEntryWrapper::entriesProperty)

    // override properties
    val providerOverride = bind(NestedEntryWrapper::providerOverrideProperty)
    val nameOverride = bind(NestedEntryWrapper::nameOverrideProperty)
    val folderOverride = bind(NestedEntryWrapper::folderOverrideProperty)
    val commentOverride = bind(NestedEntryWrapper::commentOverrideProperty)
    val descriptionOverride = bind(NestedEntryWrapper::descriptionOverrideProperty)
    val featureOverride = bind(NestedEntryWrapper::featureOverrideProperty)
    val sideOverride = bind(NestedEntryWrapper::sideOverrideProperty)
    val websiteUrlOverride = bind(NestedEntryWrapper::websiteUrlOverrideProperty)
    val dependenciesOverride = bind(NestedEntryWrapper::dependenciesOverrideProperty)
    val packageTypeOverride = bind(NestedEntryWrapper::packageTypeOverrideProperty)
    val versionOverride = bind(NestedEntryWrapper::versionOverrideProperty)
    val fileNameOverride = bind(NestedEntryWrapper::fileNameOverrideProperty)
    val fileNameRegexOverride = bind(NestedEntryWrapper::fileNameRegexOverrideProperty)
    val validMcVersionsOverride = bind(NestedEntryWrapper::validMcVersionsOverrideProperty)
    val curseMetaUrlOverride = bind(NestedEntryWrapper::curseMetaUrlOverrideProperty)
    val curseReleaseTypesOverride = bind(NestedEntryWrapper::curseReleaseTypesOverrideProperty)
    val curseOptionalDependenciesOverride = bind(NestedEntryWrapper::curseOptionalDependenciesOverrideProperty)
    val urlOverride = bind(NestedEntryWrapper::urlOverrideProperty)
    val useUrlTxtOverride = bind(NestedEntryWrapper::useUrlTxtOverrideProperty)
    val jenkinsUrlOverride = bind(NestedEntryWrapper::jenkinsUrlOverrideProperty)
    val jobOverride = bind(NestedEntryWrapper::jobOverrideProperty)
    val buildNumberOverride = bind(NestedEntryWrapper::buildNumberOverrideProperty)
    val fileSrcOverride = bind(NestedEntryWrapper::fileSrcOverrideProperty)
    val updateJsonOverride = bind(NestedEntryWrapper::updateJsonOverrideProperty)
    val updateChannelOverride = bind(NestedEntryWrapper::updateChannelOverrideProperty)
    val templateOverride = bind(NestedEntryWrapper::templateOverrideProperty)
    val entriesOverride = bind(NestedEntryWrapper::entriesOverrideProperty)


//    val forceRebuild = bind(NestedEntryWrapper::forceRebuildProperty)

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