package de.hdm.smart_penguins.ui.home

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler;
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.ui.BaseFragment



class HomeFragment : BaseFragment() {

    private var root: View? = null

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

        val timer = object: CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                oldNodes.clear()
            }
        }

        alarm.observe(this, Observer { alarm ->
            whenNotNull(alarm) {
                alarmNode = alarm.currentNode

                //if(alarmNode != oldNode) {
                if(alarmNode !in oldNodes){
                    //oldNode = alarmNode
                    oldNodes.add(alarmNode)
                    val cases = mutableListOf<String>()
                    var finishFlag = false

                    if(0!= alarm.nearestRescueLaneNodeId){
                        cases.add("emergency")
                    }
                    if(0 != alarm.nearestTrafficJamNodeId){
                        cases.add("jam")
                    }
                    if(0 != alarm.nearestBlackIceNode){
                        cases.add("blackice")
                    }
                    cases.add("")

                    finishFlag = execCase(cases)
                }

                Log.e("Emergency", alarm.nearestRescueLaneNodeId.toString())
                Log.e("Jam", alarm.nearestTrafficJamNodeId.toString())
                Log.e("Blackice", alarm.nearestBlackIceNode.toString())
                Log.e("Device Nr", alarm.currentNode.toString())

                timer.start()
            }
        })
    }

    private fun execCase(cases: MutableList<String>): Boolean{
        val caseSize = cases.size
        val timeInterval: Long = 7000
        val timeAll: Long = caseSize.toLong() * timeInterval
        val it: ListIterator<String> = cases.listIterator()

        val timer = object: CountDownTimer(timeAll, timeInterval) {
            override fun onTick(millisUntilFinished: Long) {
                val e = it.next()
                setVisibility(root,e.toString())
            }

            override fun onFinish() {
                setVisibility(root,"reset")
            }
        }.start()

        return true
    }

    private fun BEISPIELZUMAENDERNDERBROADCASTNACHRICHT(){
        //TODO Change values und update Broadcasting
        dataManager.isSlippery = true
        connectionManager.updateBleBroadcasting()
        dataManager.device
    }


    private fun setVisibility(view: View?, title: String) {
        if (view != null) {
            view.findViewById<LinearLayout>(R.id.platzhalter).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.emergency).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.balckice).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.jam).visibility = View.GONE

            when (title) {
                "emergency" -> {
                    view.findViewById<LinearLayout>(R.id.emergency).visibility = View.VISIBLE
                }
                "blackice" -> {
                    view.findViewById<LinearLayout>(R.id.balckice).visibility = View.VISIBLE
                }
                "jam" -> {
                    view.findViewById<LinearLayout>(R.id.jam).visibility = View.VISIBLE
                }
                "reset" ->{
                    view.findViewById<LinearLayout>(R.id.emergency).visibility = View.GONE
                    view.findViewById<LinearLayout>(R.id.balckice).visibility = View.GONE
                    view.findViewById<LinearLayout>(R.id.jam).visibility = View.GONE
                    view.findViewById<LinearLayout>(R.id.platzhalter).visibility = View.VISIBLE
                }
                else -> {
                    Log.e("Title", title)
                    view.findViewById<LinearLayout>(R.id.platzhalter).visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        alarm.removeObservers(this)
        nodesLiveData.removeObservers(this)
    }

    inline fun <T:Any, R> whenNotNull(input: T?, callback: (T)->R): R? {
        return input?.let(callback)
    }

}