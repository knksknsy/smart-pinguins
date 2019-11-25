package de.hdm.smart_penguins.data.model

import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.utils.ByteArrayParser
import de.hdm.smart_penguins.utils.Util.getTypeForInt

class MessageMeshBroadcast(bytes: ByteArray) {
    val parser = ByteArrayParser(Constants.OFFSET_MESSAGE_BROADCAST)
    var length = parser.readSwappedUnsignedByte(bytes)
    var type = parser.readSwappedUnsignedByte(bytes)
    val mUuid = parser.readSwappedUnsignedShort(bytes)
    val messageType = parser.readSwappedUnsignedShort(bytes)
    val advChannel = parser.readSwappedUnsignedByte(bytes)
    val deviceNumber = parser.readSwappedUnsignedByte(bytes)
    val boardType = getTypeForInt(parser.readSwappedByte(bytes))
    val clusterSize = parser.readSwappedUnsignedByte(bytes)
    val clusterId = parser.readSwappedUnsignedByte(bytes)
    val networkId = parser.readSwappedUnsignedByte(bytes)
}

