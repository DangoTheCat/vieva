import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import { userApi } from '../../api/userApi';
import { getErrorMessage } from '../../utils/errorCodes';
import { User, Key, X, CheckCircle, ShieldAlert, Eye, EyeOff } from 'lucide-react';

export function ProfileModal({ isOpen, initialTab = 'info', onClose, showToast }) {
  const { currentUser, refreshProfile, isDemoMode, setCurrentUser } = useAuth();
  const [activeTab, setActiveTab] = useState(initialTab);
  
  // Profile form state
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);

  // Password form state
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState('');

  const fullNameInputRef = useRef(null);

  useEffect(() => {
    if (currentUser) {
      setFullName(currentUser.fullName || '');
      setPhoneNumber(currentUser.phoneNumber || '');
    }
    setActiveTab(initialTab);
  }, [currentUser, initialTab, isOpen]);

  // Keyboard accessibility: Escape to close
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    const timer = setTimeout(() => {
      if (activeTab === 'info') fullNameInputRef.current?.focus();
    }, 50);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      clearTimeout(timer);
    };
  }, [isOpen, activeTab, onClose]);

  if (!isOpen) return null;

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    if (!fullName.trim()) {
      showToast({ type: 'warning', message: 'Họ và tên không được để trống.' });
      return;
    }

    if (phoneNumber && !/^(\+?[0-9]{9,11})$/.test(phoneNumber.trim())) {
      showToast({ type: 'warning', message: 'Số điện thoại phải từ 9 đến 11 chữ số.' });
      return;
    }

    setIsUpdatingProfile(true);
    try {
      if (isDemoMode) {
        const updated = {
          ...currentUser,
          fullName: fullName.trim(),
          phoneNumber: phoneNumber ? phoneNumber.trim() : null,
          updatedAt: new Date().toISOString()
        };
        setCurrentUser(updated);
        localStorage.setItem('aives_user', JSON.stringify(updated));
        showToast({ type: 'success', message: 'Cập nhật hồ sơ cá nhân thành công (Chế độ Demo)!' });
        onClose();
        return;
      }

      await userApi.updateProfile({
        fullName: fullName.trim(),
        phoneNumber: phoneNumber.trim()
      });

      await refreshProfile();
      showToast({ type: 'success', message: 'Cập nhật hồ sơ cá nhân thành công!' });
      onClose();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPasswordError('');

    if (!oldPassword) {
      setPasswordError('Vui lòng nhập mật khẩu hiện tại.');
      return;
    }
    if (newPassword.length < 6) {
      setPasswordError('Mật khẩu mới phải có tối thiểu 6 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('Mật khẩu xác nhận không trùng khớp.');
      return;
    }
    if (oldPassword === newPassword) {
      setPasswordError('Mật khẩu mới phải khác mật khẩu hiện tại (Error 1010).');
      return;
    }

    setIsChangingPassword(true);
    try {
      if (isDemoMode) {
        showToast({ type: 'success', message: 'Đổi mật khẩu thành công (Chế độ Demo)!' });
        setOldPassword('');
        setNewPassword('');
        setConfirmPassword('');
        onClose();
        return;
      }

      await userApi.changePassword({ oldPassword, newPassword });
      showToast({ type: 'success', message: 'Đổi mật khẩu thành công! Vui lòng lưu nhớ mật khẩu mới.' });
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      onClose();
    } catch (err) {
      const msg = getErrorMessage(err);
      setPasswordError(msg);
      showToast({ type: 'error', message: msg });
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div 
      className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
    >
      <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry max-h-[90vh]">
        
        {/* Header */}
        <div className="p-4 bg-sidebarBg text-white border-b border-slate-700 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-sky-500/20 text-sky-300 flex items-center justify-center">
              <User className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-heading font-extrabold text-sm text-white">Quản Lý Tài Khoản Cá Nhân</h3>
              <p className="text-[11px] text-slate-400 font-mono">Endpoint: /api/v1/users/me</p>
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

        {/* Tab Switcher */}
        <div className="flex border-b border-slate-200 bg-slate-50 text-xs font-semibold px-4 pt-2 gap-2">
          <button
            type="button"
            onClick={() => setActiveTab('info')}
            className={`pb-2.5 px-3 border-b-2 flex items-center gap-2 transition ${
              activeTab === 'info'
                ? 'border-sky-600 text-sky-700 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <User className="w-3.5 h-3.5" />
            <span>Thông Tin Cá Nhân</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('password')}
            className={`pb-2.5 px-3 border-b-2 flex items-center gap-2 transition ${
              activeTab === 'password'
                ? 'border-sky-600 text-sky-700 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Key className="w-3.5 h-3.5" />
            <span>Đổi Mật Khẩu</span>
          </button>
        </div>

        {/* Tab 1: Profile Info Form */}
        {activeTab === 'info' && (
          <form onSubmit={handleUpdateProfile} className="p-5 space-y-4 text-xs overflow-y-auto">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Mã Người Dùng (User Code):</label>
                <input
                  type="text"
                  disabled
                  value={currentUser?.userCode || ''}
                  className="w-full p-2.5 rounded-xl border border-slate-200 bg-slate-100 text-slate-500 font-mono cursor-not-allowed text-xs"
                />
              </div>
              <div>
                <label className="block font-bold text-slate-700 mb-1">Trạng Thái Tài Khoản:</label>
                <div className="p-2.5 rounded-xl border border-slate-200 bg-slate-50 text-xs flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                  <span className="font-bold text-slate-700">{currentUser?.status || 'ACTIVE'}</span>
                </div>
              </div>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Email Học Thuật (FPT Edu):</label>
              <input
                type="email"
                disabled
                value={currentUser?.email || ''}
                className="w-full p-2.5 rounded-xl border border-slate-200 bg-slate-100 text-slate-500 font-mono cursor-not-allowed text-xs"
              />
              <p className="text-[10px] text-slate-400 mt-1">Email đăng ký do Admin hoặc hệ thống cấp không thể tự thay đổi.</p>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Họ Và Tên (*):</label>
              <input
                ref={fullNameInputRef}
                type="text"
                required
                maxLength={50}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nguyễn Văn A"
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Số Điện Thoại:</label>
              <input
                type="text"
                maxLength={20}
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="0912345678"
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Vai Trò Hệ Thống:</label>
              <div className="flex flex-wrap gap-1.5">
                {currentUser?.roles?.map((role, idx) => (
                  <span
                    key={idx}
                    className="px-2.5 py-1 rounded-full text-[10px] font-mono font-bold bg-purple-50 text-purple-700 border border-purple-200"
                  >
                    {role}
                  </span>
                ))}
              </div>
            </div>

            <div className="p-4 bg-slate-50/80 border-t border-slate-100 flex items-center justify-end gap-2.5 -mx-5 -mb-5 mt-5">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold transition"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={isUpdatingProfile}
                className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                {isUpdatingProfile && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                <span>Lưu Thay Đổi</span>
              </button>
            </div>
          </form>
        )}

        {/* Tab 2: Password Form */}
        {activeTab === 'password' && (
          <form onSubmit={handleChangePassword} className="p-5 space-y-4 text-xs overflow-y-auto">
            {passwordError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 flex items-center gap-2 font-medium">
                <ShieldAlert className="w-4 h-4 shrink-0 text-rose-500" />
                <span>{passwordError}</span>
              </div>
            )}

            <div>
              <label className="block font-bold text-slate-700 mb-1">Mật Khẩu Hiện Tại (*):</label>
              <div className="relative">
                <input
                  type={showOldPassword ? 'text' : 'password'}
                  required
                  value={oldPassword}
                  onChange={(e) => setOldPassword(e.target.value)}
                  placeholder="Nhập mật khẩu đang dùng..."
                  className="w-full p-2.5 pr-8 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition"
                />
                <button
                  type="button"
                  onClick={() => setShowOldPassword(!showOldPassword)}
                  className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600 focus:outline-none"
                  aria-label={showOldPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showOldPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Mật Khẩu Mới (*):</label>
              <div className="relative">
                <input
                  type={showNewPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Tối thiểu 6 ký tự..."
                  className="w-full p-2.5 pr-8 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600 focus:outline-none"
                  aria-label={showNewPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showNewPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Xác Nhận Mật Khẩu Mới (*):</label>
              <input
                type="password"
                required
                minLength={6}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Nhập lại mật khẩu mới..."
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition"
              />
            </div>

            <div className="p-4 bg-slate-50/80 border-t border-slate-100 flex items-center justify-end gap-2.5 -mx-5 -mb-5 mt-5">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold transition"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={isChangingPassword}
                className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                {isChangingPassword && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                <span>Cập Nhật Mật Khẩu</span>
              </button>
            </div>
          </form>
        )}

      </div>
    </div>
  );
}
