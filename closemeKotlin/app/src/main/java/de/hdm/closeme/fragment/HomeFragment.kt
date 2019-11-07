package de.hdm.closeme.fragment


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.hdm.closeme.MainActivity
import de.hdm.closeme.R
import de.hdm.closeme.adapter.OpenWindowAdapter
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.listener.BeaconListListener
import de.hdm.closeme.model.AlarmBeaconList
import de.hdm.closeme.model.AlarmSpot
import de.hdm.closeme.service.BluetoothService
import kotlinx.android.synthetic.main.fragment_home.*


/**
 * A simple [Fragment] subclass.
 *
 */
class HomeFragment : Fragment(), BeaconListListener {

    private var adapter: OpenWindowAdapter? = null
    private var beaconArrayList: AlarmBeaconList = AlarmBeaconList()

    override fun onBeaconListChanged(list: AlarmBeaconList) {
        this.beaconArrayList = list
        this.beaconArrayList.sortByRange()
        updateScreen()
    }

    private fun updateScreen() {
        if (beaconArrayList.size > 0) {

            //Update Scanner info
            val inRangedBeacons = beaconArrayList.getInRangedBeacons().size
            scannerText.text = getString(R.string.beacons_found, inRangedBeacons)
            showNotConnectedWarning(inRangedBeacons)

            //Update Settings info
            val persistentBeaconCount = beaconArrayList.getPersistentBeacons().size
            val meshBeaconCount = beaconArrayList[0].clusterSize
            showMissingBeaconWarning(persistentBeaconCount - meshBeaconCount)

            if (beaconArrayList[0].isInRange()) {
                //Update nearest ruuvi info
                windowImage.setImageResource((if (beaconArrayList[0].isOpen) R.drawable.marker_window_open_vektor else R.drawable.marker_window_closed_vektor))
                windowText.text = if (beaconArrayList[0].isOpen) getString(R.string.open) else getString(R.string.closed)

                if (beaconArrayList[0].temperature != Constants.NO_SENSOR_TEMPERATURE) {
                    deviceNumber.text = getString(R.string.current_room, beaconArrayList[0].deviceNumber.toString())
                    temperatureText.text = getString(R.string.temperature, beaconArrayList[0].temperature)
                    humditiyText.text = getString(R.string.percent, beaconArrayList[0].humidity)
                    temperatureProgress.progress = beaconArrayList[0].temperature.toInt()
                    humidityProgress.progress = beaconArrayList[0].humidity.toInt()
                } else {
                    resetCurrentRoomView()
                }
                //if (beaconArrayList[0].spotCount.toInt() == beaconArrayList[0].unsecuredSpotMap.size) {
                openWindowList.visibility = View.VISIBLE
                if (adapter != null && context != null) adapter!!.updateAlarmList(this.beaconArrayList.getAlarmSpotList(context!!))

                setMapBtn(beaconArrayList[0].isPlaced())

            } else {
                resetCurrentRoomView()
                openWindowList.visibility = View.GONE
            }

        } else {
            resetCurrentRoomView()
            openWindowList.visibility = View.GONE
        }
    }

    private fun resetCurrentRoomView() {
        windowImage.setImageResource(R.drawable.marker_window_closed_vektor)
        windowText.text = getString(R.string.closed)
        deviceNumber.text = getString(R.string.current_room, "  No Beacon in range  ")
        temperatureText.text = "/"
        temperatureProgress.progress = 0
        humditiyText.text = "/"
        humidityProgress.progress = 0
        setMapBtn(false)
    }

    private fun setMapBtn(active: Boolean) {
        if (active) {
            mapProgress.alpha = 1f
            mapImage.alpha = 1f
            showOnMapTxt.text = getString(R.string.show_on_map)
            mapProgress.setOnClickListener({
                if (beaconArrayList.size > 0) {
                    ((activity) as MainActivity).
                            navigateTo(MapsFragment.
                                    newInstance(false, beaconArrayList[0].deviceNumber.toString()), true, Constants.TAG_MAP_FRAGMENT, true)
                }
            })
        } else {
            mapProgress.alpha = 0.3f
            mapImage.alpha = 0.3f
            showOnMapTxt.text = getString(R.string.beacon_not_placed)
            mapProgress.setOnClickListener {}
        }
    }


    private fun showNotConnectedWarning(inRangedBeacons: Int) {
        if (inRangedBeacons == 0) {
            warningView.visibility = View.VISIBLE
            warningViewHeadline.text = getString(R.string.no_connection)
            warningViewText.text = getString(R.string.not_connected)
        } else {
            warningView.visibility = View.GONE
        }
    }

