import React, { useState, useEffect } from 'react';
import { lecturerGradingApi } from '../api/lecturerGradingApi';
import { getErrorMessage } from '../utils/errorCodes';
import { Users, CheckCircle2, Clock, Search, X, Loader2, BarChart3, Save, ClipboardCheck } from 'lucide-react';

const ATTEMPT_LABEL = {
  READY: 'Chờ bắt đầu',
  IN_PROGRESS: 'Đang thi',
  RECONNECTING: 'Mất kết nối',
  FINALIZING: 'Đang chấm AI',
  COMPLETED: 'Đã nộp',
  INTERRUPTED: 'Gián đoạn'
};

const fmt = (n) => (n != null ? Number(n).toFixed(2).replace(/\.00$/, '') : '—');
const ghostBtn = 'px-3 py-1.5 rounded-xl bg-white border-2 border-green-300 hover:bg-green-50 text-xs font-bold transition focus:outline-none focus:ring-2 focus:ring-green-600';

function StatTile({ icon: Icon, label, value }) {
  return (
    <div className="bg-white border-2 border-green-400 rounded-2xl p-4 shadow-[3px_3px_0_#DEB9B5]">
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
  const [savingQuestionId, setSavingQuestionId] = useState(null);

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

  const saveGrade = async (questionId) => {
    const draft = drafts[questionId] || {};
    const score = Number(draft.score);
    if (draft.score === '' || !Number.isFinite(score) || score < 0 || score > 10) {
      showToast({ type: 'warning', message: 'Điểm phải nằm trong khoảng 0 – 10.' });
      return;
    }
    setSavingQuestionId(questionId);
    try {
      const updated = await lecturerGradingApi.gradeQuestion(attempt.attemptId, questionId, { score, feedback: draft.feedback?.trim() || null });
      applyAttempt(updated);
      showToast({ type: 'success', message: 'Đã lưu điểm giảng viên cho câu hỏi.' });
      loadParticipants();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setSavingQuestionId(null);
    }
  };

  const q = searchTerm.toLowerCase().trim();
  const filtered = participants.filter(p =>
    !q || [p.studentName, p.studentCode, p.studentEmail].some(v => v?.toLowerCase().includes(q))
  );

  return (
    <div className="max-w-6xl mx-auto w-full space-y-6 pb-10 text-ink animate-modal-entry">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-heading font-extrabold text-2xl tracking-tight">Chấm Điểm Bài Thi</h1>
          <p className="text-xs text-green-800 mt-1">Xem bài làm của thí sinh, điểm AI và chấm lại từng câu hỏi.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <select value={courseId} onChange={e => setCourseId(e.target.value)} aria-label="Môn học"
            className="bg-white border-2 border-green-300 rounded-xl p-2 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-green-600">
            {courses.length === 0 && <option value="">Chưa được phân công môn học</option>}
            {courses.map(c => <option key={c.subjectId} value={c.subjectId}>{c.subjectCode} — {c.subjectName}</option>)}
          </select>
          <select value={examId} onChange={e => setExamId(e.target.value)} aria-label="Kỳ thi" disabled={exams.length === 0}
            className="bg-white border-2 border-green-300 rounded-xl p-2 text-xs font-bold focus:outline-none focus:ring-2 focus:ring-green-600 disabled:opacity-50">
            {exams.length === 0 && <option value="">Chưa có kỳ thi</option>}
            {exams.map(e => <option key={e.id} value={e.id}>{e.title}</option>)}
          </select>
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

      <section className="bg-white border-2 border-green-500 rounded-2xl shadow-[4px_4px_0_#DEB9B5] overflow-hidden">
        <div className="p-4 border-b-2 border-green-100">
          <div className="relative max-w-sm">
            <Search className="w-4 h-4 text-green-700 absolute left-3 top-2.5" />
            <input value={searchTerm} onChange={e => setSearchTerm(e.target.value)} placeholder="Tìm theo tên, MSSV, email..."
              className="w-full bg-white border-2 border-green-300 rounded-xl p-2 pl-9 text-xs focus:outline-none focus:ring-2 focus:ring-green-600" />
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b-2 border-green-500 text-green-900 font-bold uppercase text-[10px] tracking-wider">
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

      {attempt && (
        <div className="fixed inset-0 z-50 bg-ink/50 flex justify-end" role="dialog" aria-modal="true">
          <div className="bg-paper w-full max-w-2xl h-full overflow-y-auto border-l-2 border-green-500">
            <div className="sticky top-0 bg-paper z-10 p-5 border-b-2 border-green-200 flex items-start justify-between gap-3">
              <div>
                <h2 className="font-heading font-black text-lg">{attempt.studentName}</h2>
                <p className="text-[11px] text-green-800">
                  {attempt.examTitle} • Điểm AI {fmt(attempt.aiScore)} • Điểm GV {fmt(attempt.teacherScore)} • Tổng {fmt(attempt.overrideTotalScore ?? attempt.totalScore)}
                </p>
              </div>
              <button type="button" onClick={() => setAttempt(null)} aria-label="Đóng" className="p-1 rounded-lg hover:bg-green-100"><X className="w-5 h-5" /></button>
            </div>

            <div className="p-5 space-y-4 text-xs">
              {isLoadingAttempt ? (
                <div className="flex items-center gap-2 text-green-800"><Loader2 className="w-4 h-4 animate-spin" /> Đang tải bài làm...</div>
              ) : (attempt.questions || []).map(qn => (
                <article key={qn.questionId} className="bg-white border-2 border-green-300 rounded-2xl p-4 space-y-3">
                  <div className="flex items-start justify-between gap-3">
                    <p className="font-bold">Câu {qn.orderIndex}. {qn.questionText}</p>
                    <span className="shrink-0 font-mono font-black bg-green-100 border border-green-400 rounded-lg px-2 py-0.5">{fmt(qn.effectiveScore)} / 10</span>
                  </div>
                  {qn.expectedAnswer && (
                    <p className="text-green-900 bg-green-50 rounded-lg p-2"><strong>Đáp án tham khảo:</strong> {qn.expectedAnswer}</p>
                  )}
                  <ol className="space-y-2">
                    {(qn.turns || []).map(t => (
                      <li key={t.turnId} className="border-l-4 border-pink-300 pl-3 space-y-1">
                        <p className="text-[11px] font-bold text-pink-800">{t.turnType === 'PROBING' ? 'Câu hỏi xoáy' : 'Câu hỏi chính'} • AI: {fmt(t.score)}</p>
                        {t.turnType === 'PROBING' && <p className="italic">{t.questionText}</p>}
                        <p><strong>Trả lời:</strong> {t.transcriptText || <span className="text-green-700">(chưa trả lời)</span>}</p>
                        {t.feedback && <p className="text-green-800"><strong>Nhận xét AI:</strong> {t.feedback}</p>}
                      </li>
                    ))}
                  </ol>
                  <div className="grid sm:grid-cols-[100px_1fr_auto] gap-2 items-start pt-2 border-t-2 border-green-100">
                    <input type="number" min="0" max="10" step="0.25" aria-label="Điểm giảng viên" placeholder="Điểm"
                      value={drafts[qn.questionId]?.score ?? ''}
                      onChange={e => setDrafts(d => ({ ...d, [qn.questionId]: { ...d[qn.questionId], score: e.target.value } }))}
                      className="bg-white border-2 border-green-300 rounded-xl p-2 text-xs focus:outline-none focus:ring-2 focus:ring-green-600" />
                    <textarea rows={2} aria-label="Nhận xét giảng viên" placeholder="Nhận xét của giảng viên (tuỳ chọn)"
                      value={drafts[qn.questionId]?.feedback ?? ''}
                      onChange={e => setDrafts(d => ({ ...d, [qn.questionId]: { ...d[qn.questionId], feedback: e.target.value } }))}
                      className="bg-white border-2 border-green-300 rounded-xl p-2 text-xs focus:outline-none focus:ring-2 focus:ring-green-600" />
                    <button type="button" onClick={() => saveGrade(qn.questionId)} disabled={savingQuestionId === qn.questionId}
                      className="px-3 py-2 rounded-xl bg-green-600 hover:bg-green-700 text-white font-bold flex items-center gap-1.5 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2">
                      {savingQuestionId === qn.questionId ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />} Lưu
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
