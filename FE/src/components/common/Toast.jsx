import React, { useEffect } from 'react';
import { CheckCircle2, AlertCircle, AlertTriangle, Info, X } from 'lucide-react';

export function Toast({ toast, onClose }) {
  if (!toast) return null;

  const { type = 'info', message, title, duration = 4000 } = toast;

  useEffect(() => {
    if (duration > 0) {
      const timer = setTimeout(() => {
        onClose();
      }, duration);
      return () => clearTimeout(timer);
    }
  }, [toast, duration, onClose]);

  const styles = {
    success: {
      card: 'bg-white border-emerald-200 text-slate-800 shadow-emerald-500/10',
      badge: 'bg-emerald-100 text-emerald-800 border-emerald-200',
      icon: <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />,
      bar: 'bg-emerald-500'
    },
    error: {
      card: 'bg-white border-rose-200 text-slate-800 shadow-rose-500/10',
      badge: 'bg-rose-100 text-rose-800 border-rose-200',
      icon: <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />,
      bar: 'bg-rose-500'
    },
    warning: {
      card: 'bg-white border-amber-200 text-slate-800 shadow-amber-500/10',
      badge: 'bg-amber-100 text-amber-800 border-amber-200',
      icon: <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />,
      bar: 'bg-amber-500'
    },
    info: {
      card: 'bg-white border-sky-200 text-slate-800 shadow-sky-500/10',
      badge: 'bg-sky-100 text-sky-800 border-sky-200',
      icon: <Info className="w-4 h-4 text-sky-600 shrink-0" />,
      bar: 'bg-sky-500'
    }
  }[type] || {
    card: 'bg-white border-slate-200 text-slate-800 shadow-slate-500/10',
    badge: 'bg-slate-100 text-slate-800 border-slate-200',
    icon: <Info className="w-4 h-4 text-slate-600 shrink-0" />,
    bar: 'bg-slate-500'
  };

  return (
    <div 
      role="alert" 
      aria-live="polite"
      className="fixed top-20 right-4 sm:right-6 z-50 max-w-sm sm:max-w-md w-full animate-modal-entry shadow-2xl rounded-2xl overflow-hidden border"
    >
      <div className={`p-4 ${styles.card} flex items-start gap-3 relative`}>
        <div className="mt-0.5">{styles.icon}</div>
        
        <div className="flex-1 min-w-0 pr-2">
          {title && (
            <h4 className="font-heading font-extrabold text-xs text-slate-900 mb-0.5 tracking-tight">
              {title}
            </h4>
          )}
          <p className="text-xs leading-relaxed text-slate-600 font-medium">
            {message}
          </p>
        </div>

        <button
          onClick={onClose}
          className="text-slate-400 hover:text-slate-700 p-1 rounded-lg hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-sky-500"
          aria-label="Đóng thông báo"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Animated countdown progress bar */}
      {duration > 0 && (
        <div className="h-1 w-full bg-slate-100">
          <div 
            className={`h-full ${styles.bar} animate-progress-bar`} 
            style={{ animationDuration: `${duration}ms` }}
          />
        </div>
      )}
    </div>
  );
}
