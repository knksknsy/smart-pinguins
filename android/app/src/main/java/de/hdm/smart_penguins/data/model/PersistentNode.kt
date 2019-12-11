package de.hdm.smart_penguins.data.model

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon

private val klaxon = Klaxon()

data class PersistentNode (
    @Json(name = "nodeId")
    val nodeID: Long,

    val lat: Double,
    val lng: Double,
    val type: Long
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<PersistentNode>(json)
    }
}
