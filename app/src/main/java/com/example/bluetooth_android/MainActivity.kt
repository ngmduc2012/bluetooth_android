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
import android.graphics.Matrix
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetooth_android.adapter.ListDeviceAdapter
import com.example.bluetooth_android.modle.JSONData
import com.example.bluetooth_android.modle.JSONData.Companion.fromJsonData
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {


    // Khai báo bluetooth adapter (Kết nối với bluetooth phần cứng).
    /**
     * Declare bluetooth adapter (Connect with the hardware)
     * ---------------------------------------------------------------------------------------------
     * Khai báo bluetooth adapter (Kết nối với bluetooth phần cứng).
     */
    private val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner = mBluetoothAdapter?.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 30000L
    var bluetoothGatt: BluetoothGatt? = null

//    private var bluetoothService: BluetoothLeService? = null
    private var gatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var gattServer: BluetoothGattServer? = null

    // LiveData for reporting the messages sent to the device
    private val _messages = MutableLiveData<Message>()
    val messages = _messages as LiveData<Message>


    //late init view, the following: https://stackoverflow.com/a/44285813/10621168
    private lateinit var btn_turn_on: Button
    private lateinit var btn_find: Button
    private lateinit var btn_server: Button
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
        btn_server = findViewById(R.id.btn_server)
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
            // Start the thread to connect with the given device
            setCurrentChatConnection(device)
//            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
//            mBluetoothAdapter?.let { adapter ->
//                try {
//                    adapter.getRemoteDevice(device.address)
//
//
//                } catch (exception: IllegalArgumentException) {
//                    Log.w(TAG, "Device not found with provided address.")
//
//                }
//                // connect to the GATT server on the device
//            } ?: run {
//                Log.w(TAG, "BluetoothAdapter not initialized")
//
//            }

        }
