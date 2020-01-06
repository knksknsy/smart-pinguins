package de.hdm.smart_penguins.data.manager

import android.content.Context
import de.hdm.smart_penguins.SmartApplication
import javax.inject.Inject
import javax.inject.Singleton
import android.hardware.*
import android.hardware.SensorManager



@Singleton
class PhoneSensorManager @Inject constructor(
    var application: SmartApplication

): SensorEventListener {

    @Inject
    lateinit var connectionManager: ConnectionManager

    @Inject
    lateinit var dataManager: DataManager

    private lateinit var sensorManager: SensorManager
    private var mAccelerometer : Sensor ?= null
    private var mMagnetometer: Sensor? = null
    private var mLastAccelerometer = FloatArray(3)
    private var mLastMagnetometer = FloatArray(3)
    private var mLastAccelerometerSet = false
    private var mLastMagnetometerSet = false
    private var mR = FloatArray(9)
    private var mOrientation = FloatArray(3)
    private var mCurrentDegree = 0f


    fun initSensorManager() {
        application.activityComponent?.inject(this)
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            this.mAccelerometer = it
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            this.mMagnetometer = it
        }
        sensorManager!!.registerListener(this,mAccelerometer,
            SensorManager.SENSOR_DELAY_GAME)
        sensorManager!!.registerListener(this,mMagnetometer,
            SensorManager.SENSOR_DELAY_GAME)
    }



    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {
        return
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.size)
            mLastAccelerometerSet = true
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.size);
            mLastMagnetometerSet = true
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer)
            SensorManager.getOrientation(mR, mOrientation)
            val azimuthInRadians = mOrientation[0].toDouble()
            val azimuthInDegrees = (Math.toDegrees(azimuthInRadians) + 360) % 360

            // mCurrentDegree ranges from 0 (North) to 90 (East) to 180 (South) to 270 (West)
            mCurrentDegree = azimuthInDegrees.toFloat()
            // mCurrentDegree / 30 => maps the values to our direction values (0 - 360 => 0 - 12)
            var currentPhoneDirection = mCurrentDegree / 30
            // 12 => 0
            if (currentPhoneDirection > 11.5) {
                currentPhoneDirection = 0f
            }
            // set phone direction in dataManager if it changed
            if(dataManager.direction != Math.round(currentPhoneDirection)) {
                dataManager.direction = Math.round(currentPhoneDirection)
                connectionManager.updateBleBroadcasting()
            }

        }
    }

    /**
     * isMyDirection, evaluates whether the parameter direction is the same as the device orientation (mCurrentDegree)
     *
     * @param direction the direction to evaluate (0 = North, 1-2 = NorthEast, 3 = East, 4-5 = SouthEast, 6 = South, 7-8 = SouthWest, 9 = West, 10 - 11 = NorthWest)
     */
    fun isMyDirection(direction : Int) : Boolean{
        if(Math.abs(direction - dataManager.direction) <= 3 || Math.abs(direction - dataManager.direction) >= 9) {
            return true
        }

        return false
    }
}



