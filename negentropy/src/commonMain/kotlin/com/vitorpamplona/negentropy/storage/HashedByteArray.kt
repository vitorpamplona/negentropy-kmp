/**
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
    private val hashCode: Int

    constructor(byte: ByteArray) {
        this.bytes = byte
        this.hashCode = computeHashcode()
    }

    @OptIn(ExperimentalStdlibApi::class)
    constructor(idHex: String) {
        this.bytes = idHex.hexToByteArray()
        this.hashCode = computeHashcode()
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

    override fun hashCode() = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HashedByteArray) return false

        return equalsId(other)
    }

    fun equalsId(other: HashedByteArray): Boolean {
        // check hashcode first for speed
        if (hashCode != other.hashCode) return false

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
            bytes.size > other.bytes.size -> 1 // `a` is longer
            bytes.size < other.bytes.size -> -1 // `b` is longer
            else -> 0 // Both arrays are equal
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun toHexString() = bytes.toHexString()
}
