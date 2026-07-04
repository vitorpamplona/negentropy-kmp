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

        buf.fill(0)

        storage.forEach(begin, end) { item ->
            Accumulator256.add(buf, item.id.bytes)
        }

        return fingerprintOf(buf, end - begin)
    }
}
