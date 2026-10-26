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
  Check,
  LayoutDashboard,
  CalendarCheck,
  ClipboardCheck,
  Scale,
  BarChart3
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
          className={`w-full relative flex items-center ${isCollapsed ? 'justify-center px-0' : 'px-3 gap-3'} py-2.5 rounded-xl transition-all duration-150 text-left cursor-pointer focus:outline-none border-2 ${
            isActive
              ? 'bg-pink-100 text-ink border-green-500 shadow-[2px_2px_0_#DEB9B5]'
              : 'text-ink border-transparent hover:text-pink-900 hover:bg-pink-50'
          }`}
          title={isCollapsed ? `${title} • ${subtitle}` : undefined}
        >
          {/* Active Accent Bar on Left */}
          {isActive && (
            <span className="absolute left-0 top-2 bottom-2 w-1.5 bg-pink-600 rounded-r-full" />
          )}

          {/* Icon Badge */}
          <div
            className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 transition-colors border-2 ${
              isActive
                ? 'bg-pink-200 text-pink-800 border-green-500'
                : 'bg-green-100 text-green-700 group-hover:text-pink-700 group-hover:bg-pink-100 border-green-200'
            }`}
          >
            <Icon className="w-4 h-4" />
          </div>

          {/* Labels & Tags (Only shown when expanded) */}
          {!isCollapsed && (
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between gap-1.5">
                <span className={`text-xs font-bold truncate ${isActive ? 'text-pink-900 font-heading' : 'text-ink group-hover:text-pink-800'}`}>
                  {title}
                </span>
                {tag && (
                  <span
                    className={`text-[9px] font-mono px-1.5 py-0.5 rounded-full tracking-wider shrink-0 whitespace-nowrap font-bold border ${
                      isActive
                        ? 'bg-pink-200 text-pink-900 border-pink-400'
                        : 'bg-green-100 text-green-800 border-green-300'
                    }`}
                  >
                    {tag}
                  </span>
                )}
              </div>
              <p className={`text-[10px] truncate mt-0.5 font-normal ${isActive ? 'text-green-800 font-medium' : 'text-green-700'}`}>
                {subtitle}
              </p>
            </div>
          )}
        </button>

        {/* Floating Tooltip in Collapsed Mode */}
        {isCollapsed && (
          <div className="absolute left-full top-1/2 -translate-y-1/2 ml-3 hidden group-hover:flex flex-col z-50 bg-paper border-2 border-green-500 text-ink px-3 py-2 rounded-xl shadow-[4px_4px_0_#DEB9B5] whitespace-nowrap pointer-events-none animate-in fade-in zoom-in-95 duration-100">
            <span className="text-xs font-bold text-ink flex items-center gap-1.5 font-heading">
              {title}
              {tag && (
                <span className="text-[9px] font-mono px-1.5 py-0.5 rounded-full bg-pink-100 text-pink-800 border border-pink-300 font-bold">
                  {tag}
                </span>
              )}
            </span>
            <span className="text-[10px] text-green-700 mt-0.5">{subtitle}</span>
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
        } shrink-0 hidden md:flex flex-col justify-between bg-paper text-ink border-r-2 border-green-500 select-none transition-all duration-200 min-h-[calc(100vh-4rem)] p-3`}
      >
        {/* TOP SECTION: CONTEXT BANNER & NAVIGATION */}
        <div className="space-y-4">

          {/* Header & Toggle Button */}
          <div className="flex items-center justify-between px-2 pt-1 pb-2.5 border-b-2 border-cream">
            {!isCollapsed ? (
              <div className="flex items-center gap-2 min-w-0">
                <div className="w-2 h-2 rounded-full bg-pink-500 animate-pulse shrink-0" />
                <span className="text-[11px] font-black uppercase tracking-wider text-green-900 truncate font-heading">
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
              className="text-green-800 hover:text-pink-700 p-1.5 rounded-xl hover:bg-pink-100 border border-transparent hover:border-green-300 transition cursor-pointer focus:outline-none"
              title={isCollapsed ? 'Mở rộng thanh menu' : 'Thu gọn thanh menu'}
              aria-label={isCollapsed ? 'Mở rộng menu' : 'Thu gọn menu'}
            >
              {isCollapsed ? <PanelLeftOpen className="w-4 h-4" /> : <PanelLeftClose className="w-4 h-4" />}
            </button>
          </div>

          {/* Admin Segmented Filter (Only when expanded) */}
          {isAdmin && !isCollapsed && (
            <div className="flex items-center p-1 bg-pink-50 rounded-xl border-2 border-green-300 text-[11px] select-none gap-0.5">
              <button
                type="button"
                onClick={() => setAdminFilter('ALL')}
                className={`flex-1 py-1.5 rounded-lg font-bold transition text-center cursor-pointer ${
                  adminFilter === 'ALL'
                    ? 'bg-pink-100 text-pink-900 border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                    : 'text-green-800 hover:text-pink-700 hover:bg-pink-100/50 border border-transparent'
                }`}
              >
                Tất Cả (6)
              </button>
              <button
                type="button"
                onClick={() => setAdminFilter('ADMIN')}
                className={`flex-1 py-1.5 rounded-lg font-bold transition text-center cursor-pointer ${
                  adminFilter === 'ADMIN'
                    ? 'bg-pink-100 text-pink-900 border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                    : 'text-green-800 hover:text-pink-700 hover:bg-pink-100/50 border border-transparent'
                }`}
              >
                Quản Trị (3)
              </button>
              <button
                type="button"
                onClick={() => setAdminFilter('LECTURER')}
                className={`flex-1 py-1.5 rounded-lg font-bold transition text-center cursor-pointer ${
                  adminFilter === 'LECTURER'
                    ? 'bg-pink-100 text-pink-900 border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                    : 'text-green-800 hover:text-pink-700 hover:bg-pink-100/50 border border-transparent'
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
                  <div className="pt-1 pb-1 text-[10px] font-black uppercase tracking-wider text-green-800 px-3 flex items-center justify-between font-heading">
                    <span>Nhóm 3: Lõi Phỏng Vấn AI</span>
                    <span className="text-[9px] font-mono px-2 py-0.5 rounded-full font-bold bg-green-100 text-green-800 border border-green-300">Core</span>
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
                  id: 'my-results',
                  title: 'Kết Quả Thi',
                  subtitle: 'Điểm & nhận xét từng lượt hỏi',
                  tag: 'Kết quả',
                  icon: CheckCircle2
                })}
              </>
            )}

            {/* ADMIN MODULE SECTION - NHÓM 7: QUẢN TRỊ HỆ THỐNG */}
            {isAdmin && (adminFilter === 'ALL' || adminFilter === 'ADMIN') && (
              <>
                {!isCollapsed && (
                  <div className="pt-2 pb-1 text-[10px] font-black uppercase tracking-wider text-green-800 px-3 flex items-center justify-between font-heading">
                    <span className="truncate">Nhóm 7: Quản Trị Hệ Thống</span>
                    <span className="text-[9px] font-mono px-2 py-0.5 rounded-full font-bold bg-green-100 text-green-800 border border-green-300 shrink-0">Admin</span>
                  </div>
                )}

                {renderItem({
                  id: 'admin-stats',
                  title: 'Thống Kê Hệ Thống',
                  subtitle: 'Người dùng, kỳ thi & kết quả',
                  tag: 'Stats',
                  icon: BarChart3
                })}

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
                  <div className="pt-3 pb-1 text-[10px] font-black uppercase tracking-wider text-green-800 px-3 flex items-center justify-between font-heading">
                    <span className="truncate">Nhóm 1: Câu Hỏi &amp; Rubric</span>
                    <span className="text-[9px] font-mono px-2 py-0.5 rounded-full font-bold bg-green-100 text-green-800 border border-green-300 shrink-0">Lecturer</span>
                  </div>
                )}

                {renderItem({
                  id: 'lecturer-dashboard',
                  title: 'Tổng Quan',
                  subtitle: 'Môn học phụ trách & tiến độ chấm',
                  tag: 'Home',
                  icon: LayoutDashboard
                })}

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

                {renderItem({
                  id: 'lecturer-exams',
                  title: 'Quản Lý Kỳ Thi',
                  subtitle: 'Tạo kỳ thi, chọn câu hỏi, mở/đóng',
                  tag: 'Kỳ thi',
                  icon: CalendarCheck
                })}

                {renderItem({
                  id: 'lecturer-grading',
                  title: 'Chấm Điểm',
                  subtitle: 'Bài làm thí sinh & điểm từng câu',
                  tag: 'Chấm',
                  icon: ClipboardCheck
                })}

                {renderItem({
                  id: 'lecturer-scores',
                  title: 'Phúc Khảo',
                  subtitle: 'Điều chỉnh điểm tổng có lý do',
                  tag: 'Điểm',
                  icon: Scale
                })}
              </>
            )}

          </nav>
        </div>

        {/* BOTTOM SECTION: SYSTEM HEALTH, RULES & USER INFO */}
        <div className="space-y-3 pt-3 border-t-2 border-cream">

          {/* Quick Rules Action Button */}
          <button
            type="button"
            onClick={() => setIsRulesModalOpen(true)}
            className={`w-full flex items-center ${isCollapsed ? 'justify-center px-0' : 'px-3 gap-2.5'} py-2 rounded-xl text-xs font-bold transition border-2 cursor-pointer ${
              isRulesModalOpen
                ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                : 'bg-pink-50 text-ink hover:text-pink-900 hover:bg-pink-100 border-green-300'
            }`}
            title="Quy Chế Thi Vấn Đáp AIVES"
          >
            <ShieldCheck className="w-4 h-4 text-pink-700 shrink-0" />
            {!isCollapsed && (
              <span className="truncate text-left flex-1 font-bold text-[11px] text-ink font-heading">
                Quy Chế Thi Vấn Đáp
              </span>
            )}
          </button>

          {/* Admin RBAC Matrix Quick Modal Shortcut */}
          {isAdmin && onOpenMatrix && !isCollapsed && (
            <button
              type="button"
              onClick={onOpenMatrix}
              className="w-full flex items-center px-3 gap-2.5 py-2 rounded-xl text-xs font-bold transition border-2 bg-green-50 text-ink hover:text-green-900 hover:bg-green-100 border-green-300 cursor-pointer"
              title="Xem Ma Trận Phân Quyền RBAC"
            >
              <Users className="w-4 h-4 text-green-700 shrink-0" />
              <span className="truncate text-left flex-1 font-bold text-[11px] text-ink font-heading">
                Ma Trận Phân Quyền RBAC
              </span>
            </button>
          )}

          {/* Smart System Connectivity Badge (When Expanded) */}
          {!isCollapsed && (
            <div className="p-2.5 rounded-xl bg-pink-50 border-2 border-green-300 text-[10px] space-y-1.5 shadow-[2px_2px_0_#C4CCB7]">
              <div className="flex items-center justify-between">
                <span className="text-green-900 font-extrabold uppercase tracking-wider text-[9px] flex items-center gap-1.5 font-heading">
                  <Activity className="w-3 h-3 text-green-700" />
                  Hạ Tầng AIVES
                </span>
                <span className="flex items-center gap-1 text-green-900 font-bold text-[9px] bg-green-200 px-1.5 py-0.5 rounded border border-green-400">
                  <span className="w-1.5 h-1.5 rounded-full bg-green-600 animate-pulse" />
                  Online
                </span>
              </div>
              <div className="flex items-center justify-between text-green-800">
                <span>PostgreSQL 15</span>
                <span className="font-mono text-ink font-semibold">Port 5432</span>
              </div>
              <div className="flex items-center justify-between text-green-800">
                <span>AI Viva Engine</span>
                <span className="font-mono text-pink-700 font-bold">RAG &amp; STT Live</span>
              </div>
            </div>
          )}

          {/* User Mini Bar */}
          <div
            onClick={() => onNavigate && onNavigate('profile')}
            title="Quản lý hồ sơ & Đổi mật khẩu"
            className={`flex items-center ${isCollapsed ? 'justify-center' : 'justify-between'} p-1.5 rounded-xl border-2 transition-all cursor-pointer shadow-[2px_2px_0_#DEB9B5] ${
              currentTab === 'profile' || currentTab === 'profile-password'
                ? 'bg-pink-100 border-green-600 ring-2 ring-pink-300'
                : 'bg-paper hover:bg-pink-50 border-green-400 hover:border-green-600'
            }`}
          >
            <div className="flex items-center gap-2.5 min-w-0">
              <div className="w-8 h-8 rounded-full bg-pink-300 border-2 border-green-600 flex items-center justify-center text-ink font-heading font-extrabold text-xs shrink-0 shadow-xs overflow-hidden">
                {currentUser?.avatarUrl ? (
                  <img src={currentUser.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                ) : (
                  getInitials(currentUser?.fullName || currentUser?.email)
                )}
              </div>
              {!isCollapsed && (
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-bold text-ink truncate leading-tight font-heading">
                    {currentUser?.fullName || currentUser?.email || 'Người Dùng'}
                  </p>
                  <p className="text-[10px] text-green-700 font-bold truncate leading-tight mt-0.5">
                    {isAdmin ? 'Quản Trị Viên' : isLecturer ? 'Giảng Viên' : 'Thí Sinh'}
                  </p>
                </div>
              )}
            </div>
            {!isCollapsed && (
              <span className="text-[10px] font-bold text-pink-700 bg-pink-50 hover:bg-pink-200 border border-pink-300 px-2 py-0.5 rounded-lg shrink-0">
                Hồ sơ
              </span>
            )}
          </div>

        </div>
      </aside>

      {/* POPUP MODAL: QUY CHẾ THI VẤN ĐÁP AIVES */}
      {isRulesModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/50 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-paper rounded-2xl max-w-md w-full p-6 shadow-[6px_6px_0_#D5A6A2] border-2 border-green-500 space-y-4 animate-modal-entry text-ink">
            
            <div className="flex items-center justify-between pb-3 border-b-2 border-cream">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-pink-100 border-2 border-green-500 text-pink-700 flex items-center justify-center shadow-[2px_2px_0_#DEB9B5]">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-heading font-black text-base text-ink">
                    Quy Chế Thi Vấn Đáp AIVES
                  </h3>
                  <p className="text-[11px] text-green-700 font-medium">Chuẩn thi học thuật SWD392 FPT University</p>
                </div>
              </div>
              <button 
                type="button"
                onClick={() => setIsRulesModalOpen(false)} 
                className="text-green-800 hover:text-pink-700 p-1 rounded-lg transition focus:outline-none cursor-pointer"
                aria-label="Đóng"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs text-green-900 leading-relaxed font-normal">
              <div className="flex items-start gap-2.5 p-3 rounded-xl bg-pink-50 border-2 border-green-200">
                <Check className="w-4 h-4 text-green-700 shrink-0 mt-0.5" />
                <span>Thí sinh phải bật Camera liên tục trong suốt quá trình trả lời câu hỏi thi.</span>
              </div>
              <div className="flex items-start gap-2.5 p-3 rounded-xl bg-pink-50 border-2 border-green-200">
                <Check className="w-4 h-4 text-green-700 shrink-0 mt-0.5" />
                <span>Trả lời trực tiếp bằng giọng nói, hệ thống bóc băng STT và đối chiếu Rubric ma trận khách quan.</span>
              </div>
              <div className="flex items-start gap-2.5 p-3 rounded-xl bg-pink-50 border-2 border-green-200">
                <Check className="w-4 h-4 text-green-700 shrink-0 mt-0.5" />
                <span>Nếu mất kết nối mạng, hệ thống tự động bảo lưu phiên thi trong <strong className="text-pink-700 font-bold">60 giây</strong> (Reconnect Recovery).</span>
              </div>
              <div className="flex items-start gap-2.5 p-3 rounded-xl bg-pink-50 border-2 border-green-200">
                <Check className="w-4 h-4 text-green-700 shrink-0 mt-0.5" />
                <span>Sau khi nộp bài, chứng cứ âm thanh và transcript được ký băm SHA-256 chống gian lận.</span>
              </div>
            </div>

            <div className="pt-2 text-right">
              <button 
                type="button"
                onClick={() => setIsRulesModalOpen(false)} 
                className="px-5 py-2.5 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-600 shadow-[2px_2px_0_#3A422E] transition active:scale-95 cursor-pointer focus:outline-none"
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
