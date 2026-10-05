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
  Bot
} from 'lucide-react';
import { VoiceWaveform } from './VoiceWaveform';

export function Navbar({ onOpenAddUser, onOpenProfile, onOpenMatrix, onOpenAssistant, onNavigate, currentTab }) {
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
        <header className="h-16 bg-[#0B132B] text-white px-6 flex items-center justify-between shrink-0 shadow-lg border-b border-slate-800 sticky top-0 z-40 select-none">
          <div className="flex items-center gap-3">
            <button 
              type="button"
              onClick={() => onNavigate && onNavigate('discover')}
              className="w-9 h-9 rounded-xl bg-gradient-to-tr from-sky-500 to-blue-600 flex items-center justify-center text-white shadow-sm hover:opacity-90 transition cursor-pointer border-none outline-none"
            >
              🎓
            </button>
            <div>
              <h1 className="font-heading font-extrabold text-sm text-white tracking-tight">
                AIVES Calibration Wizard
              </h1>
              <p className="text-[11px] text-slate-400">
                Bước chuẩn bị bắt buộc trước khi vào phòng thi chính thức
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setIsTroubleshootOpen(true)}
            className="text-xs font-semibold text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 px-3.5 py-1.5 rounded-xl border border-slate-700 transition flex items-center gap-1.5 cursor-pointer active:scale-95"
          >
            <span>⚙ Trợ Giúp Kỹ Thuật (Modal)</span>
          </button>
        </header>

        {/* TROUBLESHOOTING MODAL */}
        {isTroubleshootOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-neutral-950/75 animate-in fade-in duration-150">
            <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 text-slate-800 animate-modal-entry">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <h3 className="font-bold text-base text-slate-900">Trợ Giúp Xử Lý Thiết Bị</h3>
                <button
                  type="button"
                  onClick={() => setIsTroubleshootOpen(false)}
                  className="text-slate-400 hover:text-slate-700 text-xl font-bold p-1 cursor-pointer focus:outline-none"
                >
                  &times;
                </button>
              </div>

              <div className="space-y-3 text-xs text-slate-600 leading-relaxed font-normal">
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                  <p className="font-bold text-slate-900 mb-0.5">1. Không nghe thấy tiếng?</p>
                  <p>Kiểm tra nút âm lượng của máy tính hoặc tai nghe đã cắm chặt jack 3.5mm / Bluetooth.</p>
                </div>
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                  <p className="font-bold text-slate-900 mb-0.5">2. Trình duyệt chặn quyền truy cập?</p>
                  <p>Nhấp vào biểu tượng ổ khóa bên trái thanh địa chỉ URL để cấp quyền Microphone và Camera.</p>
                </div>
              </div>

              <div className="pt-2 text-right">
                <button
                  type="button"
                  onClick={() => setIsTroubleshootOpen(false)}
                  className="px-5 py-2 rounded-xl bg-[#0F172A] hover:bg-slate-800 text-white font-bold text-xs shadow-sm transition active:scale-95 cursor-pointer"
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
    <header className="bg-sidebarBg text-white border-b border-slate-700/80 sticky top-0 z-40 shadow-md">
      <div className="w-full px-4 sm:px-6 h-16 flex items-center justify-between">

        {/* Left: Brand & Nav Links */}
        <div className="flex items-center gap-6">
          <div
            onClick={() => onNavigate(isAdmin ? 'admin-users' : isLecturer ? 'lecturer-dashboard' : 'discover')}
            className="flex items-center gap-3 cursor-pointer group select-none"
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && onNavigate(isAdmin ? 'admin-users' : isLecturer ? 'lecturer-dashboard' : 'discover')}
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

          {/* Desktop Navigation Tabs */}
          <nav className="hidden lg:flex items-center gap-1.5 text-xs">
            {isAdmin && (
              <>
                <button
                  onClick={() => onNavigate('admin-users')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border ${currentTab === 'admin-users'
                      ? 'bg-sky-500/20 text-sky-300 border-sky-400/30 shadow-xs'
                      : 'text-slate-300 hover:text-white hover:bg-sidebarHover border-transparent'
                    }`}
                >
                  Phân Quyền (RBAC)
                </button>
                <button
                  onClick={() => onNavigate('admin-voice-lab')}
                  className={`px-3 py-1.5 rounded-xl font-bold transition-all border ${currentTab === 'admin-voice-lab' || currentTab === 'voice-lab'
                      ? 'bg-sky-500/20 text-sky-300 border-sky-400/30 shadow-xs'
                      : 'text-slate-300 hover:text-white hover:bg-sidebarHover border-transparent'
                    }`}
                >
                  Voice &amp; Speech Lab
                </button>
              </>
            )}
          </nav>
        </div>

        {/* Right: Actions, AI Engine Status, Mode Switch, Profile Dropdown */}
        <div className="flex items-center gap-3">

          {/* Real-time Voice Audio Activity (GSAP 60 FPS) */}
          <div className="hidden xl:flex items-center gap-2 px-3 py-1 rounded-xl bg-slate-900/90 border border-slate-700/80 text-[11px]">
            <Mic2 className="w-3.5 h-3.5 text-sky-400" />
            <span className="text-slate-400 text-[10px] font-mono">Speech Engine:</span>
            <VoiceWaveform isSpeaking={true} volume={0.45} barCount={6} height={14} barWidth="w-0.5" barGap="gap-0.5" />
            <span className="text-[10px] text-emerald-400 font-bold font-mono">Ready</span>
          </div>

          {/* AI Assistant */}
          <button
            onClick={onOpenAssistant}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 transition-all active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-400 cursor-pointer"
          >
            <Bot className="w-4 h-4 text-sky-400" />
            <span className="hidden sm:inline">Trợ Lý AI</span>
          </button>

          {/* Quick Action: Thêm Người Dùng */}
          {isAdmin && (
            <button
              onClick={onOpenAddUser}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold shadow-md shadow-sky-600/20 transition-all active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-400 cursor-pointer"
            >
              <UserPlus className="w-4 h-4" />
              <span className="hidden sm:inline">Thêm Người Dùng</span>
            </button>
          )}

          <div className="h-6 w-px bg-slate-700 hidden sm:block"></div>

          {/* User Profile Pill & Dropdown */}
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setDropdownOpen(!dropdownOpen)}
              className="flex items-center gap-2.5 p-1.5 pl-2 pr-3 rounded-full bg-slate-900/90 hover:bg-slate-800 border border-slate-700/80 hover:border-slate-600 transition-all focus:outline-none focus:ring-2 focus:ring-sky-500 shadow-2xs group cursor-pointer"
              aria-expanded={dropdownOpen}
              aria-haspopup="true"
            >
              <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 border border-blue-400/40 text-white font-heading font-extrabold text-xs flex items-center justify-center shadow-xs shrink-0">
                {getInitials(currentUser?.fullName)}
              </div>
              <div className="text-left leading-tight">
                <p className="font-heading font-bold text-xs text-white group-hover:text-sky-300 transition truncate max-w-[140px]">
                  {currentUser?.fullName || 'Người Dùng AIVES'}
                </p>
                <p className="text-[10px] font-semibold text-slate-400 mt-0.5 tracking-wide">
                  {isAdmin ? 'Ban Khảo Thí' : isLecturer ? 'Giảng Viên' : 'Sinh Viên'}
                </p>
              </div>
              <ChevronDown className={`w-3.5 h-3.5 text-slate-400 group-hover:text-white ml-0.5 transition-transform ${dropdownOpen ? 'rotate-180' : ''}`} />
            </button>

            {/* Dropdown Menu */}
            {dropdownOpen && (
              <div className="absolute right-0 mt-2 w-60 bg-white rounded-2xl shadow-2xl border border-slate-200 text-slate-800 py-2 z-50 animate-modal-entry">
                <div className="px-4 py-2.5 border-b border-slate-100">
                  <p className="text-xs font-bold text-slate-900 truncate">{currentUser?.fullName}</p>
                  <p className="text-[11px] text-slate-500 font-mono truncate">{currentUser?.email}</p>
                  <span className="inline-block mt-1 text-[10px] font-mono px-2 py-0.5 rounded bg-sky-50 text-sky-700 font-bold border border-sky-200">
                    {currentUser?.userCode || 'USR-CODE'}
                  </span>
                </div>

                <div className="py-1 text-xs">
                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      onOpenProfile('info');
                    }}
                    className="w-full px-4 py-2 text-left hover:bg-slate-50 flex items-center gap-2.5 text-slate-700 font-medium transition"
                  >
                    <User className="w-4 h-4 text-slate-500" />
                    <span>Hồ Sơ Của Tôi</span>
                  </button>

                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      onOpenProfile('password');
                    }}
                    className="w-full px-4 py-2 text-left hover:bg-slate-50 flex items-center gap-2.5 text-slate-700 font-medium transition"
                  >
                    <Key className="w-4 h-4 text-slate-500" />
                    <span>Đổi Mật Khẩu</span>
                  </button>
                </div>

                <div className="border-t border-slate-100 pt-1 text-xs">
                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      logout();
                      onNavigate('login');
                    }}
                    className="w-full px-4 py-2 text-left hover:bg-rose-50 flex items-center gap-2.5 text-rose-600 font-medium transition"
                  >
                    <LogOut className="w-4 h-4 text-rose-500" />
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
