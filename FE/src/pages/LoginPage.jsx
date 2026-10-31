import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import {
  Mail,
  Key,
  Eye,
  EyeOff,
  ShieldAlert,
  ShieldCheck,
  UserPlus,
  ArrowRight,
  Headphones,
  Volume2,
  FileCheck2,
  HelpCircle,
  School,
  GraduationCap,
  Shield
} from 'lucide-react';

const PORTAL_ROLES = [
  {
    code: 'ROLE_STUDENT',
    label: 'Sinh Viên',
    icon: GraduationCap,
    targetView: 'discover',
    placeholder: 'sinhvien@fpt.edu.vn'
  },
  {
    code: 'ROLE_LECTURER',
    label: 'Giảng Viên',
    icon: School,
    targetView: 'lecturer-dashboard',
    placeholder: 'giangvien@fpt.edu.vn'
  },
  {
    code: 'ROLE_ADMIN',
    label: 'Ban Khảo Thí',
    icon: Shield,
    targetView: 'admin-users',
    placeholder: 'admin@fpt.edu.vn'
  }
];

export function LoginPage({ onNavigate, showToast }) {
  const { login, isLiveBackendReachable } = useAuth();

  // Role portal selection - Standard webapp role method
  const [selectedRole, setSelectedRole] = useState('ROLE_STUDENT');

  // Form credentials - Starts empty, standard secure input
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [showForgotNotice, setShowForgotNotice] = useState(false);

  const emailInputRef = useRef(null);

  useEffect(() => {
    emailInputRef.current?.focus();
  }, [selectedRole]);

  const activePortal = PORTAL_ROLES.find(r => r.code === selectedRole) || PORTAL_ROLES[0];

  const handleRoleChange = (roleCode) => {
    setSelectedRole(roleCode);
    setErrorMsg('');
  };

  const handleSubmitLogin = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!email.trim() || !password) {
      setErrorMsg('Vui lòng nhập đầy đủ Email và Mật khẩu.');
      return;
    }

    setIsLoading(true);
    try {
      // Standard authentication via POST /api/v1/auth/login with strict role portal enforcement
      const result = await login({ email: email.trim(), password }, selectedRole);
      
      const effectiveRole = result.effectiveRole 
        || result.user?.selectedRole 
        || (result.user?.role?.startsWith('ROLE_') ? result.user.role : result.user?.role ? `ROLE_${result.user.role}` : selectedRole);

      if (showToast) {
        showToast({
          type: 'success',
          title: 'Đăng Nhập Thành Công',
          message: `Chào mừng ${result.user?.fullName || result.user?.email || 'bạn'} kết nối hệ thống AIVES!`
        });
      }

      // Route to selected portal view
      const targetConfig = PORTAL_ROLES.find(r => r.code === effectiveRole) || activePortal;
      onNavigate(targetConfig.targetView);
    } catch (err) {
      // Wrong email/password stays generic (OWASP A07); a role mismatch is only reached with the
      // correct password, so pointing at the right portal leaks nothing to a third party
      const actualPortal = PORTAL_ROLES.find(r => r.code === err.actualRole);
      const msg = err.code === 'ROLE_MISMATCH'
        ? `Tài khoản này không thuộc cổng ${activePortal.label}. Vui lòng chọn cổng ${actualPortal?.label || 'phù hợp'} rồi đăng nhập lại.`
        : err.code === '1003'
          ? 'Thông tin đăng nhập không chính xác.'
          : getErrorMessage(err);
      setErrorMsg(msg);
      if (showToast) {
        showToast({ type: 'error', title: 'Lỗi Đăng Nhập', message: msg });
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-between bg-canvasBg text-ink antialiased font-sans selection:bg-slate-200 selection:text-ink">

      {/* TOP HEADER BAR */}
      <header className="w-full max-w-7xl mx-auto px-6 py-4 flex items-center justify-between border-b border-pink-200/80 bg-white/80 backdrop-blur-sm sticky top-0 z-20">
        
        {/* Brand Logo */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-pink-700 text-white flex items-center justify-center shadow-xs">
            <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
            </svg>
          </div>
          <div>
            <span className="font-heading font-black text-xl tracking-tight text-ink">
              aives<span className="text-pink-600">.</span>
            </span>
            <span className="text-[11px] text-pink-800/80 font-medium block -mt-1">
              Hệ thống Khảo thí Vấn đáp AI • FPT University
            </span>
          </div>
        </div>

        {/* Header Right Actions */}
        <div className="flex items-center gap-3 text-xs">
          <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full bg-pink-50 border border-pink-200 text-pink-800 shadow-2xs">
            <span className={`w-2 h-2 rounded-full ${isLiveBackendReachable ? 'bg-emerald-500' : 'bg-rose-500'}`}></span>
            <span className="font-medium text-[11px]">Cổng dịch vụ trực tuyến</span>
          </div>

          <button
            onClick={() => onNavigate('register')}
            className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-white border border-pink-200 text-pink-800 hover:bg-pink-50 font-semibold transition text-xs shadow-2xs hover:bg-slate-50 hover:text-ink active:scale-[0.98] cursor-pointer"
          >
            <UserPlus className="w-3.5 h-3.5 text-pink-800/80" />
            <span>Đăng Ký</span>
          </button>
        </div>
      </header>

      {/* MAIN AUTHENTICATION CONTAINER */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-6 py-10 md:py-16 flex items-center">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-14 items-center w-full">

          {/* LEFT COLUMN: STANDARD AUTHENTICATION FORM WITH ROLE SELECTION */}
          <div className="lg:col-span-6 max-w-md w-full mx-auto lg:mx-0 space-y-6">

            {/* Headline */}
            <div className="space-y-1.5">
              <h1 className="font-heading font-bold text-2xl sm:text-3xl text-ink tracking-tight">
                Đăng nhập hệ thống
              </h1>
              <p className="text-pink-800/80 text-xs sm:text-sm leading-relaxed">
                Chọn cổng vai trò và đăng nhập bằng tài khoản học thuật FPT Education.
              </p>
            </div>

            {/* Main Form Card */}
            <div className="bg-white border border-pink-200 rounded-2xl p-6 sm:p-8 shadow-sm space-y-5">

              {/* Standard Role / Portal Segmented Tabs */}
              <div>
                <label className="block text-xs font-semibold text-ink mb-1.5">
                  Cổng đăng nhập theo vai trò:
                </label>
                <div role="group" aria-label="Cổng đăng nhập theo vai trò" className="p-1 bg-pink-100/70 rounded-xl grid grid-cols-3 gap-1 border border-pink-200/60 p-1">
                  {PORTAL_ROLES.map(({ code, label, icon: Icon }) => {
                    const isSelected = selectedRole === code;
                    return (
                      <button
                        key={code}
                        type="button"
                        aria-label={`Cổng ${label}`}
                        aria-pressed={isSelected}
                        onClick={() => handleRoleChange(code)}
                        className={`py-2 px-2.5 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-white text-pink-900 shadow-xs ring-1 ring-pink-300 font-bold'
                            : 'text-pink-800 hover:text-pink-900 hover:bg-pink-50/60'
                        }`}
                      >
                        <Icon className={`w-3.5 h-3.5 ${isSelected ? 'text-pink-600' : 'text-pink-600/70'}`} />
                        <span className="truncate">{label}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Error Message Alert */}
              {errorMsg && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-xs font-medium flex items-center gap-2 animate-in fade-in">
                  <ShieldAlert className="w-4 h-4 shrink-0 text-rose-600" />
                  <span>{errorMsg}</span>
                </div>
              )}

              {/* Login Form */}
              <form onSubmit={handleSubmitLogin} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-ink mb-1.5">
                    Email tài khoản trường <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      ref={emailInputRef}
                      type="text"
                      required
                      autoComplete="username"
                      value={email}
                      onChange={(e) => {
                        setEmail(e.target.value);
                        if (errorMsg) setErrorMsg('');
                      }}
                      placeholder={`vd: ${activePortal.placeholder}`}
                      className="w-full bg-white border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 rounded-xl py-2.5 pl-9 pr-3 text-xs text-ink placeholder:text-pink-600/70 shadow-2xs outline-none transition"
                    />
                    <Mail className="w-4 h-4 text-pink-600/70 absolute left-3 top-3" />
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-semibold text-ink">
                      Mật khẩu <span className="text-rose-500">*</span>
                    </label>
                    <button
                      type="button"
                      onClick={() => setShowForgotNotice(!showForgotNotice)}
                      className="text-[11px] text-pink-600 hover:text-pink-700 font-medium cursor-pointer"
                    >
                      Quên mật khẩu?
                    </button>
                  </div>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      required
                      autoComplete="current-password"
                      value={password}
                      onChange={(e) => {
                        setPassword(e.target.value);
                        if (errorMsg) setErrorMsg('');
                      }}
                      placeholder="Nhập mật khẩu"
                      className="w-full bg-white border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 rounded-xl py-2.5 pl-9 pr-10 text-xs text-ink placeholder:text-pink-600/70 shadow-2xs outline-none transition"
                    />
                    <Key className="w-4 h-4 text-pink-600/70 absolute left-3 top-3" />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-3 text-pink-600/70 hover:text-slate-600 focus:outline-none cursor-pointer"
                      aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                {/* Remember Me Checkbox */}
                <div className="flex items-center justify-between pt-0.5">
                  <label className="flex items-center gap-2 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={(e) => setRememberMe(e.target.checked)}
                      className="w-4 h-4 rounded border-pink-200 text-ink focus:ring-pink-500 accent-pink-600 cursor-pointer"
                    />
                    <span className="text-xs text-slate-600 font-medium">Ghi nhớ đăng nhập</span>
                  </label>
                </div>

                {/* Forgot Password Accordion */}
                {showForgotNotice && (
                  <div className="p-3.5 bg-pink-50/60 border border-pink-200 rounded-xl space-y-1.5 text-xs text-ink animate-in fade-in">
                    <div className="flex items-center justify-between font-semibold text-ink">
                      <span>Cấp lại mật khẩu</span>
                      <button
                        type="button"
                        onClick={() => setShowForgotNotice(false)}
                        className="text-pink-600/70 hover:text-slate-600 text-sm leading-none cursor-pointer"
                      >
                        &times;
                      </button>
                    </div>
                    <p className="text-[11px] text-slate-600 leading-relaxed">
                      Để bảo mật dữ liệu thi vấn đáp, vui lòng liên hệ Ban Khảo Thí để được hướng dẫn đặt lại mật khẩu học thuật.
                    </p>
                    <div className="text-[10px] text-slate-600 font-mono bg-white p-2 rounded-lg border border-pink-200">
                      Email: khaothi_hcm@fpt.edu.vn • Hotline: (028) 7300 5588
                    </div>
                  </div>
                )}

                {/* Submit button */}
                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full bg-pink-600 hover:bg-pink-700 active:bg-pink-800 text-white font-semibold text-xs py-3 px-4 rounded-xl shadow-xs active:scale-[0.99] flex items-center justify-center gap-2 transition disabled:opacity-50 cursor-pointer"
                >
                  {isLoading ? (
                    <>
                      <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                      <span>Đang xác thực...</span>
                    </>
                  ) : (
                    <>
                      <span>Đăng nhập với tư cách {activePortal.label}</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </>
                  )}
                </button>
              </form>

              {/* Register link */}
              <div className="pt-2 text-center text-xs text-pink-800/80">
                Chưa có tài khoản khảo thí?{' '}
                <button
                  type="button"
                  onClick={() => onNavigate('register')}
                  className="text-pink-600 hover:text-pink-700 font-semibold cursor-pointer"
                >
                  Đăng ký tài khoản
                </button>
              </div>

            </div>

            {/* Legal text */}
            <p className="text-[11px] text-pink-600/70 leading-relaxed text-center">
              Bằng việc đăng nhập, bạn đồng ý tuân thủ <span className="font-medium text-slate-600">Quy chế Khảo thí FPTU</span> và cam kết bảo mật nội dung đề thi.
            </p>

          </div>

          {/* RIGHT COLUMN: ACADEMIC PORTAL INFORMATION */}
          <div className="lg:col-span-6 flex justify-center lg:justify-end">
            <div className="w-full max-w-md rounded-2xl bg-white border border-pink-200 p-6 sm:p-7 space-y-5 shadow-xs">

              {/* Portal Header */}
              <div className="flex items-center justify-between pb-3 border-b border-pink-100">
                <div className="flex items-center gap-2">
                  <School className="w-4 h-4 text-pink-600" />
                  <span className="font-heading font-bold text-xs text-slate-800 uppercase tracking-wider">
                    Quy Chuẩn Phòng Thi Vấn Đáp
                  </span>
                </div>
                <span className="text-[10px] font-mono font-medium px-2 py-0.5 rounded bg-slate-100 text-slate-600 border border-pink-200">
                  Học Kỳ 2024-2025
                </span>
              </div>

              {/* Guidelines for Exam Takers */}
              <div className="space-y-3">
                <div className="p-3.5 rounded-xl bg-slate-50 border border-pink-200/80 flex items-start gap-3 shadow-2xs">
                  <div className="w-8 h-8 rounded-lg bg-white border border-pink-200 text-ink flex items-center justify-center shrink-0 shadow-2xs mt-0.5">
                    <Headphones className="w-4 h-4 text-pink-600" />
                  </div>
                  <div className="flex-1">
                    <p className="font-semibold text-xs text-slate-800">Chuẩn bị thiết bị thu âm</p>
                    <p className="text-[11px] text-pink-800/80 mt-0.5 leading-relaxed">
                      Sử dụng tai nghe có microphone để đảm bảo chất lượng nhận diện giọng nói đàm thoại với hệ thống.
                    </p>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-50 border border-pink-200/80 flex items-start gap-3 shadow-2xs">
                  <div className="w-8 h-8 rounded-lg bg-white border border-pink-200 text-ink flex items-center justify-center shrink-0 shadow-2xs mt-0.5">
                    <Volume2 className="w-4 h-4 text-pink-600" />
                  </div>
                  <div className="flex-1">
                    <p className="font-semibold text-xs text-slate-800">Không gian yên tĩnh</p>
                    <p className="text-[11px] text-pink-800/80 mt-0.5 leading-relaxed">
                      Thực hiện bài thi tại nơi yên tĩnh, hạn chế tiếng ồn xung quanh để kết quả ghi âm và chấm điểm chính xác.
                    </p>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-50 border border-pink-200/80 flex items-start gap-3 shadow-2xs">
                  <div className="w-8 h-8 rounded-lg bg-white border border-pink-200 text-ink flex items-center justify-center shrink-0 shadow-2xs mt-0.5">
                    <FileCheck2 className="w-4 h-4 text-emerald-600" />
                  </div>
                  <div className="flex-1">
                    <p className="font-semibold text-xs text-slate-800">Biên bản khảo thí điện tử</p>
                    <p className="text-[11px] text-pink-800/80 mt-0.5 leading-relaxed">
                      Mọi lượt hỏi đáp, câu hỏi đào sâu và tiêu chí Rubric được ghi nhận tự động vào hồ sơ điểm học phần.
                    </p>
                  </div>
                </div>
              </div>

              {/* Support Info Box */}
              <div className="p-4 rounded-xl bg-pink-50/60 border border-pink-200 text-xs space-y-1.5">
                <div className="flex items-center gap-1.5 font-semibold text-slate-800 text-[11px]">
                  <HelpCircle className="w-3.5 h-3.5 text-pink-800/80" />
                  <span>Bộ Phận Hỗ Trợ Kỹ Thuật Khảo Thí</span>
                </div>
                <p className="text-[11px] text-pink-800/80 leading-relaxed">
                  Nếu gặp sự cố về tài khoản hoặc kết nối âm thanh phòng thi, vui lòng liên hệ Trạm Khảo Thí:
                </p>
                <div className="text-[11px] font-mono text-ink bg-white p-2 rounded-lg border border-pink-200">
                  Phòng Khảo Thí &amp; Đảm bảo chất lượng FPTU<br />
                  Hotline: (028) 7300 5588 • Email: khaothi_hcm@fpt.edu.vn
                </div>
              </div>

            </div>
          </div>

        </div>
      </main>

      {/* GLOBAL FOOTER */}
      <footer className="bg-white border-t border-pink-200 text-pink-600/70 text-xs py-4 px-6 text-center font-normal">
        © 2026 Hệ thống Khảo thí Vấn đáp Trực tuyến AIVES • Đại học FPT
      </footer>

    </div>
  );
}
