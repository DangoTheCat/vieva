import React, { useState, useEffect, useRef } from 'react';
import { adminUserApi } from '../../api/adminUserApi';
import { ROLE_OPTIONS, getUserRole } from '../../utils/roles';
import { getErrorMessage } from '../../utils/errorCodes';
import { useAuth } from '../../context/AuthContext';
import { Edit3, X, ShieldAlert, Check, AlertTriangle, Info } from 'lucide-react';

export function EditUserModal({ isOpen, user, onClose, onSuccess, showToast }) {
  const { currentUser, isDemoMode } = useAuth();

  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [status, setStatus] = useState('ACTIVE');
  const [selectedRole, setSelectedRole] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  const isEditingSelf = user?.userId === currentUser?.userId;
  const originalRole = getUserRole(user);
  const fullNameInputRef = useRef(null);

  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setPhoneNumber(user.phoneNumber || '');
      setStatus(user.status || 'ACTIVE');
      setSelectedRole(getUserRole(user) || '');
      setFormError('');
    }
  }, [user, isOpen]);

  // Keyboard accessibility: Escape to close & autofocus
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    const timer = setTimeout(() => fullNameInputRef.current?.focus(), 50);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      clearTimeout(timer);
    };
  }, [isOpen, onClose]);

  if (!isOpen || !user) return null;

  // An account has exactly one role: picking a role replaces the current one
  const selectRole = (role) => {
    if (isEditingSelf && role !== 'ROLE_ADMIN') {
      showToast({ type: 'warning', message: 'Bạn không thể tự hạ quyền Administrator của chính mình (Error 1012).' });
      return;
    }
    setSelectedRole(role);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!fullName.trim() || fullName.trim().length < 2) {
      setFormError('Họ và tên không được để trống (tối thiểu 2 ký tự).');
      return;
    }

    if (phoneNumber && !/^(\+?[0-9]{9,11})$/.test(phoneNumber.trim())) {
      setFormError('Số điện thoại phải từ 9 đến 11 chữ số.');
      return;
    }

    if (isEditingSelf && status !== 'ACTIVE') {
      setFormError('Bạn không thể tự khóa hoặc vô hiệu hóa tài khoản của chính mình (Error 1016).');
      return;
    }

    if (isEditingSelf && selectedRole !== 'ROLE_ADMIN') {
      setFormError('Bạn không thể tự gỡ bỏ quyền Administrator của chính mình (Error 1012).');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        fullName: fullName.trim(),
        phoneNumber: phoneNumber.trim() || null,
        status,
        // Only send the single roleCode when it changed (null = keep the current role)
        roleCode: selectedRole && selectedRole !== originalRole ? selectedRole : null
      };

      if (isDemoMode) {
        const updated = {
          ...user,
          fullName: payload.fullName,
          phoneNumber: payload.phoneNumber,
          status: payload.status,
          role: payload.roleCode || user.role,
          roles: payload.roleCode ? [payload.roleCode] : user.roles,
          updatedAt: new Date().toISOString()
        };
        showToast({ type: 'success', message: `Cập nhật thông tin ${updated.fullName} thành công (Demo Mode)!` });
        onSuccess(updated);
        onClose();
        return;
      }

      const updated = await adminUserApi.updateUser(user.userId, payload);
      showToast({ type: 'success', message: `Cập nhật người dùng ${updated.fullName} thành công!` });
      onSuccess(updated);
      onClose();
    } catch (err) {
      const errMsg = getErrorMessage(err);
      setFormError(errMsg);
      showToast({ type: 'error', message: errMsg });
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
      <div className="bg-white rounded-2xl max-w-xl w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry max-h-[92vh]">
        
        {/* Header */}
        <div className="p-4 bg-slate-900 text-white border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-sky-500/20 text-sky-400 flex items-center justify-center border border-sky-400/30">
              <Edit3 className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-heading font-extrabold text-sm text-white">Chỉnh Sửa Thông Tin &amp; Vai Trò Người Dùng</h3>
              <p className="text-[11px] text-slate-400 font-mono">ID: {user.userId?.substring(0, 8)}... • Mã: {user.userCode || 'Chưa cấp'}</p>
            </div>
          </div>
          <button 
            type="button" 
            onClick={onClose} 
            className="text-slate-400 hover:text-white p-1 rounded-lg transition focus:outline-none focus:ring-2 focus:ring-sky-500 cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs overflow-y-auto">
          {formError && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 flex items-center gap-2 font-medium">
              <ShieldAlert className="w-4 h-4 shrink-0 text-rose-500" />
              <span>{formError}</span>
            </div>
          )}

          {isEditingSelf && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-900 flex items-start gap-2.5 text-[11px] font-medium leading-relaxed">
              <AlertTriangle className="w-4 h-4 shrink-0 text-amber-600 mt-0.5" />
              <span><strong>Cơ chế tự bảo vệ an toàn (Safety Guard):</strong> Bạn đang chỉnh sửa tài khoản của chính mình. Hệ thống không cho phép bạn tự hạ quyền Administrator hoặc tự khóa tài khoản để tránh mất quyền quản trị.</span>
            </div>
          )}

          {/* User Hero Preview */}
          <div className="p-3.5 bg-paper border-2 border-green-500 rounded-xl flex items-center gap-3 shadow-[2px_2px_0px_#DEB9B5]">
            <div className="w-12 h-12 rounded-xl border-2 border-green-600 shadow-[2px_2px_0px_#DEB9B5] overflow-hidden shrink-0 bg-pink-100 flex items-center justify-center font-heading font-black text-sm text-pink-900">
              {(user.avatarUrl || user.avatar || user.avatar_url) ? (
                <img
                  src={(user.avatarUrl || user.avatar || user.avatar_url).trim()}
                  alt={user.fullName || 'Avatar'}
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                    if (e.currentTarget.nextSibling) {
                      e.currentTarget.nextSibling.style.display = 'flex';
                    }
                  }}
                />
              ) : null}
              <span className={`w-full h-full flex items-center justify-center ${(user.avatarUrl || user.avatar || user.avatar_url) ? 'hidden' : ''}`}>
                {user.fullName ? user.fullName.substring(0, 2).toUpperCase() : 'US'}
              </span>
            </div>
            <div className="min-w-0 flex-1">
              <p className="font-heading font-bold text-sm text-ink truncate">{user.fullName}</p>
              <p className="text-on-surface-variant text-[11px] font-mono truncate">{user.email}</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-slate-700 mb-1">Email (Khóa cố định):</label>
              <input
                type="email"
                disabled
                value={user.email || ''}
                className="w-full p-2.5 rounded-xl border border-slate-200 bg-slate-100 text-slate-500 font-mono text-xs cursor-not-allowed font-medium"
              />
            </div>
            <div>
              <label className="block font-bold text-slate-700 mb-1">Mã Người Dùng (User Code):</label>
              <input
                type="text"
                disabled
                value={user.userCode || ''}
                className="w-full p-2.5 rounded-xl border border-slate-200 bg-slate-100 text-slate-500 font-mono text-xs cursor-not-allowed font-medium"
              />
            </div>
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Họ Và Tên (*):</label>
            <input
              ref={fullNameInputRef}
              type="text"
              required
              maxLength={150}
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition font-medium"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-slate-700 mb-1">Số Điện Thoại:</label>
              <input
                type="text"
                maxLength={20}
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="0912345678"
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition font-medium"
              />
            </div>
            <div>
              <label className="block font-bold text-slate-700 mb-1">Trạng Thái Hoạt Động:</label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                disabled={isEditingSelf}
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 text-slate-800 text-xs bg-white disabled:bg-slate-100 disabled:cursor-not-allowed transition cursor-pointer font-medium"
              >
                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                <option value="INACTIVE">Vô hiệu hóa (INACTIVE)</option>
                <option value="BANNED">Khóa vi phạm (BANNED)</option>
              </select>
            </div>
          </div>

          {/* Strict Single Role Selection */}
          <div>
            <label className="block font-bold text-slate-700 mb-1.5">
              Vai Trò Hệ Thống <span className="text-slate-400 font-normal">(chọn 1 vai trò duy nhất)</span>:
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5" role="radiogroup" aria-label="Vai trò hệ thống">
              {ROLE_OPTIONS.map(({ code, description }) => {
                const isChecked = selectedRole === code;
                const isAdmin = code === 'ROLE_ADMIN';
                const isLecturer = code === 'ROLE_LECTURER';
                const isDisabled = isEditingSelf && !isAdmin;

                return (
                  <div
                    key={code}
                    role="radio"
                    aria-checked={isChecked}
                    tabIndex={isDisabled ? -1 : 0}
                    onClick={() => !isDisabled && selectRole(code)}
                    onKeyDown={(e) => !isDisabled && (e.key === ' ' || e.key === 'Enter') && selectRole(code)}
                    className={`p-3 rounded-xl border select-none transition-all flex flex-col justify-between ${
                      isDisabled
                        ? 'opacity-40 cursor-not-allowed bg-slate-50 border-slate-200'
                        : isChecked
                        ? isAdmin
                          ? 'border-purple-600 bg-purple-50/70 text-purple-950 ring-2 ring-purple-500/20 cursor-pointer'
                          : isLecturer
                          ? 'border-sky-600 bg-sky-50/70 text-sky-950 ring-2 ring-sky-500/20 cursor-pointer'
                          : 'border-slate-800 bg-slate-50 text-slate-900 ring-2 ring-slate-400/20 cursor-pointer'
                        : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50/50 text-slate-600 cursor-pointer'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="font-heading font-bold text-xs">
                        {isAdmin ? 'Quản Trị Viên' : isLecturer ? 'Giảng Viên' : 'Sinh Viên'}
                      </span>
                      <div className={`w-4 h-4 rounded-full flex items-center justify-center border transition ${
                        isChecked 
                          ? isAdmin ? 'bg-purple-600 border-purple-600 text-white' : isLecturer ? 'bg-sky-600 border-sky-600 text-white' : 'bg-slate-900 border-slate-900 text-white'
                          : 'border-slate-300 bg-white'
                      }`}>
                        {isChecked && <Check className="w-2.5 h-2.5 stroke-[3]" />}
                      </div>
                    </div>
                    <p className="text-[11px] leading-tight text-slate-500">
                      {description}
                    </p>
                    <span className="font-mono text-[9px] text-slate-400 mt-2 font-semibold">
                      {code}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Footer Actions */}
          <div className="p-4 bg-slate-50 border-t border-slate-200/80 flex items-center justify-end gap-2.5 -mx-5 -mb-5 mt-5">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold text-xs transition cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500 cursor-pointer"
            >
              {isSubmitting && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
              <span>Lưu Cập Nhật</span>
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}
