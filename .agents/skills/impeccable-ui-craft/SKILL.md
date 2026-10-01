---
name: impeccable-ui-craft
description: 'Chỉ dẫn và huấn luyện AI Agent thiết kế, code giao diện Web/React chuẩn mực sản phẩm cao cấp (Production-grade EdTech & AI SaaS). Kế thừa từ pbakaus/impeccable, open-design, Mobbin (Heron AI) và DESIGN.md: bài trừ AI Slop (bỏ gradient tím xanh lỗi thời, bỏ card lồng card vô tội vạ), quy chuẩn Typography, Color Tokens học thuật, cấu trúc phân cấp thị giác và 5 lệnh điều khiển /craft, /critique, /polish, /harden, /delight.'
license: MIT
metadata:
  author: AIVES Team (SWD392)
  sources:
    - https://github.com/pbakaus/impeccable
    - https://github.com/nexu-io/open-design
    - https://designmd.ai
    - https://mobbin.com
triggers:
  - "dựng giao diện"
  - "thiết kế ui"
  - "ui/ux"
  - "frontend design"
  - "polish ui"
  - "critique ui"
  - "harden ui"
  - "craft ui"
  - "impeccable"
  - "design system"
---

# 🎨 Impeccable UI/UX Craft Skill for AIVES Frontend

Kỹ năng này định hướng AI Agent thiết kế và lập trình giao diện Frontend cho hệ thống **AIVES - Smart Oral Exam System**, chuyển đổi mã nguồn từ giao diện "template AI rẻ tiền" (AI Slop) sang giao diện chuẩn sản phẩm công nghiệp thực tế (High-Stakes Examination & Educational Technology).

---

## 1. Triết Lý Bài Trừ AI Slop (Anti-AI Slop Mandate)

| Tiêu chí | ❌ AI Slop (Tuyệt đối tránh) | ✅ Chuẩn AIVES EdTech (Bắt buộc tuân thủ) |
|---|---|---|
| **Typography** | Font hệ thống mặc định (Arial, Times New Roman), hoặc lạm dụng `Inter` đơn điệu không phân cấp | Phân tầng rõ rệt: Heading dùng Font học thuật/kỹ thuật (`Plus Jakarta Sans`, `Outfit`), Body text dùng (`Be Vietnam Pro`, `Inter` hỗ trợ tiếng Việt tuyệt đối), Code & UUIDs dùng `JetBrains Mono`. Luôn khóa `line-height` và `letter-spacing` chuẩn. |
| **Bảng màu & Nền** | Gradient tím - xanh neon hào nhoáng vô nghĩa, nền đen sì `#000` hoặc trắng tinh `#FFF` trần trụi | Bảng màu học thuật đáng tin cậy: Deep Navy/Slate (`#0B132B`, `#0F172A`), Nền Canvas (`#F8F9FA`, `#F1F5F9`), Accent Sky Blue (`#0284C7`), Success Emerald (`#10B981`), Warning Amber (`#F59E0B`), Danger Rose (`#EF4444`). |
| **Bố cục (Layout)** | Lồng Card trong Card vô tội vạ (nested cards), bo tròn pill-shaped cho mọi khối hình chữ nhật | Phân vùng không gian bằng khoảng trắng (Whitespace Rhythm), đường viền siêu mảnh (`border border-slate-200/90`), bo góc có phân cấp (`rounded-xl` cho controls, `rounded-2xl` cho cards, `rounded-full` chỉ cho pills/badges). |
| **Độ tương phản** | Chữ xám nhạt trên nền xám/màu gây mờ mắt | Đạt tối thiểu WCAG AA (4.5:1 cho text thường, 3:1 cho text lớn). Text phụ tối thiểu `#64748B` trên nền sáng. |
| **Phòng vệ trạng thái** | Chỉ vẽ Happy Path, thiếu trạng thái biên | Đủ 6 trạng thái bắt buộc: Default, Hover, Focus (Ring 2px rõ nét), Active, Disabled, Loading Skeleton, Empty Data, Error State. |

---

## 2. Hệ Thống 5 Lệnh Thiết Kế (The Impeccable Command Matrix)

Khi cần AI can thiệp vào giao diện, kích hoạt theo các lệnh chuẩn sau:

### `/craft <screen-or-component>`
Dựng mới toàn diện một màn hình/component từ yêu cầu nghiệp vụ:
1. **Khóa Semantic HTML:** Dùng thẻ ngữ nghĩa (`<header>`, `<nav>`, `<aside>`, `<main>`, `<dialog>`, `<table>`).
2. **Khóa Visual Hierarchy:** Tiêu đề cấp 1 > Bộ lọc & Thống kê > Vùng trọng tâm tác vụ > Bảng dữ liệu > Phân trang.
3. **Áp Design Tokens:** Sử dụng màu và font chuẩn từ `tailwind.config.js` hoặc `DESIGN.md`.

