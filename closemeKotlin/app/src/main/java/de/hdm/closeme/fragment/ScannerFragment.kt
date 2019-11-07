package de.hdm.closeme.fragment


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.hdm.closeme.MainActivity
import de.hdm.closeme.R
import de.hdm.closeme.adapter.ScannerListAdapter
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.listener.BeaconListListener
import de.hdm.closeme.model.AlarmBeacon
import de.hdm.closeme.model.AlarmBeaconList
import de.hdm.closeme.service.BluetoothService
import kotlinx.android.synthetic.main.fragment_scanner.*


/**
 * Scanner class
 *
 */
class ScannerFragment : Fragment(), BeaconListListener {

    var beaconArrayList = AlarmBeaconList()
    var adapter: ScannerListAdapter? = null
    var isServiceActive: Boolean = false
    private var isReady: Boolean = false
    private var isSetupMode: Boolean = false


    companion object {


        @JvmStatic
        fun newInstance(isSetupMode: Boolean) = ScannerFragment().apply {
            arguments = Bundle().apply {
                putBoolean(Constants.ARGUMENT_SETUP_MODE, isSetupMode)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        isServiceActive = ((activity) as MainActivity).isMyServiceRunning(BluetoothService::class.java)
        return inflater.inflate(R.layout.fragment_scanner, container, false)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        arguments?.getBoolean(Constants.ARGUMENT_SETUP_MODE)?.let { isSetupMode = it }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isReady) {
            if (isVisibleToUser) {
                registerBeaconUpdateListener()
            } else {
                unregisterBeaconUpdateListener()
            }
        }
    }


    override fun onBeaconListChanged(list: AlarmBeaconList) {
        this.beaconArrayList = ((activity) as MainActivity).getBeaconList()
        this.beaconArrayList.sortByRange()
        adapter?.updateBeaconList(beaconArrayList)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scannerServiceBtn.setOnClickListener {
            if (isServiceActive)
                ((activity) as MainActivity).stopBluetoothService()
            else
                ((activity) as MainActivity).startBluetoothService()
            isServiceActive = !isServiceActive
            setBtnText()
        }
        setBtnText()
    }

    private fun setBtnText() {
        scannerServiceBtn.text = if (isServiceActive) context?.getText(R.string.stop_foreground_service) else context?.getText(R.string.start_foreground_service)
    }

    private fun registerBeaconUpdateListener() {
        ((activity) as MainActivity).registerBeaconListListener(this)
        onBeaconListChanged(((activity) as MainActivity).getBeaconList())
    }

    private fun unregisterBeaconUpdateListener() {
        ((activity) as MainActivity).unregisterBeaconListListener()
    }

    override fun onResume() {
        super.onResume()
        if (!isSetupMode) registerBeaconUpdateListener()
        scannerBeaconList.layoutManager = LinearLayoutManager(context)
        adapter = ScannerListAdapter(((activity) as MainActivity).getBeaconList(), context,{
            beacon -> if(!beacon.isUsed)((activity) as MainActivity).saveBeaconPersistent(beacon, false)
        }, isSetupMode)
        scannerBeaconList.adapter = adapter
        isReady = true
        setBtnText()
    }

    override fun onPause() {
        super.onPause()
        if (!isSetupMode) unregisterBeaconUpdateListener()
        isReady = false
    }
    override fun onNotificationStatusChanged() {
    }
}
