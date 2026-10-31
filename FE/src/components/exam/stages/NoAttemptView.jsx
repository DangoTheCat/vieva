import React from 'react';
import { BookOpen } from 'lucide-react';

/** Oral exam room — screen shown for this stage. Extracted from OralExamRoomPage (plan item 2.12). */
export function NoAttemptView({ onNavigate }) {
  return (
    <div className="max-w-md mx-auto py-12 px-4">
      <div className="bg-white border border-pink-200 rounded-3xl p-8 shadow-md text-center space-y-4">
        <BookOpen className="w-10 h-10 text-green-700 mx-auto" />
        <h2 className="font-heading font-black text-xl text-ink">Chưa chọn ca thi</h2>
        <p className="text-xs text-green-800">Hãy chọn một ca thi đang mở tại Cổng Thi để vào phòng thi vấn đáp.</p>
        <button
          type="button"
          onClick={() => onNavigate?.('discover')}
          className="px-6 py-2.5 rounded-xl bg-green-500 hover:bg-green-600 text-white font-bold text-xs border border-pink-200 cursor-pointer focus:outline-none focus:ring-2 focus:ring-green-600"
        >
          Về Cổng Thi
        </button>
      </div>
    </div>
  );
}
