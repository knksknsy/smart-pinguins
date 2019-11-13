package de.hdm.smart_penguins.data.model

import android.os.Parcel
import android.os.Parcelable
import de.hdm.smart_penguins.data.Constants
import de.hdm.smart_penguins.utils.Util.getShort
import de.hdm.smart_penguins.utils.Util.getUnsignedChar
import de.hdm.smart_penguins.utils.Util.isBitSet

class MessageMeshAccessBroadcast : Parcelable {
    var messageType: Int = 0
        private set
    var networkId: Int = 0
        private set
    var isConnectable: Boolean = false
        private set
    private var moduleId0: Int = 0
    private var moduleId1: Int = 0
    private var moduleId2: Int = 0

    internal constructor(bytes: ByteArray) {
        messageType = getShort(bytes, Constants.OFFSET_MESSAGE_BROADCAST)
        networkId = getShort(bytes, Constants.OFFSET_MESSAGE_BROADCAST + 2)
        isConnectable = isBitSet(bytes, Constants.OFFSET_MESSAGE_BROADCAST + 4, 3)
        moduleId0 = getUnsignedChar(bytes, Constants.OFFSET_MESSAGE_BROADCAST + 9)
        moduleId1 = getUnsignedChar(bytes, Constants.OFFSET_MESSAGE_BROADCAST + 10)
        moduleId2 = getUnsignedChar(bytes, Constants.OFFSET_MESSAGE_BROADCAST + 11)
    }

    override fun describeContents(): Int {
        return 0
    }

    private constructor(`in`: Parcel) {
        messageType = `in`.readInt()
        networkId = `in`.readInt()
        isConnectable = `in`.readInt() == 1
        moduleId0 = `in`.readInt()
        moduleId1 = `in`.readInt()
        moduleId2 = `in`.readInt()

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

