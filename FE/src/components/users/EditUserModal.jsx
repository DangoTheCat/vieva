import React, { useState, useEffect, useRef } from 'react';
import { adminUserApi } from '../../api/adminUserApi';
import { ASSIGNABLE_ROLE_OPTIONS, getUserRole } from '../../utils/roles';
import { getErrorMessage } from '../../utils/errorCodes';
import { useAuth } from '../../context/AuthContext';
import { Edit3, X, ShieldAlert, Check, AlertTriangle } from 'lucide-react';

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
  // Self-registered ROLE_USER accounts keep that role until the admin picks an assignable one
  const isUnassignableRole = !!originalRole && !ASSIGNABLE_ROLE_OPTIONS.some(({ code }) => code === originalRole);
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

    if (!fullName.trim()) {
      setFormError('Họ và tên không được để trống.');
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
        phoneNumber: phoneNumber.trim(),
        status,
        // Only send the role when it changed, so editing a ROLE_USER account's other fields still works
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
      className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
    >
      <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry max-h-[90vh]">
        
        {/* Header */}
        <div className="p-4 bg-sidebarBg text-white border-b border-slate-700 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-sky-500/20 text-sky-300 flex items-center justify-center">
              <Edit3 className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-heading font-extrabold text-sm text-white">Chỉnh Sửa Quyền & Thông Tin Người Dùng</h3>
              <p className="text-[11px] text-slate-400 font-mono">PUT /api/v1/admin/users/{user.userId?.substring(0, 8)}...</p>
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs overflow-y-auto">
          {formError && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 flex items-center gap-2 font-medium">
              <ShieldAlert className="w-4 h-4 shrink-0 text-rose-500" />
              <span>{formError}</span>
            </div>
          )}

          {isEditingSelf && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 flex items-center gap-2 text-[11px] font-medium">
              <AlertTriangle className="w-4 h-4 shrink-0 text-amber-600" />
              <span>Lưu ý: Bạn đang chỉnh sửa tài khoản của chính mình. Cơ chế an toàn (Safety Guard) không cho phép bạn tự hạ quyền Admin hoặc tự khóa tài khoản.</span>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-slate-700 mb-1">Email (Khóa cố định):</label>
              <input
                type="email"
                disabled
                value={user.email || ''}
                className="w-full p-2.5 rounded-xl border border-slate-200 bg-slate-100 text-slate-500 font-mono text-xs cursor-not-allowed"
              />
            </div>
            <div>
              <label className="block font-bold text-slate-700 mb-1">Mã Người Dùng (User Code):</label>
              <input
                type="text"
                disabled
                value={user.userCode || ''}
                className="w-full p-2.5 rounded-xl border border-slate-200 bg-slate-100 text-slate-500 font-mono text-xs cursor-not-allowed"
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
              className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition"
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
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition"
              />
            </div>
            <div>
              <label className="block font-bold text-slate-700 mb-1">Trạng Thái Hoạt Động:</label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                disabled={isEditingSelf}
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 text-slate-800 text-xs bg-white disabled:bg-slate-100 disabled:cursor-not-allowed transition"
              >
                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                <option value="INACTIVE">Vô hiệu hóa (INACTIVE)</option>
                <option value="BANNED">Khóa vi phạm (BANNED)</option>
              </select>
            </div>
          </div>

          {/* Role Radio (an account has exactly one role) */}
          <div>
            <label className="block font-bold text-slate-700 mb-1.5">Vai Trò Hệ Thống (chọn 1):</label>
            {isUnassignableRole && (
              <p className="text-[11px] text-amber-700 mb-1.5">
                Tài khoản đang có vai trò {originalRole} (tự đăng ký). Chọn một vai trò bên dưới để đổi, hoặc giữ nguyên.
              </p>
            )}
            <div className="grid grid-cols-2 gap-2" role="radiogroup">
              {ASSIGNABLE_ROLE_OPTIONS.map(({ code, description }) => {
                const isChecked = selectedRole === code;
                const isAdminRole = code === 'ROLE_ADMIN';
                return (
                  <label
                    key={code}
                    role="radio"
                    aria-checked={isChecked}
                    onClick={() => selectRole(code)}
                    className={`flex items-center gap-2 p-2.5 rounded-xl border cursor-pointer select-none transition ${
                      isChecked
                        ? isAdminRole ? 'border-purple-500 bg-purple-50 text-purple-900 font-semibold' : 'border-sky-500 bg-sky-50 text-sky-900 font-semibold'
                        : 'border-slate-200 hover:bg-slate-50 text-slate-600'
                    }`}
                  >
                    <div className={`w-4 h-4 rounded-full flex items-center justify-center border ${
                      isChecked ? (isAdminRole ? 'bg-purple-600 border-purple-600 text-white' : 'bg-sky-600 border-sky-600 text-white') : 'border-slate-300'
                    }`}>
                      {isChecked && <Check className="w-3 h-3" />}
                    </div>
                    <div>
                      <p className="text-xs">{code}</p>
                      <p className="text-[10px] text-slate-400">{description}</p>
                    </div>
                  </label>
                );
              })}
            </div>
          </div>

          {/* Footer Actions */}
          <div className="p-4 bg-slate-50/80 border-t border-slate-100 flex items-center justify-end gap-2.5 -mx-5 -mb-5 mt-5">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold text-xs transition"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500"
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
