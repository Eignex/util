package com.eignex.util.platform

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

/** Hash the reference identity of [value], independently of its overridden hash code. */
@OptIn(ExperimentalNativeApi::class)
actual fun identityHash(value: Any): Int = value.identityHashCode()
