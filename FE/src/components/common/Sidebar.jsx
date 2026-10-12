import React, { useState, useEffect } from 'react';
import {
  Users,
  Database,
  Table2,
  ShieldCheck,
  GraduationCap,
  BookOpen,
  Sliders,
  Calendar,
  FileText,
  X,
  Award,
  Mic,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  PanelLeftClose,
  PanelLeftOpen,
  Activity,
  Sparkles,
  Cpu,
  Layers,
  Settings2,
  Check
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export function Sidebar({ currentTab, onNavigate, onOpenMatrix }) {
  const { isAdmin, isLecturer, currentUser, isLiveBackendReachable } = useAuth();
  const isStudent = !isAdmin && !isLecturer;
  const [isRulesModalOpen, setIsRulesModalOpen] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(() => {
    try {
      return localStorage.getItem('aives_sidebar_collapsed') === 'true';
    } catch {
      return false;
    }
  });

  // Admin Segmented Filter: 'ALL' | 'ADMIN' | 'LECTURER'
  const [adminFilter, setAdminFilter] = useState('ALL');

  // Intelligent filter auto-sync if user navigates to a tab from another group
  useEffect(() => {
    if (!isAdmin) return;
    const adminTabs = ['admin-users', 'admin-subjects', 'admin-voice-lab', 'voice-lab'];
    const lecturerTabs = ['lecturer-questions', 'lecturer-docs-rag', 'rubric-studio'];

    if (adminFilter === 'ADMIN' && lecturerTabs.includes(currentTab)) {
      setAdminFilter('LECTURER');
    } else if (adminFilter === 'LECTURER' && adminTabs.includes(currentTab)) {
      setAdminFilter('ADMIN');
    }
  }, [currentTab, isAdmin, adminFilter]);

  const toggleCollapse = () => {
    setIsCollapsed(prev => {
      const next = !prev;
      try {
        localStorage.setItem('aives_sidebar_collapsed', String(next));
      } catch {}
      return next;
    });
  };

  const getInitials = (name) => {
    if (!name) return 'US';
    return name
      .split(' ')
      .filter(Boolean)
      .map(part => part[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  };

  // Nav Item Component
  const renderItem = ({ id, title, subtitle, tag, icon: Icon, activeWhen }) => {
    const isActive = !isRulesModalOpen && (activeWhen ? activeWhen(currentTab) : currentTab === id);

    return (
      <div key={id} className="relative group">
        <button
          type="button"
          onClick={() => onNavigate(id)}
          className={`w-full relative flex items-center ${isCollapsed ? 'justify-center px-0' : 'px-3 gap-3'} py-2.5 rounded-xl transition-all duration-150 text-left cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 border ${
            isActive
              ? 'bg-sky-500/15 text-white border-sky-400/40 shadow-xs'
              : 'text-slate-300 border-transparent hover:text-white hover:bg-slate-800/60'
          }`}
          title={isCollapsed ? `${title} • ${subtitle}` : undefined}
        >
          {/* Active Accent Bar on Left */}
          {isActive && (
            <span className="absolute left-0 top-2 bottom-2 w-1 bg-sky-400 rounded-r-full shadow-xs shadow-sky-400" />
          )}

          {/* Icon Badge */}
          <div
            className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 transition-colors ${
              isActive
                ? 'bg-sky-500/25 text-sky-400 border border-sky-400/30'
                : 'bg-slate-800/80 text-slate-400 group-hover:text-slate-200 group-hover:bg-slate-700/80 border border-slate-700/50'
            }`}
          >
            <Icon className="w-4 h-4" />
          </div>

          {/* Labels & Tags (Only shown when expanded) */}
          {!isCollapsed && (
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between gap-1.5">
                <span className={`text-xs font-semibold truncate ${isActive ? 'text-white' : 'text-slate-200 group-hover:text-white'}`}>
                  {title}
                </span>
                {tag && (
                  <span
                    className={`text-[9px] font-mono px-1.5 py-0.5 rounded tracking-wider shrink-0 whitespace-nowrap ${
                      isActive
                        ? 'bg-sky-950 text-sky-300 border border-sky-700/50'
                        : 'bg-slate-800/90 text-slate-400 border border-slate-700/60'
                    }`}
                  >
                    {tag}
                  </span>
                )}
              </div>
              <p className="text-[10px] text-slate-400 truncate mt-0.5 font-normal">
                {subtitle}
              </p>
            </div>
          )}
        </button>

        {/* Floating Tooltip in Collapsed Mode */}
        {isCollapsed && (
          <div className="absolute left-full top-1/2 -translate-y-1/2 ml-3 hidden group-hover:flex flex-col z-50 bg-slate-900 border border-slate-700 text-white px-3 py-2 rounded-xl shadow-2xl whitespace-nowrap pointer-events-none animate-in fade-in zoom-in-95 duration-100">
            <span className="text-xs font-bold text-white flex items-center gap-1.5">
              {title}
              {tag && (
                <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-sky-950 text-sky-400 border border-sky-800">
                  {tag}
                </span>
              )}
            </span>
            <span className="text-[10px] text-slate-400 mt-0.5">{subtitle}</span>
          </div>
        )}
      </div>
    );
  };

  return (
    <>
      <aside
        className={`${
          isCollapsed ? 'w-20' : 'w-72'
        } shrink-0 hidden md:flex flex-col justify-between bg-sidebarBg text-slate-300 border-r border-slate-800 select-none transition-all duration-200 min-h-[calc(100vh-4rem)] p-3`}
      >
        {/* TOP SECTION: CONTEXT BANNER & NAVIGATION */}
        <div className="space-y-4">

          {/* Header & Toggle Button */}
          <div className="flex items-center justify-between px-2 pt-1 pb-2 border-b border-slate-800/80">
            {!isCollapsed ? (
              <div className="flex items-center gap-2 min-w-0">
                <div className="w-2 h-2 rounded-full bg-sky-400 animate-pulse shrink-0" />
                <span className="text-[11px] font-extrabold uppercase tracking-wider text-slate-300 truncate font-heading">
                  {isAdmin ? 'Quản Trị Trung Tâm' : isLecturer ? 'Phân Hệ Giảng Viên' : 'Cổng Thi Vấn Đáp'}
                </span>
              </div>
            ) : (
              <div className="w-full flex justify-center pb-1">
                <div className="w-2 h-2 rounded-full bg-sky-400 animate-pulse" />
              </div>
            )}

            <button
              type="button"
              onClick={toggleCollapse}
              className="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-slate-800/80 transition cursor-pointer focus:outline-none focus:ring-1 focus:ring-sky-500"
              title={isCollapsed ? 'Mở rộng thanh menu' : 'Thu gọn thanh menu'}
              aria-label={isCollapsed ? 'Mở rộng menu' : 'Thu gọn menu'}
            >
              {isCollapsed ? <PanelLeftOpen className="w-4 h-4" /> : <PanelLeftClose className="w-4 h-4" />}
            </button>
          </div>

          {/* Admin Segmented Filter (Only when expanded) */}
          {isAdmin && !isCollapsed && (
            <div className="flex items-center p-1 bg-slate-900/90 rounded-xl border border-slate-800 text-[11px] select-none gap-0.5">
              <button
                type="button"
                onClick={() => setAdminFilter('ALL')}
                className={`flex-1 py-1.5 rounded-lg font-bold transition text-center cursor-pointer ${
                  adminFilter === 'ALL'
                    ? 'bg-sky-500/20 text-sky-300 border border-sky-400/30 shadow-xs'
                    : 'text-slate-400 hover:text-white border border-transparent'
                }`}
              >
                Tất Cả (6)
              </button>
              <button
                type="button"
                onClick={() => setAdminFilter('ADMIN')}
                className={`flex-1 py-1.5 rounded-lg font-bold transition text-center cursor-pointer ${
                  adminFilter === 'ADMIN'
                    ? 'bg-sky-500/20 text-sky-300 border border-sky-400/30 shadow-xs'
                    : 'text-slate-400 hover:text-white border border-transparent'
                }`}
              >
                Quản Trị (3)
              </button>
              <button
                type="button"
                onClick={() => setAdminFilter('LECTURER')}
                className={`flex-1 py-1.5 rounded-lg font-bold transition text-center cursor-pointer ${
                  adminFilter === 'LECTURER'
                    ? 'bg-sky-500/20 text-sky-300 border border-sky-400/30 shadow-xs'
                    : 'text-slate-400 hover:text-white border border-transparent'
                }`}
              >
                Đề &amp; Rubric (3)
              </button>
            </div>
          )}

          {/* NAVIGATION LINKS */}
          <nav className="space-y-1.5 text-xs">

            {/* STUDENT PORTAL LINKS - NHÓM 3: LÕI PHỎNG VẤN AI */}
            {isStudent && (
              <>
                {!isCollapsed && (
                  <div className="pt-1 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 px-3 flex items-center justify-between">
                    <span>Nhóm 3: Lõi Phỏng Vấn AI</span>
                    <span className="text-cyan-400 font-mono text-[9px] bg-cyan-950/80 px-1.5 py-0.5 rounded border border-cyan-800/50">Core</span>
                  </div>
                )}

                {renderItem({
                  id: 'discover',
                  title: 'Lịch Thi Vấn Đáp',
                  subtitle: 'Danh sách ca thi được chỉ định',
                  tag: 'Lịch thi',
                  icon: Calendar,
                  activeWhen: (tab) => tab === 'discover' || tab === 'home'
                })}

                {renderItem({
                  id: 'calibration',
                  title: 'Kiểm Tra Thiết Bị',
                  subtitle: 'Đo micro, loa & xem trước camera',
                  tag: 'Mic/Cam',
                  icon: Sliders
                })}

                {renderItem({
                  id: 'exam-room',
                  title: 'Phòng Thi Vấn Đáp AI',
                  subtitle: 'Giám khảo AI & hỏi xoáy thích ứng',
                  tag: 'Viva Core',
                  icon: Mic
                })}

                {renderItem({
                  id: 'submission-success',
                  title: 'Biên Bản Bài Thi',
                  subtitle: 'Chứng thực băm SHA-256 kết quả',
                  tag: 'Biên bản',
                  icon: CheckCircle2
                })}
              </>
            )}

            {/* ADMIN MODULE SECTION - NHÓM 7: QUẢN TRỊ HỆ THỐNG */}
            {isAdmin && (adminFilter === 'ALL' || adminFilter === 'ADMIN') && (
              <>
                {!isCollapsed && (
                  <div className="pt-2 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 px-3 flex items-center justify-between">
                    <span className="truncate">Nhóm 7: Quản Trị Hệ Thống</span>
                    <span className="text-sky-400 font-mono text-[9px] bg-sky-950/80 px-1.5 py-0.5 rounded border border-sky-800/50 shrink-0">Admin</span>
                  </div>
                )}

                {renderItem({
                  id: 'admin-users',
                  title: 'Quản Lý Tài Khoản',
                  subtitle: 'Phân quyền người dùng & khóa tài khoản',
                  tag: 'RBAC',
                  icon: Users
                })}

                {renderItem({
                  id: 'admin-subjects',
                  title: 'Môn Học & Phân Công',
                  subtitle: 'Phân quyền giảng viên phụ trách môn',
                  tag: 'Phân công',
                  icon: BookOpen
                })}

                {renderItem({
                  id: 'admin-voice-lab',
                  title: 'Cấu Hình STT / TTS',
                  subtitle: 'Giọng đọc AI & nhận diện Việt/Anh',
                  tag: 'Voice Lab',
                  icon: Mic,
                  activeWhen: (tab) => tab === 'admin-voice-lab' || tab === 'voice-lab'
                })}
              </>
            )}

            {/* LECTURER SECTION - NHÓM 1: QUẢN LÝ NGÂN HÀNG CÂU HỎI & RUBRIC */}
            {(isLecturer || (isAdmin && (adminFilter === 'ALL' || adminFilter === 'LECTURER'))) && (
              <>
                {!isCollapsed && (
                  <div className="pt-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 px-3 flex items-center justify-between">
                    <span className="truncate">Nhóm 1: Câu Hỏi &amp; Rubric</span>
                    <span className="text-sky-400 font-mono text-[9px] bg-sky-950/80 px-1.5 py-0.5 rounded border border-sky-800/50 shrink-0">Lecturer</span>
                  </div>
                )}

                {renderItem({
                  id: 'lecturer-questions',
                  title: 'Ngân Hàng Câu Hỏi',
                  subtitle: 'Thang Bloom & duyệt đề thi AI',
                  tag: 'Bloom',
                  icon: Table2
                })}

                {renderItem({
                  id: 'lecturer-docs-rag',
                  title: 'Kho Tài Liệu RAG',
                  subtitle: 'Băm nhỏ giáo trình & slide bài giảng',
                  tag: 'RAG Docs',
                  icon: Database
                })}

                {renderItem({
                  id: 'rubric-studio',
                  title: 'Ma Trận Rubric',
                  subtitle: 'Tiêu chí & thang điểm chấm vấn đáp',
                  tag: 'Rubric',
                  icon: Award
                })}
              </>
            )}

          </nav>
        </div>

        {/* BOTTOM SECTION: SYSTEM HEALTH, RULES & USER INFO */}
        <div className="space-y-3 pt-3 border-t border-slate-800/80">

          {/* Quick Rules Action Button */}
          <button
            type="button"
            onClick={() => setIsRulesModalOpen(true)}
            className={`w-full flex items-center ${isCollapsed ? 'justify-center px-0' : 'px-3 gap-2.5'} py-2 rounded-xl text-xs font-semibold transition border ${
              isRulesModalOpen
                ? 'bg-sky-500/20 text-sky-300 border-sky-500/40'
                : 'bg-slate-900/60 text-slate-300 hover:text-white hover:bg-slate-800 border-slate-800'
            }`}
            title="Quy Chế Thi Vấn Đáp AIVES"
          >
            <ShieldCheck className="w-4 h-4 text-sky-400 shrink-0" />
            {!isCollapsed && (
              <span className="truncate text-left flex-1 font-medium text-[11px]">
                Quy Chế Thi Vấn Đáp
              </span>
            )}
          </button>

          {/* Admin RBAC Matrix Quick Modal Shortcut */}
          {isAdmin && onOpenMatrix && !isCollapsed && (
            <button
              type="button"
              onClick={onOpenMatrix}
              className="w-full flex items-center px-3 gap-2.5 py-2 rounded-xl text-xs font-semibold transition border bg-slate-900/60 text-slate-300 hover:text-white hover:bg-slate-800 border-slate-800 cursor-pointer"
              title="Xem Ma Trận Phân Quyền RBAC"
            >
              <Users className="w-4 h-4 text-emerald-400 shrink-0" />
              <span className="truncate text-left flex-1 font-medium text-[11px]">
                Ma Trận Phân Quyền RBAC
              </span>
            </button>
          )}

          {/* Smart System Connectivity Badge (When Expanded) */}
          {!isCollapsed && (
            <div className="p-2.5 rounded-xl bg-slate-900/80 border border-slate-800/90 text-[10px] space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-slate-400 font-bold uppercase tracking-wider text-[9px] flex items-center gap-1.5">
                  <Activity className="w-3 h-3 text-emerald-400" />
                  Hạ Tầng AIVES
                </span>
                <span className="flex items-center gap-1 text-emerald-400 font-bold text-[9px]">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  Online
                </span>
              </div>
              <div className="flex items-center justify-between text-slate-400">
                <span>PostgreSQL 15</span>
                <span className="font-mono text-slate-300">Port 5432</span>
              </div>
              <div className="flex items-center justify-between text-slate-400">
                <span>AI Viva Engine</span>
                <span className="font-mono text-cyan-300">RAG &amp; STT Live</span>
              </div>
            </div>
          )}

          {/* User Mini Bar */}
          <div className={`flex items-center ${isCollapsed ? 'justify-center' : 'justify-between'} p-1.5 rounded-xl bg-slate-900/40 border border-slate-800/60`}>
            <div className="flex items-center gap-2.5 min-w-0">
              <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-sky-600 to-cyan-500 flex items-center justify-center text-white font-bold text-xs shrink-0 shadow-xs">
                {getInitials(currentUser?.fullName || currentUser?.email)}
              </div>
              {!isCollapsed && (
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-bold text-white truncate leading-tight">
                    {currentUser?.fullName || currentUser?.email || 'Người Dùng'}
                  </p>
                  <p className="text-[10px] text-sky-400 font-mono truncate leading-tight mt-0.5">
                    {isAdmin ? 'Quản Trị Viên' : isLecturer ? 'Giảng Viên' : 'Thí Sinh'}
                  </p>
                </div>
              )}
            </div>
          </div>

        </div>
      </aside>

      {/* POPUP MODAL: QUY CHẾ THI VẤN ĐÁP AIVES */}
      {isRulesModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800">
            
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-sky-100 text-sky-600 flex items-center justify-center">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-heading font-extrabold text-base text-slate-900">
                    Quy Chế Thi Vấn Đáp AIVES
                  </h3>
                  <p className="text-[11px] text-slate-500">Chuẩn thi học thuật SWD392 FPT University</p>
                </div>
              </div>
              <button 
                type="button"
                onClick={() => setIsRulesModalOpen(false)} 
                className="text-slate-400 hover:text-slate-700 p-1 rounded-lg transition focus:outline-none cursor-pointer"
                aria-label="Đóng"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs text-slate-600 leading-relaxed font-normal">
              <div className="flex items-start gap-2.5 p-2 rounded-xl bg-slate-50 border border-slate-100">
                <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <span>Thí sinh phải bật Camera liên tục trong suốt quá trình trả lời câu hỏi thi.</span>
              </div>
              <div className="flex items-start gap-2.5 p-2 rounded-xl bg-slate-50 border border-slate-100">
                <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <span>Trả lời trực tiếp bằng giọng nói, hệ thống bóc băng STT và đối chiếu Rubric ma trận khách quan.</span>
              </div>
              <div className="flex items-start gap-2.5 p-2 rounded-xl bg-slate-50 border border-slate-100">
                <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <span>Nếu mất kết nối mạng, hệ thống tự động bảo lưu phiên thi trong <strong className="text-slate-900 font-bold">60 giây</strong> (Reconnect Recovery).</span>
              </div>
              <div className="flex items-start gap-2.5 p-2 rounded-xl bg-slate-50 border border-slate-100">
                <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <span>Sau khi nộp bài, chứng cứ âm thanh và transcript được ký băm SHA-256 chống gian lận.</span>
              </div>
            </div>

            <div className="pt-2 text-right">
              <button 
                type="button"
                onClick={() => setIsRulesModalOpen(false)} 
                className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-xs transition active:scale-95 cursor-pointer focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                Tôi Đã Hiểu &amp; Đồng Ý
              </button>
            </div>

          </div>
        </div>
      )}
    </>
  );
}
