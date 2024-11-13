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

class NegentropyClientServerTest {
    @Test
    fun testReconcileDifferentDBs() {
        val client = Negentropy(StorageAssets.storageP1())
        val server = Negentropy(StorageAssets.storageP2())

        val init = client.initiate()
        val resultFor1 = server.reconcile(init)

        assertEquals("610000020239b916432333e069a4386917609215cc688eb99f06fed01aadc29b1b4b92d6f0abc81d58ebe3b9a87100d47f58bf15e9b1cbf62d38623f11d0f0d17179f5f3ba", resultFor1.msgToString())
        assertEquals("", resultFor1.needsToString())
        assertEquals("", resultFor1.sendsToString())

        val resultFor2 = client.reconcile(resultFor1.msg!!)

        assertEquals(null, resultFor2.msgToString())
        assertEquals("39b916432333e069a4386917609215cc688eb99f06fed01aadc29b1b4b92d6f0", resultFor2.needsToString())
        assertEquals("eb6b05c2e3b008592ac666594d78ed83e7b9ab30f825b9b08878128f7500008c", resultFor2.sendsToString())
    }

    @Test
    fun testReconcileEqualDB() {
        val client = Negentropy(StorageAssets.storageP1())
        val server = Negentropy(StorageAssets.storageP1())

        val init = client.initiate()

        val resultFor1 = server.reconcile(init)

        assertEquals("6100000202eb6b05c2e3b008592ac666594d78ed83e7b9ab30f825b9b08878128f7500008cabc81d58ebe3b9a87100d47f58bf15e9b1cbf62d38623f11d0f0d17179f5f3ba", resultFor1.msgToString())
        assertEquals("", resultFor1.needsToString())
        assertEquals("", resultFor1.sendsToString())

        val resultFor2 = client.reconcile(resultFor1.msg!!)

        assertEquals(null, resultFor2.msg)
        assertEquals("", resultFor2.needsToString())
        assertEquals("", resultFor2.sendsToString())
    }
}
