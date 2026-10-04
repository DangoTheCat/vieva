import React from 'react';
import { 
  Users, 
  Database, 
  Table2, 
  ShieldCheck, 
  GraduationCap, 
  BookOpen, 
  ToggleLeft 
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export function Sidebar({ currentTab, onNavigate, onOpenMatrix }) {
  const { isAdmin, isLecturer } = useAuth();

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
          {isAdmin && (
            <>
              <button
                type="button"
                onClick={() => onNavigate('admin-users')}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl font-bold transition-all border text-left focus:outline-none focus:ring-2 focus:ring-sky-500 ${
                  currentTab === 'admin-users'
                    ? 'bg-sky-500/20 text-sky-300 border-sky-400/30'
                    : 'text-slate-300 hover:bg-sidebarHover hover:text-white border-transparent'
                }`}
              >
                <Users className="w-4 h-4 text-sky-400" />
                <span>Phân Quyền Người Dùng</span>
              </button>

              <button
                type="button"
                onClick={() => onNavigate('admin-subjects')}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl font-bold transition-all border text-left focus:outline-none focus:ring-2 focus:ring-sky-500 ${
                  currentTab === 'admin-subjects'
                    ? 'bg-sky-500/20 text-sky-300 border-sky-400/30'
                    : 'text-slate-300 hover:bg-sidebarHover hover:text-white border-transparent'
                }`}
              >
                <BookOpen className="w-4 h-4 text-sky-400" />
                <span>Quản Lý Môn Học</span>
              </button>
            </>
          )}

          {(isLecturer || isAdmin) && (
            <>
              <button
                type="button"
                onClick={() => onNavigate('lecturer-questions')}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl font-bold transition-all border text-left focus:outline-none focus:ring-2 focus:ring-sky-500 ${
                  currentTab === 'lecturer-questions'
                    ? 'bg-sky-500/20 text-sky-300 border-sky-400/30'
                    : 'text-slate-300 hover:bg-sidebarHover hover:text-white border-transparent'
                }`}
              >
                <Database className="w-4 h-4 text-emerald-400" />
                <span>Ngân Hàng Câu Hỏi &amp; RAG</span>
              </button>
            </>
          )}
          {/* Telemetry & Voice Lab hidden until BE exposes WebSocket/speech endpoints */}
        </nav>

      </div>
    </aside>
  );
}
