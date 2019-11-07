package de.hdm.closeme.model

import android.content.Context
import android.text.TextUtils
import de.hdm.closeme.MainActivity
import de.hdm.closeme.constant.Constants
import java.util.*
import kotlin.collections.ArrayList

class AlarmBeaconList : ArrayList<AlarmBeacon>() {

    fun addOrUpdate(beaconMessage: AlarmBeaconMessage): AlarmBeacon {
        for (beacon in this) {
            if (TextUtils.equals(beacon.mac, beaconMessage.device!!.address)) {
                beacon.updateBeaconWithMessage(beaconMessage)
                return beacon
            }
        }
        val alarmBeacon = AlarmBeacon()
        alarmBeacon.initWithAlarmBeacon(beaconMessage)
        this.add(alarmBeacon)
        return alarmBeacon
    }

    fun removeExpiredBeacon(expiryTimeInMS: Int) {
        val iterator: MutableIterator<AlarmBeacon> = this.iterator()
        while (iterator.hasNext()) {
            val beacon = iterator.next();
            if ((Calendar.getInstance().timeInMillis - beacon.date.time) > expiryTimeInMS) {
                if (beacon.isPlaced()) {
                    beacon.resetUnsecuredSpotMap(null, 0)
                    beacon.resetNonPersistenData()
                } else {
                    iterator.remove()
                }
            }
        }
    }

    fun removeUnusedBeacons(): AlarmBeaconList {
        val usedBeaconList = AlarmBeaconList()
        for (beacon: AlarmBeacon in this) {
            if (beacon.isUsed) usedBeaconList.add(beacon)
        }
        return usedBeaconList
    }

    fun addPersistentBeacons(persistentBeaconList: List<AlarmBeacon>) {
        for (persistentBeacon: AlarmBeacon in persistentBeaconList) {
            persistentBeacon.setUsed()
            var used = false
            for (beacon: AlarmBeacon in this) {
                if (persistentBeacon.deviceNumber == beacon.deviceNumber) {
                    beacon.placedType = persistentBeacon.placedType
                    beacon.lat = persistentBeacon.lat
                    beacon.lng = persistentBeacon.lng
                    used = true
                }
            }
            if (!used) this.add(persistentBeacon)
        }
    }

    fun sortByRange() {
        if (this.size != 0) {
            val sortedList = ArrayList(this.sortedWith(compareBy({ it.distance == Constants.NO_VALUE_DISTANCE }, { it.distance })))
            this.clear()
            this.addAll(sortedList)
        }
    }

    fun getUnsecuredSpotMap(): TreeMap<Short, AlarmSpot> {
        val rangedList = this.getInRangedBeacons()
        val sortedList = ArrayList(rangedList.sortedWith(compareBy({ it.distance }, { it.unsecuredSpotMap.size })))
        for (beacon: AlarmBeacon in sortedList) {
            if (beacon.unsecuredSpotMap.size == beacon.spotCount.toInt()) return beacon.unsecuredSpotMap
        }
        return TreeMap()
    }

    fun getInRangedBeacons(): AlarmBeaconList {
        val list = AlarmBeaconList()
        for (beacon: AlarmBeacon in this) {
            if (beacon.distance != Constants.NO_VALUE_DISTANCE) {
                list.add(beacon)
            }
        }
        return list
    }

    fun getPersistentBeacons(): AlarmBeaconList {
        val list = AlarmBeaconList()
        for (beacon: AlarmBeacon in this) {
            if (beacon.isPlaced()) {
                list.add(beacon)
            }
        }
        return list
    }

    fun getBeaconForNumber(deviceNumber: Short): AlarmBeacon? {
        for (beacon: AlarmBeacon in this) {
            if (beacon.deviceNumber == deviceNumber) return beacon
        }
        return null
    }

    fun setUnsecuredBeacons(unsecuredSpotList: TreeMap<Short, AlarmSpot>) {
        for (beacon: AlarmBeacon in this) {
            var open = false
            for (spot: MutableMap.MutableEntry<Short, AlarmSpot> in unsecuredSpotList) {
                if (beacon.deviceNumber == spot.value.deviceNumber) open = true
            }
            beacon.isOpen = open
        }
    }

    fun getAlarmSpotList(context: Context): TreeMap<Short, AlarmSpot> {
        if (this.size > 0) {
            val treeMap = TreeMap<Short, AlarmSpot>()
            for (entry: Map.Entry<Short, AlarmSpot> in this[0].unsecuredSpotMap) {
                for (alert in ((context) as MainActivity).getSettings().alertList) {
                    if (entry.value.deviceNumber == alert.deviceNumber ) {
                        entry.value.humdity = alert.humdity
                        entry.value.alertTemperature = alert.alertTemperature
                        entry.value.isAlarmChecked = alert.isAlarmChecked

                    }
                }
                if(entry.value.isPlaced){ treeMap.set(entry.key,entry.value)}
            }
            return treeMap
        }
        return TreeMap()
    }

}