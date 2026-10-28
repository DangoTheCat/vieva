import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import { ShieldCheck, UserPlus, Check, X, ShieldAlert, Eye, EyeOff, Award, Sparkles, CheckCircle2, ArrowRight } from 'lucide-react';

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

      const result = await register(payload);
      if (showToast) {
        showToast({
          type: 'success',
          title: 'Đăng Ký Thành Công',
          message: `Tài khoản ${result.user?.email || email} đã được kích hoạt thành công!`
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
    <div className="min-h-screen flex flex-col justify-between bg-slate-50/60 text-ink antialiased font-sans selection:bg-slate-200 selection:text-ink">

      {/* TOP HEADER BAR */}
      <header className="w-full max-w-7xl mx-auto px-6 py-5 flex items-center justify-between border-b border-pink-200/80 bg-white/70 backdrop-blur-sm sticky top-0 z-20">
        
        {/* Brand Logo */}
        <div
          className="flex items-center gap-3 cursor-pointer select-none group"
          onClick={() => onNavigate('login')}
          role="button"
          tabIndex={0}
        >
          <div className="w-9 h-9 rounded-xl bg-pink-600 text-white flex items-center justify-center shadow-xs group-hover:scale-105 transition-transform">
            <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
            </svg>
          </div>
          <div>
            <span className="font-heading font-black text-2xl tracking-tight text-ink">aives.</span>
            <span className="text-[10px] text-slate-500 font-mono block -mt-1 leading-none">Oral Exam Assessment</span>
          </div>
        </div>

        {/* Right: Link to Sign In */}
        <div className="flex items-center gap-3">
          <span className="text-xs text-slate-500 hidden sm:inline">Đã có tài khoản FPT?</span>
          <button
            onClick={() => onNavigate('login')}
            className="px-3.5 py-1.5 rounded-xl border border-pink-200 bg-white text-slate-700 hover:bg-slate-50 text-xs font-semibold shadow-2xs active:scale-[0.98] transition cursor-pointer"
          >
            Đăng Nhập ↗
          </button>
        </div>
      </header>

      {/* ================= MAIN REGISTRATION CONTAINER ================= */}
      <main className="flex-1 py-12 px-4 flex items-center justify-center">
        <div className="max-w-md w-full bg-white rounded-2xl border border-pink-200/90 shadow-sm p-6 sm:p-8 space-y-6 animate-modal-entry text-slate-800">
          
          {/* Card Header */}
          <div className="text-center space-y-2">
            <div className="w-12 h-12 rounded-xl bg-pink-50 border border-pink-200 text-pink-600 flex items-center justify-center mx-auto shadow-2xs">
              <UserPlus className="w-6 h-6 text-pink-600" />
            </div>
            <h1 className="font-heading font-extrabold text-2xl text-ink tracking-tight">
              Kích Hoạt Tài Khoản Khảo Thí
            </h1>
            <p className="text-xs text-slate-500 max-w-sm mx-auto leading-relaxed">
              Đăng ký tài khoản sinh viên để tham gia phòng thi vấn đáp ảo AIVES
            </p>
          </div>

          {/* Form Inputs */}
          <form onSubmit={handleSubmit} className="space-y-4 text-xs" autoComplete="off">

            {errorMessage && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 flex items-center gap-2 font-medium animate-in fade-in">
                <ShieldAlert className="w-4 h-4 shrink-0 text-rose-600" />
                <span>{errorMessage}</span>
              </div>
            )}

            <div className="bg-slate-50 border border-pink-200 rounded-xl p-3 flex items-start gap-2.5 text-left text-slate-600">
              <ShieldCheck className="w-4 h-4 text-pink-600 shrink-0 mt-0.5" />
              <p className="text-[11px] leading-relaxed">
                <span className="font-semibold text-slate-800">Lưu ý học thuật:</span> Tự đăng ký áp dụng cho sinh viên thi vấn đáp. Tài khoản giảng viên và giám thị được cấp bởi ban khảo thí.
              </p>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                <span>Họ Và Tên</span>
                <span className="text-rose-500 font-bold ml-1">*</span>
              </label>
              <input
                type="text"
                required
                autoComplete="name"
                maxLength={50}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="vd: Nguyễn Văn A"
                className="w-full py-2.5 px-3 text-xs rounded-xl bg-white border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 text-ink shadow-2xs outline-none transition placeholder:text-slate-400"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                <span>Email Trường Cấp</span>
                <span className="text-rose-500 font-bold ml-1">*</span>
              </label>
              <input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="vd: anva@fpt.edu.vn"
                className="w-full py-2.5 px-3 text-xs rounded-xl bg-white border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 text-ink shadow-2xs outline-none transition placeholder:text-slate-400"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                <span>Mật Khẩu</span>
                <span className="text-rose-500 font-bold ml-1">*</span>
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  autoComplete="new-password"
                  minLength={6}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Nhập mật khẩu (tối thiểu 6 ký tự)"
                  className="w-full py-2.5 pl-3 pr-9 text-xs rounded-xl bg-white border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 text-ink shadow-2xs outline-none transition placeholder:text-slate-400"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-3 text-slate-400 hover:text-slate-600 focus:outline-none cursor-pointer"
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                <span>Số Điện Thoại</span>
                <span className="text-slate-400 font-normal ml-1">(Tùy chọn)</span>
              </label>
              <input
                type="tel"
                autoComplete="tel"
                maxLength={12}
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="vd: 0912 345 678"
                className="w-full py-2.5 px-3 text-xs rounded-xl bg-white border border-pink-200 focus:border-pink-500 focus:ring-1 focus:ring-pink-500 text-ink shadow-2xs outline-none transition placeholder:text-slate-400"
              />
            </div>

            {/* Terms Agreement */}
            <div className="pt-1">
              <label className="flex items-start gap-2 cursor-pointer text-slate-600 text-[11px] select-none">
                <input
                  type="checkbox"
                  checked={agreeTerms}
                  onChange={(e) => setAgreeTerms(e.target.checked)}
                  className="rounded border border-pink-200 checked:bg-pink-600 text-ink focus:ring-pink-500 mt-0.5 shadow-2xs accent-pink-600 cursor-pointer"
                />
                <span className="leading-relaxed">
                  Tôi đồng ý với{' '}
                  <button
                    type="button"
                    onClick={() => setIsTermsOpen(true)}
                    className="text-pink-600 font-semibold underline underline-offset-2 hover:text-pink-800 transition-colors cursor-pointer"
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
              className="w-full py-3 px-4 bg-pink-600 hover:bg-pink-700 active:bg-pink-800 active:scale-[0.98] text-white font-semibold text-xs rounded-xl shadow-xs transition-all disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                  <span>Đang kích hoạt...</span>
                </>
              ) : (
                <>
                  <span>Kích Hoạt Tài Khoản &amp; Bắt Đầu</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </>
              )}
            </button>
          </form>

        </div>
      </main>

      {/* ================= FOOTER ================= */}
      <footer className="bg-white border-t border-pink-200 text-slate-500 text-xs py-4 px-6 text-center">
        © 2026 AIVES Assessment Network • FPT University SWD392
      </footer>

      {/* ================= MODAL: TERMS & INTEGRITY ================= */}
      {isTermsOpen && (
        <div
          className="fixed inset-0 z-50 bg-pink-600/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white rounded-2xl max-w-lg w-full border border-pink-200 shadow-xl overflow-hidden flex flex-col animate-modal-entry text-ink">
            <div className="p-4 border-b border-pink-100 flex items-center justify-between">
              <h3 className="font-heading font-bold text-sm text-ink">
                Điều Khoản Khảo Thí Trực Tuyến &amp; Bảo Mật Dữ Liệu
              </h3>
              <button
                type="button"
                onClick={() => setIsTermsOpen(false)}
                className="text-slate-400 hover:text-slate-700 w-8 h-8 rounded-lg flex items-center justify-center transition cursor-pointer"
                aria-label="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 overflow-y-auto space-y-2.5 text-xs text-slate-600 leading-relaxed max-h-72">
              <div className="bg-slate-50 p-3 rounded-xl border border-pink-200 text-slate-700">
                <p>1. Bản ghi âm giọng nói của sinh viên trong suốt ca thi chỉ được sử dụng cho mục đích chấm thi và phúc khảo điểm học kỳ.</p>
              </div>
              <div className="bg-slate-50 p-3 rounded-xl border border-pink-200 text-slate-700">
                <p>2. Dữ liệu âm thanh được bảo mật bằng tiêu chuẩn mã hóa AES-256 và lưu vết bằng chữ ký số SHA-256 không thể giả mạo.</p>
              </div>
              <div className="bg-slate-50 p-3 rounded-xl border border-pink-200 text-slate-700">
                <p>3. Sinh viên có quyền khiếu nại và yêu cầu phúc khảo âm thanh trong vòng 48 giờ sau khi ca thi kết thúc.</p>
              </div>
            </div>

            <div className="p-4 bg-slate-50 border-t border-pink-100 flex items-center justify-end">
              <button
                type="button"
                onClick={() => setIsTermsOpen(false)}
                className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-95 cursor-pointer"
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
