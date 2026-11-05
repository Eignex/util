package com.eignex.util.concurrent

/**
 * Synchronous mutual exclusion for a block of work. Re-entry is unsupported.
 * JVM and Native implementations synchronize threads; JS and Wasm guard synchronous
 * re-entry within one execution agent and do not synchronize separate workers.
 */
interface Mutex {
    /** Acquire the lock. Each successful acquisition must be paired with [exit]. */
    fun enter()

    /** Release the lock held by this thread or execution agent. */
    fun exit()

    /** Run [block] while holding the lock, releasing it even when the block throws. */
    fun <R> withLock(block: () -> R): R
}

/**
 * Create a platform mutex, or a no-op lock when [concurrent] is false.
 * A no-op lock requires the caller to own or otherwise synchronize the guarded state.
 */
fun Mutex(concurrent: Boolean = true): Mutex = if (concurrent) PlatformMutex() else NoopMutex

@PublishedApi
internal object NoopMutex : Mutex {
    override fun enter() = Unit
    override fun exit() = Unit
    override fun <R> withLock(block: () -> R): R = block()
}

internal expect class PlatformMutex() : Mutex {
    override fun enter()
    override fun exit()
    override fun <R> withLock(block: () -> R): R
}

/** Run [block] under this lock without allocating a capturing lambda; release on every exit path. */
inline fun <R> Mutex.guarded(block: () -> R): R {
    if (this === NoopMutex) return block()
    enter()
    try {
        return block()
    } finally {
        exit()
    }
}
