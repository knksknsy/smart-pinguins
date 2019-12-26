package de.hdm.smart_penguins.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.hdm.closeme.adapter.ScannerListAdapter
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.data.model.NodeList
import de.hdm.smart_penguins.ui.BaseFragment

class HomeFragment : BaseFragment() {
    private var adapter: ScannerListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)


        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val listView: RecyclerView = root.findViewById(R.id.list)
        listView.layoutManager = LinearLayoutManager(context)
        adapter = ScannerListAdapter(NodeList(), context)
        listView.adapter = adapter

        return root
    }

    override fun onResume() {
        super.onResume()

        nodesLiveData.observe(this, Observer { data ->
            adapter?.updateBeaconList(data)
        })

        alarm.observe(this, Observer { alarm -> })

        //TODO Change values und update Broadcasting
        dataManager.isJam = true
        connectionManager.updateBleBroadcasting()
    }

    override fun onPause() {
        super.onPause()
        alarm.removeObservers(this)
        nodesLiveData.removeObservers(this)
    }

}