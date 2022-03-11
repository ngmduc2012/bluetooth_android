package com.example.bluetooth_android

import com.beust.klaxon.Klaxon


private val klaxon = Klaxon()

class JsonPacket(
    val number: Int,
    val id: Int,
    val content: String,
) {

    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<JSONData>(json)
    }
}