package voodoo.provider

import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.core.CoreConstants.VERSION
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import voodoo.memoize
import voodoo.util.download
import voodoo.util.jenkins.JenkinsServer
import java.io.File
import java.lang.IllegalStateException
import java.time.Instant

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object JenkinsProviderThing : ProviderBase, KLogging() {
    override val name = "Jenkins Provider"

    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"


    override suspend fun resolve(entry: Entry, mcVersion: String, addEntry: SendChannel<Pair<Entry, String>>): LockEntry {
        if(entry.job.isBlank()) {
            entry.job = entry.id
        }
        val job = job(entry.job, entry.jenkinsUrl)
        val buildNumber = job.lastSuccessfulBuild?.number ?: throw IllegalStateException("buildnumber not set")
        return LockEntry(
                provider = entry.provider,
                id = entry.id,
                name = entry.name,
                //rootFolder = entry.rootFolder,
                useUrlTxt = entry.useUrlTxt,
                fileName = entry.fileName,
                side = entry.side,
                jenkinsUrl = entry.jenkinsUrl,
                job = entry.job,
                buildNumber = buildNumber,
                fileNameRegex = entry.fileNameRegex
        )
    }

    override suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String, File> {
        if(entry.job.isBlank()) {
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
        return build.timestamp.toInstant()
    }

    private val artifact = { jobName: String, url: String, buildNumber: Int, fileNameRegex: String ->
        val build = build(jobName, url, buildNumber)
        val re = Regex(fileNameRegex)

        build.artifacts.find {
            re.matches(it.fileName)
        }!!
    }.memoize()

    private val build = { jobName: String, url: String, buildNumber: Int ->
        logger.info("get build $buildNumber")
        runBlocking { job(jobName, url).getBuildByNumber(buildNumber, useragent)!! }
    }.memoize()

    private val job = { jobName: String, url: String ->
        val server = server(url)
        logger.info("get jenkins job $jobName")
        runBlocking { server.getJob(jobName, useragent) ?: throw Exception("no such job: '$jobName' on $url") }
    }.memoize()

    private val server = { url: String ->
        logger.info("get jenkins server $url")
        JenkinsServer(url)
    }.memoize()

    override fun reportData(entry: LockEntry): MutableList<Pair<Any, Any>> {
        val data = super.reportData(entry)
        data += "Job" to "`${entry.job}`"
        data += "Build" to "`${entry.buildNumber}`"
        return data
    }
}

