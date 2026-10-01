---
name: fe-resilience-hardening
description: 'Chỉ dẫn và huấn luyện AI Agent phòng vệ các trạng thái khắc nghiệt (Hardening) cho Frontend Web/React: cơ chế mất kết nối mạng (Network Offline & Reconnect Recovery), chế độ chạy ngoại tuyến (Demo Mode Fallback), ánh xạ mã lỗi Backend (ErrorCode Translation 1001-1017, 4090, 9999), xác thực an toàn thao tác dữ liệu (xóa/khóa/hạ quyền Admin) và kiểm soát tràn vỡ khung hình (Text Overflow & Layout Protection).'
license: MIT
metadata:
  author: AIVES Team (SWD392)
triggers:
  - "harden fe"
  - "phòng ngự giao diện"
  - "xử lý lỗi backend"
  - "error codes"
  - "mất mạng"
  - "reconnect"
  - "resilience"
  - "safety guards"
---

# 🛡️ Frontend Resilience & Hardening Skill

Kỹ năng này huấn luyện AI Agent đảm bảo ứng dụng Frontend **AIVES** hoạt động bền bỉ, kiên cường, không bị treo hoặc vỡ màn hình dưới các điều kiện mạng yếu, rớt kết nối, dữ liệu bất thường hoặc thao tác nguy hiểm của người dùng.

---

## 1. 5 Trụ Cột Phòng Vệ Giao Diện (The 5 Resilience Pillars)

### A. Phòng Vệ Mạng & Ngoại Tuyến (Network & Offline Resilience)
- **Tình huống:** Máy chủ Backend Spring Boot chưa chạy (hoặc gặp lỗi rớt mạng), khiến các yêu cầu API trả về lỗi mạng (Network Error / Status 0).
- **Quy chuẩn xử lý:**
  1. Tuyệt đối không để ứng dụng sụp đổ (white screen of death).
  2. Bắt lỗi tại tầng `ApiClient`, hiển thị Banner màu vàng cảnh báo kèm nút **"Thử lại"**.
  3. Cung cấp nút chuyển đổi sang **Chế độ Thử Nghiệm (Demo Mode Fallback)** để người dùng hoặc Giảng viên chấm thi có thể trải nghiệm toàn diện luồng nghiệp vụ với dữ liệu mẫu khởi tạo.
  4. Thanh Navbar phải luôn có chỉ thị màu rõ ràng:
     - 🟢 `BE Live (8080)`: Kết nối Backend thành công.
     - 🟡 `Demo Mode`: Đang dùng dữ liệu giả lập.
     - 🔴 `BE Offline`: Máy chủ Backend chưa khả dụng.

### B. Ánh Xạ Mã Lỗi Nghiệp Vụ (Backend ErrorCode Translation)
- **Tình huống:** Backend Spring Boot trả về `ErrorResponse` dạng `{ code: "1016", message: "You cannot lock or deactivate your own account" }`.
- **Quy chuẩn xử lý:**
  - Không hiển thị nguyên văn chuỗi tiếng Anh thô cứng cho người dùng Việt Nam.
  - Sử dụng từ điển ánh xạ tập trung tại `src/utils/errorCodes.js` để trả về câu thông báo sư phạm:

| Mã Lỗi (Code) | Ý Nghĩa Kỹ Thuật | Thông Báo Hiển Thị Người Dùng (Gợi Ý Hành Động) |
|---|---|---|
| `1001` | `USER_NOT_FOUND` | Không tìm thấy người dùng trong hệ thống. |
| `1002` | `USER_EXISTED` | Email này đã tồn tại trên hệ thống. Vui lòng dùng email khác. |
| `1003` | `UNAUTHENTICATED` | Phiên đăng nhập đã hết hạn. Đang chuyển hướng về cổng đăng nhập. |
| `1004` | `UNAUTHORIZED` | Bạn không có quyền thực hiện thao tác này (Yêu cầu quyền Administrator). |
| `1009` | `INCORRECT_PASSWORD` | Mật khẩu hiện tại không chính xác. Vui lòng kiểm tra lại. |
| `1010` | `PASSWORD_UNCHANGED`| Mật khẩu mới phải khác với mật khẩu cũ hiện tại. |
| `1011` | `CANNOT_DELETE_SELF`| Bạn không thể tự xóa tài khoản của chính mình. |
| `1012` | `CANNOT_DEMOTE_SELF`| Bạn không thể tự hạ quyền Administrator của chính mình. |
| `1013` | `CANNOT_DELETE_LAST_ADMIN`| Không thể xóa quản trị viên (Admin) duy nhất còn lại trong hệ thống. |
| `1014` | `CANNOT_DEMOTE_LAST_ADMIN`| Không thể hạ quyền quản trị viên (Admin) duy nhất còn lại trong hệ thống. |
| `1015` | `USER_CODE_EXISTED` | Mã người dùng (User Code / MSSV) đã tồn tại. Vui lòng nhập mã khác. |
| `1016` | `CANNOT_LOCK_SELF`  | Bạn không thể tự khóa hoặc vô hiệu hóa tài khoản của chính mình. |
| `1017` | `CANNOT_LOCK_LAST_ADMIN` | Không thể khóa hoặc vô hiệu hóa quản trị viên (Admin) duy nhất đang hoạt động. |
| `4090` | `CONCURRENCY_CONFLICT` | Xung đột dữ liệu đồng thời do phiên làm việc khác vừa sửa. Vui lòng tải lại trang. |

### C. Ngăn Chặn Thao Tác Nguy Hiểm & Mất Dữ Liệu (Accidental Data Loss Prevention)
- **Quy tắc bắt buộc:**
  - Mọi thao tác xóa (Soft Delete / Hard Delete) bắt buộc phải qua `ConfirmModal`.
  - Nút xóa tài khoản của chính mình phải ở trạng thái `disabled` và có tooltip cảnh báo ngay trên giao diện trước khi gửi request xuống server.
  - Khi sửa tài khoản của chính Admin: Dropdown trạng thái bị vô hiệu hóa kèm thông báo nhắc nhở nhẹ nhàng.

### D. Kiểm Soát Tràn Vỡ Bố Cục (Text Overflow & Layout Hardening)
- **Tên người dùng dài hoặc email lạ:** Áp dụng `truncate`, `max-w-[200px]`, `whitespace-nowrap`.
- **Khóa định danh UUID:** Đặt trong thẻ font mono có `select-all` và cung cấp nút **"Sao chép" (Copy to clipboard)** kèm hiệu ứng đổi icon sang dấu tick xanh trong 2 giây.
- **Bảng dữ liệu nhiều cột:** Bắt buộc bao quanh bằng `<div class="overflow-x-auto">` để có thanh cuộn ngang khi thu nhỏ màn hình, không làm vỡ bố cục tổng thể.

### E. Trợ Năng & Tương Tác Bàn Phím (Accessibility & Keyboard A11y)
- Nhấn phím `Escape` bất cứ lúc nào cũng phải đóng các Modal đang mở.
- Nhấn phím `Enter` trong form phải kích hoạt nút submit chính.
- Mọi ô input khi được focus phải có viền sáng rõ nét: `focus:outline-none focus:ring-2 focus:ring-sky-500`.
