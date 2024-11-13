package com.vitorpamplona.negentropy.message

import kotlin.math.max

class ByteArrayWriter(size: Int = 32) {
    init {
        require(size >= 0) { "Negative initial size: $size" }
    }

    private var buf: ByteArray = ByteArray(size)
    private var count: Int = 0

    private fun newLength(oldLength: Int, minGrowth: Int, prefGrowth: Int): Int {
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