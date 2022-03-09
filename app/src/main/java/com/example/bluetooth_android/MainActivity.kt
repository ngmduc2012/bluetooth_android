package com.example.bluetooth_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 0
    private val REQUEST_DISCOVERABLE_BT = 1
    private val BLUETOOTH_REQUEST = 2
    private val LOCATION_REQUEST = 3
    private val MESSAGE_READ = 4
    private val MESSAGE_WRITE = 5
    private val MESSAGE_TOAST = 6
    var TOAST = "toast"

    private var mSecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    private val NAME_SECURE = "BluetoothChatSecure"

    // Random UUID is born
    private val MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

    // Khai báo bluetooth adapter (Kết nối với bluetooth phần cứng.
    val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    //late init view, the following: https://stackoverflow.com/a/44285813/10621168
    private lateinit var btn_turn_on: Button
    private lateinit var btn_visible: Button
    private lateinit var btn_accept: Button
    private lateinit var btn_find: Button
    private lateinit var btn_disconnect: Button
    private lateinit var btn_send: Button
    private lateinit var tv_no_blue: TextView
    private lateinit var tv_name: TextView
    private lateinit var tv_data: TextView
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
        btn_visible = findViewById(R.id.btn_visible)
        btn_accept = findViewById(R.id.btn_accept)
        btn_find = findViewById(R.id.btn_find)
        btn_disconnect = findViewById(R.id.btn_disconnect)
        btn_send = findViewById(R.id.btn_send)

        list_device = findViewById(R.id.list_device)
        tv_name = findViewById(R.id.tv_name)
        tv_data = findViewById(R.id.tv_data)
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
            mConnectThread = ConnectThread(device)
            mConnectThread!!.start()
        }

        onInitState()

        //click on button in kotlin, the following: https://stackoverflow.com/a/64187408/10621168
        btn_turn_on.setOnClickListener { onOffBluetooth() }
        btn_visible.setOnClickListener { visibleDevice() }
        btn_accept.setOnClickListener { acceptConnect() }
        btn_find.setOnClickListener { findDevice() }
        btn_disconnect.setOnClickListener { disconnect() }
        btn_send.setOnClickListener { sendData() }

    }


    fun onInitState() {

        // Kiển tra xem thiết bị có hỗ trợ bluetooth không.
        if (mBluetoothAdapter == null) {
            tv_no_blue.append("This device not supported")
            mState = STATE_NONE
        } else {
            //if bluetooth is compatible, ask the user to
            //enable bluetooth without leaving the app itself
            mState = STATE_TURN_OFF

            if (!mBluetoothAdapter.isEnabled) {
                btn_turn_on.text = "TURN ON"
//                val btEnableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                startActivityForResult(btEnableIntent, BLUETOOTH_REQUEST)

                //call location permission access
//                locationPermissionCheck()

            } else {
                btn_turn_on.text = "TURN OFF"
                mState = STATE_TURN_ON

            }
        }
        setState()
    }

    /**
     * ################################################################################################
     * Title: Setup status for view
     * Description:
     * ------------------------------------------------------------------------------------------------
     * Mô tả:
     * (1). Khi [STATE_NONE] - thiết bị không có bluetooth, sẽ chỉ hiển thị ra 1 dòng thông báo là thiết bị
     *      không có bluetooth
     * (2). Khi [STATE_TURN_OFF] - thiết bị đang tắt bluetooth, sẽ chỉ hiển thị nút bật bluetooth
     * (3). Khi [STATE_TURN_ON] - thiết bị đã bật bluetooth, sẽ hiển thị nút tìm kiếm thiết bị và danh sách
     *      thiết bị tìm kiếm, hiển thị nút VISIBLE (cho phép hiển thị với các thiết bị đang bật bluetooth
     * (4). Khi [STATE_VISIBLE] - thiết bị đã cho phép các thiết bị khác tìm thấy mình bằng bluetooth. Giao
     *      diện sẽ hiển thị thêm nút ACCEPT để cho phép các thiết bị khác truy cập
     * (5). Khi [STATE_ACCEPT] - thiết bị đã cho phép các thiết bị khác truy cập. Không hiển thị thêm giao diện
     * (6). Khi [STATE_CONNECTED] - thiết bị đã kết nối với 1 thiết bị khác. Hiển thị khung chat và nút
     *      DISCONNECT để ngắt kết nối.
     *################################################################################################
     */
    val STATE_NONE = 7
    val STATE_TURN_OFF = 8
    val STATE_TURN_ON = 9
    val STATE_VISIBLE = 10
    val STATE_ACCEPT = 11
    val STATE_CONNECTED = 12
    private var mState = STATE_NONE
    fun setState() {
        /**(1)*/
        if (mState == STATE_NONE) {
            pb_connecting.visibility = View.GONE
            tv_no_blue.visibility = View.VISIBLE
            btn_turn_on.visibility = View.GONE
            btn_visible.visibility = View.GONE
            btn_accept.visibility = View.GONE
            btn_find.visibility = View.GONE
            btn_disconnect.visibility = View.GONE
            btn_send.visibility = View.GONE
            list_device.visibility = View.GONE
            tv_name.visibility = View.GONE
            tv_data.visibility = View.GONE
            et_data.visibility = View.GONE
        }
        /**(2)*/
        else if (mState == STATE_TURN_OFF) {
            pb_connecting.visibility = View.GONE
            tv_no_blue.visibility = View.GONE
            btn_turn_on.visibility = View.VISIBLE
            btn_visible.visibility = View.GONE
            btn_accept.visibility = View.GONE
            btn_find.visibility = View.GONE
            btn_disconnect.visibility = View.GONE
            btn_send.visibility = View.GONE
            list_device.visibility = View.GONE
            tv_name.visibility = View.GONE
            tv_data.visibility = View.GONE
            et_data.visibility = View.GONE
        }
        /**(3)*/
        else if (mState == STATE_TURN_ON) {
            pb_connecting.visibility = View.GONE
            btn_turn_on.visibility = View.VISIBLE
            btn_visible.visibility = View.VISIBLE
            btn_accept.visibility = View.GONE
            btn_find.visibility = View.VISIBLE
            btn_disconnect.visibility = View.GONE
            btn_send.visibility = View.GONE
            list_device.visibility = View.VISIBLE
            tv_name.visibility = View.GONE
            tv_data.visibility = View.GONE
            et_data.visibility = View.GONE
            btn_visible.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        }
        /**(4)*/
        else if (mState == STATE_VISIBLE) {
            pb_connecting.visibility = View.GONE
            btn_turn_on.visibility = View.VISIBLE
            btn_visible.visibility = View.VISIBLE
            btn_accept.visibility = View.VISIBLE
            btn_find.visibility = View.VISIBLE
            btn_disconnect.visibility = View.GONE
            btn_send.visibility = View.GONE
            list_device.visibility = View.VISIBLE
            tv_name.visibility = View.GONE
            tv_data.visibility = View.GONE
            et_data.visibility = View.GONE

            btn_visible.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            btn_accept.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        }
        /**(5)*/
        else if (mState == STATE_ACCEPT) {
            btn_turn_on.visibility = View.VISIBLE
            btn_visible.visibility = View.VISIBLE
            btn_accept.visibility = View.VISIBLE
            btn_find.visibility = View.VISIBLE
            btn_disconnect.visibility = View.GONE
            btn_send.visibility = View.GONE
            list_device.visibility = View.VISIBLE
            tv_name.visibility = View.GONE
            tv_data.visibility = View.GONE
            et_data.visibility = View.GONE

            btn_visible.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            btn_accept.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        }
        /**(6)*/
        else if (mState == STATE_CONNECTED) {
            pb_connecting.visibility = View.GONE
            btn_turn_on.visibility = View.VISIBLE
            btn_visible.visibility = View.VISIBLE
            btn_accept.visibility = View.VISIBLE
            btn_find.visibility = View.VISIBLE
            btn_disconnect.visibility = View.VISIBLE
            btn_send.visibility = View.VISIBLE
            list_device.visibility = View.VISIBLE
            tv_name.visibility = View.VISIBLE
            tv_data.visibility = View.VISIBLE
            et_data.visibility = View.VISIBLE

            btn_visible.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            btn_accept.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        }
    }

    @SuppressLint("MissingPermission")
    fun onOffBluetooth() {
        // Bật bluetooth
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            // hiển thị cấp phép quyền truy cập vào bluetooth,
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            locationPermissionCheck()
        }
        // Tắt bluetooth
        else {
            mBluetoothAdapter.disable()
            btn_turn_on.text = "TURN ON"
            mState = STATE_TURN_OFF
            setState()
        }
    }

    @SuppressLint("MissingPermission")
    fun visibleDevice() {
        // Cho phép hiển thị với các thiết bị khác.
        if (!mBluetoothAdapter!!.isDiscovering) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            startActivityForResult(enableBtIntent, REQUEST_DISCOVERABLE_BT)
        }

    }

    // Get result in dialog, the following: https://stackoverflow.com/a/10407371/10621168
    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Bật tắt bluetooth
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                btn_turn_on.text = "TURN OFF"
                mState = STATE_TURN_ON
                setState()
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                btn_turn_on.text = "TURN ON"
                mState = STATE_TURN_OFF
                setState()
            }
        }

        // Cho phép hiển thị với các thiết bị khác
        if (requestCode == REQUEST_DISCOVERABLE_BT) {
            if (resultCode == 120) {
                mState = STATE_VISIBLE
                setState()
            }
            if (resultCode == 0) {
                mState = STATE_TURN_ON
                setState()
            }
        }

    }

    fun acceptConnect() {
        // Cho phép các thiết bị khác kết nối dưới dạng truy cập bảo mật.
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = AcceptThread(true)
            mSecureAcceptThread!!.start()

            mState = STATE_ACCEPT
            setState()
        }
    }

    @SuppressLint("MissingPermission")
    fun findDevice() {
        if (btn_find.text == "FIND ALL") {
            btn_find.text = "FIND PAIRED"
            locationPermissionCheck()
            if (mBluetoothAdapter!!.isEnabled && !mBluetoothAdapter.isDiscovering) {
                // Register for broadcasts when a device is discovered.
                listDevice.clear()
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(receiver, filter)
                mBluetoothAdapter.startDiscovery()
            }

        } else {
            btn_find.text = "FIND ALL"

            // Lấy các thiết bị đã từng kết nối.
            listDevice.clear()
            val pairedDevices: Set<BluetoothDevice>? = mBluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                listDevice.add(device)
            }
            listDeviceAdapter.notifyDataSetChanged()
        }
    }


    fun sendData() {
        if (mConnectedThread != null) {

            Log.d("duc", "mConnectedThread != $mConnectedThread")

            mConnectedThread!!.write("${et_data.text}".toByteArray())
            // Start the thread to connect with the given device
            // Start the thread to manage the connection and perform transmissions
        } else {
            runOnUiThread {
                tv_name.visibility = View.VISIBLE
                tv_name.text = "There is no connected thread"
            }
//                Log.d("duc", "mConnectedThread == $mConnectedThread")
        }
    }

    fun disconnect() {
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
            Log.d("duc", "cancel mConnectThread")
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
            Log.d("duc", "cancel mSecureAcceptThread")
        }

        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
            Log.d("duc", "cancel mConnectedThread")
        }

        mState = STATE_VISIBLE
        runOnUiThread {
            setState()
        }
    }

    //function for requesting location permission
    fun locationPermissionCheck(): Boolean {
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

    //Tắt tìm kiếm nếu không dùng, vì rất tốn tài nguyên.
    private val receiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission", "NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    listDevice.add(device)
                    listDeviceAdapter.notifyDataSetChanged()
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread(b: Boolean) : Thread() {

//        private val mmServerSocket: BluetoothServerSocket? = null

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            // UUID là đối số giống như port trong http
            // Lắng nghe yêu cầu kết nối truyền đến
            mBluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE)
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    Log.d("duc", "mmServerSocket!!.accept()")
                    //Run layout in the thread, the following: https://stackoverflow.com/a/14114546/10621168
                    runOnUiThread {
                        tv_name.visibility = View.VISIBLE
                        tv_name.text = "Bắt đầu cho phép các thiết bị khác truy cập đến."
                    }
                    mmServerSocket!!.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    runOnUiThread {
                        tv_name.visibility = View.VISIBLE
                        tv_name.text = "Socket's accept() method failed"
                    }
                    null
                }
                socket?.also {
//                    manageMyConnectedSocket(it)
                    mConnectedThread = ConnectedThread(it)
                    mConnectedThread!!.start()
                    mmServerSocket!!.close()
                    shouldLoop = false

                    Log.d("duc", "mmServerSocket!!.close()")
                    // Khi kết nối thành công.
                    mState = STATE_CONNECTED
                    runOnUiThread {
                        setState()
                        tv_name.text = "Connected to ${socket.remoteDevice.name}"
                    }
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                runOnUiThread {
                    tv_name.visibility = View.VISIBLE
                    tv_name.text = "Could not close the connect socket"
                }
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID_SECURE)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter?.cancelDiscovery()
            runOnUiThread {
                pb_connecting.visibility = View.VISIBLE
            }
            try {
                mmSocket?.let { socket ->
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    socket.connect()

                    // Start the thread to manage the connection and perform transmissions
                    mConnectedThread = ConnectedThread(socket)
                    mConnectedThread!!.start()

                    mState = STATE_CONNECTED
                    runOnUiThread {
                        setState()
                        tv_name.text = "Connect to ${mmSocket!!.remoteDevice.name}"
                        pb_connecting.visibility = View.GONE
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    tv_name.visibility = View.VISIBLE
                    tv_name.text = "Can not connect ${mmSocket!!.remoteDevice.name} because $e"
                    pb_connecting.visibility = View.GONE
                }
//                Log.e("duc", "Kết nối ko thành công do $e")

                // Close the socket
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    runOnUiThread {
                        tv_name.visibility = View.VISIBLE
                        tv_name.text =
                            "unable to close() the security socket during connection failure"
                    }
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                runOnUiThread {
                    tv_name.visibility = View.VISIBLE
                    tv_name.text = "Could not close the client socket $e"
                    pb_connecting.visibility = View.GONE
                }
            }
        }
    }


    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            while (true) {
                // Read from the InputStream.
                try {
                    numBytes = mmInStream.read(mmBuffer)
                    // Send the obtained bytes to the UI activity.
                    val readMsg = handler.obtainMessage(
                        MESSAGE_READ, numBytes, -1,
                        mmBuffer
                    )
                    readMsg.sendToTarget()
                } catch (e: IOException) {
                    runOnUiThread {
                        tv_name.visibility = View.VISIBLE
                        tv_name.text = "Input stream was disconnected $e"
                    }
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
                // Share the sent message with the UI activity.
                val writtenMsg = handler.obtainMessage(
                    MESSAGE_WRITE, -1, -1, mmBuffer
                )
                writtenMsg.sendToTarget()
//                Log.d("duc", "writtenMsg: ${writtenMsg.peekData()}")
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                runOnUiThread {
                    tv_name.visibility = View.VISIBLE
                    tv_name.text = "Error occurred when sending data $e"
                }
                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                    Log.d("duc", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
//                Log.d("duc", "writeErrorMsg.data: ${writeErrorMsg.data}")
                return
            }

        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                runOnUiThread {
                    tv_name.visibility = View.VISIBLE
                    tv_name.text = "Could not close the connect socket $e"
                }
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
//            val activity: FragmentActivity = applicationContext
            when (msg.what) {
//                MESSAGE_STATE_CHANGE -> when (msg.arg1) {
//                    BluetoothChatService.STATE_CONNECTED -> {
//                        setStatus(getString(R.string.title_connected_to, mConnectedDeviceName))
//                        mConversationArrayAdapter.clear()
//                    }
//                    BluetoothChatService.STATE_CONNECTING -> setStatus(R.string.title_connecting)
//                    BluetoothChatService.STATE_LISTEN, BluetoothChatService.STATE_NONE -> setStatus(
//                        R.string.title_not_connected
//                    )
//                }
                MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    // construct a string from the buffer
                    val writeMessage = String(writeBuf)
//                    Log.d("duc", "writeMessage: $writeMessage")
//                    mConversationArrayAdapter.add("Me:  $writeMessage")
                }
                MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer
                    val readMessage = String(readBuf, 0, msg.arg1)
                    Log.d("duc", "readMessage: $readMessage")
                    runOnUiThread {
                        tv_data.text = "$readMessage"
                    }
//                    mConversationArrayAdapter.add(mConnectedDeviceName.toString() + ":  " + readMessage)
                }
//                MESSAGE_DEVICE_NAME -> {
//                    // save the connected device's name
//                    mConnectedDeviceName = msg.data.getString(DEVICE_NAME)
//                    if (null != activity) {
//                        Toast.makeText(
//                            activity, "Connected to "
//                                    + mConnectedDeviceName, Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
                MESSAGE_TOAST -> if (null != applicationContext) {
                    Toast.makeText(
                        applicationContext, msg.data.getString(TOAST),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }



}


