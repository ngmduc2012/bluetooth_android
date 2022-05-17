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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
//import com.example.bluetooth_android.JSONData.Companion.fromJsonData
//import com.example.bluetooth_android.JsonPacket.Companion.fromJsonPacket
//import com.example.bluetooth_android.JsonResult.Companion.fromJsonResult
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*


class MainActivity : AppCompatActivity() {

    /**
     * Using for the permission initialization: Enable bluetooth, discoverable bluetooth, location
     * (using for discover other device that opened discoverable bluetooth)
     * ------------------------------------------------------------------------------------------------
     * Dùng để khởi tạo các quyền truy cập: Bật bluetooth, hiển thị bluetooth với các thiết bị,
     * khởi tạo vị trí (sử dụng để tìm kiếm thiết bị đang bật bluetooth)
     */
    private val REQUEST_ENABLE_BT = 0
    private val REQUEST_DISCOVERABLE_BT = 1
    private val LOCATION_REQUEST = 2

    /**
     * Using for the status initialization: Read message, Write message, show toast
     * ------------------------------------------------------------------------------------------------
     * Dùng để khởi tạo các trạng thái: Đọc thông tin nhận, và viết ra thông tin cần gửi, hiển thị
     * thông báo toast
     */
    private val MESSAGE_READ = 3
    private val MESSAGE_WRITE = 4
    private val MESSAGE_TOAST = 5
    val TOAST = "toast"

    /**
     * Declare status: Accept, Connect, Connected
     * ---------------------------------------------------------------------------------------------
     * Khai báo trạng thái: Cho phép truy cập, Kết nối và đã kết nối.
     */
    private var mSecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null


    /**
     * Using for the initialization that is type of bluetooth connection: This is SECURE
     * ---------------------------------------------------------------------------------------------
     * Dùng để khởi tạo loại bluetooth sẽ kết nối: Loại bluetooth được sử dụng là dạng bảo mật.
     */
    private val NAME_SECURE = "BluetoothChatSecure"
    private val NAME_INSECURE = "BluetoothChatInsecure"

    /**
     * Random UUID is born, UUID is as same as the port in http
     * ---------------------------------------------------------------------------------------------
     * UUID được sinh ngẫu nhiên, UUID là đối số giống như port trong http
     */
    private val MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

    private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    // Khai báo bluetooth adapter (Kết nối với bluetooth phần cứng).
    /**
     * Declare bluetooth adapter (Connect with the hardware)
     * ---------------------------------------------------------------------------------------------
     * Khai báo bluetooth adapter (Kết nối với bluetooth phần cứng).
     */
    val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    //late init view, the following: https://stackoverflow.com/a/44285813/10621168
    private lateinit var btn_turn_on: Button
    private lateinit var btn_visible: Button
    private lateinit var btn_accept: Button
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
    private lateinit var cb_secure: CheckBox

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
        btn_image = findViewById(R.id.btn_image)

