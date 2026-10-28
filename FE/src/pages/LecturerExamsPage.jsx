import React, { useState, useEffect, useCallback } from 'react';
import { lecturerCatalogApi } from '../api/lecturerCatalogApi';
import { lecturerExamApi } from '../api/lecturerExamApi';
import { questionBankApi } from '../api/questionBankApi';
import { getErrorMessage } from '../utils/errorCodes';
import { ConfirmModal } from '../components/common/ConfirmModal';
import {
  CalendarPlus,
  Clock,
  ListChecks,
  Loader2,
  Pencil,
  Play,
  Square,
  Trash2,
  X,
  ArrowUp,
  ArrowDown,
  ClipboardList
} from 'lucide-react';

const STATUS_META = {
  DRAFT: { label: 'Bản nháp', tone: 'bg-amber-50 text-amber-800 border-amber-300' },
  IN_PROGRESS: { label: 'Đang mở thi', tone: 'bg-emerald-50 text-emerald-800 border-emerald-200' },
  COMPLETED: { label: 'Đã đóng', tone: 'bg-slate-100 text-slate-700 border-pink-200' }
};

const inputClass = 'w-full bg-white border border-pink-200 focus:border-pink-500 rounded-xl p-2.5 text-xs text-ink focus:outline-none focus:ring-1 focus:ring-pink-500 shadow-2xs';
const primaryBtn = 'px-4 py-2 rounded-xl bg-green-600 hover:bg-green-700 text-white text-xs font-bold flex items-center gap-1.5 transition active:scale-[0.98] disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2';
const ghostBtn = 'px-3 py-1.5 rounded-lg bg-white border border-pink-200 hover:bg-slate-50 text-xs font-medium text-slate-700 flex items-center gap-1.5 transition disabled:opacity-50 shadow-2xs cursor-pointer';

/**
 * Lecturer exam management: create exams per subject, pick approved question versions,
 * open the exam to students and close it when the session ends.
 */
