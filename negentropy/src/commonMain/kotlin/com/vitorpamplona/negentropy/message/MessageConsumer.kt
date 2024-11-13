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

import com.vitorpamplona.negentropy.fingerprint.Fingerprint
import com.vitorpamplona.negentropy.storage.Bound
import com.vitorpamplona.negentropy.storage.HashedByteArray
import com.vitorpamplona.negentropy.storage.Id

class MessageConsumer(
    buffer: ByteArray,
    lastTimestamp: Long = 0,
) {
    private val consumer = ByteArrayReader(buffer)

    // all timestamps in a Negentropy messages are deltas from the previous one
    // the first timestamp is a unit timestamp
    private var lastTimestampIn = lastTimestamp

    fun hasItemsToConsume() = consumer.available() > 0

    // faster to copy the function here.
    internal fun decodeVarInt(): Long {
        var res = 0L
        var currByte: Int

        do {
            currByte = consumer.readInt()
            res = (res shl 7) or (currByte.toLong() and 127)
        } while (!isEndVarInt(currByte))

        return res
    }

    internal fun decodeTimestampIn(): Long {
        var deltaTimestamp = decodeVarInt()
        deltaTimestamp = if (deltaTimestamp == 0L) Long.MAX_VALUE else deltaTimestamp - 1
        if (lastTimestampIn == Long.MAX_VALUE || deltaTimestamp == Long.MAX_VALUE) {
            lastTimestampIn = Long.MAX_VALUE
            return Long.MAX_VALUE
        }
        deltaTimestamp += lastTimestampIn
        lastTimestampIn = deltaTimestamp
        return deltaTimestamp
    }

    internal fun decodeBound(): Bound {
        val timestamp = decodeTimestampIn()
        val len = decodeVarInt().toInt()
        return Bound(timestamp, HashedByteArray(consumer.readNBytes(len)))
    }

    fun decodeProtocolVersion(): Byte {
        val protocolVersion = consumer.readByte()

        check(protocolVersion in 0x60..0x6F) { "invalid negentropy protocol version byte $protocolVersion" }

        return protocolVersion
    }

    // ---

    fun nextMode(): Mode {
        val currBound = decodeBound()
        val mode = decodeVarInt()
        return when (mode.toInt()) {
            Mode.Skip.CODE -> Mode.Skip(currBound)
            Mode.Fingerprint.CODE -> Mode.Fingerprint(currBound, Fingerprint(consumer.readNBytes(Fingerprint.SIZE)))
            Mode.IdList.CODE ->
                Mode.IdList(
                    currBound,
                    List(decodeVarInt().toInt()) { Id(consumer.readNBytes(Id.SIZE)) },
                )

            else -> {
                throw Error("message.Mode not found")
            }
        }
    }
}
