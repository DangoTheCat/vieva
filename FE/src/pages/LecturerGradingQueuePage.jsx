import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Select } from '../components/common/Select';
import { lecturerGradingApi } from '../api/lecturerGradingApi';
import { getErrorMessage } from '../utils/errorCodes';
import { isProbeTurn } from '../utils/activeExam';
import { Users, CheckCircle2, Clock, Search, X, Loader2, BarChart3, ClipboardCheck, Lock, EyeOff } from 'lucide-react';

const ATTEMPT_LABEL = {
  READY: 'Chờ bắt đầu',
  IN_PROGRESS: 'Đang thi',
  RECONNECTING: 'Mất kết nối',
  FINALIZING: 'Đang chấm AI',
  COMPLETED: 'Đã nộp',
  INTERRUPTED: 'Gián đoạn'
};

const fmt = (n) => (n != null ? String(Number(Number(n).toFixed(2))) : '—');
const ghostBtn = 'px-3 py-1.5 rounded-lg bg-white border border-pink-200 hover:bg-slate-50 text-xs font-semibold text-slate-700 transition shadow-2xs cursor-pointer';
const secondaryBtn = 'px-4 py-2 rounded-xl bg-white border border-pink-200 hover:bg-slate-50 text-xs font-bold text-slate-700 flex items-center gap-1.5 shadow-2xs disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2';

function StatTile({ icon: Icon, label, value }) {
  return (
    <div className="bg-white border border-pink-200 rounded-xl p-4 shadow-2xs">
      <div className="flex items-center gap-2 text-[11px] font-bold text-green-700 uppercase tracking-wide"><Icon className="w-4 h-4" /> {label}</div>
      <p className="font-heading font-black text-2xl mt-1">{value}</p>
    </div>
  );
}

/**
 * Grading workspace: subject -> exam -> participants -> per-question teacher grading.
 */
