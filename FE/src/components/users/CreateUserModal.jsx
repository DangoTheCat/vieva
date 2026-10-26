import React, { useState, useEffect, useRef } from 'react';
import { adminUserApi } from '../../api/adminUserApi';
import { ROLE_OPTIONS } from '../../utils/roles';
import { getErrorMessage } from '../../utils/errorCodes';
import { UserPlus, X, ShieldAlert, Check, Eye, EyeOff, Info, KeyRound, ShieldCheck, GraduationCap, UserCheck } from 'lucide-react';

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
      className="fixed inset-0 z-50 bg-ink/65 backdrop-blur-[2px] flex items-center justify-center p-4 transition-all duration-200"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-create-user-title"
    >
      <div className="bg-paper rounded-2xl max-w-xl w-full border-2 border-green-500 shadow-[6px_6px_0px_#DEB9B5] overflow-hidden flex flex-col animate-modal-entry max-h-[92vh] text-ink">
        
        {/* Header */}
        <div className="p-4 sm:p-5 bg-pink-50 border-b-2 border-green-200 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-pink-100 text-pink-700 border-2 border-green-500 flex items-center justify-center shadow-[2px_2px_0px_#DEB9B5] shrink-0">
              <UserPlus className="w-5 h-5" />
            </div>
            <div>
              <h3 id="modal-create-user-title" className="font-heading font-bold text-base sm:text-lg text-pink-800 tracking-tight leading-tight">
                Thêm Mới Người Dùng (Admin Provision)
              </h3>
              <p className="text-xs text-pink-900/80 font-sans mt-0.5">
                Thiết lập tài khoản người dùng theo chuẩn Single-Role RBAC
              </p>
            </div>
          </div>
          <button 
            type="button" 
            onClick={onClose} 
            className="w-8 h-8 rounded-lg border-2 border-green-500 bg-white hover:bg-pink-100 flex items-center justify-center text-ink shadow-[2px_2px_0px_#DEB9B5] hover:shadow-[1px_1px_0px_#DEB9B5] hover:translate-x-[1px] hover:translate-y-[1px] active:shadow-none active:translate-x-[2px] active:translate-y-[2px] transition-all focus:outline-none focus:ring-2 focus:ring-green-600 cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Container with scrollable fields and pinned footer */}
        <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden">
          <div className="p-5 sm:p-6 space-y-4 text-xs overflow-y-auto flex-1 bg-paper">
            {formError && (
              <div className="p-3.5 bg-pink-100/90 border-2 border-pink-700 rounded-xl text-pink-900 flex items-center gap-2.5 font-medium shadow-[2px_2px_0px_#DEB9B5]">
                <ShieldAlert className="w-4 h-4 shrink-0 text-pink-700" />
                <span className="leading-snug">{formError}</span>
              </div>
            )}

            {/* RBAC Single Role Architecture Banner */}
            <div className="p-3.5 bg-green-50 border-2 border-green-400 rounded-xl text-green-950 flex items-start gap-2.5 text-[11px] leading-relaxed shadow-[2px_2px_0px_#DEB9B5]">
              <Info className="w-4 h-4 shrink-0 text-green-700 mt-0.5" />
              <div>
                <strong className="font-bold text-green-900">Nguyên tắc Một Người Dùng - Một Vai Trò Duy Nhất:</strong>
                <p className="text-green-800 mt-0.5 leading-relaxed">
                  Mỗi tài khoản chỉ thuộc đúng một vai trò duy nhất trong hệ thống VIEVA. Giảng viên phụ trách môn học sẽ được phân quyền phạm vi môn học (Course-Scoped) độc lập tại phân hệ Quản lý Môn học.
                </p>
              </div>
            </div>

            <div>
              <label className="block font-bold text-ink mb-1.5 text-xs">Email Học Thuật (*):</label>
              <input
                ref={emailInputRef}
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="giangvien@fpt.edu.vn hoặc sinhvien@fpt.edu.vn"
                className="w-full h-11 px-3.5 rounded-xl border-2 border-green-300 focus:border-green-600 focus:ring-2 focus:ring-green-300 bg-white text-ink text-xs transition placeholder:text-green-400/80 font-medium shadow-sm outline-none"
              />
            </div>

            <div>
              <label className="block font-bold text-ink mb-1.5 text-xs">Họ Và Tên (*):</label>
              <input
                type="text"
                required
                maxLength={150}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nguyễn Văn A"
                className="w-full h-11 px-3.5 rounded-xl border-2 border-green-300 focus:border-green-600 focus:ring-2 focus:ring-green-300 bg-white text-ink text-xs transition placeholder:text-green-400/80 font-medium shadow-sm outline-none"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block font-bold text-ink mb-1.5 text-xs">Mã Định Danh (MSSV / Code):</label>
                <input
                  type="text"
                  value={userCode}
                  onChange={(e) => setUserCode(e.target.value)}
                  placeholder="Bỏ trống để tự sinh..."
                  className="w-full h-11 px-3.5 rounded-xl border-2 border-green-300 focus:border-green-600 focus:ring-2 focus:ring-green-300 bg-white text-ink text-xs font-mono transition placeholder:text-green-400/80 shadow-sm outline-none"
                />
              </div>
              <div>
                <label className="block font-bold text-ink mb-1.5 text-xs">Số Điện Thoại:</label>
                <input
                  type="text"
                  maxLength={20}
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="0912345678"
                  className="w-full h-11 px-3.5 rounded-xl border-2 border-green-300 focus:border-green-600 focus:ring-2 focus:ring-green-300 bg-white text-ink text-xs transition placeholder:text-green-400/80 font-medium shadow-sm outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block font-bold text-ink mb-1.5 text-xs">Mật Khẩu Tạm Thời:</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    minLength={6}
                    maxLength={72}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Bỏ trống để tự sinh..."
                    className="w-full h-11 px-3.5 pr-10 rounded-xl border-2 border-green-300 focus:border-green-600 focus:ring-2 focus:ring-green-300 bg-white text-ink text-xs transition placeholder:text-green-400/80 font-medium shadow-sm outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-green-600 hover:text-green-800 focus:outline-none p-1 rounded-lg cursor-pointer transition-colors"
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>
              <div>
                <label className="block font-bold text-ink mb-1.5 text-xs">Trạng Thái Khởi Tạo:</label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  className="w-full h-11 px-3.5 rounded-xl border-2 border-green-300 focus:border-green-600 focus:ring-2 focus:ring-green-300 text-ink text-xs bg-white transition cursor-pointer shadow-sm outline-none font-medium"
                >
                  <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                  <option value="INACTIVE">Chờ kích hoạt (INACTIVE)</option>
                  <option value="BANNED">Tạm khóa (BANNED)</option>
                </select>
              </div>
            </div>

            {/* Password Policy Callout */}
            <div className="text-[11px] text-pink-950 leading-normal bg-pink-50 p-3 rounded-xl border-2 border-pink-300 shadow-[2px_2px_0px_#DEB9B5] flex items-start gap-2.5">
              <KeyRound className="w-4 h-4 text-pink-700 shrink-0 mt-0.5" />
              <span>Nếu để trống mật khẩu, hệ thống sẽ tự động tạo mật khẩu ngẫu nhiên an toàn. Người dùng bắt buộc phải đổi mật khẩu ở lần đăng nhập đầu tiên.</span>
            </div>

            {/* Strict Single-Role Selection Cards */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block font-bold text-ink text-xs">
                  Vai Trò Hệ Thống <span className="text-pink-900/70 font-normal">(chọn 1 vai trò duy nhất)</span>:
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
                      className={`p-3 rounded-xl border-2 cursor-pointer select-none transition-all flex flex-col justify-between ${
                        isChecked
                          ? isAdmin
                            ? 'border-sand bg-sand/20 text-ink shadow-[3px_3px_0px_#CFB997] ring-2 ring-sand/40'
                            : isLecturer
                            ? 'border-green-600 bg-green-100 text-ink shadow-[3px_3px_0px_#819266] ring-2 ring-green-500/30'
                            : 'border-pink-500 bg-pink-100 text-ink shadow-[3px_3px_0px_#DEB9B5] ring-2 ring-pink-400/30'
                          : 'border-green-300/80 bg-white hover:border-green-500 hover:bg-green-50/50 text-ink/80 shadow-sm'
                      }`}
                    >
                      <div>
                        <div className="flex items-center justify-between mb-1.5">
                          <div className="flex items-center gap-1.5">
                            {isAdmin && <ShieldCheck className="w-3.5 h-3.5 text-sand" />}
                            {isLecturer && <GraduationCap className="w-3.5 h-3.5 text-green-700" />}
                            {!isAdmin && !isLecturer && <UserCheck className="w-3.5 h-3.5 text-pink-700" />}
                            <span className="font-heading font-bold text-xs text-ink">
                              {isAdmin ? 'Quản Trị Viên' : isLecturer ? 'Giảng Viên' : 'Sinh Viên'}
                            </span>
                          </div>
                          <div className={`w-5 h-5 rounded-full flex items-center justify-center border-2 transition ${
                            isChecked 
                              ? isAdmin
                                ? 'bg-sand border-ink text-ink'
                                : isLecturer
                                ? 'bg-green-600 border-green-800 text-white'
                                : 'bg-pink-600 border-pink-700 text-white'
                              : 'border-green-300 bg-white'
                          }`}>
                            {isChecked && <Check className="w-3 h-3 stroke-[3]" />}
                          </div>
                        </div>
                        <p className="text-[11px] leading-tight text-ink/70">
                          {description}
                        </p>
                      </div>
                      <div className="mt-2.5">
                        <span className={`font-mono text-[9px] px-1.5 py-0.5 rounded font-semibold border ${
                          isChecked
                            ? isAdmin
                              ? 'bg-sand/30 text-ink border-sand'
                              : isLecturer
                              ? 'bg-green-200 text-green-900 border-green-400'
                              : 'bg-pink-200 text-pink-900 border-pink-300'
                            : 'bg-green-50 text-green-700 border-green-200'
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
          <div className="p-4 sm:p-5 bg-green-50/70 border-t-2 border-green-200 flex items-center justify-end gap-3 shrink-0">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-5 py-2 h-11 min-h-[44px] rounded-xl border-2 border-green-500 bg-white hover:bg-green-100 text-green-900 font-bold text-xs transition shadow-[2px_2px_0px_#819266] hover:shadow-[1px_1px_0px_#819266] hover:translate-x-[1px] hover:translate-y-[1px] active:shadow-none active:translate-x-[2px] active:translate-y-[2px] focus:outline-none focus:ring-2 focus:ring-green-500 cursor-pointer disabled:opacity-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2 h-11 min-h-[44px] rounded-xl bg-pink-500 hover:bg-pink-400 active:bg-pink-600 border-2 border-ink text-ink font-bold text-xs shadow-[3px_3px_0px_#819266] hover:shadow-[1px_1px_0px_#819266] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all disabled:opacity-50 flex items-center gap-2 focus:outline-none focus:ring-2 focus:ring-green-600 cursor-pointer"
            >
              {isSubmitting && <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>}
              <span>Tạo Tài Khoản</span>
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}
