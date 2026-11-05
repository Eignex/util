package com.eignex.util.platform

/** Hash the reference identity of [value], independently of its overridden hash code. */
actual fun identityHash(value: Any): Int = System.identityHashCode(value)