export function LecturerGradingQueuePage({ showToast }) {
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [exams, setExams] = useState([]);
  const [examId, setExamId] = useState('');
  const [stats, setStats] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  const [attempt, setAttempt] = useState(null);
  const [isLoadingAttempt, setIsLoadingAttempt] = useState(false);
  const [drafts, setDrafts] = useState({}); // questionId -> { score, feedback }
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [confirmPublish, setConfirmPublish] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const data = await lecturerGradingApi.getCourses();
        const list = Array.isArray(data) ? data : [];
        setCourses(list);
        if (list.length > 0) setCourseId(list[0].subjectId);
        else setIsLoading(false);
      } catch (err) {
        setIsLoading(false);
        showToast({ type: 'error', message: getErrorMessage(err) });
      }
    })();
  }, []);

  useEffect(() => {
    if (!courseId) return;
    (async () => {
      setIsLoading(true);
      try {
        const list = await lecturerGradingApi.getCourseExams(courseId);
        const arr = Array.isArray(list) ? list : [];
        setExams(arr);
        setExamId(arr[0]?.id || '');
        if (arr.length === 0) {
          setParticipants([]);
          setStats(null);
          setIsLoading(false);
        }
      } catch (err) {
        setExams([]);
        setIsLoading(false);
        showToast({ type: 'error', message: getErrorMessage(err) });
      }
    })();
  }, [courseId]);

  const loadParticipants = async (id = examId) => {
    if (!id) return;
    setIsLoading(true);
    try {
      const [list, examStats] = await Promise.all([
        lecturerGradingApi.getParticipants(id),
        lecturerGradingApi.getExamStats(id)
      ]);
      setParticipants(Array.isArray(list) ? list : []);
      setStats(examStats);
    } catch (err) {
      setParticipants([]);
      setStats(null);
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadParticipants(examId);
  }, [examId]);

  const applyAttempt = (detail) => {
    setAttempt(detail);
    setConfirmPublish(false);
    const next = {};
    (detail?.questions || []).forEach(q => {
      next[q.questionId] = {
        score: q.teacherScore != null ? String(q.teacherScore) : '',
        feedback: q.teacherFeedback || ''
      };
    });
    setDrafts(next);
  };

  const openAttempt = async (p) => {
    setIsLoadingAttempt(true);
    setAttempt({ attemptId: p.attemptId, studentName: p.studentName, questions: [] });
    try {
      applyAttempt(await lecturerGradingApi.getAttempt(p.attemptId));
    } catch (err) {
      setAttempt(null);
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsLoadingAttempt(false);
    }
  };

  // Questions whose score/feedback draft differs from what the server last returned
  const changedQuestions = (attempt?.questions || []).filter(qn => {
    const d = drafts[qn.questionId] || {};
    const origScore = qn.teacherScore != null ? String(qn.teacherScore) : '';
    return (d.score ?? '') !== origScore || (d.feedback ?? '').trim() !== (qn.teacherFeedback || '').trim();
  });
  const isDirty = changedQuestions.length > 0;

  const submitGrades = async () => {
    for (const qn of changedQuestions) {
      const d = drafts[qn.questionId] || {};
      const score = Number(d.score);
      if (d.score === '' || !Number.isFinite(score) || score < 0 || score > 10) {
        showToast({ type: 'warning', message: `Câu ${qn.orderIndex}: điểm phải nằm trong khoảng 0 – 10.` });
        return;
      }
    }
    setIsSubmitting(true);
    try {
      let updated = null;
      for (const qn of changedQuestions) {
        const d = drafts[qn.questionId];
        updated = await lecturerGradingApi.gradeQuestion(attempt.attemptId, qn.questionId, { score: Number(d.score), feedback: d.feedback?.trim() || null });
      }
      if (updated) applyAttempt(updated);
      showToast({ type: 'success', message: `Đã cập nhật điểm chấm cho ${changedQuestions.length} câu hỏi.` });
      loadParticipants();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSubmitting(false);
    }
  };

  const closeAttempt = () => {
    if (isDirty && !window.confirm('Bạn có thay đổi điểm chưa cập nhật. Đóng và bỏ các thay đổi này?')) return;
    setAttempt(null);
  };

  // Lecturer finalizes the grade; the student only sees the score after this
  const publishScore = async () => {
    if (!attempt?.attemptId) return;
    setIsPublishing(true);
    try {
      applyAttempt(await lecturerGradingApi.publishScore(attempt.attemptId));
      showToast({ type: 'success', message: 'Đã chốt điểm. Sinh viên đã có thể xem kết quả.' });
      loadParticipants();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsPublishing(false);
      setConfirmPublish(false);
    }
  };

  const canPublish = attempt?.status === 'COMPLETED' && attempt?.aiScore != null && !attempt?.scorePublished && !isDirty;
  const primaryBtn = 'px-4 py-2 rounded-xl bg-green-600 hover:bg-green-700 active:scale-[0.98] text-white text-xs font-bold flex items-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2';

  const q = searchTerm.toLowerCase().trim();
  const filtered = participants.filter(p =>
    !q || [p.studentName, p.studentCode, p.studentEmail].some(v => v?.toLowerCase().includes(q))
  );

  return (
    <div className="max-w-6xl mx-auto w-full space-y-6 pb-10 text-ink">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-heading font-extrabold text-2xl tracking-tight">Chấm Điểm Bài Thi</h1>
          <p className="text-xs text-green-800 mt-1">Xem bài làm của thí sinh, điểm AI và chấm lại từng câu hỏi.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Select value={courseId} onChange={e => setCourseId(e.target.value)} aria-label="Môn học"
            className="bg-white border border-pink-200 rounded-xl p-2 text-xs font-semibold text-slate-800 focus:outline-none focus:border-pink-500 shadow-2xs">
            {courses.length === 0 && <option value="">Chưa được phân công môn học</option>}
            {courses.map(c => <option key={c.subjectId} value={c.subjectId}>{c.subjectCode} — {c.subjectName}</option>)}
          </Select>
          <Select value={examId} onChange={e => setExamId(e.target.value)} aria-label="Kỳ thi" disabled={exams.length === 0}
            className="bg-white border border-pink-200 rounded-xl p-2 text-xs font-semibold text-slate-800 focus:outline-none focus:border-pink-500 shadow-2xs disabled:opacity-50">
            {exams.length === 0 && <option value="">Chưa có kỳ thi</option>}
            {exams.map(e => <option key={e.id} value={e.id}>{e.title}</option>)}
          </Select>
        </div>
      </header>

      {stats && (
        <section className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatTile icon={Users} label="Thí sinh" value={stats.totalParticipants} />
          <StatTile icon={CheckCircle2} label="Đã nộp" value={stats.submittedCount} />
          <StatTile icon={Clock} label="Chưa xong" value={stats.pendingCount} />
          <StatTile icon={BarChart3} label="Điểm TB" value={fmt(stats.averageScore)} />
        </section>
      )}

      <section className="bg-white border border-pink-200 rounded-2xl shadow-2xs overflow-hidden">
        <div className="p-4 border-b border-pink-200">
          <div className="relative max-w-sm">
            <Search className="w-4 h-4 text-green-700 absolute left-3 top-2.5" />
            <input value={searchTerm} onChange={e => setSearchTerm(e.target.value)} placeholder="Tìm theo tên, MSSV, email..."
              className="w-full bg-white border border-pink-200 rounded-xl p-2 pl-9 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-pink-500 shadow-2xs" />
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-pink-200 text-slate-700 font-semibold uppercase text-[10px] tracking-wider">
              <tr>
                <th className="py-3 px-4">Thí sinh</th>
                <th className="py-3 px-4">Trạng thái</th>
                <th className="py-3 px-4">Điểm AI</th>
                <th className="py-3 px-4">Điểm GV</th>
                <th className="py-3 px-4">Điểm tổng</th>
                <th className="py-3 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-green-100">
              {isLoading ? (
                [0, 1, 2].map(i => <tr key={i}><td colSpan={6} className="p-3"><div className="h-8 rounded-lg animate-pulse bg-slate-200/80" /></td></tr>)
              ) : filtered.length === 0 ? (
                <tr><td colSpan={6} className="p-10 text-center text-green-800">
                  <ClipboardCheck className="w-8 h-8 text-green-600 mx-auto mb-2" />
                  {examId ? 'Chưa có thí sinh nào tham gia kỳ thi này.' : 'Chọn một kỳ thi để xem danh sách thí sinh.'}
                </td></tr>
              ) : filtered.map(p => (
                <tr key={p.attemptId} className="hover:bg-green-50/50">
                  <td className="py-3 px-4">
                    <p className="font-bold">{p.studentName}</p>
                    <p className="text-[11px] text-green-800">{p.studentCode} • {p.studentEmail}</p>
                  </td>
                  <td className="py-3 px-4">{ATTEMPT_LABEL[p.status] || p.status}</td>
                  <td className="py-3 px-4 font-mono">{fmt(p.aiScore)}</td>
                  <td className="py-3 px-4 font-mono">{fmt(p.teacherScore)}</td>
                  <td className="py-3 px-4 font-mono font-black text-pink-700">
                    {fmt(p.overrideTotalScore ?? p.totalScore)}
                    {p.overrideTotalScore != null && <span className="ml-1 text-[10px] text-amber-700 font-sans">(đã điều chỉnh)</span>}
                    {p.status === 'COMPLETED' && (
                      p.scorePublished
                        ? <span className="ml-1 text-[10px] text-green-700 font-sans font-bold">(đã chốt)</span>
                        : <span className="ml-1 text-[10px] text-amber-700 font-sans font-bold">(chưa chốt)</span>
                    )}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <button type="button" onClick={() => openAttempt(p)} className={ghostBtn}>Chấm bài</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {attempt && createPortal(
        <div className="fixed inset-0 z-50 bg-ink/50 backdrop-blur-xs flex items-center justify-center p-4 sm:p-6" role="dialog" aria-modal="true" aria-labelledby="grading-title">
          <div className="bg-white w-full max-w-4xl max-h-[92vh] flex flex-col rounded-2xl border border-pink-200 shadow-2xl overflow-hidden">
            <div className="shrink-0 px-6 py-4 border-b border-pink-200 flex items-start justify-between gap-3">
              <div className="min-w-0">
                <h2 id="grading-title" className="font-heading font-black text-lg truncate">{attempt.studentName}</h2>
                <p className="text-[11px] text-green-800">
                  {[attempt.examTitle, `Điểm AI ${fmt(attempt.aiScore)}`, `Điểm GV ${fmt(attempt.teacherScore)}`, `Tổng ${fmt(attempt.overrideTotalScore ?? attempt.totalScore)}`].filter(Boolean).join(' • ')}
                </p>
                {!isLoadingAttempt && attempt.status === 'COMPLETED' && (
                  attempt.scorePublished ? (
                    <p className="mt-1.5 inline-flex items-center gap-1 text-[11px] font-bold text-green-800 bg-green-50 border border-green-200 rounded-lg px-2 py-0.5">
                      <Lock className="w-3.5 h-3.5" /> Đã chốt điểm{attempt.publishedAt ? ` lúc ${new Date(attempt.publishedAt).toLocaleString('vi-VN')}` : ''} • Sinh viên đã xem được
                    </p>
                  ) : (
                    <p className="mt-1.5 inline-flex items-center gap-1 text-[11px] font-bold text-amber-800 bg-amber-50 border border-amber-200 rounded-lg px-2 py-0.5">
                      <EyeOff className="w-3.5 h-3.5" /> Chưa chốt điểm • Sinh viên chưa thấy điểm
                    </p>
                  )
                )}
              </div>
              <button type="button" onClick={closeAttempt} aria-label="Đóng" className="p-1 rounded-lg hover:bg-green-100 focus:outline-none focus:ring-2 focus:ring-green-600"><X className="w-5 h-5" /></button>
            </div>

            <div className="flex-1 overflow-y-auto px-6 py-5 space-y-4 text-xs bg-slate-50/60">
              {isLoadingAttempt ? (
                <div className="space-y-3">{[0, 1].map(i => <div key={i} className="h-40 rounded-xl animate-pulse bg-slate-200/80" />)}</div>
              ) : (attempt.questions || []).length === 0 ? (
                <p className="text-center text-green-800 py-10">Bài làm chưa có câu hỏi nào.</p>
              ) : (attempt.questions || []).map(qn => {
                const changed = changedQuestions.some(c => c.questionId === qn.questionId);
                return (
                  <article key={qn.questionId} className={`bg-white border rounded-xl p-4 space-y-3 shadow-2xs ${changed ? 'border-amber-400' : 'border-pink-200'}`}>
                    <div className="flex items-start justify-between gap-3">
                      <p className="font-bold leading-relaxed">Câu {qn.orderIndex}. {qn.questionText}</p>
                      <span className="shrink-0 font-mono font-bold bg-slate-100 border border-pink-200 text-slate-800 rounded-lg px-2 py-0.5">{fmt(qn.effectiveScore)} / 10</span>
                    </div>
                    {qn.expectedAnswer && (
                      <p className="text-green-900 bg-green-50 rounded-lg p-3 leading-relaxed whitespace-pre-line"><strong>Đáp án tham khảo:</strong> {qn.expectedAnswer}</p>
                    )}
                    <ol className="space-y-3">
                      {(qn.turns || []).map(t => (
                        <li key={t.turnId} className="border-l-4 border-pink-300 pl-3 space-y-1 leading-relaxed">
                          <p className="text-[11px] font-bold text-pink-800">{isProbeTurn(t.turnType) ? 'Câu hỏi xoáy' : 'Câu hỏi chính'} • AI: {fmt(t.score)}</p>
                          {isProbeTurn(t.turnType) && <p className="italic text-slate-700">{t.questionText}</p>}
                          <p><strong>Trả lời:</strong> {t.transcriptText || <span className="text-slate-500 italic">(chưa trả lời)</span>}</p>
                          {t.feedback && <p className="text-green-800"><strong>Nhận xét AI:</strong> {t.feedback}</p>}
                        </li>
                      ))}
                    </ol>
                    <div className="grid sm:grid-cols-[140px_1fr] gap-3 items-start pt-3 border-t border-pink-100">
                      <label className="space-y-1">
                        <span className="block text-[11px] font-bold text-slate-700">Điểm GV (0 – 10)</span>
                        <input type="number" min="0" max="10" step="0.25" placeholder="Giữ điểm AI"
                          value={drafts[qn.questionId]?.score ?? ''}
                          onChange={e => setDrafts(d => ({ ...d, [qn.questionId]: { ...d[qn.questionId], score: e.target.value } }))}
                          className="w-full bg-white border border-pink-200 rounded-xl p-2 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-pink-500 focus:ring-2 focus:ring-pink-200 shadow-2xs" />
                      </label>
                      <label className="space-y-1">
                        <span className="block text-[11px] font-bold text-slate-700">Nhận xét của giảng viên <span className="font-normal text-slate-500">(tuỳ chọn)</span></span>
                        <textarea rows={2} placeholder="Nhập nhận xét cho câu trả lời này..."
                          value={drafts[qn.questionId]?.feedback ?? ''}
                          onChange={e => setDrafts(d => ({ ...d, [qn.questionId]: { ...d[qn.questionId], feedback: e.target.value } }))}
                          className="w-full resize-y bg-white border border-pink-200 rounded-xl p-2 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-pink-500 focus:ring-2 focus:ring-pink-200 shadow-2xs" />
                      </label>
                    </div>
                  </article>
                );
              })}
            </div>

            {!isLoadingAttempt && (
              <div className="shrink-0 bg-white border-t border-pink-200 px-6 py-4 flex flex-wrap items-center justify-between gap-3 text-xs">
                <p className="text-slate-600 max-w-md">
                  {isDirty
                    ? `Có ${changedQuestions.length} câu đã thay đổi nhưng chưa được cập nhật.`
                    : attempt.scorePublished
                      ? 'Điểm đã được chốt. Nếu điều chỉnh, kết quả của sinh viên sẽ được cập nhật theo.'
                      : attempt.status !== 'COMPLETED'
                        ? 'Thí sinh chưa nộp bài, chưa thể chốt điểm.'
                        : attempt.aiScore == null
                          ? 'AI chưa chấm xong bài này, chưa thể chốt điểm.'
                          : 'Kiểm tra và chấm lại từng câu nếu cần, sau đó chốt điểm để công bố cho sinh viên.'}
                </p>
                <div className="flex items-center gap-2">
                  {confirmPublish ? (
                    <>
                      <button type="button" onClick={() => setConfirmPublish(false)} disabled={isPublishing} className={ghostBtn}>Huỷ</button>
                      <button type="button" onClick={publishScore} disabled={isPublishing} className={primaryBtn}>
                        {isPublishing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Lock className="w-4 h-4" />} Xác nhận chốt {fmt(attempt.overrideTotalScore ?? attempt.totalScore)} điểm
                      </button>
                    </>
                  ) : (
                    <>
                      <button type="button" onClick={submitGrades} disabled={!isDirty || isSubmitting} className={isDirty ? primaryBtn : secondaryBtn}>
                        {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <ClipboardCheck className="w-4 h-4" />} Cập nhật điểm chấm
                      </button>
                      {attempt.status === 'COMPLETED' && !attempt.scorePublished && (
                        <button type="button" onClick={() => setConfirmPublish(true)} disabled={!canPublish || isSubmitting} className={primaryBtn}
                          title={isDirty ? 'Hãy cập nhật điểm chấm trước khi chốt' : undefined}>
                          <Lock className="w-4 h-4" /> Chốt điểm
                        </button>
                      )}
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
