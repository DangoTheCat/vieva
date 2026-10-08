import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/userApi';
import { getErrorMessage } from '../utils/errorCodes';
import { Key, Eye, EyeOff, ShieldAlert, LogOut } from 'lucide-react';

/**
 * Shown instead of the app when an admin created the account (or reset its password):
 * the user must replace the temporary password before using anything else.
 * BE enforces the same rule (403 / 1062 on every other endpoint).
 */
export function ForceChangePasswordPage({ showToast }) {
  const { currentUser, logout, completeForcedPasswordChange } = useAuth();

  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPasswords, setShowPasswords] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const oldPasswordRef = useRef(null);

  useEffect(() => {
    oldPasswordRef.current?.focus();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!oldPassword) {
      setErrorMsg('Vui lòng nhập mật khẩu tạm thời được gửi qua email.');
      return;
    }
    if (newPassword.length < 6) {
      setErrorMsg('Mật khẩu mới phải có tối thiểu 6 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setErrorMsg('Mật khẩu xác nhận không trùng khớp.');
      return;
    }
    if (oldPassword === newPassword) {
      setErrorMsg('Mật khẩu mới phải khác mật khẩu tạm thời (Error 1010).');
      return;
    }

    setIsSubmitting(true);
    try {
      await userApi.changePassword({ oldPassword, newPassword });
      // BE revokes the current token after a password change: sign in again with the new password
      completeForcedPasswordChange();
      showToast?.({
        type: 'success',
        title: 'Đổi Mật Khẩu Thành Công',
        message: 'Vui lòng đăng nhập lại bằng mật khẩu mới.'
      });
    } catch (err) {
      setErrorMsg(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  const inputClass = 'w-full bg-[#070d1a] border border-sky-900/50 rounded-xl p-3 pl-9 pr-10 text-xs text-white focus:ring-2 focus:ring-sky-500 focus:border-sky-500 focus:outline-none transition font-sans';

  const renderPasswordField = (label, value, onChange, placeholder, autoComplete, ref) => (
    <div>
      <label className="font-bold text-slate-300 block mb-1">{label}</label>
      <div className="relative">
        <input
          ref={ref}
          type={showPasswords ? 'text' : 'password'}
          required
          autoComplete={autoComplete}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className={inputClass}
        />
        <Key className="w-4 h-4 text-slate-500 absolute left-3 top-3.5" />
      </div>
    </div>
  );

  return (
    <div className="min-h-screen flex items-center justify-center p-4 dark-tech-grid text-slate-100 antialiased font-sans">
      <div className="bg-[#0b1428]/95 backdrop-blur-xl rounded-3xl max-w-md w-full p-6 shadow-2xl border border-sky-800/60 space-y-5 text-slate-200">

        <div className="flex items-center gap-2 pb-3 border-b border-sky-900/50">
          <div className="w-8 h-8 rounded-xl bg-amber-500/20 text-amber-300 flex items-center justify-center border border-amber-500/30">
            <ShieldAlert className="w-4 h-4" />
          </div>
          <div>
            <h1 className="font-heading font-extrabold text-sm text-white">Đổi Mật Khẩu Lần Đầu</h1>
            <p className="text-[11px] text-slate-400">{currentUser?.email}</p>
          </div>
        </div>

        <p className="text-[11px] text-slate-300 leading-relaxed">
          Tài khoản của bạn được quản trị viên tạo với mật khẩu tạm thời. Hãy đặt mật khẩu mới để tiếp tục sử dụng hệ thống.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4 text-xs" autoComplete="off">
          {errorMsg && (
            <div className="p-3 bg-rose-950/80 border border-rose-800 rounded-xl text-rose-300 text-[11px] font-medium flex items-center gap-2" role="alert">
              <ShieldAlert className="w-4 h-4 shrink-0 text-rose-400" />
              <span>{errorMsg}</span>
            </div>
          )}

          {renderPasswordField('Mật Khẩu Tạm Thời (*)', oldPassword, setOldPassword, 'Mật khẩu trong email', 'current-password', oldPasswordRef)}
          {renderPasswordField('Mật Khẩu Mới (*)', newPassword, setNewPassword, 'Tối thiểu 6 ký tự', 'new-password')}
          {renderPasswordField('Xác Nhận Mật Khẩu Mới (*)', confirmPassword, setConfirmPassword, 'Nhập lại mật khẩu mới', 'new-password')}

          <button
            type="button"
            onClick={() => setShowPasswords(!showPasswords)}
            className="flex items-center gap-1.5 text-[11px] text-slate-400 hover:text-slate-200 focus:outline-none"
          >
            {showPasswords ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
            <span>{showPasswords ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}</span>
          </button>

          <div className="flex items-center justify-between gap-2.5 pt-2">
            <button
              type="button"
              onClick={logout}
              className="px-4 py-2.5 rounded-xl border border-slate-600 hover:bg-slate-800 text-slate-300 font-semibold text-xs transition flex items-center gap-1.5"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Đăng xuất</span>
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              {isSubmitting && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
              <span>Đổi Mật Khẩu</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
