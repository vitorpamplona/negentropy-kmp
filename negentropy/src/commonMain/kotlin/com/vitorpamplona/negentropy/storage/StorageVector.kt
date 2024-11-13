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
package com.vitorpamplona.negentropy.storage

class StorageVector : IStorage {
    private val items = mutableListOf<StorageUnit>()
    private var sealed = false

    override fun insert(
        timestamp: Long,
        idHex: String,
    ) = insert(timestamp, Id(idHex))

    override fun insert(
        timestamp: Long,
        id: Id,
    ) {
        check(!sealed) { "already sealed" }

        items.add(StorageUnit(timestamp, id))
    }

    override fun seal() {
        sealed = true

        items.sort()

        // checks if there are no duplicates
        for (i in 1 until items.size) {
            check(items[i - 1].compareTo(items[i]) != 0) { "duplicate item inserted" }
        }
    }

    override fun unseal() {
        sealed = false
    }

    override fun size() = items.size

    override fun getItem(index: Int): StorageUnit {
        checkSealed()
        return items[index]
    }

    override fun <T> map(run: (StorageUnit) -> T): List<T> {
        checkSealed()
        return items.map(run)
    }

    override fun <T> map(
        begin: Int,
        end: Int,
        run: (StorageUnit) -> T,
    ): List<T> {
        checkSealed()
        checkBounds(begin, end)

        if (begin == end) return emptyList()

        val list = mutableListOf<T>()

        for (i in begin until end) {
            list.add(run(items[i]))
        }

        return list
    }

    override fun forEach(
        begin: Int,
        end: Int,
        run: (StorageUnit) -> Unit,
    ) {
        checkSealed()
        checkBounds(begin, end)

        if (begin == end) return

        for (i in begin until end) {
            run(items[i])
        }
    }

    override fun findTimestamp(id: Id): Long {
        for (i in items.indices) {
            if (items[i].id.equalsId(id)) {
                return items[i].timestamp
            }
        }
        return -1
    }

    override fun iterate(
        begin: Int,
        end: Int,
        shouldContinue: (StorageUnit, Int) -> Boolean,
    ) {
        checkSealed()
        checkBounds(begin, end)

        if (begin == end) return

        for (i in begin until end) {
            if (!shouldContinue(items[i], i)) break
        }
    }

    override fun indexAtOrBeforeBound(
        bound: Bound,
        begin: Int,
        end: Int,
    ): Int {
        checkSealed()
        checkBounds(begin, end)

        if (begin == end) return begin

        val second = items.binarySearch(bound.toStorage(), begin, end)

        // if the item is not found, it returns negative
        return if (second < 0) -second - 1 else second
    }

    private fun checkSealed() = check(sealed) { "not sealed" }

    private fun checkBounds(
        begin: Int,
        end: Int,
    ) = check(begin <= end && end <= items.size) { "bad range" }
}
