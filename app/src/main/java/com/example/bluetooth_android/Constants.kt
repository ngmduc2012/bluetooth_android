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

package com.example.bluetooth_android

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import java.util.*



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

/**
 * Using for the permission initialization: Enable bluetooth, discoverable bluetooth, location
 * (using for discover other device that opened discoverable bluetooth)
 * ------------------------------------------------------------------------------------------------
 * Dùng để khởi tạo các quyền truy cập: Bật bluetooth, hiển thị bluetooth với các thiết bị,
 * khởi tạo vị trí (sử dụng để tìm kiếm thiết bị đang bật bluetooth)
 */
const val REQUEST_ENABLE_BT = 1
const val LOCATION_REQUEST = 2

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


