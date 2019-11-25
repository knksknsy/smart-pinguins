package de.hdm.closeme.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.data.model.NodeList
import kotlinx.android.synthetic.main.list_scanned_device.view.*

class ScannerListAdapter(internal var beaconMessageArrayList: NodeList, val context: Context?) :
    RecyclerView.Adapter<ScannerListAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_scanned_device, parent, false)
        return ViewHolder(view)
    }

    fun updateBeaconList(list: NodeList) {
        this.beaconMessageArrayList = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text =
            beaconMessageArrayList[position].messageMeshAccessBroadcast?.advChannel.toString()
    }


    override fun getItemCount(): Int {
        return beaconMessageArrayList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var text = itemView.text
    }
}
