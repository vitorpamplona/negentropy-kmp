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
package com.vitorpamplona.negentropy.storage

import com.vitorpamplona.negentropy.fingerprint.Fingerprint
import com.vitorpamplona.negentropy.fingerprint.FingerprintCalculator

/**
 * Sorted, sealed snapshot of (timestamp, id) items backing a Negentropy session.
 *
 * Items are kept in two parallel primitive buffers rather than a list of objects: a
 * [LongArray] of timestamps and one contiguous [ByteArray] holding every 32-byte id
 * back to back (id `i` occupies bytes `[i * Id.SIZE, (i + 1) * Id.SIZE)`). A snapshot of
 * `n` items is therefore two allocations, not `3n` ([StorageUnit] + [Id] + its byte array)
 * kept alive for the whole session. That removes the per-id allocation and GC pressure
 * that dominated loading, keeps the sorted data cache-friendly, and lets the fingerprint
 * walk and the bound binary search read straight out of the buffer with no boxing. This
 * mirrors how the C++ reference stores ids inline.
 *
 * [StorageUnit]/[Id] objects are still materialized lazily at the API boundary (e.g.
 * [getItem], [forEach], the ids handed back to callers), so the public contract is
 * unchanged.
 */
class StorageVector : IStorage {
    private var timestamps = LongArray(INITIAL_CAPACITY)
    private var ids = ByteArray(INITIAL_CAPACITY * Id.SIZE)
    private var count = 0
    private var sealed = false
    private val fingerprintCalculator = FingerprintCalculator()

    override fun insert(
        timestamp: Long,
        idHex: String,
    ) {
        check(!sealed) { "already sealed" }
        // matches the fail-fast size check the old insert(timestamp, Id(idHex)) path enforced
        require(idHex.length == Id.SIZE * 2) { "Id with invalid size. Expected ${Id.SIZE * 2} hex chars, found ${idHex.length}" }
        val slot = allocateSlot()
        // decode the id straight into its slot; no intermediate Id/ByteArray is allocated
        HashedByteArray.hexInto(idHex, ids, slot * Id.SIZE)
        timestamps[slot] = timestamp
    }

    override fun insert(
        timestamp: Long,
        id: Id,
    ) {
        check(!sealed) { "already sealed" }
        require(id.bytes.size == Id.SIZE) { "Id with invalid size. Expected ${Id.SIZE}, found ${id.bytes.size}" }
        val slot = allocateSlot()
        id.bytes.copyInto(ids, slot * Id.SIZE)
        timestamps[slot] = timestamp
    }

    private fun allocateSlot(): Int {
        ensureCapacity(count + 1)
        return count++
    }

    private fun ensureCapacity(minCapacity: Int) {
        val currentCapacity = timestamps.size
        if (minCapacity <= currentCapacity) return

        var newCapacity = currentCapacity + (currentCapacity shr 1) // grow ~1.5x
        if (newCapacity < minCapacity) newCapacity = minCapacity

        timestamps = timestamps.copyOf(newCapacity)
        ids = ids.copyOf(newCapacity * Id.SIZE)
    }

    override fun seal() {
        sealed = true

        val n = count

        // Sort item indices by (timestamp, id bytes), then gather the timestamps and ids
        // into fresh, exactly-sized buffers in sorted order. Sorting an index array avoids
        // moving 40-byte records during partitioning; the gather is a single sequential pass.
        //
        // Timestamps are the primary key, so we LSD radix-sort the indices on the timestamp
        // (linear and cache-friendly, no per-comparison indirection) and only fall back to a
        // comparison sort to break ties within equal-timestamp runs by id. This produces
        // exactly the same order as a full (timestamp, id) comparison sort.
        val order = radixSortIndicesByTimestamp(n)
        resolveTimestampTiesById(order, n)

        val sortedTimestamps = LongArray(n)
        val sortedIds = ByteArray(n * Id.SIZE)
        for (i in 0 until n) {
            val src = order[i]
            sortedTimestamps[i] = timestamps[src]
            ids.copyInto(sortedIds, i * Id.SIZE, src * Id.SIZE, src * Id.SIZE + Id.SIZE)
        }
        timestamps = sortedTimestamps
        ids = sortedIds

        // checks if there are no duplicates (adjacent equal items after sorting)
        for (i in 1 until n) {
            check(compareItems(i - 1, i) != 0) { "duplicate item inserted" }
        }
    }

    override fun unseal() {
        sealed = false
    }

    override fun size() = count

    override fun getItem(index: Int): StorageUnit {
        checkSealed()
        return unitAt(index)
    }

