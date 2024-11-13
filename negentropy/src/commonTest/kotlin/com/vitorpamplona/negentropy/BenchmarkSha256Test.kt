package com.vitorpamplona.negentropy

import com.vitorpamplona.negentropy.fingerprint.FingerprintCalculator
import com.vitorpamplona.negentropy.platform.sha256
import com.vitorpamplona.negentropy.storage.StorageVector
import com.vitorpamplona.negentropy.testutils.StorageAssets.storageFrameLimitsClient
import com.vitorpamplona.negentropy.testutils.StorageAssets.storageFrameLimitsServer
import com.vitorpamplona.negentropy.testutils.benchmark
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class BenchmarkSha256Test {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun benchmarkSha256() {
        benchmark("JIT Benchmark") { 1+1 }

        val byteArray = "39b916432333e069a4386917609215cc688eb99f06fed01aadc29b1b4b92d6f0".hexToByteArray()
        val expected = "1c197eced80d1b8d86f76bd4dc8a66a71b4051d4f2a125e708fccda4810b432a".hexToByteArray()

        benchmark("Sha256") {
            assertContentEquals(expected, sha256(byteArray))
        }
    }
}