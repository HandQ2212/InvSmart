# CHI TIẾT FLOW NGHIỆP VỤ (InvSmart)

## 1) Flow Đăng nhập

Mục tiêu: Cho phép người dùng truy cập đúng vai trò và đúng màn hình.

Tiền điều kiện:
- Người dùng đã có tài khoản Firebase Auth.
- Thiết bị có kết nối mạng.

Luồng chính:
1. Người dùng mở màn hình đăng nhập, nhập email và mật khẩu.
2. Hệ thống kiểm tra dữ liệu đầu vào không rỗng.
3. Gọi Firebase Auth để đăng nhập.
4. Nếu đăng nhập thành công, hệ thống đọc hồ sơ user trong collection `users`.
5. Hệ thống chuẩn hóa role (`master`, `manager`, `staff`) và kiểm tra `accessStatus`.
6. Lưu phiên đăng nhập cục bộ (UID, role, trạng thái đăng nhập) vào `SharedPreferences`.
7. Điều hướng:
  - `master/manager` vào luồng Manager Dashboard.
  - `staff` vào luồng Staff.

Nhánh lỗi:
- Sai email/mật khẩu: hiển thị thông báo đăng nhập thất bại.
- Không có hồ sơ Firestore: tự động đăng xuất và báo tài khoản không hợp lệ.
- Tài khoản bị khóa/vô hiệu hóa: tự động đăng xuất và báo không có quyền truy cập.
- Mất mạng: hiển thị lỗi kết nối và cho phép thử lại.

Kết quả mong đợi:
- Người dùng vào đúng màn hình theo vai trò, session được lưu ổn định.

---

## 2) Flow Đăng ký

Mục tiêu: Tạo tài khoản mới hợp lệ và có hồ sơ người dùng trong Firestore.

Tiền điều kiện:
- Email chưa được đăng ký.
- Mật khẩu đạt chuẩn bảo mật.

Luồng chính:
1. Người dùng nhập email, mật khẩu và thông tin cần thiết ở màn đăng ký.
2. Hệ thống validate mật khẩu tại UI và ViewModel:
  - Tối thiểu 8 ký tự.
  - Có chữ số, chữ thường, chữ hoa, ký tự đặc biệt.
3. Gọi Firebase Auth để tạo tài khoản.
4. Nếu thành công, tạo document user trong `users` với dữ liệu mặc định:
  - `role = staff`
  - `accessStatus = active`
5. Điều hướng về màn đăng nhập và tự điền email/mật khẩu vừa tạo.

Nhánh lỗi:
- Email đã tồn tại: báo lỗi email đã được sử dụng.
- Mật khẩu yếu: báo đúng tiêu chí chưa đạt.
- Lỗi mạng hoặc timeout: báo thất bại tạm thời, cho phép thử lại.

Kết quả mong đợi:
- Tài khoản được tạo đầy đủ ở Auth và Firestore, người dùng đăng nhập ngay được.

---

## 3) Flow Quên mật khẩu

Mục tiêu: Khôi phục quyền truy cập tài khoản qua email.

Luồng chính:
1. Người dùng nhập email ở màn quên mật khẩu.
2. Hệ thống kiểm tra định dạng email hợp lệ.
3. Gửi yêu cầu reset password qua Firebase Auth.
4. Thông báo đã gửi email hướng dẫn đặt lại mật khẩu.

Nhánh lỗi:
- Email không hợp lệ: báo lỗi nhập liệu.
- Email không tồn tại hoặc lỗi gửi mail: báo thông tin phù hợp theo response.

Kết quả mong đợi:
- Người dùng nhận email và tự đặt lại mật khẩu thành công.

---

## 4) Flow Mở app và điều hướng tự động theo phiên

Mục tiêu: Vào đúng luồng ngay khi mở app, không cần đăng nhập lại nếu phiên còn hiệu lực.

Luồng chính:
1. Khi app khởi động, hệ thống đọc session từ `SharedPreferences`.
2. Nếu chưa đăng nhập, vào luồng Auth.
3. Nếu đã đăng nhập:
  - role `manager/master` vào luồng Manager.
  - role `staff` vào luồng Staff.
