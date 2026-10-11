import React, { useState, useEffect, useRef } from 'react';
import { adminUserApi } from '../../api/adminUserApi';
import { ROLE_OPTIONS } from '../../utils/roles';
import { getErrorMessage } from '../../utils/errorCodes';
import { useAuth } from '../../context/AuthContext';
import { UserPlus, X, ShieldAlert, Check, Eye, EyeOff, ShieldCheck, Info } from 'lucide-react';

export function CreateUserModal({ isOpen, onClose, onSuccess, showToast }) {
  const { isDemoMode } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [userCode, setUserCode] = useState('');
  const [status, setStatus] = useState('ACTIVE');
  const [selectedRole, setSelectedRole] = useState('ROLE_STUDENT');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  const emailInputRef = useRef(null);

  // Keyboard accessibility: Escape to close & autofocus
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    const timer = setTimeout(() => emailInputRef.current?.focus(), 50);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      clearTimeout(timer);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!email.trim() || !/\S+@\S+\.\S+/.test(email.trim())) {
      setFormError('Email không hợp lệ. Vui lòng nhập đúng định dạng email học thuật (ví dụ: giangvien@fpt.edu.vn).');
      return;
    }
    // Password is optional: left blank, the BE generates a temporary one and emails it
    if (password && password.length < 6) {
      setFormError('Mật khẩu tạm thời phải có tối thiểu 6 ký tự (hoặc bỏ trống để hệ thống tự sinh ngẫu nhiên).');
      return;
    }
    if (!fullName.trim() || fullName.trim().length < 2) {
      setFormError('Họ và tên không được để trống (tối thiểu 2 ký tự).');
      return;
    }
    if (phoneNumber && !/^(\+?[0-9]{9,11})$/.test(phoneNumber.trim())) {
      setFormError('Số điện thoại phải từ 9 đến 11 chữ số.');
      return;
    }

    setIsSubmitting(true);
    try {
      // Backend expects a single roleCode string (1 account = 1 role)
      const payload = {
        email: email.trim().toLowerCase(),
        password: password || null,
        fullName: fullName.trim(),
        phoneNumber: phoneNumber ? phoneNumber.trim() : null,
        userCode: userCode ? userCode.trim() : null,
        status,
        roleCode: selectedRole
      };

      if (isDemoMode) {
        const dummyCreated = {
          userId: crypto.randomUUID ? crypto.randomUUID() : 'demo-' + Date.now(),
          email: payload.email,
          userCode: payload.userCode || 'USR-' + Math.floor(100000 + Math.random() * 900000),
          fullName: payload.fullName,
          phoneNumber: payload.phoneNumber,
          status: payload.status,
          role: payload.roleCode,
          roles: [payload.roleCode],
          mustChangePassword: true,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        };
        showToast({ type: 'success', message: `Tạo người dùng ${dummyCreated.fullName} thành công (Demo Mode)!` });
        onSuccess(dummyCreated);
        resetForm();
        onClose();
        return;
      }

      const created = await adminUserApi.createUser(payload);
      showToast({
        type: 'success',
        message: `Tạo thành công tài khoản cho ${created.fullName}! Email cấp tài khoản đã được chuyển giao.`
      });
      onSuccess(created);
      resetForm();
      onClose();
    } catch (err) {
      const errMsg = getErrorMessage(err);
      setFormError(errMsg);
      showToast({ type: 'error', message: errMsg });
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setEmail('');
    setPassword('');
    setShowPassword(false);
    setFullName('');
    setPhoneNumber('');
    setUserCode('');
    setStatus('ACTIVE');
    setSelectedRole('ROLE_STUDENT');
    setFormError('');
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
              <UserPlus className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-heading font-extrabold text-sm text-white">Thêm Mới Người Dùng (Admin Provision)</h3>
              <p className="text-[11px] text-slate-400">Thiết lập tài khoản người dùng theo chuẩn Single-Role RBAC</p>
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

          {/* RBAC Single Role Architecture Banner */}
          <div className="p-3 bg-sky-50/70 border border-sky-200 rounded-xl text-sky-900 flex items-start gap-2.5 text-[11px] leading-relaxed">
            <Info className="w-4 h-4 shrink-0 text-sky-600 mt-0.5" />
            <div>
              <strong className="font-semibold text-sky-950">Nguyên tắc Một Người Dùng - Một Vai Trò Duy Nhất:</strong>
              <p className="text-sky-800 mt-0.5">
                Mỗi tài khoản chỉ thuộc đúng một vai trò duy nhất trong hệ thống AIVES. Giảng viên phụ trách môn học sẽ được phân quyền phạm vi môn học (Course-Scoped) độc lập tại phân hệ Quản lý Môn học.
              </p>
            </div>
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Email Học Thuật (*):</label>
            <input
              ref={emailInputRef}
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="giangvien@fpt.edu.vn hoặc sinhvien@fpt.edu.vn"
              className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition placeholder:text-slate-400 font-medium"
            />
          </div>

          <div>
            <label className="block font-bold text-slate-700 mb-1">Họ Và Tên (*):</label>
            <input
              type="text"
              required
              maxLength={150}
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Nguyễn Văn A"
              className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition placeholder:text-slate-400 font-medium"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-slate-700 mb-1">Mã Định Danh (MSSV / Code):</label>
              <input
                type="text"
                value={userCode}
                onChange={(e) => setUserCode(e.target.value)}
                placeholder="Bỏ trống để tự sinh..."
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs font-mono transition placeholder:text-slate-400"
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
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition placeholder:text-slate-400"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-slate-700 mb-1">Mật Khẩu Tạm Thời:</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  minLength={6}
                  maxLength={72}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Bỏ trống để tự sinh..."
                  className="w-full p-2.5 pr-8 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition placeholder:text-slate-400"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600 focus:outline-none"
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
            <div>
              <label className="block font-bold text-slate-700 mb-1">Trạng Thái Khởi Tạo:</label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 text-slate-800 text-xs bg-white transition cursor-pointer"
              >
                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                <option value="INACTIVE">Chờ kích hoạt (INACTIVE)</option>
                <option value="BANNED">Tạm khóa (BANNED)</option>
              </select>
            </div>
          </div>

          <p className="text-[11px] text-slate-500">
            Nếu để trống mật khẩu, hệ thống sẽ tự động tạo mật khẩu ngẫu nhiên an toàn. Người dùng bắt buộc phải đổi mật khẩu ở lần đăng nhập đầu tiên.
          </p>

          {/* Strict Single-Role Selection Cards */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="block font-bold text-slate-700">
                Vai Trò Hệ Thống <span className="text-slate-400 font-normal">(chọn 1 vai trò duy nhất)</span>:
              </label>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5" role="radiogroup" aria-label="Vai trò hệ thống">
              {ROLE_OPTIONS.map(({ code, description }) => {
                const isChecked = selectedRole === code;
                const isAdmin = code === 'ROLE_ADMIN';
                const isLecturer = code === 'ROLE_LECTURER';

                return (
                  <div
                    key={code}
                    role="radio"
                    aria-checked={isChecked}
                    tabIndex={0}
                    onClick={() => setSelectedRole(code)}
                    onKeyDown={(e) => (e.key === ' ' || e.key === 'Enter') && setSelectedRole(code)}
                    className={`p-3 rounded-xl border cursor-pointer select-none transition-all flex flex-col justify-between ${
                      isChecked
                        ? isAdmin
                          ? 'border-purple-600 bg-purple-50/70 text-purple-950 ring-2 ring-purple-500/20'
                          : isLecturer
                          ? 'border-sky-600 bg-sky-50/70 text-sky-950 ring-2 ring-sky-500/20'
                          : 'border-slate-800 bg-slate-50 text-slate-900 ring-2 ring-slate-400/20'
                        : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50/50 text-slate-600'
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
              <span>Tạo Tài Khoản</span>
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}
