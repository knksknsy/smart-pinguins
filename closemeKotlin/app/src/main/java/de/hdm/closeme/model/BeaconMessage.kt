package de.hdm.closeme.model

import android.bluetooth.BluetoothDevice
import de.hdm.closeme.util.DistanceEstimator
import no.nordicsemi.android.support.v18.scanner.ScanRecord
import no.nordicsemi.android.support.v18.scanner.ScanResult

open class BeaconMessage(result: ScanResult) {

    var txPower: Int = 0
    var device: BluetoothDevice? = null
    var rssi: Int = 0
    var scanRecord: ScanRecord? = null
    var distance: Float = 0.toFloat()
        private set
    internal var distanceEstimator = DistanceEstimator()


    init {
        this.device = result.device
        this.distance = distanceEstimator.getDistanceInMetres(result.rssi.toFloat(), -55f)
        this.rssi = result.rssi
        this.txPower = result.txPower
        this.scanRecord = result.scanRecord
    }

    fun setDistance(rssi: Float, txPower: Float) {
        this.distance = distanceEstimator.getDistanceInMetres(rssi, -55f)
    }

}
