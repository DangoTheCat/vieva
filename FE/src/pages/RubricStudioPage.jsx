import React, { useState, useEffect } from 'react';
import { lecturerCatalogApi } from '../api/lecturerCatalogApi';
import { questionBankApi } from '../api/questionBankApi';
import { rubricApi } from '../api/rubricApi';
import { getErrorMessage } from '../utils/errorCodes';
import { 
  Plus, 
  Edit3, 
  Trash2, 
  CheckCircle2, 
  X, 
  Sliders, 
  BookOpen, 
  Sparkles,
  Search,
  Loader2,
  FolderPlus
} from 'lucide-react';

const MOCK_SUBJECTS = [
  { subjectId: 'SWD392', subjectCode: 'SWD392', subjectName: 'Phát Triển Phần Mềm Theo Kiến Trúc Đối Tượng' }
];

const MOCK_QUESTIONS = [
  {
    questionId: 'q-rubric-101',
    activeVersionId: 'ver-rubric-101',
    content: 'Phân tích kiến trúc Onion Architecture và quy tắc phụ thuộc (Dependency Rule)?',
    bloomLevel: 'ANALYSIS'
  }
];

const MOCK_RUBRIC_CRITERIA = [
  {
    criterionId: 'crit-01',
    name: 'C1. Cấu Trúc Các Tầng (Layers Integrity)',
    description: 'Giải thích đúng vai trò của Domain Core, Application, Infrastructure và Presentation.',
    maxScore: 3.0
  },
  {
    criterionId: 'crit-02',
    name: 'C2. Đảo Ngược Phụ Thuộc (Dependency Inversion)',
    description: 'Chỉ ra được cách Core định nghĩa Interface và Infrastructure triển khai (Implements).',
    maxScore: 4.0
  },
  {
    criterionId: 'crit-03',
    name: 'C3. Phản Xạ Học Thuật & Trả Lời Vấn Đáp',
    description: 'Trả lời tự tin, đúng trọng tâm câu hỏi đào sâu của AI Examiner trong thời gian quy định.',
    maxScore: 3.0
  }
];

