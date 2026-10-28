import React, { useEffect, useRef } from 'react';
import { AlertTriangle, X } from 'lucide-react';

export function ConfirmModal({
  isOpen,
  title = 'Xác Nhận Hành Động',
  message = 'Bạn có chắc chắn muốn thực hiện hành động này?',
  confirmText = 'Xác Nhận',
  cancelText = 'Hủy Bỏ',
  isDanger = true,
  isLoading = false,
  onConfirm,
  onCancel
}) {
  const confirmBtnRef = useRef(null);

  // Keyboard accessibility: Escape to cancel, Enter to confirm
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onCancel();
      } else if (e.key === 'Enter' && !isLoading) {
        e.preventDefault();
        onConfirm();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    const timer = setTimeout(() => confirmBtnRef.current?.focus(), 50);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      clearTimeout(timer);
    };
  }, [isOpen, isLoading, onCancel, onConfirm]);

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-50 bg-pink-600/60 backdrop-blur-xs flex items-center justify-center p-4 transition-all duration-200"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-modal-title"
    >
      <div className="bg-white rounded-2xl max-w-md w-full border border-pink-200 shadow-xl overflow-hidden flex flex-col transform transition-all animate-modal-entry">
        
        {/* Header */}
        <div className="p-4 bg-white border-b border-pink-100 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 border ${
              isDanger 
                ? 'bg-rose-50 text-rose-600 border-rose-200' 
                : 'bg-pink-50 text-pink-600 border-pink-200'
            }`}>
              <AlertTriangle className="w-4 h-4" />
            </div>
            <h3 id="confirm-modal-title" className="font-heading font-bold text-base text-ink tracking-tight leading-none">
              {title}
            </h3>
          </div>
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="text-slate-400 hover:text-slate-700 p-1.5 rounded-lg hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-slate-400 disabled:opacity-50 cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 text-xs sm:text-sm text-slate-600 leading-relaxed font-normal bg-white">
          {message}
        </div>

        {/* Actions */}
        <div className="p-4 bg-slate-50 border-t border-pink-100 flex items-center justify-end gap-2.5">
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="px-4 py-2 min-h-[38px] rounded-xl border border-pink-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-xs transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-slate-300 shadow-2xs disabled:opacity-50 cursor-pointer"
          >
            {cancelText}
          </button>
          <button
            ref={confirmBtnRef}
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            className={`px-5 py-2 min-h-[38px] rounded-xl font-semibold text-xs shadow-xs transition active:scale-95 disabled:opacity-50 flex items-center gap-2 focus:outline-none focus:ring-2 focus:ring-offset-1 cursor-pointer ${
              isDanger
                ? 'bg-rose-600 hover:bg-rose-700 text-white focus:ring-rose-500'
                : 'bg-pink-600 hover:bg-pink-700 text-white focus:ring-pink-500'
            }`}
          >
            {isLoading && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
            <span>{confirmText}</span>
          </button>
        </div>

      </div>
    </div>
  );
}
