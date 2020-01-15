package de.hdm.smart_penguins.ui.home

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : BaseFragment() {

    private var root: View? = null

    private val TYPE_EMERGENCY = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        root = inflater.inflate(R.layout.fragment_home, container, false)
        return root
    }

    override fun onResume() {
        super.onResume()

        nodesLiveData.observe(this, Observer { data ->
        })

        var alarmNode = 0
        //var oldNode = 0
        var oldNodes = ArrayList<Int>()
        var locker = false

        alarm.observe(this, Observer { alarm ->
            whenNotNull(alarm) {
                alarmNode = alarm.currentNode

                //if(alarmNode != oldNode) {
                if(alarmNode !in oldNodes){
                    //oldNode = alarmNode
                    oldNodes.add(alarmNode)
                    val cases = mutableListOf<String>()
                    var finishFlag = false

                    if (0 != alarm.nearestRescueLaneNodeId) {
                        cases.add("emergency")
                    }
                    if (0 != alarm.nearestTrafficJamNodeId) {
                        cases.add("jam")
                    }
                    if (0 != alarm.nearestBlackIceNode) {
                        cases.add("blackice")
                    }

                    if(alarm.isBikeNear == true) {
                        cases.add("bike")
                    }
                    cases.add("")

                    if(locker == false) {
                        locker = true
                        finishFlag = execCase(cases, locker)
                        val timerReset = object: CountDownTimer(30000, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                //Log.e("Tick:","Timer running")
                            }
                            override fun onFinish() {
                                oldNodes.clear()
                                Log.e("Timer:","Timer reset")
                            }
                        }.start()
                        locker = finishFlag
                    }
                    else{
                        Log.e("Function blocker","Old function in pipeline")
                    }

                }
                else{
                    Log.e("OldNodes",oldNodes.toString())
                }

                Log.e("Emergency", alarm.nearestRescueLaneNodeId.toString())
                Log.e("Jam", alarm.nearestTrafficJamNodeId.toString())
                Log.e("Blackice", alarm.nearestBlackIceNode.toString())
                Log.e("Device Nr", alarm.currentNode.toString())

            }
        })
    }

    private fun execCase(cases: MutableList<String>, locker: Boolean): Boolean {
        var locker = locker
        val caseSize = cases.size
        val timeInterval: Long = 7000
        val timeAll: Long = caseSize.toLong() * timeInterval
        val it: ListIterator<String> = cases.listIterator()

        val timer = object : CountDownTimer(timeAll, timeInterval) {
            override fun onTick(millisUntilFinished: Long) {
                if (it.hasNext()) {
                    val e = it.next()
                    setVisibility(e.toString())
                }
            }

            override fun onFinish() {
                setVisibility("reset")
                locker = false
            }
        }.start()

        return locker
    }

    private fun BEISPIELZUMAENDERNDERBROADCASTNACHRICHT() {
        //TODO Change values und update Broadcasting
        dataManager.isSlippery = true
        connectionManager.updateBleBroadcasting()
        dataManager.device
    }


    private fun setVisibility(title: String) {
        if (view != null) {
            platzhalter.visibility = View.GONE
            emergency.visibility = View.GONE
            balckice.visibility = View.GONE
            jam.visibility = View.GONE
            bike.visibility = View.GONE

            when (title) {
                "emergency" -> {
                    emergency.visibility = View.VISIBLE
                }
                "blackice" -> {
                    balckice.visibility = View.VISIBLE
                }
                "jam" -> {
                    jam.visibility = View.VISIBLE
                }
                "bike" -> {
                    bike.visibility = View.VISIBLE
                }
                "reset" -> {
                    emergency.visibility = View.GONE
                    balckice.visibility = View.GONE
                    jam.visibility = View.GONE
                    bike.visibility = View.GONE
                    platzhalter.visibility = View.VISIBLE
                }
                else -> {
                    Log.e("Title", title)
                    platzhalter.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        alarm.removeObservers(this)
        nodesLiveData.removeObservers(this)
    }

    inline fun <T : Any, R> whenNotNull(input: T?, callback: (T) -> R): R? {
        return input?.let(callback)
    }

}