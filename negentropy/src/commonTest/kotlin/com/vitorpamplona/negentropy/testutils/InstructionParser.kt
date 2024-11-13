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
package com.vitorpamplona.negentropy.testutils

import com.vitorpamplona.negentropy.Negentropy
import com.vitorpamplona.negentropy.platform.readTestResource
import com.vitorpamplona.negentropy.storage.Id
import com.vitorpamplona.negentropy.storage.StorageVector

class InstructionParser {
    class Node(
        val negentropy: Negentropy,
        val haves: MutableList<Id> = mutableListOf(),
        val needs: MutableList<Id> = mutableListOf(),
    )

    class Instruction(
        val toNode: String,
        val time: Long,
        val command: Command,
    )

    sealed class Command(
        val source: String,
    ) {
        class Create(
            source: String,
            val frameSizeLimit: Long,
        ) : Command(source)

        class Item(
            source: String,
            val created: Long,
            val id: Id,
        ) : Command(source)

        class Seal(
            source: String,
        ) : Command(source)

        class Initiate(
            source: String,
        ) : Command(source)

        class Message(
            source: String,
            val msg: ByteArray,
        ) : Command(source)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun parseCommand(command: String): Command {
        val items = command.split(",")
        return when (items[0]) {
            "create" -> {
                if (items.size != 2) throw Error("too few items")
                val frameSizeLimit = items[1].toLongOrNull() ?: throw Error("Invalid framesoze format")
                Command.Create(command, frameSizeLimit)
            }

            "item" -> {
                if (items.size != 3) throw Error("too few items")
                val created = items[1].toLongOrNull() ?: throw Error("Invalid timestamp format")
                val id = items[2].trim()
                Command.Item(command, created, Id(id))
            }

            "seal" -> Command.Seal(command)
            "initiate" -> Command.Initiate(command)
            "msg" -> {
                if (items.size < 2) throw Error("Message not provided")
                val q = items[1]
                Command.Message(command, q.hexToByteArray())
            }
            else -> throw Error("unknown cmd: ${items[0]}")
        }
    }

    private fun loadFile(file: String): List<Instruction> {
        val list = mutableListOf<Instruction>()

        readTestResource(file).decodeToString().lines().forEach {
            if (it.contains(",")) {
                val (time, toNode, command) = it.split(",", limit = 3)
                list.add(Instruction(toNode, time.toLong(), parseCommand(command)))
            }
        }

        return list
    }

    fun loadFiles(
        file1: String,
        file2: String,
    ): List<Instruction> = (loadFile(file1) + loadFile(file2)).sortedBy { it.time }

    @OptIn(ExperimentalStdlibApi::class)
    fun runLine(
        line: Instruction,
        nodes: MutableMap<String, Node>,
    ): String? {
        val node = nodes[line.toNode]
        val ne = node?.negentropy
        when (val command = line.command) {
            is Command.Create -> {
                check(!nodes.contains(line.toNode)) { "Node already exist" }

                nodes[line.toNode] = Node(Negentropy(StorageVector(), command.frameSizeLimit))
            }
            is Command.Item -> {
                check(ne != null) { "Negentropy not created for this Node ${line.toNode}" }
                ne.insert(command.created, command.id)
            }

            is Command.Seal -> {
                check(ne != null) { "Negentropy not created for this Node ${line.toNode}" }
                ne.seal()
            }

            is Command.Initiate -> {
                check(ne != null) { "Negentropy not created for this Node ${line.toNode}" }
                val q = ne.initiate()
                if (ne.frameSizeLimit > 0 && q.size > ne.frameSizeLimit * 2) throw Error("frameSizeLimit exceeded")
                return "msg,${q.toHexString()}"
            }

            is Command.Message -> {
                check(ne != null) { "Negentropy not created for this Node ${line.toNode}" }
                val result = ne.reconcile(command.msg)

                if (ne.frameSizeLimit > 0 && result.msg != null && result.msg!!.size > ne.frameSizeLimit * 2) {
                    throw Error("frameSizeLimit exceeded")
                }

                node.haves.addAll(result.sendIds)
                node.needs.addAll(result.needIds)

                return if (result.msg == null) {
                    "done"
                } else {
                    "msg,${result.msg!!.toHexString()}"
                }
            }
        }

        return null
    }
}
