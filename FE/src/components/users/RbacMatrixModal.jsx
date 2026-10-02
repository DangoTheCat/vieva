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
      className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
    >
      <div className="bg-white rounded-2xl max-w-2xl w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col max-h-[85vh] animate-modal-entry">
        
        {/* Header */}
        <div className="p-4 border-b border-slate-700 bg-sidebarBg text-white flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-sky-500/20 text-sky-300 flex items-center justify-center">
              <Table2 className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-heading font-extrabold text-sm text-white">
                Ma Trận Phân Quyền Chi Tiết (RBAC Matrix)
              </h3>
              <p className="text-[11px] text-slate-400">PostgreSQL Row-Level Security Rules (SWD392)</p>
            </div>
          </div>
          <button 
            type="button" 
            onClick={onClose} 
            className="text-slate-400 hover:text-white p-1 rounded-lg transition focus:outline-none focus:ring-2 focus:ring-sky-500"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Matrix Table */}
        <div className="p-5 overflow-y-auto text-xs space-y-3">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[500px]">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-bold uppercase tracking-wider text-[11px]">
                  <th className="p-3">Tính Năng / Hành Động</th>
                  <th className="p-3 text-center">Student</th>
                  <th className="p-3 text-center">Lecturer</th>
                  <th className="p-3 text-center">Auditor</th>
                  <th className="p-3 text-center">Admin</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-semibold text-slate-900">Vào phòng thi vấn đáp STOMP (Module 3)</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓ (Chỉ ca thi)</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓ (Giám sát)</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓ (Thanh tra)</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓</td>
                </tr>
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-semibold text-slate-900">Quản lý Tài khoản & Phân quyền User (Module 7)</td>
                  <td className="p-3 text-center text-rose-500 font-semibold">✗</td>
                  <td className="p-3 text-center text-rose-500 font-semibold">✗</td>
                  <td className="p-3 text-center text-rose-500 font-semibold">✗</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓ (Toàn quyền)</td>
                </tr>
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-semibold text-slate-900">Tạo & Sửa Ngân hàng Câu hỏi RAG (Module 1)</td>
                  <td className="p-3 text-center text-rose-500 font-semibold">✗</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓ (Trong môn)</td>
                  <td className="p-3 text-center text-rose-500 font-semibold">✗</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓</td>
                </tr>
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-semibold text-slate-900">Hậu kiểm & Chốt điểm FAP (Module 4)</td>
                  <td className="p-3 text-center text-rose-500 font-semibold">✗</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓ (Trong môn)</td>
                  <td className="p-3 text-center text-slate-400 font-semibold">✗ (Chỉ xem)</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓</td>
                </tr>
                <tr className="hover:bg-slate-50/60 transition-colors">
                  <td className="p-3 font-semibold text-slate-900">Cấu hình mô hình Neural STT / TTS Locale</td>
                  <td className="p-3 text-center text-rose-500 font-semibold">✗</td>
                  <td className="p-3 text-center text-rose-500 font-semibold">✗</td>
                  <td className="p-3 text-center text-rose-500 font-semibold">✗</td>
                  <td className="p-3 text-center text-emerald-600 font-bold">✓ (Toàn quyền)</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div className="p-3 bg-sky-50 rounded-xl border border-sky-200 text-sky-800 text-[11px] leading-relaxed">
            <strong>Nguyên tắc Course-Scoped RBAC:</strong> Giảng viên được phân quyền phụ trách theo từng môn học cụ thể (bảng <code>lecturer_subjects</code>). Giảng viên môn SWD392 không có quyền truy cập ngân hàng đề của PRN231.
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-50/80 border-t border-slate-100 flex items-center justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
            Đóng Ma Trận
          </button>
        </div>

      </div>
    </div>
  );
}
