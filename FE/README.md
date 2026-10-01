# 🎙️ AIVES Frontend — Admin Center & User Management (SWD392)

Giao diện quản trị người dùng, phân quyền môn học (Course-Scoped RBAC) và cổng xác thực cho hệ thống **AIVES - Artificial Intelligence Voice Evaluation System**.

Mã nguồn Frontend được xây dựng hoàn toàn tương thích với các API hiện có của Backend Spring Boot (`/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/admin/users/**`) và tuân thủ các quy tắc thiết kế học thuật chống AI-Slop tại `.agent/skills/aives-ui-ux-craft`.

---

## 🚀 Các Tính Năng Đã Hoàn Thiện

### 1. 🛡️ Quản Trị Người Dùng & Phân Quyền Theo Môn (Admin User Management)
- **Tương thích API:** `GET /api/v1/admin/users`, `GET /api/v1/admin/users/{id}`, `POST /api/v1/admin/users`, `PUT /api/v1/admin/users/{id}`, `DELETE /api/v1/admin/users/{id}`
- **Tra cứu & Bộ lọc đa chiều:**
  - Tìm kiếm tức thời theo họ tên, email `@fpt.edu.vn`, mã số sinh viên / mã giảng viên (`userCode`).
  - Lọc theo vai trò hệ thống (`ROLE_ADMIN`, `ROLE_USER`).
  - Lọc theo trạng thái tài khoản (`ACTIVE`, `INACTIVE`, `BANNED`, `DELETED`).
  - Sắp xếp linh hoạt theo thời gian tạo (`createdAt`), họ tên, email hoặc trạng thái với hướng `ASC` / `DESC`.
  - Phân trang đầy đủ (`page`, `size`, `totalElements`, `totalPages`, `isFirst`, `isLast`).
- **Thêm mới người dùng (`POST /api/v1/admin/users`):**
  - Hỗ trợ nhập email, mật khẩu khởi tạo (tối thiểu 6 ký tự), họ tên, số điện thoại, mã số tùy chỉnh hoặc tự sinh `USR-XXXXXXXX`, trạng thái và gán vai trò (`ROLE_ADMIN`, `ROLE_USER`).
- **Chỉnh sửa & Phân quyền (`PUT /api/v1/admin/users/{id}`):**
  - Chỉnh sửa họ tên, số điện thoại, trạng thái và vai trò.
  - **Quy tắc an toàn chống thao tác nhầm (Safety Guards):**
    - Không cho phép Admin tự khóa / vô hiệu hóa tài khoản của chính mình (Error Code `1016`).
    - Không cho phép tự hạ quyền Admin của chính mình (Error Code `1012`).
    - Ngăn chặn khóa hoặc hạ quyền Admin cuối cùng của hệ thống (Error Code `1014`, `1017`).
- **Xóa mềm (`DELETE /api/v1/admin/users/{id}`):**
  - Modal xác nhận hành động nguy hiểm.
  - Ngăn chặn Admin tự xóa chính mình (Error Code `1011`) và xóa Admin cuối cùng (Error Code `1013`).
- **Xem chi tiết hồ sơ:** Tra cứu toàn bộ thuộc tính, khóa định danh UUID, ngày tạo và cập nhật.
- **Ma trận phân quyền (RBAC Matrix Modal):** Bảng đối chiếu quyền hạn chi tiết giữa Student, Lecturer, Auditor và Admin.

### 2. 🔐 Cổng Đăng Nhập & Đăng Ký (Auth Gateway & Register)
- **Tương thích API:** `POST /api/v1/auth/login`, `POST /api/v1/auth/register`
- Thiết kế chuẩn học thuật: Deep Navy, Sky Blue, thẻ SchoolAI nổi, phân loại vai trò Sinh Viên, Giảng Viên, Khảo Thí.
- Lưu trữ Token JWT an toàn trong `localStorage` và tự động gắn vào Header `Authorization: Bearer <token>`.
- Xử lý sự kiện token hết hạn hoặc 401 Unauthorized tự động.

