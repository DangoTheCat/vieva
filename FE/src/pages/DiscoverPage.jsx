import React from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  ArrowRight, 
  ExternalLink
} from 'lucide-react';

export function DiscoverPage({ onNavigate }) {
  const { currentUser } = useAuth();

  const studentName = currentUser?.fullName || 'Nguyễn Văn An';
  const studentCode = currentUser?.userCode || 'SE160892';

  return (
    <div className="space-y-6 max-w-4xl mx-auto w-full animate-modal-entry text-slate-800">
      
      {/* HEADER SECTION (Clean, Minimalist) */}
      <div className="flex flex-wrap items-center justify-between gap-4 py-2">
        <div>
          <h1 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
            Cổng Thi Vấn Đáp Trực Tuyến
          </h1>
          <p className="text-xs text-slate-500 mt-1 font-medium">
            {studentName} • {studentCode} • Ngành Kỹ Thuật Phần Mềm (FPT University)
          </p>
        </div>

        <div className="flex items-center gap-3">
          <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-emerald-50 text-emerald-800 text-xs font-semibold border border-emerald-200">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            Đủ Điều Kiện Dự Thi (Eligible)
          </span>
        </div>
      </div>

      {/* PRIMARY ACTIVE SLOT CARD (SchoolAI Style) */}
      <div className="bg-white border border-slate-200/90 hover:border-slate-300 p-6 md:p-8 rounded-3xl space-y-6 shadow-2xs hover:shadow-md transition duration-200">
        
        {/* Top Badges Row */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200/80">
              Ca Thi Sắp Diễn Ra
            </span>
            <span className="text-xs font-mono font-medium text-slate-500">MÃ CA THI: #VIVA-SWD-9842</span>
          </div>
          <span className="text-xs font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-1 rounded-full flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
            Phòng Thi Đang Mở
          </span>
        </div>

        {/* Course Info */}
        <div>
          <h2 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
            SWD392 • Kiến Trúc &amp; Thiết Kế Phần Mềm
          </h2>
          <p className="text-xs text-slate-600 mt-1.5 font-medium">
            Giám thị Ảo: <strong className="text-slate-900">Dr. Sophia (AI Examiner)</strong> • Bộ môn Kỹ Thuật Phần Mềm
          </p>
        </div>

        {/* 3 Quick Info Chips */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl text-xs space-y-1">
            <span className="text-slate-400 block text-[10px] font-bold uppercase tracking-wider">Thời lượng</span>
            <span className="font-bold text-slate-900 text-sm block">15 Phút Vấn Đáp</span>
          </div>
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl text-xs space-y-1">
            <span className="text-slate-400 block text-[10px] font-bold uppercase tracking-wider">Số câu hỏi</span>
            <span className="font-bold text-slate-900 text-sm block">The 3 Whys (3 Vòng)</span>
          </div>
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl text-xs space-y-1">
            <span className="text-slate-400 block text-[10px] font-bold uppercase tracking-wider">Ngôn ngữ thi</span>
            <span className="font-bold text-slate-900 text-sm block">Tiếng Việt (vi-VN)</span>
          </div>
        </div>

        {/* Action Button */}
        <div className="pt-2 flex items-center justify-end border-t border-slate-100">
          <button 
            type="button"
            onClick={() => onNavigate && onNavigate('calibration')}
            className="bg-[#0066FF] hover:bg-[#0052CC] text-white font-bold text-xs sm:text-sm px-7 py-3 rounded-full shadow-md shadow-blue-600/20 flex items-center gap-2 transition active:scale-95 cursor-pointer"
          >
            <span>Kiểm Tra Thiết Bị &amp; Vào Thi</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>

      </div>

      {/* PAST EXAM HISTORY (Paginated Table in Elevated Card) */}
      <div className="bg-white border border-slate-200/90 rounded-2xl overflow-hidden shadow-2xs">
        <div className="p-4 sm:p-5 bg-white border-b border-slate-200/90 flex items-center justify-between">
          <h3 className="font-heading font-bold text-sm text-slate-900">
            Lịch Sử Các Bài Thi Vấn Đáp Đã Hoàn Thành
          </h3>
          <span className="text-xs text-slate-400 font-medium">1 bài đã công bố</span>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700">
            <thead className="bg-slate-50 text-[10px] font-bold text-slate-400 uppercase border-b border-slate-200">
              <tr>
                <th className="p-4">Môn Học</th>
                <th className="p-4">Ngày Thi</th>
                <th className="p-4">Điểm Rubric AI</th>
                <th className="p-4">Trạng Thái</th>
                <th className="p-4 text-right">Biên Bản</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              <tr className="hover:bg-slate-50/70 transition">
                <td className="p-4 font-bold text-slate-900">PRN231 - .NET Enterprise Architecture</td>
                <td className="p-4 text-slate-500 font-mono">18/09/2026</td>
                <td className="p-4">
                  <span className="font-bold text-emerald-600 font-mono text-sm">8.8 / 10</span>
                </td>
                <td className="p-4">
                  <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-bold text-[10px]">
                    Đã Công Bố
                  </span>
                </td>
                <td className="p-4 text-right">
                  <button 
                    type="button"
                    onClick={() => {
                      if (onNavigate) {
                        onNavigate('submission-success');
                      }
                    }}
                    className="text-blue-600 hover:text-blue-800 font-semibold inline-flex items-center gap-1 transition cursor-pointer"
                  >
                    <span>Xem Chi Tiết</span>
                    <ExternalLink className="w-3 h-3" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </div>
  );
}
