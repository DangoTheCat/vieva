import React from 'react';
import { Copy, PhoneCall, ShieldAlert, X } from 'lucide-react';

/** Oral exam room — screen shown for this stage. Extracted from OralExamRoomPage (plan item 2.12). */
export function InterruptedStage({ attemptId, copiedAttempt, copyToClipboard, interruptedReason, onNavigate, setShowSupportModal, showSupportModal, studentCode }) {
  return (
    <div className="max-w-3xl mx-auto py-10 px-4 animate-in fade-in zoom-in-95 duration-200">
      <div className="bg-rose-50 border border-rose-600 rounded-2xl p-6 sm:p-10 shadow-md text-rose-950 space-y-6">
        
        <div className="flex items-center gap-4 pb-4 border-b-2 border-rose-200">
          <div className="w-16 h-16 rounded-2xl bg-rose-200 border border-rose-700 text-rose-800 flex items-center justify-center text-3xl shadow-sm shrink-0">
            <ShieldAlert className="w-8 h-8 text-rose-700" />
          </div>
          <div>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-black uppercase tracking-wider bg-rose-200 text-rose-900 border border-rose-400 inline-block mb-1">
              Bài thi bị gián đoạn
            </span>
            <h1 className="font-heading font-black text-xl sm:text-2xl text-rose-950">
              Ca thi bị gián đoạn do mất kết nối
            </h1>
          </div>
        </div>

        <div className="bg-white border border-rose-400 rounded-2xl p-5 shadow-sm space-y-3">
          <h3 className="font-bold text-sm text-rose-900">Chi tiết lý do gián đoạn:</h3>
          <p className="text-xs sm:text-sm text-rose-950 leading-relaxed font-medium">
            {interruptedReason || 'Ca thi bị gián đoạn do mất kết nối mạng quá thời gian cho phép (120 giây) hoặc hệ thống chưa thể hoàn tất việc lưu bài.'}
          </p>
          <div className="p-3 bg-rose-100/60 border border-rose-300 rounded-xl text-xs text-rose-900">
            <strong>Dữ liệu của bạn được an toàn:</strong> các câu trả lời đã nộp trước thời điểm gián đoạn đều đã được lưu lại.
          </div>
        </div>

        {/* Technical Attempt Reference */}
        <div className="flex flex-wrap items-center justify-between gap-2 p-3 bg-rose-100 border border-rose-300 rounded-xl text-xs font-mono text-rose-900">
          <span>Mã lượt thi: #{attemptId}</span>
          <button
            type="button"
            onClick={() => copyToClipboard(attemptId)}
            className="flex items-center gap-1 font-bold text-rose-800 hover:text-rose-950 cursor-pointer underline"
          >
            <Copy className="w-3.5 h-3.5" />
            <span>{copiedAttempt ? 'Đã sao chép' : 'Sao chép mã'}</span>
          </button>
        </div>

        {/* Action buttons */}
        <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-4">
          <button
            type="button"
            onClick={() => setShowSupportModal(true)}
            className="w-full sm:w-auto px-6 py-3.5 rounded-2xl bg-rose-600 hover:bg-rose-700 text-white font-black text-xs sm:text-sm border border-rose-900 shadow-sm active:scale-[0.98] transition cursor-pointer flex items-center justify-center gap-2"
          >
            <PhoneCall className="w-4 h-4" />
            <span>Liên hệ hỗ trợ</span>
          </button>

          <button
            type="button"
            onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
            className="w-full sm:w-auto px-6 py-3 rounded-2xl bg-white hover:bg-rose-50 text-rose-900 font-bold text-xs border border-rose-500 shadow-sm cursor-pointer"
          >
            Quay lại danh sách ca thi
          </button>
        </div>

      </div>

      {/* Support Modal dialog */}
      {showSupportModal && (
        <div className="fixed inset-0 z-50 bg-ink/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="bg-paper border border-rose-600 rounded-2xl p-6 sm:p-8 max-w-md w-full shadow-md space-y-5 text-ink">
            <div className="flex items-center justify-between pb-3 border-b-2 border-rose-200">
              <div className="flex items-center gap-2 font-heading font-black text-lg text-rose-950">
                <PhoneCall className="w-5 h-5 text-rose-600" />
                <span>Kênh Hỗ Trợ Khảo Thí</span>
              </div>
              <button
                type="button"
                onClick={() => setShowSupportModal(false)}
                className="p-1 rounded-lg hover:bg-rose-100 text-rose-800 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs sm:text-sm text-rose-950">
              <div className="p-3 bg-white border border-rose-300 rounded-xl space-y-1">
                <span className="text-xs font-bold text-rose-700 uppercase tracking-wider block">Hotline Trực Tiếp Hội Đồng Thi</span>
                <p className="font-heading font-black text-base text-rose-900">1900-8888 (Nhánh 2)</p>
                <span className="text-xs text-green-800 block">Thời gian trực: 07:30 - 21:00 các ngày thi</span>
              </div>

              <div className="p-3 bg-white border border-rose-300 rounded-xl space-y-1">
                <span className="text-xs font-bold text-rose-700 uppercase tracking-wider block">Email Cán Bộ Coi Thi Trực Tuyến</span>
                <p className="font-mono font-bold text-xs text-rose-900">proctor-support@vieva.edu.vn</p>
              </div>

              <div className="p-3 bg-rose-50 border border-rose-300 rounded-xl text-xs text-rose-900">
                Vui lòng cung cấp <strong>Mã SV: {studentCode}</strong> và <strong>Mã Ca Thi: #{attemptId}</strong> khi liên hệ để được hỗ trợ mở lại ca thi ân hạn.
              </div>
            </div>

            <button
              type="button"
              onClick={() => setShowSupportModal(false)}
              className="w-full py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs border border-rose-900 shadow-sm cursor-pointer"
            >
              Đã hiểu &amp; Đóng
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
