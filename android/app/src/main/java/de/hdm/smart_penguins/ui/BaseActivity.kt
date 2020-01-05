package de.hdm.smart_penguins.ui

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.component.BleNodesLiveData
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.data.manager.ConnectionManager
import de.hdm.smart_penguins.data.manager.DataManager
import de.hdm.smart_penguins.utils.PermissionDependentTask
import de.hdm.smart_penguins.utils.PermissionsHandler
import java.io.File
import javax.inject.Inject

open class BaseActivity : AppCompatActivity() {

    @Inject
    internal lateinit var connectionManager: ConnectionManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    internal lateinit var nodesLiveData: BleNodesLiveData


    private var isBluetoothDialogShown: Boolean = false
    private var mBroadCastReceiver: BroadcastReceiver? = null
    private var isShown: Boolean = false

    private val bleCallback: BroadcastReceiver
        get() {
            if (mBroadCastReceiver == null) {
                mBroadCastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val action = intent.action

                        if (action != null && action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                            val state = intent.getIntExtra(
                                BluetoothAdapter.EXTRA_STATE,
                                BluetoothAdapter.ERROR
                            )
                            when (state) {
                                BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> if (isShown) showBLuetoothActivationDialog()
                                BluetoothAdapter.STATE_ON -> {
                                }
                                BluetoothAdapter.STATE_TURNING_ON -> {
                                }
                            }
                        }
                    }
                }
            }
            return mBroadCastReceiver as BroadcastReceiver
        }


    val isBluetoothEnabled: Boolean
        get() =
            BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initInjection()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bleCallback, filter)
    }

    private fun initInjection() {
        if (getSmartApplication().activityComponent == null) {
            getSmartApplication().createActivityComponent()
        }
        getSmartApplication().activityComponent?.inject(this);
    }

    protected override fun onDestroy() {
        super.onDestroy()
        if (mBroadCastReceiver != null) unregisterReceiver(mBroadCastReceiver)
    }

    protected override fun onResume() {
        super.onResume()
        isShown = true
    }

    protected override fun onPause() {
        super.onPause()
        isShown = false
    }


    fun executeTaskOnPermissionGranted(task: PermissionDependentTask) {
        PermissionsHandler.executeTaskOnPermissionGranted(this, task)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        PermissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    protected fun importProfileWithCamera() {
        startActivity(
            Intent(this@BaseActivity, QrScannerActivity::class.java)
        )
    }


    protected override fun onActivityResult(
        requestCode: Int,
        resultCode: Int, @Nullable data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Constants.BARCODE_READER_RESULT_CODE && data != null
            && data.getStringExtra(Constants.BARCODE_READER_RESPONSE_CODE) != null
        ) {

            var profile: File? = null
            try {
                val decodedQRCode =
                    data.getStringExtra(Constants.BARCODE_READER_RESPONSE_CODE)

            } catch (e: Exception) {
                e.printStackTrace()
            }
            setIntent(null)
        }
    }

    fun showBLuetoothActivationDialog() {
        if (!isBluetoothDialogShown) {
            isBluetoothDialogShown = true
            val builder = AlertDialog.Builder(this, R.style.AppTheme)
            builder.setTitle("Bluetooth Aktivierung")
            builder.setMessage("Damit die App richtig funktioniert muss Bluetooth eingeschalten werden.")
            builder.setCancelable(false)
            builder.setPositiveButton("Ok") { dialog, id ->
                setBluetooth(true)
                isBluetoothDialogShown = false
            }
            builder.setNegativeButton("Beenden") { dialog, id -> finishAndRemoveTask() }
            builder.create().show()
        }
    }

    private fun setBluetooth(enabled: Boolean) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter
        bluetoothAdapter = bluetoothManager.adapter
        if (enabled) {
            bluetoothAdapter.enable()
        } else {
            bluetoothAdapter.disable()
        }
    }

    fun initBLEScanning() {
        if (isBluetoothEnabled) {
            connectionManager.initBLEScanner()
            connectionManager.initBleBroadcasting()
        } else {
            showBLuetoothActivationDialog()
        }
    }

    fun stopBLEScanning() {
        Log.e(TAG, "BLE Scanning stopped")
        if (isBluetoothEnabled) {
            connectionManager.stopBLEScanner()
        } else {
            showBLuetoothActivationDialog()
        }
    }

    companion object {
        private val TAG = "BASE_ACTIVITY"
    }

    fun getSmartApplication(): SmartApplication {
        return application as SmartApplication
    }
}
