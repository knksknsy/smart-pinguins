package de.hdm.smart_penguins.utils

import kotlin.experimental.and

class ByteArrayParser(offset: Int) {

    private var pointer = 0

    init {
        pointer = offset
    }

    fun readUnsignedShort(bytes: ByteArray): Int {
        val s = (bytes[pointer]).toShort() + (bytes[pointer + 1]).toShort()
        pointer += 2
        return s
    }

    fun readSwappedUnsignedShort(bytes: ByteArray): Int {
        val i = readSwappedUnsignedShort(bytes, pointer)
        pointer += 2
        return i
    }


    fun readSwappedUnsignedByte(bytes: ByteArray): Short {
        val s = (bytes[pointer].toShort() and 0xff).toShort()
        pointer += 1
        return s
    }

    fun readSwappedByte(bytes: ByteArray): Short {
        val s = bytes[pointer].toShort()
        pointer += 1
        return s
    }

    private fun readSwappedUnsignedShort(data: ByteArray, offset: Int): Int {
        return (data[offset + 0] and 0xff.toByte()) + (data[offset + 1] and 0xff.toByte())
    }

}