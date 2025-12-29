# 🚀 Quy trình làm việc Code-First với JPA Auto-DDL

## 📋 Tổng quan

Project này đã được cấu hình để **Java Entity tự động đồng bộ với Database**. Bạn không cần phải vào DBeaver để sửa bảng thủ công nữa!

## ✅ Đã cấu hình sẵn

Các file sau đã có cấu hình `ddl-auto: update`:
- ✅ `application.yml`
- ✅ `application-dev.yml` 
- ✅ `application-prod.yml`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # ← Tự động thêm cột/bảng mới
    show-sql: true      # ← Hiển thị SQL để debug
```

## 🔄 Quy trình làm việc mới

### Trước đây (Cách cũ - THỦ CÔNG ❌)
1. Sửa file Java Entity
2. Mở DBeaver
3. Chạy `ALTER TABLE ...` thủ công
4. Quay lại code tiếp

### Bây giờ (Cách mới - TỰ ĐỘNG ✅)
1. Sửa file Java Entity (hoặc bảo Copilot sửa)
2. Bấm **Run Project**
3. → Spring Boot tự động update DB
4. Done! ✨

---

## 📝 Ví dụ thực tế

### Ví dụ 1: Thêm cột mới vào bảng User

**Prompt cho Copilot:**
```
Thêm trường phoneNumber (String, max 20 ký tự) vào entity User
```

**Copilot sẽ sửa file `User.java`:**
```java
@Column(name = "phone_number", length = 20)
private String phoneNumber;
```

**Bạn làm:**
- Bấm **Run** hoặc **Restart** Spring Boot

**Spring Boot sẽ tự động chạy:**
```sql
ALTER TABLE users ADD COLUMN phone_number VARCHAR(20);
```

✅ **Không cần mở DBeaver!**

---

### Ví dụ 2: Tạo Entity mới (Bảng mới)

**Prompt cho Copilot:**
```
Tạo entity RefundTransaction với các trường:
- id (Long, auto-increment)
- transactionId (Foreign key đến Transaction)
- reason (String, 500 ký tự)
- refundStatus (enum: PENDING, APPROVED, REJECTED)
- refundAmount (BigDecimal)
- createdAt, updatedAt
```

**Bạn làm:**
- Bấm **Run** Spring Boot

**Spring Boot sẽ tự động:**
```sql
CREATE TABLE refund_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_id BIGINT,
    reason VARCHAR(500),
    refund_status VARCHAR(20),
    refund_amount DECIMAL(20,6),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);
```

---

## 🧠 Giúp Copilot hiểu Database (Context)

### Tại sao cần file `schema.sql`?

Copilot không thể "nhìn thấy" database trực tiếp. Để nó code logic JOIN bảng, query phức tạp đúng, nó cần biết DB hiện tại trông như thế nào.

### Cách cập nhật `sql/schema.sql`

**Chỉ cần làm 1 lần hoặc khi có thay đổi lớn:**

1. Mở **DBeaver** → Kết nối database `walletdb`
2. Chuột phải vào database → **Tools** → **Generate SQL** → **DDL**
3. Cấu hình:
   - ✅ Chọn: **Tables**
   - ✅ Chọn: **Primary Keys, Foreign Keys**
   - ❌ Bỏ chọn: **Data** (không cần dữ liệu)
   - ❌ Bỏ chọn: **DROP statements**
4. Copy toàn bộ SQL → Paste vào `/sql/schema.sql`

### Sử dụng với Copilot

**Prompt tốt:**
```
@workspace Dựa trên sql/schema.sql và entity Transaction, 
viết API lấy danh sách giao dịch của user có role FREELANCER 
với tổng amount > 5000
```

**Copilot sẽ:**
- Đọc `schema.sql` để hiểu quan hệ giữa bảng
- Đọc Entity Java để biết tên field
- Viết query JOIN chính xác

---

## ⚙️ Các chế độ `ddl-auto`

| Chế độ | Tác dụng | Khi nào dùng |
|--------|----------|--------------|
| `none` | Không làm gì | Production (dùng Flyway) |
| `validate` | Chỉ kiểm tra, không sửa | CI/CD testing |
| `update` | ✅ **Thêm cột/bảng mới, giữ data** | **Development (đang dùng)** |
| `create` | Xóa DB và tạo lại | Testing tạm thời |
| `create-drop` | Xóa DB khi tắt app | Testing unit |

---

## 🔧 Xử lý sự cố

### ❗ Copilot sửa Entity nhưng DB không thay đổi?

**Nguyên nhân:** Spring Boot chưa chạy lại hoặc có lỗi kết nối DB.

**Cách fix:**
1. Kiểm tra log console có lỗi không
2. **Restart** Spring Boot (không phải reload file)
3. Kiểm tra DB connection trong `application-dev.yml`

---

### ❗ DBeaver không thấy cột mới?

**Nguyên nhân:** DBeaver cache cũ.

**Cách fix:**
- Chuột phải vào bảng → **Refresh** (hoặc phím **F5**)

---

### ❗ Muốn rename cột hoặc thay đổi kiểu dữ liệu?

**Cảnh báo:** `ddl-auto: update` **không thể** rename hoặc thay đổi kiểu dữ liệu tự động.

**Cách xử lý:**
1. **Development:** Vào DBeaver chạy `ALTER TABLE` thủ công
2. **Production:** Dùng Flyway migration (xem file `WORKFLOW_FLYWAY.md`)

---

## 📌 Lưu ý quan trọng

### ✅ Development (Đang dùng)
- `ddl-auto: update` → **BẬT**
- `show-sql: true` → Xem SQL chạy ngầm
- Rất nhanh, thuận tiện khi code

### ⚠️ Production (Khi deploy thật)
- `ddl-auto: validate` hoặc `none`
- Dùng **Flyway** để quản lý migration
- Lý do: Tránh Spring Boot tự sửa DB production, nguy hiểm

---

## 🎯 Checklist hàng ngày

Khi làm việc với feature mới:

- [ ] Prompt Copilot: Mô tả tính năng cần code
- [ ] Copilot sửa Entity/Service/Controller
- [ ] **Run Spring Boot** (DB tự động update)
- [ ] Test API bằng Postman/Thunder Client
- [ ] (Optional) Mở DBeaver → F5 để xem bảng đã đổi

---

## 🔗 Tài liệu liên quan

- [application-dev.yml](../backend/src/main/resources/application-dev.yml) - Cấu hình dev
- [schema.sql](../sql/schema.sql) - Schema hiện tại (cho Copilot)
- [function_freelancer.md](../function_freelancer.md) - Danh sách chức năng

---

**🎉 Chúc code vui vẻ! Giờ đây bạn không phải lo lắng về việc quên sửa DB nữa!**
