import React, { useState } from 'react';
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
  LayoutDashboard,
  Sparkles,
  Award,
  Mic,
  CheckCircle2
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export function Sidebar({ currentTab, onNavigate }) {
  const { isAdmin, isLecturer } = useAuth();
  const isStudent = !isAdmin && !isLecturer;
  const [isRulesModalOpen, setIsRulesModalOpen] = useState(false);

  const getItemClass = (isActive) =>
    `w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl font-semibold text-xs transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
      isActive
        ? 'bg-sky-500/15 text-sky-400 border-sky-400/30 shadow-xs'
        : 'text-slate-300 border-transparent hover:text-white hover:bg-slate-800/60'
    }`;

  const getIconClass = (isActive) =>
    `w-4 h-4 shrink-0 transition-colors ${isActive ? 'text-sky-400' : 'text-slate-400'}`;

  return (
    <>
      <aside className="w-64 shrink-0 hidden md:block bg-sidebarBg text-slate-300 p-4 border-r border-slate-800 select-none">
        <div className="space-y-6 sticky top-20">

          {/* Navigation Menu */}
          <nav className="space-y-1.5 text-xs">
            {/* Student Portal Links (Only for Student role) */}
            {isStudent && (
              <>
                <div className="pt-1 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 px-3">
                  Cổng Sinh Viên
                </div>
                <button
                  type="button"
                  onClick={() => onNavigate('discover')}
                  className={getItemClass(!isRulesModalOpen && (currentTab === 'discover' || currentTab === 'home'))}
                >
                  <Calendar className={getIconClass(!isRulesModalOpen && (currentTab === 'discover' || currentTab === 'home'))} />
                  <span>Lịch Thi Của Tôi</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('calibration')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'calibration')}
                >
                  <Sliders className={getIconClass(!isRulesModalOpen && currentTab === 'calibration')} />
                  <span>Kiểm Tra Micro / Cam</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('exam-room')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'exam-room')}
                >
                  <Mic className={getIconClass(!isRulesModalOpen && currentTab === 'exam-room')} />
                  <span>Phòng Thi Vấn Đáp (Làm Bài)</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('submission-success')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'submission-success')}
                >
                  <CheckCircle2 className={getIconClass(!isRulesModalOpen && currentTab === 'submission-success')} />
                  <span>Biên Bản Nộp Bài Thi</span>
                </button>

                <button
                  type="button"
                  onClick={() => setIsRulesModalOpen(true)}
                  className={getItemClass(isRulesModalOpen)}
                >
                  <FileText className={getIconClass(isRulesModalOpen)} />
                  <span>Quy Chế Thi Vấn Đáp</span>
                </button>
              </>
            )}

            {/* Admin Module Section */}
            {isAdmin && (
              <>
                <div className="pt-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 px-3 flex items-center justify-between">
                  <span>LUỒNG QUẢN TRỊ (ADMIN)</span>
                  <span className="text-sky-400 font-mono text-[9px] bg-sky-950/80 px-1.5 py-0.5 rounded border border-sky-800/50">4 MÀN</span>
                </div>
                
                <button
                  type="button"
                  onClick={() => onNavigate('admin-dashboard')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'admin-dashboard')}
                >
                  <LayoutDashboard className={getIconClass(!isRulesModalOpen && currentTab === 'admin-dashboard')} />
                  <div className="flex-1 flex items-center justify-between">
                    <span>1. Giám Sát Phòng Thi Live</span>
                    <span className="text-[10px] text-slate-500 font-mono">DevOps</span>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('admin-voice-lab')}
                  className={getItemClass(!isRulesModalOpen && (currentTab === 'admin-voice-lab' || currentTab === 'voice-lab'))}
                >
                  <GraduationCap className={getIconClass(!isRulesModalOpen && (currentTab === 'admin-voice-lab' || currentTab === 'voice-lab'))} />
                  <div className="flex-1 flex items-center justify-between">
                    <span>2. Voice &amp; Speech Lab</span>
                    <span className="text-[10px] text-slate-500 font-mono">TTS/STT</span>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('admin-users')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'admin-users')}
                >
                  <Users className={getIconClass(!isRulesModalOpen && currentTab === 'admin-users')} />
                  <div className="flex-1 flex items-center justify-between">
                    <span>3. Phân Quyền RBAC</span>
                    <span className="text-[10px] text-slate-500 font-mono">Single Role</span>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('admin-subjects')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'admin-subjects')}
                >
                  <BookOpen className={getIconClass(!isRulesModalOpen && currentTab === 'admin-subjects')} />
                  <div className="flex-1 flex items-center justify-between">
                    <span>4. Quản Lý Môn Học</span>
                    <span className="text-[10px] text-slate-500 font-mono">Catalog</span>
                  </div>
                </button>
              </>
            )}

            {(isLecturer || isAdmin) && (
              <>
                <div className="pt-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 px-3">
                  Phân Hệ Giảng Viên
                </div>
                <button
                  type="button"
                  onClick={() => onNavigate('lecturer-dashboard')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'lecturer-dashboard')}
                >
                  <LayoutDashboard className={getIconClass(!isRulesModalOpen && currentTab === 'lecturer-dashboard')} />
                  <span>Trang Tổng Quan</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('lecturer-docs-rag')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'lecturer-docs-rag')}
                >
                  <Database className={getIconClass(!isRulesModalOpen && currentTab === 'lecturer-docs-rag')} />
                  <span>Kho Tài Liệu RAG</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('lecturer-questions')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'lecturer-questions')}
                >
                  <Table2 className={getIconClass(!isRulesModalOpen && currentTab === 'lecturer-questions')} />
                  <span>Ngân Hàng Câu Hỏi</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('space-detail')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'space-detail')}
                >
                  <Sparkles className={getIconClass(!isRulesModalOpen && currentTab === 'space-detail')} />
                  <span>QuestionStudio</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('rubric-studio')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'rubric-studio')}
                >
                  <Award className={getIconClass(!isRulesModalOpen && currentTab === 'rubric-studio')} />
                  <span>Ma Trận Rubric</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('lecturer-grading-queue')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'lecturer-grading-queue')}
                >
                  <ShieldCheck className={getIconClass(!isRulesModalOpen && currentTab === 'lecturer-grading-queue')} />
                  <span>Hàng Đợi Chấm &amp; FAP</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('lecturer-review-regrading')}
                  className={getItemClass(!isRulesModalOpen && currentTab === 'lecturer-review-regrading')}
                >
                  <FileText className={getIconClass(!isRulesModalOpen && currentTab === 'lecturer-review-regrading')} />
                  <span>Phúc Khảo &amp; Chấm Lại</span>
                </button>
              </>
            )}
          </nav>

        </div>
      </aside>

      {/* POPUP MODAL: QUY CHẾ THI VẤN ĐÁP AIVES */}
      {isRulesModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800">
            
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-heading font-extrabold text-base text-slate-900">
                Quy Chế Thi Vấn Đáp AIVES
              </h3>
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
              <p className="flex items-start gap-2">
                <span className="text-sky-500 font-bold">•</span>
                <span>Thí sinh phải bật Camera liên tục trong suốt quá trình trả lời câu hỏi.</span>
              </p>
              <p className="flex items-start gap-2">
                <span className="text-sky-500 font-bold">•</span>
                <span>Trả lời trực tiếp bằng giọng nói, hệ thống bóc băng STT và đối chiếu Rubric tức thì.</span>
              </p>
              <p className="flex items-start gap-2">
                <span className="text-sky-500 font-bold">•</span>
                <span>Nếu mất kết nối mạng, hệ thống tự động bảo lưu phiên thi trong <strong className="text-slate-900 font-bold">60 giây</strong>.</span>
              </p>
              <p className="flex items-start gap-2">
                <span className="text-sky-500 font-bold">•</span>
                <span>Sau khi nộp bài, chứng cứ âm thanh và transcript được ký băm SHA-256 chống giả mạo.</span>
              </p>
            </div>

            <div className="pt-2 text-right">
              <button 
                type="button"
                onClick={() => setIsRulesModalOpen(false)} 
                className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-xs transition active:scale-95 cursor-pointer focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                Tôi Đã Hiểu
              </button>
            </div>

          </div>
        </div>
      )}
    </>
  );
}
