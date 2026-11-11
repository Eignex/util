@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("MatchingDeclarationName", "Filename")

package com.eignex.util.concurrent

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.PTHREAD_MUTEX_ERRORCHECK

// POSIX constants differ in signedness and width across targets.
internal actual val ERRORCHECK_MUTEX_TYPE: Int = PTHREAD_MUTEX_ERRORCHECK
