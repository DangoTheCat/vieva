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

export function LoginPage({ onNavigate, showToast }) {
  const { login, isLiveBackendReachable } = useAuth();

  const [selectedRole, setSelectedRole] = useState('stu'); // 'stu', 'lec', 'adm'
  const [isModalOpen, setIsModalOpen] = useState(false);

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
    setIsModalOpen(true);
  };

  // Auto-detect remembered role when typing email
  const handleEmailChange = (val) => {
    setEmail(val);
    if (val.trim()) {
      const emailKey = `aives_user_role_${val.trim().toLowerCase()}`;
      const savedRole = localStorage.getItem(emailKey);
      if (savedRole === 'ROLE_LECTURER') {
        setSelectedRole('lec');
      } else if (savedRole === 'ROLE_ADMIN') {
        setSelectedRole('adm');
      } else if (savedRole === 'ROLE_STUDENT') {
        setSelectedRole('stu');
      }
    }
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
      const roleOverride = selectedRole === 'lec' ? 'ROLE_LECTURER' : selectedRole === 'adm' ? 'ROLE_ADMIN' : 'ROLE_STUDENT';
      // Real BE API Call: POST /api/v1/auth/login
      const result = await login({ email: email.trim(), password }, roleOverride);
      if (showToast) {
        showToast({
          type: 'success',
          title: 'Đăng Nhập Thành Công',
          message: `Chào mừng ${result.user?.fullName || result.user?.email || 'bạn'} kết nối hệ thống AIVES!`
        });
      }
      setIsModalOpen(false);
      // The account's single role (from BE) decides the landing view, not the picked tab
      const effectiveRole = result.effectiveRole || result.user?.selectedRole || roleOverride;
      const targetView = effectiveRole === 'ROLE_LECTURER'
        ? 'lecturer-questions'
        : effectiveRole === 'ROLE_ADMIN'
          ? 'admin-users'
          : 'discover';
      onNavigate(targetView);
    } catch (err) {
      const msg = err.code === '1003' ? 'Email hoặc mật khẩu không chính xác.' : getErrorMessage(err);
      setErrorMsg(msg);
      if (showToast) showToast({ type: 'error', message: msg });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-between dark-tech-grid text-slate-100 antialiased font-sans relative overflow-x-hidden selection:bg-sky-500 selection:text-white">

      {/* TOP HEADER BAR */}
      <header className="w-full max-w-[1400px] mx-auto px-6 py-5 flex items-center justify-between z-10">

        {/* Brand Logo */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-cyan-600 via-sky-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-900/40 border border-cyan-400/30">
              <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </div>
            <span className="font-heading font-black text-2xl tracking-tight text-white">aives.</span>
          </div>
        </div>

        {/* Header Right Status & Actions */}
        <div className="flex items-center gap-3 text-xs">
          <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#0e2040]/90 border border-sky-800/60 text-slate-300">
            <span className={`w-2 h-2 rounded-full ${isLiveBackendReachable ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`}></span>
            <span className="font-medium text-[11px]">Hội đồng Khảo thí Kỹ thuật Phần mềm (FPTU)</span>
          </div>

          <button
            onClick={() => onNavigate('register')}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold transition text-xs shadow-md shadow-sky-600/30 active:scale-95 cursor-pointer"
          >
            <UserPlus className="w-3.5 h-3.5" />
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
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#0e2040]/90 border border-sky-800/60 text-xs text-slate-300 backdrop-blur-md">
              <div className="w-6 h-6 rounded-full bg-sky-500/20 flex items-center justify-center">
                <Mic className="w-3.5 h-3.5 text-sky-400" />
              </div>
              <span className="font-semibold text-slate-200">Đàm thoại Vấn đáp Giọng nói Trực quan</span>
              <span className="text-slate-600">•</span>
              <span className="text-emerald-400 font-mono font-bold text-[11px]">Active Speech-to-Speech</span>
            </div>

            {/* Main Headline */}
            <div className="space-y-3">
              <h1 className="font-heading font-extrabold text-4xl sm:text-5xl text-white tracking-tight leading-[1.12]">
                Nắm bắt năng lực vấn đáp <br />
                <span className="bg-gradient-to-r from-sky-400 via-cyan-300 to-blue-500 bg-clip-text text-transparent">
                  học thuật chuẩn xác tức thì.
                </span>
              </h1>
              <p className="text-slate-400 text-sm sm:text-base leading-relaxed font-normal">
                Hệ thống khảo thí đàm thoại AI thông minh thế hệ mới. Trực tiếp vấn đáp kiến trúc phần mềm, đối sánh Rubric đa chiều, phân tích phản xạ học thuật minh bạch và khách quan.
              </p>
            </div>

            {/* Role Filter Tabs */}
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-[11px] font-bold text-slate-400 tracking-wider uppercase">
                <User className="w-3.5 h-3.5 text-sky-400" />
                <span>Xác định vai trò </span>
              </div>
              <div className="flex items-center gap-2 p-1.5 bg-[#0c1938]/90 border border-sky-800/60 rounded-2xl text-xs">
                <button
                  type="button"
                  onClick={() => handleRoleChange('stu')}
                  className={`flex-1 py-2 px-3 rounded-xl font-bold flex items-center justify-center gap-2 transition ${selectedRole === 'stu'
                    ? 'bg-sky-600 text-white shadow-md shadow-sky-600/30'
                    : 'text-slate-400 hover:text-white hover:bg-[#122850]/70'
                    }`}
                >
                  <GraduationCap className="w-4 h-4" />
                  <span>Sinh Viên</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleRoleChange('lec')}
                  className={`flex-1 py-2 px-3 rounded-xl font-bold flex items-center justify-center gap-2 transition ${selectedRole === 'lec'
                    ? 'bg-sky-600 text-white shadow-md shadow-sky-600/30'
                    : 'text-slate-400 hover:text-white hover:bg-[#122850]/70'
                    }`}
                >
                  <School className="w-4 h-4" />
                  <span>Giảng Viên</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleRoleChange('adm')}
                  className={`flex-1 py-2 px-3 rounded-xl font-bold flex items-center justify-center gap-2 transition ${selectedRole === 'adm'
                    ? 'bg-sky-600 text-white shadow-md shadow-sky-600/30'
                    : 'text-slate-400 hover:text-white hover:bg-[#122850]/70'
                    }`}
                >
                  <Shield className="w-4 h-4" />
                  <span>Ban Khảo Thí</span>
                </button>
              </div>
            </div>

            {/* Action Buttons Stack */}
            <div className="space-y-3 pt-1">

              {/* Email & Password Gradient Main Button */}
              <button
                type="button"
                onClick={handleOpenLoginModal}
                className="w-full bg-gradient-to-r from-sky-500 via-blue-600 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-extrabold text-xs py-3.5 px-5 rounded-2xl shadow-lg shadow-blue-600/30 flex items-center justify-center gap-2.5 transition active:scale-98 focus:outline-none focus:ring-2 focus:ring-sky-400 cursor-pointer"
              >
                <Mail className="w-4 h-4 text-white shrink-0" />
                <span>Đăng nhập bằng Email &amp; Mật Khẩu</span>
              </button>

            </div>

            {/* Legal terms note */}
            <p className="text-[11px] text-slate-500 leading-relaxed pt-1">
              Bằng việc truy cập vào AIVES, thí sinh và cán bộ xác nhận đồng ý với{' '}
              <strong className="text-sky-400 font-extrabold">Quy Chế Khảo Thí </strong>{' '}
              và{' '}
              <strong className="text-sky-400 font-extrabold"> các quy định của FPT University</strong>.
            </p>

          </div>

          {/* RIGHT COLUMN: REAL BE SYSTEM MONITORING DASHBOARD CARD */}
          <div className="lg:col-span-6 flex justify-center lg:justify-end">
            <div className="w-full max-w-md rounded-3xl bg-[#0b1428]/90 border border-sky-800/60 p-6 sm:p-7 space-y-5 shadow-2xl shadow-sky-950/50 backdrop-blur-xl relative">

              {/* Top System Header - Dynamic BE Status Connection */}
              <div className="flex items-center justify-between pb-3 border-b border-sky-900/50">
                <div className="flex items-center gap-2.5">
                  <span className={`w-2.5 h-2.5 rounded-full ${isLiveBackendReachable ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`}></span>
                  <span className="font-heading font-extrabold text-xs text-white uppercase tracking-wider block">
                    HỆ THỐNG KHẢO THÍ AIVES
                  </span>
                </div>
              </div>

              {/* Feature Box 1: Exam Room & AI Examiner (ACOUSTIC SYNTHESIZER KEPT EXACTLY INTTACT) */}
              <div className="p-4 rounded-2xl bg-[#0e2246]/90 border border-cyan-500/50 space-y-3 shadow-md">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-cyan-500/20 border border-cyan-400/40 text-cyan-400 flex items-center justify-center">
                      <Bot className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="font-heading font-extrabold text-xs text-white">
                        Phòng Thi Vấn Đáp Trực Tuyến AI
                      </h3>
                      <p className="text-[11px] text-slate-400">
                        Giám thị Ảo: <strong className="text-cyan-300">Dr. Sophia (AI Examiner)</strong>
                      </p>
                    </div>
                  </div>
                </div>

                {/* Acoustic Synthesizer Live Waveform */}
                <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-[11px]">
                  <span className="text-slate-400 font-mono flex items-center gap-1.5">
                    <Mic className="w-3.5 h-3.5 text-cyan-400" />
                    Bộ tổng hợp âm thanh:
                  </span>
                  <div className="flex items-center gap-2">
                    <VoiceWaveform isSpeaking={true} volume={0.65} barCount={5} height={16} barWidth="w-1" barGap="gap-0.5" colorClass="bg-cyan-400" />
                    <span className="text-[10px] font-mono font-bold text-cyan-400">Ready</span>
                  </div>
                </div>
              </div>

              {/* Feature Box 2: JWT Bearer & BE Rate Limit Security (REAL BE CAPABILITY) */}
              <div className="p-3.5 rounded-2xl bg-[#0d1d3b]/80 border border-sky-800/60 flex items-center gap-3">
                <div className="w-8 h-8 rounded-xl bg-emerald-500/20 border border-emerald-400/30 text-emerald-400 flex items-center justify-center shrink-0">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <div className="flex-1">
                  <span className="font-bold text-xs text-white">Bảo Mật Phiên Đăng Nhập  </span>
                </div>
              </div>

              {/* Feature Box 3: Course-Scoped RBAC & Clean Architecture (REAL BE CAPABILITY) */}
              <div className="p-3.5 rounded-2xl bg-[#0d1d3b]/80 border border-sky-800/60 flex items-center gap-3">
                <div className="w-8 h-8 rounded-xl bg-sky-500/20 border border-sky-400/30 text-sky-400 flex items-center justify-center shrink-0">
                  <Lock className="w-4 h-4" />
                </div>
                <div className="flex-1">
                  <span className="font-bold text-xs text-white">Phân Quyền Linh Hoạt </span>
                </div>
              </div>

              {/* Feature Box 4: PostgreSQL PGVector & V11 Seed Rules (REAL BE CAPABILITY) */}
              <div className="p-3.5 rounded-2xl bg-[#0d1d3b]/80 border border-sky-800/60 flex items-center gap-3">
                <div className="w-8 h-8 rounded-xl bg-purple-500/20 border border-purple-400/30 text-purple-400 flex items-center justify-center shrink-0">
                  <Database className="w-4 h-4" />
                </div>
                <div className="flex-1">
                  <span className="font-bold text-xs text-white">Cơ Sở Dữ Liệu AI  </span>
                </div>
              </div>

            </div>
          </div>

        </div>
      </main>

      {/* CREDENTIALS LOGIN MODAL */}
      {isModalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-md animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-[#0b1428]/95 backdrop-blur-xl rounded-3xl max-w-md w-full p-6 shadow-2xl border border-sky-800/60 space-y-5 animate-modal-entry text-slate-200">

            <div className="flex items-center justify-between pb-3 border-b border-sky-900/50">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-sky-600/30 text-sky-400 flex items-center justify-center border border-sky-500/30">
                  <Key className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-heading font-extrabold text-sm text-white">Xác Thực Mã Định Danh AIVES</h3>
                  <p className="text-[11px] text-slate-400">Đăng nhập tài khoản hệ thống</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-white text-xl font-bold p-1 leading-none transition"
                aria-label="Đóng"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSubmitLogin} className="space-y-4 text-xs" autoComplete="off">
              {errorMsg && (
                <div className="p-3 bg-rose-950/80 border border-rose-800 rounded-xl text-rose-300 text-[11px] font-medium flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4 shrink-0 text-rose-400" />
                  <span>{errorMsg}</span>
                </div>
              )}

              <div>
                <label className="font-bold text-slate-300 block mb-1">Email Tài Khoản Trường / Mã Số (*)</label>
                <div className="relative">
                  <input
                    ref={emailInputRef}
                    type="email"
                    required
                    autoComplete="off"
                    value={email}
                    onChange={(e) => handleEmailChange(e.target.value)}
                    placeholder="student@fpt.edu.vn hoặc MSSV"
                    className="w-full bg-[#070d1a] border border-sky-900/50 rounded-xl p-3 pl-9 text-xs text-white focus:ring-2 focus:ring-sky-500 focus:border-sky-500 focus:outline-none transition font-sans"
                  />
                  <Mail className="w-4 h-4 text-slate-500 absolute left-3 top-3.5" />
                </div>
              </div>

              <div>
                <label className="font-bold text-slate-300 block mb-1">Mật Khẩu (*)</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    autoComplete="new-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Nhập mật khẩu (tối thiểu 6 ký tự)"
                    className="w-full bg-[#070d1a] border border-sky-900/50 rounded-xl p-3 pl-9 pr-10 text-xs text-white focus:ring-2 focus:ring-sky-500 focus:border-sky-500 focus:outline-none transition font-sans"
                  />
                  <Key className="w-4 h-4 text-slate-500 absolute left-3 top-3.5" />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-3.5 text-slate-500 hover:text-slate-300 focus:outline-none"
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-end text-[11px] text-slate-400 pt-1">
                <button
                  type="button"
                  onClick={() => alert('Liên hệ Trạm Khảo Thí FPTU để reset mật khẩu.')}
                  className="text-sky-400 hover:underline font-medium"
                >
                  Quên mật khẩu?
                </button>
              </div>

              <div className="pt-3 border-t border-sky-900/40 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2.5 rounded-xl border border-sky-900/50 text-xs font-semibold text-slate-400 hover:bg-[#0e2040] hover:text-white transition"
                >
                  Đóng
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-blue-600 hover:from-sky-400 hover:to-blue-500 text-white text-xs font-bold shadow-lg shadow-sky-600/30 flex items-center gap-2 disabled:opacity-50 transition active:scale-95"
                >
                  {isLoading && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                  <span>Đăng nhập</span>
                </button>
              </div>
            </form>

          </div>
        </div>
      )}

      {/* GLOBAL FOOTER */}
      <footer className="bg-slate-950/80 border-t border-slate-800/80 text-slate-400 text-xs py-4 px-6 text-center backdrop-blur-xl relative z-10">
        © 2026 AIVES Assessment Network • FPT University SWD392
      </footer>

    </div>
  );
}
