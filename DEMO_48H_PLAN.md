# InvSmart - Ke hoach 48 gio de kip demo

## 1) Muc tieu demo (MVP)

1. Dang ky/dang nhap bang Firebase Auth (Email/Password).
2. Hien thi danh sach san pham realtime tu Firestore.
3. Them san pham moi co anh (Cloudinary URL).
4. Nhap/xuat kho va ghi `inventory_logs`.
5. Hien thi canh bao san pham sap het hang.

## 2) Pham vi tam hoan (de kip deadline)

1. Phan quyen phuc tap theo role.
2. Dashboard thong ke nang cao.
3. Tim kiem/filter nang cao.
4. UI polishing qua sau (animation, theming nang).

## 3) Lich trinh thuc chien 48 gio

### Ngay 1 - On dinh nen tang

1. 08:00-09:30: Sua loi build/Hilt, app chay duoc tren may that.
2. 09:30-11:30: Chot model + repository theo schema Firestore.
3. 13:30-16:00: Hoan tat Auth flow (login/register/logout).
4. 16:00-18:00: Product list realtime (Flow/StateFlow + RecyclerView).
5. 20:00-22:00: Test tay va fix crash/blocker.

### Ngay 2 - Chot tinh nang demo

1. 08:00-10:30: Them san pham + upload anh Cloudinary.
2. 10:30-12:00: Nhap/xuat kho + ghi `inventory_logs`.
3. 13:30-15:00: Hien thi low stock (`stock_quantity < min_stock_level`).
4. 15:00-17:00: Chuan hoa loading/empty/error state + seed du lieu demo.
5. 19:00-21:00: Tong duyet 2 lan + quay video backup.

## 4) Checklist ky thuat bat buoc

1. App khong crash o cac luong chinh.
2. Firestore rules khong mo toan bo khi demo that.
3. `products`, `categories`, `inventory_logs` doc/ghi dung schema.
4. Moi thao tac IN/OUT deu tao 1 ban ghi log.
5. UI co 3 state: loading, empty, error.
6. Co du lieu mau de demo (>= 10 products, >= 3 categories).

## 5) Script demo 3-5 phut

1. Dang nhap vao app.
2. Mo danh sach products, chi ra realtime va low stock.
3. Them 1 product moi (co anh).
4. Thuc hien 1 thao tac OUT kho.
5. Mo inventory logs de chung minh luu vet.

## 6) Ke hoach fallback khi gap su co

1. Neu mang kem: dung du lieu da seed san va demo luong doc.
2. Neu upload anh loi: cho phep nhap `image_url` thu cong de tiep tuc demo.
3. Neu Auth loi: dung tai khoan demo da tao truoc.
4. Neu app crash: dung video backup da quay truoc.

## 7) Cong viec can lam ngay (60 phut toi)

1. Build debug va sua triet de loi Hilt hien tai.
2. Chay thu login/register tren 1 tai khoan that.
3. Chay thu list products realtime voi du lieu Firestore.
4. Chot danh sach blocker con lai theo muc do uu tien.

