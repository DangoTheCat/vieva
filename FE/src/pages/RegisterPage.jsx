import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import { ShieldCheck, UserPlus, Check, X, ShieldAlert, Eye, EyeOff, Award, Sparkles, CheckCircle2 } from 'lucide-react';

export function RegisterPage({ onNavigate, showToast }) {
  const { register } = useAuth();

  const [roleType, setRoleType] = useState('student'); // student, lecturer
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [activationCode, setActivationCode] = useState('');
  const [agreeTerms, setAgreeTerms] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isTermsOpen, setIsTermsOpen] = useState(false);

  // Keyboard accessibility: Escape to close terms modal
  useEffect(() => {
    if (!isTermsOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') setIsTermsOpen(false);
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isTermsOpen]);

  const handleSsoClick = () => {
    setFullName('Nguyễn Thị Mai');
    setEmail('mainvse160892@fpt.edu.vn');
    setPassword('123456');
    setPhoneNumber('0987654321');
    setActivationCode('SWD392');
    if (showToast) {
      showToast({
        type: 'info',
        title: 'FPT Edu SSO',
        message: 'Đã tự động điền mẫu thông tin FPT Education SSO!'
      });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!agreeTerms) {
      setErrorMessage('Bạn cần đồng ý với quy định liêm chính khảo thí.');
      return;
    }

    if (!email.trim() || !/\S+@\S+\.\S+/.test(email.trim())) {
      setErrorMessage('Email không đúng định dạng.');
      return;
    }

    if (!password || password.length < 6) {
      setErrorMessage('Mật khẩu bắt buộc có tối thiểu 6 ký tự.');
      return;
    }

    if (phoneNumber && !/^(\+?[0-9]{9,11})$/.test(phoneNumber.trim())) {
      setErrorMessage('Số điện thoại phải từ 9 đến 11 chữ số.');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        email: email.trim().toLowerCase(),
        password,
        fullName: fullName.trim() || (roleType === 'student' ? 'Sinh Viên FPT' : 'Giảng Viên FPT'),
        phoneNumber: phoneNumber ? phoneNumber.trim() : null
      };

      const result = await register(payload);
      if (showToast) {
        showToast({
          type: 'success',
          title: 'Đăng Ký Thành Công',
          message: `Tài khoản ${result.user?.email || email} đã được kích hoạt thành công!`
        });
      }
      onNavigate('admin-users');
    } catch (err) {
      const msg = getErrorMessage(err);
      setErrorMessage(msg);
      if (showToast) showToast({ type: 'error', message: msg });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-canvasBg text-slate-800 antialiased min-h-screen flex flex-col font-sans selection:bg-sky-500 selection:text-white">

      {/* ================= TOP APP BAR (DEEP NAVY HIGH CONTRAST) ================= */}
      <header className="bg-sidebarBg text-white border-b border-slate-700/80 sticky top-0 z-40 shadow-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          {/* Left: Brand */}
          <div
            className="flex items-center gap-2.5 cursor-pointer select-none group"
            onClick={() => onNavigate('login')}
            role="button"
            tabIndex={0}
          >
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-600 to-sky-400 flex items-center justify-center text-white shadow-md shadow-sky-900/30 group-hover:scale-105 transition-transform">
              <ShieldCheck className="w-6 h-6 text-sky-100" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-heading font-extrabold text-lg text-white tracking-tight">AIVES Exam Portal</span>
                <span className="bg-sky-500/20 text-sky-300 text-[10px] font-mono px-2 py-0.5 rounded border border-sky-400/30 font-bold">REGISTER</span>
              </div>
              <p className="text-[11px] text-slate-400">Trường Đại học FPT • Hệ Thống Khảo Thí AI</p>
            </div>
          </div>

          {/* Right: Link to Sign In */}
          <div className="flex items-center gap-3">
            <span className="text-xs text-slate-400 hidden sm:inline">Đã có tài khoản FPT?</span>
            <button
              onClick={() => onNavigate('login')}
              className="px-3.5 py-1.5 rounded-xl border border-slate-700 bg-slate-800/80 hover:bg-slate-700 text-white text-xs font-semibold transition-all focus:outline-none focus:ring-2 focus:ring-sky-500 active:scale-95"
            >
              Đăng Nhập ↗
            </button>
          </div>
        </div>
      </header>

      {/* ================= MAIN REGISTRATION CONTAINER ================= */}
      <main className="flex-1 canvas-dot-grid py-12 px-4 flex items-center justify-center">
        <div className="max-w-md w-full bg-cardBg rounded-2xl border border-slate-200/90 shadow-xl overflow-hidden p-6 sm:p-8 space-y-6 animate-modal-entry">

          {/* Card Header Icon (Filled Blue Shield Check - Image 2) */}
          <div className="text-center space-y-2">
            <div className="w-12 h-12 rounded-2xl bg-sky-50 flex items-center justify-center mx-auto border border-sky-100 shadow-2xs">
              <ShieldCheck className="w-6 h-6 text-white fill-sky-600" />
            </div>
            <h1 className="font-heading font-extrabold text-xl text-slate-900 tracking-tight">
              Kích Hoạt Tài Khoản Khảo Thí
            </h1>
            <p className="text-xs text-slate-500 max-w-sm mx-auto leading-relaxed">
              Đăng ký để tham gia phòng thi vấn đáp ảo hoặc quản lý ngân hàng câu hỏi khảo thí SWD392.
            </p>
          </div>

          {/* 1-Click FPT Edu SSO Button (Official Google Email Logo) */}
          <div>
            <button
              type="button"
              onClick={handleSsoClick}
              className="w-full flex items-center justify-center gap-3 py-3 px-4 bg-white border border-slate-300 hover:border-sky-500 hover:bg-slate-50 rounded-xl text-slate-800 text-xs font-bold transition-all shadow-2xs active:scale-95 group focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" fill="#FBBC05" />
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" fill="#EA4335" />
              </svg>
              <span>Đăng ký nhanh với Email FPT Edu</span>
            </button>
          </div>

          {/* Divider */}
          <div className="relative flex items-center justify-center">
            <div className="border-t border-slate-200 w-full"></div>
            <span className="bg-cardBg px-3 text-[10px] font-bold text-slate-400 uppercase tracking-wider shrink-0">
              HOẶC NHẬP MÃ THÍ SINH
            </span>
          </div>

          {/* Form Inputs */}
          <form onSubmit={handleSubmit} className="space-y-4 text-xs">

            {errorMessage && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 flex items-center gap-2 font-medium">
                <ShieldAlert className="w-4 h-4 shrink-0 text-rose-500" />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Role Segmented Switcher */}
            <div>
              <label className="block font-bold text-slate-700 mb-1.5">Vai Trò Đăng Ký:</label>
              <div className="grid grid-cols-2 p-1 bg-slate-100 rounded-xl gap-1">
                <button
                  type="button"
                  onClick={() => setRoleType('student')}
                  className={`py-1.5 rounded-lg font-bold text-xs transition-all ${roleType === 'student'
                    ? 'bg-white text-sky-700 shadow-2xs font-bold'
                    : 'text-slate-500 font-semibold hover:text-slate-800'
                    }`}
                >
                  Sinh Viên
                </button>
                <button
                  type="button"
                  onClick={() => setRoleType('lecturer')}
                  className={`py-1.5 rounded-lg font-bold text-xs transition-all ${roleType === 'lecturer'
                    ? 'bg-white text-sky-700 shadow-2xs font-bold'
                    : 'text-slate-500 font-semibold hover:text-slate-800'
                    }`}
                >
                  Giảng Viên
                </button>
              </div>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Họ Và Tên (*):</label>
              <input
                type="text"
                required
                maxLength={150}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder={roleType === 'student' ? 'Nguyễn Văn A' : 'TS. Trần Minh Đức'}
                className="w-full p-2.5 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white focus:outline-none transition placeholder:text-slate-400"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Email Trường Cấp (*):</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@fpt.edu.vn"
                className="w-full p-2.5 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white focus:outline-none transition placeholder:text-slate-400"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Mật Khẩu (*):</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Mật khẩu tối thiểu 6 ký tự"
                  className="w-full p-2.5 pr-8 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white focus:outline-none transition placeholder:text-slate-400"
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
              <label className="block font-bold text-slate-700 mb-1">Mã Kích Hoạt Ca Thi (Từ Exam Office):</label>
              <input
                type="text"
                value={activationCode}
                onChange={(e) => setActivationCode(e.target.value)}
                placeholder="Mã ca thi 6 ký tự "
                className={`w-full p-2.5 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white focus:outline-none transition placeholder:text-slate-400 ${activationCode ? 'font-mono font-bold tracking-wider' : 'font-sans'}`}
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Số Điện Thoại:</label>
              <input
                type="text"
                maxLength={20}
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="0xx xxx xxxx"
                className="w-full p-2.5 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white focus:outline-none transition placeholder:text-slate-400"
              />
            </div>

            {/* Terms Agreement */}
            <div className="pt-1">
              <label className="flex items-start gap-2 cursor-pointer text-slate-600 text-[11px]">
                <input
                  type="checkbox"
                  checked={agreeTerms}
                  onChange={(e) => setAgreeTerms(e.target.checked)}
                  className="rounded text-sky-600 focus:ring-sky-500 mt-0.5"
                />
                <span>
                  Tôi đồng ý với{' '}
                  <button
                    type="button"
                    onClick={() => setIsTermsOpen(true)}
                    className="text-sky-600 font-bold underline"
                  >
                    Quy định liêm chính khảo thí
                  </button>{' '}
                  và cho phép ghi âm để AI đối chiếu Rubric chấm điểm.
                </span>
              </label>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-sky-600 to-sky-500 hover:from-sky-500 hover:to-sky-400 text-white font-heading font-bold text-xs shadow-md shadow-sky-600/30 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              {isSubmitting && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
              <span>Kích Hoạt Tài Khoản & Bắt Đầu</span>
            </button>
          </form>

        </div>
      </main>

      {/* ================= FOOTER ================= */}
      <footer className="bg-cardBg border-t border-slate-200 text-slate-500 text-xs py-4 px-6 text-center">
        © 2026 AIVES Assessment Network • FPT University SWD392
      </footer>

      {/* ================= MODAL: TERMS & INTEGRITY ================= */}
      {isTermsOpen && (
        <div
          className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry">
            <div className="p-4 border-b border-slate-700 bg-sidebarBg text-white flex items-center justify-between">
              <h3 className="font-heading font-extrabold text-sm">
                Điều Khoản Khảo Thí Trực Tuyến & Bảo Mật Dữ Liệu
              </h3>
              <button
                type="button"
                onClick={() => setIsTermsOpen(false)}
                className="text-slate-400 hover:text-white p-1 rounded-lg transition focus:outline-none focus:ring-2 focus:ring-sky-500"
                aria-label="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 overflow-y-auto space-y-3 text-xs text-slate-700 leading-relaxed max-h-72">
              <p>1. Bản ghi âm giọng nói của sinh viên trong suốt ca thi chỉ được sử dụng cho mục đích chấm thi và phúc khảo điểm học kỳ.</p>
              <p>2. Dữ liệu âm thanh được bảo mật bằng tiêu chuẩn mã hóa AES-256 và lưu vết bằng chữ ký số SHA-256 không thể giả mạo.</p>
              <p>3. Sinh viên có quyền khiếu nại và yêu cầu phúc khảo âm thanh trong vòng 48 giờ sau khi ca thi kết thúc.</p>
            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end">
              <button
                type="button"
                onClick={() => setIsTermsOpen(false)}
                className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                Tôi Đã Hiểu
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
