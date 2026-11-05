package com.eignex.util.platform

/**
 * Hash of [value]'s reference identity, independent of its overridden hash code.
 * Stable for the object's lifetime within this process; distinct objects may collide.
 * Available on JVM and Native targets.
 */
expect fun identityHash(value: Any): Int
