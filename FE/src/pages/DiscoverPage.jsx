import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { oralExamApi } from '../api/oralExamApi';
import { getErrorMessage } from '../utils/errorCodes';
import { setActiveExam, gradeLabel } from '../utils/activeExam';
import {
  ArrowRight,
  Clock,
  ListChecks,
  Loader2,
  RotateCcw,
  CalendarX2
} from 'lucide-react';

// Attempt status (BE AttemptStatus) -> badge label + what the student can do next
const ATTEMPT_STATE = {
  null: { label: 'Chưa vào thi', tone: 'bg-sky-50 text-sky-800 border-sky-200', action: 'enter' },
  READY: { label: 'Đã vào phòng chờ', tone: 'bg-amber-50 text-amber-800 border-amber-200', action: 'resume' },
  IN_PROGRESS: { label: 'Đang thi', tone: 'bg-emerald-50 text-emerald-800 border-emerald-200', action: 'resume' },
  RECONNECTING: { label: 'Đang chờ kết nối lại', tone: 'bg-amber-50 text-amber-800 border-amber-200', action: 'resume' },
  FINALIZING: { label: 'Đang chấm điểm', tone: 'bg-violet-50 text-violet-800 border-violet-200', action: 'results' },
  COMPLETED: { label: 'Đã hoàn thành', tone: 'bg-emerald-50 text-emerald-800 border-emerald-200', action: 'results' },
  INTERRUPTED: { label: 'Bị gián đoạn', tone: 'bg-rose-50 text-rose-800 border-rose-200', action: null }
};

const ACTION_LABEL = {
  enter: 'Kiểm Tra Thiết Bị & Vào Thi',
  resume: 'Tiếp Tục Bài Thi',
  results: 'Xem Kết Quả'
};