        list_device = findViewById(R.id.list_device)
        tv_name = findViewById(R.id.tv_name)
        tv_data = findViewById(R.id.tv_data)
        iv_data = findViewById(R.id.iv_data)
        et_data = findViewById(R.id.et_data)
        cb_secure = findViewById(R.id.cb_secure)

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
        btn_send.setOnClickListener { sendStartData() }
        //get image from gallery, Lấy hình ảnh trong thư viện ảnh, the following: https://stackoverflow.com/a/55933725/10621168
        btn_image.setOnClickListener { checkPermissionForImage() }

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
     * (4). If [STATE_VISIBLE] - Discoverable state (other device is able to discover by bluetooth)
     * show button ACCEPT (function: allow other device connects)
     * (5). If [STATE_ACCEPT] - Allow accept state: Allow other device connect. No show more
     * (6). If [STATE_CONNECTED] - Connected state: Show frame chat and button DISCONNECT (function:
     * disconnect with
     * ------------------------------------------------------------------------------------------------
     * Mô tả:
     * (1). Khi [STATE_NONE] - thiết bị không có bluetooth, sẽ chỉ hiển thị ra 1 dòng thông báo là thiết bị
     *      không có bluetooth
     * (2). Khi [STATE_TURN_OFF] - thiết bị đang tắt bluetooth, sẽ chỉ hiển thị nút bật bluetooth
     * (3). Khi [STATE_TURN_ON] - thiết bị đã bật bluetooth, sẽ hiển thị nút tìm kiếm thiết bị và danh sách
     *      thiết bị tìm kiếm, hiển thị nút VISIBLE (cho phép hiển thị với các thiết bị đang bật bluetooth)
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
        when (mState) {
            STATE_NONE -> {
                cb_secure.isChecked = security
                cb_secure.visibility = View.GONE
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
                iv_data.visibility = View.GONE
                btn_image.visibility = View.GONE
            }
            /**(2)*/
            STATE_TURN_OFF -> {
                cb_secure.isChecked = security
                cb_secure.visibility = View.GONE
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
                iv_data.visibility = View.GONE
                btn_image.visibility = View.GONE
            }
            /**(3)*/
            STATE_TURN_ON -> {
                cb_secure.isChecked = security
                cb_secure.visibility = View.VISIBLE
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
                iv_data.visibility = View.GONE
                btn_image.visibility = View.GONE

                btn_visible.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            }
            /**(4)*/
            STATE_VISIBLE -> {
                cb_secure.isChecked = security
                cb_secure.visibility = View.GONE
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
                iv_data.visibility = View.GONE
                btn_image.visibility = View.GONE

                btn_visible.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                btn_accept.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            }
            /**(5)*/
            STATE_ACCEPT -> {
                cb_secure.isChecked = security
                cb_secure.visibility = View.GONE
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
                iv_data.visibility = View.GONE
                btn_image.visibility = View.GONE

                btn_visible.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                btn_accept.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            }
            /**(6)*/
            STATE_CONNECTED -> {
                cb_secure.isChecked = security
                cb_secure.visibility = View.GONE
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
                iv_data.visibility = View.VISIBLE
                btn_image.visibility = View.VISIBLE

                btn_visible.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                btn_accept.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            }
        }
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
            disconnect()
            mBluetoothAdapter.disable()
            btn_turn_on.text = TURN_ON
            mState = STATE_TURN_OFF
            setState()
        }
    }

    var security : Boolean = false
    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val checked: Boolean = view.isChecked
            security = checked
