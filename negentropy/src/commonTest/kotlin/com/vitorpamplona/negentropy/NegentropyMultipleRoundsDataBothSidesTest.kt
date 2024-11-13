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
class NegentropyMultipleRoundsDataBothSidesTest {
    @Test
    fun runFullInstructions() {
        val ip = InstructionParser()

        val instructions = ip.loadFiles("/multiplerounds-databothsides-node1.txt", "/multiplerounds-databothsides-node2.txt")
        val nodes = mutableMapOf<String, InstructionParser.Node>()
        var producedCommand: String? = null

        instructions.forEach {
            if (producedCommand != null) {
                assertEquals(it.command.source, producedCommand)
                producedCommand = null
            }

            producedCommand = ip.runLine(it, nodes)
        }

        val nodeClient = nodes["641313757126791"]!!
        val nodeServer = nodes["641313757240583"]!!

        // test correct importation
        assertEquals(215908, nodeClient.negentropy.storage.size())
        assertEquals(215613, nodeServer.negentropy.storage.size())

        assertEquals(true, nodeClient.negentropy.isInitiator())
        assertEquals(false, nodeServer.negentropy.isInitiator())

        val clientDB =
            nodeClient.negentropy.storage
                .map { it.id }
                .toSet()
        val serverDB =
            nodeServer.negentropy.storage
                .map { it.id }
                .toSet()

        val haveOnBoth = clientDB.intersect(serverDB)
        val onlyOnClient = clientDB - serverDB
        val onlyOnServer = serverDB - clientDB

        val clientRealNeeds = onlyOnServer
        val serverRealNeeds = onlyOnClient

        assertEquals(213523, haveOnBoth.size)
        assertEquals(2385, onlyOnClient.size)
        assertEquals(2090, onlyOnServer.size)

        assertEquals(0, nodeServer.haves.size)
        assertEquals(0, nodeServer.needs.size)

        val uniqueHaves = nodeClient.haves.toSet()

        assertEquals(clientRealNeeds.size, nodeClient.needs.size)
        assertEquals(serverRealNeeds.size, uniqueHaves.size)

        val haveDuplicationOverhead = nodeClient.haves.groupBy { it }.filter { it.value.size > 1 }

        assertEquals(269, haveDuplicationOverhead.size)
    }
}
