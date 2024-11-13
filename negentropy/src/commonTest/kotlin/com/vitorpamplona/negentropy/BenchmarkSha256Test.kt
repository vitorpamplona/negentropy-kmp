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

import com.vitorpamplona.negentropy.platform.sha256
import com.vitorpamplona.negentropy.testutils.benchmark
import kotlin.test.Test
import kotlin.test.assertContentEquals

class BenchmarkSha256Test {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun benchmarkSha256() {
        benchmark("JIT Benchmark") { 1 + 1 }

        val byteArray = "39b916432333e069a4386917609215cc688eb99f06fed01aadc29b1b4b92d6f0".hexToByteArray()
        val expected = "1c197eced80d1b8d86f76bd4dc8a66a71b4051d4f2a125e708fccda4810b432a".hexToByteArray()

        benchmark("Sha256") {
            assertContentEquals(expected, sha256(byteArray))
        }
    }
}
