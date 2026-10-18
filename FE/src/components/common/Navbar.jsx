import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import {
  ShieldCheck,
  UserPlus,
  User,
  Key,
  LogOut,
  ChevronDown,
  Mic2,
  Table2,
  GraduationCap,
  HelpCircle,
  X
} from 'lucide-react';
import { VoiceWaveform } from './VoiceWaveform';

export function Navbar({ onOpenAddUser, onOpenProfile, onOpenMatrix, onNavigate, currentTab }) {
  const { currentUser, logout, isDemoMode, setIsDemoMode, isLiveBackendReachable, isAdmin, isLecturer } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [isTroubleshootOpen, setIsTroubleshootOpen] = useState(false);
  const dropdownRef = useRef(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Keyboard accessibility: Escape closes dropdown
  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape') setDropdownOpen(false);
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const getInitials = (name) => {
    if (!name) return 'AD';
    return name
      .split(' ')
      .filter(Boolean)
      .map(part => part[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  };

  // If on calibration view, render the AIVES Calibration Wizard Header (Placed AFTER hooks to respect React Rules of Hooks)
  if (currentTab === 'calibration') {
    return (
      <>
        <header className="h-16 bg-paper text-ink px-6 flex items-center justify-between shrink-0 shadow-[0_3px_0_#DEB9B5] border-b-2 border-green-500 sticky top-0 z-40 select-none">
          <div className="flex items-center gap-3">
            <button 
              type="button"
              onClick={() => onNavigate && onNavigate('discover')}
              className="w-9 h-9 rounded-xl bg-pink-100 hover:bg-pink-200 border-2 border-green-500 flex items-center justify-center text-pink-700 shadow-[2px_2px_0_#DEB9B5] transition cursor-pointer outline-none active:scale-95"
              aria-label="Về trang chủ"
            >
              <GraduationCap className="w-5 h-5 text-pink-700" />
            </button>
            <div>
              <h1 className="font-heading font-black text-sm text-ink tracking-tight">
                AIVES Calibration Wizard
              </h1>
              <p className="text-[11px] text-green-700 font-medium">
                Bước chuẩn bị bắt buộc trước khi vào phòng thi chính thức
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setIsTroubleshootOpen(true)}
            className="text-xs font-bold text-ink hover:text-pink-900 bg-pink-100 hover:bg-pink-200 px-3.5 py-1.5 rounded-xl border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5] transition flex items-center gap-1.5 cursor-pointer active:scale-95"
          >
            <HelpCircle className="w-3.5 h-3.5 text-pink-700" />
            <span>Trợ Giúp Kỹ Thuật</span>
          </button>
        </header>

        {/* TROUBLESHOOTING MODAL */}
        {isTroubleshootOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/50 backdrop-blur-xs animate-in fade-in duration-150">
            <div className="bg-paper rounded-2xl max-w-md w-full p-6 shadow-[6px_6px_0_#D5A6A2] border-2 border-green-500 space-y-4 text-ink animate-modal-entry">
              <div className="flex items-center justify-between pb-3 border-b-2 border-cream">
                <h3 className="font-heading font-black text-base text-ink">Trợ Giúp Xử Lý Thiết Bị</h3>
                <button
                  type="button"
                  onClick={() => setIsTroubleshootOpen(false)}
                  className="text-green-800 hover:text-pink-700 p-1 rounded-lg transition cursor-pointer focus:outline-none"
                  aria-label="Đóng"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-3 text-xs text-green-900 leading-relaxed font-normal">
                <div className="p-3 bg-pink-50 rounded-xl border-2 border-green-200">
                  <p className="font-bold text-ink mb-0.5">1. Không nghe thấy tiếng?</p>
                  <p>Kiểm tra nút âm lượng của máy tính hoặc tai nghe đã cắm chặt jack 3.5mm / Bluetooth.</p>
                </div>
                <div className="p-3 bg-pink-50 rounded-xl border-2 border-green-200">
                  <p className="font-bold text-ink mb-0.5">2. Trình duyệt chặn quyền truy cập?</p>
                  <p>Nhấp vào biểu tượng ổ khóa bên trái thanh địa chỉ URL để cấp quyền Microphone và Camera.</p>
                </div>
              </div>

              <div className="pt-2 text-right">
                <button
                  type="button"
                  onClick={() => setIsTroubleshootOpen(false)}
                  className="px-5 py-2.5 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-600 shadow-[2px_2px_0_#3A422E] transition active:scale-95 cursor-pointer"
                >
                  Đã Hiểu
                </button>
              </div>
            </div>
          </div>
        )}
      </>
    );
  }

  return (
    <header className="bg-paper text-ink border-b-2 border-green-500 sticky top-0 z-40 shadow-[0_3px_0_#DEB9B5]">
      <div className="w-full px-4 sm:px-6 h-16 flex items-center justify-between">

        {/* Left: Brand & Nav Links */}
        <div className="flex items-center gap-6">
          <div
            onClick={() => onNavigate(isAdmin ? 'admin-users' : isLecturer ? 'lecturer-questions' : 'discover')}
            className="flex items-center gap-3 cursor-pointer group select-none"
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && onNavigate(isAdmin ? 'admin-users' : isLecturer ? 'lecturer-questions' : 'discover')}
          >
            <div className="flex items-center gap-2">
              <div className="w-9 h-9 rounded-xl bg-pink-100 border-2 border-green-500 flex items-center justify-center text-pink-700 shadow-[2px_2px_0_#DEB9B5] group-hover:scale-105 transition-transform">
                <svg className="w-5 h-5 text-pink-700" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
                </svg>
              </div>
              <span className="font-heading font-black text-2xl tracking-tight text-ink">
                aives<span className="text-pink-600">.</span>
              </span>
            </div>
          </div>

          {/* Desktop Navigation Tabs */}
          <nav className="hidden lg:flex items-center gap-1.5 text-xs">
            {isAdmin && (
              <>
                <button
                  onClick={() => onNavigate('admin-users')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border-2 ${currentTab === 'admin-users'
                      ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                      : 'text-green-900 hover:text-pink-700 hover:bg-green-100/60 border-transparent'
                    }`}
                >
                  1. Quản Lý Tài Khoản
                </button>
                <button
                  onClick={() => onNavigate('admin-subjects')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border-2 ${currentTab === 'admin-subjects'
                      ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                      : 'text-green-900 hover:text-pink-700 hover:bg-green-100/60 border-transparent'
                    }`}
                >
                  2. Môn Học &amp; Phân Công
                </button>
                <button
                  onClick={() => onNavigate('admin-voice-lab')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border-2 ${currentTab === 'admin-voice-lab' || currentTab === 'voice-lab'
                      ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                      : 'text-green-900 hover:text-pink-700 hover:bg-green-100/60 border-transparent'
                    }`}
                >
                  3. Cấu Hình STT/TTS
                </button>
              </>
            )}
            {isLecturer && !isAdmin && (
              <>
                <button
                  onClick={() => onNavigate('lecturer-questions')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border-2 ${currentTab === 'lecturer-questions'
                      ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                      : 'text-green-900 hover:text-pink-700 hover:bg-green-100/60 border-transparent'
                    }`}
                >
                  1. Ngân Hàng Câu Hỏi
                </button>
                <button
                  onClick={() => onNavigate('lecturer-docs-rag')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border-2 ${currentTab === 'lecturer-docs-rag'
                      ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                      : 'text-green-900 hover:text-pink-700 hover:bg-green-100/60 border-transparent'
                    }`}
                >
                  2. Tài Liệu RAG
                </button>
                <button
                  onClick={() => onNavigate('rubric-studio')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border-2 ${currentTab === 'rubric-studio'
                      ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                      : 'text-green-900 hover:text-pink-700 hover:bg-green-100/60 border-transparent'
                    }`}
                >
                  3. Ma Trận Rubric
                </button>
              </>
            )}
            {!isAdmin && !isLecturer && (
              <>
                <button
                  onClick={() => onNavigate('discover')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border-2 ${currentTab === 'discover' || currentTab === 'home'
                      ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                      : 'text-green-900 hover:text-pink-700 hover:bg-green-100/60 border-transparent'
                    }`}
                >
                  Lịch Thi
                </button>
                <button
                  onClick={() => onNavigate('calibration')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border-2 ${currentTab === 'calibration'
                      ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                      : 'text-green-900 hover:text-pink-700 hover:bg-green-100/60 border-transparent'
                    }`}
                >
                  Kiểm Tra Mic / Cam
                </button>
                <button
                  onClick={() => onNavigate('exam-room')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border-2 ${currentTab === 'exam-room'
                      ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                      : 'text-green-900 hover:text-pink-700 hover:bg-green-100/60 border-transparent'
                    }`}
                >
                  Phòng Thi Vấn Đáp AI
                </button>
              </>
            )}
          </nav>
        </div>

        {/* Right: Actions, AI Engine Status, Mode Switch, Profile Dropdown */}
        <div className="flex items-center gap-3">

          {/* Real-time Voice Audio Activity (GSAP 60 FPS) */}
          <div className="hidden xl:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-pink-50 border-2 border-green-400 text-ink shadow-[2px_2px_0_#C4CCB7] text-[11px]">
            <Mic2 className="w-3.5 h-3.5 text-pink-700" />
            <span className="text-green-800 text-[10px] font-mono font-bold">Speech Engine:</span>
            <VoiceWaveform isSpeaking={true} volume={0.45} barCount={6} height={14} barWidth="w-0.5" barGap="gap-0.5" colorClass="bg-pink-600" />
            <span className="text-[10px] text-green-900 font-bold font-mono bg-green-200 px-1.5 py-0.5 rounded border border-green-400">Ready</span>
          </div>

          {/* Quick Action: Thêm Người Dùng */}
          {isAdmin && (
            <button
              onClick={onOpenAddUser}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink text-xs font-bold border-2 border-green-600 shadow-[2px_2px_0_#3A422E] transition-all active:translate-x-0.5 active:translate-y-0.5 active:shadow-none focus:outline-none cursor-pointer"
            >
              <UserPlus className="w-4 h-4 text-ink" />
              <span className="hidden sm:inline">Thêm Người Dùng</span>
            </button>
          )}

          <div className="h-6 w-0.5 bg-green-300 hidden sm:block"></div>

          {/* User Profile Pill & Dropdown */}
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setDropdownOpen(!dropdownOpen)}
              className={`flex items-center gap-2.5 p-1.5 pl-2 pr-3 rounded-full border-2 transition-all shadow-[2px_2px_0_#DEB9B5] group cursor-pointer focus:outline-none ${
                currentTab === 'profile' || currentTab === 'profile-password'
                  ? 'bg-pink-100 border-green-600 ring-2 ring-pink-300'
                  : 'bg-paper hover:bg-pink-50 border-green-500 hover:border-green-600'
              }`}
              aria-expanded={dropdownOpen}
              aria-haspopup="true"
            >
              <div className="w-8 h-8 rounded-full bg-pink-300 border-2 border-green-600 text-ink font-heading font-extrabold text-xs flex items-center justify-center shadow-xs shrink-0 overflow-hidden">
                {currentUser?.avatarUrl ? (
                  <img 
                    src={currentUser.avatarUrl} 
                    alt="Avatar" 
                    className="w-full h-full object-cover" 
                    onError={(e) => {
                      e.currentTarget.style.display = 'none';
                      if (e.currentTarget.nextSibling) {
                        e.currentTarget.nextSibling.style.display = 'inline';
                      }
                    }}
                  />
                ) : null}
                <span className={currentUser?.avatarUrl ? 'hidden' : ''}>
                  {getInitials(currentUser?.fullName)}
                </span>
              </div>
              <div className="text-left leading-tight">
                <p className="font-heading font-bold text-xs text-ink group-hover:text-pink-700 transition truncate max-w-[140px]">
                  {currentUser?.fullName || 'Người Dùng AIVES'}
                </p>
                <p className="text-[10px] font-bold text-green-700 mt-0.5 tracking-wide">
                  {isAdmin ? 'Ban Khảo Thí' : isLecturer ? 'Giảng Viên' : 'Sinh Viên'}
                </p>
              </div>
              <ChevronDown className={`w-3.5 h-3.5 text-green-700 group-hover:text-pink-700 ml-0.5 transition-transform ${dropdownOpen ? 'rotate-180' : ''}`} />
            </button>

            {/* Dropdown Menu */}
            {dropdownOpen && (
              <div className="absolute right-0 mt-2 w-64 bg-paper rounded-2xl shadow-[4px_4px_0_#D5A6A2] border-2 border-green-500 text-ink py-2 z-50 animate-modal-entry">
                <div className="px-4 py-2.5 border-b-2 border-cream bg-pink-50/70 flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-pink-300 border-2 border-green-500 text-ink font-heading font-extrabold text-xs flex items-center justify-center shadow-xs shrink-0 overflow-hidden">
                    {currentUser?.avatarUrl ? (
                      <img 
                        src={currentUser.avatarUrl} 
                        alt="Avatar" 
                        className="w-full h-full object-cover" 
                        onError={(e) => {
                          e.currentTarget.style.display = 'none';
                          if (e.currentTarget.nextSibling) {
                            e.currentTarget.nextSibling.style.display = 'inline';
                          }
                        }}
                      />
                    ) : null}
                    <span className={currentUser?.avatarUrl ? 'hidden' : ''}>
                      {getInitials(currentUser?.fullName)}
                    </span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-bold text-ink truncate">{currentUser?.fullName}</p>
                    <p className="text-[11px] text-green-800 font-mono truncate">{currentUser?.email}</p>
                    <span className="inline-block mt-0.5 text-[10px] font-mono px-2 py-0.5 rounded bg-green-100 text-green-900 font-bold border border-green-300">
                      {currentUser?.userCode || 'USR-CODE'}
                    </span>
                  </div>
                </div>

                <div className="py-1 text-xs">
                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      onOpenProfile('info');
                    }}
                    className={`w-full px-4 py-2 text-left hover:bg-pink-100 flex items-center gap-2.5 font-medium transition cursor-pointer ${
                      currentTab === 'profile' ? 'bg-pink-100 text-pink-900 font-bold' : 'text-ink'
                    }`}
                  >
                    <User className="w-4 h-4 text-green-700" />
                    <span>Hồ Sơ Của Tôi</span>
                  </button>

                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      onOpenProfile('password');
                    }}
                    className={`w-full px-4 py-2 text-left hover:bg-pink-100 flex items-center gap-2.5 font-medium transition cursor-pointer ${
                      currentTab === 'profile-password' ? 'bg-pink-100 text-pink-900 font-bold' : 'text-ink'
                    }`}
                  >
                    <Key className="w-4 h-4 text-green-700" />
                    <span>Đổi Mật Khẩu</span>
                  </button>
                </div>

                <div className="border-t-2 border-cream pt-1 text-xs mt-1">
                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      logout();
                      onNavigate('login');
                    }}
                    className="w-full px-4 py-2 text-left hover:bg-pink-100 flex items-center gap-2.5 text-pink-700 font-bold transition cursor-pointer"
                  >
                    <LogOut className="w-4 h-4 text-pink-700" />
                    <span>Đăng Xuất</span>
                  </button>
                </div>
              </div>
            )}
          </div>

        </div>

      </div>
    </header>
  );
}
