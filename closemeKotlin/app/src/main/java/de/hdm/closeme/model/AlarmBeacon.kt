package de.hdm.closeme.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.google.android.gms.maps.model.Marker
import de.hdm.closeme.constant.Constants
import java.util.*

@Entity(tableName = "alarm_beacon")
data class AlarmBeacon(@PrimaryKey(autoGenerate = false) var deviceNumber: Short,
                       @ColumnInfo(name = "beacon_type") var beaconType: String,
                       @ColumnInfo(name = "lat") var lat: Double,
                       @ColumnInfo(name = "lng") var lng: Double,
                       @ColumnInfo(name = "placed_type") var placedType: Int,
                       @ColumnInfo(name = "mac") var mac: String,
                       @Ignore var networkId: Short,
                       @Ignore var isActivated: Boolean,
                       @Ignore var isUsed: Boolean,
                       @Ignore var txPower: Int,
                       @Ignore var rssi: Int,
                       @Ignore var distance: Float,
                       @Ignore var clusterId: Short,
                       @Ignore var humidity: Short,
                       @Ignore var temperature: Short,
                       @Ignore var clusterSize: Short,
                       @Ignore var date: Date,
                       @Ignore var unsecuredSpotMap: TreeMap<Short, AlarmSpot>,
                       @Ignore var scanRecord: ScanRecord?,
                       @Ignore var isOpen: Boolean = false,
                       @Ignore var marker: Marker? = null,
                       @Ignore var spotCount: Short,
                       @Ignore var channel: Short) {

    constructor() : this(0,
            Constants.UNKOWN_BOARD,
            -1.0,
            -1.0,
            Constants.PLACE_TYPE_NOT_PLACED,
            "",
            0,
            false,
            false,
            0,
            Constants.NO_VALUE_RSSI,
            Constants.NO_VALUE_DISTANCE,
            Constants.NO_VALUE_CLUSTER_ID,
            Constants.NO_VALUE_HUMIDITY,
            Constants.NO_VALUE_TEMPERATURE,
            0,
            Calendar.getInstance().time,
            TreeMap(),
            null,
            false,
            null,
            0,
            Constants.NO_VALUE_CHANNEL)


    fun initWithAlarmBeacon(alarmBeaconMessage: AlarmBeaconMessage) {
        this.channel = alarmBeaconMessage.advChannel
        this.clusterId = alarmBeaconMessage.clusterId
        this.humidity = alarmBeaconMessage.humidity
        this.temperature = alarmBeaconMessage.temperature
        this.deviceNumber = alarmBeaconMessage.deviceNumber
        this.clusterSize = alarmBeaconMessage.clusterSize
        this.beaconType = alarmBeaconMessage.boardType
        this.spotCount = alarmBeaconMessage.spotCount
        this.mac = alarmBeaconMessage.device!!.address
        this.rssi = alarmBeaconMessage.rssi
        this.distance = alarmBeaconMessage.distance
        this.networkId = alarmBeaconMessage.networkId
        if (alarmBeaconMessage.meshDeviceId != Constants.NO_VALUE_SPOT_NUMBER) {
            this.unsecuredSpotMap.put(alarmBeaconMessage.index, AlarmSpot(
                    alarmBeaconMessage.meshDataTemperature,
                    alarmBeaconMessage.meshDataHumidity,
                    alarmBeaconMessage.meshDeviceId,
                    false,
                    this.isPlaced(),
                    Constants.ALERT_TEMPERATURE))
        }
        this.date = Calendar.getInstance().time
        this.isActivated = alarmBeaconMessage.isActivated
    }

    fun updateWithAlarmBeacon(alarmBeacon: AlarmBeacon) {
        this.clusterId = alarmBeacon.clusterId
        this.humidity = alarmBeacon.humidity
        this.temperature = alarmBeacon.temperature
        this.deviceNumber = alarmBeacon.deviceNumber
        this.clusterSize = alarmBeacon.clusterSize
        this.beaconType = alarmBeacon.beaconType
        this.spotCount = alarmBeacon.spotCount
        this.mac = alarmBeacon.mac
        this.rssi = alarmBeacon.rssi
        this.distance = alarmBeacon.distance
        this.unsecuredSpotMap = alarmBeacon.unsecuredSpotMap
        this.date = Calendar.getInstance().time
        if (this.lat != -1.0) this.lat = alarmBeacon.lat
        if (this.lng != -1.0) this.lng = alarmBeacon.lng
        if (this.placedType != Constants.PLACE_TYPE_NOT_PLACED) this.placedType = alarmBeacon.placedType
    }

    fun updateNonPersistentInformation(alarmBeacon: AlarmBeacon) {
        this.channel = alarmBeacon.channel
        this.clusterId = alarmBeacon.clusterId
        this.humidity = alarmBeacon.humidity
        this.temperature = alarmBeacon.temperature
        this.clusterSize = alarmBeacon.clusterSize
        this.spotCount = alarmBeacon.spotCount
        this.rssi = alarmBeacon.rssi
        this.distance = alarmBeacon.distance
        this.unsecuredSpotMap = alarmBeacon.unsecuredSpotMap
        this.date = Calendar.getInstance().time
        this.isActivated = alarmBeacon.isActivated
    }


    fun updateBeaconWithMessage(alarmBeaconMessage: AlarmBeaconMessage) {
        this.channel = alarmBeaconMessage.advChannel
        this.beaconType = alarmBeaconMessage.boardType
        this.deviceNumber = alarmBeaconMessage.deviceNumber
        this.date = Calendar.getInstance().time
        this.distance = alarmBeaconMessage.distance
        this.rssi = alarmBeaconMessage.rssi
        this.clusterSize = alarmBeaconMessage.clusterSize
        this.clusterId = alarmBeaconMessage.clusterId
        this.temperature = alarmBeaconMessage.temperature
        this.humidity = alarmBeaconMessage.humidity
        setAlarmSpotList(alarmBeaconMessage)
        this.spotCount = alarmBeaconMessage.spotCount
        this.isActivated = alarmBeaconMessage.isActivated

    }

    fun isPlaced(): Boolean {
        return this.placedType != Constants.PLACE_TYPE_NOT_PLACED
    }


    fun setAlarmSpotList(alarmBeaconMessage: AlarmBeaconMessage) {
        if (alarmBeaconMessage.meshDeviceId != Constants.NO_VALUE_SPOT_NUMBER) {
            val alarmSpot = AlarmSpot(
                    alarmBeaconMessage.meshDataHumidity,
                    alarmBeaconMessage.meshDataTemperature,
                    alarmBeaconMessage.meshDeviceId,
                    false,
                    this.isPlaced(),
                    Constants.ALERT_TEMPERATURE)

            if (this.spotCount != alarmBeaconMessage.spotCount) {
                resetUnsecuredSpotMap(alarmSpot,alarmBeaconMessage.index)
                return
            }
            if (unsecuredSpotMap[alarmBeaconMessage.index] == null ||
                    unsecuredSpotMap[alarmBeaconMessage.index]?.deviceNumber == alarmSpot.deviceNumber) {
                unsecuredSpotMap.put(alarmBeaconMessage.index, alarmSpot)
            } else {
                resetUnsecuredSpotMap(alarmSpot, alarmBeaconMessage.index)
            }
        } else {
            unsecuredSpotMap.clear()
        }
    }

    fun resetUnsecuredSpotMap(alarmSpot: AlarmSpot?, index: Short) {
        unsecuredSpotMap.clear()
        if (alarmSpot != null) unsecuredSpotMap.put(index, alarmSpot)
    }

    fun resetNonPersistenData() {
        this.clusterId = 0
        this.humidity = Constants.NO_VALUE_HUMIDITY
        this.temperature = Constants.NO_VALUE_TEMPERATURE
        this.clusterSize = 0
        this.rssi = Constants.NO_VALUE_RSSI
        this.distance = Constants.NO_VALUE_DISTANCE
        this.isActivated = false
    }

    fun resetPersitentData() {
        this.lat = -1.0
        this.lng = -1.0
        this.placedType = Constants.PLACE_TYPE_NOT_PLACED
    }

    fun isOpenWindow(): Boolean {
        return this.placedType == Constants.PLACE_TYPE_WINDOW_BEACON && this.isOpen
    }

    fun isInRange(): Boolean {
        return this.distance > Constants.NO_VALUE_DISTANCE && this.distance < Constants.PARAM_IN_RANGE_DISTANCE
    }

    fun setUsed() {
        this.isUsed = this.placedType != Constants.PLACE_TYPE_NOT_PLACED
    }
}

