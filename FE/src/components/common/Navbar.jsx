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

  return (
    <header className="bg-sidebarBg text-white border-b border-slate-700/80 sticky top-0 z-40 shadow-md">
      <div className="max-w-[1440px] mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        
        {/* Left: Brand & Nav Links */}
        <div className="flex items-center gap-6">
          <div 
            onClick={() => onNavigate('admin-users')}
            className="flex items-center gap-3 cursor-pointer group select-none"
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && onNavigate('admin-users')}
          >
            <div className="w-10 h-10 rounded-xl bg-sky-600/90 border border-sky-400/40 flex items-center justify-center text-white shadow-md shadow-sky-900/30 group-hover:scale-105 transition-transform">
              <ShieldCheck className="w-6 h-6 text-sky-200" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-heading font-extrabold text-lg text-white tracking-tight">
                  AIVES Admin Center
                </span>
                <span className="bg-sky-500/20 text-sky-300 text-[10px] font-mono px-2 py-0.5 rounded border border-sky-400/30">
                  RBAC
                </span>
              </div>
              <p className="text-[11px] text-slate-400">Course-Scoped Role Based Access Control</p>
            </div>
          </div>

          {/* Desktop Navigation Tabs */}
          {isAdmin && (
          <nav className="hidden lg:flex items-center gap-1.5 text-xs">
            <button
              onClick={() => onNavigate('admin-users')}
              className={`px-3 py-1.5 rounded-xl font-bold transition-all border ${
                currentTab === 'admin-users'
                  ? 'bg-sky-500/20 text-sky-300 border-sky-400/30 shadow-xs'
                  : 'text-slate-300 hover:text-white hover:bg-sidebarHover border-transparent'
              }`}
            >
              Phân Quyền (RBAC)
            </button>
            <button
              onClick={onOpenMatrix}
              className="px-3 py-1.5 rounded-xl text-slate-300 hover:text-white hover:bg-sidebarHover border border-transparent transition-colors"
            >
              Ma Trận Quyền
            </button>
          </nav>
          )}
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

          {/* Backend Connection Status Badge */}
          <div className="hidden md:flex items-center gap-2 px-3 py-1 rounded-xl bg-slate-900/90 border border-slate-700/80 text-[11px]">
            {isDemoMode ? (
              <span className="flex items-center gap-1.5 text-amber-400 font-medium">
                <span className="w-2 h-2 rounded-full bg-amber-400"></span>
                <span className="font-semibold">Demo Mode</span>
              </span>
            ) : isLiveBackendReachable ? (
              <span className="flex items-center gap-1.5 text-emerald-400 font-medium">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                <span className="font-semibold">BE Live (8080)</span>
              </span>
            ) : (
              <span className="flex items-center gap-1.5 text-rose-400 font-medium">
                <span className="w-2 h-2 rounded-full bg-rose-400"></span>
                <span className="font-semibold">BE Offline</span>
              </span>
            )}

            <button
              onClick={() => setIsDemoMode(!isDemoMode)}
              className="ml-1 text-[10px] font-medium text-slate-400 hover:text-white underline transition"
              title="Nhấn để chuyển đổi chế độ Live BE / Demo Mode"
            >
              {isDemoMode ? 'Bật Live BE' : 'Bật Demo'}
            </button>
          </div>

          {/* AI Assistant */}
          <button
            onClick={onOpenAssistant}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 transition-all active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-400"
          >
            <Bot className="w-4 h-4 text-sky-400" />
            <span className="hidden sm:inline">Trợ Lý AI</span>
          </button>

          {/* Quick Action: Thêm Người Dùng */}
          {isAdmin && (
          <button
            onClick={onOpenAddUser}
            className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold shadow-md shadow-sky-600/20 transition-all active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-400"
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
              className="flex items-center gap-2 p-1 pl-1.5 pr-2.5 rounded-xl hover:bg-slate-800/80 border border-slate-700/80 transition-colors focus:outline-none focus:ring-2 focus:ring-sky-500"
              aria-expanded={dropdownOpen}
              aria-haspopup="true"
            >
              <div className="w-7 h-7 rounded-lg bg-purple-500/20 border border-purple-400/40 text-purple-300 font-bold text-xs flex items-center justify-center">
                {getInitials(currentUser?.fullName)}
              </div>
              <div className="hidden xl:block text-left text-xs">
                <p className="font-bold text-white leading-none truncate max-w-[130px]">
                  {currentUser?.fullName || 'SysAdmin Center'}
                </p>
                <p className="text-[10px] text-slate-400 font-mono mt-0.5">
                  {isAdmin ? 'Super Administrator' : isLecturer ? 'Lecturer' : 'Standard User'}
                </p>
              </div>
              <ChevronDown className={`w-3.5 h-3.5 text-slate-400 ml-0.5 transition-transform ${dropdownOpen ? 'rotate-180' : ''}`} />
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
