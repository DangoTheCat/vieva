import React from 'react';
import {
  Users,
  Activity,
  Mic2,
  Table2,
  ShieldCheck,
  GraduationCap,
  BookOpen,
  ToggleLeft
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export function Sidebar({ currentTab, onNavigate, onOpenMatrix }) {
  const { isDemoMode, setDemoRole, currentUser } = useAuth();

  return (
    <aside className="w-64 shrink-0 hidden md:block bg-sidebarBg text-slate-300 p-4 border-r border-slate-800 select-none">
      <div className="space-y-6 sticky top-20">

        {/* Policy Status Card */}
        <div className="p-3.5 bg-slate-900/90 rounded-2xl border border-slate-700/80 shadow-xs">
          <div className="flex items-center gap-2 mb-1">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-xs font-bold text-white tracking-wide">RBAC Policy: Enforced</span>
          </div>
          <p className="text-[11px] text-slate-400">PostgreSQL Course-scoped RLS</p>

          <button
            type="button"
            onClick={onOpenMatrix}
            className="mt-3 w-full py-2 px-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-[11px] font-bold flex items-center justify-center gap-1.5 transition-all border border-slate-700 active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
            <Table2 className="w-3.5 h-3.5 text-sky-400" />
            <span>Xem Ma Trận Quyền</span>
          </button>
        </div>

        {/* Navigation Menu */}
        <nav className="space-y-1 text-xs">
          <button
            type="button"
            onClick={() => onNavigate('admin-users')}
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl font-bold transition-all border text-left focus:outline-none focus:ring-2 focus:ring-sky-500 ${currentTab === 'admin-users'
                ? 'bg-sky-500/20 text-sky-300 border-sky-400/30'
                : 'text-slate-300 hover:bg-sidebarHover hover:text-white border-transparent'
              }`}
          >
            <Users className="w-4 h-4 text-sky-400" />
            <span>Phân Quyền RBAC</span>
          </button>

          <button
            type="button"
            onClick={() => onNavigate('telemetry')}
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl font-medium transition-all border text-left focus:outline-none focus:ring-2 focus:ring-sky-500 ${currentTab === 'telemetry'
                ? 'bg-sky-500/20 text-sky-300 border-sky-400/30 font-bold'
                : 'text-slate-400 hover:bg-sidebarHover hover:text-white border-transparent'
              }`}
          >
            <Activity className="w-4 h-4 text-slate-400" />
            <span>Live Telemetry Hub</span>
          </button>

          <button
            type="button"
            onClick={() => onNavigate('voice-lab')}
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl font-medium transition-all border text-left focus:outline-none focus:ring-2 focus:ring-sky-500 ${currentTab === 'voice-lab'
                ? 'bg-sky-500/20 text-sky-300 border-sky-400/30 font-bold'
                : 'text-slate-400 hover:bg-sidebarHover hover:text-white border-transparent'
              }`}
          >
            <Mic2 className="w-4 h-4 text-slate-400" />
            <span>Voice &amp; Speech Lab</span>
          </button>
        </nav>

        {/* Fast Role Simulator (For testing RBAC in Demo Mode) */}
        {isDemoMode && (
          <div className="pt-4 border-t border-slate-800 space-y-2">
            <div className="flex items-center justify-between text-[11px] text-slate-400 px-1">
              <span className="font-semibold uppercase tracking-wider text-[10px]">Giả Lập Vai Trò</span>
              <span className="text-amber-400 font-mono text-[10px]">Demo Switch</span>
            </div>

            <div className="grid grid-cols-2 gap-1.5 text-[11px]">
              <button
                type="button"
                onClick={() => setDemoRole('ROLE_ADMIN')}
                className={`py-1.5 px-2 rounded-xl text-center font-bold border transition ${currentUser?.roles?.includes('ROLE_ADMIN')
                    ? 'bg-purple-900/40 text-purple-300 border-purple-500/50 shadow-xs'
                    : 'bg-slate-800/60 text-slate-400 border-slate-700 hover:text-white'
                  }`}
              >
                Admin
              </button>
              <button
                type="button"
                onClick={() => setDemoRole('ROLE_USER')}
                className={`py-1.5 px-2 rounded-xl text-center font-bold border transition ${!currentUser?.roles?.includes('ROLE_ADMIN')
                    ? 'bg-sky-900/40 text-sky-300 border-sky-500/50 shadow-xs'
                    : 'bg-slate-800/60 text-slate-400 border-slate-700 hover:text-white'
                  }`}
              >
                Standard User
              </button>
            </div>
          </div>
        )}

      </div>
    </aside>
  );
}
