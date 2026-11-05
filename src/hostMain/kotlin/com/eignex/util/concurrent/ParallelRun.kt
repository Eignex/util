package com.eignex.util.concurrent

/**
 * Run [tasks] on separate threads and return their results in input order.
 * An empty list returns immediately; one task executes on the caller without creating a worker.
 * Started tasks finish before this function returns or rethrows the first task failure in input order.
 * JVM threads and Native workers are owned and joined by this call. Available on JVM and Native.
 */
expect fun <T> parallelRun(tasks: List<() -> T>): List<T>
