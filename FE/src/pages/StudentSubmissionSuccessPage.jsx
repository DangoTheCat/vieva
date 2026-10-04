import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  CheckCircle2, 
  Lock, 
  Check, 
  FileText, 
  ArrowLeft, 
  Copy
} from 'lucide-react';

export function StudentSubmissionSuccessPage({ onNavigate, showToast }) {
  const { currentUser } = useAuth();
  const [isCopied, setIsCopied] = useState(false);

  const studentName = currentUser?.fullName || 'Nguyen Van B';
  const studentCode = currentUser?.userCode || 'SE160982';

  const sha256Hash = 'e3b8c44298fc1c149afbt4c8996tb92427ae41e4649b934ca495991b7852b855';

  const handleCopyHash = () => {
    navigator.clipboard.writeText(sha256Hash);
    setIsCopied(true);
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Đã Sao Chép Chữ Ký Số',
        message: 'Mã SHA-256 Tamper-Proof đã được sao chép vào bộ nhớ tạm!'
      });
    }
    setTimeout(() => setIsCopied(false), 2000);
  };

  const handlePrintCertificate = () => {
    window.print();
  };

  return (
    <div className="min-h-full py-8 px-4 sm:px-6 max-w-4xl mx-auto space-y-8 animate-modal-entry text-slate-800">
      
      {/* TOP HERO HEADER */}
      <div className="text-center space-y-4">
        {/* Success Icon */}
        <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-sm border border-emerald-200">
          <CheckCircle2 className="w-10 h-10" />
        </div>

        {/* Big Title */}
        <h1 className="font-heading font-extrabold text-3xl sm:text-4xl text-slate-900 tracking-tight">
          Nộp Bài Thi Vấn Đáp Thành Công!
        </h1>

        <p className="text-xs sm:text-sm text-slate-600 max-w-xl mx-auto leading-relaxed">
          Toàn bộ bản ghi âm 5 lượt vấn đáp, nhật ký bóc băng STT và telemetry giám thị ảo đã được đóng gói và mã hóa lưu vào sổ cái khảo thí của Nhà trường.
        </p>

        {/* Micro Badges */}
        <div className="flex flex-wrap items-center justify-center gap-3 pt-1 text-xs">
          <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-slate-200 shadow-2xs font-mono text-slate-600">
            <Lock className="w-3.5 h-3.5 text-slate-400" />
            <span>Mã nộp bài: <strong className="text-slate-900">#SUB-2026-98421</strong></span>
          </span>

          <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-800 font-bold shadow-2xs">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
            <span>Chữ ký số: SHA-256 Tamper-Proof</span>
          </span>
        </div>
      </div>

      {/* ELEVATED EXAM CERTIFICATE CARD */}
      <div className="bg-white border border-slate-200/90 rounded-3xl p-6 sm:p-8 shadow-xs space-y-6">
        
        {/* Card Header */}
        <div className="flex flex-wrap items-center justify-between gap-3 pb-5 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center border border-blue-100">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-heading font-extrabold text-base text-slate-900">
                Biên Bản Khảo Thí Điện Tử
              </h3>
              <p className="text-xs text-slate-500 font-mono">
                Mã hóa xác thực bởi AIVES Proctor Gateway v4.8
              </p>
            </div>
          </div>

          <span className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-emerald-50 text-emerald-800 font-bold text-xs border border-emerald-200 shadow-2xs">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            Đã Lưu Hồ Sơ FAP
          </span>
        </div>

        {/* 4 Metadata Grid Boxes */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl space-y-1">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              Học Phần Khảo Thí
            </span>
            <p className="font-bold text-sm text-slate-900">SWD392 - Software Architecture &amp; Design</p>
            <p className="text-xs text-slate-500">Kỳ thi Final Viva • Fall 2026</p>
          </div>

          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl space-y-1">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              Thông Tin Thí Sinh
            </span>
            <p className="font-bold text-sm text-slate-900">{studentName}</p>
            <p className="text-xs text-slate-500 font-mono">MSSV: {studentCode} • Lớp: SE1701</p>
          </div>

          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl space-y-1">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              Giám Thị Ảo Phụ Trách
            </span>
            <p className="font-bold text-sm text-blue-600">AI Examiner Dr. Minh</p>
            <p className="text-xs text-slate-500">Phòng thi ảo: Oral Room #4</p>
          </div>

          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl space-y-1">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              Thời Gian &amp; Thời Lượng
            </span>
            <p className="font-bold text-sm text-slate-900">26 phút 40 giây</p>
            <p className="text-xs text-slate-500">Thời điểm nộp: 09:41 AM (GMT+7)</p>
          </div>
        </div>

        {/* 5 Completed Question Rounds */}
        <div className="space-y-3 pt-2">
          <h4 className="font-heading font-bold text-xs uppercase tracking-wider text-slate-900">
            5 Lượt Câu Hỏi Đã Hoàn Thành:
          </h4>

          <div className="space-y-2.5 text-xs">
            
            {/* Round 1 */}
            <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-2xl flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-7 h-7 rounded-xl bg-blue-100 text-blue-700 font-bold text-xs flex items-center justify-center shrink-0">
                  1
                </div>
                <div>
                  <p className="font-bold text-slate-900">Kiến trúc Củ hành (Onion Architecture) &amp; Phân tầng phụ thuộc</p>
                  <p className="text-[11px] text-slate-500">Thời gian trả lời: 02:45 • Âm thanh rõ nét</p>
                </div>
              </div>
              <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 font-bold text-[11px] border border-emerald-200 shrink-0">
                Đã ghi âm ✓
              </span>
            </div>

            {/* Round 2 */}
            <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-2xl flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-7 h-7 rounded-xl bg-blue-100 text-blue-700 font-bold text-xs flex items-center justify-center shrink-0">
                  2
                </div>
                <div>
                  <p className="font-bold text-slate-900">Phân biệt Monolith vs Microservices: Trade-off &amp; Blast Radius</p>
                  <p className="text-[11px] text-slate-500">Thời gian trả lời: 03:10 • Bao gồm 1 câu hỏi phụ thích ứng</p>
                </div>
              </div>
              <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 font-bold text-[11px] border border-emerald-200 shrink-0">
                Đã ghi âm ✓
              </span>
            </div>

            {/* Round 3 */}
            <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-2xl flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-7 h-7 rounded-xl bg-blue-100 text-blue-700 font-bold text-xs flex items-center justify-center shrink-0">
                  3
                </div>
                <div>
                  <p className="font-bold text-slate-900">Transactional Outbox Pattern &amp; Xử lý bất đồng bộ</p>
                  <p className="text-[11px] text-slate-500">Thời gian trả lời: 02:20 • Đạt chuẩn Bloom 4</p>
                </div>
              </div>
              <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 font-bold text-[11px] border border-emerald-200 shrink-0">
                Đã ghi âm ✓
              </span>
            </div>

            {/* Round 4 */}
            <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-2xl flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-7 h-7 rounded-xl bg-blue-100 text-blue-700 font-bold text-xs flex items-center justify-center shrink-0">
                  4
                </div>
                <div>
                  <p className="font-bold text-slate-900">Giao thức STOMP WebSockets &amp; Cửa sổ cứu vớt 60 giây</p>
                  <p className="text-[11px] text-slate-500">Thời gian trả lời: 02:50 • Âm thanh ổn định</p>
                </div>
              </div>
              <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 font-bold text-[11px] border border-emerald-200 shrink-0">
                Đã ghi âm ✓
              </span>
            </div>

            {/* Round 5 */}
            <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-2xl flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-7 h-7 rounded-xl bg-blue-100 text-blue-700 font-bold text-xs flex items-center justify-center shrink-0">
                  5
                </div>
                <div>
                  <p className="font-bold text-slate-900">Bảo mật RBAC theo khóa học &amp; Chống can thiệp dữ liệu điểm</p>
                  <p className="text-[11px] text-slate-500">Thời gian trả lời: 02:15 • Kết thúc bài thi</p>
                </div>
              </div>
              <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 font-bold text-[11px] border border-emerald-200 shrink-0">
                Đã ghi âm ✓
              </span>
            </div>

          </div>
        </div>

        {/* SHA-256 HASH FOOTER BAR */}
        <div className="bg-[#0b1428] text-white p-3.5 rounded-2xl flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2 font-mono text-[11px] truncate">
            <span className="text-cyan-400 font-bold">SHA-256:</span>
            <span className="text-slate-300 truncate">{sha256Hash}</span>
          </div>
          <button 
            type="button"
            onClick={handleCopyHash}
            className="text-xs font-semibold text-cyan-400 hover:text-cyan-300 flex items-center gap-1.5 transition cursor-pointer shrink-0"
          >
            {isCopied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{isCopied ? 'Đã Sao Chép' : 'Sao chép hash'}</span>
          </button>
        </div>

      </div>

      {/* BOTTOM ACTION BUTTONS */}
      <div className="flex flex-wrap items-center justify-center gap-4 pt-2">
        <button
          type="button"
          onClick={handlePrintCertificate}
          className="px-6 py-3 rounded-2xl bg-white border border-slate-200/90 hover:bg-slate-50 text-slate-800 font-bold text-xs sm:text-sm shadow-xs flex items-center gap-2 transition active:scale-95 cursor-pointer"
        >
          <FileText className="w-4 h-4 text-blue-600" />
          <span>Xem Biên Bản &amp; Chứng Nhận (In/Lưu)</span>
        </button>

        <button
          type="button"
          onClick={() => onNavigate && onNavigate('discover')}
          className="px-7 py-3 rounded-2xl bg-[#0066FF] hover:bg-[#0052CC] text-white font-bold text-xs sm:text-sm shadow-md shadow-blue-600/20 flex items-center gap-2 transition active:scale-95 cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay Về Trang Sinh Viên</span>
        </button>
      </div>

    </div>
  );
}
