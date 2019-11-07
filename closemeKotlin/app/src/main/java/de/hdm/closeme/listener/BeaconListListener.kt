package de.hdm.closeme.listener

import de.hdm.closeme.model.AlarmBeacon
import de.hdm.closeme.model.AlarmBeaconList

interface BeaconListListener {
    fun onBeaconListChanged(list : AlarmBeaconList)
    fun onNotificationStatusChanged()
}