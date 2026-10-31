/*
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

import com.vitorpamplona.negentropy.message.MessageConsumer
import com.vitorpamplona.negentropy.message.Mode
import com.vitorpamplona.negentropy.storage.IStorage
import com.vitorpamplona.negentropy.storage.Id
import com.vitorpamplona.negentropy.storage.StorageVector
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * When the frame size limit cuts a message, it ends with a fingerprint up to infinity that the
 * receiver reads as starting at the last bound written. That fingerprint used to start at the
 * cut range's upper index instead, leaving out the sender's items in between, which could end a
 * sync with items never reported (https://github.com/vitorpamplona/negentropy-kmp/issues/11).
 *
 * Runs syncs with frame size limits on both sides and checks that every fingerprint range in
 * every message is the sender's fingerprint of the range the receiver will read, and that the
 * sync ends with the exact set difference.
 */
class NegentropyFrameLimitsFingerprintTest {
    private fun assertFingerprintsMatchRanges(
        storage: IStorage,
        msg: ByteArray,
        context: String,
    ) {
        val consumer = MessageConsumer(msg)
        consumer.decodeProtocolVersion()

        var lowerIndex = 0
        while (consumer.hasItemsToConsume()) {
            val mode = consumer.nextMode()
            val upperIndex = storage.indexAtOrBeforeBound(mode.nextBound, lowerIndex, storage.size())
            if (mode is Mode.Fingerprint) {
                assertEquals(
                    storage.fingerprint(lowerIndex, upperIndex),
                    mode.fingerprint,
                    "$context: fingerprint range [$lowerIndex, $upperIndex) does not match the sender's items",
                )
            }
            lowerIndex = upperIndex
        }
    }

    @Test
    fun fingerprintRangesMatchSenderItemsWithFrameLimits() {
        repeat(150) { seed ->
            val rnd = Random(seed)
            val client = StorageVector()
            val server = StorageVector()
            val onlyOnClient = HashSet<Id>()
            val onlyOnServer = HashSet<Id>()

            // runs of consecutive items, each run held by the client, the server or both
            var timestamp = 1_000L
            repeat(rnd.nextInt(1, 10)) {
                val owner = rnd.nextInt(3)
                repeat(rnd.nextInt(0, 1200)) {
                    timestamp += rnd.nextLong(0, 3)
                    val id = Id(rnd.nextBytes(Id.SIZE))
                    if (owner != 1) client.insert(timestamp, id)
                    if (owner != 0) server.insert(timestamp, id)
                    if (owner == 0) onlyOnClient.add(id)
                    if (owner == 1) onlyOnServer.add(id)
                }
            }
            client.seal()
            server.seal()

            val neClient = Negentropy(client, rnd.nextLong(4096, 6000))
            val neServer = Negentropy(server, rnd.nextLong(4096, 6000))

            val haves = HashSet<Id>()
            val needs = HashSet<Id>()

            var msg: ByteArray? = neClient.initiate()
            var rounds = 0
            while (msg != null) {
                check(rounds++ < 1000) { "seed $seed: sync did not finish" }

                val response = neServer.reconcile(msg).msg!!
                assertFingerprintsMatchRanges(server, response, "seed $seed, server, round $rounds")

                val result = neClient.reconcile(response)
                result.msg?.let { assertFingerprintsMatchRanges(client, it, "seed $seed, client, round $rounds") }
                haves.addAll(result.sendIds)
                needs.addAll(result.needIds)
                msg = result.msg
            }

            assertEquals(onlyOnClient, haves, "seed $seed: haves")
            assertEquals(onlyOnServer, needs, "seed $seed: needs")
        }
    }
}
