# Firebase Setup Steps for InvSmart

Tài liệu này tổng hợp các bước Firebase theo thứ tự thực tế để mọi người trong dự án có thể setup nhanh, đúng và ít lỗi.

## 0) Lý do sử dụng Firebase cho InvSmart

1. Time-to-market nhanh:
   - Firebase Auth + Firestore giúp bỏ qua công đoạn tự xây backend auth, session, API CRUD.
2. Realtime data cho mobile:
   - Firestore hỗ trợ stream realtime, app cập nhật dữ liệu đơn hàng/sản phẩm gần như ngay lập tức.
   - Giảm độ phức tạp cho cơ chế đồng bộ thủ công.
3. Security Rules theo mô hình 1 kho:
   - Dữ liệu được tổ chức theo kho dùng chung, không cần tầng phân tách `teamId`.
   - Tập trung kiểm soát quyền theo vai trò người dùng (manager/staff) để đơn giản vận hành.
4. Hệ sinh thái vận hành gọn:
   - CLI deploy rules/indexes + console quan sát dữ liệu nhanh.
   - Dễ chuẩn hóa quy trình cho tất cả thành viên trong dự án.
5. Chi phí và vận hành phù hợp MVP:
   - Không cần quản trị server riêng cho giai đoạn đầu.
   - Dễ nâng cấp tiếp khi mở rộng (rules, indexes, cloud functions nếu cần).

## 0.1) Checklist nhanh để setup Firebase từ đầu đến cuối

1. Cài `firebase-tools` và đăng nhập CLI.
2. Gắn project alias đúng (`invsmart-391a0`).
3. Chuẩn bị service account key ngoài repo.
4. Seed dữ liệu Firestore (`--clean` trước, seed sau).
5. Deploy `firestore.rules` và `firestore.indexes.json`.
6. Verify Data/Rules/Indexes trên Firebase Console.
7. Chạy app và test luồng nghiệp vụ kho dùng chung trên dữ liệu đã seed.

## 1) Tổng quan file đã có trong repo

### Cần có

1. [firebase.json](firebase.json)
2. [firestore.rules](firestore.rules)
3. [firestore.indexes.json](firestore.indexes.json)
4. [scripts/firestore-seed/seed.js](scripts/firestore-seed/seed.js)
5. [scripts/firestore-seed/README.md](scripts/firestore-seed/README.md)

### Tại sao bước này cần

- Đây là bộ file tối thiểu để deploy rules, indexes và tạo dữ liệu mẫu một cách tái lập.
- Nếu thiếu một file, quá trình deploy hoặc seed sẽ bị dừng giữa chừng.

## 2) Mô hình dữ liệu Firestore hiện tại

### Collections chính

1. `users`
2. `products`
3. `orders`
4. `payments`
5. `orders/{orderId}/items` (subcollection)

### Tại sao thiết kế như vậy

- Bỏ các collection quản lý team để giảm độ phức tạp dữ liệu và luồng nghiệp vụ.
- Dùng một kho dữ liệu chung giúp query đơn giản, dễ debug và dễ onboarding thành viên mới trong giai đoạn MVP.
- Dùng subcollection `orders/{orderId}/items` để lưu chi tiết đơn hàng, không làm document `orders` quá lớn.

## 3) Quy trình setup từng bước (có lý do)

### Bước 1 - Cài Firebase CLI

```bash
npm i -g firebase-tools
```

Tại sao:
- CLI là công cụ chuẩn để deploy rules/indexes và đồng bộ cấu hình từ repo lên Firebase.

### Bước 2 - Đăng nhập Firebase CLI

```bash
firebase login
```

Tại sao:
- Đảm bảo lệnh deploy được xác thực đúng tài khoản có quyền trên project.

### Bước 2.1 - Rule kiểm tra mật khẩu ở bước Đăng ký (app)

Khi người dùng đăng ký tài khoản, bắt buộc validate mật khẩu có đủ các điều kiện sau:

1. Có ít nhất 1 chữ số (`0-9`).
2. Có ít nhất 1 chữ cái thường (`a-z`).
3. Có ít nhất 1 chữ cái hoa (`A-Z`).
4. Có ít nhất 1 ký tự đặc biệt (ví dụ: `!@#$%^&*()_+-=[]{}|;:,.<>?`).

Khuyến nghị thêm:

1. Độ dài tối thiểu từ 8 ký tự.
2. Hiển thị thông báo lỗi cụ thể theo từng điều kiện chưa đạt để người dùng sửa nhanh.

### Bước 3 - Gắn project alias cho repo

```bash
firebase use --add
```

Chọn project: `invsmart-391a0`.

Tại sao:
- Tránh deploy nhầm project.
- Thành viên mới khi pull code về vẫn có cùng một điểm chuẩn để thao tác.

### Bước 4 - Seed dữ liệu (xóa mẫu cũ, tạo dữ liệu mới)

Từ repository root:

