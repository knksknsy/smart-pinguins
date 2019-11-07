package de.hdm.closeme.adapter

import android.content.Context
import android.content.res.Resources
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import de.hdm.closeme.R
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.model.AlarmBeacon
import de.hdm.closeme.model.AlarmSpot
import de.hdm.closeme.util.BeaconUtil.Companion.getBeaconPlaceType
import kotlinx.android.synthetic.main.list_scanned_device.view.*
import kotlin.collections.ArrayList

class ScannerListAdapter(internal var beaconMessageArrayList: ArrayList<AlarmBeacon>, val context: Context?, val clickAction: (AlarmBeacon) -> Unit, val setupMode: Boolean) : RecyclerView.Adapter<ScannerListAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_scanned_device, parent, false)
        return ViewHolder(view)
    }

    fun updateBeaconList(list: ArrayList<AlarmBeacon>) {
        this.beaconMessageArrayList = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (context != null) {
            val beacon = beaconMessageArrayList[position]
            holder.deviceName.text = beacon.mac
            holder.distance.text = context.getString(R.string.distance, beacon.distance)
            holder.rssi.text = context.getString(R.string.rssi, beacon.rssi)
            holder.placedText.visibility = if (beacon.isPlaced()) View.VISIBLE else View.GONE
            holder.channel.text = if (beacon.channel == Constants.NO_VALUE_CHANNEL) context.getString(R.string.placeholder_minus) else beacon.channel.toString()

            holder.type.text = beacon.beaconType
            holder.temperature.text = beacon.temperature.toString()
            holder.humidity.text = beacon.humidity.toString()
            holder.clusterSize.text = beacon.clusterSize.toString()
            holder.deviceNumber.text = beacon.deviceNumber.toString()
            holder.clusterId.text = beacon.clusterId.toString()
            holder.spotCount.text = beacon.spotCount.toString()
            holder.netwokId.text = beacon.networkId.toString()
            holder.moreButton.setOnClickListener {
                if (holder.moreLayout.visibility == View.VISIBLE) {
                    holder.moreLayout.visibility = View.GONE
                    holder.moreButton.setImageResource(R.drawable.arrow_down_vektor)
                } else {
                    holder.moreLayout.visibility = View.VISIBLE
                    holder.moreButton.setImageResource(R.drawable.arrow_up_vektor)
                }
            }
            holder.useCheckbox.isChecked = beacon.isUsed
            holder.useCheckbox.setOnCheckedChangeListener { _, isChecked ->
                beacon.isUsed = isChecked
                clickAction.invoke(beacon)
            }
            holder.useCheckbox.visibility = if (setupMode) View.VISIBLE else View.GONE

            if (beacon.humidity == Constants.NO_VALUE_HUMIDITY) holder.humidity.text = context.getString(R.string.placeholder_minus)
            if (beacon.humidity == Constants.NO_SENSOR_HUMIDITY) {
                holder.humidity.text = "No\nSensor"
                holder.humidityImage.alpha = 0.3f
            } else {
                holder.humidityImage.alpha = 1f
            }
            if (beacon.temperature == Constants.NO_SENSOR_TEMPERATURE) {
                holder.temperature.text = "No\nSensor"
                holder.temperatureImage.alpha = 0.3f

            } else {
                holder.temperatureImage.alpha = 1f
            }
            if (beacon.temperature == Constants.NO_VALUE_TEMPERATURE) holder.temperature.text = context.getString(R.string.placeholder_minus)
            if (beacon.clusterId == Constants.NO_VALUE_CLUSTER_ID) holder.clusterId.text = context.getString(R.string.placeholder_minus)
            if (beacon.distance == Constants.NO_VALUE_DISTANCE) holder.distance.text = context.getString(R.string.not_in_range)
            if (beacon.isPlaced()) holder.placedText.text =
                    context.getString(R.string.placed, getBeaconPlaceType(beacon, context))
            if (beacon.rssi == Constants.NO_VALUE_RSSI) holder.rssi.text = context.getString(R.string.not_in_range)
            val list = ArrayList<Short>()
            for (entry: Map.Entry<Short, AlarmSpot> in beacon.unsecuredSpotMap) {
                list.add(entry.value.deviceNumber)
            }
            holder.noUnsecuredWindowText.visibility = if (list.size == 0) View.VISIBLE else View.GONE
            holder.alarmList.visibility = if (list.size == 0) View.INVISIBLE else View.VISIBLE
            holder.alarmList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            val adapter = AlarmListAdapter(list)
            holder.alarmList.adapter = adapter
            holder.active.text = if (beacon.isActivated) context.getString(R.string.on) else context.getString(R.string.off)
            holder.active.setTextColor(context.getColor(if (beacon.isActivated) R.color.progressGreen else R.color.progressRed))
        } else {

        }
    }


    override fun getItemCount(): Int {
        return beaconMessageArrayList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noUnsecuredWindowText = itemView.noUnsecuredWindow
        val alarmList = itemView.alarmList
        val placedText = itemView.placed
        val rssiDescription = itemView.rssiDescription
        val itemLayout = itemView.itemLayout
        val moreLayout = itemView.moreLayout
        val deviceName = itemView.deviceName
        val rssi = itemView.rssi
        val distance = itemView.distance
        val deviceNumber = itemView.deviceNumber
        val humidityImage = itemView.humditiyImage
        val temperatureImage = itemView.temperatureImage
        val type = itemView.type
        val useCheckbox = itemView.useCheckbox
        val netwokId = itemView.networkId
        val moreButton: ImageView = itemView.moreButton
        val temperature = itemView.temperature
        val humidity = itemView.humditiy
        val clusterSize = itemView.clusterSize
        val channel = itemView.channel
        val clusterId = itemView.clusterId
        val spotCount = itemView.spotCount
        val active = itemView.active
    }
}
