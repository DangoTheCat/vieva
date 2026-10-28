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
      setErrorMsg('Mật khẩu mới phải khác mật khẩu tạm thời.');
      return;
    }

    setIsSubmitting(true);
    try {
      await userApi.changePassword({ oldPassword, newPassword });
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

  const inputClass = 'w-full bg-white border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 rounded-xl py-2.5 pl-9 pr-10 text-xs text-ink placeholder:text-slate-400 shadow-2xs outline-none transition font-sans';

  const renderPasswordField = (label, value, onChange, placeholder, autoComplete, ref) => (
    <div>
      <label className="font-semibold text-slate-700 block mb-1 text-xs">{label}</label>
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
        <Key className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
      </div>
    </div>
  );

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-slate-50/80 text-ink antialiased font-sans">
      <div className="bg-white rounded-2xl max-w-md w-full p-6 sm:p-7 shadow-sm border border-pink-200/90 space-y-5 text-slate-800">

        <div className="flex items-center gap-3 pb-3 border-b border-pink-100">
          <div className="w-9 h-9 rounded-xl bg-amber-50 text-amber-600 border border-amber-200 flex items-center justify-center shrink-0">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <div>
            <h1 className="font-heading font-bold text-base text-ink">Đổi Mật Khẩu Lần Đầu</h1>
            <p className="text-xs text-slate-500">{currentUser?.email}</p>
          </div>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed">
          Tài khoản của bạn được quản trị viên cấp mật khẩu tạm thời. Vui lòng thiết lập mật khẩu mới bảo mật để kích hoạt đầy đủ quyền truy cập.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4 text-xs" autoComplete="off">
          {errorMsg && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-xs font-medium flex items-center gap-2 animate-in fade-in" role="alert">
              <ShieldAlert className="w-4 h-4 shrink-0 text-rose-600" />
              <span>{errorMsg}</span>
            </div>
          )}

          {renderPasswordField('Mật Khẩu Tạm Thời (*)', oldPassword, setOldPassword, 'Mật khẩu được cấp', 'current-password', oldPasswordRef)}
          {renderPasswordField('Mật Khẩu Mới (*)', newPassword, setNewPassword, 'Tối thiểu 6 ký tự', 'new-password')}
          {renderPasswordField('Xác Nhận Mật Khẩu Mới (*)', confirmPassword, setConfirmPassword, 'Nhập lại mật khẩu mới', 'new-password')}

          <button
            type="button"
            onClick={() => setShowPasswords(!showPasswords)}
            className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-800 focus:outline-none cursor-pointer"
          >
            {showPasswords ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
            <span>{showPasswords ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}</span>
          </button>

          <div className="flex items-center justify-between gap-2.5 pt-2 border-t border-pink-100">
            <button
              type="button"
              onClick={logout}
              className="px-4 py-2.5 rounded-xl border border-pink-200 hover:bg-slate-50 text-slate-700 font-semibold text-xs shadow-2xs transition flex items-center gap-1.5 cursor-pointer"
            >
              <LogOut className="w-3.5 h-3.5 text-slate-500" />
              <span>Đăng xuất</span>
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2.5 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 flex items-center gap-1.5 cursor-pointer"
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
