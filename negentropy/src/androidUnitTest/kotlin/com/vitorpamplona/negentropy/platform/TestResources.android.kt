package com.vitorpamplona.negentropy.platform

/** Read the given resource as binary data. */
actual fun readTestResource(resourceName: String): ByteArray {
    println(object {}.javaClass.getResource("."))

    return object {}.javaClass.getResourceAsStream(resourceName)!!.readAllBytes()
}