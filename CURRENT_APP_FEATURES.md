# InvSmart - Tinh nang hien dang hoat dong

Tai lieu nay tong hop cac tinh nang da co trong ung dung, dua tren code hien tai cua du an.

## 1) Tong quan kien truc app

1. Nen tang: Android (Kotlin), Navigation Component, ViewModel + StateFlow, Hilt DI.
2. Backend as a Service: Firebase Auth + Cloud Firestore.
3. Tich hop upload anh: Cloudinary (Android SDK).
4. Co luu session local bang SharedPreferences de vao thang man role khi mo lai app.

## 2) He thong dang nhap, dang ky, khoi phuc mat khau

### 2.1 Dang nhap

1. Dang nhap bang email/password thong qua Firebase Auth.
2. Sau khi dang nhap thanh cong:
   - Tai thong tin user trong collection `users`.
   - Chuan hoa role (`master`, `manager`, `staff`).
   - Dieu huong vao man theo role:
     - `master`/`manager` -> luong manager.
     - `staff` -> luong staff.
3. Neu tai khoan khong ton tai hoac bi khoa (`accessStatus = blocked/disabled`) thi bat dang xuat va thong bao loi.

### 2.2 Dang ky

1. Dang ky tai khoan moi bang email/password.
2. Validate mat khau bat buoc:
   - >= 8 ky tu.
   - Co it nhat 1 chu so.
   - Co it nhat 1 chu thuong.
   - Co it nhat 1 chu hoa.
   - Co it nhat 1 ky tu dac biet.
3. Sau khi tao account Firebase thanh cong:
   - Tao profile user trong collection `users` (mac dinh role `staff`, `accessStatus = active`).
4. Sau dang ky thanh cong:
   - Quay ve man dang nhap.
   - Tu dong dien san email + mat khau vua dang ky vao form login.

### 2.3 Quen mat khau

1. Kiem tra email da dang ky hay chua.
2. Neu da ton tai, gui email reset password qua Firebase Auth.

## 3) Session va dieu huong khi mo app

1. Luu trang thai login + role vao SharedPreferences (`SessionManager`).
2. Khi mo app lai:
   - Neu chua login -> vao luong auth.
   - Neu da login va role la manager/master -> vao nav manager.
   - Neu da login va role la staff -> vao nav staff.
3. Dang xuat se:
   - Sign out Firebase Auth.
   - Xoa session local.
   - Reset state tren UI va ve luong auth.

## 4) Tinh nang luong Manager/Master

### 4.1 Dashboard

1. Hien thi thong tin chao theo role (`Master Dashboard`/`Manager Dashboard`).
2. Hien doanh thu tong tu cac don da thanh toan (`status = paid`).
3. Dieu huong nhanh den:
   - Quan ly kho hang.
   - Quan ly nhan vien.
4. Co nut dang xuat.

### 4.2 Quan ly kho hang (CRUD san pham)

1. Xem danh sach san pham realtime tu Firestore (`products`).
2. Them san pham moi:
   - SKU, ten, gia, ton kho.
   - Chon anh tu may.
   - Preview anh ngay trong dialog.
   - Upload anh len Cloudinary.
   - Lay `secure_url` va luu vao `imageUrl` cua product.
3. Sua san pham:
   - Sua ten, gia, ton kho, anh.
   - Co the giu anh cu hoac chon anh moi de upload lai.
4. Xoa san pham co hop thoai xac nhan.

### 4.3 Quan ly nguoi dung

1. Tai danh sach user co the quan ly theo role cua actor:
   - Master: quan ly Manager + Staff.
   - Manager: chi xem/quan ly Staff.
2. Doi role qua lai Manager <-> Staff (chi Master duoc doi).
3. Chan doi role voi tai khoan Master.

## 5) Tinh nang luong Staff

### 5.1 Trang Staff Home

1. Hien danh sach don hang cua nhan vien hien tai (theo `staffUid`).
2. Sap xep don theo thoi gian tao giam dan.
3. Toolbar co:
   - Vao trang tai khoan.
   - Dang xuat.
4. Co nut tao phieu moi.

### 5.2 Chon san pham

1. Hien danh sach san pham de tao don.
2. Tim kiem theo ten hoac SKU.
3. Tang/giam so luong moi san pham.
4. Tong so luong da chon cap nhat tren nut "Tiep tuc (x)".

### 5.3 Xac nhan don

1. Xem danh sach item da chon + tong so luong.
2. Tao don hang (`orders`) voi thong tin:
   - staffUid, staffName, orderType.
   - items, totalQuantity, totalAmount.
   - status mac dinh `pending_payment`.
3. Cap nhat ton kho theo batch:
   - Don `sale`: tru ton.
   - Don `import`: cong ton.

### 5.4 Thanh toan QR

1. Co man thanh toan QR (`PaymentQrFragment`) sinh QR URL theo payload don hang.
2. Nut "Da thanh toan" cap nhat don:
   - `status = paid`
   - `paidAt = now`
3. Quay ve trang Staff Home sau khi cap nhat thanh cong.

## 6) Tai khoan ca nhan

1. Staff co the xem email (read-only).
2. Cap nhat ho ten va so dien thoai.
3. Luu xuong Firestore (`users`) kem `updatedAt`.

## 7) Mo hinh du lieu dang dung

### 7.1 Collections chinh

1. `users`
2. `products`
3. `orders`

### 7.2 Field nghiep vu noi bat

1. Product:
   - `productId`, `sku`, `name`, `price`, `stockQty`, `imageUrl`, ...
2. Order:
   - `orderId`, `staffUid`, `status`, `items`, `totalAmount`, `paidAt`, ...
3. User:
   - `uid`, `email`, `roleGlobal`, `isMaster`, `accessStatus`, ...

## 8) Tich hop Cloudinary hien tai

1. App init Cloudinary khi startup (`InvSmartApp`).
2. Cau hinh doc tu `BuildConfig`:
   - `CLOUDINARY_CLOUD_NAME`
   - `CLOUDINARY_UPLOAD_PRESET`
3. Upload anh su dung unsigned preset.
4. Anh sau upload luu URL vao Firestore de hien thi bang Glide.

## 9) Diem da hoan thien ve UX/logic

1. Dang ky xong quay lai login va autofill thong tin.
2. Validate mat khau tai ca UI layer va ViewModel layer.
3. Session role-based startup de bo qua man auth khi da login.
4. Preview anh truoc khi luu san pham.

## 10) Cac gioi han hien tai (de biet)

1. Payment QR dang su dung URL tao QR don gian (chua gateway thanh toan that).
2. Upload Cloudinary dang dung unsigned preset (phu hop MVP, can can nhac signed upload neu can muc do bao mat cao hon).
3. Model van giu mot so field legacy lien quan team (`teamId`, `defaultTeamId`) nhung luong hien tai da van hanh theo 1 kho chung.
