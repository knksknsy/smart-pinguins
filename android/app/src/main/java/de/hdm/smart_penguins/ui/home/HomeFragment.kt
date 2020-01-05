package de.hdm.smart_penguins.ui.home

import android.os.Bundle
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
    var isEmergency: Boolean = false
    var isSlippery: Boolean = false
    var isTraffic: Boolean = false

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

        alarm.observe(this, Observer { alarm ->

            whenNotNull(alarm) {
                if (0 != alarm.nearestRescueLaneNodeId) {
                    setVisibility(root, "emergency")
                }
                if (0 != alarm.nearestTrafficJamNodeId) {
                    setVisibility(root, "jam")
                }
                if (0 != alarm.nearestBlackIceNode) {
                    setVisibility(root, "blackice")
                }
                Log.e("Emergency", alarm.nearestRescueLaneNodeId.toString())
                Log.e("Jam", alarm.nearestTrafficJamNodeId.toString())
                Log.e("Blackice", alarm.nearestBlackIceNode.toString())
                Log.e("Device Nr", alarm.currentNode.toString())
            }
        })
    }

    private fun BEISPIELZUMAENDERNDERBROADCASTNACHRICHT(){
        //TODO Change values und update Broadcasting
        dataManager.isSlippery = true
        connectionManager.updateBleBroadcasting()
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