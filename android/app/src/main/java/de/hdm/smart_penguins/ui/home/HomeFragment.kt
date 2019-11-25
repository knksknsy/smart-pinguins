package de.hdm.smart_penguins.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import de.hdm.closeme.adapter.ScannerListAdapter
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.component.BleNodesLiveData
import de.hdm.smart_penguins.data.model.NodeList
import javax.inject.Inject

class HomeFragment : Fragment() {


    @Inject
    lateinit var nodesLiveData: BleNodesLiveData

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        getMyApplication().activityComponent?.inject(this);

        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val listView: RecyclerView = root.findViewById(R.id.list)
        val adapter = ScannerListAdapter(NodeList(), context)
        listView.adapter = adapter

        nodesLiveData.observe(this, Observer { data ->
            adapter.updateBeaconList(data)
        })
        return root
    }

    fun getMyApplication(): SmartApplication =
    this.requireActivity().application as SmartApplication

}