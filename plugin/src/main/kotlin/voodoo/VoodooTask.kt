package voodoo

import org.apache.tools.ant.types.Commandline
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

open class VoodooTask : JavaExec() {
//    @Input
//    val tasks: MutableList<List<String>> = mutableListOf()

    init {
        group = "voodoo"
    }

    @TaskAction
    override fun exec() {
//        println(tasks.map { it.joinToString(", ") })
//        val argsList = tasks.map { it.toList() }.flatMap { it + "-" } // + (args ?: listOf())
//        args = argsList.dropLastWhile { it == "-" }.dropWhile { it == "-" }
        println("executing: $args")

        super.exec()
    }
}