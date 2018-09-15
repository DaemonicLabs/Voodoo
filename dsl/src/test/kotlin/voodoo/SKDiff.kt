package voodoo

import kotlinx.serialization.json.JSON
import voodoo.data.sk.SKPack
import voodoo.data.sk.task.Task
import voodoo.util.json
import java.io.File

fun main(vararg args: String) {
    val pack1: SKPack =
        JSON.parse(File("/home/nikky/dev/Center-of-the-Multiverse/workspace/old.json").readText().also { })
    val pack2: SKPack =
        JSON.parse(File("/home/nikky/dev/Center-of-the-Multiverse/workspace/_upload/cotm.json").readText())

    val tasks: MutableList<Pair<Task, Boolean>> = mutableListOf()

    val sameTasks = mutableListOf<Task>()

    pack2.tasks.sortedBy { it.to }.forEach { task ->
        val oldTask = pack1.tasks.find { it.hash == task.hash || it.location == task.location || it.to == task.to }
        if (oldTask == null) {
            println("+++ new task: ${task.to}")
        } else {
            println("=== task: ${task.to}")
            if (task.to != oldTask.to) {
                println("-   file ${oldTask.to.substringAfterLast("/")}")
                println("+   file ${task.to.substringAfterLast("/")}")
            } else {
                println("    file ${task.to.substringAfterLast("/")}")
            }
            if (task.hash != oldTask.hash) {
                println("-   hash ${oldTask.hash}")
                println("+   hash ${task.hash}")
            } else {
                println("    hash ${task.hash}")
            }
            if (task.location != oldTask.location) {
                println("-   location ${oldTask.location}")
                println("+   location ${task.location}")
            } else {
                println("    location ${task.location}")
            }
        }
    }
}