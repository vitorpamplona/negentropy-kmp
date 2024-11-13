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

import com.vitorpamplona.negentropy.storage.Bound

class MessageBuilder(
    lastTimestamp: Long = 0L,
    skipStarter: SkipDelayer = SkipDelayer(),
) {
    private val builder = ByteArrayWriter(512)

    // all timestamps in a Negentropy messages are deltas from the previous one
    // the first timestamp is a unit timestamp
    private var lastTimestampOut = lastTimestamp

    // modes can be skipped in sequence. Instead of skipping them immediately
    // stores a state and only skips the last one.
    private var skipper = skipStarter

    // ---------------------
    // Branching and Merging
    // ---------------------

    // Branches a new builder while keeping the same timeout and skip status
    fun branch() = MessageBuilder(lastTimestampOut, skipper)

    // Merges a builder while keeping the same timeout and skip status
    fun merge(branchBuilder: MessageBuilder) {
        lastTimestampOut = branchBuilder.lastTimestampOut
        skipper = branchBuilder.skipper
        builder.write(branchBuilder.builder)
    }

    // -----------------
    // Utility functions
    // -----------------

    internal fun encodeTimestampOut(timestamp: Long): ByteArray =
        if (timestamp == Long.MAX_VALUE) {
            lastTimestampOut = Long.MAX_VALUE
            encodeVarInt(0)
        } else {
            val adjustedTimestamp = timestamp - lastTimestampOut
            lastTimestampOut = timestamp
            encodeVarInt(adjustedTimestamp + 1)
        }

    internal fun addByteArray(newBuffer: ByteArray) = builder.write(newBuffer)

    internal fun addNumber(n: Int) = builder.write(encodeVarInt(n))

    internal fun addNumber(n: Long) = builder.write(encodeVarInt(n))

    internal fun addTimestamp(timestamp: Long) = builder.write(encodeTimestampOut(timestamp))

    internal fun addBound(key: Bound) {
        addTimestamp(key.timestamp)
        addNumber(key.prefix.bytes.size)
        addByteArray(key.prefix.bytes)
    }

    internal fun addSkip(mode: Mode.Skip) {
        addBound(mode.nextBound)
        addNumber(Mode.Skip.CODE)
    }

    private fun addDelayedSkip() {
        skipper.addSkip(this)
    }

    // ---------------------
    // Public functions
    // ---------------------

    fun toByteArray(): ByteArray = builder.toByteArray()

    fun length() = builder.size()

    fun addProtocolVersion(version: Byte) = builder.write(version)

    fun addSkip(nextBound: Bound) = skipper.skip(nextBound)

    fun addIdList(mode: Mode.IdList) {
        addDelayedSkip()
        addBound(mode.nextBound)
        addNumber(Mode.IdList.CODE)
        addNumber(mode.ids.size)
        mode.ids.forEach { addByteArray(it.bytes) }
    }

    fun addFingerprint(mode: Mode.Fingerprint) {
        addDelayedSkip()
        addBound(mode.nextBound)
        addNumber(Mode.Fingerprint.CODE)
        addByteArray(mode.fingerprint.bytes)
    }

    fun addBounds(list: List<Mode>) {
        list.forEach {
            when (it) {
                is Mode.Fingerprint -> addFingerprint(it)
                is Mode.IdList -> addIdList(it)
                is Mode.Skip -> addSkip(it)
            }
        }
    }

    /**
     * Stores a skip state that can be shared between builders.
     */
    class SkipDelayer(
        currentSkipState: Bound? = null,
    ) {
        private var delaySkipBound = currentSkipState

        fun skip(nextBound: Bound) {
            delaySkipBound = nextBound
        }

        fun addSkip(builder: MessageBuilder) {
            delaySkipBound?.let {
                builder.addSkip(Mode.Skip(it))
                delaySkipBound = null
            }
        }
    }
}
