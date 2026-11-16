package com.eignex.util.process

/**
 * Look up [name] as a JVM system property, then as an environment variable on every supported target.
 * The environment name is uppercased with dots replaced by underscores.
 * Available on JVM and Native.
 */
expect fun processProperty(name: String): String?

/** Print [message] and a newline to the process's standard error stream. */
expect fun printErrorLine(message: String)

/** Flush standard output and fail if the stream reports an output error. */
expect fun flushStandardOutput()

/** Terminate the process with [status]. No further caller code executes. */
expect fun exitProcess(status: Int): Nothing