//        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
//        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        onInitState()

        //click on button in kotlin, the following: https://stackoverflow.com/a/64187408/10621168
        btn_turn_on.setOnClickListener { onOffBluetooth() }
        btn_find.setOnClickListener { findDevice() }
        btn_server.setOnClickListener { reloadServer() }
        btn_disconnect.setOnClickListener { }
        btn_send.setOnClickListener { sendData() }
        //get image from gallery, Lấy hình ảnh trong thư viện ảnh, the following: https://stackoverflow.com/a/55933725/10621168
        btn_image.setOnClickListener { checkPermissionForImage() }

    }

    private fun reloadServer() {
        stopServer()
        startServer(application)
        mState = STATE_TURN_ON
        setState()
        findDevice()
    }


    /** ============================================================================================= */

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
            TimeUnit.SECONDS.sleep(1L)
            val percent: Double = id.toDouble()/count.toDouble()*100
            Log.d(TAG, "percent $percent")
            try {
                runOnUiThread { tv_data.text = "$percent%" }
            } catch (e: InterruptedException) {
                Log.d(TAG, "e ${e.printStackTrace()}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendMessage(message: String): Boolean {
        Log.d(TAG, "Send a message")

        messageCharacteristic?.let { characteristic ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            val messageBytes = message.toByteArray(Charsets.UTF_8)
            characteristic.value = messageBytes
            gatt?.let {
                val success = it.writeCharacteristic(messageCharacteristic)
                Log.d(TAG, "onServicesDiscovered: message send: $success")
                if (success) {
                    runOnUiThread {
                        tv_data.text = success.toString()
                    }

//                    _messages.value = Message.LocalMessage(message)
                }
            } ?: run {
                Log.d(TAG, "sendMessage: no gatt connection to send a message with")
            }
        }
        return false
    }


    private lateinit var bluetoothManager: BluetoothManager

    // BluetoothAdapter should never be null if the app is installed from the Play store
    // since BLE is required per the <uses-feature> tag in the AndroidManifest.xml.
    // If the app is installed on an emulator without bluetooth then the app will crash
    // on launch since installing via Android Studio bypasses the <uses-feature> flags
    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    // LiveData for reporting the messages sent to the device
    private val _requestEnableBluetooth = MutableLiveData<Boolean>()
    private var gattServerCallback: BluetoothGattServerCallback? = null

    private fun startServer(app: Application) {
        bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!adapter.isEnabled) {
            // prompt the user to enable bluetooth
            _requestEnableBluetooth.value = true
        } else {
            _requestEnableBluetooth.value = false
            //Cài đặt Gatt server
            setupGattServer(app)
            //Để Phảt hiện ra mắt cần truy cập
            startAdvertisement()
        }
    }

    private fun stopServer() {
        stopAdvertising()
    }

    /**
     * Stops BLE Advertising.
     */
    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        Log.d(TAG, "Stopping Advertising with advertiser $advertiser")
        advertiser?.stopAdvertising(advertiseCallback)
        advertiseCallback = null
    }


    // Properties for current chat device connection
    private var currentDevice: BluetoothDevice? = null
    private fun setCurrentChatConnection(device: BluetoothDevice) {
        currentDevice = device
        connectToChatDevice(device)
    }

    private var gattClientCallback: BluetoothGattCallback? = null
    private var gattClient: BluetoothGatt? = null

    // hold reference to app context to run the chat server
    private var app: Application? = null

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

                if (isSuccess && isConnected) {
                    // discover services
                    gatt.discoverServices()

                    runOnUiThread {
                        mState = STATE_CONNECTED
                        setState()
                        tv_name.text = device.name + " " + device.address
                        tv_name.visibility = View.VISIBLE
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
                    gatt = discoveredGatt
                    val service = discoveredGatt.getService(SERVICE_UUID)
                    messageCharacteristic = service.getCharacteristic(MESSAGE_UUID)
                }
            }
        }
        gattClient = device.connectGatt(app, false, gattClientCallback)
    }


    /**
     * Function to setup a local GATT server.
     * This requires setting up the available services and characteristics that other devices
     * can read and modify.
     */
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
                if (isSuccess && isConnected) {
                    runOnUiThread {
                        mState = STATE_CONNECTED
                        setState()
                        tv_name.text = device.name + " " + device.address
                        tv_name.visibility = View.VISIBLE
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
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                    val message = value?.toString(Charsets.UTF_8)
                    Log.d(TAG, "onCharacteristicWriteRequest: Have message: \"$message\"")
                    runOnUiThread {
                        tv_data.text = message.toString()
                    }
                    getDataFromMessage(message)
                    message?.let {
//                    _messages.postValue(RemoteMessage(it))
                    }
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
                val matrix = Matrix()

                // Xoay hình ảnh - Rotate image
                // The following: https://helpex.vn/question/android-xoay-hinh-anh-trong-che-do-xem-anh-theo-mot-goc-6094e25af45eca37f4c0d40a
//                                        matrix.postRotate(90F)
                val image: Bitmap =
                    BitmapFactory.decodeByteArray(
                        decodedBytes,
                        0,
                        decodedBytes.size,
                    )

//                                        val rotated = Bitmap.createBitmap(
//                                            image, 0, 0, image.getWidth(), image.getHeight(),
//                                            matrix, true
//                                        )
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

    /**
     * Function to create the GATT Server with the required characteristics and descriptors
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

    // This property will be null if bluetooth is not enabled or if advertising is not
    // possible on the device
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var advertiseSettings: AdvertiseSettings = buildAdvertiseSettings()
    private var advertiseData: AdvertiseData = buildAdvertiseData()

    /**
     * Start advertising this device so other BLE devices can see it and connect
     */
    @SuppressLint("MissingPermission")
    private fun startAdvertisement() {
        advertiser = adapter.bluetoothLeAdvertiser
        Log.d(TAG, "startAdvertisement: with advertiser $advertiser")

        if (advertiseCallback == null) {
            advertiseCallback = DeviceAdvertiseCallback()

            advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
    }

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
    private val TURN_ON = "TURN ON"
    private val TURN_OFF = "TURN OFF"
    private fun onInitState() {
        /**(1)*/
        if (mBluetoothAdapter == null) {
            tv_no_blue.append("This device not supported")
            mState = STATE_NONE
        } else {
            /**(2)*/
            if (mBluetoothAdapter.isEnabled) {
                btn_turn_on.text = TURN_OFF
                mState = STATE_TURN_ON

            } else {
                btn_turn_on.text = TURN_ON
                mState = STATE_TURN_OFF
            }
        }
        /**(3)*/
        setState()
    }

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
    val STATE_NONE = 7
    val STATE_TURN_OFF = 8
    val STATE_TURN_ON = 9
    val STATE_CONNECTED = 12
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

    // Run the chat server as long as the app is on screen
    override fun onStart() {
        super.onStart()
        startServer(application)
    }

    override fun onStop() {
        super.onStop()
        stopServer()
    }

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
     * (1) Bật bluetooth: hiển thị thông báo truy cập bluetooth . Nếu đồng ý sẽ chuyển sang trạng
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
        }
    }


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
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                btn_turn_on.text = TURN_ON
                mState = STATE_TURN_OFF
                setState()
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


    /**
     * Return a List of [ScanFilter] objects to filter by Service UUID.
     */
    private fun buildScanFilters(): List<ScanFilter> {
        val builder = ScanFilter.Builder()
        // Comment out the below line to see all BLE devices around you
        // UUID LÀ UUID CỦA RIÊNG APP NÀY, chỉ có 2 thiết bị cùng UUID (tức là sử dụng ứng dụng này) thì mới tìm được nhau.
        builder.setServiceUuid(ParcelUuid(SERVICE_UUID))
        val filter = builder.build()
        return listOf(filter)
    }

    /**
     * Return a [ScanSettings] object set to use low power (to preserve battery life).
     */
    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun findDevice() {
        locationPermissionCheck()
        listDevice.clear()
        listDeviceAdapter.notifyDataSetChanged()
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
                runOnUiThread {
                    pb_connecting.visibility = View.GONE
                }
            }, SCAN_PERIOD)
            scanning = true
            runOnUiThread {
                pb_connecting.visibility = View.VISIBLE
            }
            // Tìm chính xác đến thiết bị có cùng UUID
            bluetoothLeScanner?.startScan(buildScanFilters(), buildScanSettings(), leScanCallback)
        } else {
            scanning = false
            runOnUiThread {
                pb_connecting.visibility = View.GONE
            }
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

//    private fun broadcastUpdate(action: String) {
//        val intent = Intent(action)
//        sendBroadcast(intent)
//    }

//    private val bluetoothGattCallback = object : BluetoothGattCallback() {
//        @SuppressLint("MissingPermission")
//        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
////            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                val isSuccess = status == BluetoothGatt.GATT_SUCCESS
//                val isConnected = newState == BluetoothProfile.STATE_CONNECTED
//                Log.d(TAG, "onConnectionStateChange: Client $gatt  success: $isSuccess connected: $isConnected")
////
//                // successfully connected to the GATT Server
//                broadcastUpdate(ACTION_GATT_CONNECTED)
////                connectionState = STATE_CONNECTED
//                // Attempts to discover services after successful connection.
//                bluetoothGatt?.discoverServices()
//            if (isSuccess && isConnected) {
//                runOnUiThread {
//                    mState = STATE_CONNECTED
//                    setState()
//                    // Stuff that updates the UI
//                }
//            } else {
//                                runOnUiThread {
//                    mState = STATE_TURN_ON
//                    setState()
//                }
//            }
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

//                broadcastUpdate(ACTION_GATT_DISCONNECTED)
////                connectionState = STATE_DISCONNECTED
//                runOnUiThread {
//                    mState = STATE_TURN_ON
//                    setState()
//                }
    // disconnected from the GATT Server
    // disconnected from the GATT Server
//            }
//        }
//        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
////                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
//            } else {
//                Log.w(TAG, "onServicesDiscovered received: $status")
//            }
//        }
//        override fun onCharacteristicRead(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            status: Int
//        ) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
//            }
//        }
//        override fun onCharacteristicChanged(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic
//        ) {
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
//        }
//    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, result.toString())
            listDevice.add(result.device)
            listDeviceAdapter.notifyDataSetChanged()
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_GATT_CONNECTED -> {
//                    connected = true
//                    updateConnectionState(R.string.connected)
                    //Thay đổi trạng thái thành kết nối
                }
                ACTION_GATT_DISCONNECTED -> {
//                    connected = false
//                    updateConnectionState(R.string.disconnected)
                    //Thay đổi trạng thánh thành không kết nối.
                }
            }
        }
    }

