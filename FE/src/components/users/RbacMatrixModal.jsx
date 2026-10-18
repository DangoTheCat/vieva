import React, { useEffect } from 'react';
import { Table2, X, Check, Minus } from 'lucide-react';

export function RbacMatrixModal({ isOpen, onClose }) {
  // Keyboard accessibility: Escape to close
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-50 bg-ink/50 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-2xl bg-surface-container-lowest rounded-2xl border-2 border-green-700 shadow-[6px_6px_0px_#819266] p-6 overflow-hidden flex flex-col max-h-[90vh] animate-modal-entry">
        
        {/* Header */}
        <div className="flex items-start justify-between border-b-2 border-green-200 pb-4 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-green-100 border-2 border-green-600 flex items-center justify-center text-green-900 shadow-[2px_2px_0px_#DEB9B5] shrink-0">
              <Table2 className="w-5 h-5 text-green-700" />
            </div>
            <div>
              <h3 className="font-heading text-lg font-bold text-ink">
                Ma Trận Quyền Hạn Course-Scoped (RBAC Matrix)
              </h3>
              <p className="text-xs text-on-surface-variant font-sans">PostgreSQL Row-Level Security Rules (SWD392) • Onion Core</p>
            </div>
          </div>
          <button 
            type="button" 
            onClick={onClose} 
            className="w-8 h-8 rounded-lg border-2 border-green-500 flex items-center justify-center text-ink hover:bg-pink-100 transition cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Matrix Table */}
        <div className="overflow-y-auto text-xs space-y-4 mb-4">
          <div className="overflow-x-auto rounded-xl border-2 border-green-300">
            <table className="w-full text-left border-collapse min-w-[500px]">
              <thead>
                <tr className="bg-green-100 border-b-2 border-green-300 text-green-900 font-sans font-bold uppercase tracking-wider text-[11px]">
                  <th className="p-3">Tính Năng / Hành Động</th>
                  <th className="p-3 text-center">Student</th>
                  <th className="p-3 text-center">Lecturer</th>
                  <th className="p-3 text-center">Auditor</th>
                  <th className="p-3 text-center">Admin</th>
                </tr>
              </thead>
              <tbody className="divide-y-2 divide-green-200 text-ink font-sans">
                <tr className="hover:bg-green-50/50 transition-colors">
                  <td className="p-3 font-semibold text-ink">Vào phòng thi vấn đáp STOMP (Module 3)</td>
                  <td className="p-3 text-center text-green-700 font-bold">✓ (Chỉ ca thi)</td>
                  <td className="p-3 text-center text-green-700 font-bold">✓ (Giám sát)</td>
                  <td className="p-3 text-center text-green-700 font-bold">✓ (Thanh tra)</td>
                  <td className="p-3 text-center text-green-700 font-bold">✓</td>
                </tr>
                <tr className="bg-paper hover:bg-green-50/50 transition-colors">
                  <td className="p-3 font-semibold text-ink">Quản lý Tài khoản & Phân quyền User (Module 7)</td>
                  <td className="p-3 text-center text-pink-700 font-semibold">✗</td>
                  <td className="p-3 text-center text-pink-700 font-semibold">✗</td>
                  <td className="p-3 text-center text-pink-700 font-semibold">✗</td>
                  <td className="p-3 text-center text-green-700 font-bold">✓ (Toàn quyền)</td>
                </tr>
                <tr className="hover:bg-green-50/50 transition-colors">
                  <td className="p-3 font-semibold text-ink">Tạo & Sửa Ngân hàng Câu hỏi RAG (Module 1)</td>
                  <td className="p-3 text-center text-pink-700 font-semibold">✗</td>
                  <td className="p-3 text-center text-pink-700 font-bold">✓ (Trong môn)</td>
                  <td className="p-3 text-center text-pink-700 font-semibold">✗</td>
                  <td className="p-3 text-center text-green-700 font-bold">✓</td>
                </tr>
                <tr className="bg-paper hover:bg-green-50/50 transition-colors">
                  <td className="p-3 font-semibold text-ink">Hậu kiểm & Chốt điểm FAP (Module 4)</td>
                  <td className="p-3 text-center text-pink-700 font-semibold">✗</td>
                  <td className="p-3 text-center text-pink-700 font-bold">✓ (Trong môn)</td>
                  <td className="p-3 text-center text-on-surface-variant font-semibold">✗ (Chỉ xem)</td>
                  <td className="p-3 text-center text-green-700 font-bold">✓</td>
                </tr>
                <tr className="hover:bg-green-50/50 transition-colors">
                  <td className="p-3 font-semibold text-ink">Cấu hình mô hình Neural STT / TTS Locale</td>
                  <td className="p-3 text-center text-pink-700 font-semibold">✗</td>
                  <td className="p-3 text-center text-pink-700 font-semibold">✗</td>
                  <td className="p-3 text-center text-pink-700 font-semibold">✗</td>
                  <td className="p-3 text-center text-green-700 font-bold">✓ (Toàn quyền)</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div className="p-3.5 bg-green-50 rounded-xl border-2 border-green-500 text-green-900 text-xs leading-relaxed shadow-[2px_2px_0px_#DEB9B5]">
            <strong className="font-bold text-ink">Nguyên tắc Course-Scoped RBAC:</strong> Giảng viên được phân quyền phụ trách theo từng môn học cụ thể (bảng <code className="font-mono bg-paper px-1 py-0.5 rounded border border-green-400">lecturer_subjects</code>). Giảng viên môn SWD392 không có quyền truy cập ngân hàng đề của PRN231.
          </div>
        </div>

        {/* Footer */}
        <div className="pt-3 border-t-2 border-green-200 flex items-center justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-pink-500 border-2 border-ink text-ink font-bold text-xs shadow-[2px_2px_0px_#819266] hover:bg-pink-400 transition active:translate-x-[1px] active:translate-y-[1px] cursor-pointer"
          >
            Đã Hiểu Ma Trận
          </button>
        </div>

      </div>
    </div>
  );
}
