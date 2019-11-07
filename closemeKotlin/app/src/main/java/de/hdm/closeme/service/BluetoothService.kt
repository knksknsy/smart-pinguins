package de.hdm.closeme.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.*
import android.support.v4.app.NotificationCompat
import android.text.TextUtils
import android.util.Log
import de.hdm.closeme.MainActivity
import de.hdm.closeme.R
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.listener.BluetoothServiceListener
import de.hdm.closeme.model.*
import de.hdm.closeme.util.ByteArrayParser
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import java.util.*
import kotlin.collections.ArrayList


class BluetoothService : Service() {
    private val LOG_TAG = "BluetoothService"
    private var scanner: BluetoothLeScannerCompat? = null
    private var listener: BluetoothServiceListener? = null

    private val binder = LocalBinder()
    private var lastIntent = Calendar.getInstance().timeInMillis - Constants.PARAM_INTENT_TIME
    var clusterSize = 0

    var beaconList = AlarmBeaconList()
    var unsecuredSpotMap = TreeMap<Short, AlarmSpot>()
    var notification: Notification? = null
    var context: Context? = null
    var timer: Runnable? = null
    var meshDisconnectionDate :Date? = null
    val handler = Handler()
    private var alertSettings = AlertSettings()


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == Constants.ACTION_STARTFOREGROUND) {
            //setTime()
            createForegroundService()
            context = this

        } else if (intent.action == Constants.ACTION_STOPFOREGROUND) {
            Log.i(LOG_TAG, "Received Stop Foreground Intent")
            stopForeground(true)
            if (timer != null) handler.removeCallbacks(timer)
            stopSelf()
        }
        return Service.START_STICKY
    }


    private fun createNotificationChannel(): String {
        val channelId = "bluetooth_service"
        val channelName = "Bluetooth Service"
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.CYAN
        chan.importance = NotificationManager.IMPORTANCE_LOW
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        service!!.createNotificationChannel(chan)
        return channelId
    }

    fun buildNotification(unsecuredSpotSize: Int): Notification {

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        // Create notification builder.
        val builder = NotificationCompat.Builder(this, createNotificationChannel())
        // Make notification show big text.
        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.setBigContentTitle(
                if (unsecuredSpotSize > 1) {
                    "$unsecuredSpotSize windows are open."
                } else if (unsecuredSpotSize == 1) {
                    "$unsecuredSpotSize window is open."
                } else {
                    "All windows are closed."
                })

        bigTextStyle.bigText(
                if (clusterSize == 0) {
                    "There are no beacons in range"
                } else {
                    // val diff = persistentBeaconSize - clusterSize
                    "The mesh network contains $clusterSize beacons."
                })
        // Set big text style.
        builder.setStyle(bigTextStyle)
        builder.setSmallIcon(R.drawable.ic_stat_name)
        builder.setWhen(System.currentTimeMillis())
        val largeIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_stat_name)
        builder.setLargeIcon(largeIconBitmap)
        // Make head-up notification.
        builder.setFullScreenIntent(pendingIntent, true)


        // Build the notification.
        return builder.build()
    }

    fun createForegroundService() {
        Log.i(LOG_TAG, "Received Start Foreground Intent ")

        // Start foreground service.

        startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, buildNotification(unsecuredSpotMap.size))
        startBluetoothScanning()
        checkExpiredBeacons()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanner = BluetoothLeScannerCompat.getScanner()
        scanner?.stopScan(nsCallback)
        Log.i(LOG_TAG, "In onDestroy")
    }

    override fun onBind(intent: Intent): IBinder? {

        return binder
    }

    fun updateNotificaton(clusterSize: Int, unsecuredSpotCount: Int) {
        this.clusterSize = clusterSize
        startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, buildNotification(unsecuredSpotCount))
    }

    private fun startBluetoothScanning() {
        scanner = BluetoothLeScannerCompat.getScanner()
        val settings = ScanSettings.Builder()
                .build()
        val filters = ArrayList<ScanFilter>()

        filters.add(ScanFilter.Builder().setDeviceAddress("F0:72:68:58:2E:40").build())
        // filters.add(ScanFilter.Builder().setDeviceAddress("E6:8F:5F:0C:ED:6B").build());
        // filters.add(ScanFilter.Builder().setDeviceAddress("EF:0F:7F:FE:50:E9").build());

        scanner?.startScan(null, settings, nsCallback)
    }

    private val nsCallback = object : no.nordicsemi.android.support.v18.scanner.ScanCallback() {

        override fun onScanResult(callbackType: Int, result: no.nordicsemi.android.support.v18.scanner.ScanResult?) {
            super.onScanResult(callbackType, result)
            // ByteArrayParser.parseScanRecord(result!!.scanRecord!!.getBytes())
            //ByteArrayParser.parseScanRecord(result!!.scanRecord!!.getBytes())
            if (result!!.scanRecord != null) {
                val scanRecord: ScanRecord = ByteArrayParser.parseScanRecord(result.scanRecord!!.getBytes())
                var beaconMessage: BeaconMessage? = null
                when (scanRecord.type) {
                    //Check if the Message-Type is from an alarmBeacon device

                    Constants.BLE_DATA_TYPE_ALARM_MESSAGE -> {
                        beaconMessage = AlarmBeaconMessage(result, scanRecord.data)

                        //Check the purpose of the message
                        if (beaconMessage.messageType == Constants.MESSAGE_TYPE_ALARM_UPDATE) {

                            val beacon = beaconList.addOrUpdate(beaconMessage)

                            beaconList.removeExpiredBeacon(Constants.EXPIRY_TIME)

                            listener?.onAlarmBeaconFound(beaconList)
                            val unsecuredSpotList = beaconList.getUnsecuredSpotMap()

                            if (clusterSize != beaconMessage.clusterSize.toInt() ||
                                    unsecuredSpotList.size != unsecuredSpotMap.size) {
                                Log.d(Constants.TAG_BLUETOOTH_SERVICE, "Notification rebuild")
                                updateNotificaton(beaconMessage.clusterSize.toInt(), unsecuredSpotList.size)
                                beaconList.setUnsecuredBeacons(unsecuredSpotList)
                                listener?.onNotificationStatusChanged()
                            }


                            unsecuredSpotMap.clear()
                            unsecuredSpotMap.putAll(unsecuredSpotList)

                            if (unsecuredSpotMap.size > 0) {
                                for (alert in alertSettings.alertList) {
                                    if (alert.isAlarmChecked && beaconMessage.meshDataTemperature < alert.alertTemperature && isAlarmTime()
                                            && TextUtils.equals(beaconMessage.boardType ,Constants.RUUVI_TAG_STRING)) {
                                        startMapFragment(alert.deviceNumber)
                                        break
                                    }
                                }
                                when (beacon.placedType) {
                                    Constants.PLACE_TYPE_DOOR_BEACON -> {
                                        for((_,spot) in unsecuredSpotMap) {
                                            if (beacon.distance < Constants.PARAM_IN_RANGE_DOOR
                                                    && isAlarmTime() && spot.isPlaced) {
                                                startMapFragment(beacon.deviceNumber)
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Constants.BLE_DATA_TYPE_MESH_MESSAGE -> beaconMessage = AlarmBeaconMessage(result, scanRecord.data)
                }
                //  foundBeacon(beaconMessage);
            }
        }
    }

    private fun startMapFragment(deviceNumber: Short) {
        if(alertSettings.soundActivated){
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        }else {
            val vibrator = getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(Constants.PARAM_VIBRATION_TIME)
        }

        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, Constants.TAG_WAKE_UP)
        wakeLock.acquire(10*60*1000L /*10 minutes*/)
        lastIntent = Calendar.getInstance().timeInMillis
        if (listener != null) {
            listener!!.onDoorBeaconAlert(deviceNumber.toString())
        } else {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(Constants.TAG_MAP_FRAGMENT, deviceNumber.toString())
            startActivity(intent);
        }
    }


    private fun isAlarmTime(): Boolean {
        return (Calendar.getInstance().timeInMillis - lastIntent > Constants.PARAM_INTENT_TIME)
    }

    fun registerBeaconListener(listener: BluetoothServiceListener, persistentBeaconList: AlarmBeaconList) {
        this.listener = listener
        this.beaconList.addPersistentBeacons(persistentBeaconList)
        listener.onAlarmBeaconFound(this.beaconList)
    }


    fun unregisterBeaconListener() {
        listener = null
    }

    inner class LocalBinder : Binder() {
        // Return this instance of MyService so clients can call public methods
        val service: BluetoothService
            get() = this@BluetoothService
    }


    fun checkExpiredBeacons() {
        timer = object : Runnable {
            override fun run() {
                beaconList.removeExpiredBeacon(Constants.EXPIRY_TIME)
                listener?.onAlarmBeaconFound(beaconList)
                if (beaconList.getInRangedBeacons().size == 0) {
                    updateNotificaton(0, unsecuredSpotMap.size)
                    listener?.onNotificationStatusChanged()

                    //Restart Bluetooth scanner after some time
                    if(meshDisconnectionDate == null){
                        meshDisconnectionDate = Calendar.getInstance().time
                    }else{
                        if(Constants.MESH_DISCONNECTION_TIME < Calendar.getInstance().timeInMillis - meshDisconnectionDate!!.time){
                            meshDisconnectionDate = null
                            //Crashed
                      //      startBluetoothScanning()
                        }
                    }
                }

                // Log.e(Constants.TAG_BLUETOOTH_SERVICE, "Timer fired")

                handler.postDelayed(this, Constants.EXPIRY_HANDLER_TIME)
            }
        }
        handler.post(timer)
    }

    fun setPersistenBeacon(persistentBeaconList: AlarmBeaconList) {
        beaconList.addPersistentBeacons(persistentBeaconList)
    }

    fun updateBeaconList(beaconList: AlarmBeaconList) {
        this.beaconList.addPersistentBeacons(beaconList)
    }

    fun setSettings(alertSettings: AlertSettings) {
        this.alertSettings = alertSettings
    }


    fun getSettings(): AlertSettings {
        return alertSettings
    }


}