4. Đồng bộ state user để các màn hình phụ thuộc dữ liệu có thể tải đúng.

Nhánh lỗi:
- Session hỏng hoặc thiếu role: xóa session cũ và đưa về Auth.

Kết quả mong đợi:
- Điều hướng nhanh, nhất quán theo role.

---

## 5) Flow Đăng xuất

Mục tiêu: Kết thúc phiên an toàn, không còn dữ liệu nhạy cảm trong bộ nhớ phiên.

Luồng chính:
1. Người dùng bấm nút đăng xuất ở dashboard hoặc màn tài khoản.
2. Hệ thống gọi `FirebaseAuth.signOut()`.
3. Xóa dữ liệu phiên local trong `SessionManager`.
4. Reset state UI/ViewModel liên quan user.
5. Điều hướng về luồng Auth và chặn quay lại màn trước.

Nhánh lỗi:
- Nếu có lỗi local khi xóa session, ưu tiên vẫn điều hướng về Auth và ghi log để theo dõi.

Kết quả mong đợi:
- Người dùng đã đăng xuất hoàn toàn, mở lại app không vào thẳng màn trong.

---

## 6) Flow Cập nhật hồ sơ cá nhân

Mục tiêu: Cho phép người dùng sửa thông tin cá nhân cơ bản.

Luồng chính:
1. Người dùng mở màn hình tài khoản.
2. Hệ thống hiển thị email ở chế độ chỉ đọc.
3. Người dùng sửa họ tên, số điện thoại.
4. Bấm lưu, hệ thống validate dữ liệu.
5. Cập nhật Firestore kèm `updatedAt`.
6. Cập nhật lại state hiển thị trên UI.

Nhánh lỗi:
- Dữ liệu không hợp lệ: báo lỗi tại trường tương ứng.
- Lỗi mạng: báo lưu thất bại và cho phép thử lại.

Kết quả mong đợi:
- Hồ sơ được cập nhật ngay và đồng bộ đúng.

---

## 7) Flow Quản lý sản phẩm (Manager/Master)

Mục tiêu: Quản lý danh mục hàng hóa và tồn kho theo thời gian thực.

### 7.1 Xem danh sách sản phẩm
1. Mở màn quản lý kho.
2. Hệ thống lắng nghe dữ liệu Firestore realtime.
3. Hiển thị danh sách sản phẩm với thông tin SKU, tên, giá, tồn kho, ảnh.

### 7.2 Thêm sản phẩm
1. Người dùng bấm thêm sản phẩm.
2. Nhập SKU, tên, giá, tồn kho; chọn ảnh từ thiết bị.
3. Ảnh được preview trong dialog.
4. Upload ảnh lên Cloudinary, nhận `secure_url`.
5. Lưu sản phẩm lên Firestore với `imageUrl = secure_url`.
6. Danh sách tự refresh realtime.

### 7.3 Sửa sản phẩm
1. Chọn sản phẩm cần sửa.
2. Cập nhật thông tin và/hoặc thay ảnh mới.
3. Nếu đổi ảnh, upload ảnh mới rồi cập nhật `imageUrl`.
4. Lưu thay đổi lên Firestore.

### 7.4 Xóa sản phẩm
1. Chọn xóa.
2. Hiển thị hộp thoại xác nhận.
3. Xác nhận xóa thì xóa document sản phẩm.

Nhánh lỗi chung:
- Upload ảnh thất bại: không lưu sản phẩm, báo lỗi rõ ràng.
- Firestore lỗi quyền hoặc mạng: báo thất bại và giữ dữ liệu form.

Kết quả mong đợi:
- CRUD sản phẩm ổn định, dữ liệu đồng bộ realtime.

---

## 8) Flow Quản lý người dùng (Manager/Master)

Mục tiêu: Quản lý phân quyền đúng phạm vi quản trị.

