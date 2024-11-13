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

import kotlin.test.Test
import kotlin.test.assertEquals

class ComparatorTest {
    @Test
    fun testComparator() {
        val st = StorageVector()

        assertEquals(0, Bound(0).compareTo(Bound(0)))
        assertEquals(0, Bound(1000).compareTo(Bound(1000)))
        assertEquals(-1, Bound(1).compareTo(Bound(2)))
        assertEquals(1, Bound(2).compareTo(Bound(1)))
    }

    @Test
    fun testComparatorByteArray() {
        assertEquals(0, Bound(0, HashedByteArray("0000")).compareTo(Bound(0, HashedByteArray("0000"))))
        assertEquals(-1, Bound(0, HashedByteArray("0000")).compareTo(Bound(0, HashedByteArray("0010"))))
        assertEquals(1, Bound(0, HashedByteArray("0100")).compareTo(Bound(0, HashedByteArray("0000"))))
        assertEquals(-1, Bound(0, HashedByteArray("1111")).compareTo(Bound(0, HashedByteArray("111100"))))
        assertEquals(1, Bound(0, HashedByteArray("111100")).compareTo(Bound(0, HashedByteArray("1111"))))
    }

    @Test
    fun testComparatorUbyteIssue() {
        assertEquals(-1, Bound(0, HashedByteArray("9075")).compareTo(Bound(0, HashedByteArray("90e2"))))
    }
}
