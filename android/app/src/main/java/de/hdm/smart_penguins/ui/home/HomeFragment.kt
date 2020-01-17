package de.hdm.smart_penguins.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


class HomeFragment : BaseFragment() {

    private var displayCounter = 0
    private val VAL_SECOND: Long = 1000
    private val DELAY_REDISPLAY: Long = 10000
    private val DELAY_DISPLAY = 3

    private var root: View? = null

    private val TAG = "HomeFragment"
    private val STATE_NONE = 0
    private val STATE_EMERGENCY = 1
    private val STATE_BLACKICE = 2
    private val STATE_JAM = 3
    private val STATE_BIKE = 4
    private var counter = 0

    val cases = ArrayList<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fabProduct.setOnClickListener {
            if(developerView.isVisible){
                developerView.visibility = View.GONE
                productView.visibility = View.VISIBLE
            }else{
                developerView.visibility = View.VISIBLE
                productView.visibility = View.GONE
            }
        }
    }

    private fun updateTimer() {
            try {
                if(isVisible) {
                    val process = Runtime.getRuntime().exec("logcat -d *:E")
                    val bufferedReader = BufferedReader(
                        InputStreamReader(process.inputStream)
                    )
                    var log = StringBuilder()
                    var line: String? = ""
                    while (bufferedReader.readLine().also({ line = it }) != null) {
                        log.append("\n")
                        log.append(line?.replaceBefore("*penguins", ""))
                    }
                    scrollview.fullScroll(View.FOCUS_DOWN)

                    if (log.length > 80000) {
                        log = log.delete(0, log.length / 2)
                        Log.e("logDelete", log.length.toString())
                    }

                    //terminal.setMovementMethod(ScrollingMovementMethod())
                    terminal.text = log.toString()
                }
            }
            catch (e: IOException) { // Handle Exception
                Log.e("Terminal Error",e.toString())
            }
    }

    override fun onResume() {
        super.onResume()
        nodesLiveData.observe(this, Observer { data ->
            if (data.size > 0 && data[0].messageMeshAccessBroadcast != null) {
                nodeId.text =
                    "NodeId: " + data[0].messageMeshAccessBroadcast!!.deviceNumber.toString()
                type.text = "Type: " + data[0].messageMeshAccessBroadcast!!.type.toString()
                clusterSize.text =
                    "Clustersize: " + data[0].messageMeshAccessBroadcast!!.clusterSize.toString()
                direction.text =
                    "Direction: " + data[0].messageMeshAccessBroadcast!!.direction.toString()
                deviceDirection.text = "DeviceDirection: " + dataManager.direction
                nodeId.visibility = View.VISIBLE
                type.visibility = View.VISIBLE
                clusterSize.visibility = View.VISIBLE
                direction.visibility = View.VISIBLE
            } else {
                nodeId.visibility = View.INVISIBLE
                type.visibility = View.INVISIBLE
                clusterSize.visibility = View.INVISIBLE
                direction.visibility = View.INVISIBLE
            }
        })
        alarm.observe(this, Observer { alarm ->
            Log.e(TAG,"Received Alarm")
            if (alarm != null) {
                //if (alarm.currentNode !in dataManager.displayedAlarms && cases.size == 0) {
                if(cases.size == 0) {
                    if (STATE_NONE != alarm.nearestRescueLaneNodeId) {
                        cases.add(STATE_EMERGENCY)
                        counter += 2
                    }
                    if (STATE_NONE != alarm.nearestTrafficJamNodeId) {
                        cases.add(STATE_JAM)
                        counter += 2
                    }
                    if (STATE_NONE != alarm.nearestBlackIceNode) {
                        cases.add(STATE_BLACKICE)
                        counter += 2
                    }

                    if (alarm.isBikeNear) {
                        cases.add(0, STATE_BIKE)
                        counter += 2
                    }
                    Log.e(
                        TAG, "Emergency: " + alarm.nearestRescueLaneNodeId.toString()
                                + " Jam: " + alarm.nearestTrafficJamNodeId.toString()
                                + " Blackice: " + alarm.nearestBlackIceNode.toString()
                                + " Device Nr: " + alarm.currentNode.toString()
                    )
                }
            }
            updateTimer()
            tickTack();
        })
    }

    private fun tickTack() {
        Log.e(TAG,"Counter " + counter + " Cases: " + cases.size)
        if (counter > 0 && cases.size > 0) {
            display(cases[0])
            displayCounter += 1
            if (displayCounter == 2) {
                cases.removeAt(0)
                displayCounter = 0
            }
        } else {
            cases.clear()
            displayNone()
            platzhalter.visibility = View.VISIBLE
        }
        if(counter > 0) counter -= 1

    }


    private fun display(state: Int) {
        displayNone()
        when (state) {
            STATE_EMERGENCY -> {
                emergency.visibility = View.VISIBLE
            }
            STATE_BLACKICE -> {
                balckice.visibility = View.VISIBLE
            }
            STATE_JAM -> {
                jam.visibility = View.VISIBLE
            }
            STATE_BIKE -> {
                bike.visibility = View.VISIBLE
            }
        }
    }

    private fun displayNone() {
        if (isVisible) {
            platzhalter.visibility = View.GONE
            emergency.visibility = View.GONE
            balckice.visibility = View.GONE
            jam.visibility = View.GONE
            bike.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        alarm.removeObservers(this)
        nodesLiveData.removeObservers(this)
    }
}