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
package com.vitorpamplona.negentropy.platform

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread

/** Read the given resource as binary data. */
@OptIn(ExperimentalForeignApi::class)
actual fun readTestResource(resourceName: String): ByteArray {
    println("Reading Test Resource $resourceName")
    val file = fopen("src/commonTest/resources$resourceName", "rb")
    val bufferSize = 8 * 1024
    val byteArray = mutableListOf<Byte>()

    try {
        memScoped {
            val buffer = allocArray<ByteVar>(bufferSize)
            do {
                val size = fread(buffer, 1u, bufferSize.toULong(), file)
                byteArray.addAll(buffer.readBytes(size.toInt()).asIterable())
            } while (size > 0u)
        }
    } finally {
        fclose(file)
    }
    return byteArray.toByteArray()
}
