# Hệ Thống Design Tokens Chuẩn AIVES (Design Tokens Specification)

Bảng định nghĩa mã màu và kiểu chữ được áp dụng xuyên suốt trong Tailwind CSS:

## 1. Bảng Màu (Color Palette)

| Token Key | Giá Trị HEX | Mục Đích Sử Dụng |
|---|---|---|
| `sidebarBg` | `#0B132B` | Nền thanh điều hướng trên và thanh bên trái (High-contrast Deep Navy) |
| `sidebarHover` | `#1C2541` | Trạng thái hover của menu thanh bên |
| `canvasBg` | `#F1F5F9` | Nền canvas trung tâm (Slate 100 có hoa văn chấm bi) |
| `cardBg` | `#FFFFFF` | Nền thẻ nổi, bảng biểu, hộp thoại modal |
| `accentSky` | `#0284C7` | Nút hành động chính, viền focus, liên kết trọng tâm |
| `accentNavy` | `#0F172A` | Màu chữ tiêu đề chính, viền bảng đậm |
| `emeraldCustom` | `#10B981` | Trạng thái hoạt động tốt (ACTIVE), đạt Rubric, WebSocket online |
| `amberCustom` | `#F59E0B` | Cảnh báo, trạng thái INACTIVE, hỏi xoáy thích ứng |
| `roseCustom` | `#EF4444` | Báo động, trạng thái BANNED/DELETED, lỗi xóa dữ liệu nguy hiểm |

## 2. Phân Hệ Phông Chữ (Typography Pairing)

```css
/* Display & Headings */
font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
font-feature-settings: "cv02", "cv03", "cv04", "cv11";

/* Body Text & Data Tables */
font-family: 'Be Vietnam Pro', 'Inter', sans-serif;

/* Codes, UUIDs, Tokens & Counters */
font-family: 'JetBrains Mono', 'Fira Code', monospace;
```
