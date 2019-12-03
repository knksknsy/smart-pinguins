package de.hdm.smart_penguins.data.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.component.BleNodesLiveData
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.data.Constants.BLE_SCAN_INTERVAL
import de.hdm.smart_penguins.data.Constants.DELAY_BLE_SCANNER
import de.hdm.smart_penguins.data.Constants.DELAY_BLE_SCANNRESULT_TIMEOUT
import de.hdm.smart_penguins.data.Constants.MANUFACTURER_DATA
import de.hdm.smart_penguins.data.Constants.NETWORK_ID_NOT_SET
import de.hdm.smart_penguins.data.Constants.SERVICE_UUID
import de.hdm.smart_penguins.data.model.BleNode
import de.hdm.smart_penguins.data.model.NodeList
import no.nordicsemi.android.support.v18.scanner.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ConnectionManager @Inject constructor(
    var application: SmartApplication
) {
    private lateinit var currentAdvertisingSet: AdvertisingSet
    @Inject
    lateinit var nodesLiveData: BleNodesLiveData
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
                    if (node.messageMeshAccessBroadcast!!.messageType == Constants.MESSAGE_TYPE_BROADCAST) {
                        nodeList.addNode(node)
                    }
                }

            }
        }
        nodeList.sort()
        if (nodeList.size > 0) Log.e(
            TAG,
            nodeList.get(0).messageMeshAccessBroadcast?.deviceNumber.toString()
        )
        nodesLiveData.value = nodeList
    }


    fun initBLEScanner() {
        application.activityComponent?.inject(this)
        if (!isScannerBlocked) {
            doInitBLEScanner()
            setScannerInitiationHandler()
        } else {
            mScannerQueue = true
        }
    }

    private fun doInitBLEScanner() {
        try {
            example1()
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
            Log.e(TAG, e.message.toString())
        }

        mScanner = null
    }

    fun example1() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val advertiser =
                BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
            val parameters = AdvertisingSetParameters.Builder()
                .setLegacyMode(true) // True by default, but set here as a reminder.
                .setConnectable(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder().addServiceData(
                   ParcelUuid.fromString(SERVICE_UUID),
            "You shog".toByteArray()).build();

            val callback = object  :AdvertisingSetCallback() {
                override fun onAdvertisingSetStarted(
                    advertisingSet: AdvertisingSet,
                    txPower: Int,
                    status: Int
                ) {
                    Log.i(
                        TAG, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                                + status
                    )
                    currentAdvertisingSet = advertisingSet
                }

                override fun onAdvertisingDataSet(
                    advertisingSet: AdvertisingSet,
                    status: Int
                ) {
                    Log.i(TAG, "onAdvertisingDataSet() :status:$status")
                }

                override fun onScanResponseDataSet(
                    advertisingSet: AdvertisingSet,
                    status: Int
                ) {
                    Log.i(TAG, "onScanResponseDataSet(): status:$status")
                }

                override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
                    Log.i(TAG, "onAdvertisingSetStopped():")
                }
            }

            advertiser.startAdvertisingSet(parameters, data, null,null , null, callback)

           /* // After onAdvertisingSetStarted callback is called, you can modify the
// advertising data and scan response data:
            currentAdvertisingSet.setAdvertisingData(
                AdvertiseData.Builder().setIncludeDeviceName(true).setIncludeTxPowerLevel(
                    true
                ).build()
            )
            // Wait for onAdvertisingDataSet callback...
            currentAdvertisingSet.setScanResponseData(
                AdvertiseData.Builder().addServiceUuid(
                    ParcelUuid(
                        randomUUID()
                    )
                ).build()
            )
            // Wait for onScanResponseDataSet callback...
// When done with the advertising:
            advertiser.stopAdvertisingSet(callback)

             */
        }
    }
}

