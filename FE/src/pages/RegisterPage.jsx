import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import { ShieldCheck, UserPlus, Check, X, ShieldAlert, Eye, EyeOff, Award, Sparkles, CheckCircle2 } from 'lucide-react';

export function RegisterPage({ onNavigate, showToast }) {
  const { register } = useAuth();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [phoneNumber, setPhoneNumber] = useState('');
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
        fullName: fullName.trim() || 'Sinh Viên FPT',
        phoneNumber: phoneNumber ? phoneNumber.trim() : null
      };

      // Self-registration always creates a student account; lecturers are created by an admin
      const result = await register(payload, 'ROLE_STUDENT');
      if (showToast) {
        showToast({
          type: result?.isDemoFallback ? 'warning' : 'success',
          title: result?.isDemoFallback ? 'Đăng Ký Khảo Thí (Chế độ Cục bộ)' : 'Đăng Ký Thành Công',
          message: result?.isDemoFallback
            ? `Đã tạo tài khoản ${result.user?.email || email} ở chế độ trải nghiệm. Bạn đã được cấp quyền Sinh viên.`
            : `Tài khoản ${result.user?.email || email} đã được kích hoạt thành công!`
        });
      }
      onNavigate('discover');
    } catch (err) {
      const msg = getErrorMessage(err);
      setErrorMessage(msg);
      if (showToast) showToast({ type: 'error', message: msg });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-between dark-tech-grid text-slate-100 antialiased font-sans relative overflow-x-hidden selection:bg-sky-500 selection:text-white">

      {/* TOP HEADER BAR */}
      <header className="w-full max-w-[1400px] mx-auto px-6 py-5 flex items-center justify-between z-10">

        {/* Brand Logo */}
        <div
          className="flex items-center gap-3 cursor-pointer select-none group"
          onClick={() => onNavigate('login')}
          role="button"
          tabIndex={0}
        >
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-cyan-600 via-sky-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-900/40 border border-cyan-400/30 group-hover:scale-105 transition-transform">
              <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </div>
            <span className="font-heading font-black text-2xl tracking-tight text-white">aives.</span>
          </div>
        </div>

        {/* Right: Link to Sign In */}
        <div className="flex items-center gap-3">
          <span className="text-xs text-slate-400 hidden sm:inline">Đã có tài khoản FPT?</span>
          <button
            onClick={() => onNavigate('login')}
            className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-800/80 hover:bg-slate-700 text-white text-xs font-semibold transition-all focus:outline-none focus:ring-2 focus:ring-sky-500 active:scale-95 cursor-pointer"
          >
            Đăng Nhập ↗
          </button>
        </div>
      </header>

      {/* ================= MAIN REGISTRATION CONTAINER ================= */}
      <main className="flex-1 py-12 px-4 flex items-center justify-center relative z-10">
        <div className="max-w-md w-full bg-slate-950/85 backdrop-blur-xl rounded-3xl border border-slate-800/90 shadow-2xl p-6 sm:p-8 space-y-6 animate-modal-entry text-slate-200">

          {/* Card Header Icon */}
          <div className="text-center space-y-2">
            <div className="w-12 h-12 rounded-2xl bg-sky-500/20 flex items-center justify-center mx-auto border border-sky-400/30 shadow-2xs">
              <ShieldCheck className="w-6 h-6 text-sky-400" />
            </div>
            <h1 className="font-heading font-extrabold text-xl text-white tracking-tight">
              Kích Hoạt Tài Khoản Khảo Thí
            </h1>
            <p className="text-xs text-slate-400 max-w-sm mx-auto leading-relaxed">
              Đăng ký để tham gia phòng thi vấn đáp ảo
            </p>
          </div>

          {/* Form Inputs */}
          <form onSubmit={handleSubmit} className="space-y-4 text-xs" autoComplete="off">

            {errorMessage && (
              <div className="p-3 bg-rose-950/80 border border-rose-800 rounded-xl text-rose-300 flex items-center gap-2 font-medium">
                <ShieldAlert className="w-4 h-4 shrink-0 text-rose-400" />
                <span>{errorMessage}</span>
              </div>
            )}

            <p className="text-[11px] text-slate-400">
              Đăng ký dành cho sinh viên. Tài khoản giảng viên do quản trị viên cấp.
            </p>

            <div>
              <label className="block font-bold text-slate-300 mb-1">Họ Và Tên :</label>
              <input
                type="text"
                required
                autoComplete="off"
                maxLength={50}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nguyễn Quang Thổ"
                className="w-full p-2.5 text-xs rounded-xl bg-slate-900/90 border border-slate-800 text-white focus:ring-2 focus:ring-sky-500 focus:outline-none transition placeholder:text-slate-500"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-300 mb-1">Email Trường Cấp :</label>
              <input
                type="email"
                required
                autoComplete="off"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="student@fpt.edu.vn"
                className="w-full p-2.5 text-xs rounded-xl bg-slate-900/90 border border-slate-800 text-white focus:ring-2 focus:ring-sky-500 focus:outline-none transition placeholder:text-slate-500"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-300 mb-1">Mật Khẩu :</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  autoComplete="new-password"
                  minLength={6}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Nhập mật khẩu (tối thiểu 6 ký tự)"
                  className="w-full p-2.5 pr-8 text-xs rounded-xl bg-slate-900/90 border border-slate-800 text-white focus:ring-2 focus:ring-sky-500 focus:outline-none transition placeholder:text-slate-500"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-200 focus:outline-none"
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>


            <div>
              <label className="block font-bold text-slate-300 mb-1">Số Điện Thoại (Tùy chọn):</label>
              <input
                type="text"
                autoComplete="off"
                maxLength={12}
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="0xx xxx xxxx "
                className="w-full p-2.5 text-xs rounded-xl bg-slate-900/90 border border-slate-800 text-white focus:ring-2 focus:ring-sky-500 focus:outline-none transition placeholder:text-slate-500"
              />
            </div>

            {/* Terms Agreement */}
            <div className="pt-1">
              <label className="flex items-start gap-2 cursor-pointer text-slate-400 text-[11px]">
                <input
                  type="checkbox"
                  checked={agreeTerms}
                  onChange={(e) => setAgreeTerms(e.target.checked)}
                  className="rounded bg-slate-900 border-slate-700 text-sky-500 focus:ring-sky-500 mt-0.5"
                />
                <span>
                  Tôi đồng ý với{' '}
                  <button
                    type="button"
                    onClick={() => setIsTermsOpen(true)}
                    className="text-sky-400 font-bold underline"
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
      <footer className="bg-slate-950/80 border-t border-slate-800/80 text-slate-400 text-xs py-4 px-6 text-center backdrop-blur-xl">
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
