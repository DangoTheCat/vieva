import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import { ShieldCheck, Mail, User, Key, Eye, EyeOff, X, ArrowRight, ShieldAlert, CheckCircle2, Sparkles } from 'lucide-react';

export function LoginPage({ onNavigate, showToast }) {
  const { login } = useAuth();

  const [selectedRole, setSelectedRole] = useState('stu'); // stu, lec, adm
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Form credentials - empty by default (no hardcoded credentials)
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const emailInputRef = useRef(null);

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

  const handleRoleChange = (role) => {
    setSelectedRole(role);
  };

  const handleOpenLoginModal = () => {
    setErrorMsg('');
    setIsModalOpen(true);
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
      // Real BE API Call: POST /api/v1/auth/login
      const result = await login({ email: email.trim(), password });
      if (showToast) {
        showToast({
          type: 'success',
          title: 'Đăng Nhập Thành Công',
          message: `Chào mừng ${result.user?.fullName || result.user?.email || 'bạn'} đã kết nối Backend thành công!`
        });
      }
      setIsModalOpen(false);
      onNavigate('admin-users');
    } catch (err) {
      // BE answers wrong credentials with 401 / 1003, which the generic map reads as "session expired"
      const msg = err.code === '1003' ? 'Email hoặc mật khẩu không chính xác.' : getErrorMessage(err);
      setErrorMsg(msg);
      if (showToast) showToast({ type: 'error', message: msg });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="h-full min-h-screen flex flex-col justify-between bg-[#F8F9FA] text-slate-800 antialiased selection:bg-blue-600 selection:text-white font-sans">

      {/* ================= TOP MINIMALIST NAVIGATION BAR ================= */}
      <header className="w-full max-w-7xl mx-auto px-6 py-5 flex items-center justify-between">
        {/* Brand Logo */}
        <div
          onClick={() => onNavigate('login')}
          className="flex items-center gap-2 group cursor-pointer select-none"
          role="button"
          tabIndex={0}
        >
          <div className="w-8 h-8 rounded-full bg-slate-900 text-white flex items-center justify-center font-bold text-sm shadow-xs group-hover:scale-105 transition">
            <svg className="w-4 h-4 text-sky-400" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2L3 9L12 16L21 9L12 2Z" fillOpacity="0.9" />
              <path d="M7 13L12 17L17 13" stroke="white" strokeWidth="2" strokeLinecap="round" />
              <path d="M5 16L12 21L19 16" stroke="white" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </div>
          <div className="flex items-baseline gap-1">
            <span className="font-heading font-extrabold text-xl tracking-tight text-slate-900">aives</span>
            <span className="w-1.5 h-1.5 rounded-full bg-blue-600"></span>
          </div>
        </div>

        {/* Top Right Links */}
        <div className="flex items-center gap-3">
          <a
            href="/AIVES_Master_Suite.html"
            target="_blank"
            rel="noreferrer"
            className="pill-btn inline-flex items-center gap-1.5 px-4 py-2 rounded-full border border-slate-200 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 shadow-2xs"
          >
            <span>Master Suite</span>
            <span className="text-blue-600 font-bold">↗</span>
          </a>
          <button
            onClick={() => onNavigate('register')}
            className="pill-btn inline-flex items-center px-4 py-2 rounded-full bg-slate-900 text-white text-xs font-semibold hover:bg-slate-800 shadow-2xs focus:outline-none focus:ring-2 focus:ring-slate-700 active:scale-95"
          >
            Đăng Ký Mới
          </button>
        </div>
      </header>

      {/* ================= MAIN SPLIT HERO & AUTH GATEWAY ================= */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-6 py-6 md:py-12 flex items-center">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center w-full">

          {/* LEFT COLUMN: BRAND PROMISE & STACKED ROUNDED-FULL PILL ACTIONS */}
          <div className="lg:col-span-6 space-y-6 max-w-lg">

            {/* Iridescent Radiant Orb */}
            <div
              className="w-14 h-14 rounded-full iridescent-orb flex items-center justify-center shadow-lg"
              style={{
                background: 'radial-gradient(circle at 35% 35%, #93C5FD 0%, #C084FC 45%, #F472B6 70%, #FBBF24 100%)',
                boxShadow: '0 10px 30px -5px rgba(192, 132, 252, 0.45)'
              }}
            >
              <div className="w-7 h-7 rounded-full bg-white/40 backdrop-blur-xs flex items-center justify-center">
                <svg className="w-4 h-4 text-white fill-current" viewBox="0 0 24 24">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z" />
                </svg>
              </div>
            </div>

            {/* Headline & Academic Context */}
            <div className="space-y-3">
              <h1 className="font-heading font-extrabold text-3xl sm:text-4xl text-slate-900 tracking-tight leading-[1.15]">
                Nắm bắt năng lực vấn đáp học thuật tức thì.
              </h1>
              <p className="text-slate-600 text-sm sm:text-base leading-relaxed">
                Hệ thống khảo thí đàm thoại AI thông minh. Đồng hành cùng sinh viên trong các kỳ thi vấn đáp kiến trúc phần mềm, đối chiếu Rubric ma trận minh bạch và khách quan.
              </p>
            </div>

            {/* Role Selector Pill Filter */}
            <div className="flex items-center gap-2 p-1 bg-slate-200/60 rounded-full max-w-xs text-xs font-semibold">
              <button
                type="button"
                onClick={() => handleRoleChange('stu')}
                className={`flex-1 py-1.5 px-3 rounded-full transition ${selectedRole === 'stu'
                    ? 'bg-white text-slate-900 shadow-2xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                  }`}
              >
                Sinh Viên
              </button>
              <button
                type="button"
                onClick={() => handleRoleChange('lec')}
                className={`flex-1 py-1.5 px-3 rounded-full transition ${selectedRole === 'lec'
                    ? 'bg-white text-slate-900 shadow-2xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                  }`}
              >
                Giảng Viên
              </button>
              <button
                type="button"
                onClick={() => handleRoleChange('adm')}
                className={`flex-1 py-1.5 px-3 rounded-full transition ${selectedRole === 'adm'
                    ? 'bg-white text-slate-900 shadow-2xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                  }`}
              >
                Khảo Thí
              </button>
            </div>

            {/* Stacked Rounded-Full Pill Buttons */}
            <div className="space-y-3 pt-1">

              {/* 1. Google FPT Edu SSO Pill */}
              <button
                type="button"
                onClick={handleOpenLoginModal}
                className="pill-btn w-full bg-white border border-slate-300/90 hover:border-slate-400 text-slate-700 font-semibold text-sm py-3 px-5 rounded-full shadow-2xs flex items-center justify-center gap-3 transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" />
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" />
                </svg>
                <span>Tiếp tục với Google FPT Edu (@fpt.edu.vn)</span>
              </button>

              {/* 2. Microsoft SSO Pill */}
              <button
                type="button"
                onClick={handleOpenLoginModal}
                className="pill-btn w-full bg-white border border-slate-300/90 hover:border-slate-400 text-slate-700 font-semibold text-sm py-3 px-5 rounded-full shadow-2xs flex items-center justify-center gap-3 transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <svg className="w-4 h-4 shrink-0" viewBox="0 0 23 23">
                  <path fill="#f35325" d="M1 1h10v10H1z" />
                  <path fill="#81bc06" d="M12 1h10v10H1z" />
                  <path fill="#05a6f0" d="M1 12h10v10H1z" />
                  <path fill="#ffba08" d="M12 12h10v10H1z" />
                </svg>
                <span>Tiếp tục với Microsoft 365 Campus</span>
              </button>

              {/* 3. Student ID / MSSV Pill */}
              <button
                type="button"
                onClick={handleOpenLoginModal}
                className="pill-btn w-full bg-white border border-slate-300/90 hover:border-slate-400 text-slate-700 font-semibold text-sm py-3 px-5 rounded-full shadow-2xs flex items-center justify-center gap-3 transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <User className="w-4 h-4 text-slate-500 shrink-0" />
                <span>Tiếp tục với Mã Số Sinh Viên (MSSV)</span>
              </button>

              {/* 4. Email & Password Direct Pill */}
              <button
                type="button"
                onClick={handleOpenLoginModal}
                className="pill-btn w-full bg-[#0066FF] hover:bg-[#0052CC] text-white font-semibold text-sm py-3 px-5 rounded-full shadow-xs flex items-center justify-center gap-3 transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <Mail className="w-4 h-4 text-white shrink-0" />
                <span>Đăng nhập bằng Email &amp; Mật Khẩu</span>
              </button>

            </div>

            {/* Legal note */}
            <p className="text-xs text-slate-500 pt-1 leading-relaxed">
              Bằng việc đăng nhập, bạn đồng ý với <a href="#" onClick={(e) => { e.preventDefault(); alert('Chính sách khảo thí AIVES FPT University'); }} className="underline hover:text-slate-800">Quy Chế Khảo Thí Trực Tuyến</a> và <a href="#" onClick={(e) => { e.preventDefault(); alert('Chính sách bảo mật dữ liệu AIVES'); }} className="underline hover:text-slate-800">Chính Sách Bảo Mật</a> của FPT University.
            </p>

          </div>

          {/* RIGHT COLUMN: ELEVATED WHITE CARD WITH REAL-TIME SYSTEM ARCHITECTURE */}
          <div className="lg:col-span-6 flex justify-center lg:justify-end">
            <div className="schoolai-card w-full max-w-md rounded-3xl p-6 sm:p-7 space-y-5 shadow-xl">

              {/* Card Header */}
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <div className="flex items-center gap-2.5">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                  <span className="text-xs font-bold text-slate-900 uppercase tracking-wider">Hệ Thống Khảo Thí AIVES</span>
                </div>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                  Backend 8080 Live
                </span>
              </div>

              {/* Title Context */}
              <div>
                <h2 className="font-heading font-extrabold text-lg text-slate-900">
                  Phòng Thi Vấn Đáp Trực Tuyến AI
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Giám thị Ảo: <strong>Dr. Sophia (AI Examiner)</strong> • PostgreSQL PGVector
                </p>
              </div>

              {/* System Security Features */}
              <div className="space-y-3 text-xs">
                <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/70 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-slate-900 flex items-center gap-2">
                      <ShieldCheck className="w-4 h-4 text-emerald-600" />
                      Xác Thực Bảo Mật JWT Bearer
                    </span>
                    <span className="text-[10px] font-bold bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full">ACTIVE</span>
                  </div>
                  <p className="text-[11px] text-slate-500">Mã hóa mật khẩu BCrypt, cấp Token phiên thi an toàn từ Spring Boot.</p>
                </div>

                <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/70 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-slate-900 flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-sky-600" />
                      Phân Quyền Vai Trò RBAC
                    </span>
                    <span className="text-[10px] font-bold bg-sky-100 text-sky-800 px-2 py-0.5 rounded-full">ENFORCED</span>
                  </div>
                  <p className="text-[11px] text-slate-500">Bảo mật phạm vi môn học theo chuẩn Onion Architecture (Course-scoped RLS).</p>
                </div>
              </div>

              {/* Footer */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                <span className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                  Spring Boot REST API
                </span>
                <span className="font-semibold text-slate-700">Flyway DB • SHA-256</span>
              </div>

            </div>
          </div>

        </div>
      </main>

      {/* ================= MODAL: CREDENTIALS LOGIN ================= */}
      {isModalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry">

            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <h3 className="font-heading font-bold text-base text-slate-900">Xác Thực Mã Định Danh</h3>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-700 text-xl font-bold leading-none p-1"
                aria-label="Đóng"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSubmitLogin} className="space-y-3.5 text-xs text-slate-700">
              {errorMsg && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-[11px] font-medium flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4 shrink-0 text-rose-500" />
                  <span>{errorMsg}</span>
                </div>
              )}

              <div>
                <label className="font-bold text-slate-800 block mb-1">Mã Số Sinh Viên / Email Trường (*)</label>
                <input
                  ref={emailInputRef}
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Nhập email: name@fpt.edu.vn..."
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:bg-white focus:outline-none transition"
                />
              </div>

              <div>
                <label className="font-bold text-slate-800 block mb-1">Mật Khẩu (*)</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Nhập mật khẩu..."
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 pr-8 text-xs focus:ring-2 focus:ring-blue-500 focus:bg-white focus:outline-none transition"
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

              <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1">
                <label className="flex items-center gap-1.5 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="rounded text-blue-600 focus:ring-blue-500"
                  />
                  <span>Ghi nhớ phiên thi</span>
                </label>
                <button
                  type="button"
                  onClick={() => alert('Vui lòng liên hệ Trạm Khảo Thí để được cấp lại mật khẩu.')}
                  className="text-blue-600 font-semibold hover:underline"
                >
                  Quên mật khẩu?
                </button>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-full border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50"
                >
                  Đóng
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="pill-btn px-5 py-2 rounded-full bg-[#0066FF] hover:bg-[#0052CC] text-white text-xs font-bold shadow-xs flex items-center gap-1.5 disabled:opacity-50"
                >
                  {isLoading && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                  <span>Đăng Nhập BE ↗</span>
                </button>
              </div>
            </form>

          </div>
        </div>
      )}

      {/* ================= FOOTER ================= */}
      <footer className="w-full max-w-7xl mx-auto px-6 py-4 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-2 border-t border-slate-200/60">
        <span>© 2026 AIVES — Artificial Intelligence Voice Evaluation System. Môn học SWD392 FPT University.</span>
        <div className="flex items-center gap-4">
          <a
            href="/AIVES_Master_Suite.html"
            target="_blank"
            rel="noreferrer"
            className="hover:text-slate-800 font-semibold text-blue-600"
          >
            Master Platform
          </a>
        </div>
      </footer>

    </div>
  );
}
