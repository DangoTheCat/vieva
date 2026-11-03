import React from 'react';
import { ArrowLeft, CheckCircle2, Clock, MessageSquareText } from 'lucide-react';
import { toExamResult } from '../examRoomModel';

/** Oral exam room — screen shown for this stage. Extracted from OralExamRoomPage (plan item 2.12). */
export function CompletedStage({ activeExam, attemptId, examResult, onNavigate, questions, studentCode, studentName }) {
  const displayResult = examResult || toExamResult(null);
  const isPublished = displayResult.totalScore != null;

  return (
    <div className="max-w-3xl mx-auto py-10 px-4 animate-in fade-in slide-in-from-bottom-2 duration-500">
      <div className="bg-white border border-slate-200/90 rounded-2xl shadow-sm overflow-hidden">

        <header className="px-6 sm:px-10 pt-10 pb-8 text-center">
          <div className="w-14 h-14 rounded-full bg-emerald-50 text-emerald-600 mx-auto flex items-center justify-center">
            <CheckCircle2 className="w-7 h-7" strokeWidth={2.25} />
          </div>
          <h1 className="mt-5 font-heading font-extrabold text-2xl sm:text-[28px] tracking-tight text-ink">
            Bạn đã hoàn thành bài thi
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            {activeExam?.title || `Ca thi #${attemptId}`}
          </p>
          <p className="mt-1 text-xs text-slate-500">
            {studentName}<span className="mx-1.5 text-slate-300">•</span><span className="font-mono">{studentCode}</span>
          </p>
        </header>

        <dl className="grid grid-cols-3 border-y border-slate-200/90 divide-x divide-slate-200/90">
          <div className="px-4 py-5 text-center">
            <dt className="text-[11px] font-semibold text-slate-500">Điểm tổng kết</dt>
            <dd className="mt-1 font-heading font-extrabold text-2xl text-ink">
              {isPublished ? displayResult.totalScore : '—'}
              <span className="text-sm font-semibold text-slate-400"> / {displayResult.maxScore}</span>
            </dd>
          </div>
          <div className="px-4 py-5 text-center">
            <dt className="text-[11px] font-semibold text-slate-500">Xếp loại</dt>
            <dd className="mt-1 font-heading font-extrabold text-2xl text-ink">{isPublished ? displayResult.grade : '—'}</dd>
          </div>
          <div className="px-4 py-5 text-center">
            <dt className="text-[11px] font-semibold text-slate-500">Số câu hỏi</dt>
            <dd className="mt-1 font-heading font-extrabold text-2xl text-ink">{questions.length}</dd>
          </div>
        </dl>

        <div className="px-6 sm:px-10 py-8 space-y-6">
          {!isPublished && (
            <div className="flex items-start gap-3 rounded-xl bg-slate-50 border border-slate-200/90 p-4">
              <Clock className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
              <div className="text-sm">
                <p className="font-semibold text-ink">Đang chờ giảng viên chốt điểm</p>
                <p className="mt-0.5 text-slate-600 text-xs leading-relaxed">
                  Bài làm của bạn đã được ghi nhận. Điểm và nhận xét sẽ hiển thị tại mục Kết Quả Thi ngay khi giảng viên công bố.
                </p>
              </div>
            </div>
          )}

          {displayResult.feedback && (
            <section className="space-y-2">
              <h2 className="flex items-center gap-2 text-sm font-bold text-ink">
                <MessageSquareText className="w-4 h-4 text-slate-500" /> Nhận xét chung
              </h2>
              <p className="text-sm text-slate-700 leading-relaxed">{displayResult.feedback}</p>
            </section>
          )}

          {isPublished && displayResult.rubrics.length > 0 && (
            <section className="space-y-3">
              <h2 className="text-sm font-bold text-ink">Chi tiết theo tiêu chí</h2>
              <ul className="divide-y divide-slate-100 border border-slate-200/90 rounded-xl">
                {displayResult.rubrics.map((r, i) => (
                  <li key={i} className="flex items-start justify-between gap-4 px-4 py-3">
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-ink">{r.name}</p>
                      {r.comment && <p className="mt-0.5 text-xs text-slate-600 leading-relaxed">{r.comment}</p>}
                    </div>
                    <span className="shrink-0 font-mono text-sm font-semibold text-slate-800">
                      {r.score != null ? r.score : '—'}<span className="text-slate-400">/{r.max}</span>
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>

        <footer className="px-6 sm:px-10 py-5 bg-slate-50/70 border-t border-slate-200/90 flex justify-center">
          <button
            type="button"
            onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-ink text-white text-sm font-semibold hover:opacity-90 active:scale-[0.98] transition focus:outline-none focus:ring-2 focus:ring-sky-500 focus:ring-offset-2"
          >
            <ArrowLeft className="w-4 h-4" /> Về trang chủ
          </button>
        </footer>
      </div>
    </div>
  );
}