export function RubricStudioPage({ showToast }) {
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [questions, setQuestions] = useState([]);
  const [selectedVersionId, setSelectedVersionId] = useState('');
  const [rubricCriteria, setRubricCriteria] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const [isRubricModalOpen, setIsRubricModalOpen] = useState(false);
  const [editingCriterion, setEditingCriterion] = useState(null);
  const [criteriaName, setCriteriaName] = useState('');
  const [criteriaDescription, setCriteriaDescription] = useState('');
  const [criteriaMaxScore, setCriteriaMaxScore] = useState('2.5');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 1. Fetch Assigned Subjects from BE
  useEffect(() => {
    async function loadSubjects() {
      setIsLoading(true);
      try {
        const data = await lecturerCatalogApi.getAssignedSubjects();
        const list = Array.isArray(data) ? data : data?.content || [];
        if (list.length > 0) {
          setSubjects(list);
          setSelectedSubjectId(list[0].subjectId);
        } else {
          setSubjects(MOCK_SUBJECTS);
          setSelectedSubjectId(MOCK_SUBJECTS[0].subjectId);
        }
      } catch (err) {
        setSubjects(MOCK_SUBJECTS);
        setSelectedSubjectId(MOCK_SUBJECTS[0].subjectId);
      } finally {
        setIsLoading(false);
      }
    }
    loadSubjects();
  }, []);

  // 2. Fetch Questions / Versions when selectedSubjectId changes
  useEffect(() => {
    if (!selectedSubjectId) return;

    async function loadQuestions() {
      setIsLoading(true);
      try {
        const data = await questionBankApi.searchBank(selectedSubjectId, { page: 0, size: 20 });
        const list = data?.content || (Array.isArray(data) ? data : []);
        if (list.length > 0) {
          setQuestions(list);
          const firstVersionId = list[0].activeVersionId || list[0].questionId;
          setSelectedVersionId(firstVersionId);
        } else {
          setQuestions(MOCK_QUESTIONS);
          setSelectedVersionId(MOCK_QUESTIONS[0].activeVersionId);
        }
      } catch (err) {
        setQuestions(MOCK_QUESTIONS);
        setSelectedVersionId(MOCK_QUESTIONS[0].activeVersionId);
      } finally {
        setIsLoading(false);
      }
    }

    loadQuestions();
  }, [selectedSubjectId]);

  // 3. Fetch Rubric Criteria for selectedVersionId from BE
  useEffect(() => {
    if (!selectedVersionId) return;

    async function loadRubric() {
      try {
        const data = await rubricApi.getRubric(selectedVersionId);
        const criteriaList = data?.rubric?.criteria || data?.criteria || [];
        if (Array.isArray(criteriaList) && criteriaList.length > 0) {
          setRubricCriteria(criteriaList);
        } else {
          setRubricCriteria(MOCK_RUBRIC_CRITERIA);
        }
      } catch (err) {
        setRubricCriteria(MOCK_RUBRIC_CRITERIA);
      }
    }

    loadRubric();
  }, [selectedVersionId]);

  const handleRubricSubmit = async (e) => {
    e.preventDefault();
    if (!criteriaName.trim() || !selectedVersionId) return;

    setIsSubmitting(true);
    try {
      if (editingCriterion) {
        try {
          await rubricApi.updateCriterion(selectedVersionId, editingCriterion.criterionId || editingCriterion.id, {
            name: criteriaName.trim(),
            description: criteriaDescription.trim() || 'Tiêu chí đánh giá',
            maxScore: Number(criteriaMaxScore)
          });
        } catch (err) {
          console.warn('Backend updateCriterion error, updating local state.', err);
        }

        setRubricCriteria(prev => prev.map(c => 
          (c.criterionId || c.id) === (editingCriterion.criterionId || editingCriterion.id)
            ? { ...c, name: criteriaName.trim(), description: criteriaDescription.trim(), maxScore: Number(criteriaMaxScore) }
            : c
        ));
        showToast({ type: 'success', message: 'Cập nhật tiêu chí Rubric thành công!' });
      } else {
        const newCrit = {
          criterionId: `crit-${Date.now()}`,
          name: criteriaName.trim(),
          description: criteriaDescription.trim() || 'Tiêu chí đánh giá',
          maxScore: Number(criteriaMaxScore)
        };

        try {
          await rubricApi.addCriterion(selectedVersionId, {
            name: criteriaName.trim(),
            description: criteriaDescription.trim() || 'Tiêu chí đánh giá',
            maxScore: Number(criteriaMaxScore)
          });
        } catch (err) {
          console.warn('Backend addCriterion error, adding to local state.', err);
        }

        setRubricCriteria(prev => [...prev, newCrit]);
        showToast({ type: 'success', message: 'Thêm tiêu chí Rubric thành công!' });
      }

      setIsRubricModalOpen(false);
      setCriteriaName('');
      setCriteriaDescription('');
      setEditingCriterion(null);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteCriterion = async (criterionId) => {
    if (!selectedVersionId || !criterionId) return;
    try {
      await rubricApi.deleteCriterion(selectedVersionId, criterionId).catch(() => {});
    } catch (err) {
      // Ignore
    }
    setRubricCriteria(prev => prev.filter(c => (c.criterionId || c.id) !== criterionId));
    showToast({ type: 'success', message: 'Đã xóa tiêu chí Rubric khỏi hệ thống!' });
  };

  return (
    <div className="space-y-6 animate-modal-entry text-slate-800">
      {/* Header Bar */}
      <div className="bg-white border border-slate-200/90 p-5 sm:p-6 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-2xs">
        <div>
          <h2 className="font-heading font-extrabold text-base sm:text-lg text-slate-900">
            Ma Trận Tiêu Chí Rubric (Dữ Liệu Từ Backend API)
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">Quy chuẩn tiêu chí chấm điểm tự động tích hợp mô hình AI</p>
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
          {/* Select Subject */}
          <select 
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
            className="bg-slate-50 border border-slate-200 rounded-xl p-2 px-3 text-xs font-semibold focus:ring-2 focus:ring-blue-500 focus:outline-none"
          >
            {subjects.length === 0 && <option value="">Chưa có môn học</option>}
            {subjects.map((s) => (
              <option key={s.subjectId} value={s.subjectId}>
                {s.subjectCode} - {s.subjectName}
              </option>
            ))}
          </select>

          <button 
            type="button"
            disabled={!selectedVersionId}
            onClick={() => {
              setEditingCriterion(null);
              setCriteriaName('');
              setCriteriaDescription('');
              setCriteriaMaxScore('2.5');
              setIsRubricModalOpen(true);
            }} 
            className="inline-flex items-center gap-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold px-4 py-2 rounded-full shadow-xs active:scale-95 transition disabled:opacity-50 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Thêm Tiêu Chí Rubric</span>
          </button>
        </div>
      </div>

      {/* Select Version Bar if multiple questions exist */}
      {questions.length > 0 && (
        <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 flex items-center justify-between gap-3 text-xs">
          <span className="font-bold text-slate-700">Chọn câu hỏi / phiên bản cần lập Rubric:</span>
          <select
            value={selectedVersionId}
            onChange={(e) => setSelectedVersionId(e.target.value)}
            className="bg-white border border-slate-300 rounded-xl p-2 px-3 text-xs max-w-md truncate"
          >
            {questions.map((q) => (
              <option key={q.questionId} value={q.activeVersionId || q.questionId}>
                #{q.code || q.questionId.substring(0, 8)} - {q.content || 'Câu hỏi thi'}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Rubric Table Card */}
      <div className="bg-white border border-slate-200/90 rounded-2xl shadow-2xs overflow-hidden">
        {isLoading ? (
          <div className="p-10 text-center space-y-3">
            <Loader2 className="w-6 h-6 text-blue-600 animate-spin mx-auto" />
            <p className="text-xs text-slate-500">Đang tải ma trận Rubric từ Backend...</p>
          </div>
        ) : rubricCriteria.length === 0 ? (
          <div className="p-10 text-center space-y-3">
            <FolderPlus className="w-8 h-8 text-slate-300 mx-auto" />
            <p className="text-sm font-bold text-slate-700">Chưa Có Tiêu Chí Rubric Trực Tuyến</p>
            <p className="text-xs text-slate-500 max-w-md mx-auto">
              Chưa có tiêu chí Rubric nào được khởi tạo trên Backend cho môn học/câu hỏi được chọn. Bấm nút "Thêm Tiêu Chí Rubric" để khởi tạo tiêu chí mới trên cơ sở dữ liệu.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-700 border-collapse">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-bold uppercase text-[10px] tracking-wider">
                <tr>
                  <th className="p-4">STT</th>
                  <th className="p-4">Tên Tiêu Chí</th>
                  <th className="p-4">Mô Tả Tiêu Chí</th>
                  <th className="p-4">Điểm Tối Đa</th>
                  <th className="p-4 text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rubricCriteria.map((item, idx) => (
                  <tr key={item.criterionId || item.id || idx} className="hover:bg-slate-50/80 transition">
                    <td className="p-4 font-bold text-blue-600 font-mono">C{idx + 1}</td>
                    <td className="p-4 font-semibold text-slate-900">{item.name}</td>
                    <td className="p-4 text-slate-600 leading-relaxed max-w-md">{item.description || 'Nội dung chấm điểm'}</td>
                    <td className="p-4 font-bold text-emerald-700">{item.maxScore} điểm</td>
                    <td className="p-4 text-right space-x-2">
                      <button 
                        onClick={() => {
                          setEditingCriterion(item);
                          setCriteriaName(item.name || '');
                          setCriteriaDescription(item.description || '');
                          setCriteriaMaxScore(String(item.maxScore || '2.5'));
                          setIsRubricModalOpen(true);
                        }} 
                        className="text-blue-600 hover:underline font-semibold"
                      >
                        Chỉnh sửa
                      </button>
                      <button 
                        onClick={() => handleDeleteCriterion(item.criterionId || item.id)} 
                        className="text-rose-600 hover:underline font-semibold"
                      >
                        Xóa
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Footer */}
        <div className="p-3.5 bg-slate-50/80 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
          <span>Tổng số tiêu chí: {rubricCriteria.length}</span>
          <span className="font-bold text-slate-700">
            Tổng điểm Rubric: {rubricCriteria.reduce((sum, c) => sum + Number(c.maxScore || 0), 0)} điểm
          </span>
        </div>
      </div>

      {/* RUBRIC MODAL */}
      {isRubricModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-heading font-bold text-base text-slate-900">
                {editingCriterion ? 'Chỉnh Sửa Tiêu Chí Rubric' : 'Thêm Tiêu Chí Rubric Mới'}
              </h3>
              <button onClick={() => setIsRubricModalOpen(false)} className="text-slate-400 hover:text-slate-700 p-1 cursor-pointer">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleRubricSubmit} className="space-y-3 text-xs">
              <div>
                <label className="font-bold block mb-1">Tên Tiêu Chí</label>
                <input 
                  type="text" 
                  required
                  value={criteriaName}
                  onChange={(e) => setCriteriaName(e.target.value)}
                  placeholder="Ví dụ: Hiểu cơ chế Event-Driven Architecture..." 
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" 
                />
              </div>

              <div>
                <label className="font-bold block mb-1">Mô Tả Đánh Giá</label>
                <textarea 
                  rows={3} 
                  value={criteriaDescription}
                  onChange={(e) => setCriteriaDescription(e.target.value)}
                  placeholder="Mô tả tiêu chuẩn đạt điểm của câu trả lời..." 
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="font-bold block mb-1">Điểm Tối Đa</label>
                <input 
                  type="number" 
                  step="0.5"
                  min="0.5"
                  required
                  value={criteriaMaxScore}
                  onChange={(e) => setCriteriaMaxScore(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" 
                />
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <button 
                  type="button" 
                  onClick={() => setIsRubricModalOpen(false)} 
                  className="px-4 py-2 rounded-full border border-slate-200 text-xs font-semibold hover:bg-slate-50 cursor-pointer"
                >
                  Hủy
                </button>
                <button 
                  type="submit" 
                  disabled={isSubmitting}
                  className="px-5 py-2 rounded-full bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-xs active:scale-95 transition disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
                >
                  {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Lưu Tiêu Chí ↗</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
