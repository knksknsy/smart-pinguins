package de.hdm.smart_penguins.data.model

import android.os.Parcel
import android.os.Parcelable

class MessageMeshAccessBroadcast() : Parcelable {
    var messageType: Int = 0
        private set
    var networkId: Int = 0
        private set
    var isConnectable: Boolean = false
        private set
    private var moduleId0: Int = 0
    private var moduleId1: Int = 0
    private var moduleId2: Int = 0

    constructor(parcel: Parcel) : this() {
        moduleId0 = parcel.readInt()
        moduleId1 = parcel.readInt()
        moduleId2 = parcel.readInt()
    }


    override fun describeContents(): Int {
        return 0
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(messageType)
        parcel.writeInt(networkId)
        parcel.writeInt(if (isConnectable) 1 else 0)
        parcel.writeInt(moduleId0)
        parcel.writeInt(moduleId1)
        parcel.writeInt(moduleId2)

    }

    companion object CREATOR : Parcelable.Creator<MessageMeshAccessBroadcast> {
        override fun createFromParcel(parcel: Parcel): MessageMeshAccessBroadcast {
            return MessageMeshAccessBroadcast(parcel)
        }

        override fun newArray(size: Int): Array<MessageMeshAccessBroadcast?> {
            return arrayOfNulls(size)
        }
    }

}

