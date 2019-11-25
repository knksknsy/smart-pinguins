package de.hdm.smart_penguins.utils

import de.hdm.smart_penguins.data.Constants
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

object Util {

    fun getInt(data: ByteArray, byteIndex: Int): Long {
        return toUnsignedInt(Arrays.copyOfRange(data, byteIndex, byteIndex + 4))
    }

    private fun toUnsignedInt(bytes: ByteArray): Long {
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put(bytes)
            .put(byteArrayOf(0, 0, 0, 0))
        buffer.position(0)
        return buffer.long
    }

    fun setInt(data: ByteArray, byteIndex: Int, value: Long) {
        val bytes = fromUnsignedInt(value)
        data[byteIndex] = bytes[0]
        data[byteIndex + 1] = bytes[1]
        data[byteIndex + 2] = bytes[2]
        data[byteIndex + 3] = bytes[3]
    }

    private fun fromUnsignedInt(value: Long): ByteArray {
        val bytes = ByteArray(8)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(value)
        return Arrays.copyOfRange(bytes, 0, 4)
    }


    fun setShort(data: ByteArray, byteIndex: Int, value: Int) {
        data[byteIndex] = (value and 0xff).toByte()
        data[byteIndex + 1] = (value shr 8 and 0xff).toByte()
    }

    fun setUnsignedChar(data: ByteArray, byteIndex: Int, value: Int) {
        data[byteIndex] = (value and 0xff).toByte()
    }

    fun setTunnelType(data: ByteArray, byteIndex: Int, type: Int) {
        data[byteIndex] = (type and 0x3).toByte()
    }


    fun setUUID(data: ByteArray, byteIndex: Int, uuid: UUID) {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        System.arraycopy(bb.array(), 0, data, byteIndex, data.size)
    }

    fun getUUID(data: ByteArray, byteIndex: Int): UUID {
        val byteBuffer = ByteBuffer.wrap(data)
        byteBuffer.get(data, byteIndex, 16)
        val high = byteBuffer.long
        val low = byteBuffer.long
        return UUID(high, low)
    }

    fun getTypeForInt(typeNumber: Short): String {
        when (typeNumber) {
            Constants.RUUVI_TAG -> return Constants.RUUVI_TAG_STRING
            Constants.ARCONNA -> return Constants.ARCONNA_STRING
            Constants.DEV_BOARD -> return Constants.DEV_BOARD_STRING
            else -> return Constants.UNKOWN_BOARD
        }
    }
}
