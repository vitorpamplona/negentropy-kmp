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

/**
 * 256-bit little-endian modular arithmetic over 32-byte buffers.
 *
 * Negentropy fingerprints an id range by summing every 32-byte id as a
 * little-endian 256-bit integer (mod 2^256) and hashing the result. Keeping
 * the add/subtract primitives here lets both the walk-the-range calculator
 * and prefix-sum based storages share the exact same accumulation, so the
 * resulting fingerprints stay byte-identical.
 */
internal object Accumulator256 {
    private fun get4BytesAsLongInLittleEndian(
        bytes: ByteArray,
        offset: Int,
    ): Long =
        (bytes[offset].toUInt().toLong() and 0xFF) or
            ((bytes[offset + 1].toUInt().toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toUInt().toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toUInt().toLong() and 0xFF) shl 24)

    private fun putLongInLittleEndianAs4Bytes(
        bytes: ByteArray,
        offset: Int,
        value: Long,
    ) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    /**
     * Adds the 32 bytes at [toAdd]/[toAddOffset] into the 32 bytes at
     * [base]/[baseOffset] in place, treating both as little-endian 256-bit
     * integers (mod 2^256). Offsets let callers pack many accumulators into a
     * single flat buffer (e.g. a prefix-sum table) and add in place without
     * slicing.
     */
    fun add(
        base: ByteArray,
        toAdd: ByteArray,
        baseOffset: Int = 0,
        toAddOffset: Int = 0,
    ) {
        var currCarry = 0L

        for (i in 0 until 8) {
            val bo = baseOffset + i * 4
            val to = toAddOffset + i * 4

            val p = get4BytesAsLongInLittleEndian(base, bo)
            val po = get4BytesAsLongInLittleEndian(toAdd, to)

            // must get 4 bytes from buffer, convert signed to unsigned int and
            // place it in a bigger variable to allow the if below
            val next = p + currCarry + po

            putLongInLittleEndianAs4Bytes(base, bo, next)
            currCarry = if (next > 0xFFFFFFFF) 1 else 0
        }
    }

    /**
     * Subtracts the 32 bytes at [toSubtract]/[toSubtractOffset] from the 32
     * bytes at [base]/[baseOffset] in place, treating both as little-endian
     * 256-bit integers (mod 2^256). This is the exact inverse of [add], so
     * `subtract(add(x, y), y) == x`.
     */
    fun subtract(
        base: ByteArray,
        toSubtract: ByteArray,
        baseOffset: Int = 0,
        toSubtractOffset: Int = 0,
    ) {
        var currBorrow = 0L

        for (i in 0 until 8) {
            val bo = baseOffset + i * 4
            val so = toSubtractOffset + i * 4

            val p = get4BytesAsLongInLittleEndian(base, bo)
            val po = get4BytesAsLongInLittleEndian(toSubtract, so)

            val next = p - po - currBorrow

            // putLong masks to the low 4 bytes, so a negative value wraps to
            // its two's-complement 32-bit representation (i.e. + 2^32).
            putLongInLittleEndianAs4Bytes(base, bo, next)
            currBorrow = if (next < 0) 1 else 0
        }
    }
}
