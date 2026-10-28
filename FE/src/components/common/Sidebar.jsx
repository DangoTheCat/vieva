import React, { useState, useEffect } from 'react';
import {
  Users,
  Database,
  Table2,
  BookOpen,
  Sliders,
  Calendar,
  Award,
  Mic,
  CheckCircle2,
  PanelLeftClose,
  PanelLeftOpen,
  LayoutDashboard,
  CalendarCheck,
  ClipboardCheck,
  Scale,
  BarChart3,
  User
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export function Sidebar({ currentTab, onNavigate, onOpenMatrix }) {
  const { isAdmin, isLecturer } = useAuth();
  const isStudent = !isAdmin && !isLecturer;
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
    const adminTabs = ['admin-stats', 'admin-users', 'admin-subjects', 'admin-voice-lab', 'voice-lab'];
    const lecturerTabs = ['lecturer-dashboard', 'lecturer-questions', 'lecturer-docs-rag', 'rubric-studio', 'lecturer-exams', 'lecturer-grading', 'lecturer-scores'];

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

  // Nav Item Component (Clean single-line title, no redundant descriptions)
  const renderItem = ({ id, title, icon: Icon, activeWhen }) => {
    const isActive = activeWhen ? activeWhen(currentTab) : currentTab === id;

    return (
      <div key={id} className="relative group">
        <button
          type="button"
          onClick={() => onNavigate(id)}
          className={`w-full relative flex items-center ${isCollapsed ? 'justify-center px-0' : 'px-3 gap-3'} py-2.5 rounded-xl transition text-left cursor-pointer focus:outline-none ${
            isActive
              ? 'bg-pink-100/80 text-pink-900 font-semibold border border-pink-200 shadow-2xs'
              : 'text-ink hover:text-pink-900 hover:bg-pink-50/70 font-medium'
          }`}
          title={isCollapsed ? title : undefined}
        >
          {/* Active Accent Bar on Left */}
          {isActive && (
            <span className="absolute left-0 top-2 bottom-2 w-1 bg-pink-500 rounded-r-full" />
          )}

          {/* Icon Badge */}
          <div
            className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 transition ${
              isActive
                ? 'bg-pink-200 text-pink-900'
                : 'bg-pink-50 text-pink-700 group-hover:bg-pink-100 group-hover:text-pink-900'
            }`}
          >
            <Icon className="w-4 h-4" />
          </div>

          {/* Navigation Label */}
          {!isCollapsed && (
            <span className={`flex-1 min-w-0 text-xs font-semibold truncate ${isActive ? 'text-pink-900 font-bold' : 'text-ink group-hover:text-pink-900'}`}>
              {title}
            </span>
          )}
        </button>

        {/* Floating Tooltip in Collapsed Mode */}
        {isCollapsed && (
          <div className="absolute left-full top-1/2 -translate-y-1/2 ml-3 hidden group-hover:flex z-50 bg-pink-950 border border-pink-900 text-white px-3 py-1.5 rounded-lg shadow-lg whitespace-nowrap pointer-events-none animate-in fade-in zoom-in-95 duration-100">
            <span className="text-xs font-semibold text-white">
              {title}
            </span>
          </div>
        )}
      </div>
    );
  };

  return (
    <>
      <aside
        className={`${
          isCollapsed ? 'w-18' : 'w-72'
        } shrink-0 hidden md:flex flex-col justify-between bg-white text-ink border-r border-pink-200 select-none transition-all duration-200 min-h-[calc(100vh-4rem)] p-3`}
      >
        {/* TOP SECTION: CONTEXT BANNER & NAVIGATION */}
        <div className="space-y-4 flex-1">

          {/* Header & Toggle Button */}
          <div className="flex items-center px-2 pt-1 pb-2.5 border-b border-pink-100">
            {!isCollapsed ? (
              <div className="flex items-center gap-2 min-w-0">
                <div className="w-2 h-2 rounded-full bg-pink-500 animate-pulse shrink-0" />
                <span className="text-[11px] font-bold uppercase tracking-wider text-pink-900 truncate font-bold">
                  {isAdmin ? 'Quản Trị Trung Tâm' : isLecturer ? 'Phân Hệ Giảng Viên' : 'Cổng Thi Vấn Đáp'}
                </span>
              </div>
            ) : (
              <div className="w-full flex justify-center pb-1">
                <div className="w-2 h-2 rounded-full bg-pink-500 animate-pulse" />
              </div>
            )}

            <button
              type="button"
              onClick={toggleCollapse}
              className="text-pink-700 hover:text-pink-900 p-1 rounded-lg hover:bg-pink-50 transition cursor-pointer focus:outline-none"
              title={isCollapsed ? 'Mở rộng thanh menu' : 'Thu gọn thanh menu'}
              aria-label={isCollapsed ? 'Mở rộng menu' : 'Thu gọn menu'}
            >
              {isCollapsed ? <PanelLeftOpen className="w-4 h-4" /> : <PanelLeftClose className="w-4 h-4" />}
            </button>
          </div>

          {/* Admin Segmented Filter (Only when expanded) */}
          {isAdmin && !isCollapsed && (
            <div className="flex items-center p-1 bg-pink-50 rounded-xl border border-pink-200 text-[11px] select-none gap-1">
              <button
                type="button"
                onClick={() => setAdminFilter('ALL')}
                className={`flex-1 py-1 rounded-lg font-semibold transition text-center cursor-pointer ${
                  adminFilter === 'ALL'
                    ? 'bg-white text-pink-900 shadow-xs ring-1 ring-pink-300 font-bold'
                    : 'text-pink-800 hover:text-pink-900 hover:bg-pink-100/60 font-semibold'
                }`}
              >
                Tất Cả
              </button>
              <button
                type="button"
                onClick={() => setAdminFilter('ADMIN')}
                className={`flex-1 py-1 rounded-lg font-semibold transition text-center cursor-pointer ${
                  adminFilter === 'ADMIN'
                    ? 'bg-white text-pink-900 shadow-xs ring-1 ring-pink-300 font-bold'
                    : 'text-pink-800 hover:text-pink-900 hover:bg-pink-100/60 font-semibold'
                }`}
              >
                Quản Trị
              </button>
              <button
                type="button"
                onClick={() => setAdminFilter('LECTURER')}
                className={`flex-1 py-1 rounded-lg font-semibold transition text-center cursor-pointer ${
                  adminFilter === 'LECTURER'
                    ? 'bg-white text-pink-900 shadow-xs ring-1 ring-pink-300 font-bold'
                    : 'text-pink-800 hover:text-pink-900 hover:bg-pink-100/60 font-semibold'
                }`}
              >
                Đề &amp; Rubric
              </button>
            </div>
          )}

          {/* NAVIGATION LINKS */}
          <nav className="space-y-1 text-xs">

            {/* STUDENT PORTAL LINKS */}
            {isStudent && (
              <>
                {!isCollapsed && (
                  <div className="pt-1 pb-1 text-[10px] font-semibold uppercase tracking-wider text-pink-800/80 px-3 font-bold flex items-center">
                    <span>Lõi Phỏng Vấn AI</span>
                    
                  </div>
                )}

                {renderItem({
                  id: 'discover',
                  title: 'Lịch Thi Vấn Đáp',
                  icon: Calendar,
                  activeWhen: (tab) => tab === 'discover' || tab === 'home'
                })}

                {renderItem({
                  id: 'calibration',
                  title: 'Kiểm Tra Thiết Bị',
                  icon: Sliders
                })}

                {renderItem({
                  id: 'exam-room',
                  title: 'Phòng Thi Vấn Đáp AI',
                  icon: Mic
                })}

                {renderItem({
                  id: 'my-results',
                  title: 'Kết Quả Thi',
                  icon: CheckCircle2
                })}
              </>
            )}

            {/* ADMIN MODULE SECTION */}
            {isAdmin && (adminFilter === 'ALL' || adminFilter === 'ADMIN') && (
              <>
                {!isCollapsed && (
                  <div className="pt-2 pb-1 text-[10px] font-semibold uppercase tracking-wider text-pink-800/80 px-3 font-bold flex items-center">
                    <span className="truncate">Quản Trị Hệ Thống</span>
                    
                  </div>
                )}

                {renderItem({
                  id: 'admin-stats',
                  title: 'Thống Kê Hệ Thống',
                  icon: BarChart3
                })}

                {renderItem({
                  id: 'admin-users',
                  title: 'Quản Lý Tài Khoản',
                  icon: Users
                })}

                {renderItem({
                  id: 'admin-subjects',
                  title: 'Môn Học & Phân Công',
                  icon: BookOpen
                })}

                {renderItem({
                  id: 'admin-voice-lab',
                  title: 'Cấu Hình STT / TTS',
                  icon: Mic,
                  activeWhen: (tab) => tab === 'admin-voice-lab' || tab === 'voice-lab'
                })}
              </>
            )}

            {/* LECTURER SECTION */}
            {(isLecturer || (isAdmin && (adminFilter === 'ALL' || adminFilter === 'LECTURER'))) && (
              <>
                {!isCollapsed && (
                  <div className="pt-2 pb-1 text-[10px] font-semibold uppercase tracking-wider text-pink-800/80 px-3 font-bold flex items-center">
                    <span className="truncate">Câu Hỏi &amp; Rubric</span>
                    
                  </div>
                )}

                {renderItem({
                  id: 'lecturer-dashboard',
                  title: 'Tổng Quan',
                  icon: LayoutDashboard
                })}

                {renderItem({
                  id: 'lecturer-questions',
                  title: 'Ngân Hàng Câu Hỏi',
                  icon: Table2
                })}

                {renderItem({
                  id: 'lecturer-docs-rag',
                  title: 'Kho Tài Liệu RAG',
                  icon: Database
                })}

                {renderItem({
                  id: 'rubric-studio',
                  title: 'Ma Trận Rubric',
                  icon: Award
                })}

                {renderItem({
                  id: 'lecturer-exams',
                  title: 'Quản Lý Kỳ Thi',
                  icon: CalendarCheck
                })}

                {renderItem({
                  id: 'lecturer-grading',
                  title: 'Chấm Điểm',
                  icon: ClipboardCheck
                })}

                {renderItem({
                  id: 'lecturer-scores',
                  title: 'Phúc Khảo',
                  icon: Scale
                })}
              </>
            )}

          </nav>
        </div>

        {/* BOTTOM PINNED SECTION: HỒ SƠ CỦA TÔI */}
        <div className="pt-2 border-t border-pink-100 shrink-0 mt-3">
          {renderItem({
            id: 'profile',
            title: 'Hồ Sơ Của Tôi',
            icon: User,
            activeWhen: (tab) => tab === 'profile' || tab === 'profile-password'
          })}
        </div>
      </aside>
    </>
  );
}
