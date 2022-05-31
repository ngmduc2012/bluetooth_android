# bluetooth_android
Bluetooth API, Bluetooth LE

Chức năng các nhánh:

Master: Sử dụng bluetooth class để tạo 1 ứng dụng cho phép đọc chuỗi JSON. 

Blue_class_text: Sử dụng bluetooth class để tạo 1 ứng dụng chỉ đọc và gửi chuỗi ký tự ngắn qua nhau  

Blue_le_text: Sử dụng bluetooth low energy để tạo 1 ứng dụng chỉ đọc và gửi chuỗi ký tự ngắn qua nhau

Blue_le:  Sử dụng bluetooth low energy để tạo 1 ứng dụng cho phép đọc chuỗi JSON. 

Blue_le_autoconnect: Vì bluetooth low energy không tự kết nối ngược lại tới client, nên ở nhánh này sẽ 
tạo thêm chức năng tự kết nối lại để thuận tiện cho giao tiếp 2 chiều. 

(CHÚ Ý: Ở nhánh Blue_le_text và Blue_le_autoconnect đã thêm chức năng kết nối với bluetooth IOS, tham 
khảo tại https://github.com/ngmduc2012/bluetooth_le_ios.git)

--- 

Branch: 

Master: Build an application read JSON string by Bluetooth class

Blue_class_text: Build an application read short string by Bluetooth class

Blue_le_text: Build an application read short string by Bluetooth low energy

Blue_le: Build an application read JSON string by Bluetooth low energy 

Blue_le_autoconnect: Because Using bluetooth low energy no support send message back to client, so in 
this branch we add function auto connect again for 2-way communication. 

(NOTE: In Blue_le_text and Blue_le_autoconnect is added connect with an application of IOS by bluetooth 
low energy, the following: https://github.com/ngmduc2012/bluetooth_le_ios.git)


