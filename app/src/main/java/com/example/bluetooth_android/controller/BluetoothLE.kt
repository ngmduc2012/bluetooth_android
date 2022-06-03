package com.example.bluetooth_android.controller

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.example.bluetooth_android.*
import java.util.concurrent.TimeUnit

class BluetoothLE(private val listener: CallBack) {

    // Khai báo bluetooth adapter (Kết nối với bluetooth phần cứng).
    /**
     * Declare bluetooth adapter (Connect with the hardware)
     * ---------------------------------------------------------------------------------------------
     * Khai báo bluetooth adapter (Kết nối với bluetooth phần cứng).
     */
//    val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    /** START SERVER*/
    /**
     * ################################################################################################
     * FUNCTION   : Setup status for view
     * DESCRIPTION:
     * (1) Checking if the device supports the bluetooth
     * (2) Checking if the bluetooth is opened
     * (3) Setup status for showing on UI
     * ------------------------------------------------------------------------------------------------
     * CHỨC NĂNG: Cài đặt trạng thái ban đầu
     * MÔ TẢ    :
     * (1) Kiểm tra thiết bị có hỗ trợ bluetooth hay không
     * (2) Kiếm tra thiết bị đang bật hay tắt bluetooth
     * (3) Cài đặt trạng thái để hiển thị ra giao diện ngoài màn hình.
     * ################################################################################################
     */
    private lateinit var bluetoothManager: BluetoothManager
    fun startServer(
        app: Application,
        mBluetoothAdapter: BluetoothAdapter?,
//        gattServerCallback: BluetoothGattServerCallback?,
//        gattServer: BluetoothGattServer?,
        messageCharacteristic: BluetoothGattCharacteristic?,
        gattClient: BluetoothGatt?
    ) {
        bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        /**(1)*/
        if (mBluetoothAdapter == null) {
            listener.showNotify("This device not supported", 7, null, null, null)
        } else {
            /**(2)*/
            if (mBluetoothAdapter.isEnabled) {
                listener.showNotify(null, STATE_TURN_ON, TURN_OFF, null, null)
                //Cài đặt Gatt server - setup Gatt Server
                setupGattServer(
                    app,
//                    gattServerCallback, gattServer
                    mBluetoothAdapter,
                    messageCharacteristic,
                gattClient
                )
                //Quảng bá thiết bị - Advertise device
                startAdvertisement(mBluetoothAdapter)

            } else {
                listener.showNotify(null, STATE_TURN_OFF, TURN_ON, null, null)
            }
        }
        /**(3)*/
//        MainActivity().setState()
    }

