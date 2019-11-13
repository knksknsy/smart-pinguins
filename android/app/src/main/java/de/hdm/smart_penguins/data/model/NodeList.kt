package de.hdm.smart_penguins.data.model

import android.bluetooth.BluetoothDevice
import android.text.TextUtils
import android.util.Log
import de.hdm.smart_penguins.data.Constants.NODE_RSSI_NOT_IN_RANGE
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*

class NodeList : ArrayList<BleNode>() {
    private val TAG = "NodeList"

    private val compareByRSSI =
        { o1, o2 -> Integer.compare(-o1.getCurrentRssi(), -o2.getCurrentRssi()) }

    fun clearNodes() {
        synchronized(this) {
            val iterator = this.iterator()
            while (iterator.hasNext()) {
                val node = iterator.next()
                node.updateRssi()
                if (node.currentRssi === NODE_RSSI_NOT_IN_RANGE && node.previousRssi === NODE_RSSI_NOT_IN_RANGE) {
                    node.offlineCounter += 1
                }
                if (node.offlineCounter > 2) iterator.remove()
            }
        }
    }

    fun addNode(node: BleNode) {
        synchronized(this) {
            for (bleNode in this) {
                if (bleNode.matches(node.scanResult!!.device.getAddress())) {
                    bleNode.scanResult = node.scanResult
                    bleNode.currentRssi = node.currentRssi
                    bleNode.messageMeshAccessBroadcast = node.messageMeshAccessBroadcast
                    bleNode.offlineCounter = 0
                    return
                }
            }
            this.add(node)
        }
    }

    fun updateNode(scanResult: ScanResult) {
        synchronized(this) {
            for (bleNode in this) {
                if (bleNode.matches(scanResult.device.address)) {
                    bleNode.currentRssi = scanResult.rssi
                    bleNode.offlineCounter = 0
                    Log.d(TAG, String.format("BleNode %s updated", scanResult.device.address))
                    return
                }
            }
        }
    }


    fun sort() {
        Collections.sort(this, compareByRSSI)
    }


    fun getNode(networkId: Int): BleNode? {
        synchronized(this) {
            for (bleNode in this) {
                if (bleNode.messageMeshAccessBroadcast!!.networkId === networkId && bleNode.messageMeshAccessBroadcast!!.isConnectable) {
                    return bleNode
                }
            }
            return null
        }
    }

    fun getNode(networkId: Int, bluetoothDevice: BluetoothDevice): BleNode? {
        synchronized(this) {
            for (bleNode in this) {
                if (bleNode.messageMeshAccessBroadcast!!.networkId === networkId && TextUtils.equals(
                        bleNode.scanResult!!.device.address,
                        bluetoothDevice.address
                    )
                ) {
                    return bleNode
                }
            }
            return null
        }
    }
}
