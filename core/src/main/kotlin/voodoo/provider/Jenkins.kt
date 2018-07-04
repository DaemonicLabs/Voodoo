package voodoo.provider

import aballano.kotlinmemoization.memoize
import mu.KLogging
import voodoo.core.CoreConstants.VERSION
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.util.download
import voodoo.util.jenkins.JenkinsServer
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object JenkinsProviderThing : ProviderBase, KLogging() {
    override val name = "Jenkins Provider"

    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"


    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry? {
        val job = job(entry.job, entry.jenkinsUrl)
        val buildNumber = job.lastSuccessfulBuild?.number
        return if (buildNumber == null) null
        else LockEntry(
                provider = entry.provider,
                name = entry.name,
                //folder = entry.folder,
                useUrlTxt = entry.useUrlTxt,
                fileName = entry.fileName,
                side = entry.side,
                jenkinsUrl = entry.jenkinsUrl,
                job = entry.job,
                buildNumber = buildNumber,
                fileNameRegex = entry.fileNameRegex
        )
    }

    override fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String, File> {
        val build = build(entry.job, entry.jenkinsUrl, entry.buildNumber)
        val artifact = artifact(entry.job, entry.jenkinsUrl, entry.buildNumber, entry.fileNameRegex)
        val url = build.url + "artifact/" + artifact.relativePath
        val targetFile = targetFolder.resolve(entry.fileName ?: artifact.fileName)
        targetFile.download(url, cacheDir.resolve("JENKINS").resolve(entry.job).resolve(entry.buildNumber.toString()))
        return Pair(url, targetFile)
    }

    override fun getAuthors(entry: LockEntry): List<String> {
        return listOf(entry.job.substringBeforeLast('/').substringBeforeLast('/').substringAfterLast('/'))
    }

    override fun getProjectPage(entry: LockEntry): String {
        val server = server(entry.jenkinsUrl)
        return server.getUrl(entry.job)
    }

    override fun getVersion(entry: LockEntry): String {
        val artifact = artifact(entry.job, entry.jenkinsUrl, entry.buildNumber, entry.fileNameRegex)
        return artifact.fileName
    }

    override fun getReleaseDate(entry: LockEntry): Instant? {
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
        job(jobName, url).getBuildByNumber(buildNumber, useragent)!!
    }.memoize()

    private val job = { jobName: String, url: String ->
        val server = server(url)
        logger.info("get jenkins job $jobName")
        server.getJob(jobName, useragent) ?: throw Exception("no such job: '$jobName' on $url")
    }.memoize()

    private val server = { url: String ->
        logger.info("get jenkins server $url")
        JenkinsServer(url)
    }.memoize()
}

