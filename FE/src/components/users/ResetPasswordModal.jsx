import React, { useState, useEffect } from 'react';
import { KeyRound, X, Eye, EyeOff } from 'lucide-react';
import { adminUserApi } from '../../api/adminUserApi';
import { getErrorMessage } from '../../utils/errorCodes';

const MIN_PASSWORD_LENGTH = 6;

export function ResetPasswordModal({ user, onClose, showToast }) {
  const [newPassword, setNewPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    setNewPassword('');
    setShowPassword(false);
  }, [user]);

  // Keyboard accessibility: Escape to close
  useEffect(() => {
    if (!user) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !isSubmitting) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [user, isSubmitting, onClose]);

  if (!user) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (newPassword.length < MIN_PASSWORD_LENGTH) {
      showToast({ type: 'error', message: `Mật khẩu mới cần ít nhất ${MIN_PASSWORD_LENGTH} ký tự.` });
      return;
    }
    setIsSubmitting(true);
    try {
      await adminUserApi.resetPassword(user.userId, newPassword);
      showToast({ type: 'success', message: `Đã đặt lại mật khẩu cho ${user.email}.` });
      onClose();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
    >
      <form
        onSubmit={handleSubmit}
        className="bg-white rounded-2xl max-w-md w-full border border-slate-200 shadow-2xl p-5 space-y-4 animate-modal-entry"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-slate-900">
            <KeyRound className="w-4 h-4 text-amber-500" />
            <h3 className="font-heading font-extrabold text-sm">Đặt Lại Mật Khẩu</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-700"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-xs text-slate-500">
          Đặt mật khẩu mới cho <span className="font-bold text-slate-800">{user.fullName}</span> ({user.email}).
        </p>

        <div className="relative">
          <input
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            minLength={MIN_PASSWORD_LENGTH}
            required
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder={`Mật khẩu mới (tối thiểu ${MIN_PASSWORD_LENGTH} ký tự)`}
            className="w-full pl-3 pr-10 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
          <button
            type="button"
            onClick={() => setShowPassword(v => !v)}
            className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-slate-700"
            aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          >
            {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>

        <div className="flex justify-end gap-2 pt-1">
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition"
          >
            Hủy Bỏ
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-5 py-2 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-2 focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
            {isSubmitting && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
            <span>Đặt Lại</span>
          </button>
        </div>
      </form>
    </div>
  );
}
