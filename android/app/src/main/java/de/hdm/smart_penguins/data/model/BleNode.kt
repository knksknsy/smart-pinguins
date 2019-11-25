package de.hdm.smart_penguins.data.model

import android.text.TextUtils
import de.hdm.smart_penguins.data.Constants.MESSAGE_SIZE_MESH_BROADCAST
import de.hdm.smart_penguins.data.Constants.NODE_RSSI_NOT_IN_RANGE
import de.hdm.smart_penguins.utils.DistanceEstimator
import no.nordicsemi.android.support.v18.scanner.ScanResult

class BleNode(scanResult: ScanResult) {
    var distance: Float = 0.toFloat()
    var txPower: Int = 0
    internal var distanceEstimator = DistanceEstimator()
    var scanResult: ScanResult? = scanResult
    var messageMeshAccessBroadcast: MessageMeshBroadcast? = null
    var name: String? = null
    var currentRssi: Int = 0
    internal var previousRssi = NODE_RSSI_NOT_IN_RANGE
    internal var offlineCounter = 0

    val highestRssi: Int
        get() = if (previousRssi > currentRssi) previousRssi else currentRssi


    init {
        this.messageMeshAccessBroadcast = receiveMeshAccessBroadcastFromScanResult(scanResult)
        this.currentRssi = scanResult.rssi
        this.name = scanResult.device.name
        this.txPower = scanResult.txPower
        this.distance = distanceEstimator.getDistanceInMetres(scanResult.rssi.toFloat(), -55f)
    }

    internal fun updateRssi() {
        previousRssi = currentRssi
        currentRssi = NODE_RSSI_NOT_IN_RANGE
    }

    internal fun matches(address: String): Boolean {
        return TextUtils.equals(this.scanResult!!.device.address, address)
    }

    private fun receiveMeshAccessBroadcastFromScanResult(scanResult: ScanResult): MessageMeshBroadcast? {
        if (scanResult.scanRecord != null) {
            val bytes = scanResult.scanRecord!!.bytes
            if (bytes != null && bytes.size > MESSAGE_SIZE_MESH_BROADCAST) {
                return MessageMeshBroadcast(bytes)
            }
        }
        return null
    }
}
