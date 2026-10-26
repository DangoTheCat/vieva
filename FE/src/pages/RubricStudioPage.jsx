import React, { useState, useEffect } from 'react';
import { lecturerCatalogApi } from '../api/lecturerCatalogApi';
import { questionBankApi } from '../api/questionBankApi';
import { rubricApi } from '../api/rubricApi';
import { getErrorMessage } from '../utils/errorCodes';
import { 
  Plus, 
  Trash2, 
  CheckCircle2, 
  X, 
  Sliders, 
  Sparkles,
  Loader2,
  FolderPlus,
  Award
} from 'lucide-react';

export function RubricStudioPage({ showToast }) {
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [questions, setQuestions] = useState([]);
  const [selectedVersionId, setSelectedVersionId] = useState('');
  const [rubricCriteria, setRubricCriteria] = useState([]);
  const [versionLock, setVersionLock] = useState(null);
  const [versionTotalScore, setVersionTotalScore] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const [isRubricModalOpen, setIsRubricModalOpen] = useState(false);
  const [editingCriterion, setEditingCriterion] = useState(null);
  const [criteriaName, setCriteriaName] = useState('');
  const [criteriaDescription, setCriteriaDescription] = useState('');
  const [criteriaMaxScore, setCriteriaMaxScore] = useState('2.5');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Rubric templates of the selected subject
  const [templates, setTemplates] = useState([]);
  const [templateName, setTemplateName] = useState('');
  const [templateBusyId, setTemplateBusyId] = useState(null);

  // Keyboard accessibility: Escape to close rubric modal
  useEffect(() => {
    if (!isRubricModalOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') setIsRubricModalOpen(false);
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isRubricModalOpen]);

  // 1. Fetch Assigned Subjects from BE
  useEffect(() => {
    async function loadSubjects() {
      setIsLoading(true);
      try {
        const data = await lecturerCatalogApi.getAssignedSubjects();
        const list = Array.isArray(data) ? data : data?.content || [];
        setSubjects(list);
        if (list.length > 0) {
          setSelectedSubjectId(list[0].subjectId);
        } else {
          setSelectedSubjectId('');
        }
      } catch (err) {
        setSubjects([]);
        setSelectedSubjectId('');
      } finally {
        setIsLoading(false);
      }
    }
    loadSubjects();
  }, []);

  // 2. Fetch Questions / Versions when selectedSubjectId changes
  useEffect(() => {
    if (!selectedSubjectId) {
      setQuestions([]);
      setSelectedVersionId('');
      return;
    }

    async function loadQuestions() {
      setIsLoading(true);
      try {
        const data = await questionBankApi.searchBank(selectedSubjectId, { page: 0, size: 50 });
        const list = data?.content || (Array.isArray(data) ? data : []);
        // Rubrics hang off a question *version*; bank items expose it as currentVersion
        const withVersion = list.filter(q => q.currentVersion?.versionId);
        setQuestions(withVersion);
        setSelectedVersionId(withVersion[0]?.currentVersion.versionId || '');
      } catch (err) {
        setQuestions([]);
        setSelectedVersionId('');
        showToast({ type: 'error', message: getErrorMessage(err) });
      } finally {
        setIsLoading(false);
      }
    }

    loadQuestions();
    loadTemplates();
  }, [selectedSubjectId]);

  async function loadTemplates() {
    try {
      const list = await rubricApi.listTemplates(selectedSubjectId);
      setTemplates(Array.isArray(list) ? list : []);
    } catch (err) {
      setTemplates([]);
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  }

  const handleSaveAsTemplate = async (e) => {
    e.preventDefault();
    if (!templateName.trim() || rubricCriteria.length === 0) return;
    setTemplateBusyId('new');
    try {
      const criteria = rubricCriteria.map((c, i) => ({
        name: c.name,
        description: c.description,
        maxScore: Number(c.maxScore),
        levels: c.levels || [],
        orderIndex: c.orderIndex ?? i + 1
      }));
      await rubricApi.createTemplate(selectedSubjectId, {
        name: templateName.trim(),
        description: null,
        criteriaJson: JSON.stringify(criteria)
      });
      setTemplateName('');
      showToast({ type: 'success', message: 'Đã lưu rubric hiện tại thành mẫu.' });
      loadTemplates();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setTemplateBusyId(null);
    }
  };

  const handleApplyTemplate = async (template) => {
    setTemplateBusyId(template.id);
    try {
      await rubricApi.applyTemplate(selectedSubjectId, template.id, selectedVersionId);
      await loadRubric();
      showToast({ type: 'success', message: `Đã áp dụng mẫu "${template.name}" cho câu hỏi.` });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setTemplateBusyId(null);
    }
  };

  const handleDeleteTemplate = async (template) => {
    setTemplateBusyId(template.id);
    try {
      await rubricApi.deleteTemplate(selectedSubjectId, template.id);
      setTemplates(prev => prev.filter(t => t.id !== template.id));
      showToast({ type: 'success', message: `Đã xoá mẫu "${template.name}".` });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setTemplateBusyId(null);
    }
  };

  const templateCriteriaCount = (t) => {
    try {
      const parsed = JSON.parse(t.criteriaJson || '[]');
      return Array.isArray(parsed) ? parsed.length : 0;
    } catch (_) {
      return 0;
    }
  };

  const selectedVersion = questions.find(q => q.currentVersion.versionId === selectedVersionId)?.currentVersion;
  const isVersionEditable = selectedVersion?.status === 'DRAFT';

  // 3. Fetch Rubric Criteria for selectedVersionId from BE
  useEffect(() => {
    if (!selectedVersionId) {
      setRubricCriteria([]);
      return;
    }

    loadRubric();
  }, [selectedVersionId]);

  async function loadRubric() {
    try {
      const data = await rubricApi.getRubric(selectedVersionId);
      applyRubricResponse(data);
    } catch (err) {
      setRubricCriteria([]);
      setVersionTotalScore(null);
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  }

  // BE answers rubric reads/writes with the version DTO ({ rubric, lockVersion, ... }) or a bare rubric
  function applyRubricResponse(data) {
    const criteriaList = data?.rubric?.criteria || data?.criteria || [];
    setRubricCriteria(Array.isArray(criteriaList) ? criteriaList : []);
    if (data?.lockVersion != null) setVersionLock(data.lockVersion);
    const total = data?.rubric?.totalScore ?? data?.totalScore;
    setVersionTotalScore(total != null ? total : null);
  }

  const handleRubricSubmit = async (e) => {
    e.preventDefault();
    if (!criteriaName.trim() || !selectedVersionId) return;

    setIsSubmitting(true);
    try {
      const payload = {
        name: criteriaName.trim(),
        description: criteriaDescription.trim() || 'Tiêu chí đánh giá chất lượng câu trả lời',
        maxScore: Number(criteriaMaxScore),
        orderIndex: editingCriterion ? (editingCriterion.orderIndex || 1) : (rubricCriteria.length + 1),
        ...(versionLock != null ? { expectedVersion: versionLock } : {})
      };

      if (editingCriterion) {
        const critId = editingCriterion.criterionId || editingCriterion.id;
        applyRubricResponse(await rubricApi.updateCriterion(selectedVersionId, critId, payload));
        showToast({ type: 'success', message: 'Cập nhật tiêu chí Rubric thành công!' });
      } else {
        applyRubricResponse(await rubricApi.addCriterion(selectedVersionId, payload));
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
      const res = await rubricApi.deleteCriterion(selectedVersionId, criterionId, versionLock);
      if (res) applyRubricResponse(res);
      else await loadRubric();
      showToast({ type: 'success', message: 'Đã xóa tiêu chí Rubric khỏi hệ thống!' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  const currentTotalScore = versionTotalScore != null 
    ? Number(versionTotalScore) 
    : rubricCriteria.reduce((sum, c) => sum + Number(c.maxScore || 0), 0);
  const isBudgetValid = Math.abs(currentTotalScore - 10.0) < 0.05;
  const progressPercent = Math.min(Math.max((currentTotalScore / 10.0) * 100, 0), 100);

  return (
    <div className="space-y-6 animate-modal-entry text-ink max-w-7xl mx-auto w-full pb-10">

      {/* 1. Header Bar */}
      <div className="bg-paper border-2 border-green-500 rounded-2xl p-5 sm:p-6 md:p-8 shadow-[4px_4px_0_#DEB9B5] relative overflow-hidden">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 relative z-10">
          <div className="flex-1 space-y-2">
            <div className="inline-flex items-center gap-1.5 px-3 py-0.5 bg-pink-100 border-2 border-green-500 rounded-full shadow-[2px_2px_0_#DEB9B5]">
              <Award className="w-4 h-4 text-pink-700" />
              <span className="text-xs font-semibold text-pink-900 tracking-wide">
                Phân Hệ Quản Lý Rubric Đánh Giá (Group 1)
              </span>
            </div>
            <h1 className="font-heading font-extrabold text-xl sm:text-2xl text-ink tracking-tight">
              Ma Trận Tiêu Chí Rubric Điểm Vấn Đáp
            </h1>
            <p className="text-xs sm:text-sm text-on-surface-variant max-w-2xl">
              Tiêu chuẩn chấm điểm tự động. Bắt buộc tổng điểm tiêu chí chính xác 10.0 điểm theo quy chế khảo thí.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            {/* Select Subject */}
            <select 
              value={selectedSubjectId}
              onChange={(e) => setSelectedSubjectId(e.target.value)}
              className="bg-paper text-ink font-semibold text-xs px-3.5 py-2.5 rounded-xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] focus:outline-none focus:border-green-700 cursor-pointer transition-all"
            >
              {subjects.length === 0 && <option value="">Chưa có môn học</option>}
              {subjects.map((s) => (
                <option key={s.subjectId} value={s.subjectId}>
                  [{s.subjectCode}] {s.subjectName}
                </option>
              ))}
            </select>

            {/* Add Rubric Criterion Button */}
            <button 
              type="button"
              disabled={!selectedVersionId || !isVersionEditable}
              onClick={() => {
                setEditingCriterion(null);
                setCriteriaName('');
                setCriteriaDescription('');
                setCriteriaMaxScore('2.5');
                setIsRubricModalOpen(true);
              }} 
              className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-full bg-pink-500 hover:bg-pink-400 text-ink font-bold text-xs border-2 border-green-600 shadow-[3px_3px_0_#DEB9B5] hover:shadow-[1px_1px_0_#DEB9B5] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all disabled:opacity-50 cursor-pointer whitespace-nowrap"
            >
              <Plus className="w-4 h-4 text-pink-900 font-bold" />
              <span>Thêm Tiêu Chí Rubric</span>
            </button>
          </div>
        </div>
      </div>

      {/* 2. Select Version Bar (if questions exist) */}
      {questions.length > 0 && (
        <div className="bg-paper border-2 border-green-500 rounded-2xl p-4 sm:p-5 shadow-[3px_3px_0_#DEB9B5]">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
            <div className="flex items-center gap-2 shrink-0">
              <Sliders className="w-4 h-4 text-green-700" />
              <span className="text-xs font-semibold text-ink">
                Chọn câu hỏi cần cấu hình Rubric:
              </span>
            </div>
            <select
              value={selectedVersionId}
              onChange={(e) => setSelectedVersionId(e.target.value)}
              className="w-full md:max-w-3xl bg-green-50 text-ink text-xs px-3.5 py-2.5 rounded-xl border-2 border-green-500 shadow-[2px_2px_0_#DADFD3] focus:outline-none focus:border-green-700 cursor-pointer transition-all truncate"
            >
              {questions.map((q) => (
                <option key={q.questionId} value={q.currentVersion.versionId}>
                  #{q.questionCode} v{q.currentVersion.versionNumber} [{q.currentVersion.status}] — {q.currentVersion.content} {q.currentVersion.bloomLevelLabel ? `(${q.currentVersion.bloomLevelLabel})` : ''}
                </option>
              ))}
            </select>
          </div>
          {selectedVersion && !isVersionEditable && (
            <p className="mt-3 text-[11px] font-medium text-amber-800 bg-amber-50 border border-amber-300 rounded-lg px-3 py-2">
              Phiên bản này đang ở trạng thái {selectedVersion.status} — chỉ phiên bản DRAFT mới chỉnh sửa hoặc áp dụng mẫu rubric được.
            </p>
          )}
        </div>
      )}

      {/* 2b. Rubric templates of the subject */}
      {selectedSubjectId && (
        <div className="bg-paper border-2 border-green-500 rounded-2xl p-4 sm:p-5 shadow-[3px_3px_0_#DEB9B5] space-y-3 text-xs">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="font-heading font-bold text-sm">Mẫu Rubric của môn học ({templates.length})</h2>
            <form onSubmit={handleSaveAsTemplate} className="flex items-center gap-2">
              <input
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
                placeholder="Tên mẫu mới"
                aria-label="Tên mẫu rubric"
                className="bg-white border-2 border-green-300 rounded-xl p-2 text-xs focus:outline-none focus:ring-2 focus:ring-green-600"
              />
              <button
                type="submit"
                disabled={!templateName.trim() || rubricCriteria.length === 0 || templateBusyId === 'new'}
                title={rubricCriteria.length === 0 ? 'Rubric hiện tại chưa có tiêu chí' : ''}
                className="px-3 py-2 rounded-xl bg-white border-2 border-green-500 font-bold hover:bg-green-50 disabled:opacity-50 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-green-600"
              >
                {templateBusyId === 'new' && <Loader2 className="w-3.5 h-3.5 animate-spin" />} Lưu rubric hiện tại thành mẫu
              </button>
            </form>
          </div>
          {templates.length === 0 ? (
            <p className="text-on-surface-variant">Chưa có mẫu nào. Soạn rubric cho một câu hỏi rồi lưu lại để tái sử dụng.</p>
          ) : (
            <ul className="divide-y divide-green-100 border-2 border-green-200 rounded-xl bg-white">
              {templates.map(t => (
                <li key={t.id} className="p-3 flex flex-wrap items-center justify-between gap-2">
                  <span>
                    <span className="font-bold">{t.name}</span>
                    <span className="text-on-surface-variant"> • {templateCriteriaCount(t)} tiêu chí</span>
                  </span>
                  <span className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => handleApplyTemplate(t)}
                      disabled={!isVersionEditable || templateBusyId === t.id}
                      title={!isVersionEditable ? 'Chỉ áp dụng được cho phiên bản DRAFT' : 'Thay rubric của câu hỏi đang chọn bằng mẫu này'}
                      className="px-3 py-1.5 rounded-xl bg-green-600 hover:bg-green-700 text-white font-bold disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2"
                    >
                      Áp dụng
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDeleteTemplate(t)}
                      disabled={templateBusyId === t.id}
                      aria-label={`Xoá mẫu ${t.name}`}
                      className="px-2 py-1.5 rounded-xl border-2 border-pink-300 text-pink-800 hover:bg-pink-50 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-pink-500"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* 3. Rubric Budget Tracker (10.0 points meter banner) */}
      <div className="bg-paper border-2 border-green-500 rounded-2xl p-5 sm:p-6 shadow-[4px_4px_0_#DEB9B5] relative overflow-hidden">
        <div className="absolute left-0 top-0 bottom-0 w-2 bg-green-500"></div>
        <div className="pl-2 space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs text-green-800 uppercase tracking-widest font-bold">
                [ RUBRIC BUDGET TRACKER ]
              </span>
              <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold shadow-[2px_2px_0_#DADFD3] border-2 ${
                isBudgetValid 
                  ? 'bg-green-100 border-green-500 text-green-900' 
                  : 'bg-pink-100 border-pink-400 text-pink-900'
              }`}>
                <CheckCircle2 className={`w-3.5 h-3.5 ${isBudgetValid ? 'text-green-700' : 'text-pink-700'}`} />
                <span>{isBudgetValid ? '[ ĐẠT CHUẨN 10.0 ]' : `[ CHƯA ĐẠT: ${currentTotalScore.toFixed(1)}/10.0 ]`}</span>
              </span>
            </div>
            <div className="flex items-baseline gap-1">
              <span className="text-xs text-on-surface-variant uppercase">Tổng điểm:</span>
              <span className={`font-heading text-xl sm:text-2xl font-bold tracking-tight ${isBudgetValid ? 'text-green-800' : 'text-pink-700'}`}>
                {currentTotalScore.toFixed(1)}
              </span>
              <span className="text-sm text-on-surface-variant font-normal">/ 10.0</span>
            </div>
          </div>

          {/* Linear Segmented Paper Progress Meter */}
          <div className="space-y-1.5">
            <div className="w-full h-3.5 bg-green-100 rounded-full border-2 border-green-500 overflow-hidden p-0.5 shadow-inner">
              <div 
                className={`h-full rounded-full transition-all duration-500 ease-out flex items-center justify-end pr-1 ${
                  isBudgetValid ? 'bg-green-500' : 'bg-pink-500'
                }`} 
                style={{ width: `${progressPercent}%` }}
              >
                <div className="w-1.5 h-1.5 rounded-full bg-paper"></div>
              </div>
            </div>
            <div className="flex justify-between items-center text-on-surface-variant text-[11px]">
              <span>0.0đ</span>
              <span className="text-ink font-medium">Ngân sách quy chuẩn: 10.0 điểm</span>
              <span>10.0đ ({progressPercent.toFixed(0)}%)</span>
            </div>
          </div>

          <p className={`text-xs rounded-lg p-2.5 flex items-center gap-2 border ${
            isBudgetValid 
              ? 'text-green-900 bg-green-50 border-green-300' 
              : 'text-pink-900 bg-pink-50 border-pink-300'
          }`}>
            <CheckCircle2 className="w-4 h-4 text-green-700 shrink-0" />
            <span>
              {isBudgetValid 
                ? <>Tổng điểm các tiêu chí con đã bằng chính xác <strong>10.0 điểm</strong>. Đủ điều kiện để phê duyệt câu hỏi.</>
                : <>Tổng điểm hiện tại là <strong>{currentTotalScore.toFixed(1)} điểm</strong>. Vui lòng điều chỉnh lại điểm các tiêu chí để đạt chính xác <strong>10.0 điểm</strong>.</>
              }
            </span>
          </p>
        </div>
      </div>

      {/* 4. Rubric Table Card */}
      <div className="bg-paper border-2 border-green-500 rounded-2xl p-4 sm:p-6 shadow-[6px_6px_0_#D5A6A2] space-y-4">
        <div className="flex items-center justify-between border-b-2 border-cream pb-3">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-pink-700" />
            <h2 className="font-heading font-semibold text-base sm:text-lg text-ink">
              Danh Sách Tiêu Chí Đánh Giá Chi Tiết
            </h2>
          </div>
          <span className="text-xs text-on-surface-variant bg-green-100 border border-green-500 rounded-full px-2.5 py-0.5 shadow-[1px_1px_0_#DEB9B5]">
            Phiên bản ban hành v1.0
          </span>
        </div>

        {isLoading ? (
          <div className="p-10 text-center bg-paper space-y-3">
            <Loader2 className="w-7 h-7 text-green-700 animate-spin mx-auto" />
            <p className="text-xs text-on-surface-variant font-medium">Đang tải ma trận Rubric từ Backend...</p>
          </div>
        ) : rubricCriteria.length === 0 ? (
          <div className="p-10 text-center bg-paper space-y-3">
            <FolderPlus className="w-9 h-9 text-outline mx-auto" />
            <p className="text-sm font-bold text-ink">Chưa Có Tiêu Chí Rubric Trực Tuyến</p>
            <p className="text-xs text-on-surface-variant max-w-md mx-auto">
              Chưa có tiêu chí Rubric nào được khởi tạo trên Backend cho môn học/câu hỏi được chọn. Bấm nút "Thêm Tiêu Chí Rubric" để khởi tạo tiêu chí mới trên cơ sở dữ liệu.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-ink border-collapse">
              <thead>
                <tr className="border-b-2 border-green-500 bg-green-50 text-ink font-semibold text-xs">
                  <th className="py-2.5 px-3 w-16 text-center">STT</th>
                  <th className="py-2.5 px-3 w-1/4">Tên Tiêu Chí</th>
                  <th className="py-2.5 px-3">Mô Tả Tiêu Chí</th>
                  <th className="py-2.5 px-3 w-28 text-right">Điểm Tối Đa</th>
                  <th className="py-2.5 px-3 w-32 text-center">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y-2 divide-cream">
                {rubricCriteria.map((item, idx) => (
                  <tr key={item.criterionId || item.id || idx} className="hover:bg-pink-50/60 transition-colors">
                    <td className="py-3 px-3 text-center font-bold text-green-800 font-mono">
                      {String(idx + 1).padStart(2, '0')}
                    </td>
                    <td className="py-3 px-3 align-top">
                      <span className="font-semibold text-ink block">{item.name}</span>
                    </td>
                    <td className="py-3 px-3 align-top text-on-surface-variant leading-relaxed">
                      {item.description || 'Nội dung chấm điểm'}
                    </td>
                    <td className="py-3 px-3 align-top text-right whitespace-nowrap">
                      <span className="inline-block px-2.5 py-1 bg-green-100 border-2 border-green-500 rounded-lg font-bold text-green-900 shadow-[2px_2px_0_#DADFD3]">
                        {Number(item.maxScore).toFixed(1)} điểm
                      </span>
                    </td>
                    <td className="py-3 px-3 align-top text-center whitespace-nowrap">
                      <div className="inline-flex items-center gap-1.5">
                        <button 
                          type="button"
                          onClick={() => {
                            setEditingCriterion(item);
                            setCriteriaName(item.name || '');
                            setCriteriaDescription(item.description || '');
                            setCriteriaMaxScore(String(item.maxScore || '2.5'));
                            setIsRubricModalOpen(true);
                          }} 
                          className="px-2.5 py-1 text-xs font-semibold text-green-800 bg-paper hover:bg-green-100 rounded-lg border-2 border-green-500 shadow-[1px_1px_0_#DEB9B5] active:translate-x-[1px] active:translate-y-[1px] cursor-pointer transition-all"
                        >
                          Chỉnh sửa
                        </button>
                        <button 
                          type="button"
                          onClick={() => handleDeleteCriterion(item.criterionId || item.id)} 
                          className="px-2.5 py-1 text-xs font-semibold text-pink-700 bg-pink-50 hover:bg-pink-100 rounded-lg border-2 border-pink-400 shadow-[1px_1px_0_#DEB9B5] active:translate-x-[1px] active:translate-y-[1px] cursor-pointer transition-all"
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Table Footer Summary */}
        <div className="pt-3 border-t-2 border-green-500 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-paper">
          <div className="flex items-center gap-2">
            <span className="text-xs text-on-surface-variant">Tổng số tiêu chí:</span>
            <span className="px-2.5 py-0.5 bg-paper border-2 border-green-500 rounded-md text-xs font-bold text-ink shadow-[2px_2px_0_#DADFD3]">
              {rubricCriteria.length}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-ink">Tổng điểm Rubric:</span>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-green-50 border-2 border-green-500 rounded-full shadow-[2px_2px_0_#DEB9B5]">
              <span className="w-2.5 h-2.5 rounded-full bg-green-600"></span>
              <span className="text-xs font-bold text-green-900">
                {currentTotalScore.toFixed(1)} / 10.0 điểm
              </span>
              <span className="text-[11px] text-green-700 font-semibold">
                {isBudgetValid ? '(Chuẩn)' : '(Chưa chuẩn)'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* 5. Rubric Modal (Thêm / Chỉnh Sửa Tiêu Chí Rubric) */}
      {isRubricModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-paper border-2 border-green-500 rounded-2xl max-w-md w-full p-6 shadow-[6px_6px_0_#DEB9B5] space-y-4 animate-modal-entry text-ink">
            <div className="flex items-center justify-between pb-3 border-b-2 border-cream">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-pink-100 border-2 border-green-500 text-pink-700 flex items-center justify-center font-bold text-sm shadow-[2px_2px_0_#DEB9B5]">
                  <Award className="w-4 h-4 text-pink-700" />
                </div>
                <h3 className="font-heading font-bold text-base text-ink">
                  {editingCriterion ? 'Chỉnh Sửa Tiêu Chí Rubric' : 'Thêm Tiêu Chí Rubric Mới'}
                </h3>
              </div>
              <button 
                type="button"
                onClick={() => setIsRubricModalOpen(false)} 
                className="text-pink-700 hover:text-pink-900 p-1 cursor-pointer transition-colors"
                aria-label="Đóng"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleRubricSubmit} className="space-y-4 text-xs sm:text-sm text-ink">
              <div>
                <label className="font-bold text-ink block mb-1">Tên Tiêu Chí</label>
                <input 
                  type="text" 
                  required
                  value={criteriaName}
                  onChange={(e) => setCriteriaName(e.target.value)}
                  placeholder="Ví dụ: Hiểu cơ chế Event-Driven Architecture..." 
                  className="w-full bg-surface-container-lowest border-2 border-green-500 rounded-xl p-2.5 text-xs text-ink placeholder:text-outline focus:outline-none focus:border-green-700 shadow-[1px_1px_0_#DEB9B5]" 
                />
              </div>

              <div>
                <label className="font-bold text-ink block mb-1">Mô Tả Đánh Giá</label>
                <textarea 
                  rows={3} 
                  value={criteriaDescription}
                  onChange={(e) => setCriteriaDescription(e.target.value)}
                  placeholder="Mô tả tiêu chuẩn đạt điểm của câu trả lời..." 
                  className="w-full bg-surface-container-lowest border-2 border-green-500 rounded-xl p-2.5 text-xs text-ink placeholder:text-outline focus:outline-none focus:border-green-700 shadow-[1px_1px_0_#DEB9B5]"
                />
              </div>

              <div>
                <label className="font-bold text-ink block mb-1">Điểm Tối Đa (Tổng cần bằng 10.0)</label>
                <input 
                  type="number" 
                  step="0.5"
                  min="0.5"
                  required
                  value={criteriaMaxScore}
                  onChange={(e) => setCriteriaMaxScore(e.target.value)}
                  className="w-full bg-surface-container-lowest border-2 border-green-500 rounded-xl p-2.5 text-xs text-ink placeholder:text-outline focus:outline-none focus:border-green-700 shadow-[1px_1px_0_#DEB9B5]" 
                />
              </div>

              <div className="pt-3 border-t-2 border-cream flex items-center justify-end gap-2.5">
                <button 
                  type="button" 
                  onClick={() => setIsRubricModalOpen(false)} 
                  className="px-4 py-2 rounded-full border-2 border-outline-variant text-xs font-semibold text-on-surface-variant bg-paper shadow-[2px_2px_0_#DEB9B5] hover:bg-green-50 cursor-pointer transition-all"
                >
                  Hủy
                </button>
                <button 
                  type="submit" 
                  disabled={isSubmitting}
                  className="px-5 py-2 rounded-full bg-pink-500 hover:bg-pink-400 text-ink text-xs font-bold border-2 border-green-600 shadow-[3px_3px_0_#DEB9B5] hover:shadow-[1px_1px_0_#DEB9B5] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
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
