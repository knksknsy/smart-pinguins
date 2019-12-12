package de.hdm.smart_penguins.data.manager

import de.hdm.smart_penguins.SmartApplication
import de.hdm.smart_penguins.data.model.PersistentNode
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DataManager @Inject constructor(
    var application: SmartApplication

) {
    var qrScannedNodes = ArrayList<PersistentNode>()

    private val TAG = "DATA_MANAGER"
}