Luồng chính:
1. Mở màn quản lý người dùng.
2. Hệ thống tải danh sách theo quyền hiện tại:
  - `master` thấy `manager` và `staff`.
  - `manager` chỉ thấy `staff`.
3. Thực hiện thay đổi role khi được phép.
4. Lưu role mới lên Firestore.

Ràng buộc quyền:
- Chặn thao tác đổi quyền tài khoản `master`.
- Manager không được tự nâng quyền hoặc sửa role ngoài phạm vi.

Nhánh lỗi:
- Không đủ quyền: báo lỗi quyền truy cập.
- Lỗi cập nhật Firestore: rollback UI về trạng thái cũ.

Kết quả mong đợi:
- Role được quản lý đúng chính sách, không vượt quyền.

---

## 9) Flow Staff tạo đơn hàng

Mục tiêu: Cho phép nhân viên tạo phiếu bán/nhập và cập nhật tồn kho chính xác.

### 9.1 Chọn sản phẩm
1. Staff mở màn tạo phiếu.
2. Tìm kiếm theo tên hoặc SKU.
3. Tăng/giảm số lượng từng sản phẩm.
4. Với phiếu bán (`sale`), không cho vượt tồn kho hiện có.
5. Nút tiếp tục hiển thị số mặt hàng đã chọn.

### 9.2 Xác nhận đơn
1. Mở màn xác nhận đơn.
2. Hiển thị từng dòng hàng: tên, đơn giá, số lượng, thành tiền.
3. Hiển thị tổng số lượng và tổng tiền.
4. Staff bấm xác nhận tạo đơn.

### 9.3 Ghi dữ liệu và cập nhật kho
1. Tạo document trong `orders` với trạng thái `pending_payment`.
2. Lưu `items`, `totalQuantity`, `totalAmount`, `staffUid`, `staffName`, loại đơn.
3. Cập nhật tồn kho theo batch:
  - `sale`: trừ kho.
  - `import`: cộng kho.
4. Nếu transaction/batch thành công, trả về mã đơn.

Nhánh lỗi:
- Tồn kho thay đổi trong lúc tạo đơn: từ chối tạo đơn và yêu cầu kiểm tra lại.
- Lỗi ghi `orders` hoặc batch kho: rollback thao tác, không tạo dữ liệu nửa chừng.

Kết quả mong đợi:
- Đơn tạo thành công và tồn kho nhất quán.

---

## 10) Flow Staff xem danh sách đơn và chi tiết đơn

Mục tiêu: Nhân viên theo dõi đơn đã tạo của chính mình.

### 10.1 Danh sách đơn của tôi
1. Mở màn Đơn hàng của tôi.
2. Hệ thống query `orders` theo `staffUid` hiện tại.
3. Sắp xếp theo thời gian tạo giảm dần.
4. Hiển thị mã đơn, loại đơn, ngày tạo, tổng số lượng, tổng tiền.
5. Refresh dữ liệu khi app resume hoặc state user sẵn sàng.

### 10.2 Chi tiết hóa đơn
1. Người dùng chọn 1 đơn từ danh sách.
2. Màn chi tiết hiển thị:
  - Thông tin đầu đơn: mã đơn, loại đơn, ngày tạo, trạng thái.
  - Danh sách item: tên hàng, đơn giá, số lượng, thành tiền.
  - Tổng số lượng, tổng tiền.

Nhánh lỗi:
- Không tải được đơn: hiển thị trạng thái lỗi và nút thử lại.

Kết quả mong đợi:
- Nhân viên tra cứu nhanh, dữ liệu chi tiết chính xác.

---

## 11) Luật nghiệp vụ quan trọng

1. Role quyết định toàn bộ quyền truy cập màn hình và thao tác.
2. Đăng xuất luôn phải xóa session local và reset state.
3. Với `sale`, số lượng bán không được âm kho.
4. Tạo đơn và cập nhật kho phải theo cơ chế atomic (batch/transaction).
5. Tài khoản bị khóa hoặc hồ sơ không hợp lệ phải bị buộc đăng xuất.