export function LecturerExamsPage({ showToast }) {
  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState('');
  const [exams, setExams] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const [form, setForm] = useState(null); // { examId?, title, minutes }
  const [isSaving, setIsSaving] = useState(false);
  const [confirm, setConfirm] = useState(null); // { exam, action: 'delete' | 'open' | 'close' }
  const [isConfirming, setIsConfirming] = useState(false);

  const [questionEditor, setQuestionEditor] = useState(null); // { exam, approved, selected: [{questionId, questionVersionId, content}] }
  const [isLoadingQuestions, setIsLoadingQuestions] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const data = await lecturerCatalogApi.getAssignedSubjects();
        const list = Array.isArray(data) ? data : data?.content || [];
        setSubjects(list);
        if (list.length > 0) setSubjectId(list[0].subjectId);
        else setIsLoading(false);
      } catch (err) {
        setIsLoading(false);
        showToast({ type: 'error', message: getErrorMessage(err) });
      }
    })();
  }, []);

  const loadExams = useCallback(async () => {
    if (!subjectId) return;
    setIsLoading(true);
    try {
      const data = await lecturerExamApi.listExams(subjectId);
      setExams(Array.isArray(data) ? data : []);
    } catch (err) {
      setExams([]);
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsLoading(false);
    }
  }, [subjectId]);

  useEffect(() => {
    loadExams();
  }, [loadExams]);

  const handleSaveExam = async (e) => {
    e.preventDefault();
    const title = form.title.trim();
    const minutes = Number(form.minutes);
    if (!title) return;
    if (!form.examId && (!Number.isFinite(minutes) || minutes < 1)) {
      showToast({ type: 'warning', message: 'Thời lượng phải tối thiểu 1 phút.' });
      return;
    }
    setIsSaving(true);
    try {
      if (form.examId) {
        await lecturerExamApi.updateTitle(form.examId, title);
        showToast({ type: 'success', message: 'Đã cập nhật tên kỳ thi.' });
      } else {
        await lecturerExamApi.createExam({ subjectId, title, durationSeconds: Math.round(minutes * 60) });
        showToast({ type: 'success', message: 'Đã tạo kỳ thi ở trạng thái Bản nháp. Hãy chọn câu hỏi trước khi mở thi.' });
      }
      setForm(null);
      loadExams();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSaving(false);
    }
  };

  const handleConfirm = async () => {
    const { exam, action } = confirm;
    setIsConfirming(true);
    try {
      if (action === 'delete') {
        await lecturerExamApi.deleteExam(exam.id);
        showToast({ type: 'success', message: `Đã xoá kỳ thi "${exam.title}".` });
      } else {
        await lecturerExamApi.updateStatus(exam.id, action === 'open' ? 'IN_PROGRESS' : 'COMPLETED');
        showToast({ type: 'success', message: action === 'open' ? 'Kỳ thi đã mở cho sinh viên.' : 'Kỳ thi đã được đóng.' });
      }
      setConfirm(null);
      loadExams();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsConfirming(false);
    }
  };

  const openQuestionEditor = async (exam) => {
    setIsLoadingQuestions(true);
    setQuestionEditor({ exam, approved: [], selected: [] });
    try {
      const [detail, page] = await Promise.all([
        lecturerExamApi.getExam(exam.id),
        // BE caps a page at 100 versions
        questionBankApi.searchVersions(subjectId, { status: 'APPROVED', page: 0, size: 100 })
      ]);
      const approved = page?.content || (Array.isArray(page) ? page : []);
      const contentByVersion = new Map(approved.map(v => [v.versionId, v.content]));
      const selected = (detail?.questions || [])
        .slice()
        .sort((a, b) => a.orderIndex - b.orderIndex)
        .map(q => ({
          questionId: q.questionId,
          questionVersionId: q.questionVersionId,
          content: q.content || contentByVersion.get(q.questionVersionId) || ''
        }));
      setQuestionEditor({ exam, approved, selected });
    } catch (err) {
      setQuestionEditor(null);
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsLoadingQuestions(false);
    }
  };

  const toggleQuestion = (version) => {
    setQuestionEditor(prev => {
      const item = { questionId: version.questionId, questionVersionId: version.versionId, content: version.content };
      let selected;
      if (prev.selected.some(s => s.questionVersionId === version.versionId)) {
        selected = prev.selected.filter(s => s.questionVersionId !== version.versionId);
      } else if (prev.selected.some(s => s.questionId === version.questionId)) {
        // BE allows one version per question: picking another version replaces it in place
        selected = prev.selected.map(s => (s.questionId === version.questionId ? item : s));
      } else {
        selected = [...prev.selected, item];
      }
      return { ...prev, selected };
    });
  };

  const moveQuestion = (index, delta) => {
    setQuestionEditor(prev => {
      const selected = [...prev.selected];
      const target = index + delta;
      if (target < 0 || target >= selected.length) return prev;
      [selected[index], selected[target]] = [selected[target], selected[index]];
      return { ...prev, selected };
    });
  };

  const saveQuestions = async () => {
    const { exam, selected } = questionEditor;
    if (selected.length === 0) {
      showToast({ type: 'warning', message: 'Hãy chọn ít nhất một câu hỏi.' });
      return;
    }
    setIsSaving(true);
    try {
      await lecturerExamApi.setQuestions(exam.id, selected.map((s, i) => ({
        questionId: s.questionId,
        questionVersionId: s.questionVersionId,
        orderIndex: i + 1
      })));
      showToast({ type: 'success', message: `Đã lưu ${selected.length} câu hỏi cho kỳ thi.` });
      setQuestionEditor(null);
      loadExams();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSaving(false);
    }
  };

  const confirmCopy = confirm && {
    delete: { title: 'Xoá kỳ thi', message: `Xoá vĩnh viễn kỳ thi "${confirm.exam.title}"? Chỉ xoá được bản nháp chưa có lượt thi.`, confirmText: 'Xoá', isDanger: true },
    open: { title: 'Mở kỳ thi', message: `Mở "${confirm.exam.title}" cho sinh viên? Sau khi mở sẽ không thể đổi danh sách câu hỏi.`, confirmText: 'Mở thi', isDanger: false },
    close: { title: 'Đóng kỳ thi', message: `Đóng "${confirm.exam.title}"? Sinh viên sẽ không thể vào thi nữa.`, confirmText: 'Đóng thi', isDanger: true }
  }[confirm.action];

  return (
    <div className="max-w-5xl mx-auto w-full space-y-6 pb-10 text-ink animate-modal-entry">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-heading font-extrabold text-2xl tracking-tight">Quản Lý Kỳ Thi</h1>
          <p className="text-xs text-green-800 mt-1">Tạo kỳ thi, chọn câu hỏi đã duyệt và mở/đóng ca thi cho sinh viên.</p>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={subjectId}
            onChange={(e) => setSubjectId(e.target.value)}
            className="bg-white border border-pink-200 rounded-xl p-2 text-xs font-semibold text-slate-800 focus:outline-none focus:border-pink-500 shadow-2xs"
            aria-label="Môn học"
          >
            {subjects.length === 0 && <option value="">Chưa được phân công môn học</option>}
            {subjects.map(s => (
              <option key={s.subjectId} value={s.subjectId}>{s.subjectCode} — {s.subjectName}</option>
            ))}
          </select>
          <button type="button" disabled={!subjectId} onClick={() => setForm({ title: '', minutes: 15 })} className={primaryBtn}>
            <CalendarPlus className="w-4 h-4" /> Tạo kỳ thi
          </button>
        </div>
      </header>

      <section className="bg-white border border-pink-200 rounded-2xl shadow-2xs overflow-hidden">
        {isLoading ? (
          <div className="p-4 space-y-3">{[0, 1, 2].map(i => <div key={i} className="h-16 rounded-xl animate-pulse bg-slate-200/80" />)}</div>
        ) : exams.length === 0 ? (
          <div className="p-10 text-center space-y-2">
            <ClipboardList className="w-8 h-8 text-green-600 mx-auto" />
            <p className="font-bold text-sm">Chưa có kỳ thi nào</p>
            <p className="text-xs text-green-800">{subjectId ? 'Bấm "Tạo kỳ thi" để bắt đầu.' : 'Bạn cần được Admin phân công môn học.'}</p>
          </div>
        ) : (
          <ul className="divide-y divide-slate-100">
            {exams.map(exam => {
              const meta = STATUS_META[exam.status] || STATUS_META.DRAFT;
              const isDraft = exam.status === 'DRAFT';
              return (
                <li key={exam.id} className="p-4 flex flex-col md:flex-row md:items-center justify-between gap-3">
                  <div className="min-w-0 space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="font-bold text-sm">{exam.title}</p>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${meta.tone}`}>{meta.label}</span>
                    </div>
                    <p className="text-[11px] text-green-800 flex items-center gap-3">
                      <span className="flex items-center gap-1"><Clock className="w-3.5 h-3.5" /> {Math.round(exam.durationSeconds / 60)} phút</span>
                      <span className="flex items-center gap-1"><ListChecks className="w-3.5 h-3.5" /> {exam.questionCount} câu hỏi</span>
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <button type="button" onClick={() => setForm({ examId: exam.id, title: exam.title })} className={ghostBtn}>
                      <Pencil className="w-3.5 h-3.5" /> Đổi tên
                    </button>
                    {isDraft && (
                      <>
                        <button type="button" onClick={() => openQuestionEditor(exam)} className={ghostBtn}>
                          <ListChecks className="w-3.5 h-3.5" /> Câu hỏi
                        </button>
                        <button
                          type="button"
                          disabled={exam.questionCount === 0}
                          title={exam.questionCount === 0 ? 'Cần ít nhất một câu hỏi' : ''}
                          onClick={() => setConfirm({ exam, action: 'open' })}
                          className={ghostBtn}
                        >
                          <Play className="w-3.5 h-3.5" /> Mở thi
                        </button>
                        <button type="button" onClick={() => setConfirm({ exam, action: 'delete' })} className={`${ghostBtn} text-rose-600 border-rose-200 hover:bg-rose-50`}>
                          <Trash2 className="w-3.5 h-3.5" /> Xoá
                        </button>
                      </>
                    )}
                    {exam.status === 'IN_PROGRESS' && (
                      <button type="button" onClick={() => setConfirm({ exam, action: 'close' })} className={ghostBtn}>
                        <Square className="w-3.5 h-3.5" /> Đóng thi
                      </button>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {/* CREATE / RENAME */}
      {form && (
        <div className="fixed inset-0 z-50 bg-ink/50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
          <form onSubmit={handleSaveExam} className="bg-white border border-pink-200 rounded-2xl w-full max-w-md p-6 space-y-4 shadow-md">
            <div className="flex items-center justify-between">
              <h2 className="font-heading font-black text-lg">{form.examId ? 'Đổi tên kỳ thi' : 'Tạo kỳ thi mới'}</h2>
              <button type="button" onClick={() => setForm(null)} aria-label="Đóng" className="p-1 rounded-lg hover:bg-green-100"><X className="w-5 h-5" /></button>
            </div>
            <label className="block text-xs font-bold space-y-1">
              <span>Tên kỳ thi</span>
              <input autoFocus required value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} className={inputClass} placeholder="VD: Vấn đáp giữa kỳ — Nhóm 1" />
            </label>
            {!form.examId && (
              <label className="block text-xs font-bold space-y-1">
                <span>Thời lượng (phút)</span>
                <input type="number" min="1" required value={form.minutes} onChange={e => setForm({ ...form, minutes: e.target.value })} className={inputClass} />
              </label>
            )}
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={() => setForm(null)} className={ghostBtn}>Huỷ</button>
              <button type="submit" disabled={isSaving} className={primaryBtn}>
                {isSaving && <Loader2 className="w-4 h-4 animate-spin" />} Lưu
              </button>
            </div>
          </form>
        </div>
      )}

      {/* QUESTION PICKER */}
      {questionEditor && (
        <div className="fixed inset-0 z-50 bg-ink/50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
          <div className="bg-white border border-pink-200 rounded-2xl w-full max-w-4xl max-h-[88vh] flex flex-col shadow-md">
            <div className="p-5 border-b border-pink-200 flex items-center justify-between">
              <div>
                <h2 className="font-heading font-black text-lg">Câu hỏi cho "{questionEditor.exam.title}"</h2>
                <p className="text-[11px] text-green-800">Chỉ hiển thị phiên bản câu hỏi đã được duyệt (APPROVED) của môn học.</p>
              </div>
              <button type="button" onClick={() => setQuestionEditor(null)} aria-label="Đóng" className="p-1 rounded-lg hover:bg-green-100"><X className="w-5 h-5" /></button>
            </div>

            {isLoadingQuestions ? (
              <div className="p-10 flex items-center justify-center gap-2 text-xs text-green-800"><Loader2 className="w-4 h-4 animate-spin" /> Đang tải...</div>
            ) : (
              <div className="grid md:grid-cols-2 gap-4 p-5 overflow-y-auto text-xs">
                <div className="space-y-2">
                  <h3 className="font-bold">Ngân hàng câu hỏi đã duyệt ({questionEditor.approved.length})</h3>
                  {questionEditor.approved.length === 0 && (
                    <p className="text-green-800">Chưa có câu hỏi nào được duyệt. Hãy duyệt câu hỏi ở trang Ngân Hàng Câu Hỏi.</p>
                  )}
                  {questionEditor.approved.map(v => {
                    const checked = questionEditor.selected.some(s => s.questionVersionId === v.versionId);
                    return (
                      <label key={v.versionId} className={`flex gap-2 p-3 rounded-xl border-2 cursor-pointer ${checked ? 'border-slate-900 bg-slate-50' : 'border-pink-200 bg-white hover:bg-slate-50'}`}>
                        <input type="checkbox" checked={checked} onChange={() => toggleQuestion(v)} className="mt-0.5 accent-green-600" />
                        <span className="space-y-0.5">
                          <span className="block font-medium">{v.content}</span>
                          <span className="block text-[10px] text-green-700">{v.questionCode} • v{v.versionNumber}{v.topicName ? ` • ${v.topicName}` : ''}{v.bloomLevelLabel ? ` • ${v.bloomLevelLabel}` : ''}</span>
                        </span>
                      </label>
                    );
                  })}
                </div>
                <div className="space-y-2">
                  <h3 className="font-bold">Thứ tự trong kỳ thi ({questionEditor.selected.length})</h3>
                  {questionEditor.selected.length === 0 && <p className="text-green-800">Chưa chọn câu hỏi nào.</p>}
                  <ol className="space-y-2">
                    {questionEditor.selected.map((s, i) => (
                      <li key={s.questionVersionId} className="flex items-start gap-2 p-3 rounded-xl border border-pink-200 bg-white">
                        <span className="font-mono font-black text-green-700">{i + 1}.</span>
                        <span className="flex-1">{s.content}</span>
                        <span className="flex flex-col gap-1">
                          <button type="button" onClick={() => moveQuestion(i, -1)} disabled={i === 0} aria-label="Lên" className="p-0.5 rounded hover:bg-green-100 disabled:opacity-30"><ArrowUp className="w-3.5 h-3.5" /></button>
                          <button type="button" onClick={() => moveQuestion(i, 1)} disabled={i === questionEditor.selected.length - 1} aria-label="Xuống" className="p-0.5 rounded hover:bg-green-100 disabled:opacity-30"><ArrowDown className="w-3.5 h-3.5" /></button>
                        </span>
                      </li>
                    ))}
                  </ol>
                </div>
              </div>
            )}

            <div className="p-5 border-t border-pink-200 flex justify-end gap-2">
              <button type="button" onClick={() => setQuestionEditor(null)} className={ghostBtn}>Huỷ</button>
              <button type="button" onClick={saveQuestions} disabled={isSaving || isLoadingQuestions} className={primaryBtn}>
                {isSaving && <Loader2 className="w-4 h-4 animate-spin" />} Lưu danh sách câu hỏi
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmModal
        isOpen={!!confirm}
        title={confirmCopy?.title}
        message={confirmCopy?.message}
        confirmText={confirmCopy?.confirmText}
        isDanger={confirmCopy?.isDanger}
        isLoading={isConfirming}
        onConfirm={handleConfirm}
        onCancel={() => setConfirm(null)}
      />
    </div>
  );
}
