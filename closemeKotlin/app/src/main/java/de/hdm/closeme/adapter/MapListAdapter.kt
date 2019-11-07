package de.hdm.closeme.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.hdm.closeme.R
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.model.AlarmBeacon
import de.hdm.closeme.model.AlarmBeaconList
import de.hdm.closeme.util.BeaconUtil.Companion.getBeaconPlaceType
import kotlinx.android.synthetic.main.list_map_device.view.*
import java.util.*

class MapListAdapter(internal var beaconMessageArrayList: AlarmBeaconList, val context: Context?, val clickAction: (AlarmBeacon, Int) -> Unit, val placeAction: ((AlarmBeacon) -> Unit)?) : RecyclerView.Adapter<MapListAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_map_device, parent, false)
        return ViewHolder(view)
    }

    fun updateBeaconList(list: AlarmBeaconList) {
        this.beaconMessageArrayList = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val beacon = beaconMessageArrayList[position]
        if(beacon.isUsed) {
            holder.itemLayout.visibility = View.VISIBLE
            val setupMode = placeAction != null
            holder.deviceName.text = beacon.mac
            holder.deviceNumber.text = beacon.deviceNumber.toString()
            holder.placeButton.setOnClickListener {
                placeAction?.invoke(beacon)
                setBeaconPlaced(holder, beacon.isPlaced(), beacon)
            }
            holder.humidityImage.alpha = if (beacon.humidity == Constants.NO_VALUE_HUMIDITY) 0.3f else 1f
            holder.temperatureImage.alpha = if (beacon.temperature == Constants.NO_VALUE_TEMPERATURE) 0.3f else 1f
            if (setupMode) setBeaconPlaced(holder, beacon.placedType != Constants.PLACE_TYPE_NOT_PLACED, beacon)
            holder.placeButton.visibility = if (setupMode) View.VISIBLE else View.GONE
        }else{
            holder.itemLayout.visibility = View.GONE
        }
    }


    private fun setBeaconPlaced(holder: ViewHolder, isPlaced: Boolean,beacon: AlarmBeacon) {
        if(context != null) {
            holder.setIsRecyclable(false)
            holder.placeButton.text = if (isPlaced) context.getString(R.string.replace) else context.getString(R.string.place)
            if (isPlaced) {
                context.getColor(R.color.colorPrimary).let { color -> holder.placeButton.setTextColor(color) }
                holder.placeType.text = getBeaconPlaceType(beacon, context)
            } else
                context.getColor(R.color.White).let { color -> holder.placeButton.setTextColor(color) }
            holder.placeType.visibility = if (isPlaced) View.VISIBLE else View.INVISIBLE
            holder.humidityImage.visibility = if (isPlaced) View.INVISIBLE else View.VISIBLE
            holder.temperatureImage.visibility = if (isPlaced) View.INVISIBLE else View.VISIBLE
        }
    }


    override fun getItemCount(): Int {
        return beaconMessageArrayList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeButton = itemView.placeButton
        val itemLayout = itemView.itemLayout
        val deviceName = itemView.deviceName
        val deviceNumber = itemView.deviceNumber
        val humidityImage = itemView.humditiyImage
        val temperatureImage = itemView.temperatureImage
        val placeType = itemView.placeType

    }
}
