package de.hdm.smart_penguins.data.model

import de.hdm.smart_penguins.utils.Util.setShort
import de.hdm.smart_penguins.utils.Util.setUUID
import de.hdm.smart_penguins.utils.Util.setUnsignedChar
import java.util.*

class DeviceBroadcast(
    length: Int,
    type: Int,
    uuid: UUID,
    messageType: Int,
    advertisingChannel: Int,
    deviceType: Int,
    direction: Int,
    isSlippery: Boolean,
    isEmergency: Boolean
) {
    lateinit var byteArray: ByteArray

    init {
        this.byteArray = ByteArray(length)
        setUnsignedChar(byteArray, 0, length)
        setUnsignedChar(byteArray, 1, type)
        setUUID(byteArray, 2, uuid)
        setShort(byteArray, 4, messageType)
        setUnsignedChar(byteArray, 5, advertisingChannel)
        setUnsignedChar(byteArray, 6, deviceType)
        setUnsignedChar(byteArray, 7, direction)
        setUnsignedChar(
            byteArray, 8, if (isSlippery) {
                1
            } else {
                0
            }
        )
        setUnsignedChar(
            byteArray, 9, if (isEmergency) {
                1
            } else {
                0
            }
        )


    }

}


