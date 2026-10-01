---
name: gsap-motion-craft
description: 'Chỉ dẫn và huấn luyện AI Agent thiết kế hoạt ảnh (Motion & Animation) chuyên nghiệp cho React/Web bằng GSAP 3 và CSS GPU-accelerated micro-interactions. Kế thừa từ greensock/gsap-skills và Emil Kowalski: nguyên tắc chuyển động có chủ đích, tối ưu hóa 60fps không giật khung hình, Audio Waveform sóng giọng nói cho phòng thi vấn đáp và hiệu ứng tương tác nút bấm, modals.'
license: MIT
metadata:
  author: AIVES Team (SWD392)
  sources:
    - https://github.com/greensock/gsap-skills
triggers:
  - "gsap"
  - "motion"
  - "animation"
  - "audio wave"
  - "voice waveform"
  - "micro interaction"
  - "hoạt ảnh"
---

# 🎬 GSAP Motion & Micro-Interactions Skill

Kỹ năng này huấn luyện AI Agent áp dụng các kỹ thuật chuyển động tinh tế, mượt mà và tối ưu hiệu năng đồ họa cao nhất (60 FPS) cho hệ thống **AIVES** mà không làm phân tán sự tập trung của sinh viên và giảng viên.

---

## 1. Triết Lý Chuyển Động Có Chủ Đích (Purposeful Motion)

Trong phần mềm khảo thí và giáo dục đại học, chuyển động **chỉ tồn tại để cung cấp phản hồi thị giác**, tuyệt đối không dùng hiệu ứng bay nhảy màu mè gây mất tập trung:

1. **Phản hồi tức thời (Feedback):** Nút bấm lún nhẹ khi click (`active:scale-[0.98]`, thời lượng `100ms - 150ms`).
2. **Định hướng không gian (Orientation):** Modal phóng to nhẹ từ tâm (`scale: 0.95 -> 1.0`, opacity `0 -> 1`), thời lượng `150ms - 200ms`.
3. **Biểu thị trạng thái liên tục (State Indication):**
   - Đang kết nối / xử lý: Spinner xoay đều nhẹ nhàng.
   - Sóng giọng nói (Voice Waveform): Các cột sóng dao động theo cường độ âm thanh thời gian thực.

---

## 2. Quy Tắc Tối Ưu Hóa GPU 60 FPS (Hardware Acceleration)

Để tránh hiện tượng tụt khung hình (Frame Drop) và giật lag trình duyệt:

| Thuộc tính ❌ CẤM Animate (Gây Reflow/Layout Thrashing) | Thuộc tính ✅ BẮT BUỘC Dùng (Đẩy vào GPU Compositor) |
|---|---|
| `height`, `max-height` (Ví dụ: cột sóng âm thanh) | `transform: scaleY()` |
| `top`, `bottom`, `left`, `right` | `transform: translate3d(x, y, 0)` hoặc `translate()` |
| `width`, `margin`, `padding` | `transform: scaleX()` hoặc padding tĩnh |
| `border-width` | `opacity` hoặc `box-shadow` mỏng |

---

## 3. Mẫu Triển Khai Sóng Âm Thanh Giọng Nói 60 FPS (Audio Waveform in React)

Khi dựng giao diện phòng thi vấn đáp (Module 3) hoặc phòng thử giọng AI (Voice Lab Module 7):

```jsx
import React, { useEffect, useRef } from 'react';

export function VoiceWaveform({ isSpeaking, volume = 0.5 }) {
  const barsRef = useRef([]);

  useEffect(() => {
    if (!isSpeaking) {
      barsRef.current.forEach(bar => {
        if (bar) bar.style.transform = 'scaleY(0.15)';
      });
      return;
    }

    let animationFrameId;
    const animate = () => {
      barsRef.current.forEach((bar, index) => {
        if (bar) {
          // Tính toán chiều cao dựa trên sin wave + volume
          const time = Date.now() * 0.006;
          const heightFactor = Math.abs(Math.sin(time + index * 0.4)) * volume + 0.2;
          bar.style.transform = `scaleY(${Math.min(1.0, heightFactor)})`;
        }
      });
      animationFrameId = requestAnimationFrame(animate);
    };

    animationFrameId = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(animationFrameId);
  }, [isSpeaking, volume]);

  return (
    <div className="flex items-center gap-1 h-8 px-3 bg-slate-900/60 rounded-xl border border-slate-700">
      {Array.from({ length: 12 }).map((_, i) => (
        <span
          key={i}
          ref={el => barsRef.current[i] = el}
          className="w-1 bg-sky-400 rounded-full transition-transform origin-bottom duration-75"
          style={{ height: '24px', transform: 'scaleY(0.15)' }}
        />
      ))}
    </div>
  );
}
```

---

## 4. Đường Cong Chuyển Động Chuẩn (Standard Easing Curves)

- **Modal & Popups:** `cubic-bezier(0.16, 1, 0.3, 1)` (Spring-like smooth deceleration).
- **Hover & Tooltip:** `cubic-bezier(0.4, 0, 0.2, 1)` (Standard Material ease-in-out).
- **Toast In / Out:** `duration: 200ms`, `ease: power2.out`.
