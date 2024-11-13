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

import com.vitorpamplona.negentropy.testutils.StorageAssets.storageFrameLimitsClient
import com.vitorpamplona.negentropy.testutils.StorageAssets.storageFrameLimitsServer
import com.vitorpamplona.negentropy.testutils.benchmark
import kotlin.test.Test
import kotlin.test.assertEquals

class BenchmarkSimpleTest {
    @Test
    fun benchmarkInitialize() {
        benchmark("JIT Benchmark") { 1 + 1 }

        val clientDB = storageFrameLimitsClient()
        val serverDB = storageFrameLimitsServer()

        assertEquals(111, clientDB.size())
        assertEquals(147, serverDB.size())

        benchmark("Initialize Client") { Negentropy(clientDB).initiate() }
        benchmark("Initialize Server") { Negentropy(serverDB).initiate() }
    }

    @Test
    fun benchmarkReconcile() {
        benchmark("JIT Benchmark") { 1 + 1 }

        val clientDB = storageFrameLimitsClient()
        val serverDB = storageFrameLimitsServer()

        assertEquals(111, clientDB.size())
        assertEquals(147, serverDB.size())

        val neClient = Negentropy(clientDB)
        val neServer = Negentropy(serverDB)

        val init = neClient.initiate()

        // send to the server.
        benchmark("Reconcile Simple") { neServer.reconcile(init) }
    }
}
