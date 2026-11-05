@file:Suppress("MatchingDeclarationName", "Filename")

package com.eignex.util.concurrent

internal actual class PlatformMutex actual constructor() : Mutex {
    private var held = false

    actual override fun enter() {
        check(!held) { "PlatformMutex is not reentrant" }
        held = true
    }

    actual override fun exit() {
        check(held) { "PlatformMutex is not held" }
        held = false
    }

    actual override fun <R> withLock(block: () -> R): R = guarded(block)
}
