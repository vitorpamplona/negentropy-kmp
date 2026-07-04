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

/**
 * Creates an ByteArray class that implements hashcode and equals of byte arrays
 * to be used by java collections. This implementation caches the value of the
 * hashcode to speed up processing.
 *
 * This **assumes** the byte array will never change
 */
open class HashedByteArray : Comparable<HashedByteArray> {
    val bytes: ByteArray

    // Lazily computed and cached on first hashCode()/equals() use. The vast majority of ids
    // are only ever inserted, sorted and fingerprinted (all of which touch bytes directly,
    // never the hash), so hashing every id at construction is wasted work on the hot load
    // path. 0 doubles as the "not computed yet" sentinel; a genuine 0 hash just recomputes,
    // which is cheap and rare. Safe because bytes never change (see class doc).
    private var hashCode: Int = 0

    constructor(byte: ByteArray) {
        this.bytes = byte
    }

    constructor(idHex: String) {
        this.bytes = hexToByteArray(idHex)
    }

    companion object {
        // Nibble lookup indexed directly by char code (0..255): value for '0'-'9', 'a'-'f'
        // and 'A'-'F', -1 for everything else. Kotlin's stdlib hexToByteArray() runs through
        // a configurable, separator-aware parser that dominates load time when constructing
        // millions of ids. This flat 256-entry table (as in Amethyst/Quartz's Hex.decode)
        // lets us index by char.code with no range branch; validity is folded into a single
        // (hi or lo) < 0 test per byte so we still reject bad hex. Produces the exact same
        // bytes as idHex.hexToByteArray() for valid input.
        private val HEX_NIBBLE =
            IntArray(256) { -1 }.apply {
                for (c in '0'..'9') this[c.code] = c - '0'
                for (c in 'a'..'f') this[c.code] = c - 'a' + 10
                for (c in 'A'..'F') this[c.code] = c - 'A' + 10
            }

        internal fun hexToByteArray(hex: String): ByteArray {
            val len = hex.length
            require(len and 1 == 0) { "Not a valid hex: odd length $len" }

            val table = HEX_NIBBLE
            return ByteArray(len / 2) {
                // char.code >= 256 (e.g. non-latin unicode) indexes out of the table and
                // throws, so it is rejected just like an in-range non-hex char below.
                val hi = table[hex[2 * it].code]
                val lo = table[hex[2 * it + 1].code]
                require((hi or lo) >= 0) { "Not a valid hex: $hex" }
                ((hi shl 4) or lo).toByte()
            }
        }
    }

    /**
     * Guarantees that two arrays with the same content get the same hashcode
     */
    private fun computeHashcode(): Int {
        var hash = 1
        for (i in bytes.size - 1 downTo 0) {
            hash = 31 * hash + bytes[i].toInt()
        }
        return hash
    }

    override fun hashCode(): Int {
        var h = hashCode
        if (h == 0) {
            h = computeHashcode()
            hashCode = h
        }
        return h
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HashedByteArray) return false

        return equalsId(other)
    }

    fun equalsId(other: HashedByteArray): Boolean {
        // check hashcode first for speed
        if (hashCode() != other.hashCode()) return false

        // if hashcode matches, check content.
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun compareTo(other: HashedByteArray): Int {
        for (i in bytes.indices) {
            if (i >= other.bytes.size) return 1 // `a` is longer

            // avoids conversion to unsigned byte if the same
            if (bytes[i] == other.bytes[i]) continue

            // avoids conversion to unsigned byte if both are negative
            if (bytes[i] < 0 && other.bytes[i] < 0) {
                if (bytes[i] < other.bytes[i]) return -1
                if (bytes[i] > other.bytes[i]) return 1
            }

            if (bytes[i] < 0) return 1 // other.bytes[i] is positive or zero

            if (other.bytes[i] < 0) return -1 // bytes[i] is positive or zero

            // both are positive
            if (bytes[i] < other.bytes[i]) return -1
            if (bytes[i] > other.bytes[i]) return 1
        }
        return when {
            bytes.size > other.bytes.size -> 1

            // `a` is longer
            bytes.size < other.bytes.size -> -1

            // `b` is longer
            else -> 0 // Both arrays are equal
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun toHexString() = bytes.toHexString()
}
