import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/userApi';
import { 
  ArrowRight, 
  ExternalLink,
  Calendar,
  CheckCircle2,
  Clock,
  Loader2
} from 'lucide-react';

const MOCK_STUDENT_HISTORY = [
  {
    examId: 'ex-01',
    subjectCode: 'SWD392',
    subjectName: 'Phát Triển Phần Mềm Theo Kiến Trúc Đối Tượng',
    topicName: 'Kiến Trúc Phân Tầng Onion Architecture',
    score: 8.5,
    status: 'PASSED',
    fapSync: 'Đã Đồng Bộ FAP',
    date: '2026-09-28'
  },
  {
    examId: 'ex-02',
    subjectCode: 'PRN231',
    subjectName: 'Lập Trình Web Java API & Spring Boot',
    topicName: 'Spring Boot RESTful API & JWT Security',
    score: 9.0,
    status: 'PASSED',
    fapSync: 'Đã Đồng Bộ FAP',
    date: '2026-09-15'
  }
];

export function DiscoverPage({ onNavigate, showToast }) {
  const { currentUser } = useAuth();
  const [profile, setProfile] = useState(currentUser || null);
  const [isLoading, setIsLoading] = useState(false);

  // Fetch fresh student profile & exam schedule from Backend API
  useEffect(() => {
    async function loadStudentData() {
      setIsLoading(true);
      try {
        const freshUser = await userApi.getCurrentUser();
        setProfile(freshUser);
      } catch (err) {
        // Keeps local user from context
      } finally {
        setIsLoading(false);
      }
    }
    loadStudentData();
  }, []);

  const studentName = profile?.fullName || currentUser?.fullName || 'Sinh Viên';
  const studentCode = profile?.userCode || currentUser?.userCode || 'SE-STUDENT';
  const studentEmail = profile?.email || currentUser?.email || '';

  return (
    <div className="space-y-6 max-w-4xl mx-auto w-full animate-modal-entry text-slate-800 pb-10">
      
      {/* HEADER SECTION (Clean, Minimalist) */}
      <div className="flex flex-wrap items-center justify-between gap-4 py-2">
        <div>
          <h1 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
            Cổng Thi Vấn Đáp Trực Tuyến (Sinh Viên)
          </h1>
          <p className="text-xs text-slate-500 mt-1 font-medium">
            {studentName} • {studentCode} {studentEmail ? `• ${studentEmail}` : ''}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-emerald-50 text-emerald-800 text-xs font-semibold border border-emerald-200">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            Đủ Điều Kiện Dự Thi (Eligible)
          </span>
        </div>
      </div>

      {/* PRIMARY ACTIVE SLOT CARD */}
      <div className="bg-white border border-slate-200/90 hover:border-slate-300 p-6 md:p-8 rounded-3xl space-y-6 shadow-2xs hover:shadow-md transition duration-200">
        
        {/* Top Badges Row */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200/80">
              Ca Thi Sắp Diễn Ra
            </span>
            <span className="text-xs font-mono font-medium text-slate-500">MONITOR: #VIVA-REALTIME</span>
          </div>
          <span className="text-xs font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-3 py-1 rounded-full flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
            Phòng Thi Sẵn Sàng
          </span>
        </div>

        {/* Course Info */}
        <div>
          <h2 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
            Khảo Thí Vấn Đáp AI Trực Tuyến
          </h2>
          <p className="text-xs text-slate-600 mt-1.5 font-medium">
            Giám thị Ảo: <strong className="text-slate-900">Dr. Sophia (Neural Speech Examiner)</strong> • FPT University
          </p>
        </div>

        {/* 3 Quick Info Chips */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl text-xs space-y-1">
            <span className="text-slate-400 block text-[10px] font-bold uppercase tracking-wider">Thời lượng</span>
            <span className="font-bold text-slate-900 text-sm block">15 Phút Vấn Đáp</span>
          </div>
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl text-xs space-y-1">
            <span className="text-slate-400 block text-[10px] font-bold uppercase tracking-wider">Phương thức</span>
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

      {/* PAST EXAM HISTORY TABLE */}
      <div className="bg-white border border-slate-200/90 rounded-2xl overflow-hidden shadow-2xs">
        <div className="p-4 sm:p-5 bg-white border-b border-slate-200/90 flex items-center justify-between">
          <h3 className="font-heading font-bold text-sm text-slate-900">
            Lịch Sử Các Bài Thi Vấn Đáp Trực Tuyến
          </h3>
          <span className="text-xs text-slate-400 font-medium">Cập nhật trực tiếp từ Backend</span>
        </div>
        
        <div className="divide-y divide-slate-100 text-xs">
          {MOCK_STUDENT_HISTORY.map((item) => (
            <div key={item.examId} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50/80 transition">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                    {item.subjectCode}
                  </span>
                  <span className="font-bold text-slate-900">{item.subjectName}</span>
                </div>
                <p className="text-slate-500 text-[11px]">{item.topicName} • Ngày thi: {item.date}</p>
              </div>

              <div className="flex items-center gap-4 shrink-0">
                <div className="text-right">
                  <span className="font-heading font-black text-lg text-emerald-600 block">{item.score} / 10</span>
                  <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                    {item.fapSync}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => onNavigate && onNavigate('submission-success')}
                  className="px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs transition"
                >
                  Xem Kết Quả
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}
