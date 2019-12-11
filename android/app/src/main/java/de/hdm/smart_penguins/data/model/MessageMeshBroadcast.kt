package de.hdm.smart_penguins.data.model

import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.utils.ByteArrayParser
import de.hdm.smart_penguins.utils.Util.getTypeForInt

class MessageMeshBroadcast(bytes: ByteArray) {
    val parser = ByteArrayParser(Constants.OFFSET_MESSAGE_MESH_BROADCAST)
    var length = parser.readSwappedUnsignedByte(bytes)
    var type = parser.readSwappedUnsignedByte(bytes)
    val mUuid = parser.readSwappedUnsignedShort(bytes)
    val messageType = parser.readSwappedUnsignedShort(bytes)
    val advChannel = parser.readSwappedUnsignedByte(bytes)
    val deviceNumber = parser.readSwappedUnsignedByte(bytes)
    val txPower = parser.readSwappedUnsignedByte(bytes)
    val boardType = getTypeForInt(parser.readSwappedByte(bytes))
    val currentClusterSize = parser.readSwappedUnsignedByte(bytes)
    val clusterSize = parser.readSwappedUnsignedByte(bytes)
    val networkId = parser.readSwappedUnsignedByte(bytes)
    val nearestTrafficJamNodeId = parser.readSwappedUnsignedByte(bytes)
    val nearestBlackIceNodeId = parser.readSwappedUnsignedByte(bytes)
    val nearestRescueLaneNodeId = parser.readSwappedUnsignedByte(bytes)

}

