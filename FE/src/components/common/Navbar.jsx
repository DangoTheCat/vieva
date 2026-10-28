import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import {
  ShieldCheck,
  UserPlus,
  User,
  LogOut,
  ChevronDown,
  Table2,
  GraduationCap,
  HelpCircle,
  X
} from 'lucide-react';
import { NotificationBell } from './NotificationBell';

export function Navbar({ onOpenAddUser, onOpenProfile, onOpenMatrix, onNavigate, currentTab }) {
  const { currentUser, logout, isLiveBackendReachable, isAdmin, isLecturer } = useAuth();
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

  // If on calibration view, render the Hiệu Chuẩn Thiết Bị AIVES Header
  if (currentTab === 'calibration') {
    return (
      <>
        <header className="h-16 bg-white text-ink px-6 flex items-center justify-between shrink-0 shadow-2xs border-b border-pink-200 sticky top-0 z-40 select-none">
          <div className="flex items-center gap-3">
            <button 
              type="button"
              onClick={() => onNavigate && onNavigate('discover')}
              className="w-8 h-8 rounded-lg bg-pink-50 hover:bg-pink-100 border border-pink-200 flex items-center justify-center text-pink-700 transition cursor-pointer outline-none active:scale-95"
              aria-label="Về trang chủ"
            >
              <GraduationCap className="w-4 h-4 text-ink" />
            </button>
            <div>
              <h1 className="font-heading font-bold text-sm text-ink tracking-tight">
                AIVES Calibration Wizard
              </h1>
              <p className="text-[11px] text-slate-500 font-medium">
                Bước chuẩn bị bắt buộc trước khi vào phòng thi chính thức
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setIsTroubleshootOpen(true)}
            className="text-xs font-semibold text-pink-800 hover:text-pink-900 bg-pink-50 hover:bg-pink-100 px-3.5 py-1.5 rounded-lg border border-pink-200 transition flex items-center gap-1.5 cursor-pointer active:scale-95"
          >
            <HelpCircle className="w-3.5 h-3.5 text-slate-600" />
            <span>Trợ Giúp Kỹ Thuật</span>
          </button>
        </header>

        {/* TROUBLESHOOTING MODAL */}
        {isTroubleshootOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-pink-600/60 backdrop-blur-xs animate-in fade-in duration-150">
            <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-pink-200 space-y-4 text-ink animate-modal-entry">
              <div className="flex items-center justify-between pb-3 border-b border-pink-100">
                <h3 className="font-heading font-bold text-base text-ink">Trợ Giúp Xử Lý Thiết Bị</h3>
                <button
                  type="button"
                  onClick={() => setIsTroubleshootOpen(false)}
                  className="text-slate-400 hover:text-ink p-1 rounded-lg transition cursor-pointer focus:outline-none"
                  aria-label="Đóng"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-3 text-xs text-slate-600 leading-relaxed font-normal">
                <div className="p-3 bg-slate-50 rounded-xl border border-pink-200">
                  <p className="font-semibold text-ink mb-0.5">1. Không nghe thấy tiếng?</p>
                  <p>Kiểm tra nút âm lượng của máy tính hoặc tai nghe đã cắm chặt jack 3.5mm / Bluetooth.</p>
                </div>
                <div className="p-3 bg-slate-50 rounded-xl border border-pink-200">
                  <p className="font-semibold text-ink mb-0.5">2. Trình duyệt chặn quyền truy cập?</p>
                  <p>Nhấp vào biểu tượng ổ khóa bên trái thanh địa chỉ URL để cấp quyền Microphone và Camera.</p>
                </div>
              </div>

              <div className="pt-2 text-right">
                <button
                  type="button"
                  onClick={() => setIsTroubleshootOpen(false)}
                  className="px-5 py-2.5 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-95 cursor-pointer"
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
    <header className="bg-white text-ink border-b border-pink-200 sticky top-0 z-40 shadow-2xs">
      <div className="w-full px-4 sm:px-6 h-16 flex items-center justify-between">

        {/* Left: Brand & Context Title (No duplicate navigation tabs) */}
        <div className="flex items-center gap-4">
          <div
            onClick={() => onNavigate(isAdmin ? 'admin-users' : isLecturer ? 'lecturer-dashboard' : 'discover')}
            className="flex items-center gap-2.5 cursor-pointer group select-none"
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && onNavigate(isAdmin ? 'admin-users' : isLecturer ? 'lecturer-dashboard' : 'discover')}
          >
            <div className="w-8 h-8 rounded-lg bg-pink-700 text-white flex items-center justify-center shadow-xs group-hover:scale-105 transition-transform">
              <svg className="w-4 h-4 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </div>
            <span className="font-heading font-black text-xl tracking-tight text-ink">
              aives<span className="text-pink-600">.</span>
            </span>
          </div>

          <div className="hidden sm:flex items-center gap-2 pl-4 border-l border-pink-200">
            <span className="text-xs font-medium text-slate-500">
              Hệ thống Khảo thí Vấn đáp AI • FPT University
            </span>
          </div>
        </div>

        {/* Right: Actions, AI Engine Status, Profile Dropdown */}
        <div className="flex items-center gap-3">



          {/* Quick Action: Thêm Người Dùng */}
          {isAdmin && (
            <button
              onClick={onOpenAddUser}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-pink-600 hover:bg-pink-700 text-white text-xs font-semibold shadow-xs transition active:scale-[0.98] cursor-pointer"
            >
              <UserPlus className="w-3.5 h-3.5 text-white" />
              <span className="hidden sm:inline">Thêm Người Dùng</span>
            </button>
          )}

          <NotificationBell />

          <div className="h-5 w-px bg-pink-200 hidden sm:block"></div>

          {/* User Profile Pill & Dropdown */}
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setDropdownOpen(!dropdownOpen)}
              className={`flex items-center gap-2.5 p-1 pl-1.5 pr-3 rounded-full border transition-all cursor-pointer focus:outline-none ${
                currentTab === 'profile' || currentTab === 'profile-password'
                  ? 'bg-slate-100 border-pink-200 ring-2 ring-slate-200'
                  : 'bg-white hover:bg-pink-50 border-pink-200 hover:border-pink-300 shadow-2xs'
              }`}
              aria-expanded={dropdownOpen}
              aria-haspopup="true"
            >
              <div className="w-7 h-7 rounded-full bg-pink-200 text-pink-900 font-heading font-bold text-xs flex items-center justify-center shrink-0 overflow-hidden">
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
                <p className="font-heading font-bold text-xs text-ink transition truncate max-w-[130px]">
                  {currentUser?.fullName || 'Người Dùng AIVES'}
                </p>
                <p className="text-[10px] font-semibold text-pink-700 font-semibold tracking-tight">
                  {isAdmin ? 'Ban Khảo Thí' : isLecturer ? 'Giảng Viên' : 'Sinh Viên'}
                </p>
              </div>
              <ChevronDown className={`w-3.5 h-3.5 text-pink-600 ml-0.5 transition-transform ${dropdownOpen ? 'rotate-180' : ''}`} />
            </button>

            {/* Dropdown Menu */}
            {dropdownOpen && (
              <div className="absolute right-0 mt-2 w-64 bg-white rounded-xl shadow-lg border border-pink-200 text-slate-800 py-1.5 z-50 animate-modal-entry">
                <div className="px-3.5 py-2.5 border-b border-pink-100 bg-pink-50/70 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-slate-200 text-slate-800 font-heading font-bold text-xs flex items-center justify-center shrink-0 overflow-hidden">
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
                    <p className="text-xs font-semibold text-ink truncate">{currentUser?.fullName}</p>
                    <p className="text-[11px] text-slate-500 font-mono truncate">{currentUser?.email}</p>
                    <span className="inline-block mt-0.5 text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-200/80 text-ink font-semibold">
                      {currentUser?.userCode || 'USR-CODE'}
                    </span>
                  </div>
                </div>

                <div className="py-1 text-xs">
                  <button
                    type="button"
                    onClick={() => {
                      setDropdownOpen(false);
                      if (onOpenProfile) {
                        onOpenProfile();
                      } else if (onNavigate) {
                        onNavigate('profile');
                      }
                    }}
                    className={`w-full px-3.5 py-2 text-left hover:bg-pink-50 flex items-center gap-2.5 font-medium transition cursor-pointer ${
                      currentTab === 'profile' || currentTab === 'profile-password' ? 'bg-pink-100 text-pink-900 font-semibold' : 'text-ink'
                    }`}
                  >
                    <User className="w-4 h-4 text-slate-500" />
                    <span>Hồ Sơ Của Tôi</span>
                  </button>
                </div>

                <div className="border-t border-pink-100 pt-1 text-xs mt-0.5">
                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      logout();
                      onNavigate('login');
                    }}
                    className="w-full px-3.5 py-2 text-left hover:bg-rose-50 flex items-center gap-2.5 text-rose-600 font-semibold transition cursor-pointer"
                  >
                    <LogOut className="w-4 h-4 text-rose-600" />
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
