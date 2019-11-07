package de.hdm.closeme.util

import android.content.Context
import android.text.TextUtils
import de.hdm.closeme.R
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.model.AlarmBeacon
import de.hdm.closeme.model.AlarmBeaconMessage
import de.hdm.closeme.model.AlarmSpot
import de.hdm.closeme.model.BeaconMessage
import java.util.*
import kotlin.collections.ArrayList


class BeaconUtil() {
    companion object {
        fun getTypeForInt(typeNumber: Short): String {
            when (typeNumber) {
                Constants.RUUVI_TAG -> return Constants.RUUVI_TAG_STRING
                Constants.ARCONNA -> return Constants.ARCONNA_STRING
                Constants.DEV_BOARD -> return Constants.DEV_BOARD_STRING
                else -> return Constants.UNKOWN_BOARD
            }
        }





        fun getBeaconPlaceType(beacon: AlarmBeacon, context: Context): String {
            when (beacon.placedType) {
                Constants.PLACE_TYPE_FLOOR_BEACON -> return context.getString(R.string.place_type_floor_breacon)
                Constants.PLACE_TYPE_DOOR_BEACON -> return context.getString(R.string.place_type_door_breacon)
                Constants.PLACE_TYPE_WINDOW_BEACON -> return context.getString(R.string.place_type_window_breacon)
                else -> {
                    return context.getString(R.string.place_type_not_placed)
                }
            }

        }
    }
}