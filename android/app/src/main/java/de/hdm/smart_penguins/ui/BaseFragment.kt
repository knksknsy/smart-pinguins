package de.hdm.smart_penguins.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.component.BleNodesLiveData
import javax.inject.Inject

open class BaseFragment() : Fragment() {

    @Inject
    lateinit var nodesLiveData: BleNodesLiveData

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