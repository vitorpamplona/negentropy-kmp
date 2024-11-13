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
package com.vitorpamplona.negentropy

import com.vitorpamplona.negentropy.testutils.StorageAssets
import kotlin.test.Test
import kotlin.test.assertEquals

class NegentropyBasicTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testInitiation() {
        val expected = "6100000203eb6b05c2e3b008592ac666594d78ed83e7b9ab30f825b9b08878128f7500008c39b916432333e069a4386917609215cc688eb99f06fed01aadc29b1b4b92d6f0abc81d58ebe3b9a87100d47f58bf15e9b1cbf62d38623f11d0f0d17179f5f3ba"
        val operator = Negentropy(StorageAssets.defaultStorage())
        val result = operator.initiate()

        assertEquals(expected, result.toHexString())
    }

    @Test
    fun testReconcile() {
        val operator = Negentropy(StorageAssets.defaultStorage())
        val init = operator.initiate()
        val result = operator.reconcile(init)

        assertEquals(null, result.msg)
        assertEquals(0, result.needIds.size)
        assertEquals(0, result.sendIds.size)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testReconcileSimple() {
        val ne = Negentropy(StorageAssets.defaultStorage())
        val result = ne.reconcile("62aabbccddeeff".hexToByteArray())

        assertEquals("61", result.msg?.toHexString())
    }
}
