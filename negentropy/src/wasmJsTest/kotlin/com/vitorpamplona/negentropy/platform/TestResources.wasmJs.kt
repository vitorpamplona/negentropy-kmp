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
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.vitorpamplona.negentropy.platform

/** Read the given resource as binary data. */
actual fun readTestResource(resourceName: String): ByteArray {
    val buffer =
        if (isNodeJs()) {
            // Node.js: resources are in the kotlin/ output directory
            nodeReadFileSync("kotlin$resourceName")
        } else {
            // Browser (karma): resources are served at /base/kotlin/ by the karma server
            browserFetchSync("/base/kotlin$resourceName")
        }
    return buffer.toByteArray()
}

private fun isNodeJs(): Boolean = js("typeof window === 'undefined'")

private fun nodeReadFileSync(path: String): JsAny = js("require('fs').readFileSync(path)")

private fun browserFetchSync(url: String): JsAny = js("new TextEncoder().encode((function(u){var x=new XMLHttpRequest();x.open('GET',u,false);x.send(null);return x.responseText;})(url))")

private fun JsAny.toByteArray(): ByteArray {
    val length = getLength(this)
    val result = ByteArray(length)
    for (i in 0 until length) {
        result[i] = getByteAt(this, i)
    }
    return result
}

private fun getLength(buffer: JsAny): Int = js("buffer.length")

private fun getByteAt(
    buffer: JsAny,
    index: Int,
): Byte = js("buffer[index]")