### `/critique <file-or-screen>`
Đóng vai trò Chuyên gia Thiết kế Sản phẩm (Product Design Lead) soi xét:
- Độ tương phản màu sắc đạt WCAG AA chưa?
- Có bị lạm dụng border hay shadow quá đậm không?
- Khoảng cách padding/margin có tuân theo hệ số 4px/8px không?
- Layout có bị vỡ tràn ngang khi thu nhỏ xuống 1024px không?

### `/polish <file-or-screen>`
Tinh chỉnh chi tiết vi mô (Micro-refinements):
- Đồng bộ `border-radius`: tất cả input, select, button trong cùng form phải cùng bán kính bo góc.
- Bổ sung hiệu ứng tương tác: `hover:bg-slate-50`, `active:scale-[0.98]`, `transition-all duration-150`.
- Thêm focus ring sắc sảo: `focus:outline-none focus:ring-2 focus:ring-sky-500`.

### `/harden <file-or-screen>`
Phòng vệ các trạng thái thực tế khắc nghiệt:
- Khi mạng mất kết nối hoặc API sập: Hiển thị Banner cảnh báo + Nút Thử lại + Chế độ Offline Demo.
- Khi dữ liệu đang tải: Dùng `SkeletonRows` hoặc `CardSkeleton` mượt mà, không giật giật màn hình.
- Khi danh sách trống: Hiển thị Empty State trang nhã kèm nút kêu gọi hành động (Call To Action).
- Khi văn bản quá dài: Áp dụng `truncate`, `max-w-[...]`, `break-words`.

### `/delight <target>`
Bổ sung điểm nhấn trải nghiệm người dùng:
- Toast thông báo kết quả với thanh đếm ngược tự tắt.
- Chuyển tab mượt mà bằng CSS transitions.
- Badge trạng thái đập nhịp nhẹ nhàng (`animate-pulse`).

---

## 3. Quy Chuẩn Thành Phần Giao Diện AIVES (Component Specs)

### A. Thanh Điều Hướng Trên (Top App Bar)
- Chiều cao cố định `h-16`, dính đỉnh `sticky top-0 z-40`, bóng mờ tinh tế `shadow-md`.
- Nền Deep Navy `#0B132B`, đường viền dưới `border-slate-700/80`.
- Chứa: Logo nhận diện AIVES, Huy hiệu phân hệ (RBAC / Telemetry / Voice Lab), Trạng thái kết nối Backend (Live 8080 / Demo), Nút tác vụ nhanh, và Capsule hồ sơ tài khoản hiện tại.

### B. Bảng Dữ Liệu Học Thuật (Academic Data Table)
- Nền thẻ trắng `#FFFFFF`, viền bo góc `rounded-2xl border border-slate-200/90`.
- Header bảng: `bg-slate-50 text-slate-500 uppercase tracking-wider text-[11px] font-bold`.
- Hàng dữ liệu: Avatar chữ cái đầu (Initials Avatar), Email font mono, huy hiệu vai trò có viền mảnh, trạng thái chấm tròn màu.
- Hàng hover: `hover:bg-slate-50/80 transition-colors`.
- Thanh phân trang gắn liền dưới chân bảng: thể hiện số lượng bản ghi, nút Trước/Sau, danh sách số trang có ellipsis.

### C. Hộp Thoại (Modal Dialogs)
- Lớp phủ nền mờ làm tối: `fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4`.
- Khung hộp thoại: `bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden`.
- Header Deep Navy đồng nhất màu hệ thống, có icon đại diện và nút đóng `X`.
- Phím tắt bàn phím: Luôn bắt sự kiện phím `Escape` để đóng modal an toàn.

---

## 4. Checklist Tự Kiểm Tra (Impeccable Self-Review)

Trước khi bàn giao mã nguồn UI, kiểm tra danh sách 5 câu hỏi vàng:
1. [ ] Màn hình có toát lên vẻ nghiêm túc của phần mềm thi cử học thuật đại học, không phải landing page thương mại?
2. [ ] Tất cả nút bấm, input, select có đủ 5 trạng thái (Default, Hover, Focus, Active, Disabled)?
3. [ ] Có trạng thái Loading Skeleton và Empty Data State khi chưa có dữ liệu không?
4. [ ] Mọi trường số điện thoại, email, mật khẩu có validation và hiển thị lỗi thân thiện bằng tiếng Việt không?
5. [ ] Đã kiểm tra tương thích hiển thị trên màn hình laptop (1280px) không bị tràn ngang (`overflow-x-hidden`)?
