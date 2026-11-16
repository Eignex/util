package com.eignex.util.concurrent

/** Run tasks on owned threads, joining all started threads before returning or propagating a failure. */
@Suppress("TooGenericExceptionCaught")
actual fun <T> parallelRun(tasks: List<() -> T>): List<T> {
    if (tasks.size == 1) return listOf(tasks[0]())
    val results = arrayOfNulls<Any?>(tasks.size)
    val errors = arrayOfNulls<Throwable>(tasks.size)
    val threads = ArrayList<Thread>(tasks.size)
    var launchFailure: Throwable? = null
    try {
        tasks.forEachIndexed { index, task ->
            val thread = Thread {
                try {
                    results[index] = task()
                } catch (failure: Throwable) {
                    errors[index] = failure
                }
            }.apply { isDaemon = true }
            thread.start()
            threads.add(thread)
        }
    } catch (failure: Throwable) {
        launchFailure = failure
    }
    var interrupted = false
    for (thread in threads) {
        while (thread.isAlive) {
            try {
                thread.join()
            } catch (_: InterruptedException) {
                interrupted = true
            }
        }
    }
    if (interrupted) {
        Thread.currentThread().interrupt()
        throw InterruptedException("Interrupted while awaiting parallel tasks")
    }
    (launchFailure ?: errors.firstOrNull { it != null })?.let { throw it }
    @Suppress("UNCHECKED_CAST")
    return List(tasks.size) { results[it] as T }
}
