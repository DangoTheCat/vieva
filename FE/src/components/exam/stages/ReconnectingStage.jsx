import React from 'react';
import { AlertTriangle, Clock, RefreshCw } from 'lucide-react';

/** Oral exam room — screen shown for this stage. Extracted from OralExamRoomPage (plan item 2.12). */
export function ReconnectingStage({ handleReconnect, isReconnecting, onNavigate, reconnectGraceLeft }) {
  return (
    <div className="max-w-3xl mx-auto py-10 px-4 animate-in fade-in zoom-in-95 duration-200">
      <div className="bg-amber-50 border border-amber-300 rounded-3xl p-6 sm:p-10 shadow-xl text-amber-950 space-y-6">
        
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-amber-100 border border-amber-300 text-amber-800 flex items-center justify-center text-2xl shadow-xs shrink-0">
            <AlertTriangle className="w-8 h-8 text-amber-900" />
          </div>
          <div>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-black uppercase tracking-wider bg-amber-200 text-amber-900 border border-amber-400 inline-block mb-1">
              TRẠNG THÁI: RECONNECTING • TẠM DỪNG ĐỒNG HỒ
            </span>
            <h2 className="font-heading font-black text-xl sm:text-2xl text-amber-950">
              Mất kết nối mạng! Đồng hồ thi tạm dừng. Đang thử kết nối lại... (Thời gian chờ tối đa 120s)
            </h2>
          </div>
        </div>

        <div className="bg-white/80 border-2 border-amber-500 rounded-2xl p-5 shadow-sm space-y-4">
          <div className="flex items-center justify-between text-xs sm:text-sm font-bold text-amber-900">
            <span className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-amber-700" />
              Thời gian ân hạn còn lại:
            </span>
            <span className="font-mono text-base font-black text-amber-950 bg-amber-200 px-3 py-1 rounded-xl border border-amber-400">
              {reconnectGraceLeft}s / 120s
            </span>
          </div>

          {/* Grace progress bar */}
          <div className="h-4 bg-amber-200 rounded-full border border-amber-400 overflow-hidden p-0.5">
            <div 
              className="h-full bg-amber-600 rounded-full transition-all duration-1000 ease-linear"
              style={{ width: `${(reconnectGraceLeft / 120) * 100}%` }}
            />
          </div>

          <p className="text-xs text-amber-900 leading-relaxed font-medium">
            Hệ thống đã tự động tạm dừng đồng hồ làm bài thi để bảo vệ thời gian của bạn. 
            Nếu bạn phục hồi kết nối trong vòng <strong>120 giây</strong>, bài thi sẽ được tiếp tục ngay tại câu hỏi hiện tại.
          </p>
        </div>

        {/* Action buttons */}
        <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-4">
          <button
            type="button"
            onClick={handleReconnect}
            disabled={isReconnecting}
            className="w-full sm:w-auto px-8 py-3.5 rounded-2xl bg-amber-500 hover:bg-amber-600 text-amber-950 font-black text-xs sm:text-sm border-2 border-amber-800 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isReconnecting ? 'animate-spin' : ''}`} />
            <span>{isReconnecting ? 'Đang Kiểm Tra Kết Nối...' : 'Thử Kết Nối Lại Ngay'}</span>
          </button>

          <button
            type="button"
            onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
            className="w-full sm:w-auto px-5 py-3 rounded-2xl bg-white hover:bg-amber-50 text-amber-900 font-bold text-xs border-2 border-amber-600 shadow-sm cursor-pointer"
          >
            Quay lại danh sách ca thi
          </button>
        </div>

      </div>
    </div>
  );
}
