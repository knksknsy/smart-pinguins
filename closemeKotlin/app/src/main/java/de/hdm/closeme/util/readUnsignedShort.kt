package de.hdm.closeme.util

fun readUnsignedShort(bytes: ByteArray): Int {
        val s = (bytes[pointer]).toShort() + (bytes[pointer + 1]).toShort()
        pointer += 2
        return s
    }