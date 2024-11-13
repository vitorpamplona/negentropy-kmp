package com.vitorpamplona.negentropy

import com.vitorpamplona.negentropy.testutils.InstructionParser
import com.vitorpamplona.negentropy.testutils.benchmark
import kotlin.test.Test

class BenchmarkMultipleRoundsDataBothSidesTest {
    @Test
    fun benchmark() {
        benchmark("JIT Benchmark") { 1+1 }

        val ip = InstructionParser()
        val instructions = ip.loadFiles("/multiplerounds-databothsides-node1.txt", "/multiplerounds-databothsides-node2.txt")

        benchmark("Reconcile 7 Rounds at 50000-byte frame and 250,000 records in each side") {
            val nodes = mutableMapOf<String, InstructionParser.Node>()

            instructions.forEach {
                ip.runLine(it, nodes)
            }
        }
    }
}