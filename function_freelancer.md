🔹 FR-01: Quản lý tài khoản người dùng

Chức năng hệ thống

Đăng ký tài khoản

Đăng ký bằng email + mật khẩu

Xác thực email

Gán role: FREELANCER | EMPLOYER

Đăng nhập

JWT Authentication

Refresh token

Phân quyền truy cập

Freelancer / Employer / Admin

Quản lý bảo mật

Đổi mật khẩu

Bật / tắt 2FA (TOTP)

Xác minh 2FA khi:

Rút tiền

Chuyển Points

Quản lý hồ sơ cá nhân

Avatar

Thông tin liên hệ

Quốc gia / múi giờ

Audit log

Ghi nhận các hành động nhạy cảm (login, change password, 2FA)

🔹 FR-02: Hồ sơ Freelancer

Chức năng

Tạo & chỉnh sửa hồ sơ nghề nghiệp

Tiêu đề nghề nghiệp

Mô tả bản thân

Quản lý kỹ năng

Danh sách skills

Level / kinh nghiệm

Portfolio

Thêm link dự án

Upload file demo

Lịch sử làm việc

Danh sách project đã hoàn thành

Thu nhập theo thời gian

Đánh giá

Hiển thị rating & review từ Employer

🔹 FR-03: Hồ sơ Employer

Chức năng

Quản lý thông tin cá nhân / công ty

Danh sách job đã đăng

Danh sách project đang chạy

Lịch sử thuê Freelancer

Đánh giá Freelancer sau khi kết thúc project

3.2. Module Project Management
🔹 FR-04: Đăng và quản lý Job

Chức năng

Employer đăng job

Tiêu đề

Mô tả

Ngân sách dự kiến

Deadline

Chỉnh sửa / đóng job

Freelancer xem danh sách job

Lọc theo kỹ năng / ngân sách

Freelancer gửi proposal

Giá đề xuất

Thời gian thực hiện

Nội dung đề xuất

🔹 FR-05: Hệ thống Đấu thầu (Bidding System)

Chức năng

Freelancer gửi proposal

Employer:

Xem danh sách proposal

Chat thương lượng

Chấp nhận / từ chối proposal

Khi chấp nhận:

Proposal → trạng thái AWARDED

Job → chuyển thành PROJECT

🔹 FR-06: Quản lý Dự án (Workspace)

Chức năng

Tạo Workspace khi award

Thành phần Workspace:

Danh sách milestone

Trạng thái dự án

File & tài liệu

Chat dự án

Quản lý milestone

Tạo milestone

Sửa / xóa (trước khi fund)

Theo dõi tiến độ

Freelancer submit work

Employer review & approve

3.3. Module Payment System (CORE)
🔹 FR-07: Nạp tiền (Deposit)

Chức năng

Tạo địa chỉ nạp USDT (TRC20)

Mỗi user có sub-wallet

Quét giao dịch blockchain

Detect USDT transfer

Chờ confirmations

Quy đổi USDT → Points (1:1)

Credit Points vào ví Employer

Quản lý lịch sử nạp tiền

TX hash

Amount

Status (Pending / Confirmed)

🔹 FR-08: Hệ thống Escrow

Chức năng

Fund milestone (Escrow Lock)

Employer dùng Points để lock

Quản lý trạng thái Escrow

LOCKED

RELEASED

REFUNDED

Giải ngân milestone

Employer approve

Chuyển Points cho Freelancer

Trừ platform fee

Hoàn tiền Escrow

Admin xử lý dispute

Points trả lại Employer

Ledger & Audit

Ghi nhận mọi thay đổi số dư

🔹 FR-09: Chuyển tiền nội bộ (P2P Points)

Chức năng

User chuyển Points cho user khác

Điều kiện:

Đủ số dư

Xác thực mật khẩu / 2FA

Ghi nhận giao dịch

Người gửi

Người nhận

Amount

Lưu lịch sử giao dịch P2P

⚠️ (Có thể disable trong MVP nếu muốn đơn giản)

🔹 FR-10: Rút tiền (Withdrawal)

Chức năng

Tạo yêu cầu rút Points

Nhập amount

Nhập địa chỉ USDT TRC20

Xác thực bảo mật

Mật khẩu

2FA

Lock Points khi tạo yêu cầu

Xử lý on-chain

Ký & broadcast giao dịch

Hoàn tất / hoàn tiền khi lỗi

Hiển thị:

Phí rút

Trạng thái giao dịch

3.4. Module Communication
🔹 FR-11: Chat thời gian thực

Chức năng

Chat trong Workspace

Gửi:

Text

File

Image

Lưu lịch sử chat

Phân quyền:

Chỉ user trong project mới chat được

🔹 FR-12: Hệ thống Thông báo (Notification)

Chức năng

Thông báo sự kiện:

Deposit thành công

Milestone funded

Milestone released

Withdrawal completed

Kênh thông báo:

In-app

Email

Đánh dấu đã đọc / chưa đọc

3.5. Module Admin
🔹 FR-13: Quản lý người dùng

Chức năng

Xem danh sách user

Khóa / mở tài khoản

Thay đổi role

Xem lịch sử hoạt động user

🔹 FR-14: Quản lý giao dịch

Chức năng

Theo dõi deposit

Theo dõi withdrawal

Theo dõi escrow

Retry giao dịch lỗi

Báo cáo:

Doanh thu platform

Tổng volume giao dịch

🔹 FR-15: Giải quyết tranh chấp

Chức năng

Nhận khiếu nại

Xem toàn bộ lịch sử:

Chat

Milestone

Escrow

Quyết định:

Refund

Partial release

Ghi audit & lý do xử lý