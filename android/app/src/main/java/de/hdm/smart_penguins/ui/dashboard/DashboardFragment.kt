package de.hdm.smart_penguins.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import de.hdm.smart_penguins.R


class DashboardFragment : Fragment() {

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
        dashboardViewModel =
            ViewModelProviders.of(this).get(DashboardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val emergency_btn: Button = root.findViewById(R.id.emergency_btn)
        val cycle_btn: Button = root.findViewById(R.id.cycle_btn)
        val walk_btn: Button = root.findViewById(R.id.walk_btn)
        val left_btn: ImageButton = root.findViewById(R.id.left_btn)
        val right_btn: ImageButton = root.findViewById(R.id.right_btn)

        emergency_btn.setOnClickListener(View.OnClickListener {
            if (emergencyState == false) {
                emergency_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
                emergencyState = true
            } else {
                emergency_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                emergencyState = false
            }
        })

        cycle_btn.setOnClickListener(View.OnClickListener {
            if (cycleState == false) {
                cycle_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
                cycleState = true
            } else {
                cycle_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                cycleState = false
            }
        })

        walk_btn.setOnClickListener(View.OnClickListener {
            if (walkState == false) {
                walk_btn.setBackgroundColor(resources.getColor(R.color.LightGrey))
                walkState = true
            } else {
                walk_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                walkState = false
            }
        })

        left_btn.setOnClickListener(View.OnClickListener {
            if (leftState == false) {
                left_btn.setBackgroundColor(resources.getColor(R.color.Green))
                leftState = true
            } else {
                left_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                leftState = false
            }
        })

        right_btn.setOnClickListener(View.OnClickListener {
            if (rightState == false) {
                right_btn.setBackgroundColor(resources.getColor(R.color.Green))
                rightState = true
            } else {
                right_btn.setBackgroundColor(resources.getColor(R.color.defaultBtn))
                rightState = false
            }
        })

        return root
    }
}