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
class NegentropyJavaSriptKotlinTest {
    @Test
    fun runKotlinJs() {
        val ip = InstructionParser()

        val instructions = ip.loadFiles("/lang1-js-client.txt", "/lang1-kotlin-server.txt")
        val nodes = mutableMapOf<String, InstructionParser.Node>()
        var currentCommand: String? = null
        var currentCommandFrom: String? = null

        instructions.forEach {
            if (currentCommand != null) {
                assertEquals(it.command.source, currentCommand, "From: $currentCommandFrom to ${it.toNode}")
                currentCommand = null
            }

            currentCommand = ip.runLine(it, nodes)
            currentCommandFrom = it.toNode
        }

        val nodeClient = nodes["kotlin-side"]!!
        val nodeServer = nodes["js-side"]!!

        // test correct importation
        assertEquals(25, nodeClient.negentropy.storage.size())
        assertEquals(25, nodeServer.negentropy.storage.size())

        assertEquals(true, nodeClient.negentropy.isInitiator())
        assertEquals(false, nodeServer.negentropy.isInitiator())

        assertEquals(0, nodeServer.haves.size)
        assertEquals(0, nodeServer.needs.size)

        assertEquals(0, nodeClient.haves.size)
        assertEquals(0, nodeClient.needs.size)
    }

    @Test
    fun runJsKotlin() {
        val ip = InstructionParser()

        val instructions = ip.loadFiles("/lang2-js-server.txt", "/lang2-kotlin-client.txt")
        val nodes = mutableMapOf<String, InstructionParser.Node>()
        var currentCommand: String? = null
        var currentCommandFrom: String? = null

        instructions.forEach {
            if (currentCommand != null) {
                assertEquals(it.command.source, currentCommand, "From: $currentCommandFrom to ${it.toNode}")
                currentCommand = null
            }

            currentCommand = ip.runLine(it, nodes)
            currentCommandFrom = it.toNode
        }

        val nodeClient = nodes["js-side"]!!
        val nodeServer = nodes["kotlin-side"]!!

        // test correct importation
        assertEquals(25, nodeClient.negentropy.storage.size())
        assertEquals(25, nodeServer.negentropy.storage.size())

        assertEquals(true, nodeClient.negentropy.isInitiator())
        assertEquals(false, nodeServer.negentropy.isInitiator())

        assertEquals(0, nodeServer.haves.size)
        assertEquals(0, nodeServer.needs.size)

        assertEquals(0, nodeClient.haves.size)
        assertEquals(0, nodeClient.needs.size)
    }

    @Test
    fun runJsKotlin2() {
        val ip = InstructionParser()

        val instructions = ip.loadFiles("/lang3-js-server.txt", "/lang3-js-client.txt")
        val nodes = mutableMapOf<String, InstructionParser.Node>()
        var currentCommand: String? = null
        var currentCommandFrom: String? = null

        instructions.forEach {
            if (currentCommand != null) {
                assertEquals(it.command.source, currentCommand, "From: $currentCommandFrom to ${it.toNode}")
                currentCommand = null
            }

            currentCommand = ip.runLine(it, nodes)
            currentCommandFrom = it.toNode
        }

        val nodeClient = nodes["js-side1731072008120"]!!
        val nodeServer = nodes["js-side1731072008121"]!!

        // test correct importation
        assertEquals(24681, nodeClient.negentropy.storage.size())
        assertEquals(24715, nodeServer.negentropy.storage.size())

        assertEquals(true, nodeClient.negentropy.isInitiator())
        assertEquals(false, nodeServer.negentropy.isInitiator())

        assertEquals(0, nodeServer.haves.size)
        assertEquals(0, nodeServer.needs.size)

        assertEquals(5386, nodeClient.haves.size)
        assertEquals(5420, nodeClient.needs.size)
    }
}
