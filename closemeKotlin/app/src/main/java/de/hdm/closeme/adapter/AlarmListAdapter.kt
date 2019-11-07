package de.hdm.closeme.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.hdm.closeme.R
import de.hdm.closeme.model.AlarmBeacon
import kotlinx.android.synthetic.main.list_alarm.view.*
import java.util.*

class AlarmListAdapter(internal var alarmList: ArrayList<Short>) : RecyclerView.Adapter<AlarmListAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_alarm, parent, false)
        return ViewHolder(view)
    }

    fun updateAlarmList(list: ArrayList<Short>) {
        this.alarmList = list
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.deviceNumber.text = alarmList[position].toString()
    }


    override fun getItemCount(): Int {
        return alarmList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNumber = itemView.deviceNumber
    }
}
