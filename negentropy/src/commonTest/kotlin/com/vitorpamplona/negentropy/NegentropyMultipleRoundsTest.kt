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

import com.vitorpamplona.negentropy.testutils.InstructionParser
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Uses a custom variant of the test object to run full instructions
 */
class NegentropyMultipleRoundsTest {
    @Test
    fun runFullInstructions() {
        val ip = InstructionParser()

        val instructions = ip.loadFiles("/multiplerounds-node1.txt", "/multiplerounds-node2.txt")
        val nodes = mutableMapOf<String, InstructionParser.Node>()
        var producedCommand: String? = null

        instructions.forEach {
            if (producedCommand != null) {
                assertEquals(it.command.source, producedCommand)
                producedCommand = null
            }

            producedCommand = ip.runLine(it, nodes)
        }

        val nodeClient = nodes["559789284344708"]!!
        val nodeServer = nodes["559789284489666"]!!

        // test correct importation
        assertEquals(0, nodeClient.negentropy.storage.size())
        assertEquals(100000, nodeServer.negentropy.storage.size())

        assertEquals(true, nodeClient.negentropy.isInitiator())
        assertEquals(false, nodeServer.negentropy.isInitiator())

        assertEquals(0, nodeServer.haves.size)
        assertEquals(0, nodeServer.needs.size)

        assertEquals(0, nodeClient.haves.size)
        assertEquals(100000, nodeClient.needs.size)
    }
}
