package com.example.bluetooth_android.modle

import com.beust.klaxon.Klaxon


private val klaxon = Klaxon()

data class JSONData(
    val mes: String,
    val image: String,
) {
    fun toJsonData() = klaxon.toJsonString(this)

    companion object {
        fun fromJsonData(json: String) = klaxon.parse<JSONData>(json)
    }
}
