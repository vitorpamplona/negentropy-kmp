package com.vitorpamplona.negentropy.testutils

import com.vitorpamplona.negentropy.platform.readTestResource
import com.vitorpamplona.negentropy.storage.IStorage
import com.vitorpamplona.negentropy.storage.Id

@OptIn(ExperimentalStdlibApi::class)
fun loadFile(file: String, db: IStorage) {
    readTestResource(file).decodeToString().lines().forEach {
        if (it.contains(",")) {
            val (timestamp, hex) = it.split(",")

            timestamp.toLongOrNull()?.let {
                db.insert(it, Id(hex.hexToByteArray()))
            }
        }
    }

    db.seal()
}