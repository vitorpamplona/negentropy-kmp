/*
 * Copyright (c) 2024 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.negentropy.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Stress tests for the flat-buffer [StorageVector]'s hand-rolled index sort: sizes above the
 * insertion-sort cutoff, heavy timestamp collisions (so ordering falls through to the id
 * bytes), shuffled insert order, and adjacent-duplicate detection at [StorageVector.seal].
 */
class StorageVectorSortTest {
    // Deterministic 32-byte hex id from a seed, no external RNG dependency.
    private fun idHex(seed: Int): String {
        var s = (seed * -0x61c88647).toLong() and 0xFFFFFFFFL
        val sb = StringBuilder(64)
        repeat(32) {
            s = (s * 6364136223846793005L + 1442695040888963407L) and 0xFFFFFFFFFFFFFFL
            sb.append("0123456789abcdef"[((s ushr 24) and 0xF).toInt()])
            sb.append("0123456789abcdef"[((s ushr 8) and 0xF).toInt()])
        }
        return sb.toString()
    }

    private fun assertFullySorted(storage: StorageVector) {
        for (i in 1 until storage.size()) {
            assertTrue(
                storage.getItem(i - 1) < storage.getItem(i),
                "items out of order at $i: ${storage.getItem(i - 1).id.toHexString()} !< ${storage.getItem(i).id.toHexString()}",
            )
        }
    }

    @Test
    fun sortsLargeShuffledInputWithTimestampCollisions() {
        val n = 5000
        // Build (timestamp, id) pairs then insert in a shuffled order. Only ~50 distinct
        // timestamps, so most comparisons are decided by the id bytes.
        val order = IntArray(n) { it }
        // simple deterministic shuffle
        var r = 12345
        for (i in n - 1 downTo 1) {
            r = (r * 1103515245 + 12345) and 0x7fffffff
            val j = r % (i + 1)
            val t = order[i]; order[i] = order[j]; order[j] = t
        }

        val storage = StorageVector()
        for (k in order) {
            storage.insert(1677970000L + (k % 50), idHex(k))
        }
        storage.seal()

        assertEquals(n, storage.size())
        assertFullySorted(storage)
    }

    @Test
    fun sortsExactlyAtInsertionSortCutoffBoundary() {
        // exercise sizes right around the insertion-sort / quicksort switch
        for (n in listOf(1, 2, 31, 32, 33, 64, 100)) {
            val storage = StorageVector()
            for (k in 0 until n) storage.insert(1000L, idHex(n * 1000 + k))
            storage.seal()
            assertEquals(n, storage.size())
            assertFullySorted(storage)
        }
    }

    @Test
    fun detectsDuplicateWithSameTimestampAndId() {
        val dup = idHex(7)
        val storage =
            StorageVector().apply {
                insert(1677970000L, idHex(1))
                insert(1677970000L, dup)
                insert(1677970005L, idHex(2))
                insert(1677970000L, dup) // same timestamp + id as an earlier insert
            }
        assertFailsWith<IllegalStateException> { storage.seal() }
    }
}
