package com.vitorpamplona.negentropy.message

class ByteArrayReader(private val buf: ByteArray) {
    private var pos: Int = 0

    fun available() = buf.size - pos

    fun readInt() = if ((pos < buf.size)) (buf[pos++].toInt() and 0xff) else -1

    fun readByte() = if ((pos < buf.size)) buf[pos++] else -1

    fun readNBytes(len: Int): ByteArray {
        require(len >= 0) { "len < 0" }
        require(pos + len <= buf.size) { "len < buf.size" }

        val result = buf.copyOfRange(pos, pos + len)
        pos += len
        return result
    }
}