import React, { useState, useEffect, useRef } from 'react';
import { adminUserApi } from '../../api/adminUserApi';
import { ROLE_OPTIONS } from '../../utils/roles';
import { getErrorMessage } from '../../utils/errorCodes';
import { UserPlus, X, ShieldAlert, Check, Eye, EyeOff, KeyRound, ShieldCheck, GraduationCap, UserCheck } from 'lucide-react';

export function CreateUserModal({ isOpen, onClose, onSuccess, showToast }) {

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
      className="fixed inset-0 z-50 bg-pink-600/60 backdrop-blur-xs flex items-center justify-center p-4 transition-all duration-200"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-create-user-title"
    >
      <div className="bg-white rounded-2xl max-w-xl w-full border border-pink-200 shadow-xl overflow-hidden flex flex-col animate-modal-entry max-h-[92vh] text-ink">
        
        {/* Header */}
        <div className="p-4 sm:p-5 bg-white border-b border-pink-200 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-slate-100 text-slate-800 border border-pink-200 flex items-center justify-center shadow-2xs shrink-0">
              <UserPlus className="w-5 h-5 text-slate-700" />
            </div>
            <div>
              <h3 id="modal-create-user-title" className="font-heading font-bold text-base sm:text-lg text-ink tracking-tight leading-tight">
                Thêm Mới Người Dùng
              </h3>
              <p className="text-xs text-slate-500 font-sans mt-0.5">
                Cấp tài khoản mới cho Giảng viên, Sinh viên hoặc Quản trị viên
              </p>
            </div>
          </div>
          <button 
            type="button" 
            onClick={onClose} 
            className="w-8 h-8 rounded-lg border border-pink-200 bg-white hover:bg-slate-100 flex items-center justify-center text-slate-500 hover:text-ink transition-colors cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Container with scrollable fields and pinned footer */}
        <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden">
          <div className="p-5 sm:p-6 space-y-4 text-xs overflow-y-auto flex-1 bg-white">
            {formError && (
              <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 flex items-center gap-2.5 font-medium shadow-2xs">
                <ShieldAlert className="w-4 h-4 shrink-0 text-rose-600" />
                <span className="leading-snug">{formError}</span>
              </div>
            )}

            <div>
              <label className="block font-semibold text-slate-700 mb-1.5 text-xs">Email Học Thuật (*):</label>
              <input
                ref={emailInputRef}
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="giangvien@fpt.edu.vn hoặc sinhvien@fpt.edu.vn"
                className="w-full h-10 px-3.5 rounded-xl border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 bg-white text-ink text-xs transition placeholder:text-slate-400 font-medium shadow-2xs outline-none"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1.5 text-xs">Họ Và Tên (*):</label>
              <input
                type="text"
                required
                maxLength={150}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nguyễn Văn A"
                className="w-full h-10 px-3.5 rounded-xl border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 bg-white text-ink text-xs transition placeholder:text-slate-400 font-medium shadow-2xs outline-none"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block font-semibold text-slate-700 mb-1.5 text-xs">Mã Định Danh (MSSV / Code):</label>
                <input
                  type="text"
                  value={userCode}
                  onChange={(e) => setUserCode(e.target.value)}
                  placeholder="Bỏ trống để tự sinh..."
                  className="w-full h-10 px-3.5 rounded-xl border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 bg-white text-ink text-xs font-mono transition placeholder:text-slate-400 shadow-2xs outline-none"
                />
              </div>
              <div>
                <label className="block font-semibold text-slate-700 mb-1.5 text-xs">Số Điện Thoại:</label>
                <input
                  type="text"
                  maxLength={20}
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="0912345678"
                  className="w-full h-10 px-3.5 rounded-xl border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 bg-white text-ink text-xs transition placeholder:text-slate-400 font-medium shadow-2xs outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block font-semibold text-slate-700 mb-1.5 text-xs">Mật Khẩu Tạm Thời:</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    minLength={6}
                    maxLength={72}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Bỏ trống để tự sinh..."
                    className="w-full h-10 px-3.5 pr-10 rounded-xl border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 bg-white text-ink text-xs transition placeholder:text-slate-400 font-medium shadow-2xs outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none p-1 rounded-lg cursor-pointer transition-colors"
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>
              <div>
                <label className="block font-semibold text-slate-700 mb-1.5 text-xs">Trạng Thái Khởi Tạo:</label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  className="w-full h-10 px-3.5 rounded-xl border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 text-ink text-xs bg-white transition cursor-pointer shadow-2xs outline-none font-medium"
                >
                  <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                  <option value="INACTIVE">Chờ kích hoạt (INACTIVE)</option>
                  <option value="BANNED">Tạm khóa (BANNED)</option>
                </select>
              </div>
            </div>

            {/* Password Policy Callout */}
            <div className="text-[11px] text-slate-600 leading-normal bg-slate-50 p-3 rounded-xl border border-pink-200 shadow-2xs flex items-start gap-2.5">
              <KeyRound className="w-4 h-4 text-slate-500 shrink-0 mt-0.5" />
              <span>Nếu để trống mật khẩu, hệ thống sẽ tự động tạo mật khẩu ngẫu nhiên an toàn. Người dùng bắt buộc phải đổi mật khẩu ở lần đăng nhập đầu tiên.</span>
            </div>

            {/* Strict Single-Role Selection Cards */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block font-semibold text-slate-700 text-xs">
                  Vai Trò Người Dùng:
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5" role="radiogroup" aria-label="Vai trò hệ thống">
                {ROLE_OPTIONS.map(({ code, description }) => {
                  const isChecked = selectedRole === code;
                  const isAdminRole = code === 'ROLE_ADMIN';
                  const isLecturerRole = code === 'ROLE_LECTURER';

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
                          ? 'border-slate-900 bg-slate-50/80 ring-1 ring-slate-900 shadow-xs'
                          : 'border-pink-200 bg-white hover:border-pink-200 hover:bg-slate-50/50 shadow-2xs'
                      }`}
                    >
                      <div>
                        <div className="flex items-center justify-between mb-1.5">
                          <div className="flex items-center gap-1.5">
                            {isAdminRole && <ShieldCheck className="w-3.5 h-3.5 text-slate-700" />}
                            {isLecturerRole && <GraduationCap className="w-3.5 h-3.5 text-pink-600" />}
                            {!isAdminRole && !isLecturerRole && <UserCheck className="w-3.5 h-3.5 text-slate-700" />}
                            <span className="font-heading font-bold text-xs text-ink">
                              {isAdminRole ? 'Quản Trị Viên' : isLecturerRole ? 'Giảng Viên' : 'Sinh Viên'}
                            </span>
                          </div>
                          <div className={`w-4 h-4 rounded-full flex items-center justify-center border transition ${
                            isChecked 
                              ? 'bg-pink-600 border-slate-900 text-white'
                              : 'border-pink-200 bg-white'
                          }`}>
                            {isChecked && <Check className="w-2.5 h-2.5 stroke-[3]" />}
                          </div>
                        </div>
                        <p className="text-[11px] leading-tight text-slate-500">
                          {description}
                        </p>
                      </div>
                      <div className="mt-2.5">
                        <span className={`font-mono text-[9px] px-1.5 py-0.5 rounded font-semibold border ${
                          isChecked
                            ? 'bg-slate-200 text-slate-800 border-pink-200'
                            : 'bg-slate-100 text-slate-500 border-pink-200'
                        }`}>
                          {code}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="p-4 sm:p-5 bg-slate-50 border-t border-pink-200 flex items-center justify-end gap-3 shrink-0">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 h-9 rounded-xl border border-pink-200 bg-white hover:bg-slate-100 text-slate-700 font-semibold text-xs transition cursor-pointer disabled:opacity-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 h-9 rounded-xl bg-pink-600 hover:bg-pink-700 active:bg-pink-800 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 flex items-center gap-2 cursor-pointer"
            >
              {isSubmitting && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
              <span>Tạo Tài Khoản</span>
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}
