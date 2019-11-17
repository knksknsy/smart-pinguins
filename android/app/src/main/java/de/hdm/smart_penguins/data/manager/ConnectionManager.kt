package de.hdm.smart_penguins.data.manager

import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.data.Constants.BLE_SCAN_INTERVAL
import de.hdm.smart_penguins.data.Constants.DELAY_BLE_SCANNER
import de.hdm.smart_penguins.data.Constants.DELAY_BLE_SCANNRESULT_TIMEOUT
import de.hdm.smart_penguins.data.Constants.MANUFACTURER_DATA
import de.hdm.smart_penguins.data.Constants.NETWORK_ID_NOT_SET
import de.hdm.smart_penguins.data.model.BleNode
import de.hdm.smart_penguins.data.model.NodeList
import no.nordicsemi.android.support.v18.scanner.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class  ConnectionManager @Inject constructor(context: Context) {
    private var listener: ((BleNode) -> Unit)? = null
    private var mScanner: BluetoothLeScannerCompat? = null
    val nodeList = NodeList()
    private var mNetworkFilter = NETWORK_ID_NOT_SET
    private var mScannerQueue: Boolean = false
    private var isScannerBlocked: Boolean = false
    private val mScanResultRunnable = Runnable { this.initBLEScanner() }
    private val mScanResultHandler = Handler()

    private val TAG = "CONNECTION_MANAGER"

    private val scanCallback = object : ScanCallback() {

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)

            if (results.size > 0) {
                mScanResultHandler.removeCallbacks(mScanResultRunnable)
                receiveMeshAccessBroadcastFromBatch(results)
            }
        }

    }

    private fun receiveMeshAccessBroadcastFromBatch(results: List<ScanResult>) {
        nodeList.clearNodes()

        for (scanResult in results) {
            if (scanResult.scanRecord != null &&
                scanResult.scanRecord!!.bytes != null &&
                scanResult.scanRecord!!.bytes!!.size >= Constants.MESSAGE_SIZE_JOINME
            ) {
                val isMeshBroadCastMessage = (scanResult.scanRecord!!.serviceUuids != null
                        && scanResult.scanRecord!!.serviceUuids!!.size > 0
                        && scanResult.scanRecord!!.serviceUuids!![0] == ParcelUuid.fromString(
                    Constants.SERVICE_UUID
                ))
                if (isMeshBroadCastMessage) {
                    val node = BleNode(scanResult)
                    if (node.messageMeshAccessBroadcast!!.messageType == Constants.MESSAGE_TYPE_BROADCAST && node.messageMeshAccessBroadcast!!.isConnectable) {
                        nodeList.addNode(node)
                    }
                }

                val isJoinMeMessage =
                    scanResult.scanRecord!!.manufacturerSpecificData != null && scanResult.scanRecord!!.manufacturerSpecificData!!.get(
                        MANUFACTURER_DATA
                    ) != null

                if (isJoinMeMessage) {
                    nodeList.updateNode(scanResult)
                }

            }
        }
        nodeList.sort()
    }


    fun initBLEScanner() {
        if (!isScannerBlocked) {
            doInitBLEScanner()
            setScannerInitiationHandler()
        } else {
            mScannerQueue = true
        }
    }

    private fun doInitBLEScanner() {
        try {
            stopBLEScanner()
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(BLE_SCAN_INTERVAL)
                .setUseHardwareBatchingIfSupported(false)
                .setUseHardwareFilteringIfSupported(false)
                .build()
            val filters = ArrayList<ScanFilter>()
            filters.add(
                ScanFilter.Builder()
                    .setManufacturerData(MANUFACTURER_DATA, null)
                    .build()
            )
            filters.add(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(Constants.SERVICE_UUID))
                    .build()
            )

            mScanner = BluetoothLeScannerCompat.getScanner()
            mScanner!!.startScan(filters, scanSettings, scanCallback)
            mScanResultHandler.postDelayed(mScanResultRunnable, DELAY_BLE_SCANNRESULT_TIMEOUT)
        } catch (e: IllegalStateException) {
            Log.e(TAG, e.message)
        }

    }

    private fun setScannerInitiationHandler() {
        val handler = Handler()
        this.isScannerBlocked = true
        val initiationRunnable = {
            isScannerBlocked = false
            if (mScannerQueue) {
                mScannerQueue = false
                initBLEScanner()
            }
        }
        handler.postDelayed(initiationRunnable, DELAY_BLE_SCANNER)
    }

    fun stopBLEScanner() {
        mScanResultHandler.removeCallbacks(mScanResultRunnable)
        try {
            mScanner = BluetoothLeScannerCompat.getScanner()
            mScanner!!.stopScan(scanCallback)
        } catch (e: IllegalStateException) {
            Log.e(TAG, e.message)
        }

        mScanner = null
    }


    fun registerNodeListener(networkId: Int, listener: (BleNode) -> Unit) {
        this.mNetworkFilter = networkId
        this.listener = listener
    }

    fun unregisterNodeListener() {
        this.mNetworkFilter = NETWORK_ID_NOT_SET
        this.listener = null
        if (mScanner != null) {
            try {
                mScanner!!.flushPendingScanResults(scanCallback)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }

        }
    }


}

