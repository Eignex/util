package com.eignex.util.process

/** System property lookup with the documented environment fallback. */
actual fun processProperty(name: String): String? =
    System.getProperty(name) ?: System.getenv(name.uppercase().replace('.', '_'))

/** Write a line to standard error. */
actual fun printErrorLine(message: String) = System.err.println(message)

/** Flush standard output and report stream errors. */
actual fun flushStandardOutput() {
    System.out.flush()
    check(!System.out.checkError()) { "Standard output failed" }
}

/** Terminate the JVM process. */
actual fun exitProcess(status: Int): Nothing = kotlin.system.exitProcess(status)