    /**
     * Start advertising this device so other BLE devices can see it and connect
     * ------------------------------------------------------------------------------------------------
     * Bắt đầu quản bá (advertising) thiết bị để nhữn thiết bị sử dụng BLE khác có thể tìm thấy
     * và kết nối.
     */
    @SuppressLint("MissingPermission")
    private fun startAdvertisement(mBluetoothAdapter: BluetoothAdapter?) {
        advertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser
        Log.d(TAG, "startAdvertisement: with advertiser $advertiser")

        if (advertiseCallback == null) {
            advertiseCallback = DeviceAdvertiseCallback()
            advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
    }

    // This property will be null if bluetooth is not enabled or if advertising is not
    // possible on the device
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var advertiseSettings: AdvertiseSettings = buildAdvertiseSettings()
    private var advertiseData: AdvertiseData = buildAdvertiseData()

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class DeviceAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            // Send error state to display
            val errorMessage = "Advertise failed with error: $errorCode"
            Log.d(TAG, "Advertising failed")
            //_viewState.value = DeviceScanViewState.Error(errorMessage)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Advertising successfully started")
        }
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTimeout(0)
            .build()
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private fun buildAdvertiseData(): AdvertiseData {
        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         * This limit is outlined in section 2.3.1.1 of this document:
         * https://inst.eecs.berkeley.edu/~ee290c/sp18/note/BLE_Vol6.pdf
         *
         * This limit includes everything put into AdvertiseData including UUIDs, device info, &
         * arbitrary service or manufacturer data.
         * Attempting to send packets over this limit will result in a failure with error code
         * AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         * onStartFailure() method of an AdvertiseCallback implementation.
         */
        val dataBuilder = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(true)

        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());
        return dataBuilder.build()
    }


    /** STOP SERVER*/
    /**
     * Stop advertising this device so other BLE devices can NOT see it and connect
     * ------------------------------------------------------------------------------------------------
     * Dừng quản bá (advertising) thiết bị để những thiết bị sử dụng BLE khác KHÔNG thể tìm thấy
     * và kết nối.
     */
    fun stopServer() {
        stopAdvertising()
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        Log.d(TAG, "Stopping Advertising with advertiser $advertiser")
        advertiser?.stopAdvertising(advertiseCallback)
        advertiseCallback = null
    }

    /** SERVER */
    /**
     * Function to setup a local GATT server.
     * This requires setting up the available services and characteristics that other devices
     * can read and modify.
     */

    private var gattServerCallback: BluetoothGattServerCallback? = null
    private var gattServer: BluetoothGattServer? = null

    @SuppressLint("MissingPermission")
    private fun setupGattServer(
        app: Application,
        mBluetoothAdapter: BluetoothAdapter?
//        gattServerCallback: BluetoothGattServerCallback?,
//        gattServer: BluetoothGattServer?
    ,
        messageCharacteristic: BluetoothGattCharacteristic?,
        gattClient: BluetoothGatt?
    ) {
        this.gattServerCallback = object : BluetoothGattServerCallback() {

            @SuppressLint("MissingPermission", "SetTextI18n")
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(device, status, newState)
                val isSuccess = status == BluetoothGatt.GATT_SUCCESS
                val isConnected = newState == BluetoothProfile.STATE_CONNECTED
                Log.d(
                    TAG,
                    "onConnectionStateChange: Server $device ${device.name} success: $isSuccess connected: $isConnected"
                )
                listener.showNotify(
                    null,
                    null,
                    null,
                    "Server $device ${device.name} success: $isSuccess connected: $isConnected",
                    null
                )
                if (isSuccess && isConnected) {
                    connectToChatDevice(device, app, mBluetoothAdapter)
                    listener.setTypeDevice(DEVICE_TYPE_LE)
                    listener.showNotify(
                        null,
                        STATE_CONNECTED,
                        null,
                        null, device.name + " " + device.address
                    )
                } else {
                    listener.showNotify(
                        null,
                        STATE_TURN_ON,
                        null,
                        null, null
                    )
                }
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                if (characteristic.uuid == MESSAGE_UUID) {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        "ok".toByteArray(Charsets.UTF_8)
                    )
                    val message = value?.toString(Charsets.UTF_8)
                    Log.d(TAG, "onCharacteristicWriteRequest: Have message: \"$message\"")
//                    runOnUiThread {
//                        tv_data.text = message.toString()
//                    }
                    getDataFromMessage(message)

                }
            }
        }


        this.gattServer = bluetoothManager.openGattServer(
            app,
            gattServerCallback
        ).apply {
            addService(setupGattService())
        }
