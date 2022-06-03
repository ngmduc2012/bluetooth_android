package com.example.bluetooth_android


/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.*


/**
 * O. FOR BOTH ##############
 */

/**
 * Using for the permission initialization: Enable bluetooth, discoverable bluetooth, location
 * (using for discover other device that opened discoverable bluetooth)
 * ------------------------------------------------------------------------------------------------
 * Dùng để khởi tạo các quyền truy cập: Bật bluetooth, hiển thị bluetooth với các thiết bị,
 * khởi tạo vị trí (sử dụng để tìm kiếm thiết bị đang bật bluetooth)
 */
const val REQUEST_ENABLE_BT = 0
const val LOCATION_REQUEST = 2
val REQUEST_DISCOVERABLE_BT = 1
val TAG = "123"




/**
 * I. BLUETOOTH LE ##############
 */
/**
 * Constants for use in the Bluetooth LE Chat sample
 * UUID identified with this app - set as Service UUID for BLE Chat.
 *
 * Bluetooth requires a certain format for UUIDs associated with Services.
 * The official specification can be found here:
 * [://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery][https]
 * ------------------------------------------------------------------------------------------------
 * UUID được định danh cùng với ứng dụng này - thiết lập tương tự như UUID của Service cho dứng dụng
 * BLE chat.
 *
 * Bluetooth yêu cầu một định dạng cố định cho các UUID được liên kết với Service.
 * Các định dạng UUID được chấp nhận rộng rãi có thể được tham khảo tại:
 * [://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery][https]
 */
val SERVICE_UUID: UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")

val SERVICE_UUID_IOS: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")

/**
 * UUID for the message
 */
val MESSAGE_UUID: UUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b")

val MESSAGE_UUID_IOS: UUID = UUID.fromString("89d3502b-0f36-433a-8ef4-c502ad55f8dc")

/**
 * UUID to confirm device connection
 */
//val CONFIRM_UUID: UUID = UUID.fromString("36d4dc5c-814b-4097-a5a6-b93b39085928")


val STATE_NONE = 7
val STATE_TURN_OFF = 8
val STATE_TURN_ON = 9
val STATE_CONNECTED = 12

val TURN_ON = "TURN ON"
val TURN_OFF = "TURN OFF"

val PERMISSION_CODE_READ = 14

val IMAGE_PICK_CODE = 13

/**
 * Stops scanning after 30 seconds.
 * ---------------------------------------------------------------------------------------------
 * Tự động dừng tìm kiếm sau 30 giây
 */
val SCAN_PERIOD: Long = 30000L


const val ACTION_GATT_CONNECTED =
    "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED =
    "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"


/**
 * II. BLUETOOTH CLASS ##############
 */

/**
 * Type of bluetooth
 * ---------------------------------------------------------------------------------------------
 * thể loại của bluetooth
 */
val DEVICE_TYPE_CLASSIC = 1
val DEVICE_TYPE_DUAL = 3
val DEVICE_TYPE_LE = 2
 val DEVICE_TYPE_UNKNOWN = 0
val NO_DEVICE = 4

/**
 * Using for the initialization that is type of bluetooth connection: This is SECURE
 * ---------------------------------------------------------------------------------------------
 * Dùng để khởi tạo loại bluetooth sẽ kết nối: Loại bluetooth được sử dụng là dạng bảo mật.
 */
val NAME_SECURE = "BluetoothChatSecure"
val NAME_INSECURE = "BluetoothChatInsecure"

/**
 * Random UUID is born, UUID is as same as the port in http
 * ---------------------------------------------------------------------------------------------
 * UUID được sinh ngẫu nhiên, UUID là đối số giống như port trong http
 */
val MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")


/**
 * Using for the status initialization: Read message, Write message, show toast
 * ------------------------------------------------------------------------------------------------
 * Dùng để khởi tạo các trạng thái: Đọc thông tin nhận, và viết ra thông tin cần gửi, hiển thị
 * thông báo toast
 */
val MESSAGE_READ = 3
val MESSAGE_WRITE = 4
val MESSAGE_TOAST = 5
val TOAST = "toast"
