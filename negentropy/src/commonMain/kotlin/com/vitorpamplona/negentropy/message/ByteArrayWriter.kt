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

import kotlin.math.max

class ByteArrayWriter(
    size: Int = 32,
) {
    init {
        require(size >= 0) { "Negative initial size: $size" }
    }

    private var buf: ByteArray = ByteArray(size)
    private var count: Int = 0

    private fun newLength(
        oldLength: Int,
        minGrowth: Int,
        prefGrowth: Int,
    ): Int {
        val prefLength = (oldLength + max(minGrowth, prefGrowth)) // might overflow
        return if (prefLength in 1..Int.MAX_VALUE - 8) {
            prefLength
        } else {
            val minLength = oldLength + minGrowth
            return if (minLength < 0) { // overflow
                throw IllegalArgumentException("Required array length $oldLength + $minGrowth is too large")
            } else if (minLength <= Int.MAX_VALUE - 8) {
                Int.MAX_VALUE - 8
            } else {
                minLength
            }
        }
    }

    private fun ensureCapacity(minCapacity: Int) {
        val minGrowth = minCapacity - buf.size
        if (minGrowth > 0) {
            buf = buf.copyOf(newLength(buf.size, minGrowth, buf.size))
        }
    }

    fun write(b: Byte) {
        ensureCapacity(count + 1)
        buf[count] = b
        count += 1
    }

    fun write(b: ByteArray) {
        ensureCapacity(count + b.size)
        b.copyInto(buf, count)
        count += b.size
    }

    fun write(buffer: ByteArrayWriter) {
        ensureCapacity(count + buffer.count)
        buffer.buf.copyInto(buf, count, 0, buffer.count)
        count += buffer.count
    }

    fun toByteArray() = buf.copyOf(count)

    fun size() = count
}
