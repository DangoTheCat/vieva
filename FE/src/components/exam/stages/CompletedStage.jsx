import React from 'react';
import { Award, Sparkles } from 'lucide-react';
import { toExamResult } from '../examRoomModel';

/** Oral exam room — screen shown for this stage. Extracted from OralExamRoomPage (plan item 2.12). */
export function CompletedStage({ attemptId, examResult, onNavigate, questions, studentCode, studentName }) {
  const displayResult = examResult || toExamResult(null);

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 animate-in fade-in duration-300">
      <div className="bg-white border border-pink-200 rounded-3xl p-6 sm:p-10 shadow-md space-y-8">
        
        {/* Header */}
        <div className="text-center space-y-3 pb-6 border-b border-pink-200">
          <div className="w-20 h-20 rounded-3xl bg-slate-100 border border-pink-200 text-slate-700 mx-auto flex items-center justify-center text-3xl shadow-sm">
            🎓
          </div>
          <div className="flex items-center justify-center gap-2">
            <span className="text-xs font-bold uppercase tracking-wider text-green-700 block">
              Báo Cáo Đánh Giá Khảo Thí Vấn Đáp • TRẠNG THÁI: COMPLETED
            </span>
            <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
              Đã lưu DB ✓
            </span>
          </div>
          <h1 className="font-heading font-black text-2xl sm:text-3xl text-ink">
            Kết Quả Bài Thi Vấn Đáp Trực Tuyến
          </h1>
          <p className="text-xs sm:text-sm text-green-800">
            Thí sinh: <strong>{studentName}</strong> • Mã SV: <strong>{studentCode}</strong> • Ca thi: <strong>#{attemptId}</strong>
          </p>
        </div>

        {/* Overall Score Box */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white border border-pink-200 p-5 rounded-2xl shadow-sm text-center">
            <span className="text-xs font-bold text-green-700 uppercase tracking-wider block mb-1">
              Điểm Tổng Kết
            </span>
            <div className="font-heading font-black text-4xl text-pink-600">
              {displayResult.totalScore != null ? displayResult.totalScore : '—'} <span className="text-lg text-ink font-sans">/ {displayResult.maxScore}</span>
            </div>
          </div>

          <div className="bg-white border border-pink-200 p-5 rounded-2xl shadow-sm text-center">
            <span className="text-xs font-bold text-green-700 uppercase tracking-wider block mb-1">
              Xếp Loại Đạt Được
            </span>
            <div className="font-heading font-black text-2xl sm:text-3xl text-green-700">
              {displayResult.grade}
            </div>
          </div>

          <div className="bg-white border border-pink-200 p-5 rounded-2xl shadow-sm text-center">
            <span className="text-xs font-bold text-green-700 uppercase tracking-wider block mb-1">
              Số Lượt Câu Hỏi
            </span>
            <div className="font-heading font-black text-2xl sm:text-3xl text-ink">
              {questions.length}
            </div>
          </div>
        </div>

        {displayResult.totalScore == null && (
          <div className="p-4 bg-amber-50 border-2 border-amber-300 rounded-2xl text-xs text-amber-900 font-medium">
            Bài thi đã được nộp và đang chờ chấm điểm. Điểm sẽ hiển thị tại Cổng Thi khi giảng viên/AI hoàn tất.
          </div>
        )}

        {/* AI Comprehensive Feedback */}
        {displayResult.feedback && (
          <div className="bg-white border border-pink-200 p-5 sm:p-6 rounded-2xl shadow-2xs space-y-2">
            <div className="flex items-center gap-2 text-pink-900 font-bold text-sm">
              <Sparkles className="w-5 h-5 text-pink-600" />
              <span>Nhận Xét Chuyên Môn Từ Giám Thị AI</span>
            </div>
            <p className="text-xs sm:text-sm text-pink-950 leading-relaxed font-medium">
              "{displayResult.feedback}"
            </p>
          </div>
        )}

        {/* Rubric Breakdown List */}
        <div className="space-y-4">
          <h3 className="font-heading font-black text-lg text-ink flex items-center gap-2">
            <Award className="w-5 h-5 text-green-700" />
            Chi Tiết Đánh Giá Theo Ma Trận Rubric Chuẩn Bloom
          </h3>

          <div className="space-y-3">
            {displayResult.rubrics.map((r, i) => (
              <div 
                key={i} 
                className="bg-white border border-pink-200 p-4 rounded-2xl shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div className="space-y-1">
                  <span className="font-bold text-xs text-ink block">{r.name}</span>
                  <p className="text-xs text-green-800">{r.comment}</p>
                </div>
                <div className="shrink-0 font-mono font-bold text-sm bg-slate-100 text-slate-800 border border-pink-200 px-3 py-1 rounded-xl self-start sm:self-center">
                  {r.score != null ? r.score : '—'} / {r.max}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Actions */}
        <div className="pt-6 border-t border-pink-200 flex flex-col sm:flex-row items-center justify-between gap-4">
          <button
            type="button"
            onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
            className="px-6 py-3 rounded-2xl bg-white hover:bg-green-50 text-green-900 font-bold text-xs border border-pink-200 shadow-sm cursor-pointer"
          >
            ← Trở Về Danh Sách Học Phần
          </button>

        </div>

      </div>

    </div>
  );
}
