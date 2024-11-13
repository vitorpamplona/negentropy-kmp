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

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageConsumerTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testTimestampInDecoder() {
        val operator = MessageConsumer("86a091d70e0202".hexToByteArray())

        assertEquals(1678011277, operator.decodeTimestampIn())
        assertEquals(1678011278, operator.decodeTimestampIn())
        assertEquals(1678011279, operator.decodeTimestampIn())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testBoundDecoder() {
        val operator = MessageConsumer("86a091d70e20eb6b05c2e3b008592ac666594d78ed83e7b9ab30f825b9b08878128f7500008c".hexToByteArray())

        val bound = operator.decodeBound()

        assertEquals(1678011277, bound.timestamp)
        assertEquals("eb6b05c2e3b008592ac666594d78ed83e7b9ab30f825b9b08878128f7500008c", bound.prefix.toHexString())
    }
}
