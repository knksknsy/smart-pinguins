package de.hdm.smart_penguins.data.manager

import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.data.model.DeviceBroadcast
import de.hdm.smart_penguins.data.model.PersistentNode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject constructor(
    var application: SmartApplication
) {
    var qrScannedNodes =  ArrayList<PersistentNode>()
    private val TAG = "DATA_MANAGER"
    var isSlippery = false
    var isJam = false
    var isEmergency = false
    var direction = 1
    var type = 0
    var device = Constants.DEVICE_TYPE_CAR


    fun getDeviceBroadcast(): ByteArray {
        return DeviceBroadcast().init(
            Constants.MESSAGE_SIZE_DEVICE_BROADCAST,
            type,
            Constants.MESSAGE_TYPE_DEVICE_BROADCAST,
            device,
            direction,
            isSlippery,
            isEmergency,
            isJam
        )
    }
}






