package com.eignex.util.runtime

/** Hash the reference identity of [value], independently of its overridden hash code. */
actual fun identityHash(value: Any): Int = System.identityHashCode(value)
