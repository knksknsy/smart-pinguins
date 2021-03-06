package de.hdm.smart_penguins.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.component.AlarmLiveData
import de.hdm.smart_penguins.component.BleNodesLiveData
import de.hdm.smart_penguins.data.manager.ConnectionManager
import de.hdm.smart_penguins.data.manager.DataManager
import javax.inject.Inject

open class BaseFragment() : Fragment() {

    @Inject
    lateinit var connectionManager: ConnectionManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var nodesLiveData: BleNodesLiveData

    @Inject
    lateinit var alarm: AlarmLiveData

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        getMyApplication().activityComponent?.inject(this)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    fun getMyApplication(): SmartApplication =
        this.requireActivity().application as SmartApplication

}