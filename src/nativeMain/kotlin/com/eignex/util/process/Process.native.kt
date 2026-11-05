@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.eignex.util.process

import kotlinx.cinterop.toKString
import platform.posix.ferror
import platform.posix.fflush
import platform.posix.fprintf
import platform.posix.getenv
import platform.posix.stderr
import platform.posix.stdout

/** Read the environment name derived from [name]. */
actual fun processProperty(name: String): String? = getenv(name.uppercase().replace('.', '_'))?.toKString()

/** Write a line to standard error. */
actual fun printErrorLine(message: String) {
    fprintf(stderr, "%s\n", message)
}

/** Flush standard output and report stream errors. */
actual fun flushStandardOutput() {
    check(fflush(stdout) == 0 && ferror(stdout) == 0) { "Standard output failed" }
}

/** Terminate the Native process. */
actual fun exitProcess(status: Int): Nothing = kotlin.system.exitProcess(status)
