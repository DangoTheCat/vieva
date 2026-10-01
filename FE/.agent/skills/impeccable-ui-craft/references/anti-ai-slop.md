# Sổ Tay Bài Trừ AI Slop Trong Thiết Kế Giao Diện (Anti-AI Slop Guide)

Kế thừa từ triết lý của **Paul Bakaus (Impeccable)** và các bộ quy tắc thiết kế công nghiệp:

## 1. Dấu Hiệu Nhận Biết AI Slop (Những Thứ Phải Loại Bỏ Ngay)
1. **Gradient Tím - Hồng - Xanh Neon vô tội vạ**:
   - AI hay sinh các mảng background `bg-gradient-to-r from-purple-500 via-pink-500 to-indigo-500` cho mọi nút bấm hoặc background. Điều này biến giao diện học thuật thành website bán hàng tiền điện tử lừa đảo.
   - *Cách khắc phục*: Dùng màu đơn sắc học thuật vững chắc: Deep Slate `#0F172A`, Deep Navy `#0B132B`, Accent Sky Blue `#0284C7`.
2. **Card Lồng Card (Infinite Card Nesting)**:
   - Một trang có card ngoài cùng, bên trong lồng 3 card con, mỗi card con lại chứa 2 card cháu, tất cả đều có `border` và `shadow` dày cộm.
   - *Cách khắc phục*: Dùng khoảng trắng (Whitespace), vạch phân cách mỏng `border-b border-slate-100`, hoặc đổi nền nhẹ `bg-slate-50/50`.
3. **Typography Không Có Cá Tính Học Thuật**:
   - Dùng font mặc định hoặc chỉ dùng 1 font Inter với font-weight 400 cho mọi thẻ `p`, `h1`, `h2`.
   - *Cách khắc phục*: Tiêu đề dùng font `Plus Jakarta Sans` với độ đậm `font-extrabold` (700/800), giãn cách chữ nhẹ `tracking-tight`. Thân bài dùng `Be Vietnam Pro` hỗ trợ gõ dấu tiếng Việt hoàn hảo.
4. **Thiếu Trạng Thái Biên (State Amnesia)**:
   - Giao diện trông rất đẹp khi có đúng 5 dòng dữ liệu mẫu, nhưng khi mạng rớt hoặc danh sách rỗng 0 phần tử thì trắng xóa màn hình hoặc lỗi runtime `undefined`.
   - *Cách khắc phục*: Luôn bọc điều kiện kiểm tra, bổ sung Skeleton Loader và Empty State.
