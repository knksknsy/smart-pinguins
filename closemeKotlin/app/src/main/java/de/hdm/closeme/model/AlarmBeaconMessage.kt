package de.hdm.closeme.model

import de.hdm.closeme.constant.Constants
import de.hdm.closeme.util.BeaconUtil.Companion.getTypeForInt
import de.hdm.closeme.util.ByteArrayParser
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*
import kotlin.collections.ArrayList

class AlarmBeaconMessage(result: ScanResult, data: ByteArray) : BeaconMessage(result) {
    var length: Byte = 0
    var type: Byte = 0
    private var mUuid: Int = 0
    var messageType: Int = 0
    var advChannel: Short = 0
    var clusterId: Short = Constants.NO_VALUE_CLUSTER_ID
    var humidity: Short = Constants.NO_VALUE_HUMIDITY
    var temperature: Short = Constants.NO_VALUE_TEMPERATURE
    var deviceNumber: Short = 0
    var clusterSize: Short = 0
    var index: Short = 0
    var boardType: String = Constants.UNKOWN_BOARD
    var spotId: Short = 0
    var spotCount: Short = 0
    var meshDeviceId: Short = Constants.NO_VALUE_SPOT_NUMBER
    var meshDataTemperature : Short = 0
    var meshDataHumidity: Short = 0
    var networkId: Short = 0
    var date: Date = Calendar.getInstance().time
    var isActivated = false
    var meshDataType: Short = 0


    init {
        initWithBytes(data)
        date = Calendar.getInstance().time
        this.setDistance(result.rssi.toFloat(), txPower.toFloat())
    }



    fun initWithBytes(bytes: ByteArray) {
        val parser = ByteArrayParser(0)
        mUuid = parser.readSwappedUnsignedShort(bytes)
        messageType = parser.readSwappedUnsignedShort(bytes)
        advChannel = parser.readSwappedUnsignedByte(bytes)
        deviceNumber = parser.readSwappedUnsignedByte(bytes)
        txPower = parser.readSwappedByte(bytes).toInt()
        boardType = getTypeForInt(parser.readSwappedByte(bytes))
        temperature = parser.readSwappedByte(bytes)
        humidity = parser.readSwappedUnsignedByte(bytes)
        isActivated = parser.readSwappedUnsignedByte(bytes).toInt() == Constants.ACTIVE_STATE_ACTIVE
        clusterSize = parser.readSwappedUnsignedByte(bytes)
        clusterId = parser.readSwappedUnsignedByte(bytes)
        networkId = parser.readSwappedUnsignedByte(bytes)

        meshDataType = parser.readSwappedUnsignedByte(bytes)

        //Check if payload is from window sensor
        if(meshDataType == Constants.DATA_TYPE_WINDOW_VERSION_ONE) {
            spotCount = parser.readSwappedUnsignedByte(bytes)
            index = parser.readSwappedUnsignedByte(bytes)
            meshDeviceId = parser.readSwappedUnsignedByte(bytes)
            meshDataTemperature = parser.readSwappedByte(bytes)
            meshDataHumidity = parser.readSwappedUnsignedByte(bytes)
        }
    }

}
