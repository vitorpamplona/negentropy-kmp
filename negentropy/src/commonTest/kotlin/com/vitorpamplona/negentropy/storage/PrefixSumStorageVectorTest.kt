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

import com.vitorpamplona.negentropy.Negentropy
import com.vitorpamplona.negentropy.platform.sha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class PrefixSumStorageVectorTest {
    // Deterministic, random-looking 32-byte id derived from an index. sha256
    // spreads the bytes so ranges exercise carries/borrows across every limb.
    private fun idAt(i: Int): Id = Id(sha256(byteArrayOf(i.toByte(), (i shr 8).toByte(), (i shr 16).toByte())))

    private fun fill(
        storage: IStorage,
        count: Int,
    ): IStorage {
        for (i in 0 until count) {
            storage.insert(1_700_000_000L + i, idAt(i))
        }
        storage.seal()
        return storage
    }

    @Test
    fun matchesStorageVectorAcrossAllRanges() {
        val count = 200
        val reference = fill(StorageVector(), count)
        val accelerated = fill(PrefixSumStorageVector(), count)

        assertEquals(reference.size(), accelerated.size())

        // Every half-open sub-range must produce a byte-identical fingerprint.
        for (begin in 0..count) {
            for (end in begin..count) {
                assertEquals(
                    reference.fingerprint(begin, end).toHexString(),
                    accelerated.fingerprint(begin, end).toHexString(),
                    "range [$begin, $end)",
                )
            }
        }
    }

    @Test
    fun emptyRangeMatchesZeroFingerprint() {
        val accelerated = fill(PrefixSumStorageVector(), 10)
        val reference = fill(StorageVector(), 10)

        assertEquals(
            reference.fingerprint(3, 3).toHexString(),
            accelerated.fingerprint(3, 3).toHexString(),
        )
    }

    @Test
    fun canReSealAfterUnseal() {
        val storage = PrefixSumStorageVector()
        storage.insert(1L, idAt(0))
        storage.insert(2L, idAt(1))
        storage.seal()

        val before = storage.fingerprint(0, storage.size()).toHexString()

        storage.unseal()
        storage.insert(3L, idAt(2))
        storage.seal()

        // The rebuilt table must cover the newly added element.
        val expected =
            StorageVector()
                .apply {
                    insert(1L, idAt(0))
                    insert(2L, idAt(1))
                    insert(3L, idAt(2))
                    seal()
                }.fingerprint(0, 3)
                .toHexString()

        assertEquals(expected, storage.fingerprint(0, storage.size()).toHexString())
        // sanity: adding an element changed the whole-range fingerprint
        assertTrue(before != storage.fingerprint(0, storage.size()).toHexString())
    }

    @Test
    fun reconcileIsIdenticalToStorageVector() {
        val count = 500

        // Same data on both sides -> identical-set reconcile (the hot path).
        val clientRef = Negentropy(fill(StorageVector(), count))
        val serverRef = Negentropy(fill(StorageVector(), count))

        val clientFast = Negentropy(fill(StorageVector(), count))
        val serverFast = Negentropy(fill(PrefixSumStorageVector(), count))

        // Reference run with the stock storage on both sides.
        val initRef = clientRef.initiate()
        val roundRef = serverRef.reconcile(initRef)

        // Same protocol run, but the server uses the accelerated storage.
        val initFast = clientFast.initiate()
        val roundFast = serverFast.reconcile(initFast)

        assertEquals(initRef.toHexString(), initFast.toHexString())
        assertEquals(roundRef.msg?.toHexString(), roundFast.msg?.toHexString())
        assertEquals(roundRef.needIds.size, roundFast.needIds.size)
        assertEquals(roundRef.sendIds.size, roundFast.sendIds.size)
    }
}
