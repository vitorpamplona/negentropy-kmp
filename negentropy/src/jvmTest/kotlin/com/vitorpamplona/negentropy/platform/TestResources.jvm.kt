package com.vitorpamplona.negentropy.platform

/** Read the given resource as binary data. */
actual fun readTestResource(resourceName: String): ByteArray {
    return object {}.javaClass.getResourceAsStream(resourceName)!!.readBytes()
}