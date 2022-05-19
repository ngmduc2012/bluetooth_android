package com.example.bluetooth_android

import com.beust.klaxon.Klaxon


private val klaxon = Klaxon()

class JsonResult(
    val id: Int,
    val result: Boolean,
) {

    public fun toJsonResult() = klaxon.toJsonString(this)

    companion object {
        public fun fromJsonResult(json: String) = klaxon.parse<JsonResult>(json)
    }
}