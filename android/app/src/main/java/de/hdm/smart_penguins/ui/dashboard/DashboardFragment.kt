package de.hdm.smart_penguins.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.lifecycle.ViewModelProviders
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_dashboard.*


class DashboardFragment : BaseFragment() {



    private lateinit var dashboardViewModel: DashboardViewModel

    private var emergencyState = false
    private var cycleState = false
    private var walkState = false
    private var leftState = false
    private var rightState = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        dashboardViewModel = ViewModelProviders.of(this).get(DashboardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val emergency_btn: Button = root.findViewById(R.id.emergency_btn)
        val cycle_btn: Button = root.findViewById(R.id.cycle_btn)
        val walk_btn: Button = root.findViewById(R.id.walk_btn)
        val left_btn: ImageButton = root.findViewById(R.id.left_btn)
        val right_btn: ImageButton = root.findViewById(R.id.right_btn)

        if(dataManager.device == Constants.DEVICE_TYPE_EMERGENCY){
            emergency_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
        } else if(dataManager.device == Constants.DEVICE_TYPE_BIKE) {
            cycle_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
        } else if (dataManager.device == Constants.DEVICE_TYPE_WALK) {
            walk_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
        }

        if (dataManager.isLeftTurn) {
            left_btn.setBackgroundColor(resources.getColor(R.color.Green))
        } else if (dataManager.isRightTurn){
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

        right_btn.setOnClickListener(View.OnClickListener {
            onResetIndicator()
            onRight()
        })

        return root
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