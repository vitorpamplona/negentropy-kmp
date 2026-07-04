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

import com.vitorpamplona.negentropy.fingerprint.Accumulator256
import com.vitorpamplona.negentropy.fingerprint.Fingerprint
import com.vitorpamplona.negentropy.fingerprint.FingerprintCalculator

/**
 * A [StorageVector] that answers [fingerprint] in O(1) instead of walking the
 * range on every call.
 *
 * At [seal] it builds an additive prefix-sum table over the sorted ids: entry
 * `i` holds the little-endian 256-bit sum (mod 2^256) of the first `i` ids.
 * The fingerprint of any half-open range [begin, end) is then
 * `sha256( (prefix[end] - prefix[begin]) mod 2^256 || varint(end - begin) )[0:16]`,
 * which is byte-identical to summing the ids in that range directly. This trades
 * an O(range) walk per fingerprint for a single 256-bit subtraction plus one
 * sha256, at the cost of an O(n) table rebuild on every [seal].
 *
 * Behaves exactly like [StorageVector] for every other operation, so it is a
 * drop-in replacement wherever an [IStorage] is accepted (e.g. as the sealed
 * snapshot handed to a server session).
 */
class PrefixSumStorageVector(
    private val base: StorageVector = StorageVector(),
) : IStorage by base {
    // prefix[i] = sum of the first i ids (mod 2^256), little-endian; prefix[0] is zero.
    private var prefix: Array<ByteArray> = arrayOf(ByteArray(Id.SIZE))

    override fun seal() {
        base.seal()

        val size = base.size()
        val table = arrayOfNulls<ByteArray>(size + 1)
        table[0] = ByteArray(Id.SIZE)

        for (i in 0 until size) {
            val next = table[i]!!.copyOf()
            Accumulator256.add(next, base.getItem(i).id.bytes)
            table[i + 1] = next
        }

        @Suppress("UNCHECKED_CAST")
        prefix = table as Array<ByteArray>
    }

    override fun unseal() {
        base.unseal()
        prefix = arrayOf(ByteArray(Id.SIZE))
    }

    override fun fingerprint(
        begin: Int,
        end: Int,
    ): Fingerprint {
        check(begin in 0..end && end <= base.size()) { "bad range" }

        if (begin == end) return FingerprintCalculator.ZERO_RANGE_FINGERPRINT

        val diff = prefix[end].copyOf()
        Accumulator256.subtract(diff, prefix[begin])

        return FingerprintCalculator.fingerprintOf(diff, end - begin)
    }
}
