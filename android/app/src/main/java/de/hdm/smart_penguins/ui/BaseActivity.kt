package de.hdm.smart_penguins.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.data.callbacks.UpdateCallback
import de.hdm.smart_penguins.data.manager.ConnectionManager
import de.hdm.smart_penguins.di.component.ApplicationComponent
import de.hdm.smart_penguins.di.module.ConnectionModule
import java.io.File
import javax.inject.Inject


class BaseActivity : Activity() {

    @Inject
    internal var connectionManager: ConnectionManager? = null

    internal var applicationComponent: ApplicationComponent? = null
    private var isBluetoothDialogShown: Boolean = false
    private var mBroadCastReceiver: BroadcastReceiver? = null
    private var mUpdateCallback: UpdateCallback? = null
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
                                BluetoothAdapter.STATE_ON -> if (mUpdateCallback != null) mUpdateCallback!!.onUpdate()
                                BluetoothAdapter.STATE_TURNING_ON -> {
                                }
                            }
                        }
                    }
                }
            }
            return mBroadCastReceiver as BroadcastReceiver
        }

    protected val isPermissionGranted: Boolean
        get() {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            if (!checkPermissions(permissions)) {
                showPermissionDialog(permissions, Constants.PERMISSION_REQUEST_CODE)
                return false
            }
            return true
        }

    val isBluetoothEnabled: Boolean
        get() =
            BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getApplicationComponent()!!.inject(this)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bleCallback, filter)
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

    fun getApplicationComponent(): ApplicationComponent? {
        if (applicationComponent == null) {
            applicationComponent = DaggerApplicationComponent.builder()
                .connectionModule(ConnectionModule(this))
                .build()
        }
        return applicationComponent
    }


    protected fun checkPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (!checkPermission(permission)) {
                return false
            }
        }
        return true
    }

    private fun checkPermission(permission: String): Boolean {
        return this.checkSelfPermission(
            permission
        ) === PackageManager.PERMISSION_GRANTED
    }

    protected fun showPermissionDialog(permissions: Array<String>, requestCode: Int) {
        val builder = AlertDialog.Builder(this, R.style.AppTheme)
        val context = this
        builder.setTitle("Berechtigungen")
            .setMessage("Damit die App funktioniert, brauchen wir Rechte!")
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                ActivityCompat.requestPermissions(context, permissions, requestCode)
                dialog.cancel()
            }
            .show()
    }

    protected fun importProfileWithCamera() {
        startActivityForResult(
            Intent(this, CameraActivity::class.java),
            Constants.BARCODE_READER_RESULT_CODE
        )
    }


    protected override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
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
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.adapter
            if (enabled) {
                bluetoothAdapter.enable()
            } else {
                bluetoothAdapter.disable()
            }
        }
    }

    fun initBLEScanning() {
        if (isBluetoothEnabled) {
            connectionManager!!.initBLEScanner()
        } else {
            showBLuetoothActivationDialog()
        }
    }

    fun stopBLEScanning() {
        Log.e(TAG, "BLE Scanning stopped")
        if (isBluetoothEnabled) {
            connectionManager!!.stopBLEScanner()
        } else {
            showBLuetoothActivationDialog()
        }
    }

    companion object {
        private val TAG = "BASE_ACTIVITY"
    }

}
