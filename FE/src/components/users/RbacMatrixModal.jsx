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
      className="fixed inset-0 z-50 bg-pink-600/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-2xl bg-white rounded-2xl border border-pink-200 shadow-xl p-6 overflow-hidden flex flex-col max-h-[90vh] animate-modal-entry text-ink">
        
        {/* Header */}
        <div className="flex items-start justify-between border-b border-pink-200 pb-4 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-slate-100 border border-pink-200 flex items-center justify-center text-slate-700 shadow-2xs shrink-0">
              <Table2 className="w-5 h-5 text-slate-700" />
            </div>
            <div>
              <h3 className="font-heading text-lg font-bold text-ink">
                Bảng Phân Quyền Vai Trò Hệ Thống
              </h3>
              <p className="text-xs text-slate-500 font-sans">Quy định quyền truy cập theo từng vai trò người dùng trong hệ thống AIVES</p>
            </div>
          </div>
          <button 
            type="button" 
            onClick={onClose} 
            className="w-8 h-8 rounded-lg border border-pink-200 flex items-center justify-center text-slate-500 hover:text-ink hover:bg-slate-50 transition cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Matrix Table */}
        <div className="overflow-y-auto text-xs space-y-4 mb-4">
          <div className="overflow-x-auto rounded-xl border border-pink-200">
            <table className="w-full text-left border-collapse min-w-[500px]">
              <thead>
                <tr className="bg-slate-50 border-b border-pink-200 text-slate-700 font-semibold uppercase tracking-wider text-[11px]">
                  <th className="p-3">Tính Năng / Thao Tác</th>
                  <th className="p-3 text-center">Sinh Viên</th>
                  <th className="p-3 text-center">Giảng Viên</th>
                  <th className="p-3 text-center">Quản Trị Viên</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-800">
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-medium text-ink">Tham gia phòng thi vấn đáp trực tuyến</td>
                  <td className="p-3 text-center text-emerald-600 font-semibold">✓ (Ca thi được phân công)</td>
                  <td className="p-3 text-center text-emerald-600 font-semibold">✓ (Giám sát)</td>
                  <td className="p-3 text-center text-emerald-600 font-semibold">✓</td>
                </tr>
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-medium text-ink">Quản lý tài khoản & người dùng</td>
                  <td className="p-3 text-center text-slate-400 font-normal">—</td>
                  <td className="p-3 text-center text-slate-400 font-normal">—</td>
                  <td className="p-3 text-center text-emerald-600 font-semibold">✓ (Toàn quyền)</td>
                </tr>
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-medium text-ink">Tạo & quản lý ngân hàng câu hỏi</td>
                  <td className="p-3 text-center text-slate-400 font-normal">—</td>
                  <td className="p-3 text-center text-emerald-600 font-semibold">✓ (Môn phụ trách)</td>
                  <td className="p-3 text-center text-emerald-600 font-semibold">✓</td>
                </tr>
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-medium text-ink">Chấm điểm & duyệt kết quả thi</td>
                  <td className="p-3 text-center text-slate-400 font-normal">—</td>
                  <td className="p-3 text-center text-emerald-600 font-semibold">✓ (Môn phụ trách)</td>
                  <td className="p-3 text-center text-emerald-600 font-semibold">✓</td>
                </tr>
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-medium text-ink">Cấu hình nhận diện giọng nói (STT/TTS)</td>
                  <td className="p-3 text-center text-slate-400 font-normal">—</td>
                  <td className="p-3 text-center text-slate-400 font-normal">—</td>
                  <td className="p-3 text-center text-emerald-600 font-semibold">✓ (Toàn quyền)</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div className="p-3.5 bg-slate-50 rounded-xl border border-pink-200 text-slate-600 text-xs leading-relaxed">
            <strong className="font-semibold text-slate-800">Quy định phân quyền môn học:</strong> Giảng viên chỉ có quyền quản trị ngân hàng đề và chấm điểm cho các môn học được Ban Khảo thí phân công chính thức.
          </div>
        </div>

        {/* Footer */}
        <div className="pt-3 border-t border-pink-200 flex items-center justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-pink-600 text-white font-semibold text-xs shadow-xs hover:bg-pink-700 transition active:scale-[0.98] cursor-pointer"
          >
            Đóng
          </button>
        </div>

      </div>
    </div>
  );
}
