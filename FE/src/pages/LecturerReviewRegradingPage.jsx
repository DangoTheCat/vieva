import React, { useState, useEffect } from 'react';
import { lecturerGradingApi } from '../api/lecturerGradingApi';
import { getErrorMessage } from '../utils/errorCodes';
import { gradeLabel } from '../utils/activeExam';
import { Search, X, Loader2, Scale, RotateCcw } from 'lucide-react';

const fmt = (n) => (n != null ? Number(n).toFixed(2).replace(/\.00$/, '') : '—');

/**
 * Score review (phúc khảo): every final score across the lecturer's exams,
 * with a total-score override that requires a reason.
 */
export function LecturerReviewRegradingPage({ showToast }) {
  const [scores, setScores] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [examFilter, setExamFilter] = useState('');

  const [editing, setEditing] = useState(null); // { score, value, reason }
  const [isSaving, setIsSaving] = useState(false);

  const loadScores = async () => {
    setIsLoading(true);
    try {
      const data = await lecturerGradingApi.listScores();
      setScores(Array.isArray(data) ? data : []);
    } catch (err) {
      setScores([]);
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadScores();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    const value = Number(editing.value);
    if (editing.value === '' || !Number.isFinite(value) || value < 0 || value > 10) {
      showToast({ type: 'warning', message: 'Điểm điều chỉnh phải nằm trong khoảng 0 – 10.' });
      return;
    }
    if (!editing.reason.trim()) {
      showToast({ type: 'warning', message: 'Vui lòng nhập lý do điều chỉnh điểm.' });
      return;
    }
    setIsSaving(true);
    try {
      const updated = await lecturerGradingApi.overrideScore(editing.score.scoreId, {
        overrideTotalScore: value,
        reason: editing.reason.trim()
      });
      setScores(prev => prev.map(s => (s.scoreId === updated.scoreId ? updated : s)));
      showToast({ type: 'success', message: `Đã điều chỉnh điểm của ${updated.studentName}.` });
      setEditing(null);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSaving(false);
    }
  };

  const examOptions = Array.from(new Map(scores.map(s => [s.examId, s.examTitle])).entries());
  const q = searchTerm.toLowerCase().trim();
  const filtered = scores.filter(s =>
    (!examFilter || s.examId === examFilter) &&
    (!q || s.studentName?.toLowerCase().includes(q) || s.examTitle?.toLowerCase().includes(q))
  );

  return (
    <div className="max-w-5xl mx-auto w-full space-y-6 pb-10 text-ink animate-modal-entry">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-heading font-extrabold text-2xl tracking-tight">Phúc Khảo &amp; Điều Chỉnh Điểm</h1>
          <p className="text-xs text-green-800 mt-1">Điểm tổng kết của các kỳ thi bạn phụ trách. Mọi điều chỉnh đều cần ghi lý do.</p>
        </div>
        <button type="button" onClick={loadScores} disabled={isLoading}
          className="px-3.5 py-1.5 rounded-xl bg-white border border-pink-200 text-xs font-semibold text-slate-700 flex items-center gap-1.5 hover:bg-slate-50 disabled:opacity-50 shadow-2xs cursor-pointer">
          <RotateCcw className="w-3.5 h-3.5" /> Làm mới
        </button>
      </header>

      <section className="bg-white border border-pink-200 rounded-2xl shadow-2xs overflow-hidden">
        <div className="p-4 border-b border-pink-200 flex flex-wrap gap-2">
          <div className="relative flex-1 min-w-[200px] max-w-sm">
            <Search className="w-4 h-4 text-green-700 absolute left-3 top-2.5" />
            <input value={searchTerm} onChange={e => setSearchTerm(e.target.value)} placeholder="Tìm theo tên thí sinh hoặc kỳ thi..."
              className="w-full bg-white border border-pink-200 rounded-xl p-2 pl-9 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-pink-500 shadow-2xs" />
          </div>
          <select value={examFilter} onChange={e => setExamFilter(e.target.value)} aria-label="Lọc theo kỳ thi"
            className="bg-white border border-pink-200 rounded-xl p-2 text-xs font-semibold text-slate-800 focus:outline-none focus:border-pink-500 shadow-2xs">
            <option value="">Tất cả kỳ thi</option>
            {examOptions.map(([id, title]) => <option key={id} value={id}>{title}</option>)}
          </select>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-pink-200 text-slate-700 font-semibold uppercase text-[10px] tracking-wider">
              <tr>
                <th className="py-3 px-4">Thí sinh</th>
                <th className="py-3 px-4">Kỳ thi</th>
                <th className="py-3 px-4">Điểm hệ thống</th>
                <th className="py-3 px-4">Điểm điều chỉnh</th>
                <th className="py-3 px-4">Xếp loại</th>
                <th className="py-3 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-green-100">
              {isLoading ? (
                [0, 1, 2].map(i => <tr key={i}><td colSpan={6} className="p-3"><div className="h-8 rounded-lg animate-pulse bg-slate-200/80" /></td></tr>)
              ) : filtered.length === 0 ? (
                <tr><td colSpan={6} className="p-10 text-center text-green-800">
                  <Scale className="w-8 h-8 text-green-600 mx-auto mb-2" />
                  Chưa có điểm nào để phúc khảo.
                </td></tr>
              ) : filtered.map(s => (
                <tr key={s.scoreId} className="hover:bg-green-50/50">
                  <td className="py-3 px-4 font-bold">{s.studentName}</td>
                  <td className="py-3 px-4">{s.examTitle}</td>
                  <td className="py-3 px-4 font-mono">{fmt(s.totalScore)}</td>
                  <td className="py-3 px-4 font-mono font-black text-pink-700">{fmt(s.overrideTotalScore)}</td>
                  <td className="py-3 px-4">{gradeLabel(s.grade)}</td>
                  <td className="py-3 px-4 text-right">
                    <button type="button"
                      onClick={() => setEditing({ score: s, value: String(s.overrideTotalScore ?? s.totalScore ?? ''), reason: '' })}
                      className="px-3 py-1.5 rounded-lg bg-white border border-pink-200 hover:bg-slate-50 text-xs font-semibold text-slate-700 shadow-2xs cursor-pointer">
                      Điều chỉnh
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {editing && (
        <div className="fixed inset-0 z-50 bg-ink/50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
          <form onSubmit={handleSave} className="bg-white border border-pink-200 rounded-2xl w-full max-w-md p-6 space-y-4 shadow-xl text-xs text-ink">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="font-heading font-black text-lg">Điều chỉnh điểm</h2>
                <p className="text-green-800">{editing.score.studentName} • {editing.score.examTitle}</p>
              </div>
              <button type="button" onClick={() => setEditing(null)} aria-label="Đóng" className="p-1 rounded-lg hover:bg-green-100"><X className="w-5 h-5" /></button>
            </div>
            <p>Điểm hệ thống hiện tại: <strong className="font-mono">{fmt(editing.score.totalScore)}</strong></p>
            <label className="block font-bold space-y-1">
              <span>Điểm tổng mới (0 – 10)</span>
              <input type="number" min="0" max="10" step="0.25" required autoFocus value={editing.value}
                onChange={e => setEditing({ ...editing, value: e.target.value })}
                className="w-full bg-white border border-pink-200 rounded-xl p-2.5 text-xs text-ink focus:outline-none focus:border-pink-500 shadow-2xs" />
            </label>
            <label className="block font-bold space-y-1">
              <span>Lý do điều chỉnh</span>
              <textarea rows={3} required value={editing.reason} onChange={e => setEditing({ ...editing, reason: e.target.value })}
                placeholder="VD: Phúc khảo theo đơn ngày 27/10 — bổ sung điểm câu 2"
                className="w-full bg-white border border-pink-200 rounded-xl p-2.5 text-xs text-ink focus:outline-none focus:border-pink-500 shadow-2xs" />
            </label>
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setEditing(null)} className="px-4 py-2 rounded-xl bg-white border border-pink-200 font-semibold text-slate-700 hover:bg-slate-50 shadow-2xs cursor-pointer">Huỷ</button>
              <button type="submit" disabled={isSaving}
                className="px-4 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold flex items-center gap-1.5 disabled:opacity-50 shadow-xs cursor-pointer focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2">
                {isSaving && <Loader2 className="w-4 h-4 animate-spin" />} Lưu điều chỉnh
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
