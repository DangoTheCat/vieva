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
    <div className="min-h-screen flex flex-col justify-between bg-paper text-ink antialiased font-sans relative overflow-x-hidden selection:bg-pink-200 selection:text-ink">

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
            <div className="w-9 h-9 rounded-xl bg-pink-50 text-pink-600 flex items-center justify-center shadow-[3px_3px_0px_#DEB9B5] border border-pink-200 group-hover:scale-105 transition-transform">
              <svg className="w-5 h-5 text-pink-700" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </div>
            <span className="font-heading font-black text-2xl tracking-tight text-ink">aives.</span>
          </div>
        </div>

        {/* Right: Link to Sign In */}
        <div className="flex items-center gap-3">
          <span className="text-xs text-on-surface-variant hidden sm:inline">Đã có tài khoản FPT?</span>
          <button
            onClick={() => onNavigate('login')}
            className="px-4 py-2 rounded-xl border border-green-200 bg-paper text-green-800 hover:bg-green-50 text-xs font-bold shadow-[3px_3px_0px_#DEB9B5] active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all focus:outline-none focus:ring-2 focus:ring-green-600 cursor-pointer"
          >
            Đăng Nhập ↗
          </button>
        </div>
      </header>

      {/* ================= MAIN REGISTRATION CONTAINER ================= */}
      <main className="flex-1 py-12 px-4 flex items-center justify-center relative z-10">
        <div className="max-w-md w-full bg-paper rounded-[24px] border-[3px] border-green-600 shadow-[6px_6px_0_#D5A6A2] p-6 sm:p-8 space-y-6 animate-modal-entry text-ink relative overflow-hidden">
          
          {/* Subtle top paper accent strip */}
          <div className="absolute top-0 left-0 right-0 h-2 bg-pink-100 border-b border-green-200" />

          {/* Card Header Icon */}
          <div className="text-center space-y-2 pt-1">
            <div className="w-14 h-14 rounded-2xl bg-paper border-[3px] border-green-600 shadow-[4px_4px_0_#DEB9B5] flex items-center justify-center mx-auto text-pink-600">
              <ShieldCheck className="w-7 h-7 text-pink-600" />
            </div>
            <h1 className="font-heading font-extrabold text-xl text-ink tracking-tight">
              Kích Hoạt Tài Khoản Khảo Thí
            </h1>
            <p className="text-xs text-on-surface-variant max-w-sm mx-auto leading-relaxed">
              Đăng ký để tham gia phòng thi vấn đáp ảo
            </p>
          </div>

          {/* Form Inputs */}
          <form onSubmit={handleSubmit} className="space-y-4 text-xs" autoComplete="off">

            {errorMessage && (
              <div className="p-3 bg-pink-100 border border-pink-300 rounded-xl text-pink-900 flex items-center gap-2 font-medium animate-in fade-in">
                <ShieldAlert className="w-4 h-4 shrink-0 text-pink-700" />
                <span>{errorMessage}</span>
              </div>
            )}

            <div className="bg-green-50 border-2 border-green-500/70 rounded-xl p-3 flex items-start gap-2.5 text-left">
              <ShieldCheck className="w-4 h-4 text-green-800 shrink-0 mt-0.5" />
              <p className="text-[11px] text-green-900 leading-relaxed">
                <span className="font-bold text-green-800">Lưu ý học thuật:</span> Đăng ký dành cho sinh viên. Tài khoản giảng viên do quản trị viên cấp.
              </p>
            </div>

            <div>
              <label className="block font-bold text-ink mb-1">
                <span>Họ Và Tên</span>
                <span className="text-pink-700 font-bold ml-1">*</span>
              </label>
              <input
                type="text"
                required
                autoComplete="off"
                maxLength={50}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nguyễn Quang Thổ"
                className="w-full p-2.5 text-xs rounded-xl bg-surface-container-lowest border-2 border-green-300 focus:border-green-600 focus:shadow-[3px_3px_0_#DEB9B5] text-ink focus:outline-none transition placeholder:text-on-surface-variant/60"
              />
            </div>

            <div>
              <label className="block font-bold text-ink mb-1">
                <span>Email Trường Cấp</span>
                <span className="text-pink-700 font-bold ml-1">*</span>
              </label>
              <input
                type="email"
                required
                autoComplete="off"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="student@fpt.edu.vn"
                className="w-full p-2.5 text-xs rounded-xl bg-surface-container-lowest border-2 border-green-300 focus:border-green-600 focus:shadow-[3px_3px_0_#DEB9B5] text-ink focus:outline-none transition placeholder:text-on-surface-variant/60"
              />
            </div>

            <div>
              <label className="block font-bold text-ink mb-1">
                <span>Mật Khẩu</span>
                <span className="text-pink-700 font-bold ml-1">*</span>
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
                  className="w-full p-2.5 pr-8 text-xs rounded-xl bg-surface-container-lowest border-2 border-green-300 focus:border-green-600 focus:shadow-[3px_3px_0_#DEB9B5] text-ink focus:outline-none transition placeholder:text-on-surface-variant/60"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2.5 top-2.5 text-on-surface-variant hover:text-ink focus:outline-none cursor-pointer"
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block font-bold text-ink mb-1">
                <span>Số Điện Thoại</span>
                <span className="text-on-surface-variant font-normal ml-1">(Tùy chọn)</span>:
              </label>
              <input
                type="text"
                autoComplete="off"
                maxLength={12}
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="0xx xxx xxxx"
                className="w-full p-2.5 text-xs rounded-xl bg-surface-container-lowest border-2 border-green-300 focus:border-green-600 focus:shadow-[3px_3px_0_#DEB9B5] text-ink focus:outline-none transition placeholder:text-on-surface-variant/60"
              />
            </div>

            {/* Terms Agreement */}
            <div className="pt-1">
              <label className="flex items-start gap-2 cursor-pointer text-ink text-[11px] select-none">
                <input
                  type="checkbox"
                  checked={agreeTerms}
                  onChange={(e) => setAgreeTerms(e.target.checked)}
                  className="rounded border-2 border-green-500 checked:bg-green-600 checked:border-green-700 text-green-600 focus:ring-green-500 mt-0.5 shadow-[2px_2px_0_#DEB9B5] accent-green-600 cursor-pointer"
                />
                <span className="leading-relaxed">
                  Tôi đồng ý với{' '}
                  <button
                    type="button"
                    onClick={() => setIsTermsOpen(true)}
                    className="text-green-800 font-bold underline underline-offset-2 hover:text-green-950 transition-colors cursor-pointer"
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
              className="w-full py-3 px-4 bg-pink-500 hover:bg-pink-600 active:translate-x-[3px] active:translate-y-[3px] active:shadow-none text-ink font-heading font-bold text-xs rounded-2xl border-2 border-ink shadow-[3px_3px_0_#DEB9B5] transition-all disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer group"
            >
              {isSubmitting && <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>}
              <span>Kích Hoạt Tài Khoản &amp; Bắt Đầu</span>
            </button>
          </form>

        </div>
      </main>

      {/* ================= FOOTER ================= */}
      <footer className="bg-paper border-t border-green-200 text-green-800 text-xs py-4 px-6 text-center relative z-10">
        © 2026 AIVES Assessment Network • FPT University SWD392
      </footer>

      {/* ================= MODAL: TERMS & INTEGRITY ================= */}
      {isTermsOpen && (
        <div
          className="fixed inset-0 z-50 bg-ink/65 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-paper rounded-2xl max-w-lg w-full border-[3px] border-green-600 shadow-[6px_6px_0_#D5A6A2] overflow-hidden flex flex-col animate-modal-entry text-ink">
            <div className="p-4 border-b-2 border-cream bg-paper text-ink flex items-center justify-between">
              <h3 className="font-heading font-extrabold text-sm text-pink-800">
                Điều Khoản Khảo Thí Trực Tuyến &amp; Bảo Mật Dữ Liệu
              </h3>
              <button
                type="button"
                onClick={() => setIsTermsOpen(false)}
                className="text-green-800 hover:text-pink-700 hover:bg-pink-100 w-8 h-8 rounded-lg flex items-center justify-center transition-colors cursor-pointer"
                aria-label="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 overflow-y-auto space-y-3 text-xs text-on-surface-variant leading-relaxed max-h-72">
              <div className="bg-green-50 p-3 rounded-xl border border-green-200 text-green-900">
                <p>1. Bản ghi âm giọng nói của sinh viên trong suốt ca thi chỉ được sử dụng cho mục đích chấm thi và phúc khảo điểm học kỳ.</p>
              </div>
              <div className="bg-pink-50 p-3 rounded-xl border border-pink-200 text-pink-900">
                <p>2. Dữ liệu âm thanh được bảo mật bằng tiêu chuẩn mã hóa AES-256 và lưu vết bằng chữ ký số SHA-256 không thể giả mạo.</p>
              </div>
              <div className="bg-surface-container-low p-3 rounded-xl border border-cream text-ink">
                <p>3. Sinh viên có quyền khiếu nại và yêu cầu phúc khảo âm thanh trong vòng 48 giờ sau khi ca thi kết thúc.</p>
              </div>
            </div>

            <div className="p-4 bg-paper border-t-2 border-cream flex items-center justify-end">
              <button
                type="button"
                onClick={() => setIsTermsOpen(false)}
                className="px-5 py-2 rounded-xl bg-pink-500 hover:bg-pink-600 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none text-ink font-bold text-xs border-2 border-ink shadow-[2px_2px_0_#DEB9B5] transition-all cursor-pointer"
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
