package com.vitorpamplona.negentropy.platform

actual fun sha256(bytes: ByteArray): ByteArray {
    val sha256 = Sha256()
    sha256.update(bytes, 0, bytes.size)
    return sha256.digest()
}