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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The id/fingerprint hex decoder is a hand-rolled replacement for the Kotlin stdlib
 * [hexToByteArray], which dominated load time. These tests pin it to produce exactly the
 * same bytes as the stdlib on the inputs the protocol sees.
 */
@OptIn(ExperimentalStdlibApi::class)
class HexDecodeTest {
    @Test
    fun matchesStdlibForKnownVectors() {
        val vectors =
            listOf(
                "",
                "00",
                "ff",
                "0102030405060708090a0b0c0d0e0f",
                "eb6b05c2e3b008592ac666594d78ed83e7b9ab30f825b9b08878128f7500008c",
            )

        for (hex in vectors) {
            assertTrue(
                hex.hexToByteArray().contentEquals(HashedByteArray(hex).bytes),
                "mismatch for '$hex'",
            )
        }
    }

    @Test
    fun acceptsUpperAndMixedCase() {
        assertTrue(
            byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte())
                .contentEquals(HashedByteArray("ABCDEF").bytes),
        )
        assertTrue(
            byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte())
                .contentEquals(HashedByteArray("aBcDeF").bytes),
        )
    }

    @Test
    fun roundTripsThroughToHexString() {
        val hex = "39b916432333e069a4386917609215cc688eb99f06fed01aadc29b1b4b92d6f0"
        assertEquals(hex, HashedByteArray(hex).toHexString())
    }

    @Test
    fun equalIdsFromHexAndBytesShareHashAndEquality() {
        val hex = "abc81d58ebe3b9a87100d47f58bf15e9b1cbf62d38623f11d0f0d17179f5f3ba"
        val fromHex = Id(hex)
        val fromBytes = Id(hex.hexToByteArray())
        assertEquals(fromHex.hashCode(), fromBytes.hashCode())
        assertEquals(fromHex, fromBytes)
    }

    @Test
    fun rejectsOddLength() {
        assertFailsWith<IllegalArgumentException> { HashedByteArray("abc") }
    }

    @Test
    fun rejectsNonHexCharacters() {
        assertFailsWith<IllegalArgumentException> { HashedByteArray("zz") }
    }
}
