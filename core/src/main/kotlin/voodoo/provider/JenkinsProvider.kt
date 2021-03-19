package voodoo.provider

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import voodoo.core.GeneratedConstants
import voodoo.data.EntryReportData
import voodoo.data.Quadruple
import voodoo.data.flat.FlatEntry
import voodoo.data.flat.FlatModPack
import voodoo.data.lock.LockEntry
import voodoo.memoize
import voodoo.util.download
import voodoo.util.jenkins.Artifact
import voodoo.util.jenkins.BuildWithDetails
import voodoo.util.jenkins.JenkinsServer
import voodoo.util.jenkins.Job
import java.io.File
import java.time.Instant
import java.util.*

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object JenkinsProvider : ProviderBase("Jenkins Provider") {
    const val useragent = "voodoo/${GeneratedConstants.FULL_VERSION} (https://github.com/DaemonicLabs/Voodoo)"

    override suspend fun resolve(
        entry: FlatEntry,
        modPack: FlatModPack,
        addEntry: SendChannel<FlatEntry>
    ): LockEntry {
        entry as FlatEntry.Jenkins
        require(entry.job.isNotBlank()) { "entry: '${entry.id}' does not have the jenkins job set" }
//        if (entry.job.isBlank()) {
//            entry.job = entry.id
//        }
        val job = job(entry.job, entry.jenkinsUrl)
        val buildNumber = job.lastSuccessfulBuild?.number ?: throw IllegalStateException("buildnumber not set")
        val common = entry.lockCommon()
        val artifact = artifact(entry.job, entry.jenkinsUrl, buildNumber, entry.fileNameRegex)
        return LockEntry.Jenkins(
            id = common.id,
            path = common.path,
            name = common.name,
            fileName = common.fileName,
            side = common.side,
            description = common.description,
            optionalData = common.optionalData,
            dependencies = common.dependencies,
            jenkinsUrl = entry.jenkinsUrl,
            job = entry.job,
            buildNumber = buildNumber,
            fileNameRegex = entry.fileNameRegex,
            artifactRelativePath = artifact.relativePath,
            artifactFileName = artifact.fileName,
            useOriginalUrl = entry.useOriginalUrl
        )
    }

    suspend fun getDownloadUrl(
        entry: LockEntry.Jenkins
    ): Pair<String, String> {
        val build = build(entry.job, entry.jenkinsUrl, entry.buildNumber)
//        val artifact = artifact(entry.job, entry.jenkinsUrl, entry.buildNumber, entry.fileNameRegex)
        val url = build.url + "artifact/" + entry.artifactRelativePath
        return url to entry.artifactFileName
    }

    override suspend fun download(
        stopwatch: Stopwatch,
        entry: LockEntry,
        targetFolder: File,
        cacheDir: File
    ): Pair<String?, File> = stopwatch {
        entry as LockEntry.Jenkins
        require(entry.job.isNotBlank()) { "entry: '${entry.id}' does not have the jenkins job set" }
//        if (entry.job.isBlank()) {
//            entry.job = entry.id
//        }
        val (url, fileName) = getDownloadUrl(entry)

        val targetFile = targetFolder.resolve(entry.fileName ?: fileName)
        targetFile.download(url, cacheDir.resolve("JENKINS").resolve(entry.job).resolve(entry.buildNumber.toString()))
        return@stopwatch url to targetFile
    }

    override suspend fun generateName(entry: LockEntry): String {
        entry as LockEntry.Jenkins
        return "${entry.job} ${entry.buildNumber}"
    }

    override suspend fun getAuthors(entry: LockEntry): List<String> {
        entry as LockEntry.Jenkins
        return listOf(entry.job.substringBeforeLast('/').substringBeforeLast('/').substringAfterLast('/'))
    }

    override suspend fun getProjectPage(entry: LockEntry): String {
        entry as LockEntry.Jenkins
        val server = server(entry.jenkinsUrl)
        return server.getUrl(entry.job)
    }

    override suspend fun getVersion(entry: LockEntry): String {
        entry as LockEntry.Jenkins
        val artifact = artifact(entry.job, entry.jenkinsUrl, entry.buildNumber, entry.fileNameRegex)
        return artifact.fileName
    }

    override suspend fun getReleaseDate(entry: LockEntry): Instant? {
        entry as LockEntry.Jenkins
        val build = build(entry.job, entry.jenkinsUrl, entry.buildNumber)
        return Instant.ofEpochSecond(build.timestamp)
    }

    private val artifactCache: MutableMap<Quadruple<String, String, Int, String>, Artifact> =
        Collections.synchronizedMap(hashMapOf())

    suspend fun artifact(jobName: String, url: String, buildNumber: Int, fileNameRegex: String): Artifact {
        val a = Quadruple(jobName, url, buildNumber, fileNameRegex)
        return artifactCache.getOrPut(a) { artifactCall(jobName, url, buildNumber, fileNameRegex) }
    }

    private suspend fun artifactCall(jobName: String, url: String, buildNumber: Int, fileNameRegex: String): Artifact {
        val build = build(jobName, url, buildNumber)
        val re = Regex(fileNameRegex)

        return build.artifacts.find {
            re.matches(it.fileName)
        } ?: run {
            logger.error("artifacts: ${build.artifacts.map { it.fileName }}")
            throw IllegalStateException("no artifact matching $re found")
        }
    }

    private val buildCache: MutableMap<Triple<String, String, Int>, BuildWithDetails> =
        Collections.synchronizedMap(hashMapOf())

    suspend fun build(jobName: String, url: String, buildNumber: Int): BuildWithDetails {
        val a = Triple(jobName, url, buildNumber)
        return buildCache.getOrPut(a) { buildCall(jobName, url, buildNumber) }
    }

    private suspend fun buildCall(jobName: String, url: String, buildNumber: Int): BuildWithDetails {
        logger.info("get build $buildNumber")
        delay(20)
        return job(jobName, url).getBuildByNumber(buildNumber, useragent)!!
    }

    private val jobCache: MutableMap<Pair<String, String>, Job> = Collections.synchronizedMap(hashMapOf())
    suspend fun job(jobName: String, url: String): Job {
        val a = jobName to url
        return jobCache.getOrPut(a) { jobCall(jobName, url) }
    }

    private suspend fun jobCall(jobName: String, url: String): Job {
        val server = server(url)
        logger.info("get jenkins job $jobName")
        return server.getJob(jobName, useragent) ?: throw Exception("no such job: '$jobName' on $url")
    }

    val server = ::serverCall.memoize()
    private fun serverCall(url: String): JenkinsServer {
        logger.info("get jenkins server $url")
        return JenkinsServer(url)
    }

    override fun reportData(entry: LockEntry): MutableMap<EntryReportData, String> {
        entry as LockEntry.Jenkins
        val (url, fileName) = runBlocking {
            getDownloadUrl(entry)
        }
        return super.reportData(entry).also { data ->
//            data["BaseUrl"] = entry.jenkinsUrl // do we need this ?
            data[EntryReportData.FILE_NAME] = entry.fileName ?: fileName
            data[EntryReportData.DIRECT_URL] = url
            data[EntryReportData.JENKINS_JOB] = entry.job
            data[EntryReportData.JENKINS_BUILD] = "${entry.buildNumber}"
        }
    }

    override fun generateReportTableOverrides(entry: LockEntry): Map<String, Any?> {
        entry as LockEntry.Jenkins
        val (url, fileName) = runBlocking {
            getDownloadUrl(entry)
        }
        return mapOf(
            "File Name" to (entry.fileName ?: fileName),
            "Direct URL" to url,
            "Jenkins Job" to entry.job,
            "Jenkins Build" to entry.buildNumber
        )
    }
}