    private fun showMissingBeaconWarning(diff: Int) {
        if (diff == 1) {
            missingBeaconView.visibility = View.VISIBLE
            missingBeaconViewText.text = getString(R.string.missing_beacon)
            missingBeaconViewHeadline.text = getString(R.string.missing_beacon_headline)
        } else if (diff > 1) {
            missingBeaconView.visibility = View.VISIBLE
            missingBeaconViewText.text = getString(R.string.missing_beacons, diff)
            missingBeaconViewHeadline.text = getString(R.string.missing_beacon_headline)
        } else {
            missingBeaconView.visibility = View.GONE
        }

    }

    override fun onNotificationStatusChanged() {
    }

    companion object {
        private var isServiceActive: Boolean = false
        @JvmStatic
        fun newInstance() = HomeFragment().apply {
            arguments = Bundle().apply {}
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        isServiceActive = ((activity) as MainActivity).isMyServiceRunning(BluetoothService::class.java)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsProgress.setOnClickListener {
            ((activity) as MainActivity).navigateTo(SetupFragment.newInstance(), true, Constants.TAG_SETUP_FRAGMENT, true)
        }
        powerProgress.setOnClickListener {
            if (isServiceActive)
                ((activity) as MainActivity).stopBluetoothService()
            else
                ((activity) as MainActivity).startBluetoothService()
            isServiceActive = !isServiceActive
            setBtnText()
        }
        scannerPorgress.setOnClickListener({
            ((activity) as MainActivity).navigateTo(ScannerFragment.newInstance(false), true, Constants.TAG_SCANNER_FRAGMENT, true)
        })
        alarmTypeProgress.setOnClickListener({ _ ->
            if (TextUtils.equals(alarmTypeText.text, getString(R.string.vibrate))) {
                ((activity) as MainActivity).getSettings().soundActivated = true
            } else {
                ((activity) as MainActivity).getSettings().soundActivated = false
            }
            setAlarmType()
            ((activity) as MainActivity).setSettings()
        })

    }

    private fun setAlarmType() {
        if (((activity) as MainActivity).getSettings().soundActivated) {
            alarmTypeText.text = getString(R.string.ring)
            alarmTypeImage.setImageResource(R.drawable.bell_vektor)
        } else {
            alarmTypeText.text = getString(R.string.vibrate)
            alarmTypeImage.setImageResource(R.drawable.vibration_vektor)
        }
    }

    private fun setBtnText() {
        powerImg.setImageResource(if (isServiceActive) R.drawable.power_on_vektor else R.drawable.power_off_vektor)
        powerProgress.progress = if (isServiceActive) 0 else 100
        //powerProgress.setProgressStartColor(if(isServiceActive) R.color.progressGreen else R.color.progressRed)
        powerText.text = if (isServiceActive) "ON" else "OFF"

    }


    override fun onResume() {
        super.onResume()
        registerBeaconUpdateListener()
        this.beaconArrayList.sortByRange()
        openWindowList.layoutManager = LinearLayoutManager(context)
        if (context != null) {
            adapter = OpenWindowAdapter(this.beaconArrayList.getAlarmSpotList(context!!), context!!, { alarmSpot ->
                setAlert(alarmSpot)
                ((activity) as MainActivity).setSettings()
            }, { deviceNumber -> ((activity) as MainActivity).showAlertOnMap(deviceNumber.toString()) })
            openWindowList.adapter = adapter
        }
        updateScreen()
        setBtnText()
        setAlarmType()
    }

    private fun setAlert(alarmSpot: AlarmSpot) {
        var isSet = false
        for (alert in ((activity) as MainActivity).getSettings().alertList) {
            if (alarmSpot.deviceNumber == alert.deviceNumber) {
                alert.isPlaced = alarmSpot.isPlaced
                alert.humdity = alarmSpot.humdity
                alert.alertTemperature = alarmSpot.alertTemperature
                alert.isAlarmChecked = alarmSpot.isAlarmChecked
                isSet = true
            }
        }
        if (!isSet) {
            ((activity) as MainActivity).getSettings().alertList.add(alarmSpot)
        }

    }


    private fun registerBeaconUpdateListener() {
        ((activity) as MainActivity).registerBeaconListListener(this)
        onBeaconListChanged(((activity) as MainActivity).getBeaconList())
    }

    private fun unregisterBeaconUpdateListener() {
        ((activity) as MainActivity).unregisterBeaconListListener()
    }

    override fun onPause() {
        super.onPause()
        unregisterBeaconUpdateListener()
    }
}
