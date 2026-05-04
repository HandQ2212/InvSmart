# Danh Sách Nghiệp Vụ Ứng Dụng InvSmart

## 1. Nghiệp vụ xác thực và tài khoản

1. Đăng ký tài khoản bằng email/mật khẩu.
2. Đăng nhập bằng email/mật khẩu.
3. Quên mật khẩu qua email reset.
4. Lưu và khôi phục phiên đăng nhập.
5. Đăng xuất và xóa session local.
6. Cập nhật thông tin cá nhân (họ tên, số điện thoại).

## 2. Nghiệp vụ phân quyền và điều hướng

1. Phân quyền theo vai trò: `master`, `manager`, `staff`.
2. Điều hướng vào đúng luồng màn hình theo vai trò sau đăng nhập.
3. Chặn truy cập chức năng không đúng quyền.
4. Kiểm tra trạng thái tài khoản (`active`/`blocked`) trước khi cho truy cập nghiệp vụ.

## 3. Nghiệp vụ quản lý kho (Manager/Master)

1. Xem danh sách sản phẩm theo thời gian thực.
2. Thêm sản phẩm mới: SKU, tên, giá, tồn kho, ảnh.
3. Sửa thông tin sản phẩm.
4. Xóa sản phẩm có xác nhận.
5. Upload ảnh sản phẩm lên Cloudinary và lưu URL vào Firestore.

## 4. Nghiệp vụ quản lý người dùng

1. Xem danh sách user theo phạm vi quyền.
2. Đổi quyền người dùng theo chính sách phân quyền.
3. Chặn thao tác đổi quyền trái phép (đặc biệt với tài khoản `master`).

## 5. Nghiệp vụ tạo đơn hàng (Staff)

1. Tạo phiếu mới (bán/nhập).
2. Tìm kiếm sản phẩm theo tên hoặc SKU.
3. Chọn số lượng từng mặt hàng.
4. Kiểm tra ràng buộc tồn kho khi tạo phiếu bán.
5. Xác nhận đơn với tổng số lượng và tổng tiền.
6. Lưu đơn hàng với trạng thái ban đầu `pending_payment`.
7. Cập nhật tồn kho theo loại đơn:
   - `sale`: trừ kho.
   - `import`: cộng kho.

## 6. Nghiệp vụ theo dõi đơn hàng

1. Xem danh sách “Đơn hàng của tôi” theo `staffUid`.
2. Sắp xếp đơn theo thời gian tạo giảm dần.
3. Xem chi tiết hóa đơn:
   - Thông tin đầu đơn (mã đơn, loại đơn, ngày tạo, trạng thái).
   - Danh sách item (tên hàng, đơn giá, số lượng, thành tiền).
   - Tổng số lượng và tổng tiền.

## 7. Nghiệp vụ dashboard

1. Hiển thị dashboard theo vai trò (`master`/`manager`).
2. Thống kê doanh thu từ đơn đã thanh toán.
3. Điều hướng nhanh sang các màn hình quản trị.
4. Hỗ trợ đăng xuất trực tiếp từ dashboard.

## 8. Nghiệp vụ dữ liệu và đồng bộ

1. Lưu dữ liệu nghiệp vụ trên Firestore (`users`, `products`, `orders`, `payments`).
2. Đồng bộ dữ liệu realtime cho các màn quan trọng.
3. Áp dụng Security Rules theo vai trò để kiểm soát truy cập.

## 9. Ràng buộc nghiệp vụ cốt lõi

1. Mọi quyền truy cập phụ thuộc vai trò người dùng.
2. Tài khoản không hợp lệ hoặc bị khóa phải bị buộc đăng xuất.
3. Phiếu bán không được làm âm tồn kho.
4. Tạo đơn và cập nhật kho phải đảm bảo tính nhất quán dữ liệu.
5. Đăng xuất phải xóa toàn bộ session local.