//            when (view.id) {
//                R.id.cb_secure -> {
//            if (checked) {
//                // Put some meat on the sandwich
//            } else {
//                // Remove the meat
//            }
//                }
//            }
        }
    }

    /**
     * ################################################################################################
     * FUNCTION   : Discoverable - Other device is able to see this device
     * DESCRIPTION:
     *
     * Show dialog allow, if ok, change UI to [STATE_VISIBLE] state, else change UI to [STATE_TURN_ON]
     * ------------------------------------------------------------------------------------------------
     * CHỨC NĂNG: Cho phép hiển thị với các thiết bị khác.
     * MÔ TẢ    :
     *
     * Hiển thị thông báo cho phép hiển thị, nếu đồng ý thì chuyển sang trạng thái [STATE_VISIBLE]
     * nếu không đồng ý thì chuyển sang trạng thái [STATE_TURN_ON]
     * ################################################################################################
     */
    @SuppressLint("MissingPermission")
    fun visibleDevice() {
        if (!mBluetoothAdapter!!.isDiscovering) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            startActivityForResult(enableBtIntent, REQUEST_DISCOVERABLE_BT)
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

        /** The result for [visibleDevice]*/
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

        /** The result for [pickImageFromGallery] */
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            iv_data.setImageURI(data?.data)
            val resolver = applicationContext.contentResolver
            resolver.openInputStream(data?.data!!).use { stream ->
                encodedImage = Base64.getEncoder().encodeToString(stream!!.readBytes())
                Log.d("duc", "encodedImage: $encodedImage")
            }

        }

    }

    /**
     * ################################################################################################
     * FUNCTION   : Allow other device is able to connect
     * DESCRIPTION:
     *
     * Start [mSecureAcceptThread], allow connect form other device and change to [STATE_ACCEPT] state
     * ------------------------------------------------------------------------------------------------
     * CHỨC NĂNG: Cho phép các thiết bị khác kết nối.
     * MÔ TẢ    :
     *
     * Bật luồng cho phép các thiết bị khác kết nối dưới dạng bảo mật và chuyển trạng thái thành
     * [STATE_ACCEPT]
     * ################################################################################################
     */
    private fun acceptConnect() {
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = AcceptThread(true)
            mSecureAcceptThread!!.start()

            mState = STATE_ACCEPT
            setState()
        }
    }

    /**
     * ################################################################################################
     * FUNCTION   : Find other device that is turning on bluetooth.
     * DESCRIPTION:
     *
     * (1) Status of finding all device nearby:
     *  *  Open location permission (Discover other device)
     *  *  If in discovering state, using [receiver] for finding, result devices is saved in [listDevice]
     *  (Note: [receiver] takes a lot of the Bluetooth adapter's resources, turn off if necessary
     * (2) Found paired devices and show in list
     * ------------------------------------------------------------------------------------------------
     * CHỨC NĂNG: Tìm kiếm các thiết bị bật bluetooth xung quanh hoặc đã từng kết nối.
     * MÔ TẢ    :
     *
     * (1) Trạng thái tìm kiếm tất cả các thiết bị gần đấy:
     *  *  Bật quyền truy cập vị trí (dùng để tìm các thết bị khác)
     *  *  Nếu đang ở trạng thái hiển thị với các thiết bị khác thì bắt đầu tìm kiếm bằng biển
     *  [receiver], kết quả được lưu vào [listDevice]. (Chú ý: [receiver] tốn tài nguyên, tắt khi không
     *  cần thiết. ([onDestroy])
     * (2) Tìm kiếm thiết bị đã từng kết nối và hiển thị ra màng hình ([pairedDevices])
     * ################################################################################################
     */
    private val FIND_ALL = "FIND ALL"
    private val FIND_PAIRED = "FIND PAIRED"

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    fun findDevice() {
        /** (1) */
        if (btn_find.text == FIND_ALL) {
            btn_find.text = FIND_PAIRED
            locationPermissionCheck()
            if (mBluetoothAdapter!!.isEnabled && !mBluetoothAdapter.isDiscovering) {
                // Register for broadcasts when a device is discovered.
                listDevice.clear()
                //Tham khảo/ The following: https://developer.android.com/guide/topics/connectivity/bluetooth/find-bluetooth-devices
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(receiver, filter)
                mBluetoothAdapter.startDiscovery()
            }

        }
        /** (2) */
        else {
            btn_find.text = FIND_ALL
            listDevice.clear()

            val pairedDevices: Set<BluetoothDevice>? = mBluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                listDevice.add(device)
            }
            listDeviceAdapter.notifyDataSetChanged()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission", "NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
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


//    /**
//     * ################################################################################################
//     * FUNCTION   : Handle data before send message.
//     * DESCRIPTION:
//     *
//     * (1) [content] The content of message: Get data in [et_data] and image (Base64String) in
//     * [encodedImage] into [JSONData] type, then change to String type and save in [content]
//     *  *  [number] the number of items will split and setup in a packet [JsonPacket], ~700 is the
//     * number that makes less mistakes.
//     *  *  [id] is the numbered packet order, start 0, Example: There are 10 packets ([sum]), [id]
//     * start from 0..9
//     * (2) [sum] is number of packets, = number of items in [content] / number of items in a packet
//     * [number], if there is a residual, add more 1 packet.
//     * (3) [sendData] Send message - FOR SENDER: data is split at [content] from [id] * [number] to
//     * ([id] + 1) * [number] or the end of [content], data will be packed into the packet [JsonPacket]
//     * include: [sum] number of packets, [id] the numbered packet order, the data that just split, (4)
//     * hash of data that just split (by MD5)
//     * (5) [sendResult] send result - FOR RECIPIENT: After getting data from SENDER, RECIPIENT will verify
//     * hash and add data [result], if success, return true, else, return false. Form result message
//     * [JsonResult] type, include: [id] the order of packet and result true or false.
//     * ------------------------------------------------------------------------------------------------
//     * CHỨC NĂNG: Xử lý dữ liệu cần truyền
//     * MÔ TẢ    :
//     *
//     * (1) [content] nội dung dữ liệu cần truyền: Lấy dữ liệu trong [et_data] và hình ảnh dạng base64String
//     * [encodedImage] thành json dạng [JSONData] sau đó chuyển thành String và lưu vào [content]
//     *  *  [number] số lượng phần tử trong [content] sẽ được cắt ra để truyền vào 1 gói tin dạng
//     * [JsonPacket], ~700 phẩn tử là số lượng truyền khó bị sảy ra sai sót.
//     *  *  [id] là thứ tự gói tin được đánh số bắt đầu từ 0, nếu có 10 gói tin ([sum]) thì [id] sẽ
//     *  chạy từ 0..9
//     * (2) [sum] là tổng số gói tin sẽ được truyền, = độ dài của [content] / số lượng phần tử trong 1 gói
//     * [number], nếu dư thì sẽ thêm 1 gói tin nữa.
//     * (3) [sendData] gửi dữ liệu đi - DÀNH CHO BÊN GỬI: dữ liệu sẽ được cắt từ [content] tại vị trí [id] * [number]
//     * đến vị trí ([id] + 1) * [number] hoặc vị trí cuối cùng của [content], dữ liệu sẽ được đóng gói
//     * vào gói tin [JsonPacket] bao gồm: [sum] tổng số gói tin sẽ truyền, [id] số thứ tự của gói tin
//     * đang được gửi đi, dữ liệu vừa cắt, (4) hàm băm MD5 của dữ liệu vừa được cắt.
//     * (5) [sendResult] gửi về kết quả - DÀNH CHO BÊN NHẬN: Sau khi nhận dữ liệu từ bên GỬI, bên NHẬN sẽ
//     * xác thực hàm băm và thêm dữ liệu vào [result], nếu thành công thì sẽ trả về kết quả thành công hoặc
//     * thất bại dưới dạng [JsonResult] gồm: [id] thứ tự gói gửi đi và kết quả true hoặc false
//     * ################################################################################################
//     */
//    var content: String? = null
//    var sum: Int = 0
//    var id = 0
//    var number = 700
//    var result: String = ""
    private fun sendStartData() {

        if (mConnectedThread != null) {
//            /** (1) */
//            val jsonData = JSONData("${et_data.text}", "$encodedImage")
//            content = jsonData.toJsonData()
//            id = 0
//            /** (2) */
//            sum = if (content!!.length % number > 0) {
//                (content!!.length / number + 1)
//            } else {
//                content!!.length / number
//            }
//            /** (3) */
//            sendData()
            mConnectedThread!!.write("${et_data.text}".toByteArray())


        } else {
            runOnUiThread {
                tv_name.visibility = View.VISIBLE
                tv_name.text = "There is no connected thread"
            }
//                Log.d("duc", "mConnectedThread == $mConnectedThread")
        }
    }

//    fun sendData() {
//        val data = content!!.substring(
//            id * number,
//            if (content!!.length < (id + 1) * number) content!!.length else (id + 1) * number
//        )
//        val jsonPacket = JsonPacket(
//            sum,
//            id,
//            data,
//            /** (4) */
//            md5(data)
//        )
//        mConnectedThread!!.write(jsonPacket.toJsonPacket().toByteArray())
//    }

//    // hash MD5, the following: https://stackoverflow.com/a/64171625/10621168
//    fun md5(input: String): String {
//        val md = MessageDigest.getInstance("MD5")
//        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
//    }

//    /** (5) */
//    fun sendResult(id: Int, result: Boolean) {
//        val jsonResult = JsonResult(id, result)
//        mConnectedThread!!.write(jsonResult.toJsonResult().toByteArray())
//    }


    /**
     * ################################################################################################
     * FUNCTION   : Disconnect with connected device.
     * DESCRIPTION:
     *
     * Close all connect thread (connect, accept, connected) then change to [STATE_VISIBLE] status,
     * setup value for sent message to default.
     * ------------------------------------------------------------------------------------------------
     * CHỨC NĂNG: Ngắt kết nối với thiết bị đang được kết nối.
     * MÔ TẢ    :
     *
     * Đóng các luồng kết nối và chuyển về trạng thái [STATE_VISIBLE], các biến để truyền dữ liệu đi sẽ
     * được đưa về mặc định.
     * ################################################################################################
     */
    private fun disconnect() {
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
//        id = 0
//        sum = 0
//        content = null
//        result = ""
        encodedImage = ""
        security = false

        mState = STATE_VISIBLE
        runOnUiThread {
            setState()
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

    // Connect bluetooth, the following: https://developer.android.com/guide/topics/connectivity/bluetooth/connect-bluetooth-devices
    @SuppressLint("MissingPermission")
    private inner class AcceptThread(b: Boolean) : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            if(security){
                mBluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE)
            } else {
                mBluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                    NAME_INSECURE, MY_UUID_INSECURE
                )
            }

        }

        @SuppressLint("SetTextI18n")
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
        @SuppressLint("SetTextI18n")
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
            if(security){
                device.createRfcommSocketToServiceRecord(MY_UUID_SECURE)
            } else {
                device.createInsecureRfcommSocketToServiceRecord(
                    MY_UUID_INSECURE
                )
            }
        }

        @SuppressLint("SetTextI18n")
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


    // Transfer Bluetooth data, the following: https://developer.android.com/guide/topics/connectivity/bluetooth/transfer-data
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        @SuppressLint("SetTextI18n")
        override fun run() {
            var numBytes: Int // bytes returned from read()

            while (true) {
                // Read from the InputStream.
                try {
                    Log.d("duc", "mmInStream: ${mmInStream}")
                    numBytes = mmInStream.read(mmBuffer)
                    mmInStream.available()
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
    private val handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        @SuppressLint("NewApi", "SetTextI18n")
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
                }
                /**
                 * ################################################################################################
                 * FUNCTION   : Handle message.
                 * DESCRIPTION:
                 *
                 * Overview:
                 * Because transmission the large data by bluetooth make a lot of mistakes, have to split into
                 * small packets.
                 * SENDER: Sending message by [JSONData] type, after change to String type, the data will be split
                 * into packets ([JsonPacket] type), Be numbered and send at [sendStartData]
                 * (1) RECIPIENT: After get data that is [JsonPacket] type, comparison data with hash (MD5), then
                 * add data to [result] and send the result message [JsonResult]
                 * (2) SENDER: Get the result message [JsonResult], if the result is success (true). Send next
                 * packet with [id]++, else resend this packet.
                 * (3) RECIPIENT: After get all packets ([id] +1 = [sum]), change [result] from String type to
                 * [JSONData] read and show on UI.
                 * ------------------------------------------------------------------------------------------------
                 * CHỨC NĂNG: Xử lý dữ liệu cần truyền
                 * MÔ TẢ    :
                 *
                 * Tổng quan:
                 * Bởi vì khi truyền dữ liệu lớn bằng bluetooth sẽ gây ra sai sót, cần chia nhỏ thành các gói nhỏ.
                 * BÊN GỬI: dữ liệu sẽ được tạo dưới dạng [JSONData] sau đó được chuyển thành String, dữ liệu sẽ
                 * được cắt ra thành các gói dạng [JsonPacket], được đánh số và gửi đi. (tại [sendStartData])
                 * (1) BÊN NHẬN: sau khi nhận được dữ liệu dạng [JsonPacket], sẽ tiến hành so sáng với hàm băm, sau đó
                 * ghép thêm kết quả vào [result] và trả về thông điện kết quả dạng [JsonResult]
                 * (2) BÊN GỬI: Nhận thông điệp kết quả [JsonResult], nếu gói đó truyền thành công thì sẽ truyền
                 * tiếp gói tin tiếp theo [id]++, nếu không truyền lại gói tin vừa gửi.
                 * (3) BÊN NHẬN: Sau khi nhận hết gói tin ([id] +1 = [sum]) thì sẽ chuyển dự liệu trong [result] thành
                 * [JSONData] và hiển thị ảnh và dữ liệu ra ngoài màn hình.
                 * ################################################################################################
                 */
                MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer
                    val readMessage = String(readBuf, 0, msg.arg1)
                    Log.d("duc", "readMessage: $readMessage")

                    runOnUiThread {
                        tv_data.text = readMessage
                    }
                    /**
                     * Dành cho BÊN NHẬN
                     * -----------------
                     * For RECIPIENT
                     * */
//                    try {
//
//                        val jsonPacket: JsonPacket? = fromJsonPacket(readMessage)
//                        Log.d("duc", "jsonPacket: ${jsonPacket!!.content}")
//                        Log.d("duc", "jsonPacket: ${jsonPacket.hash}")
//                        Log.d("duc", "jsonPacket: ${jsonPacket.id}")
//                        Log.d("duc", "jsonPacket: ${jsonPacket.sum}")
//                        /** (1) */
//                        if (md5(jsonPacket.content) == jsonPacket.hash) {
//                            sendResult(jsonPacket.id, true)
//                            if (jsonPacket.id + 1 < jsonPacket.sum) {
//                                // Hiển thị % dữ liệu truyền được. - Show percent loading.
//                                // The following: https://www.baeldung.com/java-calculate-percentage
//                                result += jsonPacket.content
//                                val percent: Double =
//                                    (jsonPacket.id.toDouble() / jsonPacket.sum.toDouble()) * 100
//                                runOnUiThread {
//                                    tv_data.text = "${percent}%"
//                                }
//                            } else {
//                                result += jsonPacket.content
//                                try {
//                                    /** (3) */
//                                    val jsonData: JSONData? = fromJsonData(result)
//                                    Log.d("duc", "jsonData: ${jsonData!!.mes}")
//                                    Log.d("duc", "jsonData: ${jsonData.image}")
//
//                                    if (jsonData.image.length > 1) {
//                                        val decodedBytes =
//                                            Base64.getDecoder().decode(jsonData.image)
//                                        val matrix = Matrix()
//
//                                        // Xoay hình ảnh - Rotate image
//                                        // The following: https://helpex.vn/question/android-xoay-hinh-anh-trong-che-do-xem-anh-theo-mot-goc-6094e25af45eca37f4c0d40a
////                                        matrix.postRotate(90F)
//                                        val image: Bitmap =
//                                            BitmapFactory.decodeByteArray(
//                                                decodedBytes,
//                                                0,
//                                                decodedBytes.size,
//                                            )
//
////                                        val rotated = Bitmap.createBitmap(
////                                            image, 0, 0, image.getWidth(), image.getHeight(),
////                                            matrix, true
////                                        )
//                                        runOnUiThread {
//                                            tv_data.text = jsonData.mes
//                                            iv_data.setImageBitmap(image)
//                                        }
//                                    } else {
//                                        runOnUiThread {
//                                            tv_data.text = jsonData.mes
//                                        }
//                                    }
//
//                                } catch (e: Exception) {
//                                    Log.d("duc", "jsonData no")
//                                    runOnUiThread {
//                                        tv_data.text = "Không thành công!"
//                                    }
//
//                                }
//                                //After read [result], set [result] empty
//                                //Sau khi đọc xong hết dữ liệu thì result sẽ trở về rỗng.
////                                result = ""
//                            }
//
//                        } else {
////                            sendResult(jsonPacket.id, false)
//                        }
//
//
//                    } catch (e: Exception) {
//
//                        /**
//                         * Dành cho BÊN GỬI
//                         * ----------------
//                         * For SENDER
//                         * */
//                        try {
//
//                            val jsonResult: JsonResult? = fromJsonResult(readMessage)
//                            Log.d("duc", "jsonResult: ${jsonResult!!.id}")
//                            Log.d("duc", "jsonResult: ${jsonResult.result}")
//                            /** (2) */
//                            if (jsonResult.result) {
//                                id++
//                                if (id < sum) {
//                                    sendData()
//                                } else {
//                                    id = 0
//                                    sum = 0
//                                    content = null
//                                    result = ""
//                                }
//                            } else {
//                                sendData()
//                            }
//
//                        } catch (e: Exception) {
//                            Log.d("duc", "jsonData no")
//                            runOnUiThread {
//                                tv_name.visibility = View.VISIBLE
//                                tv_name.text = "$e"
//                            }
//                        }
//                    }
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


