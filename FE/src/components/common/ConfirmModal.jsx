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
    // Focus confirmation button safely
    const timer = setTimeout(() => confirmBtnRef.current?.focus(), 50);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      clearTimeout(timer);
    };
  }, [isOpen, isLoading, onCancel, onConfirm]);

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-modal-title"
    >
      <div className="bg-white rounded-2xl max-w-md w-full border border-slate-200/90 shadow-2xl overflow-hidden flex flex-col animate-modal-entry">
        
        {/* Header */}
        <div className="p-4 bg-slate-50 border-b border-slate-200/80 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className={`w-8 h-8 rounded-xl flex items-center justify-center ${isDanger ? 'bg-rose-100 text-rose-600' : 'bg-sky-100 text-sky-600'}`}>
              <AlertTriangle className="w-4 h-4" />
            </div>
            <h3 id="confirm-modal-title" className="font-heading font-extrabold text-sm text-slate-900">
              {title}
            </h3>
          </div>
          <button
            onClick={onCancel}
            disabled={isLoading}
            className="text-slate-400 hover:text-slate-700 p-1.5 rounded-lg hover:bg-slate-200/60 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 text-xs text-slate-600 leading-relaxed font-medium">
          {message}
        </div>

        {/* Actions */}
        <div className="p-4 bg-slate-50/80 border-t border-slate-100 flex items-center justify-end gap-2.5">
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold text-xs transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-slate-400"
          >
            {cancelText}
          </button>
          <button
            ref={confirmBtnRef}
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            className={`px-5 py-2 rounded-xl text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-2 focus:outline-none focus:ring-2 ${
              isDanger
                ? 'bg-rose-600 hover:bg-rose-700 focus:ring-rose-500 shadow-rose-600/20'
                : 'bg-sky-600 hover:bg-sky-700 focus:ring-sky-500 shadow-sky-600/20'
            }`}
          >
            {isLoading && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
            <span>{confirmText}</span>
          </button>
        </div>

      </div>
    </div>
  );
}
