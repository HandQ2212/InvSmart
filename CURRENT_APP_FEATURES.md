# InvSmart - Chi tiết chức năng ứng dụng

Tài liệu này mô tả chi tiết các chức năng đang hoạt động trong ứng dụng InvSmart, theo đúng trạng thái code hiện tại.

## 1. Tổng quan hệ thống

1. Nền tảng: Android (Kotlin).
2. Kiến trúc: Navigation Component, ViewModel + StateFlow, Hilt DI.
3. Dịch vụ backend: Firebase Authentication + Cloud Firestore.
4. Upload ảnh: Cloudinary Android SDK. 
5. Lưu phiên đăng nhập cục bộ: SharedPreferences (SessionManager).

## 2. Chức năng xác thực tài khoản

### 2.1 Đăng nhập

1. Đăng nhập bằng email và mật khẩu qua Firebase Auth.
2. Sau đăng nhập thành công:
   - Tải hồ sơ user từ `users`.
   - Chuẩn hóa và kiểm tra role (`master`, `manager`, `staff`).
   - Điều hướng đúng luồng màn hình theo role.
3. Chặn tài khoản không hợp lệ:
   - Nếu thiếu hồ sơ Firestore, bị khóa hoặc bị vô hiệu hóa (`accessStatus`), hệ thống tự đăng xuất và hiển thị lỗi.

### 2.2 Đăng ký

1. Tạo tài khoản mới bằng email và mật khẩu.
2. Kiểm tra độ mạnh mật khẩu ở cả UI và ViewModel:
   - Tối thiểu 8 ký tự.
   - Có ít nhất 1 chữ số.
   - Có ít nhất 1 chữ thường.
   - Có ít nhất 1 chữ hoa.
   - Có ít nhất 1 ký tự đặc biệt.
3. Sau khi đăng ký thành công:
   - Tạo hồ sơ người dùng trong `users`.
   - Gán mặc định role là `staff`, trạng thái `active`.
   - Quay về màn đăng nhập và tự điền email/mật khẩu vừa đăng ký.

### 2.3 Quên mật khẩu

1. Kiểm tra email tồn tại trong hệ thống.
2. Nếu hợp lệ, gửi email đặt lại mật khẩu qua Firebase Auth.

## 3. Phiên đăng nhập và điều hướng khi mở app

1. Lưu trạng thái đăng nhập và role vào SharedPreferences.
2. Khi mở lại app:
   - Chưa đăng nhập: vào luồng Auth.
   - Đã đăng nhập `manager/master`: vào luồng Manager.
   - Đã đăng nhập `staff`: vào luồng Staff.
3. Khi đăng xuất:
   - Sign out Firebase.
   - Xóa session local.
   - Reset state UI và quay về luồng Auth.

## 4. Chức năng theo vai trò Manager/Master

### 4.1 Dashboard quản trị

1. Hiển thị tiêu đề theo role (Master Dashboard hoặc Manager Dashboard).
2. Hiển thị tổng doanh thu từ các đơn đã thanh toán (`status = paid`).
3. Điều hướng nhanh đến:
   - Quản lý kho hàng.
   - Quản lý nhân viên.
4. Hỗ trợ đăng xuất trực tiếp trên dashboard.

### 4.2 Quản lý kho hàng

1. Xem danh sách sản phẩm từ Firestore theo thời gian thực.
2. Thêm sản phẩm mới gồm:
   - SKU.
   - Tên sản phẩm.
   - Giá (đ).
   - Tồn kho.
   - Ảnh sản phẩm.
3. Chọn ảnh từ thiết bị, xem preview ngay trong dialog.
4. Upload ảnh lên Cloudinary và lưu `secure_url` vào `imageUrl` trong Firestore.
5. Sửa thông tin sản phẩm và có thể thay ảnh mới.
6. Xóa sản phẩm có xác nhận để tránh thao tác nhầm.

### 4.3 Quản lý người dùng

1. Tải danh sách user theo quyền hiện tại:
   - Master quản lý được Manager và Staff.
   - Manager chỉ quản lý Staff.
2. Đổi quyền Manager/Staff (chỉ Master được phép).
3. Chặn thao tác đổi quyền tài khoản Master.

## 5. Chức năng theo vai trò Staff

### 5.1 Trang Đơn hàng của tôi

