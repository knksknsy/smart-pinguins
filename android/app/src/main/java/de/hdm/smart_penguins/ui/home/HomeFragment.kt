package de.hdm.smart_penguins.ui.home

import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.NullPointerException
import java.util.*
import kotlin.collections.ArrayList


class HomeFragment : BaseFragment() {

    private var root: View? = null
    private var onTouch = false
    val terminalTimerTask = Timer()

    private val TYPE_EMERGENCY = 1

    val asyncTimer = AsyncTimer()


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

        asyncTimer.execute()

    }

    inner class AsyncTimer: AsyncTask<Int, String, String>() {
        var flag = false

        override fun onPreExecute() {
            super.onPreExecute()
            flag = true
        }

        override fun doInBackground(vararg params: Int?): String {
            var sender = "Test"

            while(flag) {
                terminalTimerTask?.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        var log = StringBuilder()
                        val process = Runtime.getRuntime().exec("logcat -d *:E")
                        val bufferedReader = BufferedReader(
                            InputStreamReader(process.inputStream)
                        )
                        var line: String? = ""
                        while (bufferedReader.readLine().also({ line = it }) != null) {
                            log.append(line)
                        }

                        if (log.length > 80000) {
                            log = log.delete(0, log.length / 2)
                        }

                        sender = log.toString()

                    }
                }, 0, 1000)

            }

            return sender
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            terminal.text = result
            scrollview.fullScroll(View.FOCUS_DOWN)
            flag = false
        }

    }

    override fun onResume() {
        super.onResume()

        //asyncTimer.execute()

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

                    if(dataManager.device == Constants.DEVICE_TYPE_CAR){

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

                }
                else{
                    Log.e("OldNodes",oldNodes.toString())
                }

                Log.e("Emergency", alarm.nearestRescueLaneNodeId.toString())
                Log.e("Jam", alarm.nearestTrafficJamNodeId.toString())
                Log.e("Blackice", alarm.nearestBlackIceNode.toString())
                Log.e("Device Nr", alarm.currentNode.toString())
                Log.e("Is Bike Near", alarm.isBikeNear.toString())

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
        terminalTimerTask.cancel()

        asyncTimer.cancel(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        alarm.removeObservers(this)
        nodesLiveData.removeObservers(this)
        terminalTimerTask.cancel()

        asyncTimer.cancel(true)
    }

    inline fun <T : Any, R> whenNotNull(input: T?, callback: (T) -> R): R? {
        return input?.let(callback)
    }

}