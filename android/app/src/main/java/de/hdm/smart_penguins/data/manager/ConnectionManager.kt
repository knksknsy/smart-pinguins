package de.hdm.smart_penguins.data.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.component.AlarmLiveData
import de.hdm.smart_penguins.component.BleNodesLiveData
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.data.Constants.BLE_SCAN_INTERVAL
import de.hdm.smart_penguins.data.Constants.DELAY_BLE_SCANNER
import de.hdm.smart_penguins.data.Constants.DELAY_BLE_SCANNRESULT_TIMEOUT
import de.hdm.smart_penguins.data.Constants.MANUFACTURER_DATA
import de.hdm.smart_penguins.data.Constants.MESSAGE_TYPE_DEVICE_BROADCAST
import de.hdm.smart_penguins.data.Constants.NETWORK_ID_NOT_SET
import de.hdm.smart_penguins.data.Constants.SERVICE_UUID
import de.hdm.smart_penguins.data.Constants.VAR_NOT_SET
import de.hdm.smart_penguins.data.model.*
import de.hdm.smart_penguins.utils.Util.ternary
import no.nordicsemi.android.support.v18.scanner.*
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ConnectionManager @Inject constructor(
    var application: SmartApplication
) {
    private var currentAdvertisingSet: AdvertisingSet? = null
    private var callback: Any? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var isBroadcasting: Boolean = false
    @Inject
    lateinit var nodesLiveData: BleNodesLiveData
    @Inject
    lateinit var alarm: AlarmLiveData
    @Inject
    lateinit var sensorManager: PhoneSensorManager
    @Inject
    lateinit var dataManager: DataManager

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
            } else {
                alarm.value = null
            }
        }

    }

    private fun receiveMeshAccessBroadcastFromBatch(results: List<ScanResult>) {
        nodeList.clearNodes()
        var  bikeAlarmId = VAR_NOT_SET
        for (scanResult in results) {
            if (scanResult.scanRecord != null &&
                scanResult.scanRecord!!.bytes != null &&
                scanResult.scanRecord!!.bytes!!.size >= Constants.MESSAGE_SIZE_MESH_BROADCAST
            ) {
                val isMwayMessage = (scanResult.scanRecord!!.serviceUuids != null
                        && scanResult.scanRecord!!.serviceUuids!!.size > 0
                        && scanResult.scanRecord!!.serviceUuids!![0] == ParcelUuid.fromString(
                    SERVICE_UUID
                ))
                if (isMwayMessage) {
                    try {
                        val deviceBroadcast: DeviceBroadcast =
                            DeviceBroadcast().initWithBytes(scanResult.scanRecord!!.bytes!!)
                        val node = BleNode(scanResult)
                        if (deviceBroadcast.messageType == MESSAGE_TYPE_DEVICE_BROADCAST) {
                            Log.e(TAG, "Received device broadcast")
                            if (deviceBroadcast.deviceType == Constants.DEVICE_TYPE_BIKE && dataManager.isRightTurn) {
                                bikeAlarmId = deviceBroadcast.deviceId
                            }
                        } else if (node.messageMeshAccessBroadcast!!.messageType == Constants.MESSAGE_TYPE_BROADCAST) {
                            Log.e(TAG, "Received node message")
                            nodeList.addNode(node)
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, ex.toString())
                    }

                }

            }
        }
        nodeList.sort()
        nodesLiveData.value = nodeList
        if(bikeAlarmId != VAR_NOT_SET){
            alarm.value = Alarm(
                0,
                0,
                0,
                bikeAlarmId,
                true
            )
        }
        else if (nodeList.size > 0) {
            val broadcast = nodeList.get(0).messageMeshAccessBroadcast
            Log.e(TAG, broadcast?.deviceNumber.toString())
            checkNodeForAlarm(broadcast)
        }else {
            alarm.value = null
        }
    }

    private fun checkNodeForAlarm(broadcast: MessageMeshBroadcast?) {
        if (broadcast != null) {
            val direction = dataManager.getDirectionForNode(broadcast.deviceNumber)
            val isMyDirection = sensorManager.isMyDirection(
                ternary(direction != VAR_NOT_SET, direction, broadcast.direction.toInt())
            )
            if ((directionAndNodeCheck(broadcast, isMyDirection))) {
                alarm.value = Alarm(
                    (ternary(
                        isMyDirection,
                        broadcast.nearestBlackIceNodeId,
                        broadcast.nearestBlackIceOppositeLaneNodeId
                    ).toInt()),
                    (ternary(
                        isMyDirection,
                        broadcast.nearestRescueLaneNodeId,
                        broadcast.nearestRescueLaneOppositeLaneNodeId
                    ).toInt()),
                    (ternary(
                        isMyDirection,
                        broadcast.nearestTrafficJamNodeId,
                        broadcast.nearestTrafficJamOppositeLaneNodeId
                    ).toInt()),
                    broadcast.deviceNumber.toInt(),
                    false
                )
            } else {
                alarm.value = null
            }
        }
    }

    private fun directionAndNodeCheck(
        broadcast: MessageMeshBroadcast,
        isMyDirection: Boolean
    ): Boolean {
        return ((broadcast.nearestRescueLaneNodeId.toInt() != 0
                || broadcast.nearestTrafficJamNodeId.toInt() != 0
                || broadcast.nearestBlackIceNodeId.toInt() != 0)
                && isMyDirection)
                || ((broadcast.nearestBlackIceOppositeLaneNodeId.toInt() != 0
                || broadcast.nearestRescueLaneOppositeLaneNodeId.toInt() != 0
                || broadcast.nearestTrafficJamOppositeLaneNodeId.toInt() != 0)
                && !isMyDirection)
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
                    .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
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

    fun broadcastOnOldDevices(data: AdvertiseData) {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        callback = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.e(TAG, "Advertising failed");
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
            }
        }
        advertiser?.startAdvertising(
            settings,
            data,
            callback as AdvertiseCallback
        )

    }

    @SuppressLint("NewApi")
    fun broadcastDeviceBroadcast(data: AdvertiseData) {

        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(true) // True by default, but set here as a reminder.
            .setConnectable(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .build()

        val callback = object : AdvertisingSetCallback() {
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

        advertiser?.startAdvertisingSet(
            parameters,
            data,
            null,
            null,
            null,
            callback
        )
        /* // After onAdvertisingSetStarted callback is called, you can modify the
    // advertising data and scan response data:

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

    fun getBroadCastData(): AdvertiseData {
        return AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
            .addServiceData(ParcelUuid.fromString(SERVICE_UUID), dataManager.getDeviceBroadcast())
            .build()
    }

    fun initBleBroadcasting() {

        if (!isBroadcasting) {
            advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                broadcastDeviceBroadcast(getBroadCastData())
            } else {
                broadcastOnOldDevices(getBroadCastData())
            }
            isBroadcasting = true;
        }
    }

    fun updateBleBroadcasting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentAdvertisingSet?.setAdvertisingData(
                getBroadCastData()
            )
        } else {
            stopBLEScanner()
            initBleBroadcasting()
        }
    }

    fun stopBleBroadcasting() {
        if (isBroadcasting) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                advertiser?.stopAdvertisingSet(callback as AdvertisingSetCallback)
            } else {
                advertiser?.stopAdvertising(
                    callback as AdvertiseCallback?
                )
            }
            isBroadcasting = false
        }
    }

}