export function DiscoverPage({ onNavigate, showToast }) {
  const { currentUser } = useAuth();
  const [exams, setExams] = useState([]);
  const [scores, setScores] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [enteringId, setEnteringId] = useState(null);

  const loadData = async () => {
    setIsLoading(true);
    setLoadError('');
    try {
      const [examList, scoreList] = await Promise.all([oralExamApi.listExams(), oralExamApi.getScores()]);
      setExams(Array.isArray(examList) ? examList : []);
      setScores(Array.isArray(scoreList) ? scoreList : []);
    } catch (err) {
      setLoadError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleExamAction = async (exam, action) => {
    if (action === 'results') {
      onNavigate?.('my-results');
      return;
    }

    let attemptId = exam.studentAttemptId;
    let status = exam.studentAttemptStatus;
    if (action === 'enter') {
      setEnteringId(exam.id);
      try {
        const attempt = await oralExamApi.enterExam(exam.id);
        attemptId = attempt.attemptId;
        status = attempt.status;
      } catch (err) {
        showToast?.({ type: 'error', title: 'Không Thể Vào Thi', message: getErrorMessage(err) });
        return;
      } finally {
        setEnteringId(null);
      }
    }

    setActiveExam({
      examId: exam.id,
      attemptId,
      title: exam.title,
      durationSeconds: exam.durationSeconds,
      questionCount: exam.questionCount
    });
    // A fresh attempt goes through the device check first; a running one goes straight back to the room
    onNavigate?.(status === 'READY' || !status ? 'calibration' : 'exam-room');
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto w-full animate-modal-entry text-ink pb-10">
      <header className="flex flex-wrap items-center justify-between gap-4 py-2">
        <div>
          <h1 className="font-heading font-extrabold text-2xl tracking-tight">Cổng Thi Vấn Đáp Trực Tuyến</h1>
          <p className="text-xs text-green-800 mt-1 font-medium">
            {currentUser?.fullName || 'Sinh viên'} • {currentUser?.userCode || ''} {currentUser?.email ? `• ${currentUser.email}` : ''}
          </p>
        </div>
        <button
          type="button"
          onClick={loadData}
          disabled={isLoading}
          className="px-3.5 py-1.5 rounded-xl bg-white border border-pink-200 text-xs font-semibold text-slate-700 flex items-center gap-1.5 hover:bg-slate-50 disabled:opacity-50 shadow-2xs cursor-pointer"
        >
          <RotateCcw className="w-3.5 h-3.5" /> Làm mới
        </button>
      </header>

      {loadError && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-2xl text-xs text-rose-800">{loadError}</div>
      )}

      {/* OPEN EXAMS */}
      <section className="space-y-3">
        <h2 className="font-heading font-bold text-sm">Ca thi đang mở</h2>
        {isLoading ? (
          [0, 1].map(i => <div key={i} className="h-32 rounded-3xl animate-pulse bg-slate-200/80" />)
        ) : exams.length === 0 ? (
          <div className="bg-white border border-pink-200 rounded-2xl p-10 text-center space-y-2 shadow-2xs">
            <CalendarX2 className="w-8 h-8 text-green-600 mx-auto" />
            <p className="font-bold text-sm">Hiện chưa có ca thi nào đang mở</p>
            <p className="text-xs text-green-800">Ca thi sẽ xuất hiện khi giảng viên mở kỳ thi cho môn học của bạn.</p>
          </div>
        ) : (
          exams.map((exam) => {
            const state = ATTEMPT_STATE[exam.studentAttemptStatus ?? 'null'] || ATTEMPT_STATE.null;
            return (
              <article key={exam.id} className="bg-white border border-pink-200 rounded-2xl p-6 shadow-2xs space-y-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <h3 className="font-heading font-extrabold text-xl tracking-tight">{exam.title}</h3>
                  <span className={`px-3 py-1 rounded-full text-xs font-bold border ${state.tone}`}>{state.label}</span>
                </div>
                <div className="flex flex-wrap gap-4 text-xs text-green-900 font-semibold">
                  <span className="flex items-center gap-1.5"><Clock className="w-4 h-4" /> {Math.round((exam.durationSeconds || 0) / 60)} phút</span>
                  <span className="flex items-center gap-1.5"><ListChecks className="w-4 h-4" /> {exam.questionCount} câu hỏi chính</span>
                </div>
                <div className="pt-3 border-t border-pink-100 flex justify-end">
                  {state.action ? (
                    <button
                      type="button"
                      onClick={() => handleExamAction(exam, state.action)}
                      disabled={enteringId === exam.id}
                      className="bg-green-600 hover:bg-green-700 text-white font-bold text-xs px-5 py-2.5 rounded-xl flex items-center gap-2 transition active:scale-[0.98] disabled:opacity-60 cursor-pointer focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2"
                    >
                      {enteringId === exam.id && <Loader2 className="w-4 h-4 animate-spin" />}
                      <span>{ACTION_LABEL[state.action]}</span>
                      <ArrowRight className="w-4 h-4" />
                    </button>
                  ) : (
                    <p className="text-xs text-pink-800 font-medium">Ca thi bị gián đoạn — vui lòng liên hệ giảng viên để được hỗ trợ.</p>
                  )}
                </div>
              </article>
            );
          })
        )}
      </section>

      {/* RECENT SCORES */}
      <section className="bg-white border border-pink-200 rounded-2xl overflow-hidden shadow-2xs">
        <div className="p-4 border-b border-pink-200 flex items-center justify-between">
          <h2 className="font-heading font-bold text-sm">Kết quả gần đây</h2>
          <button
            type="button"
            onClick={() => onNavigate?.('my-results')}
            className="text-xs font-bold text-pink-700 hover:text-pink-900 underline focus:outline-none focus:ring-2 focus:ring-green-600 rounded"
          >
            Xem tất cả
          </button>
        </div>
        {!isLoading && scores.length === 0 ? (
          <p className="p-6 text-center text-xs text-green-800">Bạn chưa hoàn thành ca thi nào.</p>
        ) : (
          <ul className="divide-y-2 divide-green-50 text-xs">
            {scores.slice(0, 3).map((s) => (
              <li key={s.attemptId} className="p-4 flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-bold truncate">{s.examTitle}</p>
                  <p className="text-xs text-green-800">{s.completedAt ? new Date(s.completedAt).toLocaleDateString('vi-VN') : ''}</p>
                </div>
                <div className="text-right shrink-0">
                  <p className="font-heading font-black text-lg text-pink-600 leading-none">{s.totalScore != null ? Number(s.totalScore) : '—'} / 10</p>
                  <p className="text-xs font-bold text-green-700">{gradeLabel(s.gradeLevel)}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
