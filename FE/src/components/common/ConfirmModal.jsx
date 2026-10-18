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
      className="fixed inset-0 z-50 bg-ink/65 backdrop-blur-[2px] flex items-center justify-center p-4 transition-all duration-200"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-modal-title"
    >
      <div className="bg-paper rounded-lg max-w-md w-full border border-green-200 shadow-modal overflow-hidden flex flex-col transform transition-all animate-modal-entry">
        
        {/* Header */}
        <div className="p-4 bg-pink-50 border-b border-green-200 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className={`w-8 h-8 rounded-sm flex items-center justify-center shrink-0 border ${
              isDanger 
                ? 'bg-pink-100 text-pink-700 border-pink-200' 
                : 'bg-green-100 text-green-700 border-green-200'
            }`}>
              <AlertTriangle className="w-4 h-4" />
            </div>
            <h3 id="confirm-modal-title" className="font-fraunces font-semibold text-base text-pink-800 tracking-tight leading-none">
              {title}
            </h3>
          </div>
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="text-green-600 hover:text-pink-700 p-1.5 rounded-sm hover:bg-pink-100 transition-colors focus:outline-none focus:ring-2 focus:ring-green-600 disabled:opacity-50"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 text-sm text-pink-900 leading-relaxed font-normal bg-paper">
          {message}
        </div>

        {/* Actions */}
        <div className="p-4 bg-green-50/70 border-t border-green-200 flex items-center justify-end gap-2.5">
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="px-4 py-2 min-h-[40px] rounded-md border border-green-300 bg-white hover:bg-green-100 text-green-700 font-semibold text-xs transition-all active:scale-95 focus:outline-none focus:ring-2 focus:ring-green-600 shadow-sm disabled:opacity-50"
          >
            {cancelText}
          </button>
          <button
            ref={confirmBtnRef}
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            className={`px-5 py-2 min-h-[40px] rounded-md font-semibold text-xs shadow-md transition-all active:scale-95 disabled:opacity-50 flex items-center gap-2 focus:outline-none focus:ring-2 focus:ring-offset-2 ${
              isDanger
                ? 'bg-pink-600 hover:bg-pink-700 text-white focus:ring-pink-600'
                : 'bg-green-600 hover:bg-green-700 text-white focus:ring-green-600'
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
