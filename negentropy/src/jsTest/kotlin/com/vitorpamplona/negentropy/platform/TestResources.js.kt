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
package com.vitorpamplona.negentropy.platform

/** Read the given resource as binary data. */
actual fun readTestResource(resourceName: String): ByteArray {
    // unsafeCast avoids Kotlin/JS IR runtime type-check failures on JS primitives.
    val typeofWindow = js("typeof window").unsafeCast<String>()
    return if (typeofWindow == "undefined") {
        // Node.js: resources are in the kotlin/ output directory.
        // Read as UTF-8 text to avoid Node.js Buffer-to-ArrayBuffer cast issues.
        val path = "kotlin$resourceName"
        val content = js("require('fs').readFileSync(path, 'utf8')").unsafeCast<String>()
        content.encodeToByteArray()
    } else {
        // Browser (karma): resources are served by the karma server at /base/kotlin/<name>.
        // Use synchronous XHR to fetch the file content.
        val url = "/base/kotlin$resourceName"
        val content = js("(function(u){var x=new XMLHttpRequest();x.open('GET',u,false);x.send(null);return x.responseText;})(url)").unsafeCast<String>()
        content.encodeToByteArray()
    }
}
