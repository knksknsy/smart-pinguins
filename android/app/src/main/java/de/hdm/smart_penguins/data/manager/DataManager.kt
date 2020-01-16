package de.hdm.smart_penguins.data.manager

import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.data.Constants.VAR_NOT_SET
import de.hdm.smart_penguins.data.model.DeviceBroadcast
import de.hdm.smart_penguins.data.model.PersistentNode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject constructor(
    var application: SmartApplication
) {
    //mutual live data .value text rein
    //var isEmergency =
    var qrScannedNodes = ArrayList<PersistentNode>()
    private val TAG = "DATA_MANAGER"
    var isSlippery = false
    var isJam = false
    var isEmergency = false
    // (0 = North, 1-2 = NorthEast, 3 = East, 4-5 = SouthEast, 6 = South, 7-8 = SouthWest, 9 = West, 10 - 11 = NorthWest)
    var direction = 0
    var type = 0
    var device = Constants.DEVICE_TYPE_CAR
    var isRightTurn = false
    var isLeftTurn = false
    var deviceId = System.currentTimeMillis()


    fun getDeviceBroadcast(): ByteArray {
        return DeviceBroadcast().init(
            Constants.MESSAGE_SIZE_DEVICE_BROADCAST,
            type,
            Constants.MESSAGE_TYPE_DEVICE_BROADCAST,
            device,
            direction,
            isSlippery,
            isEmergency,
            isJam,
            deviceId.toInt()
        )
    }

    fun getDirectionForNode(deviceNumber: Short): Int {
        val node = qrScannedNodes.stream()
            .filter { id -> id.nodeID == deviceNumber.toLong() }
            .findFirst().orElse(null)
        if (node != null) {
            return node.direction.toInt()
        } else {
            return VAR_NOT_SET
        }
    }
}






