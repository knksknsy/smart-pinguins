package de.hdm.closeme.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.hdm.closeme.MainActivity
import de.hdm.closeme.R
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.model.AlarmBeacon
import de.hdm.closeme.model.AlarmSpot
import kotlinx.android.synthetic.main.list_open_window.view.*
import java.util.*

class OpenWindowAdapter(internal var unsercuredSpotMap: TreeMap<Short, AlarmSpot>, val context: Context, val clickAction: (AlarmSpot) -> Unit, val mapAction: (Short) -> Unit) : RecyclerView.Adapter<OpenWindowAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_open_window, parent, false)
        return ViewHolder(view)
    }

    fun updateAlarmList(list: TreeMap<Short, AlarmSpot>) {
        this.unsercuredSpotMap = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarmSpot: AlarmSpot? = unsercuredSpotMap.get(position.toShort())
        if (alarmSpot != null) {
            holder.deviceNumber.text = context.getString(R.string.open_window, alarmSpot.deviceNumber.toString())
            holder.humdityText.text = if(alarmSpot.humdity ==  Constants.NO_SENSOR_HUMIDITY)
                "/" else context.getString(R.string.percent, alarmSpot.humdity)
            holder.humidityProgress.progress = alarmSpot.humdity.toInt()
            holder.tempProgress.progress = alarmSpot.temperature.toInt()
            holder.tempText.text = if(alarmSpot.temperature == Constants.NO_SENSOR_HUMIDITY)
                "/" else context.getString(R.string.temperature, alarmSpot.temperature)
            holder.notifiyCheckbox.isChecked = alarmSpot.isAlarmChecked
            holder.tempEditText.text = if(alarmSpot.isAlarmChecked) context.getString(R.string.temperature, alarmSpot.alertTemperature) else "/"
            if(alarmSpot.isPlaced){
                holder.mapButton.alpha = 1f
                holder.mapImage.alpha = 1f
                holder.showOnMapTxt.text = context.getString(R.string.show_on_map)
                holder.mapButton.setOnClickListener({mapAction.invoke(alarmSpot.deviceNumber)})
            }else{
                holder.mapButton.alpha = 0.3f
                holder.mapImage.alpha = 0.3f
                holder.showOnMapTxt.text = context.getString(R.string.beacon_not_placed)

            }

            holder.plusButton.setOnClickListener({updateTemperature(holder.tempEditText,1,alarmSpot)})
            holder.minusButton.setOnClickListener({updateTemperature(holder.tempEditText,-1,alarmSpot)})
            holder.notifiyCheckbox.setOnCheckedChangeListener({_,checked ->
                alarmSpot.isAlarmChecked = checked
                holder.tempEditText.text = if(checked) context.getString(R.string.temperature, alarmSpot.alertTemperature) else "/"
                setAlarm(alarmSpot)
            })
        }
    }

    private fun updateTemperature(textView: TextView?, i: Int, alarmSpot: AlarmSpot) {
        if (textView != null) {
            alarmSpot.alertTemperature = (alarmSpot.alertTemperature + i).toShort()
            textView.text = context.getString(R.string.temperature, alarmSpot.alertTemperature)
            setAlarm(alarmSpot)
        }
    }

    private fun setAlarm(alarmSpot: AlarmSpot){
        clickAction.invoke(alarmSpot)
    }



    override fun getItemCount(): Int {
        return unsercuredSpotMap.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNumber = itemView.deviceNumber
        val humidityProgress = itemView.humidityProgress
        val humdityText = itemView.humditiyText
        val tempText = itemView.temperatureText
        val tempEditText = itemView.tempEditText
        val tempProgress = itemView.temperatureProgress
        val plusButton = itemView.tempEditPlus
        val minusButton = itemView.tempEditMinus
        val notifiyCheckbox = itemView.notifyCheckbox
        val mapButton = itemView.mapProgress
        val mapImage = itemView.mapImage
        val showOnMapTxt = itemView.showOnMapTxt
        val openWindowListLayout = itemView.openWindowListLayout
    }
}
