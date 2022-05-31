# bluetooth_android
Bluetooth API, Bluetooth LE

Chức năng các nhánh:

Master: Sử dụng bluetooth class để tạo 1 ứng dụng cho phép đọc chuỗi JSON. 

Blue_class_text: Sử dụng bluetooth class để tạo 1 ứng dụng chỉ đọc và gửi chuỗi ký tự ngắn qua nhau  

Blue_le_text: Sử dụng bluetooth low energy để tạo 1 ứng dụng chỉ đọc và gửi chuỗi ký tự ngắn qua nhau

Blue_le:  Sử dụng bluetooth low energy để tạo 1 ứng dụng cho phép đọc chuỗi JSON. 

Blue_le_autoconnect: Vì bluetooth low energy không tự kết nối ngược lại tới server, nên ở nhánh này sẽ 
thạo thêm chức năng tự kết nối lại để thuận tiện cho giao tiếp 2 chiều. 

(CHÚ Ý: Ở nhánh Blue_le_text và Blue_le_autoconnect đã thêm chức năng kết nối với bluetooth IOS, tham 
khảo tại https://github.com/ngmduc2012/bluetooth_le_ios.git)
