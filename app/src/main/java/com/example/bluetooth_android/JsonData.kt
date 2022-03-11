package com.example.bluetooth_android

import com.beust.klaxon.Klaxon


private val klaxon = Klaxon()

data class JSONData (
    val mes: String,
    val image: String,
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<JSONData>(json)
    }
}
