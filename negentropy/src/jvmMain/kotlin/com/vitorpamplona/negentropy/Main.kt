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

import com.vitorpamplona.negentropy.storage.IStorage
import com.vitorpamplona.negentropy.storage.Id
import com.vitorpamplona.negentropy.storage.StorageVector
import java.io.PrintStream
import java.util.Scanner

val DEBUG = false

fun initDebug(owner: String) {
    if (DEBUG) {
        System.setErr(PrintStream("$owner.txt"))
    }
}

fun debug(
    owner: String,
    message: String,
) {
    if (DEBUG) {
        val time = System.currentTimeMillis()
        System.err.println("$time,$owner,$message")
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val owner = "kotlin-side" + System.nanoTime().toString()

    initDebug(owner)

    val frameSizeLimitStr = System.getenv("FRAMESIZELIMIT") ?: "0"
    val frameSizeLimit = frameSizeLimitStr.toLongOrNull() ?: 0
    val scanner = Scanner(System.`in`)

    var ne: Negentropy? = null
    val storage: IStorage = StorageVector()

    debug(owner, "create,$frameSizeLimitStr")

    while (scanner.hasNextLine()) {
        val line = scanner.nextLine()

        debug(owner, line)

        val items = line.split(",")

        when (items[0]) {
            "item" -> {
                if (items.size != 3) throw Error("too few items")
                val created = items[1].toLongOrNull() ?: throw Error("Invalid timestamp format")
                storage.insert(created, Id(items[2].trim()))
            }

            "seal" -> {
                storage.seal()
                ne = Negentropy(storage, frameSizeLimit)
            }

            "initiate" -> {
                if (ne == null) throw Error("Negentropy not created")
                val q = ne.initiate()
                if (frameSizeLimit > 0 && q.size > frameSizeLimit * 2) throw Error("frameSizeLimit exceeded")
                println("msg,${q.toHexString()}")
            }

            "msg" -> {
                if (ne == null) throw Error("Negentropy not created")
                if (items.size < 2) throw Error("Message not provided")

                val result = ne.reconcile(items[1].hexToByteArray())

                result.sendIds.forEach { id -> println("have,${id.toHexString()}") }
                result.needIds.forEach { id -> println("need,${id.toHexString()}") }

                if (frameSizeLimit > 0 && result.msg != null && result.msg.size > frameSizeLimit * 2) {
                    throw Error("frameSizeLimit exceeded")
                }

                if (result.msg == null) {
                    println("done")
                } else {
                    println("msg,${result.msg.toHexString()}")
                }
            }

            else -> throw Error("unknown cmd: ${items[0]}")
        }
    }
}
