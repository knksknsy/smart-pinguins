package de.hdm.closeme.listener

import de.hdm.closeme.model.AlarmBeaconList

interface BluetoothServiceListener {
    fun onAlarmBeaconFound(beaconList: AlarmBeaconList)
    fun onDoorBeaconAlert(deviceNumber: String)
    fun onNotificationStatusChanged()
}