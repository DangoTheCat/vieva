import React from 'react';
import { X, Sparkles } from 'lucide-react';

export function RegenerateQuestionModal({
  target,
  onClose,
  feedback,
  setFeedback,
  onConfirm
}) {
  if (!target) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in" role="dialog">
      <div className="bg-paper rounded-2xl max-w-md w-full border border-pink-200 shadow-md p-5 space-y-4 animate-modal-entry">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-pink-100 pb-2.5">
          <div className="flex items-center gap-2 text-pink-900 font-bold text-xs">
            <Sparkles className="w-4 h-4 text-pink-600" />
            <span className="font-heading uppercase tracking-wider">Yêu Cầu AI Tạo Lại Câu Hỏi</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed">
          Cung cấp thêm chỉ dẫn điều chỉnh (Feedback). AI sẽ giữ nguyên ngữ cảnh trích dẫn ban đầu và viết lại câu hỏi phù hợp hơn.
        </p>

        <textarea
          rows="3"
          autoFocus
          placeholder="Ví dụ: Hãy chuyển câu hỏi thành dạng bài toán tình huống thực tế, làm rõ yêu cầu về tính mở rộng..."
          value={feedback}
          onChange={(e) => setFeedback(e.target.value)}
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
            onClick={onConfirm}
            className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] flex items-center gap-1.5 cursor-pointer"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Sinh Lại Câu Hỏi</span>
          </button>
        </div>
      </div>
    </div>
  );
}
