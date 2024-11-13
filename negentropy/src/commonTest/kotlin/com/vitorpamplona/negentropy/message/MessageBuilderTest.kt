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
import com.vitorpamplona.negentropy.storage.HashedByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageBuilderTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testTimestampOutEncoder() {
        val operator = MessageBuilder()

        assertEquals("86a091d70e", operator.encodeTimestampOut(1678011277).toHexString())
        assertEquals("02", operator.encodeTimestampOut(1678011278).toHexString())
        assertEquals("02", operator.encodeTimestampOut(1678011279).toHexString())
        assertEquals("02", operator.encodeTimestampOut(1678011280).toHexString())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testBoundEncoder() {
        val test = Bound(1678011277, HashedByteArray("eb6b05c2e3b008592ac666594d78ed83e7b9ab30f825b9b08878128f7500008c"))
        val operator = MessageBuilder()
        operator.addBound(test)

        assertEquals("86a091d70e20eb6b05c2e3b008592ac666594d78ed83e7b9ab30f825b9b08878128f7500008c", operator.toByteArray().toHexString())
    }
}
