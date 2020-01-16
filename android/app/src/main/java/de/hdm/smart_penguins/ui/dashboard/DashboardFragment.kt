package de.hdm.smart_penguins.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_dashboard.*


class DashboardFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_dashboard, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (dataManager.device == Constants.DEVICE_TYPE_EMERGENCY) {
            emergency_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
        } else if (dataManager.device == Constants.DEVICE_TYPE_BIKE) {
            cycle_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
        } else if (dataManager.device == Constants.DEVICE_TYPE_WALK) {
            walk_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
        }

        if (dataManager.isLeftTurn) {
            left_btn.setBackgroundColor(resources.getColor(R.color.Green))
        } else if (dataManager.isRightTurn) {
            right_btn.setBackgroundColor(resources.getColor((R.color.Green)))
        }


        emergency_btn.setOnClickListener({
            onResetDevice()
            onEmergency()
        })

        cycle_btn.setOnClickListener({
            onResetDevice()
            onCycle()
        })

        walk_btn.setOnClickListener({
            onResetDevice()
            onWalk()
        })

        left_btn.setOnClickListener({
            onResetIndicator()
            onLeft()
        })

        right_btn.setOnClickListener({
            onResetIndicator()
            onRight()
        })
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
            }
        })
    }

    override fun onPause() {
        super.onPause()
        nodesLiveData.removeObservers(this)

    }

    private fun onResetDevice() {
        emergency_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
        cycle_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
        walk_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
    }

    private fun onResetIndicator() {
        left_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
        right_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
    }

    private fun onLeft() {
        if (dataManager.isLeftTurn == false) {
            left_btn.setBackgroundColor(resources.getColor(R.color.Green))
            dataManager.isLeftTurn = true
            dataManager.isRightTurn = false
        } else {
            dataManager.isLeftTurn = false
        }
    }

    private fun onRight() {
        if (dataManager.isRightTurn == false) {
            right_btn.setBackgroundColor(resources.getColor(R.color.Green))
            dataManager.isRightTurn = true
            dataManager.isLeftTurn = false
        } else {
            dataManager.isRightTurn = false
        }
    }

    private fun onEmergency() {
        if (dataManager.device == Constants.DEVICE_TYPE_EMERGENCY) {
            dataManager.device = Constants.DEVICE_TYPE_CAR
        } else {
            emergency_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
            dataManager.device = Constants.DEVICE_TYPE_EMERGENCY
        }
    }

    private fun onCycle() {
        if (dataManager.device == Constants.DEVICE_TYPE_BIKE) {
            dataManager.device = Constants.DEVICE_TYPE_CAR
        } else {
            cycle_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
            dataManager.device = Constants.DEVICE_TYPE_BIKE
        }
    }

    private fun onWalk() {
        if (dataManager.device == Constants.DEVICE_TYPE_WALK) {
            dataManager.device = Constants.DEVICE_TYPE_CAR
        } else {
            walk_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
            dataManager.device = Constants.DEVICE_TYPE_WALK
        }
    }
}