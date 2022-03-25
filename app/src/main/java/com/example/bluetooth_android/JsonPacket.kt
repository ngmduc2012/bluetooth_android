package com.example.bluetooth_android

import com.beust.klaxon.Klaxon


private val klaxon = Klaxon()

class JsonPacket(
    val sum: Int,
    val id: Int,
    val content: String,
    val hash: String,
) {

    public fun toJsonPacket() = klaxon.toJsonString(this)

    companion object {
        public fun fromJsonPacket(json: String) = klaxon.parse<JsonPacket>(json)
    }
}