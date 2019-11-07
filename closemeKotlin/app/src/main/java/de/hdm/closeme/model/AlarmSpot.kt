package de.hdm.closeme.model

import de.hdm.closeme.constant.Constants

data class AlarmSpot( var humdity: Short,
                      var temperature: Short,
                      var deviceNumber: Short,
                      var isAlarmChecked: Boolean,
                      var isPlaced: Boolean,
                      var alertTemperature: Short) {

    constructor () : this(Constants.NO_VALUE_HUMIDITY,Constants.NO_VALUE_TEMPERATURE,-1,false,false,Constants.ALERT_TEMPERATURE)
}