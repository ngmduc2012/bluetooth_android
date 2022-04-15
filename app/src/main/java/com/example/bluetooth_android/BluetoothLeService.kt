//package com.example.bluetooth_android
//
//import android.annotation.SuppressLint
//import android.app.Application
//import android.app.Service
//import android.bluetooth.*
//import android.bluetooth.le.AdvertiseCallback
//import android.bluetooth.le.AdvertiseData
//import android.bluetooth.le.AdvertiseSettings
//import android.bluetooth.le.BluetoothLeAdvertiser
//import android.content.ContentValues
//import android.content.ContentValues.TAG
//import android.content.Context
//import android.content.Intent
//import android.os.Binder
//import android.os.IBinder
//import android.os.ParcelUuid
//import android.util.Log
//import androidx.lifecycle.MutableLiveData
//import java.util.*
//
//class BluetoothLeService : Service() {
//
//
//    private val binder = LocalBinder()
//    private var bluetoothAdapter: BluetoothAdapter? = null
//    private var bluetoothGatt: BluetoothGatt? = null
//
//
//    override fun onBind(intent: Intent): IBinder {
//        return binder
//    }
//
//    fun initialize(): Boolean {
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//        if (bluetoothAdapter == null) {
//            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
//            return false
//        }
//        return true
//    }
//
//    private fun broadcastUpdate(action: String) {
//        val intent = Intent(action)
//        sendBroadcast(intent)
//    }
//
//    // trạng thía kết nối.
////    private var connectionState = STATE_DISCONNECTED
//
//    //    private val bluetoothGattCallback = object : BluetoothGattCallback() {
////        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
////            if (newState == BluetoothProfile.STATE_CONNECTED) {
////                // successfully connected to the GATT Server
////                val isSuccess = status == BluetoothGatt.GATT_SUCCESS
////                val isConnected = newState == BluetoothProfile.STATE_CONNECTED
////                Log.d(TAG, "onConnectionStateChange: Client2 $gatt  success: $isSuccess connected: $isConnected")
////                connectionState = STATE_CONNECTED
////                broadcastUpdate(ACTION_GATT_CONNECTED)
////            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
////                // disconnected from the GATT Server
////                connectionState = STATE_DISCONNECTED
////                broadcastUpdate(ACTION_GATT_DISCONNECTED)
////            }
////
////        }
////
////    }
//    companion object {
////        private lateinit var bluetoothManager: BluetoothManager
////
////        // BluetoothAdapter should never be null if the app is installed from the Play store
////        // since BLE is required per the <uses-feature> tag in the AndroidManifest.xml.
////        // If the app is installed on an emulator without bluetooth then the app will crash
////        // on launch since installing via Android Studio bypasses the <uses-feature> flags
////        private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
////
////        // LiveData for reporting the messages sent to the device
////        private val _requestEnableBluetooth = MutableLiveData<Boolean>()
////        private var gattServerCallback: BluetoothGattServerCallback? = null
////        private var gattServer: BluetoothGattServer? = null
////        fun startServer(app: Application) {
////            bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
////            if (!adapter.isEnabled) {
////                // prompt the user to enable bluetooth
////                _requestEnableBluetooth.value = true
////            } else {
////                _requestEnableBluetooth.value = false
////                //Cài đặt Gatt server
////                setupGattServer(app)
////                //Để Phảt hiện ra mắt cần truy cập
////                startAdvertisement()
////            }
////        }
////
////        fun stopServer() {
////            stopAdvertising()
////        }
////
////        /**
////         * Stops BLE Advertising.
////         */
////        @SuppressLint("MissingPermission")
////        private fun stopAdvertising() {
////            Log.d(TAG, "Stopping Advertising with advertiser $advertiser")
////            advertiser?.stopAdvertising(advertiseCallback)
////            advertiseCallback = null
////        }
////
////
////        // Properties for current chat device connection
////        private var currentDevice: BluetoothDevice? = null
////        fun setCurrentChatConnection(device: BluetoothDevice) {
////            currentDevice = device
////            connectToChatDevice(device)
////        }
////
////        private var gattClientCallback: BluetoothGattCallback? = null
////        private var gattClient: BluetoothGatt? = null
////        private var gatt: BluetoothGatt? = null
////        private var messageCharacteristic: BluetoothGattCharacteristic? = null
////
////        // hold reference to app context to run the chat server
////        private var app: Application? = null
////
////        @SuppressLint("MissingPermission")
////        private fun connectToChatDevice(device: BluetoothDevice) {
////            gattClientCallback = GattClientCallback()
////            gattClient = device.connectGatt(app, false, gattClientCallback)
////        }
////
////        private class GattClientCallback : BluetoothGattCallback() {
////            @SuppressLint("MissingPermission")
////            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
////                super.onConnectionStateChange(gatt, status, newState)
////                val isSuccess = status == BluetoothGatt.GATT_SUCCESS
////                val isConnected = newState == BluetoothProfile.STATE_CONNECTED
////                Log.d(
////                    TAG,
////                    "onConnectionStateChange: Client $gatt  success: $isSuccess connected: $isConnected"
////                )
////                // try to send a message to the other device as a test
////
////                if (isSuccess && isConnected) {
////                    // discover services
////                    gatt.discoverServices()
////                }
////            }
////
////            override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
////                super.onServicesDiscovered(discoveredGatt, status)
////                if (status == BluetoothGatt.GATT_SUCCESS) {
////                    Log.d(TAG, "onServicesDiscovered: Have gatt $discoveredGatt")
////                    gatt = discoveredGatt
////                    val service = discoveredGatt.getService(SERVICE_UUID)
////                    messageCharacteristic = service.getCharacteristic(MESSAGE_UUID)
////                }
////            }
////        }
////
////        /**
////         * Custom callback for the Gatt Server this device implements
////         */
////        private class GattServerCallback : BluetoothGattServerCallback() {
////            @SuppressLint("MissingPermission")
////            override fun onConnectionStateChange(
////                device: BluetoothDevice,
////                status: Int,
////                newState: Int
////            ) {
////                super.onConnectionStateChange(device, status, newState)
////                val isSuccess = status == BluetoothGatt.GATT_SUCCESS
////                val isConnected = newState == BluetoothProfile.STATE_CONNECTED
////                Log.d(
////                    TAG,
////                    "onConnectionStateChange: Server $device ${device.name} success: $isSuccess connected: $isConnected"
////                )
////                if (isSuccess && isConnected) {
//////                _connectionRequest.postValue(device)
////                } else {
//////                _deviceConnection.postValue(DeviceConnectionState.Disconnected)
////                }
////            }
////
////            @SuppressLint("MissingPermission")
////            override fun onCharacteristicWriteRequest(
////                device: BluetoothDevice,
////                requestId: Int,
////                characteristic: BluetoothGattCharacteristic,
////                preparedWrite: Boolean,
////                responseNeeded: Boolean,
////                offset: Int,
////                value: ByteArray?
////            ) {
////                super.onCharacteristicWriteRequest(
////                    device,
////                    requestId,
////                    characteristic,
////                    preparedWrite,
////                    responseNeeded,
////                    offset,
////                    value
////                )
////                if (characteristic.uuid == MESSAGE_UUID) {
////                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
////                    val message = value?.toString(Charsets.UTF_8)
////                    Log.d(TAG, "onCharacteristicWriteRequest: Have message: \"$message\"")
////                    message?.let {
//////                    _messages.postValue(RemoteMessage(it))
////                    }
////                }
////            }
////        }
////
////        /**
////         * Function to setup a local GATT server.
////         * This requires setting up the available services and characteristics that other devices
////         * can read and modify.
////         */
////        @SuppressLint("MissingPermission")
////        private fun setupGattServer(app: Application) {
////            gattServerCallback = GattServerCallback()
////
////            gattServer = bluetoothManager.openGattServer(
////                app,
////                gattServerCallback
////            ).apply {
////                addService(setupGattService())
////            }
////        }
////
////        /**
////         * Function to create the GATT Server with the required characteristics and descriptors
////         */
////        private fun setupGattService(): BluetoothGattService {
////            // Setup gatt service
////            val service =
////                BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
////            // need to ensure that the property is writable and has the write permission
////            val messageCharacteristic = BluetoothGattCharacteristic(
////                MESSAGE_UUID,
////                BluetoothGattCharacteristic.PROPERTY_WRITE,
////                BluetoothGattCharacteristic.PERMISSION_WRITE
////            )
////            service.addCharacteristic(messageCharacteristic)
////            val confirmCharacteristic = BluetoothGattCharacteristic(
////                CONFIRM_UUID,
////                BluetoothGattCharacteristic.PROPERTY_WRITE,
////                BluetoothGattCharacteristic.PERMISSION_WRITE
////            )
////            service.addCharacteristic(confirmCharacteristic)
////
////            return service
////        }
////
////        // This property will be null if bluetooth is not enabled or if advertising is not
////        // possible on the device
////        private var advertiser: BluetoothLeAdvertiser? = null
////        private var advertiseCallback: AdvertiseCallback? = null
////        private var advertiseSettings: AdvertiseSettings = buildAdvertiseSettings()
////        private var advertiseData: AdvertiseData = buildAdvertiseData()
////
////        /**
////         * Start advertising this device so other BLE devices can see it and connect
////         */
////        @SuppressLint("MissingPermission")
////        private fun startAdvertisement() {
////            advertiser = adapter.bluetoothLeAdvertiser
////            Log.d(TAG, "startAdvertisement: with advertiser $advertiser")
////
////            if (advertiseCallback == null) {
////                advertiseCallback = DeviceAdvertiseCallback()
////
////                advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
////            }
////        }
////
////        /**
////         * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
////         * in an Intent to be picked up by AdvertiserFragment and stops this Service.
////         */
////        private class DeviceAdvertiseCallback : AdvertiseCallback() {
////            override fun onStartFailure(errorCode: Int) {
////                super.onStartFailure(errorCode)
////                // Send error state to display
////                val errorMessage = "Advertise failed with error: $errorCode"
////                Log.d(TAG, "Advertising failed")
////                //_viewState.value = DeviceScanViewState.Error(errorMessage)
////            }
////
////            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
////                super.onStartSuccess(settingsInEffect)
////                Log.d(TAG, "Advertising successfully started")
////            }
////        }
////
////        /**
////         * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
////         * and disable the built-in timeout since this code uses its own timeout runnable.
////         */
////        private fun buildAdvertiseSettings(): AdvertiseSettings {
////            return AdvertiseSettings.Builder()
////                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
////                .setTimeout(0)
////                .build()
////        }
////
////        /**
////         * Returns an AdvertiseData object which includes the Service UUID and Device Name.
////         */
////        private fun buildAdvertiseData(): AdvertiseData {
////            /**
////             * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
////             * This limit is outlined in section 2.3.1.1 of this document:
////             * https://inst.eecs.berkeley.edu/~ee290c/sp18/note/BLE_Vol6.pdf
////             *
////             * This limit includes everything put into AdvertiseData including UUIDs, device info, &
////             * arbitrary service or manufacturer data.
////             * Attempting to send packets over this limit will result in a failure with error code
////             * AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
////             * onStartFailure() method of an AdvertiseCallback implementation.
////             */
////            val dataBuilder = AdvertiseData.Builder()
////                .addServiceUuid(ParcelUuid(SERVICE_UUID))
////                .setIncludeDeviceName(true)
////
////            /* For example - this will cause advertising to fail (exceeds size limit) */
////            //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
////            //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());
////            return dataBuilder.build()
////        }
////
////
////
////        private const val STATE_DISCONNECTED = 0
////        private const val STATE_CONNECTED = 2
//
//    }
//
//
////    @SuppressLint("MissingPermission")
////    fun connect(address: String): Boolean {
////        bluetoothAdapter?.let { adapter ->
////            try {
////                val device = adapter.getRemoteDevice(address)
////                // connect to the GATT server on the device
////                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
////                return true
////            } catch (exception: IllegalArgumentException) {
////                Log.w(ContentValues.TAG, "Device not found with provided address.")
////                return false
////            }
////            // connect to the GATT server on the device
////        } ?: run {
////            Log.w(ContentValues.TAG, "BluetoothAdapter not initialized")
////            return false
////        }
////    }
//
//    inner class LocalBinder : Binder() {
//        fun getService(): com.example.bluetooth_android.BluetoothLeService {
//            return this@BluetoothLeService
//        }
//    }
//
//    fun getSupportedGattServices(): List<BluetoothGattService?>? {
//        return bluetoothGatt?.services
//    }
//
//    override fun onUnbind(intent: Intent?): Boolean {
//        close()
//        return super.onUnbind(intent)
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun close() {
//        bluetoothGatt?.let { gatt ->
//            gatt.close()
//            bluetoothGatt = null
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
//        bluetoothGatt?.let { gatt ->
//            gatt.readCharacteristic(characteristic)
//        } ?: run {
//            Log.w(TAG, "BluetoothGatt not initialized")
//            return
//        }
//    }
//}