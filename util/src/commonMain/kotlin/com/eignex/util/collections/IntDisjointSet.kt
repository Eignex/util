package com.eignex.util.collections

/**
 * Dense union-find over `0 until size`, with path compression and arbitrary-root union.
 * Not thread-safe.
 */
class IntDisjointSet(size: Int) {
    private val parent = IntArray(size) { it }

    /** Representative root of [x]'s set, compressing the path during lookup. */
    fun find(x: Int): Int {
        var root = x
        while (parent[root] != root) root = parent[root]
        var cur = x
        while (parent[cur] != cur) {
            val next = parent[cur]
            parent[cur] = root
            cur = next
        }
        return root
    }

    /** Merge the sets containing [a] and [b]. A no-op if they already share a root. */
    fun union(a: Int, b: Int) {
        val ra = find(a)
        val rb = find(b)
        if (ra != rb) parent[ra] = rb
    }

    /** Whether [a] and [b] are in the same set. */
    fun connected(a: Int, b: Int): Boolean = find(a) == find(b)

    /**
     * Partition of `0 until size`: one [IntArray] per non-empty root, each holding that set's
     * member indices in ascending order. Roots appear in ascending order of their smallest member.
     */
    fun groups(): List<IntArray> {
        val counts = IntArray(parent.size)
        var groupCount = 0
        for (x in parent.indices) {
            val root = find(x)
            if (counts[root]++ == 0) groupCount++
        }
        val members = ArrayList<IntArray>(groupCount)
        val positions = IntArray(groupCount)
        for (x in parent.indices) {
            // The first pass compressed every path. Reuse counts to map roots to groups.
            val root = parent[x]
            if (counts[root] > 0) {
                members.add(IntArray(counts[root]))
                counts[root] = -members.size
            }
            val slot = -counts[root] - 1
            members[slot][positions[slot]++] = x
        }
        return members
    }
}
