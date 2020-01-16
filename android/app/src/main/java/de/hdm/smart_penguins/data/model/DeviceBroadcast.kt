package de.hdm.smart_penguins.data.model

import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.data.Constants.MAX_DATA_LENGTH
import de.hdm.smart_penguins.data.Constants.VAR_NOT_SET
import de.hdm.smart_penguins.utils.ByteArrayParser
import de.hdm.smart_penguins.utils.Util.setShort
import de.hdm.smart_penguins.utils.Util.setUnsignedChar
import de.hdm.smart_penguins.utils.Util.ternary

class DeviceBroadcast {
    var byteArray = ByteArray(MAX_DATA_LENGTH)
    var length: Int = VAR_NOT_SET
    var type: Int = VAR_NOT_SET
    var messageType: Int = VAR_NOT_SET
    var deviceType: Int = VAR_NOT_SET
    var direction: Int = VAR_NOT_SET
    var isEmergency: Boolean = false
    var isSlippery: Boolean = false
    var isJam: Boolean = false
    var deviceId: Int = VAR_NOT_SET


    fun init(
        length: Int,
        type: Int,
        messageType: Int,
        deviceType: Int,
        direction: Int,
        isSlippery: Boolean,
        isEmergency: Boolean,
        isJam: Boolean,
        deviceId: Int
    ): ByteArray {
        setUnsignedChar(byteArray, 0, length)
        setUnsignedChar(byteArray, 1, type)
        setShort(byteArray, 2, messageType)
        setUnsignedChar(byteArray, 4, deviceType)
        setUnsignedChar(byteArray, 5, direction)
        setUnsignedChar(byteArray, 6, ternary(isEmergency, 1, 0))
        setUnsignedChar(byteArray, 7, ternary(isSlippery, 1, 0))
        setUnsignedChar(byteArray, 8, ternary(isJam, 1, 0))
        setShort(byteArray, 9, deviceId)
        return byteArray
    }

    fun initWithBytes(byteArray: ByteArray): DeviceBroadcast {
        val parser = ByteArrayParser(Constants.OFFSET_MESSAGE_DEVICE_BROADCAST)
        length = parser.readSwappedUnsignedByte(byteArray).toInt()
        type = parser.readSwappedUnsignedByte(byteArray).toInt()
        messageType = parser.readSwappedUnsignedShort(byteArray)
        deviceType = parser.readSwappedUnsignedByte(byteArray).toInt()
        direction = parser.readSwappedUnsignedByte(byteArray).toInt()
        isEmergency = parser.readSwappedUnsignedByte(byteArray).toInt() == 1
        isSlippery = parser.readSwappedUnsignedByte(byteArray).toInt() == 1
        isJam = parser.readSwappedUnsignedByte(byteArray).toInt() == 1
        deviceId = parser.readSwappedUnsignedShort(byteArray)
        return this
    }

}


