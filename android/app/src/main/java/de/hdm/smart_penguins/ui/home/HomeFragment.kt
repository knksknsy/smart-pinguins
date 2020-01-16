package de.hdm.smart_penguins.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_home.*


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
        root = inflater.inflate(R.layout.fragment_home, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fabDismiss.setOnClickListener({ displayNone() })
    }

    override fun onResume() {
        super.onResume()
        dataManager.displayedAlarms.clear()
        alarm.observe(this, Observer { alarm ->
            Log.e(TAG, (System.currentTimeMillis() / 1000).toString())
            if (alarm != null) {
                //if (alarm.currentNode !in dataManager.displayedAlarms && cases.size == 0) {
                if(cases.size == 0){
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
                    //TODO
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
                    dataManager.displayedAlarms.add(alarm.currentNode)
                }
                Log.e("DisplayedNodes", dataManager.displayedAlarms.toString())
            }
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
    }
}