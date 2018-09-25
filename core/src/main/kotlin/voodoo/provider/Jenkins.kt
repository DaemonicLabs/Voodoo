package voodoo.provider

import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.delay
import mu.KLogging
import voodoo.core.CoreConstants.VERSION
import voodoo.data.Quadruple
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import voodoo.memoize
import voodoo.util.download
import voodoo.util.jenkins.Artifact
import voodoo.util.jenkins.BuildWithDetails
import voodoo.util.jenkins.JenkinsServer
import voodoo.util.jenkins.Job
import java.io.File
import java.time.Instant
import java.util.Collections

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object JenkinsProvider : ProviderBase( "Jenkins Provider") {
    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    override suspend fun resolve(
        entry: Entry,
        mcVersion: String,
        addEntry: SendChannel<Pair<Entry, String>>
    ): LockEntry {
        if (entry.job.isBlank()) {
            entry.job = entry.id
        }
        val job = job(entry.job, entry.jenkinsUrl)
        val buildNumber = job.lastSuccessfulBuild?.number ?: throw IllegalStateException("buildnumber not set")
        return entry.lock {
            jenkinsUrl = entry.jenkinsUrl
            this.job = entry.job
            this.buildNumber = buildNumber
            fileNameRegex = entry.fileNameRegex
        }
    }

    override suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String, File> {
        if (entry.job.isBlank()) {
            entry.job = entry.id
        }

        val build = build(entry.job, entry.jenkinsUrl, entry.buildNumber)
        val artifact = artifact(entry.job, entry.jenkinsUrl, entry.buildNumber, entry.fileNameRegex)
        val url = build.url + "artifact/" + artifact.relativePath
        val targetFile = targetFolder.resolve(entry.fileName ?: artifact.fileName)
        targetFile.download(url, cacheDir.resolve("JENKINS").resolve(entry.job).resolve(entry.buildNumber.toString()))
        return Pair(url, targetFile)
    }

    override suspend fun generateName(entry: LockEntry): String {
        return "${entry.job} ${entry.buildNumber}"
    }

    override suspend fun getAuthors(entry: LockEntry): List<String> {
        return listOf(entry.job.substringBeforeLast('/').substringBeforeLast('/').substringAfterLast('/'))
    }

    override suspend fun getProjectPage(entry: LockEntry): String {
        val server = server(entry.jenkinsUrl)
        return server.getUrl(entry.job)
    }

    override suspend fun getVersion(entry: LockEntry): String {
        val artifact = artifact(entry.job, entry.jenkinsUrl, entry.buildNumber, entry.fileNameRegex)
        return artifact.fileName
    }

    override suspend fun getReleaseDate(entry: LockEntry): Instant? {
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
        }!!
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
        val a = Pair(jobName, url)
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

    override fun reportData(entry: LockEntry): MutableList<Pair<Any, Any>> {
        val data = super.reportData(entry)
        data += "Job" to "`${entry.job}`"
        data += "Build" to "`${entry.buildNumber}`"
        return data
    }
}

