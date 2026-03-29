# Firebase Setup Steps for InvSmart

Tai lieu nay tong hop cac buoc Firebase theo thu tu thuc te de moi nguoi trong team co the setup nhanh, dung va it loi.

## 1) Tong quan file da co trong repo

### Can co

1. [firebase.json](firebase.json)
2. [firestore.rules](firestore.rules)
3. [firestore.indexes.json](firestore.indexes.json)
4. [scripts/firestore-seed/seed.js](scripts/firestore-seed/seed.js)
5. [scripts/firestore-seed/README.md](scripts/firestore-seed/README.md)

### Tai sao buoc nay can

- Day la bo file toi thieu de deploy rules, indexes va tao du lieu mau mot cach tai lap.
- Neu thieu mot file, qua trinh deploy hoac seed se bi dung giua chung.

## 2) Mo hinh du lieu Firestore hien tai

### Collections chinh

1. `users`
2. `teams`
3. `team_members`
4. `team_invites`
5. `products`
6. `orders`
7. `payments`
8. `orders/{orderId}/items` (subcollection)

### Tai sao thiet ke nhu vay

- Tach `team_members` va `team_invites` de quan ly vong doi nhan su ro rang (moi, chap nhan, roi team).
- Dat `teamId` tren collection nghiep vu (`products`, `orders`, `payments`) de query nhanh va de viet rules don gian hon.
- Dung subcollection `orders/{orderId}/items` de luu chi tiet don hang khong lam document `orders` qua lon.

## 3) Quy trinh setup tung buoc (co ly do)

### Buoc 1 - Cai Firebase CLI

```bash
npm i -g firebase-tools
```

Tai sao:
- CLI la cong cu chuan de deploy rules/indexes va dong bo cau hinh tu repo len Firebase.

### Buoc 2 - Dang nhap Firebase CLI

```bash
firebase login
```

Tai sao:
- Dam bao lenh deploy duoc xac thuc dung tai khoan co quyen tren project.

### Buoc 3 - Gan project alias cho repo

```bash
firebase use --add
```

Chon project: `invsmart-391a0`.

Tai sao:
- Tranh deploy nham project.
- Team moi khi pull code ve van co cung mot diem chuan de thao tac.

### Buoc 4 - Seed du lieu (xoa mau cu, tao du lieu moi)

Tu repository root:

```powershell
$env:SERVICE_ACCOUNT_PATH="D:/keys/invsmart-391a0-firebase-adminsdk-fbsvc-1985770511.json"
Set-Location scripts/firestore-seed
node .\seed.js --clean
node .\seed.js
```

Tai sao:
- `--clean` giup loai bo du lieu demo cu, tranh trung lap va sai logic khi test.
- Seed lai du lieu giup team co mot baseline giong nhau de test app.
- Service Account cho phep script admin ghi du lieu on dinh hon so voi client SDK.

### Buoc 5 - Deploy Rules va Indexes

Tu repository root:

```bash
firebase deploy --only firestore
```

Hoac deploy rieng:

```bash
firebase deploy --only firestore:rules
firebase deploy --only firestore:indexes
```

Tai sao:
- Rules la lop bao mat quan trong nhat, can dong bo theo code truoc khi release.
- Indexes dam bao cac query co `where/orderBy` chay duoc va dung toc do.

Neu gap loi `HTTP Error: 403` (`serviceusage.googleapis.com`), cap IAM cho service account:

1. `roles/serviceusage.serviceUsageConsumer`
2. `roles/datastore.owner` (hoac custom role tuong duong)
3. `roles/firebaserules.admin` (neu phan quyen theo tach biet)

Sau do doi vai phut cho IAM propagation roi deploy lai.

### Buoc 6 - Verify tren Firebase Console

1. Firestore -> Data:
   - Kiem tra cac collections da co du du lieu seed.
2. Firestore -> Rules:
   - Kiem tra rules da publish khop [firestore.rules](firestore.rules).
3. Firestore -> Indexes:
   - Kiem tra index status la `Enabled`.

Tai sao:
- Deploy thanh cong tren CLI chua chac nghia la moi tai nguyen da san sang ngay lap tuc.
- Verify tren console giup bat loi som (index chua build xong, rules publish sai file, ...).

## 4) So do tong quat quan he giua collections

### Mermaid ER-style diagram

```mermaid
flowchart LR
    U[users\nuid, email, displayName] --> TM[team_members\nteamId, userId, role, status]
    T[teams\nteamId, name, ownerUserId] --> TM

    T --> TI[team_invites\nteamId, inviteeEmail, status]
    U --> TI

    T --> P[products\nteamId, ...]
    U --> P

    T --> O[orders\nteamId, createdBy, ...]
    U --> O
    O --> OI[orders/{orderId}/items\nproductId, qty, price]

    O --> PAY[payments\norderId, teamId, status, ...]
    T --> PAY
```

### Doc nhanh y nghia quan he

1. `users` la danh tinh toan cuc theo Firebase Auth UID.
2. `teams` dai dien don vi kinh doanh/nhom lam viec.
3. `team_members` la bang map N-N giua user va team, dong thoi chua role.
4. `team_invites` luu loi moi theo email truoc khi user thanh `team_members`.
5. `products`, `orders`, `payments` deu bi khoa theo `teamId` de cach ly du lieu giua cac team.
6. `orders/{orderId}/items` luu chi tiet dong hang cua moi order.

## 5) Rules behavior da trien khai

1. Team-based isolation theo `teamId`.
2. Staff chi duoc doc/ghi trong team co membership hop le.
3. Manager chi duoc thao tac trong pham vi team cua minh.
4. Invite flow ho tro manager tao invite, staff chap nhan/tu choi.
5. Master user co quyen thao tac nhay cam (nhu delete trong mot so truong hop).

## 6) Cong viec app code nen lam tiep

1. Bat buoc truyen `teamId` trong moi query repository.
2. Hoan thien UI manager moi nhan vien (`team_invites`).
3. Hoan thien UI staff chap nhan/tu choi invite.
4. Khoa chuc nang doi role tren UI theo logic master/manager.
5. Chuan hoa payment status (`pending`, `success`, `failed`, `canceled`) trong ca UI va Firestore.

## 6.1) Trang thai trien khai use case (da lam)

1. Manager GUI loi moi theo email o man `StaffManager`.
2. He thong tao dong thoi:
   - `team_invites` trang thai `pending`.
   - `team_members/{teamId_uid}` trang thai `pending`.
3. Staff khi dang nhap vao `StaffHome` se duoc hien hop thoai chap nhan/tu choi loi moi.
4. Neu staff chap nhan:
   - `team_invites.status = accepted`
   - `team_members.status = active`
   - `users.defaultTeamId` duoc cap nhat theo team moi.
5. App da doi sang query theo `teamId` cho `products` va `orders` de khop Security Rules.

## 6.2) Cach test nhanh dung use case

1. Dang nhap manager da thuoc team.
2. Vao man Quan ly nhan vien, nhap email staff da dang ky, bam Gui loi moi.
3. Dang xuat manager, dang nhap staff vua duoc moi.
4. Xac nhan hop thoai loi moi xuat hien:
   - Bam Chap nhan -> dang nhap lai, staff thay du lieu team.
   - Bam Tu choi -> loi moi chuyen `rejected`.
5. Voi staff da active team, thu tao don va doc san pham de xac nhan rules cho phep.

## 7) Security notes

1. Khong commit service-account JSON len git.
2. Giu pattern ignore key trong [.gitignore](.gitignore).
3. Neu lo key, rotate ngay lap tuc va revoke key cu.