1. Hiển thị danh sách đơn theo `staffUid` của tài khoản hiện tại.
2. Sắp xếp đơn theo thời gian tạo giảm dần.
3. Mỗi card đơn hiển thị:
   - Mã đơn.
   - Loại đơn.
   - Ngày tạo.
   - Tổng số lượng.
   - Tổng tiền hóa đơn (đ).
4. Dữ liệu được refresh khi:
   - User state sẵn sàng sau khi mở app.
   - Quay lại foreground (`onResume`).
5. Nhấn vào card đơn để mở màn chi tiết hóa đơn.

### 5.2 Tạo phiếu mới

1. Vào danh sách sản phẩm để chọn hàng cần lập đơn.
2. Tìm kiếm theo tên hoặc SKU.
3. Tăng/giảm số lượng từng sản phẩm (không vượt tồn kho khi bán).
4. Nút tiếp tục hiển thị số mặt hàng đã chọn.

### 5.3 Xác nhận tạo đơn

1. Màn xác nhận hiển thị từng dòng hàng:
   - Tên mặt hàng.
   - Đơn giá (đ).
   - Số lượng.
   - Thành tiền.
2. Cuối màn có tổng kết:
   - Tổng số lượng.
   - Tổng tiền đơn.
3. Khi xác nhận:
   - Tạo document trong `orders` với `pending_payment`.
   - Lưu `items`, `totalQuantity`, `totalAmount`, `staffUid`, `staffName`.
   - Cập nhật tồn kho theo batch:
     - `sale`: trừ kho.
     - `import`: cộng kho.

### 5.4 Chi tiết hóa đơn đã tạo

1. Mở từ danh sách Đơn hàng của tôi.
2. Hiển thị thông tin đầu hóa đơn:
   - Mã đơn.
   - Loại đơn.
   - Ngày tạo.
   - Trạng thái (Chờ thanh toán hoặc Đã thanh toán).
3. Hiển thị danh sách item trong hóa đơn, gồm:
   - Tên hàng.
   - Đơn giá.
   - Số lượng.
   - Thành tiền từng dòng.
4. Hiển thị tổng số lượng và tổng tiền cuối hóa đơn.

### 5.5 Thanh toán QR

1. Sinh mã QR từ payload đơn hàng.
2. Hiển thị tổng thanh toán theo định dạng đ.
3. Nút Tôi đã thanh toán:
   - Cập nhật `status = paid`.
   - Cập nhật `paidAt = now`.
4. Sau cập nhật thành công, quay về Staff Home.

## 6. Chức năng tài khoản cá nhân

1. Xem email (chỉ đọc).
2. Cập nhật họ tên và số điện thoại.
3. Lưu thay đổi lên Firestore kèm `updatedAt`.

## 7. Chuẩn hiển thị tiền tệ

1. Toàn bộ tiền trong app hiển thị theo hậu tố `đ`.
2. Dùng formatter dùng chung để đảm bảo định dạng nhất quán trên các màn:
   - Danh sách sản phẩm.
   - Danh sách đơn hàng.
   - Màn xác nhận đơn.
   - Màn chi tiết hóa đơn.
   - Dashboard doanh thu.
   - Thanh toán QR.

## 8. Mô hình dữ liệu chính

### 8.1 Collections

1. `users`
2. `products`
3. `orders`

### 8.2 Trường dữ liệu nổi bật

1. `users`:
   - `uid`, `email`, `fullName`, `phoneNumber`, `roleGlobal`, `accessStatus`, `updatedAt`.
2. `products`:
   - `productId`, `sku`, `name`, `price`, `stockQty`, `imageUrl`.
3. `orders`:
   - `orderId`, `staffUid`, `staffName`, `orderType`, `status`, `items`, `totalQuantity`, `totalAmount`, `paidAt`, `createdAt`.

## 9. Tích hợp Cloudinary

1. Khởi tạo Cloudinary từ `BuildConfig` khi app start.
2. Dùng unsigned upload preset cho flow upload ảnh sản phẩm.
3. URL ảnh sau upload lưu vào Firestore và hiển thị bằng Glide.

## 10. Giới hạn hiện tại

1. QR payment hiện là flow mô phỏng xác nhận thanh toán, chưa tích hợp cổng thanh toán thật.
2. Upload Cloudinary đang dùng unsigned preset, phù hợp MVP; có thể nâng cấp signed upload khi cần bảo mật cao hơn.
3. Một số trường legacy liên quan team vẫn còn trong model, nhưng luồng nghiệp vụ hiện tại đang vận hành theo một kho chung.
