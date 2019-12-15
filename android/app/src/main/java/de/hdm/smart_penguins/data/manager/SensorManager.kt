package de.hdm.smart_penguins.data.manager

import de.hdm.smart_penguins.SmartApplication
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SensorManager @Inject constructor(
    var application: SmartApplication

) {
    private val TAG = "SENSOR_MANAGER"

    //TODO Add Direction Managemnt and update @ConnenctionManagers Advertisment

    fun isMyDirection(direction : Int) : Boolean{
        return true
    }
}



