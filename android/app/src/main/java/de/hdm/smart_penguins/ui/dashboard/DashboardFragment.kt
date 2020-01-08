package de.hdm.smart_penguins.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.lifecycle.ViewModelProviders
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.data.manager.DataManager
import de.hdm.smart_penguins.ui.BaseFragment


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



        emergency_btn.setOnClickListener(View.OnClickListener {
            if (emergencyState == false && cycleState == false && walkState == false) {
                emergency_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
                dataManager.device = 3
                dataManager.isEmergency = true
                emergencyState = true
            } else {
                emergency_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                dataManager.device
                dataManager.device = 1
                dataManager.isEmergency = false
                emergencyState = false
            }
        })

        cycle_btn.setOnClickListener(View.OnClickListener {
            if (emergencyState == false && cycleState == false && walkState == false) {
                cycle_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
                dataManager.device = 2
                cycleState = true
            }
            else {
                cycle_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                dataManager.device = 1
                cycleState = false
            }
        })

        walk_btn.setOnClickListener(View.OnClickListener {
            if (emergencyState == false && cycleState == false && walkState == false) {
                walk_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
                dataManager.device = 4
                walkState = true
            } else {
                walk_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                dataManager.device = 1
                walkState = false
            }
        })

        left_btn.setOnClickListener(View.OnClickListener {
            if (leftState == false && rightState == false) {
                left_btn.setBackgroundColor(resources.getColor(R.color.Green))
                dataManager.isLeftTurn = true
                leftState = true
            } else {
                left_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                dataManager.isLeftTurn = false
                leftState = false
            }
        })

        right_btn.setOnClickListener(View.OnClickListener {
            if (rightState == false && leftState == false) {
                right_btn.setBackgroundColor(resources.getColor(R.color.Green))
                dataManager.isRightTurn = true
                rightState = true
            } else {
                right_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                dataManager.isRightTurn = false
                rightState = false
            }
        })

        return root
    }
}