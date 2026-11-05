// The shared host test source set is outside the conventions' standard test-path exclusions.
@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "FunctionNaming")

package com.eignex.util.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class IdentityHashTest {
    private data class Value(var number: Int)

    @Test
    fun `identity hashing is stable when the value hash changes`() {
        val value = Value(1)
        val hash = identityHash(value)

        value.number = 2

        assertEquals(hash, identityHash(value))
    }
}
