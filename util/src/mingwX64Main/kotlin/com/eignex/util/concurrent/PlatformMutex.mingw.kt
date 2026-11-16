@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
@file:Suppress("MatchingDeclarationName", "Filename")

package com.eignex.util.concurrent

import kotlinx.cinterop.Arena
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import platform.windows.AcquireSRWLockExclusive
import platform.windows.InitializeSRWLock
import platform.windows.ReleaseSRWLockExclusive
import platform.windows.SRWLOCK
import kotlin.native.ref.createCleaner

internal actual class PlatformMutex actual constructor() : Mutex {
    private val arena = Arena()
    private val lock = arena.alloc<SRWLOCK>().also { InitializeSRWLock(it.ptr) }

    @Suppress("unused")
    private val cleaner = createCleaner(arena) { it.clear() }

    actual override fun enter() = AcquireSRWLockExclusive(lock.ptr)

    actual override fun exit() = ReleaseSRWLockExclusive(lock.ptr)

    actual override fun <R> withLock(block: () -> R): R = guarded(block)
}
