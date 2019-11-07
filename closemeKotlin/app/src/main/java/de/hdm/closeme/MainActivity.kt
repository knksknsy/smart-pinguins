package de.hdm.closeme

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.db.AppDatabase
import de.hdm.closeme.fragment.HomeFragment
import de.hdm.closeme.fragment.MapsFragment
import de.hdm.closeme.fragment.ScannerFragment
import de.hdm.closeme.listener.BeaconListListener
import de.hdm.closeme.listener.BluetoothServiceListener
import de.hdm.closeme.model.AlarmBeacon
import de.hdm.closeme.model.AlarmBeaconList
import de.hdm.closeme.model.AlarmSpot
import de.hdm.closeme.service.BluetoothService
import kotlinx.android.synthetic.main.activity_main.*
import android.bluetooth.BluetoothAdapter
import de.hdm.closeme.fragment.SetupFragment
import de.hdm.closeme.model.AlertSettings


class MainActivity : AppCompatActivity(), BluetoothServiceListener {

    private var bluetoothService: BluetoothService? = null

    private var alarmSpotList: ArrayList<AlarmSpot> = ArrayList()

    private var beaconListListener: BeaconListListener? = null
    private var isBound = false
    private var beaconList = AlarmBeaconList()
    private var setupViewPagerForwardAction: ((Int) -> Unit)? = null
    private var bottomNavigationView: BottomNavigationView? = null
    private var alertSettings = AlertSettings()


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        var fragment: Fragment = ScannerFragment()
        var tag = Constants.TAG_HOME_FRAGMENT
        when (item.itemId) {
            R.id.navigation_scanner -> {
                tag = Constants.TAG_SCANNER_FRAGMENT
                fragment = supportFragmentManager.findFragmentByTag(tag) as ScannerFragment?
                        ?: ScannerFragment.newInstance(false)
            }
            R.id.navigation_map -> {
                tag = Constants.TAG_MAP_FRAGMENT
                fragment = supportFragmentManager.findFragmentByTag(tag) as MapsFragment?
                        ?: MapsFragment.newInstance(false, null)
            }
            R.id.navigation_home -> {
                tag = Constants.TAG_HOME_FRAGMENT
                fragment = supportFragmentManager.findFragmentByTag(tag) as HomeFragment?
                        ?: HomeFragment.newInstance()

                setSetupModeEnabled(false)
            }
            R.id.navigation_back -> {
                setupViewPagerForwardAction?.invoke(-1)
                bottomNavigationView?.selectedItemId = -1
                return@OnNavigationItemSelectedListener false
            }
            R.id.navigation_forward -> {
                setupViewPagerForwardAction?.invoke(1)
                bottomNavigationView?.selectedItemId = -1
                return@OnNavigationItemSelectedListener false
            }
        }
        navigateTo(fragment, true, tag, false)
        return@OnNavigationItemSelectedListener true
    }

    fun navigateTo(fragment: Fragment, addToBackStack: Boolean, tag: String, setBottomBar: Boolean) {
        if (bottomNavigationView != null && setBottomBar) {
            when (tag) {
                Constants.TAG_SCANNER_FRAGMENT -> bottomNavigationView!!.selectedItemId = R.id.navigation_scanner
                Constants.TAG_MAP_FRAGMENT -> bottomNavigationView!!.selectedItemId = R.id.navigation_map
                Constants.TAG_HOME_FRAGMENT -> bottomNavigationView!!.selectedItemId = R.id.navigation_home
            }
        }

        val manager = supportFragmentManager
        val ft = manager.beginTransaction()
        if (addToBackStack) {
            ft.addToBackStack(tag)
        }
        ft.replace(R.id.frame, fragment, tag)
        ft.commitAllowingStateLoss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (isMyServiceRunning(BluetoothService::class.java)) {
            bindService(createServiceIntent(), serviceConnection, Context.BIND_AUTO_CREATE)
            isBound = true
        }
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        this.bottomNavigationView = navigation
        loadPersistentBeacons()
        Log.e("MAIN", (intent.getStringExtra(Constants.TAG_MAP_FRAGMENT) != null).toString())
        if (intent.getStringExtra(Constants.TAG_MAP_FRAGMENT) != null) {
            showAlertOnMap(intent.getStringExtra(Constants.TAG_MAP_FRAGMENT))
        } else {
            discardSession()
        }
    }

    override fun onDoorBeaconAlert(deviceNumber: String) {
        showAlertOnMap(deviceNumber)
    }

    fun showAlertOnMap(deviceNumber: String) {
        if (!isSetupMode()) {
            val tag = Constants.TAG_MAP_FRAGMENT
            val fragment = MapsFragment.newInstance(false, deviceNumber)
            navigateTo(fragment, true, tag, true)
        }
    }

    private fun isSetupMode(): Boolean {
        val fragment = supportFragmentManager.findFragmentByTag(Constants.TAG_SETUP_FRAGMENT)
        return fragment != null && fragment.isVisible
    }


    fun discardSession() {
        unRegisterSetupViewPagerNavigationListener()
        supportFragmentManager?.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        navigateTo(HomeFragment.newInstance(), true, Constants.TAG_HOME_FRAGMENT, true)
        //    bluetoothService?.updateAlarmList(beaconList)
    }

    private fun createServiceIntent(): Intent {
        val startIntent = Intent(this@MainActivity, BluetoothService::class.java)
        startIntent.action = Constants.ACTION_STARTFOREGROUND
        return startIntent
    }

    private fun resetService() {
        stopBluetoothService()
        startBluetoothService()
    }

    fun loadPersistentBeacons() {
        AsyncTask.execute() {
            val database = AppDatabase.getInstance(this@MainActivity)
            val persistentBeacon = database.beaconDao().getAll()
            if (persistentBeacon.size == 0) {
                runOnUiThread{
                val builder: AlertDialog.Builder
                builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.setup))
                        .setMessage(getString(R.string.setup_message))
                        .setNegativeButton(android.R.string.no) { dialog, _ ->
                            dialog.cancel()
                        }
                        .setPositiveButton(android.R.string.yes) { dialog, _ ->
                            navigateTo(SetupFragment.newInstance(), true, Constants.TAG_SETUP_FRAGMENT, true)
                        }
                        .show()
                }

            }
            this.beaconList.addPersistentBeacons(persistentBeacon)

            Log.e(Constants.TAG_MAIN_ACTIVITY, "Beacon Loaded: " + this.beaconList.size)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        bluetoothService?.unregisterBeaconListener() // unregister
        bluetoothService = null
        if (isBound) unbindService(serviceConnection)
    }


    protected fun showPermissionDialog(permissions: Array<String>, requestCode: Int) {
        val builder: AlertDialog.Builder
        builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.permission))
                .setMessage(getString(R.string.permission_message))
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    ActivityCompat.requestPermissions(this, permissions, requestCode)
                    dialog.cancel()
                }
                .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    checkPermissions(Constants.PERMISSION_REQUEST_CODE)
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (bluetoothService != null) {
            alertSettings = bluetoothService!!.getSettings()
        }
        checkPermissions(Constants.PERMISSION_REQUEST_CODE)
        checkBottomBarIcons()
    }

    private fun checkBottomBarIcons() {
        val fragment = supportFragmentManager.findFragmentByTag(Constants.TAG_SETUP_FRAGMENT)
        setSetupModeEnabled(fragment != null && fragment.isVisible)
    }

    protected fun checkPermissions(requestCode: Int) {
        val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.INTERNET,
                Manifest.permission.BLUETOOTH_ADMIN)
        if (!checkPermissions(permissions)) {
            showPermissionDialog(permissions, requestCode)
        }else{

        }
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
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    /** Callbacks for service binding, passed to bindService()  */
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // cast the IBinder and get MyService instance
            val binder = service as BluetoothService.LocalBinder
            bluetoothService = binder.service
            bluetoothService?.registerBeaconListener(this@MainActivity, beaconList)
            bluetoothService?.setSettings(alertSettings)

            // register
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bluetoothService?.unregisterBeaconListener() // unregister
            bluetoothService = null
        }
    }

    internal fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onAlarmBeaconFound(beaconList: AlarmBeaconList) {
        this.beaconList = beaconList
        //this.beaconList.addOrUpdate(beaconList)
        runOnUiThread {
            beaconListListener?.onBeaconListChanged(this.beaconList)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        checkBottomBarIcons()
    }

    fun startBluetoothService() {
        if (isBluetoothActive()) {
            startForegroundService(createServiceIntent())
            if (!isBound) bindService(createServiceIntent(), serviceConnection, Context.BIND_AUTO_CREATE)
            isBound = true
        }
    }

    private fun isBluetoothActive(): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            return false
        } else {
            if (!mBluetoothAdapter.isEnabled) {
                val builder = android.app.AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.bluetooth_deactivated))
                builder.setMessage(getString(R.string.please_turn_bluetooth_on))
                builder.setPositiveButton(getString(R.string.settings)) { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent(Intent.ACTION_MAIN, null)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    val cn = ComponentName(Constants.SETTINGS_PATH,
                            Constants.SETTINGS_PATH_BLUETOOTH)
                    intent.component = cn
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog: android.app.AlertDialog = builder.create()
                dialog.show()
                return false
            }
        }
        return true
    }

    fun stopBluetoothService() {
        val stopIntent = Intent(this@MainActivity, BluetoothService::class.java)
        stopIntent.action = Constants.ACTION_STOPFOREGROUND
        if (isBound) unbindService(serviceConnection)
        stopService(stopIntent)
        isBound = false
    }


    fun registerBeaconListListener(listener: BeaconListListener) {
        this.beaconListListener = listener
    }

    fun unregisterBeaconListListener() {
        beaconListListener = null
    }


    fun getBeaconList(): AlarmBeaconList {
        return beaconList
    }

    fun registerSetupViewPagerNavigationListener(action: (Int) -> Unit, registration: (BottomNavigationView) -> Unit) {
        this.setupViewPagerForwardAction = action
        bottomNavigationView?.let { registration(it) }
        setSetupModeEnabled(true)

    }

    fun unRegisterSetupViewPagerNavigationListener() {
        this.setupViewPagerForwardAction = null
        bottomNavigationView?.menu?.getItem(Constants.NAVIGATION_ITEM_BACK)?.title = getString(R.string.dots)
        bottomNavigationView?.menu?.getItem(Constants.NAVIGATION_ITEM_BACK)?.icon = null
        bottomNavigationView?.menu?.getItem(Constants.NAVIGATION_ITEM_FORWARD)?.title = getString(R.string.forward)
        setSetupModeEnabled(false)
    }


    private fun setSetupModeEnabled(isEnabled: Boolean) {
        bottomNavigationView?.menu?.getItem(Constants.NAVIGATION_ITEM_SCANNER)?.setVisible(!isEnabled)
        bottomNavigationView?.menu?.getItem(Constants.NAVIGATION_ITEM_HOME)?.setVisible(!isEnabled)
        bottomNavigationView?.menu?.getItem(Constants.NAVIGATION_ITEM_MAP)?.setVisible(!isEnabled)
        bottomNavigationView?.menu?.getItem(Constants.NAVIGATION_ITEM_BACK)?.setVisible(isEnabled)
        bottomNavigationView?.menu?.getItem(Constants.NAVIGATION_ITEM_FORWARD)?.setVisible(isEnabled)
    }


    fun saveBeaconPersistent(beacon: AlarmBeacon, isSaved: Boolean) {
        if (isSaved) {
            beacon.distance = -1f
            beacon.rssi = 0
            AsyncTask.execute() {
                AppDatabase.getInstance(this@MainActivity).beaconDao().insertOne(beacon)
                runOnUiThread() { beaconListListener?.onBeaconListChanged(this.beaconList) }
            }

        } else {
            beacon.resetPersitentData()
            AsyncTask.execute() {
                AppDatabase.getInstance(this@MainActivity).beaconDao().deleteOne(beacon)
                runOnUiThread() { beaconListListener?.onBeaconListChanged(this.beaconList) }
            }

        }
    }

    override fun onNotificationStatusChanged() {
        if (beaconListListener != null) {
            beaconListListener!!.onNotificationStatusChanged()
        }
    }

    fun setSettings() {
        bluetoothService?.setSettings(alertSettings)
    }

    fun getSettings(): AlertSettings {
        if (bluetoothService != null) {
            return bluetoothService!!.getSettings()
        } else {
            return alertSettings
        }
    }


}