    override fun <T> map(run: (StorageUnit) -> T): List<T> {
        checkSealed()
        val list = ArrayList<T>(count)
        for (i in 0 until count) {
            list.add(run(unitAt(i)))
        }
        return list
    }

    override fun <T> map(
        begin: Int,
        end: Int,
        run: (StorageUnit) -> T,
    ): List<T> {
        checkSealed()
        checkBounds(begin, end)

        if (begin == end) return emptyList()

        val list = ArrayList<T>(end - begin)
        for (i in begin until end) {
            list.add(run(unitAt(i)))
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
            run(unitAt(i))
        }
    }

    override fun findTimestamp(id: Id): Long {
        val target = id.bytes
        for (i in 0 until count) {
            if (idEquals(i, target)) return timestamps[i]
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
            if (!shouldContinue(unitAt(i), i)) break
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

        // Binary search for the first index whose item is >= bound (the insertion point when
        // absent, the match index when present), comparing straight against the flat buffers
        // so no StorageUnit/Id is allocated per search.
        var low = begin
        var high = end - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cmp = compareItemToBound(mid, bound)
            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return low
    }

    override fun fingerprint(
        begin: Int,
        end: Int,
    ): Fingerprint {
        checkSealed()
        checkBounds(begin, end)
        return fingerprintCalculator.runFlat(ids, begin, end)
    }

    /**
     * The flat, sorted id buffer: id [index] occupies bytes `[index * Id.SIZE, (index + 1) *
     * Id.SIZE)`. Exposed so accelerated storages layered on this one (e.g. the prefix-sum
     * table) can read ids in bulk without materializing an [Id] per item. Only valid while
     * sealed, and callers must treat it as read-only.
     */
    internal fun idBuffer(): ByteArray = ids

    // ---------------------------------------------------------------------
    // internals
    // ---------------------------------------------------------------------

    private fun unitAt(index: Int): StorageUnit = StorageUnit(timestamps[index], idAt(index))

    private fun idAt(index: Int): Id {
        val offset = index * Id.SIZE
        return Id(ids.copyOfRange(offset, offset + Id.SIZE))
    }

    private fun idEquals(
        index: Int,
        target: ByteArray,
    ): Boolean {
        val offset = index * Id.SIZE
        for (k in 0 until Id.SIZE) {
            if (ids[offset + k] != target[k]) return false
        }
        return true
    }

    /** Compares the sorted items at [a] and [b] by (timestamp, unsigned id bytes). */
    private fun compareItems(
        a: Int,
        b: Int,
    ): Int {
        val ta = timestamps[a]
        val tb = timestamps[b]
        if (ta != tb) return if (ta < tb) -1 else 1

        val oa = a * Id.SIZE
        val ob = b * Id.SIZE
        for (k in 0 until Id.SIZE) {
            val x = ids[oa + k].toInt() and 0xFF
            val y = ids[ob + k].toInt() and 0xFF
            if (x != y) return x - y
        }
        return 0
    }

    /**
     * Compares the item at [index] against [bound], matching StorageUnit/HashedByteArray
     * ordering: timestamp first, then the id bytes against the (possibly shorter) bound
     * prefix; if the prefix is a prefix of the id, the shorter prefix sorts first.
     */
    private fun compareItemToBound(
        index: Int,
        bound: Bound,
    ): Int {
        val ts = timestamps[index]
        if (ts != bound.timestamp) return if (ts < bound.timestamp) -1 else 1

        val prefix = bound.prefix.bytes
        val offset = index * Id.SIZE
        val shared = if (prefix.size < Id.SIZE) prefix.size else Id.SIZE
        for (k in 0 until shared) {
            val x = ids[offset + k].toInt() and 0xFF
            val y = prefix[k].toInt() and 0xFF
            if (x != y) return x - y
        }
        // all shared bytes equal: the longer sequence sorts after the shorter one
        return Id.SIZE.compareTo(prefix.size)
    }

    // ---- timestamp radix sort + id tie-break ----

    /**
     * Stable LSD radix sort of the item indices by timestamp. Timestamps are XORed with
     * [Long.MIN_VALUE] so signed-Long order matches unsigned byte order (correct even for
     * negative timestamps), and byte passes over lanes that are constant across all items
     * are skipped, so typical timestamps (which vary in only a few low bytes) cost only a
     * few linear passes.
     */
    private fun radixSortIndicesByTimestamp(n: Int): IntArray {
        var from = IntArray(n) { it }
        if (n < 2) return from

        // Find which byte lanes actually differ between items so constant passes are skipped.
        var orAll = 0L
        var andAll = -1L
        for (i in 0 until n) {
            val key = timestamps[i] xor Long.MIN_VALUE
            orAll = orAll or key
            andAll = andAll and key
        }
        val varying = orAll xor andAll

        var to = IntArray(n)
        val counts = IntArray(RADIX + 1)
        var shift = 0
        while (shift < Long.SIZE_BITS) {
            if ((varying ushr shift) and 0xFFL != 0L) {
                counts.fill(0)
                for (i in 0 until n) {
                    val d = ((timestamps[from[i]] xor Long.MIN_VALUE) ushr shift and 0xFFL).toInt()
                    counts[d + 1]++
                }
                for (i in 0 until RADIX) counts[i + 1] += counts[i]
                for (i in 0 until n) {
                    val d = ((timestamps[from[i]] xor Long.MIN_VALUE) ushr shift and 0xFFL).toInt()
                    to[counts[d]++] = from[i]
                }
                val tmp = from
                from = to
                to = tmp
            }
            shift += 8
        }
        return from
    }

    /**
     * After the timestamp radix sort, items sharing a timestamp are contiguous but not yet
     * ordered by id. Sort each such run by id (timestamps are equal, so [compareItems] falls
     * through to the id bytes). Runs are usually tiny; large runs use the O(k log k)
     * quicksort rather than an O(k^2) insertion sort.
     */
    private fun resolveTimestampTiesById(
        order: IntArray,
        n: Int,
    ) {
        var i = 0
        while (i < n) {
            val ts = timestamps[order[i]]
            var j = i + 1
            while (j < n && timestamps[order[j]] == ts) j++
            if (j - i > 1) quicksortIndices(order, i, j - 1)
            i = j
        }
    }

    // ---- index quicksort (median-of-three, insertion-sort cutoff) ----

    private fun quicksortIndices(
        order: IntArray,
        lowStart: Int,
        highStart: Int,
    ) {
        var low = lowStart
        var high = highStart
        // Recurse into the smaller side, loop on the larger, to bound stack depth to O(log n).
        while (low < high) {
            if (high - low < INSERTION_SORT_CUTOFF) {
                insertionSortIndices(order, low, high)
                return
            }

            val p = partition(order, low, high)
            if (p - low < high - p) {
                quicksortIndices(order, low, p - 1)
                low = p + 1
            } else {
                quicksortIndices(order, p + 1, high)
                high = p - 1
            }
        }
    }

    private fun partition(
        order: IntArray,
        low: Int,
        high: Int,
    ): Int {
        val mid = (low + high) ushr 1
        // median-of-three pivot selection on low/mid/high to avoid quadratic behavior
        if (compareByOrder(order, mid, low) < 0) swap(order, mid, low)
        if (compareByOrder(order, high, low) < 0) swap(order, high, low)
        if (compareByOrder(order, high, mid) < 0) swap(order, high, mid)
        // place pivot (order[mid]) at high-1
        swap(order, mid, high - 1)
        val pivot = order[high - 1]

        var i = low
        var j = high - 1
        while (true) {
            while (compareIndexToItem(order[++i], pivot) < 0) { /* advance */ }
            while (compareIndexToItem(order[--j], pivot) > 0) { /* advance */ }
            if (i >= j) break
            swap(order, i, j)
        }
        swap(order, i, high - 1) // restore pivot
        return i
    }

    private fun insertionSortIndices(
        order: IntArray,
        low: Int,
        high: Int,
    ) {
        for (i in low + 1..high) {
            val v = order[i]
            var j = i - 1
            while (j >= low && compareIndexToItem(order[j], v) > 0) {
                order[j + 1] = order[j]
                j--
            }
            order[j + 1] = v
        }
    }

    private fun compareByOrder(
        order: IntArray,
        a: Int,
        b: Int,
    ): Int = compareItems(order[a], order[b])

    /** Compares two original item indices (pre-sort buffers). */
    private fun compareIndexToItem(
        a: Int,
        b: Int,
    ): Int = compareItems(a, b)

    private fun swap(
        order: IntArray,
        a: Int,
        b: Int,
    ) {
        val t = order[a]
        order[a] = order[b]
        order[b] = t
    }

    private fun checkSealed() = check(sealed) { "not sealed" }

    private fun checkBounds(
        begin: Int,
        end: Int,
    ) = check(begin <= end && end <= count) { "bad range" }

    companion object {
        private const val INITIAL_CAPACITY = 16
        private const val INSERTION_SORT_CUTOFF = 32
        private const val RADIX = 256
    }
}