### 3. 👤 Quản Lý Hồ Sơ Cá Nhân & Đổi Mật Khẩu (User Profile)
- **Tương thích API:** `GET /api/v1/users/me`, `PATCH /api/v1/users/me`, `PUT /api/v1/users/me/password`
- Xem hồ sơ hiện tại, cập nhật họ tên và số điện thoại.
- Đổi mật khẩu với kiểm tra mật khẩu cũ, mật khẩu mới tối thiểu 6 ký tự và xác nhận trùng khớp.

### 4. ⚡ Cơ Chế Phòng Thủ & Kết Nối Linh Hoạt (Dual-mode Resilience)
- **Live Backend Mode:** Tự động kết nối tới Spring Boot Backend tại cổng `8080` qua cấu hình Vite Proxy (hoặc direct CORS).
- **Demo / Offline Fallback Mode:** Nếu cơ sở dữ liệu hoặc Backend chưa sẵn sàng, hệ thống cung cấp dữ liệu giả lập (mock data) đầy đủ để kiểm thử toàn diện mọi trạng thái giao diện (6 states: default, hover, focus ring, loading skeleton, empty state, error toast).
- **Ánh xạ mã lỗi (ErrorCode Mapping):** Chuyển đổi mã lỗi Backend (`1001` - `1017`, `4090`, `9999`) thành thông báo tiếng Việt sư phạm dễ hiểu.

---

## 🛠️ Công Nghệ Sử Dụng

- **Framework:** React 19 + Vite 5 (Fast HMR, tối ưu bundle size).
- **Styling:** Tailwind CSS 3 với bộ Design Tokens AIVES (`sidebarBg`, `canvasBg`, `cardBg`, `accentSky`).
- **Typography:** `Plus Jakarta Sans` (Display Headings), `Be Vietnam Pro` / `Inter` (Body Text), `JetBrains Mono` (Codes/UUIDs).
- **Iconography:** `lucide-react`.

---

## 💻 Hướng Dẫn Cài Đặt & Khởi Chạy

### 1. Yêu Cầu Tiên Quyết
- Node.js `v20.x` hoặc mới hơn.
- npm `10.x` hoặc mới hơn.

### 2. Cài Đặt Thư Viện
```bash
cd FE
npm install
```

### 3. Chạy Môi Trường Phát Triển (Dev Server)
```bash
npm run dev
```
Truy cập: `http://localhost:5173`

*(Mọi request bắt đầu bằng `/api` sẽ tự động được proxy sang `http://localhost:8080` của Backend Spring Boot).*

### 4. Đóng Gói Production (Build)
```bash
npm run build
```
Kết quả đóng gói sẽ được tạo tại thư mục `dist/`.

---

## 📋 Danh Sách Endpoint Backend Tương Thích

| Phương thức | Đường dẫn | Chức năng FE | Trạng thái tích hợp |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Đăng nhập hệ thống | ✅ Hoàn tất |
| `POST` | `/api/v1/auth/register` | Đăng ký tài khoản mới | ✅ Hoàn tất |
| `GET` | `/api/v1/users/me` | Lấy thông tin tài khoản hiện tại | ✅ Hoàn tất |
| `PATCH` | `/api/v1/users/me` | Cập nhật hồ sơ cá nhân | ✅ Hoàn tất |
| `PUT` | `/api/v1/users/me/password` | Đổi mật khẩu cá nhân | ✅ Hoàn tất |
| `GET` | `/api/v1/admin/users` | Lấy danh sách người dùng (phân trang, lọc, tìm kiếm) | ✅ Hoàn tất |
| `GET` | `/api/v1/admin/users/{id}` | Lấy chi tiết người dùng theo UUID | ✅ Hoàn tất |
| `POST` | `/api/v1/admin/users` | Admin tạo người dùng mới | ✅ Hoàn tất |
| `PUT` | `/api/v1/admin/users/{id}` | Admin cập nhật thông tin & vai trò | ✅ Hoàn tất |
| `DELETE` | `/api/v1/admin/users/{id}` | Admin xóa mềm người dùng | ✅ Hoàn tất |
