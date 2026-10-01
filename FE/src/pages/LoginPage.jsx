import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import { Lock, Mail, ArrowRight, ShieldCheck, X, Mic2, Eye, EyeOff } from 'lucide-react';
import { VoiceWaveform } from '../components/common/VoiceWaveform';

export function LoginPage({ onNavigate, showToast }) {
  const { login } = useAuth();

  const [selectedRole, setSelectedRole] = useState('adm'); // stu, lec, adm
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Form credentials
  const [email, setEmail] = useState('admin@aives.edu.vn');
  const [password, setPassword] = useState('password123');
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
    if (role === 'adm') {
      setEmail('admin@aives.edu.vn');
    } else if (role === 'lec') {
      setEmail('vancee@fpt.edu.vn');
    } else {
      setEmail('bnvse160982@fpt.edu.vn');
    }
  };

  const handleSubmitLogin = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!email || !password) {
      setErrorMsg('Vui lòng nhập đầy đủ Email và Mật khẩu.');
      return;
    }

    setIsLoading(true);
    try {
      const result = await login({ email: email.trim(), password });
      showToast({
        type: 'success',
        title: 'Đăng Nhập Thành Công',
        message: `Chào mừng ${result.user?.fullName || 'Người Dùng'} quay trở lại hệ thống AIVES!`
      });
      setIsModalOpen(false);
      onNavigate('admin-users');
    } catch (err) {
      const msg = getErrorMessage(err);
      setErrorMsg(msg);
      showToast({ type: 'error', message: msg });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-between bg-canvasBg text-slate-800 antialiased selection:bg-sky-500 selection:text-white font-sans">
      
      {/* TOP HEADER */}
      <header className="w-full max-w-7xl mx-auto px-6 py-5 flex items-center justify-between">
        <div 
          className="flex items-center gap-2.5 group cursor-pointer select-none" 
          onClick={() => onNavigate('login')}
          role="button"
          tabIndex={0}
        >
          <div className="w-9 h-9 rounded-xl bg-slate-900 border border-slate-700 text-white flex items-center justify-center font-bold text-sm shadow-xs group-hover:scale-105 transition-transform">
            <ShieldCheck className="w-5 h-5 text-sky-400" />
          </div>
          <div className="flex items-baseline gap-1">
            <span className="font-heading font-extrabold text-xl tracking-tight text-slate-900">aives</span>
            <span className="w-1.5 h-1.5 rounded-full bg-sky-600"></span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => onNavigate('admin-users')}
            className="pill-btn inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 shadow-2xs focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
            <span>Admin Center</span>
            <span className="text-sky-600 font-bold">↗</span>
          </button>
          <button
            onClick={() => onNavigate('register')}
            className="pill-btn inline-flex items-center px-4 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold shadow-2xs focus:outline-none focus:ring-2 focus:ring-slate-700"
          >
            Đăng Ký Mới
          </button>
        </div>
      </header>

      {/* MAIN SPLIT HERO */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-6 py-6 md:py-12 flex items-center">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center w-full">
          
          {/* LEFT COLUMN: HERO PROMISE & ACTION BUTTONS */}
          <div className="lg:col-span-6 space-y-6 max-w-lg">
            
            {/* Academic Voice Examiner Crest (Replaces AI-slop iridescent orb) */}
            <div className="inline-flex items-center gap-3 px-3 py-1.5 rounded-2xl bg-white border border-slate-200/90 shadow-xs">
              <div className="w-8 h-8 rounded-xl bg-slate-900 flex items-center justify-center text-sky-400">
                <Mic2 className="w-4 h-4" />
              </div>
              <div>
                <p className="text-[11px] font-bold text-slate-900 leading-tight">AI Examiner Pro</p>
                <p className="text-[10px] text-slate-500 font-mono">Neural Voice Proctoring • 60 FPS</p>
              </div>
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse ml-1"></span>
            </div>

            {/* Headline */}
            <div className="space-y-3">
              <h1 className="font-heading font-extrabold text-3xl sm:text-4xl text-slate-900 tracking-tight leading-[1.15]">
                Đánh giá năng lực vấn đáp học thuật bằng Giám thị Ảo AI.
              </h1>
              <p className="text-slate-600 text-sm sm:text-base leading-relaxed">
                Hệ thống khảo thí đàm thoại AI thông minh thế hệ mới. Hỗ trợ sinh viên trong các kỳ thi vấn đáp kiến trúc phần mềm, hỏi xoáy thích ứng và đối chiếu Rubric ma trận minh bạch.
              </p>
            </div>

            {/* Role Filter Selector */}
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                Chọn vai trò thử nghiệm nhanh:
              </label>
              <div className="flex items-center gap-1.5 p-1 bg-slate-200/70 rounded-xl max-w-xs text-xs font-semibold">
                <button
                  type="button"
                  onClick={() => handleRoleChange('stu')}
                  className={`flex-1 py-1.5 px-3 rounded-lg transition ${
                    selectedRole === 'stu'
                      ? 'bg-white text-slate-900 shadow-2xs font-bold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Sinh Viên
                </button>
                <button
                  type="button"
                  onClick={() => handleRoleChange('lec')}
                  className={`flex-1 py-1.5 px-3 rounded-lg transition ${
                    selectedRole === 'lec'
                      ? 'bg-white text-slate-900 shadow-2xs font-bold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Giảng Viên
                </button>
                <button
                  type="button"
                  onClick={() => handleRoleChange('adm')}
                  className={`flex-1 py-1.5 px-3 rounded-lg transition ${
                    selectedRole === 'adm'
                      ? 'bg-white text-slate-900 shadow-2xs font-bold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Quản Trị
                </button>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="space-y-3 pt-1">
              <button
                type="button"
                onClick={() => setIsModalOpen(true)}
                className="pill-btn w-full bg-white border border-slate-300 hover:border-slate-400 text-slate-700 font-semibold text-sm py-3 px-5 rounded-xl shadow-2xs flex items-center justify-center gap-3 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
                </svg>
                <span>Đăng nhập qua Google FPT (@fpt.edu.vn)</span>
              </button>

              <button
                type="button"
                onClick={() => setIsModalOpen(true)}
                className="pill-btn w-full bg-sky-600 hover:bg-sky-700 text-white font-bold text-sm py-3 px-5 rounded-xl shadow-md shadow-sky-600/20 flex items-center justify-center gap-3 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                <Mail className="w-4 h-4 text-white shrink-0" />
                <span>Đăng nhập với Email &amp; Mật Khẩu</span>
              </button>
            </div>

            <p className="text-xs text-slate-500 pt-1 leading-relaxed">
              Bằng việc đăng nhập, bạn đồng ý tuân thủ Quy Chế Khảo Thí Trực Tuyến và Tiêu Chuẩn Liêm Chính Học Thuật của Đại học FPT.
            </p>

          </div>

          {/* RIGHT COLUMN: REAL-TIME EXAM CARD WITH VOICE WAVEFORM */}
          <div className="lg:col-span-6 flex justify-center lg:justify-end">
            <div className="bg-white border border-slate-200/90 rounded-3xl p-6 sm:p-7 space-y-5 shadow-xl max-w-md w-full">
              
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <div className="flex items-center gap-2.5">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                  <span className="text-xs font-bold text-slate-900 uppercase tracking-wider">Phòng Thi Vấn Đáp Trực Tuyến</span>
                </div>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-sky-50 text-sky-700 border border-sky-200">
                  SWD392 • Fall 2026
                </span>
              </div>

              <div>
                <h2 className="font-heading font-extrabold text-lg text-slate-900">
                  Phòng Thi 01: Kiến Trúc Phần Mềm
                </h2>
                <div className="flex items-center justify-between mt-2 pt-2 border-t border-slate-100">
                  <p className="text-xs text-slate-600">
                    Giám thị Ảo: <strong>Dr. Sophia (AI Examiner)</strong>
                  </p>
                  <VoiceWaveform isSpeaking={true} volume={0.65} barCount={10} height={18} barWidth="w-1" barGap="gap-0.5" />
                </div>
              </div>

              {/* Progress samples */}
              <div className="space-y-3">
                <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/70 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-sky-100 text-sky-700 font-bold flex items-center justify-center text-xs ring-2 ring-white">
                        NM
                      </div>
                      <div>
                        <h4 className="font-bold text-xs text-slate-900">Nguyễn Thị Mai</h4>
                        <p className="text-[11px] font-mono text-slate-500">SE160892 • Đang trả lời C2</p>
                      </div>
                    </div>
                    <span className="font-mono text-xs font-bold text-emerald-600 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full">
                      9.2 / 10
                    </span>
                  </div>
                  <div className="flex items-center gap-2 pt-0.5 text-[11px]">
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-semibold">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> 3 Đạt Rubric
                    </span>
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-800 font-semibold">
                      <span className="w-1.5 h-1.5 rounded-full bg-amber-500"></span> 1 Đang Hỏi Xoáy
                    </span>
                  </div>
                </div>

                <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/70 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-purple-100 text-purple-700 font-bold flex items-center justify-center text-xs ring-2 ring-white">
                        TQ
                      </div>
                      <div>
                        <h4 className="font-bold text-xs text-slate-900">Trần Hoàng Quân</h4>
                        <p className="text-[11px] font-mono text-slate-500">SE160451 • Hỏi xoáy Outbox</p>
                      </div>
                    </div>
                    <span className="font-mono text-xs font-bold text-emerald-600 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full">
                      8.5 / 10
                    </span>
                  </div>
                  <div className="flex items-center gap-2 pt-0.5 text-[11px]">
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-semibold">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> 2 Đạt Rubric
                    </span>
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-sky-100 text-sky-800 font-semibold">
                      <span className="w-1.5 h-1.5 rounded-full bg-sky-500"></span> 3 Thích Ứng
                    </span>
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                <span className="flex items-center gap-1.5 font-medium">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                  Giao thức Real-time STOMP
                </span>
                <span className="font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded-md">
                  60s Reconnect Guard
                </span>
              </div>

            </div>
          </div>

        </div>
      </main>

      {/* LOGIN MODAL */}
      {isModalOpen && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-xs animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry">
            
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <h3 className="font-heading font-extrabold text-base text-slate-900">
                Xác Thực Tài Khoản AIVES
              </h3>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-700 p-1 rounded-lg transition"
                aria-label="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmitLogin} className="space-y-3.5 text-xs text-slate-700">
              {errorMsg && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-[11px] font-medium">
                  {errorMsg}
                </div>
              )}

              <div>
                <label className="font-bold text-slate-800 block mb-1">Email Học Thuật (@fpt.edu.vn)</label>
                <input
                  ref={emailInputRef}
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="admin@aives.edu.vn"
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-sky-500 focus:bg-white focus:outline-none transition"
                />
              </div>

              <div>
                <label className="font-bold text-slate-800 block mb-1">Mật Khẩu</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 pr-9 text-xs focus:ring-2 focus:ring-sky-500 focus:bg-white focus:outline-none transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600 focus:outline-none"
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1">
                <label className="flex items-center gap-1.5 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="rounded text-sky-600 focus:ring-sky-500"
                  />
                  <span>Ghi nhớ phiên</span>
                </label>
                <button
                  type="button"
                  onClick={() => alert('Vui lòng liên hệ Exam Office trường để đặt lại mật khẩu.')}
                  className="text-sky-600 font-semibold hover:underline"
                >
                  Quên mật khẩu?
                </button>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
                >
                  Đóng
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="pill-btn px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold shadow-xs flex items-center gap-1.5 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-sky-500"
                >
                  {isLoading && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                  <span>Vào Hệ Thống ↗</span>
                </button>
              </div>
            </form>

          </div>
        </div>
      )}

      {/* FOOTER */}
      <footer className="w-full max-w-7xl mx-auto px-6 py-4 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-2 border-t border-slate-200/60">
        <span>© 2026 AIVES — Artificial Intelligence Voice Evaluation System. Môn học SWD392 FPT University.</span>
        <div className="flex items-center gap-4">
          <button onClick={() => onNavigate('admin-users')} className="hover:text-slate-800 font-semibold text-sky-600">
            Admin Center
          </button>
        </div>
      </footer>

    </div>
  );
}
