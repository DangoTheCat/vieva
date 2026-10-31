import React from 'react';
import { Loader2 } from 'lucide-react';

/** Oral exam room — screen shown for this stage. Extracted from OralExamRoomPage (plan item 2.12). */
export function FinalizingStage({ finalizingStep, transcript }) {
  return (
    <div className="fixed inset-0 z-50 bg-ink/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-300">
      <div className="bg-white border border-pink-200 rounded-3xl p-8 sm:p-10 max-w-xl w-full shadow-lg space-y-6 text-center text-ink">
        
        <div className="w-20 h-20 rounded-3xl bg-slate-100 border border-pink-200 text-slate-700 mx-auto flex items-center justify-center text-3xl shadow-sm animate-pulse">
          <Loader2 className="w-10 h-10 text-pink-600 animate-spin" />
        </div>

        <div className="space-y-2">
          <span className="text-xs font-black uppercase tracking-wider text-green-700 block">
            Trạng Thái Ca Thi: FINALIZING
          </span>
          <h2 className="font-heading font-black text-xl sm:text-2xl text-ink">
            Đang đồng bộ dữ liệu và Giám thị AI đang chấm điểm đa tầng...
          </h2>
          <p className="text-xs sm:text-sm text-green-800">
            Vui lòng giữ nguyên cửa sổ trình duyệt. Hệ thống đang tiến hành bóc băng, mã hóa SHA-256 và chấm điểm theo khung Rubric chuẩn Bloom.
          </p>
        </div>

        {/* Synchronizing 3 Steps Progress */}
        <div className="space-y-3 text-left">
          <div className={`p-3.5 rounded-2xl border-2 flex items-center justify-between text-xs transition ${
            finalizingStep >= 1 ? 'bg-white border-slate-900 text-ink font-semibold shadow-2xs' : 'bg-slate-50 border-pink-200 text-slate-400'
          }`}>
            <div className="flex items-center gap-2">
              <span className={`w-5 h-5 rounded-full flex items-center justify-center text-xs ${
                finalizingStep > 1 ? 'bg-emerald-600 text-white' : 'bg-pink-600 text-white'
              }`}>
                {finalizingStep > 1 ? '✓' : <Loader2 className="w-3 h-3 animate-spin" />}
              </span>
              <span>1. Đóng gói transcript &amp; bản ghi âm các lượt vấn đáp</span>
            </div>
            <span className="text-xs text-green-700">
              {finalizingStep > 1 ? 'Hoàn tất ✓' : 'Đang xử lý...'}
            </span>
          </div>

          <div className={`p-3.5 rounded-2xl border-2 flex items-center justify-between text-xs transition ${
            finalizingStep >= 2 ? 'bg-white border-slate-900 font-semibold shadow-sm' : 'bg-gray-100 border-gray-300 text-gray-500'
          }`}>
            <div className="flex items-center gap-2">
              <span className={`w-5 h-5 rounded-full flex items-center justify-center text-xs ${
                finalizingStep > 2 ? 'bg-emerald-600 text-white' : finalizingStep === 2 ? 'bg-pink-600 text-white' : 'bg-slate-200 text-slate-600'
              }`}>
                {finalizingStep > 2 ? '✓' : finalizingStep === 2 ? <Loader2 className="w-3 h-3 animate-spin" /> : '2'}
              </span>
              <span>2. Đồng bộ hóa dữ liệu với máy chủ khảo thí trung tâm</span>
            </div>
            <span className="text-xs text-green-700">
              {finalizingStep > 2 ? 'Hoàn tất ✓' : finalizingStep === 2 ? 'Đang đồng bộ...' : 'Chờ...'}
            </span>
          </div>

          <div className={`p-3.5 rounded-2xl border-2 flex items-center justify-between text-xs transition ${
            finalizingStep >= 3 ? 'bg-white border-slate-900 font-semibold shadow-sm' : 'bg-gray-100 border-gray-300 text-gray-500'
          }`}>
            <div className="flex items-center gap-2">
              <span className={`w-5 h-5 rounded-full flex items-center justify-center text-xs ${
                finalizingStep === 3 ? 'bg-pink-600 text-white' : 'bg-slate-200 text-slate-600'
              }`}>
                {finalizingStep === 3 ? <Loader2 className="w-3 h-3 animate-spin" /> : '3'}
              </span>
              <span>3. Hội đồng Giám thị AI chấm điểm đa tầng &amp; lập biên bản Rubric</span>
            </div>
            <span className="text-xs text-green-700">
              {finalizingStep === 3 ? 'Đang chấm điểm...' : 'Chờ...'}
            </span>
          </div>
        </div>

        <div className="pt-2 text-center text-xs text-green-800 font-mono">
          * Phiên thi được bảo mật toàn vẹn qua Transactional Outbox Pattern
        </div>

      </div>
    </div>
  );
}