//        listener.setGattServerCallback(this.gattServerCallback)
//        listener.setGattServer(this.gattServer)
    }

    /**
     * Function to create the GATT Server with the required characteristics and descriptors
     * ------------------------------------------------------------------------------------------------
     * Chức năng tạo Máy chủ GATT với các đặc tính và bộ mô tả cần thiết
     */
    private fun setupGattService(): BluetoothGattService {
        // Setup gatt service
        val service =
            BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        // need to ensure that the property is writable and has the write permission
        val messageCharacteristic = BluetoothGattCharacteristic(
            MESSAGE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(messageCharacteristic)
//        val confirmCharacteristic = BluetoothGattCharacteristic(
//            CONFIRM_UUID,
//            BluetoothGattCharacteristic.PROPERTY_WRITE,
//            BluetoothGattCharacteristic.PERMISSION_WRITE
//        )
//        service.addCharacteristic(confirmCharacteristic)

        return service
    }


    /** CLIENT */
    /**
     * Transmission and connection:
     *
     * STEP 1: Push the "FIND" button [btn_find]. When running the app [onStart] (or turning on
     * Bluetooth [onOffBluetooth]),[setupGattServer] and [startAdvertisement] were started.
     * [setupGattServer] assigns a UUID for server configuration, while [startAdvertisement]
     * aids in the discovery of other devices.
     * The device list will have devices that use BLE and have the same UUID that is setup in
     * [setupGattServer].
     *
     * STEP 2: Select one of them to connect with. NOTE: The CLIENT can only send messages to
     * the SERVER and cannot receive messages from it. An example: when A device finds and connects
     * to a B device, the B device is the server, and the A device is the client. A can only send
     * message to B. If you want to send a message from B to A, B has to find and connect to A, B
     * will be the client and A will be the server at that time. At this time, both device A and B
     * are both client and server of each other.
     *
     * STEP 3: Transmission: Each transmission can only transmit up to 20 characters and are
     * separated by a certain time interval of about 1 second.
     * ------------------------------------------------------------------------------------------------
     * Mô tả kết nối và truyền dữ liệu:
     *
     * B1: Ấn nút Tìm kiếm [btn_find], lúc này [setupGattServer] và [startAdvertisement] đã được bật
     * từ lúc bắt đầu chạy ứng dụng [onStart] (hoặc sau khi bật bluetooth [onOffBluetooth]). Trong đó,
     * [setupGattServer] cấu hình server và [startAdvertisement] giúp các thiết bị khác tìm thấy.
     * Danh sách tìm kiếm sẽ chứa các thiết bị sử dụng BLE và cùng các UUID được cài đặt trong
     * [setupGattServer].
     *
     * B2: Chọn 1 trong các thiết bị và tiến hành giao tiếp. CHÚ Ý: Chỉ có thể giao tiếp từ CLIENT
     * lên SERVER và không thể làm ngược lại. VD: Khi máy A thực hiện tìm kiếm và kết nối với máy B,
     * thì máy B là server và máy A là Client. Chỉ có thể giao tiếp từ máy A lên máy B. Muốn giao
     * tiếp từ máy B sang máy A thì máy B phải thực hiện tìm kiếm và kết nối tới máy A, khi này máy
     * B là client và máy A là server. lúc này cả máy A, B vừa là client và server của nhau.
     *
     * B3: Truyền dữ liệu: Mỗi lần truyền chỉ truyền được tối đa 20 kỹ tự và cách nhau 1 khoản thời
     * gian nhất định khoảng 1 giây.
     * */
    private var gattClientCallback: BluetoothGattCallback? = null

    private var gattClient: BluetoothGatt? = null
    @SuppressLint("MissingPermission")
    fun connectToChatDevice(
        device: BluetoothDevice,
        context: Context,
        mBluetoothAdapter: BluetoothAdapter?,
    ) {
        gattClientCallback = object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission", "SetTextI18n")
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                val isSuccess = status == BluetoothGatt.GATT_SUCCESS
                val isConnected = newState == BluetoothProfile.STATE_CONNECTED
                Log.d(
                    TAG,
                    "onConnectionStateChange: Client $gatt  success: $isSuccess connected: $isConnected"
                )
                // try to send a message to the other device as a test
                listener.showNotify(
                    null,
                    null,
                    null,
                    "Client ${gatt}  success: $isSuccess connected: $isConnected", null
                )
                if (isSuccess && isConnected) {
                    // discover services
                    gatt.discoverServices()

                    listener.showNotify(
                        null,
                        STATE_CONNECTED,
                        null,
                        null, device.name + " " + device.address
                    )
                } else {

                    listener.showNotify(
                        null,
                        STATE_TURN_ON,
                        null,
                        null, null
                    )
                }
            }

            override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(discoveredGatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "onServicesDiscovered: Have gatt $discoveredGatt")
                    gattClient = discoveredGatt
                    listener.setGattClient(gattClient)
                    val service = discoveredGatt.getService(SERVICE_UUID)
                    discoveredGatt.services.forEach {
                        Log.e(TAG, "discoveredGatt: getService ${it.uuid}")
                    }
//                    service.characteristics.forEach {
//                        Log.e(TAG, "service: characteristics ${it.uuid}")
//                    }
                    if (service != null) {
                        messageCharacteristic = service.getCharacteristic(MESSAGE_UUID)
                        listener.setMessageCharacteristic(messageCharacteristic)
//                        Log.e(
//                            TAG,
//                            "messageCharacteristic: ${service.getCharacteristic(MESSAGE_UUID)}"
//                        )
                        // IOS không thể nhận diện khi thiết bị kết nối, nên sau khi kết nối sẽ truyền
                        // tên của thiết bị android sang để IOS tự tìm và kết nối
                        // ---
                        // IOS can not recognize device connect to, so after connection, this device
                        // will send name to IOS for discover and connection
                        sendName(
                            mBluetoothAdapter,
                            messageCharacteristic,
                            gattClient
                        )
//                        sendMessage("123")
                    } else {
                        Log.e(
                            TAG,
                            "service: Have no getCharacteristic ${discoveredGatt.device.address}"
                        )
                    }
                }
            }
        }
        gattClient = device.connectGatt(context, false, gattClientCallback)
        listener.setGattClient(gattClient)
    }


    /** SENT/RECEIVE MESSAGE */
    /**
     * The input data type is [JSONData]. It includes an image that is Base64String [encodedImage]
     * and the text input from the screen [et_data].
     * [sendData]: The data will be converted to String type and split into 20-character and
     * transmitted by [sendMessage] one by one every 1 second. (*)
     * NOTE: Entering the character "} from the screen is not permitted.
     * [getDataFromMessage] When receiving a message from the client, the message is added to [data].
     * If the message contains the character "}, then convert the String [data] to the [JSONData] type,
     * read it, and display the result on the screen.(JSON structure is {"a": "x"; "b":"y"} so "}
     * is the end of the string message.
     * ------------------------------------------------------------------------------------------------
     * Giữ liệu đầu vào dạng [JSONData] gồm hình ảnh dạng Base64String [encodedImage] và văn bản được
     * nhập từ màn hình [et_data]
     * [sendData]: Dữ kiệu sẽ được chuyển thành String và cắt ra thành từng chuỗi 20 kỹ tự và lần lượt
     * được truyền đi bằng [sendMessage] sau mỗi 1 giây. (*)
     * CHÚ Ý: không nhập ký tự "} từ màn hình.
     * [getDataFromMessage] khi nhận dữ liệu, kết quả sẽ được ghép thêm vào [data], khi nhận được
     * dữ liệu có kết quả chứa ký tự "} thì sẽ tiến hành chuyển chuỗi String [data] thành [JSONData],
     * đọc và in kết quả ra ngoài màn hình. (Cấu trúc JSON là {"a": "x"; "b":"y"} nên "} là kết thúc của
     * chuỗi String.
     * */
