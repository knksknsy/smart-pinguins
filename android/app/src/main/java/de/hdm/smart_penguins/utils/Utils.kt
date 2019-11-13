package de.hdm.smart_penguins.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.zip.GZIPInputStream

object Util {

    private val hexArray = "0123456789ABCDEF".toCharArray()

    @Throws(IOException::class)
    fun stream2file(`in`: InputStream): File {
        val tempFile = File.createTempFile(PREFIX, SUFFIX)
        tempFile.deleteOnExit()
        write2File(`in`, tempFile)
        return tempFile
    }

    @Throws(IOException::class)
    fun write2File(`is`: InputStream, importedProfile: File) {
        val os = FileOutputStream(importedProfile)
        val buff = ByteArray(1024)
        var len: Int
        while ((len = `is`.read(buff)) > 0) {
            os.write(buff, 0, len)
        }
        `is`.close()
        os.close()
    }


    fun decodeQRCode(data: String): ByteArray? {
        val encodingTable = CharArray(128)
        val decodingTable = CharArray(256)
        for (i in 0..25) {
            encodingTable[i] = (i + 'A').toChar()
            encodingTable[i + 26] = (i + 'a').toChar()
            encodingTable[i + 64] = (i + 'A'.toInt() + 128).toChar()
            encodingTable[i + 26 + 64] = (i + 'a'.toInt() + 128).toChar()
        }
        for (i in 0..9) {
            encodingTable[i + 26 * 2] = (i + '0').toChar()
            encodingTable[i + 26 * 2 + 64] = (i + '0'.toInt() + 128).toChar()
        }
        encodingTable[26 * 2 + 10] = '+'
        encodingTable[26 * 2 + 11] = '/'
        encodingTable[26 * 2 + 10 + 64] = ('+'.toInt() + 128).toChar()
        encodingTable[26 * 2 + 11 + 64] = ('/'.toInt() + 128).toChar()
        for (i in encodingTable.indices) {
            decodingTable[encodingTable[i]] = i.toChar()
        }
        var inLength = data.length
        val string = data.toCharArray()
        if (inLength == 0 || inLength % 8 != 0) {
            return null
        }

        while (inLength > 0 && string[inLength - 1] == '=')
            inLength--

        val outLength = inLength * 7 / 8
        val output = ByteArray(outLength)
        var inpPos = 0
        var outPos = 0

        while (inpPos < inLength) {
            var value: Long = 0
            for (j in 0..7) {
                if (j < 2 || inpPos < inLength) {
                    val ch = string[inpPos++]
                    val decoded = decodingTable[ch].toLong()
                    val shift = (7 * (7 - j)).toShort()
                    value = value or (decoded shl shift)
                }
            }
            for (j in 0..6) {
                if (outPos < outLength) {
                    val shift = (8 * (6 - j)).toShort()
                    val decoded = (value shr shift).toByte()
                    output[outPos++] = decoded
                }
            }
        }
        return output
    }

    @Throws(IOException::class)
    fun decompress(compressed: ByteArray): String {
        val BUFFER_SIZE = 32
        val `is` = ByteArrayInputStream(compressed)
        val gis = GZIPInputStream(`is`, BUFFER_SIZE)
        val string = StringBuilder()
        val data = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while ((bytesRead = gis.read(data)) != -1) {
            string.append(String(data, 0, bytesRead))
        }
        gis.close()
        `is`.close()
        return string.toString()
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun incrementUnsignedInt(value: Long): Long {
        var value = value
        return if (value++ == 4294967296L) 0L else value
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j] and 0xFF
            hexChars[j * 2] = hexArray[v.ushr(4)]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] =
                ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun trimHex(hexString: String): String {
        var trimmedString = hexString.replace("0x".toRegex(), "")
        trimmedString = trimmedString.replace(":".toRegex(), "")
        return trimmedString
    }

    fun xorFile(key: ByteArray, data: ByteArray, length: Int) {
        for (i in 0 until length) {
            data[i] = (data[i] xor key[i]).toByte()
        }
    }


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

    fun getShort(data: ByteArray, byteIndex: Int): Int {
        return (data[byteIndex] and 0xff or (data[byteIndex + 1] and 0xff shl 8)).toShort() and 0xffff
    }

    fun setShort(data: ByteArray, byteIndex: Int, value: Int) {
        data[byteIndex] = (value and 0xff).toByte()
        data[byteIndex + 1] = (value shr 8 and 0xff).toByte()
    }

    fun getUnsignedChar(data: ByteArray, byteIndex: Int): Int {
        return data[byteIndex] and 0xff
    }

    fun setUnsignedChar(data: ByteArray, byteIndex: Int, value: Int) {
        data[byteIndex] = (value and 0xff).toByte()
    }

    fun isBitSet(data: ByteArray, byteIndex: Int, bit: Int): Boolean {
        return data[byteIndex] and (1 shl bit) > 0
    }

    fun setBit(data: ByteArray, byteIndex: Int, bit: Int, set: Boolean) {
        if (set) {
            data[byteIndex] = data[byteIndex] or (1 shl bit).toByte()
        } else {
            data[byteIndex] = data[byteIndex] and (1 shl bit).inv().toByte()
        }
    }

    fun setTunnelType(data: ByteArray, byteIndex: Int, type: Int) {
        data[byteIndex] = (type and 0x3).toByte()
    }


    fun getMessageType(data: ByteArray): Int {
        return getUnsignedChar(data, 0)
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

    fun hexToDecimal(hex: String): Int {
        var hex = hex
        val digits = "0123456789ABCDEF"
        hex = hex.toUpperCase()
        var `val` = 0
        for (i in 0 until hex.length) {
            val c = hex[i]
            val d = digits.indexOf(c.toInt())
            `val` = 16 * `val` + d
        }
        return `val`
    }

    fun getBitmapFromView(view: View?): Bitmap? {
        if (view != null) {
            val returnedBitmap =
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(returnedBitmap)
            val bgDrawable = view.background
            if (bgDrawable != null)
                bgDrawable.draw(canvas)
            else
                canvas.drawColor(Color.WHITE)
            view.draw(canvas)
            return returnedBitmap
        }
        return null
    }

}
