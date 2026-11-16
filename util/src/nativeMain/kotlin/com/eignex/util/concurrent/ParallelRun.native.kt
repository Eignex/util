@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class)

package com.eignex.util.concurrent

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

/** Run tasks on owned workers; failures travel as values so all results can be joined before rethrowing. */
actual fun <T> parallelRun(tasks: List<() -> T>): List<T> {
    if (tasks.size == 1) return listOf(tasks[0]())
    val workers = ArrayList<Worker>(tasks.size)
    try {
        val futures = tasks.map { task ->
            val worker = Worker.start()
            workers.add(worker)
            worker.execute(TransferMode.SAFE, { task }) { runCatching { it() } }
        }
        val outcomes = futures.map { it.result }
        return outcomes.map { it.getOrThrow() }
    } finally {
        workers.forEach { it.requestTermination(processScheduledJobs = true).result }
    }
}
