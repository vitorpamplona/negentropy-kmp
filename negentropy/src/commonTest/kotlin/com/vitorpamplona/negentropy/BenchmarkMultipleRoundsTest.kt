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
import com.vitorpamplona.negentropy.testutils.benchmark
import kotlin.test.Test

class BenchmarkMultipleRoundsTest {
    @Test
    fun benchmark() {
        benchmark("JIT Benchmark") { 1 + 1 }

        val ip = InstructionParser()
        val instructions = ip.loadFiles("/multiplerounds-node1.txt", "/multiplerounds-node2.txt")

        benchmark("Reconcile 7 Rounds at 60000-byte frame and 100,000 records") {
            val nodes = mutableMapOf<String, InstructionParser.Node>()

            instructions.forEach {
                ip.runLine(it, nodes)
            }
        }
    }
}