//    private var data = ""

    @SuppressLint("NewApi")
    fun getDataFromMessage(message: String?) {
//        data += message
//        if (message!!.contains("\"}")) {
//            val jsonData: JSONData? = fromJsonData(data)
//            Log.d(TAG, "jsonData: ${jsonData!!.mes}")
//            Log.d(TAG, "jsonData: ${jsonData.image}")
//            if (jsonData.image.length > 1) {
//                val decodedBytes =
//                    Base64.getDecoder().decode(jsonData.image)
//                val image: Bitmap =
//                    BitmapFactory.decodeByteArray(
//                        decodedBytes,
//                        0,
//                        decodedBytes.size,
//                    )
//                runOnUiThread {
//                    tv_data.text = jsonData.mes
//                    iv_data.setImageBitmap(image)
//                }
//            } else {
//                runOnUiThread {
//                    tv_data.text = jsonData.mes
//                }
//            }
//            data = ""
//
//        }
//        runOnUiThread {
//            tv_data.text = message
//        }
        listener.showNotify(
            null,
            null,
            null,
            message, null
        )
    }

    // văn bản ko được chứa  }"
    @SuppressLint("SetTextI18n", "MissingPermission")
    fun sendData() {
//        val number = 20
//        val data = JSONData(et_data.text.toString(), encodedImage).toJsonData()
//        val count = if (data.length % number > 0) {
//            data.length / number
//        } else {
//            data.length / number - 1
//        }
//        Log.d(TAG, "data $data, count $count")
//        for (id in 0..count) {
////            Log.d(
////                TAG, "mess ${
////                    data.substring(
////                        id * number,
////                        if (data.length < (id + 1) * number) data.length else (id + 1) * number
////                    )
////                }"
////            )
//            sendMessage(
//                data.substring(
//                    id * number,
//                    if (data.length < (id + 1) * number) data.length else (id + 1) * number
//                )
//            )
//            /** (*) */
//            TimeUnit.SECONDS.sleep(1L)
//            val percent: Double = id.toDouble() / count.toDouble() * 100
//            Log.d(TAG, "percent $percent")
//            try {
//                runOnUiThread { tv_data.text = "$percent%" }
//            } catch (e: InterruptedException) {
//                Log.d(TAG, "e ${e.printStackTrace()}")
//            }
//        }

//        sendMessage(et_data.text.toString())


//        Log.i(TAG, "Value to be written is [" + et_data.text.toString() + "]")
//        // c.setValue("U");
//        // c.setValue("U");

//        gattClient?.let { it.writeCharacteristic(messageCharacteristic)} ?: run {}
    }


    @SuppressLint("MissingPermission")
    /** STATE OF DISPLAY */
    /**
     * ################################################################################################
     * Title: Send name device to IOS
     * Description:
     * After connection, only send onetime, so split 19 characters of name device. Send them to IOS for
     * discover and find the device that contain 19 characters of name.
     * ------------------------------------------------------------------------------------------------
     * Mô tả:
     * Sau khi kết nối, chỉ có thể truyền thành công 1 lần duy nhất với 19 ký tự, nên tên thiết bị sẽ bị cắt
     * lấy 19 ký tự đầu. 19 ký tự của tên thiết bị khi được truyền sang thiết bị IOS sẽ được lấy ra
     * và tiến hành tìm hiếm thiết bị có chưa 19 ký tự đầu trùng khớp.
     *################################################################################################
     */
    private fun sendName(
        mBluetoothAdapter: BluetoothAdapter?, messageCharacteristic: BluetoothGattCharacteristic?,
        gattClient: BluetoothGatt?
    ) {
        val number = 19
        val data =
            if (mBluetoothAdapter != null) mBluetoothAdapter?.name else ""
        val count = if (data.length % number > 0) {
            data.length / number
        } else {
            data.length / number - 1
        }
        Log.d(TAG, "data $data, count $count")
        for (id in 0..count) {
//            Log.d(
//                TAG, "mess ${
//                    data.substring(
//                        id * number,
//                        if (data.length < (id + 1) * number) data.length else (id + 1) * number
//                    )
//                }"
//            )
            sendMessage(
                "#" + data.substring(
                    id * number,
                    if (data.length < (id + 1) * number) data.length else (id + 1) * number
                ),
                messageCharacteristic,
                gattClient
            )
            break // chỉ lấy lần lặp đầu tiên.

            /** (*) */
            TimeUnit.SECONDS.sleep(1L)
            val percent: Double = id.toDouble() / count.toDouble() * 100
            Log.d(TAG, "percent $percent")
            try {
//                runOnUiThread { tv_data.text = "$percent%" }
            } catch (e: InterruptedException) {
                Log.d(TAG, "e ${e.printStackTrace()}")
            }
        }
    }

    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    @SuppressLint("MissingPermission", "SetTextI18n")
    fun sendMessage(
        message: String,
        messageCharacteristic: BluetoothGattCharacteristic?,
        gattClient: BluetoothGatt?
    ): Boolean {
        Log.d(TAG, "Send a message")

        if (messageCharacteristic == null) Log.e(TAG, "sendMessage: NO messageCharacteristic")
        messageCharacteristic?.let { characteristic ->
            Log.e(TAG, "sendMessage: messageCharacteristic")
            // WRITE_TYPE_NO_RESPONSE: Do IOS sử dụng .writeWithoutResponse nên phải sử dụng chung
            // thể loại truyền. Ưu điểm của kiểu truyền này nhanh hơn WRITE_TYPE_DEFAULT
            // với IOS truyền được tối đa 182 ký tự.
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

            val messageBytes = message.toByteArray(Charsets.UTF_8)
            characteristic.value = messageBytes
            gattClient?.let {
                val success = it.writeCharacteristic(messageCharacteristic)
                Log.d(TAG, "onServicesDiscovered: message send: ${success}")
                if (success) {
                    listener.showNotify(
                        null,
                        null,
                        null,
                        "onServicesDiscovered: message send: $success", null
                    )
                }
            } ?: run {
                Log.e(TAG, "sendMessage: no gatt connection to send a message with")
                listener.showNotify(
                    null,
                    null,
                    null,
                    "sendMessage: no gatt connection to send a message with", null
                )
            }
        }
        return false
    }


}