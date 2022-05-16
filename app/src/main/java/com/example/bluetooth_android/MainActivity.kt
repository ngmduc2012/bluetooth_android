package com.example.bluetooth_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetooth_android.adapter.ListDeviceAdapter
import com.example.bluetooth_android.modle.JSONData
import com.example.bluetooth_android.modle.JSONData.Companion.fromJsonData
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    /**
     * Declare bluetooth adapter (Connect with the hardware)
     * ---------------------------------------------------------------------------------------------
     * Khai báo bluetooth adapter (Kết nối với bluetooth phần cứng).
     */
    private val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner = mBluetoothAdapter?.bluetoothLeScanner
    private val handler = Handler()

    //late init view, the following: https://stackoverflow.com/a/44285813/10621168
    private lateinit var btn_turn_on: Button
    private lateinit var btn_find: Button
    private lateinit var btn_disconnect: Button
    private lateinit var btn_send: Button
    private lateinit var btn_image: Button
    private lateinit var tv_no_blue: TextView
    private lateinit var tv_name: TextView
    private lateinit var tv_data: TextView
    private lateinit var iv_data: ImageView
    private lateinit var et_data: EditText
    private lateinit var pb_connecting: ProgressBar

    private lateinit var list_device: RecyclerView
    var listDevice: ArrayList<BluetoothDevice> = ArrayList()
    private lateinit var listDeviceAdapter: ListDeviceAdapter

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_no_blue = findViewById(R.id.tv_no_blue)
        btn_turn_on = findViewById(R.id.btn_turn_on)
        btn_disconnect = findViewById(R.id.btn_disconnect)
        btn_send = findViewById(R.id.btn_send)
        btn_image = findViewById(R.id.btn_image)
        btn_find = findViewById(R.id.btn_find)

        list_device = findViewById(R.id.list_device)
        tv_name = findViewById(R.id.tv_name)
        tv_data = findViewById(R.id.tv_data)
        iv_data = findViewById(R.id.iv_data)
        et_data = findViewById(R.id.et_data)

        // Show dialog progress, the following: https://www.tutorialkart.com/kotlin-android/android-indeterminate-progressbar-kotlin-example/
        pb_connecting = findViewById(R.id.pb_connecting)

        // Set up recycle view, the following: https://www.tutorialkart.com/kotlin-android/kotlin-android-recyclerview/
        listDeviceAdapter = ListDeviceAdapter(listDevice)
        val layoutManager = LinearLayoutManager(applicationContext)
        list_device.layoutManager = layoutManager
        list_device.adapter = listDeviceAdapter

        // Click in item in recycle view, the following: https://stackoverflow.com/a/49480714/10621168
        listDeviceAdapter.onItemClick = { device ->
            connectToChatDevice(device)
        }

        //click on button in kotlin, the following: https://stackoverflow.com/a/64187408/10621168
        btn_turn_on.setOnClickListener { onOffBluetooth() }
        btn_find.setOnClickListener { findDevice() }
        btn_disconnect.setOnClickListener { }
        btn_send.setOnClickListener { sendData() }
        //get image from gallery, Lấy hình ảnh trong thư viện ảnh, the following: https://stackoverflow.com/a/55933725/10621168
        btn_image.setOnClickListener { checkPermissionForImage() }

    }

    override fun onStart() {
        super.onStart()
        startServer(application)
    }

    override fun onStop() {
        super.onStop()
        stopServer()
    }

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
    private fun startServer(app: Application) {
        bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        /**(1)*/
        if (mBluetoothAdapter == null) {
            tv_no_blue.append("This device not supported")
            mState = STATE_NONE
        } else {
            /**(2)*/
            if (mBluetoothAdapter.isEnabled) {
                btn_turn_on.text = TURN_OFF
                mState = STATE_TURN_ON
                //Cài đặt Gatt server - setup Gatt Server
                setupGattServer(app)
                //Quảng bá thiết bị - Advertise device
                startAdvertisement()

            } else {
                btn_turn_on.text = TURN_ON
                mState = STATE_TURN_OFF
            }
        }
        /**(3)*/
        setState()
    }

    /**
     * Start advertising this device so other BLE devices can see it and connect
     * ------------------------------------------------------------------------------------------------
     * Bắt đầu quản bá (advertising) thiết bị để nhữn thiết bị sử dụng BLE khác có thể tìm thấy
     * và kết nối.
     */
    @SuppressLint("MissingPermission")
    private fun startAdvertisement() {
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
    private fun stopServer() {
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
    private fun setupGattServer(app: Application) {
        gattServerCallback = object : BluetoothGattServerCallback() {

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
                runOnUiThread {
                    tv_data.text =
                        "Server $device ${device.name} success: $isSuccess connected: $isConnected"
                    // Stuff that updates the UI
                }
                if (isSuccess && isConnected) {
                    runOnUiThread {
                        mState = STATE_CONNECTED
                        setState()
                        tv_name.text = device.name + " " + device.address
                        // Stuff that updates the UI
                    }
                } else {
                    runOnUiThread {
                        mState = STATE_TURN_ON
                        setState()
                    }
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
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, "ok".toByteArray(Charsets.UTF_8))
                    val message = value?.toString(Charsets.UTF_8)
                    Log.d(TAG, "onCharacteristicWriteRequest: Have message: \"$message\"")
                    runOnUiThread {
                        tv_data.text = message.toString()
                    }
                    getDataFromMessage(message)

                }
            }
        }

        gattServer = bluetoothManager.openGattServer(
            app,
            gattServerCallback
        ).apply {
            addService(setupGattService())
        }
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
        val confirmCharacteristic = BluetoothGattCharacteristic(
            CONFIRM_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(confirmCharacteristic)

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
    private fun connectToChatDevice(device: BluetoothDevice) {
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
                runOnUiThread {
                    tv_data.text = "Client ${gatt}  success: $isSuccess connected: $isConnected"
                    // Stuff that updates the UI
                }
                if (isSuccess && isConnected) {
                    // discover services
                    gatt.discoverServices()

                    runOnUiThread {
                        mState = STATE_CONNECTED
                        setState()
                        tv_name.text = device.name + " " + device.address
                        // Stuff that updates the UI
                    }
                } else {
                    runOnUiThread {
                        mState = STATE_TURN_ON
                        setState()
                    }
                }
            }

            override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(discoveredGatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "onServicesDiscovered: Have gatt $discoveredGatt")
                    gattClient = discoveredGatt
                    val service = discoveredGatt.getService(SERVICE_UUID)
                    messageCharacteristic = service.getCharacteristic(MESSAGE_UUID)
                }
            }
        }
        gattClient = device.connectGatt(application, false, gattClientCallback)
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
    private var data = ""

    @SuppressLint("NewApi")
    fun getDataFromMessage(message: String?) {
        data += message
        if (message!!.contains("\"}")) {
            val jsonData: JSONData? = fromJsonData(data)
            Log.d(TAG, "jsonData: ${jsonData!!.mes}")
            Log.d(TAG, "jsonData: ${jsonData.image}")
            if (jsonData.image.length > 1) {
                val decodedBytes =
                    Base64.getDecoder().decode(jsonData.image)
                val image: Bitmap =
                    BitmapFactory.decodeByteArray(
                        decodedBytes,
                        0,
                        decodedBytes.size,
                    )
                runOnUiThread {
                    tv_data.text = jsonData.mes
                    iv_data.setImageBitmap(image)
                }
            } else {
                runOnUiThread {
                    tv_data.text = jsonData.mes
                }
            }
            data = ""

        }
    }

    // văn bản ko được chứa  }"
    @SuppressLint("SetTextI18n")
    private fun sendData() {
        val number = 20
        val data = JSONData(et_data.text.toString(), encodedImage).toJsonData()
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
                data.substring(
                    id * number,
                    if (data.length < (id + 1) * number) data.length else (id + 1) * number
                )
            )
            /** (*) */
            TimeUnit.SECONDS.sleep(1L)
            val percent: Double = id.toDouble() / count.toDouble() * 100
            Log.d(TAG, "percent $percent")
            try {
                runOnUiThread { tv_data.text = "$percent%" }
            } catch (e: InterruptedException) {
                Log.d(TAG, "e ${e.printStackTrace()}")
            }
        }
    }

    private var messageCharacteristic: BluetoothGattCharacteristic? = null

    @SuppressLint("MissingPermission", "SetTextI18n")
    fun sendMessage(message: String): Boolean {
        Log.d(TAG, "Send a message")

        messageCharacteristic?.let { characteristic ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            val messageBytes = message.toByteArray(Charsets.UTF_8)
            characteristic.value = messageBytes
            gattClient?.let {
                val success = it.writeCharacteristic(messageCharacteristic)
                Log.d(TAG, "onServicesDiscovered: message send: ${success}")
                if (success) {
                    runOnUiThread {
                        tv_data.text = "onServicesDiscovered: message send: $success"
                    }
                }
            } ?: run {
                Log.d(TAG, "sendMessage: no gatt connection to send a message with")
                runOnUiThread {
                    tv_data.text = "sendMessage: no gatt connection to send a message with"
                }
            }
        }
        return false
    }


    /** STATE OF DISPLAY */
    /**
     * ################################################################################################
     * Title: Setup status for view
     * Description:
     * (1). If [STATE_NONE] - The meaning is the device no support bluetooth, only show one notify
     * line: This device not supported
     * (2). If [STATE_TURN_OFF] - Turning off bluetooth state, only show button turn on bluetooth
     * (3). If [STATE_TURN_ON] - Turning on bluetooth state, show button find device, list device,
     * button VISIBLE (function: discoverable for other device)
     * (4). If [STATE_CONNECTED] - Connected state: Show frame chat and button DISCONNECT (function:
     * disconnect with
     * ------------------------------------------------------------------------------------------------
     * Mô tả:
     * (1). Khi [STATE_NONE] - thiết bị không có bluetooth, sẽ chỉ hiển thị ra 1 dòng thông báo là thiết bị
     *      không có bluetooth
     * (2). Khi [STATE_TURN_OFF] - thiết bị đang tắt bluetooth, sẽ chỉ hiển thị nút bật bluetooth
     * (3). Khi [STATE_TURN_ON] - thiết bị đã bật bluetooth, sẽ hiển thị nút tìm kiếm thiết bị và danh sách
     *      thiết bị tìm kiếm, hiển thị nút VISIBLE (cho phép hiển thị với các thiết bị đang bật bluetooth)
     * (4). Khi [STATE_CONNECTED] - thiết bị đã kết nối với 1 thiết bị khác. Hiển thị khung chat và nút
     *      DISCONNECT để ngắt kết nối.
     *################################################################################################
     */
    private var mState = STATE_NONE
    private fun setState() {
        /**(1)*/
        when (mState) {
            STATE_NONE -> {
                pb_connecting.visibility = View.GONE
                tv_no_blue.visibility = View.VISIBLE
                btn_turn_on.visibility = View.GONE
                btn_find.visibility = View.GONE
                btn_disconnect.visibility = View.GONE
                btn_send.visibility = View.GONE
                list_device.visibility = View.GONE
                tv_name.visibility = View.GONE
                tv_data.visibility = View.GONE
                et_data.visibility = View.GONE
                iv_data.visibility = View.GONE
                btn_image.visibility = View.GONE
            }
            /**(2)*/
            STATE_TURN_OFF -> {
                pb_connecting.visibility = View.GONE
                tv_no_blue.visibility = View.GONE
                btn_turn_on.visibility = View.VISIBLE
                btn_find.visibility = View.GONE
                btn_disconnect.visibility = View.GONE
                btn_send.visibility = View.GONE
                list_device.visibility = View.GONE
                tv_name.visibility = View.GONE
                tv_data.visibility = View.GONE
                et_data.visibility = View.GONE
                iv_data.visibility = View.GONE
                btn_image.visibility = View.GONE
            }
            /**(3)*/
            STATE_TURN_ON -> {
                pb_connecting.visibility = View.GONE
                btn_turn_on.visibility = View.VISIBLE
                btn_find.visibility = View.VISIBLE
                btn_disconnect.visibility = View.GONE
                btn_send.visibility = View.GONE
                list_device.visibility = View.VISIBLE
                tv_name.visibility = View.GONE
                tv_data.visibility = View.GONE
                et_data.visibility = View.GONE
                iv_data.visibility = View.GONE
                btn_image.visibility = View.GONE

            }
            /**(4)*/
            STATE_CONNECTED -> {
                pb_connecting.visibility = View.GONE
                btn_turn_on.visibility = View.VISIBLE
                btn_find.visibility = View.VISIBLE
                btn_disconnect.visibility = View.VISIBLE
                btn_send.visibility = View.VISIBLE
                list_device.visibility = View.VISIBLE
                tv_name.visibility = View.VISIBLE
                tv_data.visibility = View.VISIBLE
                et_data.visibility = View.VISIBLE
                iv_data.visibility = View.VISIBLE
                btn_image.visibility = View.VISIBLE

            }
        }
    }

    /** ON OFF BLUETOOTH */
    /**
     * ################################################################################################
     * FUNCTION   : On/Off bluetooth
     * DESCRIPTION:
     *
     * (1) Turn on: Show dialog allow turn on bluetooth. If agree, change to [STATE_TURN_ON] state
     * (2) Turn off: If bluetooth is opened, turn off and change to [STATE_TURN_OFF] state
     * ------------------------------------------------------------------------------------------------
     * CHỨC NĂNG: Bật/Tắt bluetooth
     * MÔ TẢ    :
     *
     * (1) Bật bluetooth: hiển thị thông báo truy cập bluetooth. Nếu đồng ý sẽ chuyển sang trạng
     * thái [STATE_TURN_ON]
     * (2) Tắt bluetooth: Nếu bluetooth đang bật sẽ được tắt và chuyển về trạng thái [STATE_TURN_OFF]
     * ################################################################################################
     */
    @SuppressLint("MissingPermission")
    fun onOffBluetooth() {
        /**(1)*/
        if (!mBluetoothAdapter!!.isEnabled) {
            //Tham khảo/ The following: https://developer.android.com/guide/topics/connectivity/bluetooth/setup
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        /**(2)*/
        else {
            mBluetoothAdapter.disable()
            btn_turn_on.text = TURN_ON
            mState = STATE_TURN_OFF
            setState()
            stopServer()
        }
    }

    /** onActivityResult */
    // Get result in dialog, the following: https://stackoverflow.com/a/10407371/10621168
    @SuppressLint("MissingPermission", "NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /** The result for [onOffBluetooth]*/
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                btn_turn_on.text = TURN_OFF
                mState = STATE_TURN_ON
                setState()
                startServer(application)
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                btn_turn_on.text = TURN_ON
                mState = STATE_TURN_OFF
                setState()
                stopServer()
            }
        }


        /** The result for [pickImageFromGallery] */
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            iv_data.setImageURI(data?.data)
            val resolver = applicationContext.contentResolver
            resolver.openInputStream(data?.data!!).use { stream ->
                encodedImage = Base64.getEncoder().encodeToString(stream!!.readBytes())
                Log.d(TAG, "encodedImage: $encodedImage")
            }

        }

    }

    /** FIND DEVICE */
    private var isScanning = false

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun findDevice() {
        locationPermissionCheck()
        listDevice.clear()
        listDeviceAdapter.notifyDataSetChanged()
        if (!isScanning) {
            // Stops scanning after a pre-defined scan period - Dừng tìm kiếm thiết bị sau 1 khoản thời gian
            handler.postDelayed({
                isScanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
                runOnUiThread {
                    pb_connecting.visibility = View.GONE
                }
            }, SCAN_PERIOD)
            isScanning = true
            runOnUiThread {
                pb_connecting.visibility = View.VISIBLE
            }
            // Scan the device that is the same UUID with app - Tìm chính xác đến thiết bị có cùng UUID
            bluetoothLeScanner?.startScan(buildScanFilters(), buildScanSettings(), leScanCallback)
        } else {
            isScanning = false
            runOnUiThread {
                pb_connecting.visibility = View.GONE
            }
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    /**
     * Return a List of [ScanFilter] objects to filter by Service UUID [SERVICE_UUID]. That means they
     * have the same [SERVICE_UUID]
     * ---------------------------------------------------------------------------------------------
     * Trả về Danh sách các đối tượng [ScanFilter] lọc theo Service UUID [SERVICE_UUID]. Tức là chỉ
     * có thiết bị nào sử dụng cùng [SERVICE_UUID]
     */
    private fun buildScanFilters(): List<ScanFilter> {
        val builder = ScanFilter.Builder()
        builder.setServiceUuid(ParcelUuid(SERVICE_UUID))
        val filter = builder.build()
        return listOf(filter)
    }

    /**
     * Return a [ScanSettings] object set to use low power (to preserve battery life).
     * ---------------------------------------------------------------------------------------------
     * Cài đặt chế độ sử dụng tìm kiếm với mức tiêu thụ pin thấp. (Nhằm duy trì tuổi thọ pin)
     */
    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }

    /**
     * Device scan callback. After discovering a new device, add it to [listDevice] for display.
     * ---------------------------------------------------------------------------------------------
     * Kết quả trả về sau khi tìm kiếm thiết bị. Sau khi tìm thấy thiết bị mới, thêm vào [listDevice]
     * để hiển thị ra ngoài màn hình.
     */
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, result.toString())
            listDevice.add(result.device)
            listDeviceAdapter.notifyDataSetChanged()
        }
    }


    /** PERMISSION */
    //function for requesting location permission
    private fun locationPermissionCheck(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            !== PackageManager.PERMISSION_GRANTED
        ) {

            //show explanation for allowing permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                AlertDialog.Builder(this).setTitle("Locations Permission Needed")
                    .setMessage("Locations permission needed to continue")
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                        ActivityCompat.requestPermissions(
                            this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            LOCATION_REQUEST
                        )
                    }).create().show()

                //request permission without explanation
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_REQUEST
                )
            }
            false
        } else {
            true
        }
    }

    //function for requesting accept gallery permission
    private val galleryPermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private fun checkPermissionForImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                && (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            ) {
                requestPermissions(
                    galleryPermissions,
                    PERMISSION_CODE_READ
                )
            } else {
                pickImageFromGallery()
            }
        }

    }

    /**
     * ################################################################################################
     * FUNCTION   : Get image and change type to Base64 String in [encodedImage]
     * DESCRIPTION:
     *
     * Get the image from gallery and change type to Base64 String in [encodedImage], this for create
     * the message by [JSONData]
     * ------------------------------------------------------------------------------------------------
     * CHỨC NĂNG: Lấy hình ảnh và chuyển về dạng base64 string lưu vào biến [encodedImage]
     * MÔ TẢ    :
     *
     * Lấy hình ảnh từ thư viện ảnh và chuyển về dạng Base64 String tại biến [encodedImage], biến này
     * sau đó sẽ được truyền vào json để gửi đi ([JSONData]).
     * ################################################################################################
     */
    var encodedImage: String = ""
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(
            intent,
            IMAGE_PICK_CODE
        )
    }


}


