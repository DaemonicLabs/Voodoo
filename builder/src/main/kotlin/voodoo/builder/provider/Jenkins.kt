package voodoo.builder.provider

import aballano.kotlinmemoization.memoize
import mu.KLogging
import voodoo.builder.VERSION
import voodoo.core.data.flat.Entry
import voodoo.core.data.flat.ModPack
import voodoo.core.data.lock.LockEntry
import voodoo.util.jenkins.JenkinsServer

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class JenkinsProviderThing : ProviderBase {
    override val name = "Jenkins Provider"

    companion object: KLogging() {
        val useragent = "voodoo/${VERSION} (https://github.com/elytra/Voodoo)"
    }

    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry? {
        val job = job(entry.job, entry.jenkinsUrl)
        val buildNumber = job.lastSuccessfulBuild?.number
        return if(buildNumber == null) null
        else LockEntry(provider = entry.provider, jenkinsUrl = entry.jenkinsUrl, job = entry.job, buildNumber = buildNumber)
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

