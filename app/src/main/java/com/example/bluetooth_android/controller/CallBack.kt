package com.example.bluetooth_android.controller

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback

interface CallBack  {
    /**
     * BLuetooth LE
     */
    fun showNotify(noBlue: String?, state: Int?, btnOn: String?, textData: String?, textName: String?) {}
    fun setMessageCharacteristic(messageCharacteristic: BluetoothGattCharacteristic?){}
//    fun setGattServerCallback(gattServerCallback: BluetoothGattServerCallback?){}
//    fun setGattServer(gattServer: BluetoothGattServer?){}
    fun setGattClient(gattClient: BluetoothGatt?){}
    fun setTypeDevice(typeDevice: Int?){}

    /**
     * BLuetooth Class
     */
}