//    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
//        val intent = Intent(action)
//
//        // This is special handling for the Heart Rate Measurement profile. Data
//        // parsing is carried out as per profile specifications.
//        when (characteristic.uuid) {
//            UUID_HEART_RATE_MEASUREMENT -> {
//                val flag = characteristic.properties
//                val format = when (flag and 0x01) {
//                    0x01 -> {
//                        Log.d(TAG, "Heart rate format UINT16.")
//                        BluetoothGattCharacteristic.FORMAT_UINT16
//                    }
//                    else -> {
//                        Log.d(TAG, "Heart rate format UINT8.")
//                        BluetoothGattCharacteristic.FORMAT_UINT8
//                    }
//                }
//                val heartRate = characteristic.getIntValue(format, 1)
//                Log.d(TAG, String.format("Received heart rate: %d", heartRate))
//                intent.putExtra(EXTRA_DATA, (heartRate).toString())
//            }
//            else -> {
//                // For all other profiles, writes the data formatted in HEX.
//                val data: ByteArray? = characteristic.value
//                if (data?.isNotEmpty() == true) {
//                    val hexString: String = data.joinToString(separator = " ") {
//                        String.format("%02X", it)
//                    }
//                    intent.putExtra(EXTRA_DATA, "$data\n$hexString")
//                }
//            }
//        }
//        sendBroadcast(intent)
//    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
//        if (bluetoothService != null) {
            // dùng để tái kết nối sau khi quay lại .
