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
package com.vitorpamplona.negentropy.message

// Cache for most common values
val ZERO_BYTE_ARRAY = byteArrayOf(0)
val ONE_BYTE_ARRAY = encodeVarIntInternal(1)
val TWO_BYTE_ARRAY = encodeVarIntInternal(2)
val ID_BYTE_ARRAY = encodeVarIntInternal(32)

fun isEndVarInt(byte: Int) = ((byte and 128) == 0)

fun decodeVarInt(bytes: ByteArray): Long {
    var res = 0L

    bytes.forEach {
        res = (res shl 7) or (it.toLong() and 127)
    }

    return res
}

fun encodeVarInt(n: Int): ByteArray {
    if (n == 0) return ZERO_BYTE_ARRAY
    if (n == 1) return ONE_BYTE_ARRAY
    if (n == 2) return TWO_BYTE_ARRAY
    if (n == 32) return ID_BYTE_ARRAY

    return encodeVarIntInternal(n)
}

fun encodeVarIntInternal(n: Int): ByteArray {
    val list = mutableListOf<Int>()

    var number = n
    while (number != 0) {
        list.add(number and 127)
        number = number ushr 7
    }

    return ByteArray(list.size) {
        if (it == list.size - 1) {
            list[list.size - 1 - it].toByte()
        } else {
            (list[list.size - 1 - it] or 128).toByte()
        }
    }
}

fun encodeVarInt(n: Long): ByteArray {
    if (n == 0L) return ZERO_BYTE_ARRAY
    if (n == 1L) return ONE_BYTE_ARRAY
    if (n == 2L) return TWO_BYTE_ARRAY
    if (n == 32L) return ID_BYTE_ARRAY

    val list = mutableListOf<Long>()

    var number = n
    while (number != 0L) {
        list.add(number and 127)
        number = number ushr 7
    }

    return ByteArray(list.size) {
        if (it == list.size - 1) {
            list[list.size - 1 - it].toByte()
        } else {
            (list[list.size - 1 - it] or 128).toByte()
        }
    }
}
