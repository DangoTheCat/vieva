# 🚀 HƯỚNG DẪN AI AGENT CHO THƯ MỤC FRONTEND AIVES (SWD392)

Tài liệu này là chỉ đạo tối cao (Master Steering Instructions) cho mọi AI Coding Agent (Antigravity, Cursor, Claude Code, GitHub Copilot) khi phân tích, thiết kế hoặc sinh mã nguồn trong thư mục **Frontend (`FE`)** của hệ thống **AIVES - Smart Oral Exam System**.

---

## 📌 1. Ngữ Cảnh Ứng Dụng (Application Essence)
- **Môn học:** SWD392 — Software Architecture and Design (Trường Đại học FPT).
- **Phân hệ:** Web Client SPA dành cho Khảo thí trực tuyến có AI Giám thị (Oral Exam Room), Quản lý người dùng & Phân quyền môn học (Admin Center RBAC), và Cổng xác thực sinh viên/giảng viên.
- **Công nghệ chính:** React 19, Vite 5, Tailwind CSS 3, Lucide React.
- **Tài liệu hệ thống thiết kế:** Xem trực tiếp tại [`./DESIGN.md`](./DESIGN.md).

---

## 🛠️ 2. Hệ Thống Kỹ Năng Đang Kích Hoạt Tại Thư Mục FE (Active Agent Skills)
Toàn bộ kỹ năng chuyên biệt đã được đóng gói trực tiếp trong thư mục `./.agent/skills/` và `./.agents/skills/`:

1. **`impeccable-ui-craft`** (`./.agent/skills/impeccable-ui-craft/SKILL.md`):
   - Kế thừa từ: **pbakaus/impeccable**, **open-design**, **Mobbin (Heron AI)** và **designmd.ai**.
   - Mục đích: Dựng giao diện học thuật cao cấp, bài trừ AI Slop (bỏ gradient neon tím-hồng-xanh, bỏ card lồng card vô tội vạ, phân cấp thị giác rõ nét).
   - 5 lệnh chuẩn: `/craft`, `/critique`, `/polish`, `/harden`, `/delight`.
2. **`fe-resilience-hardening`** (`./.agent/skills/fe-resilience-hardening/SKILL.md`):
   - Mục đích: Phòng ngự các trạng thái thực tế khắc nghiệt (rớt mạng, Backend chưa bật, xung đột phiên làm việc, lỗi nghiệp vụ).
   - Tự động fallback giữa Live Backend (cổng 8080) và Demo Mode với dữ liệu mẫu khởi tạo.
   - Ánh xạ mã lỗi Backend (`1001-1017`, `4090`, `9999`) sang tiếng Việt có hướng giải quyết cụ thể.
   - Xác thực an toàn: Modal xác nhận trước khi xóa, cấm Admin tự khóa/hạ quyền/tự xóa.
3. **`gsap-motion-craft`** (`./.agent/skills/gsap-motion-craft/SKILL.md`):
   - Kế thừa từ: **greensock/gsap-skills** và Emil Kowalski.
   - Mục đích: Chuyển động có chủ đích, tối ưu hóa phần cứng GPU 60 FPS bằng `transform` (`scaleY`, `translate3d`) và `opacity`, tránh animate `height`/`width` gây reflow.
   - Sóng âm thanh giọng nói (Audio Waveform) phục vụ phòng thi vấn đáp và Voice Lab.

---

## 📐 3. Quy Chuẩn Bắt Buộc Khi Viết Code Giao Diện (Hard Constraints)

1. **Bảng màu học thuật & Phông chữ (Design Tokens):**
   - Nền thanh Top Bar & Sidebar: Deep Navy `#0B132B`.
   - Nền Canvas trung tâm: `#F1F5F9` hoặc `#F8F9FA` có hoa văn chấm bi `.canvas-dot-grid`.
   - Nền thẻ bề mặt: `#FFFFFF` viền siêu mỏng `border border-slate-200/90` bo góc `rounded-2xl`.
   - Màu nhấn: Sky Blue `#0284C7` / `#0066FF`, Xanh lá `#10B981`, Vàng hổ phách `#F59E0B`, Đỏ cảnh báo `#EF4444`.
   - Typography: Heading dùng `Plus Jakarta Sans`, Body text dùng `Be Vietnam Pro` (hoặc `Inter`), Code/MSSV/UUID dùng `JetBrains Mono`.
2. **Đủ 6 trạng thái giao diện bắt buộc:**
   - Default, Hover, Focus (luôn có `focus:outline-none focus:ring-2 focus:ring-sky-500`), Active (`active:scale-[0.98]`), Loading Skeleton (`animate-pulse bg-slate-200/80`), Empty State thanh lịch khi danh sách trống.
3. **Tương thích API Backend:**
   - Mọi API call phải đi qua `src/api/client.js` để tự động đính kèm Token JWT `Authorization: Bearer <token>` và bắt sự kiện 401 Unauthorized.
   - Các API tương thích gồm:
     - `POST /api/v1/auth/login`, `POST /api/v1/auth/register`
     - `GET /api/v1/users/me`, `PATCH /api/v1/users/me`, `PUT /api/v1/users/me/password`
     - `GET /api/v1/admin/users`, `POST /api/v1/admin/users`, `PUT /api/v1/admin/users/{id}`, `DELETE /api/v1/admin/users/{id}`
