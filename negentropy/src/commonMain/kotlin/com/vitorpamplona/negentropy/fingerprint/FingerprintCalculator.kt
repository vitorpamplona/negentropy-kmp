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
package com.vitorpamplona.negentropy.fingerprint

import com.vitorpamplona.negentropy.fingerprint.Fingerprint.Companion.SIZE
import com.vitorpamplona.negentropy.message.encodeVarInt
import com.vitorpamplona.negentropy.platform.sha256
import com.vitorpamplona.negentropy.storage.IStorage
import com.vitorpamplona.negentropy.storage.Id

class FingerprintCalculator {
    companion object {
        /**
         * Hashes an already-accumulated 256-bit id sum together with the number
         * of ids it covers into the final range fingerprint. Storages that keep
         * running id sums (e.g. prefix-sum tables) can reuse this to produce a
         * fingerprint byte-identical to the walk-the-range [run] below.
         */
        fun fingerprintOf(
            sum: ByteArray,
            count: Int,
        ): Fingerprint = Fingerprint(sha256(sum + encodeVarInt(count)).copyOfRange(0, SIZE))

        // cached common result
        val ZERO_RANGE_FINGERPRINT = fingerprintOf(ByteArray(Id.SIZE), 0)
    }

    private val buf: ByteArray = ByteArray(Id.SIZE)

    fun run(
        storage: IStorage,
        begin: Int,
        end: Int,
    ): Fingerprint {
        if (begin == end) return ZERO_RANGE_FINGERPRINT

        // Accumulate each id into eight 64-bit little-endian lanes with deferred carry
        // instead of re-reading and re-writing the 32-byte buffer on every id (as repeated
        // Accumulator256.add would). A single carry-propagation pass at the end reduces the
        // lanes back to 256 bits, producing the exact same sum (mod 2^256), hence the same
        // fingerprint. Long lanes hold the sum safely for any realistic id count.
        var l0 = 0L
        var l1 = 0L
        var l2 = 0L
        var l3 = 0L
        var l4 = 0L
        var l5 = 0L
        var l6 = 0L
        var l7 = 0L

        storage.forEach(begin, end) { item ->
            val b = item.id.bytes
            l0 += le32(b, 0)
            l1 += le32(b, 4)
            l2 += le32(b, 8)
            l3 += le32(b, 12)
            l4 += le32(b, 16)
            l5 += le32(b, 20)
            l6 += le32(b, 24)
            l7 += le32(b, 28)
        }

        val out = buf
        var carry = writeLane(out, 0, l0, 0L)
        carry = writeLane(out, 4, l1, carry)
        carry = writeLane(out, 8, l2, carry)
        carry = writeLane(out, 12, l3, carry)
        carry = writeLane(out, 16, l4, carry)
        carry = writeLane(out, 20, l5, carry)
        carry = writeLane(out, 24, l6, carry)
        writeLane(out, 28, l7, carry)

        return fingerprintOf(out, end - begin)
    }

    private fun le32(
        b: ByteArray,
        o: Int,
    ): Long =
        (b[o].toLong() and 0xFF) or
            ((b[o + 1].toLong() and 0xFF) shl 8) or
            ((b[o + 2].toLong() and 0xFF) shl 16) or
            ((b[o + 3].toLong() and 0xFF) shl 24)

    private fun writeLane(
        b: ByteArray,
        o: Int,
        lane: Long,
        carryIn: Long,
    ): Long {
        val v = lane + carryIn
        b[o] = (v and 0xFF).toByte()
        b[o + 1] = ((v shr 8) and 0xFF).toByte()
        b[o + 2] = ((v shr 16) and 0xFF).toByte()
        b[o + 3] = ((v shr 24) and 0xFF).toByte()
        return v ushr 32
    }
}