//            val result = bluetoothService!!.connect(deviceAddress)
//            Log.d(TAG, "Connect request result=$result")
//        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(ACTION_GATT_CONNECTED)
            addAction(ACTION_GATT_DISCONNECTED)
        }
    }


    // Code to manage Service lifecycle.
//    private val serviceConnection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(
//            componentName: ComponentName,
//            service: IBinder
//        ) {
//            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
//            bluetoothService?.let { bluetooth ->
//                if (!bluetooth.initialize()) {
//                    Log.e(TAG, "Unable to initialize Bluetooth")
//                    finish()
//                }
////                bluetooth.connect(deviceAddress)
//                // call functions on service to check connection and connect to devices
//            }
//        }
//
//        override fun onServiceDisconnected(componentName: ComponentName) {
//            bluetoothService = null
//        }
//    }


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
    private val IMAGE_PICK_CODE = 13
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(
            intent,
            IMAGE_PICK_CODE
        )
    }

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
    private var PERMISSION_CODE_READ = 14
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

    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView on the UI.
//    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
//        if (gattServices == null) return
//        var uuid: String?
//        val unknownServiceString: String = resources.getString(R.string.unknown_service)
//        val unknownCharaString: String = resources.getString(R.string.unknown_characteristic)
//        val gattServiceData: MutableList<HashMap<String, String>> = mutableListOf()
//        val gattCharacteristicData: MutableList<ArrayList<HashMap<String, String>>> =
//            mutableListOf()
//        mGattCharacteristics = mutableListOf()

    // Loops through available GATT Services.
//        gattServices.forEach { gattService ->
//            val currentServiceData = HashMap<String, String>()
//            uuid = gattService.uuid.toString()
//            currentServiceData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownServiceString)
//            currentServiceData[LIST_UUID] = uuid
//            gattServiceData += currentServiceData
//
//            val gattCharacteristicGroupData: ArrayList<HashMap<String, String>> = arrayListOf()
//            val gattCharacteristics = gattService.characteristics
//            val charas: MutableList<BluetoothGattCharacteristic> = mutableListOf()
//
//            // Loops through available Characteristics.
//            gattCharacteristics.forEach { gattCharacteristic ->
//                charas += gattCharacteristic
//                val currentCharaData: HashMap<String, String> = hashMapOf()
//                uuid = gattCharacteristic.uuid.toString()
//                currentCharaData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownCharaString)
//                currentCharaData[LIST_UUID] = uuid
//                gattCharacteristicGroupData += currentCharaData
//            }
//            mGattCharacteristics += charas
//            gattCharacteristicData += gattCharacteristicGroupData
//        }
//    }

//    @SuppressLint("MissingPermission")
//    fun setCharacteristicNotification(
//        characteristic: BluetoothGattCharacteristic,
//        enabled: Boolean
//    ) {
//        bluetoothGatt?.let { gatt ->
//            gatt.setCharacteristicNotification(characteristic, enabled)
//
//            // This is specific to Heart Rate Measurement.
//            if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
//                val descriptor =
//                    characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//            }
//        } ?: run {
//            Log.w(TAG, "BluetoothGatt not initialized")
//        }
//    }


}


