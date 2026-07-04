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
 * At [seal] it builds an additive prefix-sum table over the sorted ids: slot
 * `i` holds the little-endian 256-bit sum (mod 2^256) of the first `i` ids.
 * The fingerprint of any half-open range [begin, end) is then
 * `sha256( (prefix[end] - prefix[begin]) mod 2^256 || varint(end - begin) )[0:16]`,
 * which is byte-identical to summing the ids in that range directly. This trades
 * an O(range) walk per fingerprint for a single 256-bit subtraction plus one
 * sha256, at the cost of an O(n) table rebuild on every [seal].
 *
 * The table is kept in a single flat [ByteArray] of `(size + 1) * Id.SIZE`
 * bytes (slot `i` starts at `i * Id.SIZE`) rather than an array of arrays, so
 * the rebuild — the operation that dominates this snapshot-per-generation model
 * — is a sequential, cache-friendly pass with no per-slot allocation. This
 * mirrors how the C++ reference packs subtree accumulators inline in its B-tree
 * nodes.
 *
 * Behaves exactly like [StorageVector] for every other operation, so it is a
 * drop-in replacement wherever an [IStorage] is accepted (e.g. as the sealed
 * snapshot handed to a server session).
 */
class PrefixSumStorageVector(
    private val base: StorageVector = StorageVector(),
) : IStorage by base {
    // Flat prefix-sum table: slot i (bytes [i*Id.SIZE, (i+1)*Id.SIZE)) holds the
    // little-endian 256-bit sum (mod 2^256) of the first i ids; slot 0 is zero.
    private var prefix: ByteArray = ByteArray(Id.SIZE)

    override fun seal() {
        base.seal()

        val size = base.size()
        val table = ByteArray((size + 1) * Id.SIZE) // slot 0 stays zero-filled

        // Read ids straight from the sorted flat buffer instead of materializing an Id per
        // item via getItem(), so the table rebuild allocates nothing per element.
        val idBuffer = base.idBuffer()
        for (i in 0 until size) {
            val prev = i * Id.SIZE
            val curr = (i + 1) * Id.SIZE
            // prefix[i+1] = prefix[i] + id[i]
            table.copyInto(table, curr, prev, curr)
            Accumulator256.add(table, idBuffer, baseOffset = curr, toAddOffset = i * Id.SIZE)
        }

        prefix = table
    }

    override fun unseal() {
        base.unseal()
        prefix = ByteArray(Id.SIZE)
    }

    override fun fingerprint(
        begin: Int,
        end: Int,
    ): Fingerprint {
        check(begin in 0..end && end <= base.size()) { "bad range" }

        if (begin == end) return FingerprintCalculator.ZERO_RANGE_FINGERPRINT

        // diff = prefix[end] - prefix[begin]
        val diff = prefix.copyOfRange(end * Id.SIZE, (end + 1) * Id.SIZE)
        Accumulator256.subtract(diff, prefix, toSubtractOffset = begin * Id.SIZE)

        return FingerprintCalculator.fingerprintOf(diff, end - begin)
    }
}
