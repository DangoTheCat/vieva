import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import {
  ShieldCheck,
  Mail,
  User,
  Key,
  Eye,
  EyeOff,
  ShieldAlert,
  CheckCircle2,
  Lock,
  Database,
  Bot,
  ExternalLink,
  Mic,
  GraduationCap,
  School,
  Shield,
  UserPlus,
  Activity,
  Cpu,
  Layers
} from 'lucide-react';
import { VoiceWaveform } from '../components/common/VoiceWaveform';

const PORTAL_ROLES = {
  stu: { code: 'ROLE_STUDENT', label: 'Sinh Viên', view: 'discover' },
  lec: { code: 'ROLE_LECTURER', label: 'Giảng Viên', view: 'lecturer-dashboard' },
  adm: { code: 'ROLE_ADMIN', label: 'Ban Khảo Thí (Admin)', view: 'admin-users' }
};

const ROLE_TO_PORTAL = {
  ROLE_STUDENT: 'stu',
  ROLE_LECTURER: 'lec',
  ROLE_ADMIN: 'adm'
};

export function LoginPage({ onNavigate, showToast }) {
  const { login, logout, isLiveBackendReachable } = useAuth();

  const [selectedRole, setSelectedRole] = useState('stu'); // 'stu', 'lec', 'adm'
  const [roleMismatch, setRoleMismatch] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showForgotNotice, setShowForgotNotice] = useState(false);

  // Form credentials
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const emailInputRef = useRef(null);

  // Select role tab
  const handleRoleChange = (role) => {
    setSelectedRole(role);
    setRoleMismatch(null);
    setErrorMsg('');
  };

  // Focus email input when modal opens & handle Escape key
  useEffect(() => {
    if (isModalOpen) {
      const timer = setTimeout(() => emailInputRef.current?.focus(), 50);
      const handleKeyDown = (e) => {
        if (e.key === 'Escape') setIsModalOpen(false);
      };
      window.addEventListener('keydown', handleKeyDown);
      return () => {
        clearTimeout(timer);
        window.removeEventListener('keydown', handleKeyDown);
      };
    }
  }, [isModalOpen]);

  const handleOpenLoginModal = () => {
    setErrorMsg('');
    setEmail('');
    setPassword('');
    setShowForgotNotice(false);
    setIsModalOpen(true);
  };

  // Handle email change without auto-switching the selected role
  const handleEmailChange = (val) => {
    setEmail(val);
    if (errorMsg) setErrorMsg('');
    if (roleMismatch) setRoleMismatch(null);
  };

  const handleSubmitLogin = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setRoleMismatch(null);

    if (!email.trim() || !password) {
      setErrorMsg('Vui lòng nhập đầy đủ Email và Mật khẩu.');
      return;
    }

    setIsLoading(true);
    try {
      const expectedRole = PORTAL_ROLES[selectedRole]?.code || 'ROLE_STUDENT';
      // Real BE API Call: POST /api/v1/auth/login
      const result = await login({ email: email.trim(), password }, expectedRole);
      
      const actualRole = result.effectiveRole || result.user?.selectedRole || (result.user?.role?.startsWith('ROLE_') ? result.user.role : result.user?.role ? `ROLE_${result.user.role}` : expectedRole);

      // Strict Role Portal Enforcement
      if (actualRole && actualRole !== expectedRole) {
        // Sign out immediately so incorrect role session does not persist
        await logout();

        const errMsg = 'Vai trò đã chọn không chính xác đối với tài khoản này.';
        setErrorMsg(errMsg);
        if (showToast) {
          showToast({
            type: 'error',
            title: 'Lỗi Đăng Nhập',
            message: errMsg
          });
        }
        return;
      }

      if (showToast) {
        showToast({
          type: 'success',
          title: 'Đăng Nhập Thành Công',
          message: `Chào mừng ${result.user?.fullName || result.user?.email || 'bạn'} kết nối hệ thống AIVES!`
        });
      }
      setIsModalOpen(false);
      const targetView = PORTAL_ROLES[selectedRole]?.view || 'discover';
      onNavigate(targetView);
    } catch (err) {
      if (err.code === 'ROLE_MISMATCH') {
        const errMsg = 'Vai trò đã chọn không chính xác đối với tài khoản này.';
        setErrorMsg(errMsg);
        if (showToast) {
          showToast({
            type: 'error',
            title: 'Lỗi Đăng Nhập',
            message: errMsg
          });
        }
        return;
      }
      const msg = err.code === '1003' ? 'Email hoặc mật khẩu không chính xác.' : getErrorMessage(err);
      setErrorMsg(msg);
      if (showToast) showToast({ type: 'error', title: 'Lỗi Đăng Nhập', message: msg });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-between bg-paper text-ink antialiased font-sans relative overflow-x-hidden selection:bg-pink-200 selection:text-pink-900">

      {/* TOP HEADER BAR */}
      <header className="w-full max-w-[1400px] mx-auto px-6 py-5 flex items-center justify-between z-10">

        {/* Brand Logo */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-pink-50 text-pink-600 flex items-center justify-center shadow-[3px_3px_0px_#DEB9B5] border border-pink-200">
              <svg className="w-5 h-5 text-pink-700" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </div>
            <span className="font-heading font-black text-2xl tracking-tight text-ink">aives.</span>
          </div>
        </div>

        {/* Header Right Status & Actions */}
        <div className="flex items-center gap-3 text-xs">
          <div className="hidden sm:flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-green-50 border border-green-200 text-green-900 shadow-[3px_3px_0px_#DEB9B5]">
            <span className={`w-2 h-2 rounded-full ${isLiveBackendReachable ? 'bg-green-600 animate-pulse' : 'bg-pink-700'}`}></span>
            <span className="font-medium text-[11px]">Hội đồng Khảo thí Kỹ thuật Phần mềm (FPTU)</span>
          </div>

          <button
            onClick={() => onNavigate('register')}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-paper border border-green-200 text-green-800 font-bold transition text-xs shadow-[3px_3px_0px_#DEB9B5] hover:bg-green-50 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none cursor-pointer"
          >
            <UserPlus className="w-3.5 h-3.5 text-green-800" />
            <span>Đăng Ký </span>
          </button>
        </div>
      </header>

      {/* MAIN TWO-COLUMN CONTAINER */}
      <main className="flex-1 max-w-[1400px] w-full mx-auto px-6 py-8 md:py-12 flex items-center z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center w-full">

          {/* LEFT COLUMN: HERO & AUTH BUTTONS */}
          <div className="lg:col-span-6 space-y-7 max-w-xl">

            {/* Top Micro-badge */}
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-green-50 border border-green-200 text-xs text-green-900 shadow-[3px_3px_0px_#DEB9B5]">
              <div className="w-6 h-6 rounded-full bg-pink-100 flex items-center justify-center">
                <Mic className="w-3.5 h-3.5 text-pink-700" />
              </div>
              <span className="font-semibold text-green-900">Đàm thoại Vấn đáp Giọng nói Trực quan</span>
              <span className="text-green-600">•</span>
              <span className="text-green-700 font-mono font-bold text-[11px]">Active Speech-to-Speech</span>
            </div>

            {/* Main Headline */}
            <div className="space-y-3">
              <h1 className="font-heading font-extrabold text-4xl sm:text-5xl text-ink tracking-tight leading-[1.12]">
                Nắm bắt năng lực vấn đáp <br />
                <span className="text-pink-600">
                  học thuật chuẩn xác tức thì.
                </span>
              </h1>
              <p className="text-on-surface-variant text-sm sm:text-base leading-relaxed font-normal">
                Hệ thống khảo thí đàm thoại AI thông minh thế hệ mới. Trực tiếp vấn đáp kiến trúc phần mềm, đối sánh Rubric đa chiều, phân tích phản xạ học thuật minh bạch và khách quan.
              </p>
            </div>

            {/* Action Buttons Stack */}
            <div className="space-y-3 pt-2">

              {/* Email & Password Main Button */}
              <button
                type="button"
                onClick={handleOpenLoginModal}
                className="w-full bg-pink-500 hover:bg-pink-400 active:bg-pink-600 text-ink font-extrabold text-xs py-3.5 px-5 rounded-2xl shadow-[3px_3px_0px_#DEB9B5] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none flex items-center justify-center gap-2.5 transition active:scale-98 focus:outline-none focus:ring-2 focus:ring-green-600 cursor-pointer"
              >
                <Mail className="w-4 h-4 text-pink-900 shrink-0" />
                <span>Đăng nhập bằng Email &amp; Mật Khẩu</span>
              </button>

            </div>

            {/* Legal terms note */}
            <p className="text-[11px] text-on-surface-variant leading-relaxed pt-1">
              Bằng việc truy cập vào AIVES, thí sinh và cán bộ xác nhận đồng ý với{' '}
              <strong className="text-pink-700 font-extrabold">Quy Chế Khảo Thí </strong>{' '}
              và{' '}
              <strong className="text-pink-700 font-extrabold"> các quy định của FPT University</strong>.
            </p>

          </div>

          {/* RIGHT COLUMN: REAL BE SYSTEM MONITORING DASHBOARD CARD */}
          <div className="lg:col-span-6 flex justify-center lg:justify-end">
            <div className="w-full max-w-md rounded-3xl bg-paper border border-green-200 p-6 sm:p-7 space-y-5 shadow-[6px_6px_0px_#D5A6A2] relative overflow-hidden text-ink">

              {/* Top System Header - Dynamic BE Status Connection */}
              <div className="flex items-center justify-between pb-3 border-b border-green-200">
                <div className="flex items-center gap-2.5">
                  <span className={`w-2.5 h-2.5 rounded-full ${isLiveBackendReachable ? 'bg-green-600 animate-pulse' : 'bg-pink-700'}`}></span>
                  <span className="font-heading font-extrabold text-xs text-pink-800 uppercase tracking-wider block">
                    HỆ THỐNG KHẢO THÍ AIVES
                  </span>
                </div>
              </div>

              {/* Feature Box 1: Exam Room & AI Examiner (ACOUSTIC SYNTHESIZER KEPT EXACTLY INTTACT) */}
              <div className="p-4 rounded-2xl bg-surface-container-low border border-green-200 space-y-3 shadow-[3px_3px_0px_#DEB9B5]">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-pink-100 border border-pink-200 text-pink-700 flex items-center justify-center">
                      <Bot className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="font-heading font-extrabold text-xs text-ink">
                        Phòng Thi Vấn Đáp Trực Tuyến AI
                      </h3>
                      <p className="text-[11px] text-green-700">
                        Giám thị Ảo: <strong className="text-pink-800">Dr. Sophia (AI Examiner)</strong>
                      </p>
                    </div>
                  </div>
                </div>

                {/* Acoustic Synthesizer Live Waveform */}
                <div className="pt-2 border-t border-green-200 flex items-center justify-between text-[11px]">
                  <span className="text-green-700 font-mono flex items-center gap-1.5">
                    <Mic className="w-3.5 h-3.5 text-pink-700" />
                    Bộ tổng hợp âm thanh:
                  </span>
                  <div className="flex items-center gap-2">
                    <VoiceWaveform isSpeaking={true} volume={0.65} barCount={5} height={16} barWidth="w-1" barGap="gap-0.5" colorClass="bg-pink-500" />
                    <span className="text-[10px] font-mono font-bold text-pink-800 bg-pink-100 px-2 py-0.5 rounded">Ready</span>
                  </div>
                </div>
              </div>

              {/* Feature Box 2: JWT Bearer & BE Rate Limit Security (REAL BE CAPABILITY) */}
              <div className="p-3.5 rounded-2xl bg-surface-container-low border border-green-200 flex items-center gap-3 shadow-[2px_2px_0px_#DEB9B5]">
                <div className="w-8 h-8 rounded-xl bg-green-100 border border-green-200 text-green-700 flex items-center justify-center shrink-0">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <div className="flex-1">
                  <span className="font-bold text-xs text-ink">Bảo Mật Phiên Đăng Nhập  </span>
                </div>
              </div>

              {/* Feature Box 3: Course-Scoped RBAC & Clean Architecture (REAL BE CAPABILITY) */}
              <div className="p-3.5 rounded-2xl bg-surface-container-low border border-green-200 flex items-center gap-3 shadow-[2px_2px_0px_#DEB9B5]">
                <div className="w-8 h-8 rounded-xl bg-pink-100 border border-pink-200 text-pink-700 flex items-center justify-center shrink-0">
                  <Lock className="w-4 h-4" />
                </div>
                <div className="flex-1">
                  <span className="font-bold text-xs text-ink">Phân Quyền Linh Hoạt </span>
                </div>
              </div>

              {/* Feature Box 4: PostgreSQL PGVector & V11 Seed Rules (REAL BE CAPABILITY) */}
              <div className="p-3.5 rounded-2xl bg-surface-container-low border border-green-200 flex items-center gap-3 shadow-[2px_2px_0px_#DEB9B5]">
                <div className="w-8 h-8 rounded-xl bg-sand/30 border border-sand text-ink flex items-center justify-center shrink-0">
                  <Database className="w-4 h-4" />
                </div>
                <div className="flex-1">
                  <span className="font-bold text-xs text-ink">Cơ Sở Dữ Liệu AI  </span>
                </div>
              </div>

            </div>
          </div>

        </div>
      </main>

      {/* CREDENTIALS LOGIN MODAL */}
      {isModalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/65 backdrop-blur-[2px] animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-paper rounded-3xl max-w-md w-full p-6 shadow-modal border border-green-200 space-y-5 animate-modal-entry text-ink">

            <div className="flex items-center justify-between pb-3 border-b border-green-200">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-pink-100 text-pink-700 flex items-center justify-center border border-pink-200">
                  <Key className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-heading font-extrabold text-sm text-pink-800">Xác Thực Mã Định Danh AIVES</h3>
                  <p className="text-[11px] text-green-700">Đăng nhập tài khoản hệ thống</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-green-800 hover:text-pink-700 hover:bg-pink-100 rounded-lg p-1 text-xl font-bold transition leading-none cursor-pointer"
                aria-label="Đóng"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSubmitLogin} className="space-y-4 text-xs" autoComplete="off">
              {/* Role Selection Tabs inside Modal */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-[11px] font-bold text-green-900">
                  <span className="flex items-center gap-1.5">
                    <User className="w-3.5 h-3.5 text-pink-700" />
                    <span>Vai trò đăng nhập:</span>
                  </span>
                  <span className="text-pink-800 bg-pink-100 px-2 py-0.5 rounded-full font-semibold">{PORTAL_ROLES[selectedRole]?.label}</span>
                </div>
                <div className="flex items-center gap-1.5 p-1 bg-surface-container border border-green-200 rounded-xl text-xs shadow-[2px_2px_0px_#DEB9B5]">
                  <button
                    type="button"
                    onClick={() => handleRoleChange('stu')}
                    className={`flex-1 py-2 px-2 rounded-lg font-bold flex items-center justify-center gap-1.5 transition text-[11px] cursor-pointer ${
                      selectedRole === 'stu'
                        ? 'bg-green-500 text-paper font-semibold shadow-[2px_2px_0px_#3A422E]'
                        : 'text-on-surface-variant hover:text-ink hover:bg-green-100'
                    }`}
                  >
                    <GraduationCap className="w-3.5 h-3.5" />
                    <span>Sinh Viên</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleRoleChange('lec')}
                    className={`flex-1 py-2 px-2 rounded-lg font-bold flex items-center justify-center gap-1.5 transition text-[11px] cursor-pointer ${
                      selectedRole === 'lec'
                        ? 'bg-green-500 text-paper font-semibold shadow-[2px_2px_0px_#3A422E]'
                        : 'text-on-surface-variant hover:text-ink hover:bg-green-100'
                    }`}
                  >
                    <School className="w-3.5 h-3.5" />
                    <span>Giảng Viên</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleRoleChange('adm')}
                    className={`flex-1 py-2 px-2 rounded-lg font-bold flex items-center justify-center gap-1.5 transition text-[11px] cursor-pointer ${
                      selectedRole === 'adm'
                        ? 'bg-green-500 text-paper font-semibold shadow-[2px_2px_0px_#3A422E]'
                        : 'text-on-surface-variant hover:text-ink hover:bg-green-100'
                    }`}
                  >
                    <Shield className="w-3.5 h-3.5" />
                    <span>Ban Khảo Thí</span>
                  </button>
                </div>
              </div>

              {errorMsg && (
                <div className="p-3 bg-pink-100 border border-pink-300 rounded-xl text-pink-900 text-[11px] font-medium flex items-center gap-2 animate-in fade-in">
                  <ShieldAlert className="w-4 h-4 shrink-0 text-pink-700" />
                  <span>{errorMsg}</span>
                </div>
              )}

              <div>
                <label className="font-bold text-ink block mb-1">
                  <span>Email Tài Khoản Trường</span>
                  <span className="text-pink-700 ml-1">(*)</span>
                </label>
                <div className="relative">
                  <input
                    ref={emailInputRef}
                    type="email"
                    required
                    autoComplete="off"
                    value={email}
                    onChange={(e) => handleEmailChange(e.target.value)}
                    placeholder={
                      selectedRole === 'stu'
                        ? 'student@fpt.edu.vn'
                        : selectedRole === 'lec'
                        ? 'vancee@fpt.edu.vn'
                        : 'admin@aives.edu.vn'
                    }
                    className="w-full bg-white border border-green-300 focus:border-green-600 rounded-xl p-3 pl-9 text-xs text-ink placeholder:text-outline focus:ring-2 focus:ring-green-600 shadow-[2px_2px_0px_#DEB9B5] focus:outline-none transition font-sans"
                  />
                  <Mail className="w-4 h-4 text-green-700 absolute left-3 top-3.5" />
                </div>
              </div>

              <div>
                <label className="font-bold text-ink block mb-1">
                  <span>Mật Khẩu</span>
                  <span className="text-pink-700 ml-1">(*)</span>
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    autoComplete="new-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Nhập mật khẩu (tối thiểu 6 ký tự)"
                    className="w-full bg-white border border-green-300 focus:border-green-600 rounded-xl p-3 pl-9 pr-10 text-xs text-ink placeholder:text-outline focus:ring-2 focus:ring-green-600 shadow-[2px_2px_0px_#DEB9B5] focus:outline-none transition font-sans"
                  />
                  <Key className="w-4 h-4 text-green-700 absolute left-3 top-3.5" />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-3.5 text-on-surface-variant hover:text-ink focus:outline-none"
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {showForgotNotice ? (
                <div className="p-3.5 bg-green-50 border border-green-200 rounded-xl space-y-2 text-xs text-green-900 shadow-xs animate-in fade-in">
                  <div className="flex items-center justify-between text-pink-800 font-bold">
                    <div className="flex items-center gap-1.5">
                      <Shield className="w-4 h-4 text-pink-700" />
                      <span>Hỗ Trợ Khôi Phục Mật Khẩu</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setShowForgotNotice(false)}
                      className="text-green-800 hover:text-pink-700 text-sm leading-none"
                    >
                      &times;
                    </button>
                  </div>
                  <p className="text-[11px] leading-relaxed text-green-900">
                    Theo quy chuẩn bảo mật khảo thí AIVES, người dùng quên mật khẩu vui lòng liên hệ trực tiếp <strong>Trạm Khảo Thí FPTU</strong> hoặc Quản trị viên để được cấp lại mật khẩu tạm thời.
                  </p>
                  <div className="text-[10px] text-pink-900 font-mono bg-white border border-green-200 p-2 rounded-lg">
                    📧 Email: <span className="text-ink font-semibold">khaothi_hcm@fpt.edu.vn</span> • Hotline: 028.7300.5588
                  </div>
                </div>
              ) : (
                <div className="flex items-center justify-end text-[11px] pt-1">
                  <button
                    type="button"
                    onClick={() => setShowForgotNotice(true)}
                    className="text-pink-700 hover:text-pink-900 underline font-medium cursor-pointer"
                  >
                    Quên mật khẩu?
                  </button>
                </div>
              )}

              <div className="pt-3 border-t border-green-200 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2.5 rounded-xl border border-green-300 text-xs font-semibold text-green-800 bg-white hover:bg-green-100 transition shadow-xs cursor-pointer"
                >
                  Đóng
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="px-6 py-2.5 rounded-xl bg-pink-500 hover:bg-pink-400 active:bg-pink-600 text-ink text-xs font-bold shadow-[2px_2px_0px_#DEB9B5] active:translate-x-[2px] active:translate-y-[2px] active:shadow-none flex items-center gap-2 disabled:opacity-50 transition cursor-pointer"
                >
                  {isLoading && <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>}
                  <span>Đăng nhập</span>
                </button>
              </div>
            </form>

          </div>
        </div>
      )}

      {/* GLOBAL FOOTER */}
      <footer className="bg-paper border-t border-green-200 text-green-800 text-xs py-4 px-6 text-center relative z-10">
        © 2026 AIVES Assessment Network • FPT University SWD392
      </footer>

    </div>
  );
}
