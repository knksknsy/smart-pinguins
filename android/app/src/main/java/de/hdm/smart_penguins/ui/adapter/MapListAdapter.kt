package de.hdm.smart_penguins.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.hdm.smart_penguins.R
import de.hdm.smart_penguins.data.model.PersistentNode
import kotlinx.android.synthetic.main.list_map_device.view.*

class MapListAdapter(
    val context: Context?,
    val clickAction: (PersistentNode, Int) -> Unit,
    var persistentNodeList: ArrayList<PersistentNode>
) : RecyclerView.Adapter<MapListAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_map_device, parent, false)
        return ViewHolder(view)
    }

    fun update(list: ArrayList<PersistentNode>) {
        this.persistentNodeList = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val node = persistentNodeList[position]
        holder.placeButton.setOnClickListener {
            clickAction.invoke(node, position)
        }
        holder.deviceNumber.text = node.nodeID.toString()
    }


    override fun getItemCount(): Int {
        return persistentNodeList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeButton = itemView.placeButton
        val itemLayout = itemView.itemLayout
        val deviceNumber = itemView.deviceNumber
    }
}
