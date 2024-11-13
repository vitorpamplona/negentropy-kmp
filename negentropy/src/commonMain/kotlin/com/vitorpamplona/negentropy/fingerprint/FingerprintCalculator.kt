package com.vitorpamplona.negentropy.fingerprint

import com.vitorpamplona.negentropy.fingerprint.Fingerprint.Companion.SIZE
import com.vitorpamplona.negentropy.message.encodeVarInt
import com.vitorpamplona.negentropy.platform.sha256
import com.vitorpamplona.negentropy.storage.IStorage
import com.vitorpamplona.negentropy.storage.Id

class FingerprintCalculator {
    companion object {
        // cached common result
        val ZERO_RANGE_FINGERPRINT = FingerprintCalculator().fingerprint(ByteArray(Id.SIZE) + encodeVarInt(0))
    }

    private val buf: ByteArray = ByteArray(Id.SIZE)

    private fun get4BytesAsLongInLittleEndian(bytes: ByteArray, offset: Int): Long {
        return (bytes[offset].toUInt().toLong() and 0xFF) or
                ((bytes[offset + 1].toUInt().toLong() and 0xFF) shl 8) or
                ((bytes[offset + 2].toUInt().toLong() and 0xFF) shl 16) or
                ((bytes[offset + 3].toUInt().toLong() and 0xFF) shl 24)
    }

    private fun putLongInLittleEndianAs4Bytes(bytes: ByteArray, offset: Int, value: Long) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun add(base: ByteArray, toAdd: ByteArray) {
        var currCarry = 0L

        for (i in 0 until 8) {
            val offset = i * 4

            val p = get4BytesAsLongInLittleEndian(base, offset)
            val po = get4BytesAsLongInLittleEndian(toAdd, offset)

            // must get 4 bytes from butter, convert signed to unsigned int and
            // place it in a bigger variable to allow the if below
            val next = p + currCarry + po

            putLongInLittleEndianAs4Bytes(base, offset, next)
            currCarry = if (next > 0xFFFFFFFF) 1 else 0
        }
    }

    fun run(storage: IStorage, begin: Int, end: Int): Fingerprint {
        if (begin == end) return ZERO_RANGE_FINGERPRINT

        buf.fill(0)

        storage.forEach(begin, end) { item ->
            add(buf, item.id.bytes)
        }

        return fingerprint(buf + encodeVarInt(end - begin))
    }

    private fun fingerprint(bytes: ByteArray) = Fingerprint(sha256(bytes).copyOfRange(0, SIZE))
}