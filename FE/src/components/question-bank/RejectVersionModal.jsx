import React, { useState } from 'react';
import { X, AlertCircle } from 'lucide-react';

export function RejectVersionModal({
  target,
  onClose,
  onConfirm
}) {
  const [reason, setReason] = useState('');

  if (!target) return null;

  const handleConfirm = () => {
    if (!reason.trim()) return;
    onConfirm(target, reason.trim());
    setReason('');
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in" role="dialog">
      <div className="bg-paper rounded-2xl max-w-md w-full border border-pink-200 shadow-md p-5 space-y-4 animate-modal-entry">
        <div className="flex items-center justify-between border-b border-pink-100 pb-2.5">
          <h3 className="font-heading font-extrabold text-sm text-ink flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600" />
            <span>Từ Chối Phê Duyệt Câu Hỏi</span>
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed">
          Vui lòng nêu rõ lý do từ chối để hỗ trợ giảng viên soạn thảo hoặc định hướng lại prompt cho AI.
        </p>

        <textarea
          rows="3"
          required
          autoFocus
          placeholder="Ví dụ: Câu hỏi chưa gắn đúng trích dẫn giáo trình hoặc đáp án chưa đủ độ sâu kỹ thuật..."
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-2xs font-medium"
        />

        <div className="flex justify-end gap-2 pt-2 border-t border-pink-100">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-white border border-pink-200 text-slate-700 text-xs font-bold shadow-2xs hover:bg-slate-50 transition cursor-pointer"
          >
            Hủy Bỏ
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={!reason.trim()}
            className="px-5 py-2 rounded-xl bg-white border border-rose-200 text-rose-600 hover:bg-rose-50 font-bold text-xs shadow-2xs disabled:opacity-50 transition cursor-pointer"
          >
            Xác Nhận Từ Chối
          </button>
        </div>
      </div>
    </div>
  );
}