```powershell
$env:SERVICE_ACCOUNT_PATH="D:/keys/invsmart-391a0-firebase-adminsdk-fbsvc-1985770511.json"
Set-Location scripts/firestore-seed
node .\seed.js --clean
node .\seed.js
```

Tại sao:
- `--clean` giúp loại bỏ dữ liệu demo cũ, tránh trùng lặp và sai logic khi test.
- Seed lại dữ liệu giúp team có một baseline giống nhau để test app.
- Service Account cho phép script admin ghi dữ liệu ổn định hơn so với client SDK.

### Bước 5 - Deploy Rules và Indexes

Từ repository root:

```bash
firebase deploy --only firestore
```

Hoặc deploy riêng:

```bash
firebase deploy --only firestore:rules
firebase deploy --only firestore:indexes
```

Tại sao:
- Rules là lớp bảo mật quan trọng nhất, cần đồng bộ theo code trước khi release.
- Indexes đảm bảo các query có `where/orderBy` chạy được và đúng tốc độ.

Nếu gặp lỗi `HTTP Error: 403` (`serviceusage.googleapis.com`), cấp IAM cho service account:

1. `roles/serviceusage.serviceUsageConsumer`
2. `roles/datastore.owner` (hoặc custom role tương đương)
3. `roles/firebaserules.admin` (nếu phân quyền theo tách biệt)

Sau đó đợi vài phút cho IAM propagation rồi deploy lại.

### Bước 6 - Verify trên Firebase Console

1. Firestore -> Data:
   - Kiểm tra các collections đã có đủ dữ liệu seed.
2. Firestore -> Rules:
   - Kiểm tra rules đã publish khớp [firestore.rules](firestore.rules).
3. Firestore -> Indexes:
   - Kiểm tra index status là `Enabled`.

Tại sao:
- Deploy thành công trên CLI chưa chắc nghĩa là mọi tài nguyên đã sẵn sàng ngay lập tức.
- Verify trên console giúp bắt lỗi sớm (index chưa build xong, rules publish sai file, ...).

## 4) Sơ đồ tổng quát quan hệ giữa collections

### Mermaid ER-style diagram

```mermaid
flowchart LR
   U[users\nuid, email, displayName, role] --> O[orders\ncreatedBy, createdAt, ...]
   U --> P[products\nname, stock, minStock, ...]
    O --> OI[orders/{orderId}/items\nproductId, qty, price]

   O --> PAY[payments\norderId, status, ...]
```

### Đọc nhanh ý nghĩa quan hệ

1. `users` là danh tính toàn cục theo Firebase Auth UID.
2. `products`, `orders`, `payments` dùng chung trong một kho duy nhất.
3. Quyền thao tác được kiểm soát bằng role người dùng thay vì phân vùng theo team.
4. `orders/{orderId}/items` lưu chi tiết dòng hàng của mỗi order.

## 5) Rules behavior đã triển khai

1. Mô hình một kho dữ liệu chung, không kiểm tra `teamId`.
2. Người dùng đã xác thực có thể đọc dữ liệu cần thiết cho vận hành kho.
3. Quyền ghi/sửa/xóa được phân theo role (manager/staff).
4. Chặn thao tác nhạy cảm với tài khoản không đủ quyền.
5. Validation dữ liệu đầu vào được giữ ở rules để tránh ghi sai schema.

## 6) Công việc app code nên làm tiếp

1. Loại bỏ hoàn toàn filter `teamId` ở repository/query còn sót.
2. Rà soát lại rules để khớp mô hình 1 kho (không phụ thuộc `team_members`, `team_invites`).
3. Khóa chức năng nhạy cảm trên UI theo role manager/staff.
4. Chuẩn hóa payment status (`pending`, `success`, `failed`, `canceled`) trong cả UI và Firestore.
5. Bổ sung test cho luồng nhập/xuất kho và tạo đơn trong kho dùng chung.

## 6.1) Trạng thái triển khai use case (đã làm)

1. Hệ thống đã vận hành theo mô hình 1 kho dùng chung.
2. Các collection nghiệp vụ chính tập trung vào `products`, `orders`, `payments`.
3. Luồng tạo đơn và đọc sản phẩm không còn phụ thuộc membership theo team.
4. App đã bỏ phần quản lý lời mời/thành viên team trong nghiệp vụ kho.
5. Query dữ liệu đã được đơn giản hóa để khớp Security Rules hiện tại.

## 6.2) Cách test nhanh đúng use case

1. Đăng nhập bằng tài khoản manager.
2. Kiểm tra danh sách sản phẩm hiển thị đầy đủ dữ liệu kho dùng chung.
3. Tạo một đơn hàng mới và thêm item vào `orders/{orderId}/items`.
4. Thực hiện cập nhật trạng thái thanh toán và kiểm tra dữ liệu `payments`.
5. Đăng nhập tài khoản staff, xác nhận chỉ các thao tác được cấp quyền mới thực hiện được.

## 7) Security notes

1. Không commit service-account JSON lên git.
2. Giữ pattern ignore key trong [.gitignore](.gitignore).
3. Nếu lộ key, rotate ngay lập tức và revoke key cũ.
