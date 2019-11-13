package de.hdm.smart_penguins.data.model

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import de.hdm.smart_penguins.data.Constants.MESSAGE_SIZE_MESH_BROADCAST
import de.hdm.smart_penguins.data.Constants.NODE_RSSI_NOT_IN_RANGE
import no.nordicsemi.android.support.v18.scanner.ScanResult

class BleNode : Parcelable {

    var scanResult: ScanResult? = null
        internal set
    var messageMeshAccessBroadcast: MessageMeshAccessBroadcast? = null
        internal set
    var name: String? = null
    var currentRssi: Int = 0
        internal set
    internal var previousRssi = NODE_RSSI_NOT_IN_RANGE
        private set
    internal var offlineCounter = 0

    val highestRssi: Int
        get() = if (previousRssi > currentRssi) previousRssi else currentRssi


    constructor(scanResult: ScanResult) {
        this.scanResult = scanResult
        this.messageMeshAccessBroadcast = receiveMeshAccessBroadcastFromScanResult(scanResult)
        this.currentRssi = scanResult.rssi
        this.name = scanResult.device.name
    }

    private constructor(`in`: Parcel) {
        scanResult = `in`.readParcelable(ScanResult::class.java.classLoader)
        messageMeshAccessBroadcast =
            `in`.readParcelable<MessageMeshAccessBroadcast>(MessageMeshAccessBroadcast::class.java!!.getClassLoader())
        name = `in`.readString()
        currentRssi = `in`.readInt()
        previousRssi = `in`.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(scanResult, flags)
        parcel.writeParcelable(messageMeshAccessBroadcast, flags)
        parcel.writeString(name)
        parcel.writeInt(currentRssi)
        parcel.writeInt(previousRssi)
    }

    override fun describeContents(): Int {
        return 0
    }

    internal fun updateRssi() {
        previousRssi = currentRssi
        currentRssi = NODE_RSSI_NOT_IN_RANGE
    }

    internal fun matches(address: String): Boolean {
        return TextUtils.equals(this.scanResult!!.device.address, address)
    }

    private fun receiveMeshAccessBroadcastFromScanResult(scanResult: ScanResult): MessageMeshAccessBroadcast? {
        if (scanResult.scanRecord != null) {
            val bytes = scanResult.scanRecord!!.bytes
            if (bytes != null && bytes.size > MESSAGE_SIZE_MESH_BROADCAST) {
                return MessageMeshAccessBroadcast(bytes)
            }
        }
        return null
    }


    companion object CREATOR : Parcelable.Creator<BleNode> {
        override fun createFromParcel(parcel: Parcel): BleNode {
            return BleNode(parcel)
        }

        override fun newArray(size: Int): Array<BleNode?> {
            return arrayOfNulls(size)
        }
    }
}
