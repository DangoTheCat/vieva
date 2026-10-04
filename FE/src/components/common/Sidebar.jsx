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
  X
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export function Sidebar({ currentTab, onNavigate }) {
  const { isAdmin, isLecturer } = useAuth();
  const [isRulesModalOpen, setIsRulesModalOpen] = useState(false);

  return (
    <>
      <aside className="w-64 shrink-0 hidden md:block bg-sidebarBg text-slate-300 p-4 border-r border-slate-800 select-none">
        <div className="space-y-6 sticky top-20">

          {/* Navigation Menu */}
          <nav className="space-y-1.5 text-xs">
            {/* Student Portal Links */}
            <button
              type="button"
              onClick={() => onNavigate('discover')}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-full font-bold transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
                !isRulesModalOpen && (currentTab === 'discover' || currentTab === 'home')
                  ? 'bg-[#092244] text-sky-400 border-sky-500 shadow-sm'
                  : 'text-white border-transparent hover:bg-slate-800/60'
              }`}
            >
              <Calendar className={`w-4 h-4 ${!isRulesModalOpen && (currentTab === 'discover' || currentTab === 'home') ? 'text-sky-400' : 'text-white'}`} />
              <span>Lịch Thi Của Tôi</span>
            </button>

            <button
              type="button"
              onClick={() => onNavigate('calibration')}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-full font-bold transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
                !isRulesModalOpen && currentTab === 'calibration'
                  ? 'bg-[#092244] text-sky-400 border-sky-500 shadow-sm'
                  : 'text-white border-transparent hover:bg-slate-800/60'
              }`}
            >
              <Sliders className={`w-4 h-4 ${!isRulesModalOpen && currentTab === 'calibration' ? 'text-sky-400' : 'text-white'}`} />
              <span>Kiểm Tra Micro / Cam</span>
            </button>

            <button
              type="button"
              onClick={() => setIsRulesModalOpen(true)}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-full font-bold transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
                isRulesModalOpen
                  ? 'bg-[#092244] text-sky-400 border-sky-500 shadow-sm'
                  : 'text-white border-transparent hover:bg-slate-800/60'
              }`}
            >
              <FileText className={`w-4 h-4 ${isRulesModalOpen ? 'text-sky-400' : 'text-white'}`} />
              <span>Quy Chế Thi Vấn Đáp</span>
            </button>

            {/* Admin & Faculty Module Section */}
            {isAdmin && (
              <>
                <div className="pt-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 px-3">
                  Quản Trị Hệ Thống
                </div>
                <button
                  type="button"
                  onClick={() => onNavigate('admin-users')}
                  className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-full font-bold transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
                    !isRulesModalOpen && currentTab === 'admin-users'
                      ? 'bg-[#092244] text-sky-400 border-sky-500 shadow-sm'
                      : 'text-white border-transparent hover:bg-slate-800/60'
                  }`}
                >
                  <Users className={`w-4 h-4 ${!isRulesModalOpen && currentTab === 'admin-users' ? 'text-sky-400' : 'text-white'}`} />
                  <span>Phân Quyền Người Dùng</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('admin-subjects')}
                  className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-full font-bold transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
                    !isRulesModalOpen && currentTab === 'admin-subjects'
                      ? 'bg-[#092244] text-sky-400 border-sky-500 shadow-sm'
                      : 'text-white border-transparent hover:bg-slate-800/60'
                  }`}
                >
                  <BookOpen className={`w-4 h-4 ${!isRulesModalOpen && currentTab === 'admin-subjects' ? 'text-sky-400' : 'text-white'}`} />
                  <span>Quản Lý Môn Học</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('admin-voice-lab')}
                  className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-full font-bold transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
                    !isRulesModalOpen && (currentTab === 'admin-voice-lab' || currentTab === 'voice-lab')
                      ? 'bg-[#092244] text-sky-400 border-sky-500 shadow-sm'
                      : 'text-white border-transparent hover:bg-slate-800/60'
                  }`}
                >
                  <GraduationCap className={`w-4 h-4 ${!isRulesModalOpen && (currentTab === 'admin-voice-lab' || currentTab === 'voice-lab') ? 'text-sky-400' : 'text-white'}`} />
                  <span>Voice &amp; Speech Lab</span>
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
                  onClick={() => onNavigate('lecturer-docs-rag')}
                  className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-full font-bold transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
                    !isRulesModalOpen && currentTab === 'lecturer-docs-rag'
                      ? 'bg-[#092244] text-sky-400 border-sky-500 shadow-sm'
                      : 'text-white border-transparent hover:bg-slate-800/60'
                  }`}
                >
                  <Database className={`w-4 h-4 ${!isRulesModalOpen && currentTab === 'lecturer-docs-rag' ? 'text-sky-400' : 'text-white'}`} />
                  <span>Kho Tài Liệu RAG</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('lecturer-questions')}
                  className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-full font-bold transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
                    !isRulesModalOpen && currentTab === 'lecturer-questions'
                      ? 'bg-[#092244] text-sky-400 border-sky-500 shadow-sm'
                      : 'text-white border-transparent hover:bg-slate-800/60'
                  }`}
                >
                  <Table2 className={`w-4 h-4 ${!isRulesModalOpen && currentTab === 'lecturer-questions' ? 'text-sky-400' : 'text-white'}`} />
                  <span>Ngân Hàng Câu Hỏi</span>
                </button>

                <button
                  type="button"
                  onClick={() => onNavigate('lecturer-grading-queue')}
                  className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-full font-bold transition-all border text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 cursor-pointer ${
                    !isRulesModalOpen && (currentTab === 'lecturer-grading-queue' || currentTab === 'lecturer-review-regrading')
                      ? 'bg-[#092244] text-sky-400 border-sky-500 shadow-sm'
                      : 'text-white border-transparent hover:bg-slate-800/60'
                  }`}
                >
                  <ShieldCheck className={`w-4 h-4 ${!isRulesModalOpen && (currentTab === 'lecturer-grading-queue' || currentTab === 'lecturer-review-regrading') ? 'text-sky-400' : 'text-white'}`} />
                  <span>Hàng Đợi Chấm &amp; FAP</span>
                </button>
              </>
            )}
          </nav>

        </div>
      </aside>

      {/* POPUP MODAL: QUY CHẾ THI VẤN ĐÁP AIVES */}
      {isRulesModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-neutral-950/75 animate-in fade-in duration-150">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-7 shadow-2xl border border-slate-100 space-y-5 animate-modal-entry text-slate-800">
            
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <h3 className="font-heading font-extrabold text-lg text-slate-900">
                Quy Chế Thi Vấn Đáp AIVES
              </h3>
              <button 
                type="button"
                onClick={() => setIsRulesModalOpen(false)} 
                className="text-slate-400 hover:text-slate-700 p-1 transition focus:outline-none cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3.5 text-xs text-slate-600 leading-relaxed font-normal">
              <p className="flex items-start gap-2">
                <span className="text-slate-400 font-bold">•</span>
                <span>Thí sinh phải bật Camera liên tục trong suốt quá trình trả lời câu hỏi.</span>
              </p>
              <p className="flex items-start gap-2">
                <span className="text-slate-400 font-bold">•</span>
                <span>Trả lời trực tiếp bằng giọng nói, hệ thống bóc băng STT và đối chiếu Rubric tức thì.</span>
              </p>
              <p className="flex items-start gap-2">
                <span className="text-slate-400 font-bold">•</span>
                <span>Nếu mất kết nối mạng, hệ thống tự động bảo lưu phiên thi trong <strong className="text-slate-900 font-bold">60 giây</strong>.</span>
              </p>
              <p className="flex items-start gap-2">
                <span className="text-slate-400 font-bold">•</span>
                <span>Sau khi nộp bài, chứng cứ âm thanh và transcript được ký băm SHA-256 chống giả mạo.</span>
              </p>
            </div>

            <div className="pt-2 text-right">
              <button 
                type="button"
                onClick={() => setIsRulesModalOpen(false)} 
                className="px-6 py-2.5 rounded-full bg-[#111827] hover:bg-slate-800 text-white font-bold text-xs shadow-md transition active:scale-95 cursor-pointer"
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
