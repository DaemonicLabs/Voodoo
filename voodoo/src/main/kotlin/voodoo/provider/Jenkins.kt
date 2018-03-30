package voodoo.provider

import aballano.kotlinmemoization.memoize
import mu.KLogging
import voodoo.util.jenkins.JenkinsServer
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class JenkinsProviderThing : ProviderBase("Jenkins Provider") {
    companion object : KLogging()

    init {
        register("setBuildNumber",
                {
                    with(it) {
                        listOf(jenkinsUrl, job).all { it.isNotBlank() }
                                && it.buildNumber <= 0
                    }
                },
                { e, _ ->
                    val job = job(e.job, e.jenkinsUrl)
                    e.buildNumber = job.lastSuccessfulBuild?.number ?: -1
                }
        )
        register("setName",
                {
                    with(it) {
                        listOf(jenkinsUrl, job).all { it.isNotBlank() }
                                && it.buildNumber > 0 && it.name.isBlank()
                    }
                },
                { e, _ ->
                    val job = job(e.job, e.jenkinsUrl)
                    e.name = job.fullName
                }
        )
//        register("setDescription",
//                {
//                    with(it) {
//                        listOf(jenkinsUrl, job).all { it.isNotBlank() }
//                                && it.buildNumber > 0 && it.description.isBlank()
//                    }
//                },
//                { e, _ ->
//                    val job = job(e.job, e.jenkinsUrl)
//                    // crashes when job description is not set
//                    e.description = job.description
//                }
//        )
        register("setWebsiteUrl",
                {
                    with(it) {
                        listOf(jenkinsUrl, job).all { it.isNotBlank() }
                                && it.buildNumber > 0 && it.websiteUrl.isBlank()
                    }
                },
                { e, _ ->
                    val build = build(e.job, e.jenkinsUrl, e.buildNumber)

                    e.websiteUrl = build.url
                }
        )
        register("setUrl",
                {
                    with(it) {
                        listOf(jenkinsUrl, job).all { it.isNotBlank() }
                                && buildNumber > 0 && url.isBlank()
                    }
                },
                { e, _ ->
                    val build = build(e.job, e.jenkinsUrl, e.buildNumber)
                    val artifact = artifact(e.job, e.jenkinsUrl, e.buildNumber, e.jenkinsFileNameRegex)

                    logger.info("got artifact $artifact")
                    e.url = build.url + "artifact/" + artifact.relativePath
                }
        )
        register("setFileName",
                {
                    with(it) {
                        kotlin.collections.listOf(jenkinsUrl, job).all { it.isNotBlank() }
                                && buildNumber > 0 && fileName.isBlank()
                    }
                },
                { e, _ ->
                    val artifact = artifact(e.job, e.jenkinsUrl, e.buildNumber, e.jenkinsFileNameRegex)

                    e.fileName = artifact.fileName
                }
        )
//        register("setPackageType",
//                { it.id > 0 && it.packageType == PackageType.none },
//                { e, _ ->
//                    e.packageType = getAddon(e.id)!!.packageType
//                }
//        )
//        register("setTargetPath",
//                { it.id > 0 && it.targetPath.isBlank() },
//                { e, _ ->
//                    e.targetPath = getAddon(e.id)!!.categorySection.path
//                }
//        )
        register("cacheRelpath",
                { it.internal.cacheRelpath.isBlank() },
                { e, _ ->
                    e.internal.cacheRelpath = File(e.provider.toString()).resolve(e.job).path
                }
        )
        register("prepareDownload",
                {
                    with(it) {
                        listOf(url, name, fileName, internal.cachePath).all { it.isNotBlank() }
                    }
                },
                { e, _ ->
                    e.provider = Provider.DIRECT
                }
        )
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
        job(jobName, url).getBuildByNumber(buildNumber, "noagent")!!
    }.memoize()

    private val job = { jobName: String, url: String ->
        val server = server(url)
        logger.info("get jenkins job $jobName")
        server.getJob(jobName, "noagent") ?: throw Exception("no such job: '$jobName' on $url")
    }.memoize()

    private val server = { url: String ->
        logger.info("get jenkins server $url")
        JenkinsServer(url)
    }.memoize()

}

