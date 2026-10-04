import React, { useState, useEffect, useMemo } from 'react';
import { 
  Database, 
  FileText, 
  Sparkles, 
  Upload, 
  Download, 
  Plus, 
  Search, 
  Filter, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  RefreshCw, 
  Trash2, 
  Edit3, 
  Eye, 
  EyeOff, 
  HelpCircle, 
  Layers, 
  BookOpen, 
  ArrowRight, 
  Check, 
  X, 
  RotateCcw, 
  FileSpreadsheet, 
  ChevronDown, 
  ChevronUp, 
  FileCheck, 
  FileWarning, 
  Cpu, 
  ListOrdered, 
  Sliders
} from 'lucide-react';
import { lecturerCatalogApi } from '../api/lecturerCatalogApi';
import { courseDocumentApi } from '../api/courseDocumentApi';
import { questionBankApi } from '../api/questionBankApi';
import { questionGenerationApi } from '../api/questionGenerationApi';
import { questionImportApi } from '../api/questionImportApi';
import { rubricApi } from '../api/rubricApi';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import { ConfirmModal } from '../components/common/ConfirmModal';
import { SkeletonCard, SkeletonTable } from '../components/common/Skeleton';

// Bloom Taxonomy Levels & Color Tokens (Pedagogical UI)
const BLOOM_LEVELS = [
  { value: 'REMEMBER', label: '1. Nhận Biết (Remember)', color: 'bg-blue-50 text-blue-700 border-blue-200' },
  { value: 'UNDERSTAND', label: '2. Thông Hiểu (Understand)', color: 'bg-cyan-50 text-cyan-700 border-cyan-200' },
  { value: 'APPLY', label: '3. Vận Dụng (Apply)', color: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  { value: 'ANALYZE', label: '4. Phân Tích (Analyze)', color: 'bg-amber-50 text-amber-700 border-amber-200' },
  { value: 'EVALUATE', label: '5. Đánh Giá (Evaluate)', color: 'bg-orange-50 text-orange-700 border-orange-200' },
  { value: 'CREATE', label: '6. Sáng Tạo (Create)', color: 'bg-purple-50 text-purple-700 border-purple-200' }
];

// Split total 30/40/30 over REMEMBER/UNDERSTAND/APPLY; BE requires the sum to equal total exactly
const BLOOM_SPLIT = [['UNDERSTAND', 0.4], ['REMEMBER', 0.3], ['APPLY', 0.3]];

function buildBloomDistribution(total) {
  const dist = {};
  let assigned = 0;
  BLOOM_SPLIT.forEach(([level, ratio]) => {
    dist[level] = Math.floor(total * ratio);
    assigned += dist[level];
  });
  for (let i = 0; assigned < total; i = (i + 1) % BLOOM_SPLIT.length) {
    dist[BLOOM_SPLIT[i][0]] += 1;
    assigned += 1;
  }
  return dist;
}

// Rubric criteria rules mirror BE CriterionRequest: name + description required, maxScore > 0
function validateCriteria(criteria) {
  if (criteria.length === 0) return 'Rubric cần ít nhất 1 tiêu chí.';
  if (criteria.some(c => !c.name?.trim() || !c.description?.trim())) {
    return 'Mỗi tiêu chí rubric cần có tên và mô tả.';
  }
  if (criteria.some(c => !(Number(c.maxScore) > 0))) {
    return 'Điểm tối đa của mỗi tiêu chí phải lớn hơn 0.';
  }
  return null;
}

function toRubricPayload(criteria) {
  return {
    totalScore: criteria.reduce((sum, c) => sum + Number(c.maxScore), 0),
    criteria: criteria.map((c, idx) => ({
      name: c.name.trim(),
      description: c.description.trim(),
      maxScore: Number(c.maxScore),
      levels: c.levels,
      orderIndex: idx + 1
    }))
  };
}

function RubricCriteriaEditor({ criteria, onChange }) {
  const updateAt = (idx, patch) => onChange(criteria.map((c, i) => i === idx ? { ...c, ...patch } : c));

  return (
    <>
      <div className="space-y-2">
        {criteria.map((crit, idx) => (
          <div key={idx} className="bg-white p-2 rounded-lg border border-slate-200 space-y-1.5">
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="Tên tiêu chí (VD: Tính đúng đắn)"
                value={crit.name}
                onChange={(e) => updateAt(idx, { name: e.target.value })}
                className="flex-1 px-2.5 py-1.5 rounded-lg text-xs border border-slate-200 font-medium focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
              <input
                type="number"
                min="0.5"
                step="0.5"
                max="999.99"
                value={crit.maxScore}
                onChange={(e) => updateAt(idx, { maxScore: e.target.value })}
                className="w-16 px-2 py-1.5 rounded-lg text-xs border border-slate-200 font-mono font-bold text-center focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
              <button
                type="button"
                onClick={() => onChange(criteria.filter((_, i) => i !== idx))}
                disabled={criteria.length <= 1}
                className="p-1.5 text-slate-400 hover:text-rose-600 disabled:opacity-30"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
            <input
              type="text"
              placeholder="Mô tả tiêu chí (bắt buộc)"
              value={crit.description}
              onChange={(e) => updateAt(idx, { description: e.target.value })}
              className="w-full px-2.5 py-1.5 rounded-lg text-[11px] border border-slate-200 focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={() => onChange([...criteria, { name: 'Tiêu chí bổ sung', description: '', maxScore: 2 }])}
        className="text-xs font-bold text-sky-600 hover:text-sky-700 inline-flex items-center gap-1"
      >
        <Plus className="w-3.5 h-3.5" />
        <span>Thêm tiêu chí chấm</span>
      </button>
    </>
  );
}

export function LecturerQuestionBankPage({ showToast }) {
  const { currentUser } = useAuth();

  // Active workspace tab: 'bank' | 'documents' | 'review'
  const [activeTab, setActiveTab] = useState('bank');

  // Subjects & Topics
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [topics, setTopics] = useState([]);
  const [isLoadingCatalog, setIsLoadingCatalog] = useState(true);

  // Tab 1: Documents State
  const [documents, setDocuments] = useState([]);
  const [isLoadingDocs, setIsLoadingDocs] = useState(false);
  const [isUploadingDoc, setIsUploadingDoc] = useState(false);
  const [docFileToUpload, setDocFileToUpload] = useState(null);

  // Tab 2: Bank State
  const [bankQuestions, setBankQuestions] = useState([]);
  const [isLoadingBank, setIsLoadingBank] = useState(false);
  const [searchBankKeyword, setSearchBankKeyword] = useState('');
  const [filterTopicId, setFilterTopicId] = useState('ALL');
  const [filterBloom, setFilterBloom] = useState('ALL');
  const [expandedExpectedAnswers, setExpandedExpectedAnswers] = useState({});

  // Tab 3: Review Queue State
  const [reviewVersions, setReviewVersions] = useState([]);
  const [isLoadingReview, setIsLoadingReview] = useState(false);
  const [reviewStatusFilter, setReviewStatusFilter] = useState('DRAFT');

  // Modals state
  const [isCreateTopicModalOpen, setIsCreateTopicModalOpen] = useState(false);
  const [newTopicName, setNewTopicName] = useState('');
  const [newTopicDesc, setNewTopicDesc] = useState('');

  const [isManualQuestionModalOpen, setIsManualQuestionModalOpen] = useState(false);
  const [manualTopicId, setManualTopicId] = useState('');
  const [manualContent, setManualContent] = useState('');
  const [manualExpectedAnswer, setManualExpectedAnswer] = useState('');
  const [manualBloom, setManualBloom] = useState('UNDERSTAND');
  const [manualCriteria, setManualCriteria] = useState([
    { name: 'Nội dung cốt lõi', description: 'Đáp ứng đúng trọng tâm câu hỏi', maxScore: 5 },
    { name: 'Lập luận và minh họa', description: 'Đưa ra ví dụ hoặc phân tích mở rộng', maxScore: 5 }
  ]);
  const [isSubmittingQuestion, setIsSubmittingQuestion] = useState(false);

  // AI RAG Generation Modal / Drawer State
  const [isAiGenModalOpen, setIsAiGenModalOpen] = useState(false);
  const [genTopicId, setGenTopicId] = useState('');
  const [genDocumentIds, setGenDocumentIds] = useState([]);
  const [genTotalQuestions, setGenTotalQuestions] = useState(3);
  const [genLecturerNote, setGenLecturerNote] = useState('');
  const [isGeneratingAi, setIsGeneratingAi] = useState(false);
  const [failedGenRequest, setFailedGenRequest] = useState(null);

  // Import Modal State
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [isDryRun, setIsDryRun] = useState(true);
  const [isImporting, setIsImporting] = useState(false);
  const [importReport, setImportReport] = useState(null);

  // Confirm delete / action targets
  const [deleteDocTarget, setDeleteDocTarget] = useState(null);
  const [deleteDraftTarget, setDeleteDraftTarget] = useState(null);
  const [archiveQuestionTarget, setArchiveQuestionTarget] = useState(null);

  // Question detail modal: null | { isLoading, detail, history }
  const [questionDetail, setQuestionDetail] = useState(null);

  // Edit draft modal: null | { version, content, expectedAnswer, bloomLevel, criteria }
  const [editDraft, setEditDraft] = useState(null);
  const [isSavingDraft, setIsSavingDraft] = useState(false);

  // Reject Modal State
  const [rejectVersionTarget, setRejectVersionTarget] = useState(null);
  const [rejectReason, setRejectReason] = useState('');

  // Single Question Regenerate State
  const [regenVersionTarget, setRegenVersionTarget] = useState(null);
  const [regenFeedback, setRegenFeedback] = useState('');

  // 1. Load subjects assigned to lecturer
  useEffect(() => {
    async function loadCatalog() {
      setIsLoadingCatalog(true);
      try {
        const data = await lecturerCatalogApi.getAssignedSubjects();
        const list = Array.isArray(data) ? data : data.content || [];
        setSubjects(list);
        if (list.length > 0 && !selectedSubjectId) {
          setSelectedSubjectId(list[0].subjectId);
        }
      } catch (err) {
        showToast({ type: 'error', message: getErrorMessage(err) });
        setSubjects([]);
      } finally {
        setIsLoadingCatalog(false);
      }
    }

    loadCatalog();
  }, []);

  // 2. Load Topics whenever selectedSubjectId changes
  useEffect(() => {
    if (!selectedSubjectId) return;

    async function loadTopics() {
      try {
        const list = await lecturerCatalogApi.getTopics(selectedSubjectId);
        setTopics(Array.isArray(list) ? list : []);
      } catch (err) {
        setTopics([]);
      }
    }

    loadTopics();
  }, [selectedSubjectId]);

  // 3. Load tab specific data
  useEffect(() => {
    if (!selectedSubjectId) return;

    if (activeTab === 'documents') {
      loadDocuments();
    } else if (activeTab === 'bank') {
      loadBank();
    } else if (activeTab === 'review') {
      loadReviewQueue();
    }
  }, [selectedSubjectId, activeTab]);

  const loadDocuments = async () => {
    setIsLoadingDocs(true);
    try {
      const data = await courseDocumentApi.getDocuments(selectedSubjectId);
      setDocuments(Array.isArray(data) ? data : []);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
      setDocuments([]);
    } finally {
      setIsLoadingDocs(false);
    }
  };

  const loadBank = async () => {
    setIsLoadingBank(true);
    try {
      const params = { page: 0, size: 50 };
      if (filterTopicId !== 'ALL') params.topicId = filterTopicId;
      if (filterBloom !== 'ALL') params.bloomLevel = filterBloom;
      if (searchBankKeyword) params.keyword = searchBankKeyword;

      const data = await questionBankApi.searchBank(selectedSubjectId, params);
      setBankQuestions(data.content || data || []);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
      setBankQuestions([]);
    } finally {
      setIsLoadingBank(false);
    }
  };

  const loadReviewQueue = async () => {
    setIsLoadingReview(true);
    try {
      const data = await questionBankApi.searchVersions(selectedSubjectId, {
        status: reviewStatusFilter === 'ALL' ? undefined : reviewStatusFilter,
        page: 0,
        size: 50
      });
      setReviewVersions(data.content || data || []);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
      setReviewVersions([]);
    } finally {
      setIsLoadingReview(false);
    }
  };

  // Create Topic
  const handleCreateTopic = async (e) => {
    e.preventDefault();
    if (!newTopicName.trim()) return;

    try {
      const created = await lecturerCatalogApi.createTopic(selectedSubjectId, {
        name: newTopicName.trim(),
        description: newTopicDesc.trim()
      });
      setTopics(prev => [...prev, created]);
      showToast({ type: 'success', message: 'Tạo chủ đề đề cương thành công!' });
      setIsCreateTopicModalOpen(false);
      setNewTopicName('');
      setNewTopicDesc('');
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  // Upload Document
  const handleUploadDocument = async () => {
    if (!docFileToUpload) return;
    setIsUploadingDoc(true);
    try {
      await courseDocumentApi.uploadDocument(selectedSubjectId, docFileToUpload);
      setDocFileToUpload(null);
      showToast({ type: 'success', message: 'Đã nhận tài liệu! Đang xử lý bóc tách và lập chỉ mục ngầm.' });
      loadDocuments();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsUploadingDoc(false);
    }
  };

  // Retry Indexing
  const handleRetryIndexing = async (documentId) => {
    try {
      await courseDocumentApi.retryIndexing(documentId);
      showToast({ type: 'success', message: 'Đã gửi yêu cầu lập chỉ mục lại.' });
      loadDocuments();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  // Delete Document
  const handleConfirmDeleteDoc = async () => {
    if (!deleteDocTarget) return;
    try {
      await courseDocumentApi.deleteDocument(deleteDocTarget.documentId);
      showToast({ type: 'success', message: 'Xóa tài liệu giáo trình thành công.' });
      setDocuments(prev => prev.filter(d => d.documentId !== deleteDocTarget.documentId));
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setDeleteDocTarget(null);
    }
  };

  // Create Manual Question
  const handleCreateManualQuestion = async (e) => {
    e.preventDefault();
    if (!manualContent.trim() || !manualExpectedAnswer.trim()) {
      showToast({ type: 'error', message: 'Vui lòng nhập nội dung câu hỏi và đáp án mong đợi.' });
      return;
    }

    const rubricError = validateCriteria(manualCriteria);
    if (rubricError) {
      showToast({ type: 'error', message: rubricError });
      return;
    }

    setIsSubmittingQuestion(true);
    try {
      const payload = {
        topicId: manualTopicId || null,
        content: manualContent.trim(),
        expectedAnswer: manualExpectedAnswer.trim(),
        bloomLevel: manualBloom,
        rubric: toRubricPayload(manualCriteria)
      };

      await questionBankApi.createManualQuestion(selectedSubjectId, payload);
      showToast({ type: 'success', message: 'Tạo câu hỏi thành công! Bản nháp đã được gửi vào hàng đợi duyệt.' });
      setActiveTab('review');
      loadReviewQueue();
      setIsManualQuestionModalOpen(false);
      setManualContent('');
      setManualExpectedAnswer('');
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSubmittingQuestion(false);
    }
  };

  // Copy on write: create draft from approved
  const handleCreateDraftFromApproved = async (questionId) => {
    try {
      await questionBankApi.createDraftFromApproved(questionId);
      showToast({ type: 'success', message: 'Đã tạo bản nháp mới (Copy-on-Write). Chuyển đến Hàng đợi duyệt để chỉnh sửa.' });
      setActiveTab('review');
      loadReviewQueue();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  // Question detail + version history
  const handleOpenQuestionDetail = async (questionId) => {
    setQuestionDetail({ isLoading: true });
    try {
      const [detail, history] = await Promise.all([
        questionBankApi.getQuestionDetail(questionId),
        questionBankApi.getHistory(questionId)
      ]);
      setQuestionDetail({ isLoading: false, detail, history: Array.isArray(history) ? history : [] });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
      setQuestionDetail(null);
    }
  };

  // Archive question
  const handleConfirmArchive = async () => {
    if (!archiveQuestionTarget) return;
    try {
      await questionBankApi.archiveQuestion(archiveQuestionTarget.questionId);
      showToast({ type: 'success', message: 'Đã lưu trữ câu hỏi (ARCHIVED).' });
      loadBank();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setArchiveQuestionTarget(null);
    }
  };

  // Trigger RAG Generation
  const handleTriggerAiGeneration = async () => {
    if (genDocumentIds.length === 0) {
      showToast({ type: 'error', message: 'Vui lòng chọn ít nhất 1 tài liệu giáo trình READY để sinh câu hỏi.' });
      return;
    }

    setIsGeneratingAi(true);
    try {
      const total = Number(genTotalQuestions);
      const payload = {
        subjectId: selectedSubjectId,
        topicId: genTopicId || null,
        documentIds: genDocumentIds,
        totalQuestions: total,
        bloomDistribution: buildBloomDistribution(total),
        lecturerNote: genLecturerNote.trim()
      };

      const res = await questionGenerationApi.generate(payload);
      handleGenerationResult(res);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsGeneratingAi(false);
    }
  };

  // BE runs generation synchronously and may return PARTIAL / FAILED with 201
  const handleGenerationResult = (res) => {
    if (res?.status === 'COMPLETED') {
      setFailedGenRequest(null);
      showToast({ type: 'success', message: `Đã sinh thành công ${res.generatedCount} câu hỏi AI RAG!` });
      setIsAiGenModalOpen(false);
      setActiveTab('review');
      loadReviewQueue();
      return;
    }
    setFailedGenRequest(res);
    if (res?.status === 'PARTIAL') {
      showToast({ type: 'warning', message: `Chỉ sinh được ${res.generatedCount}/${res.totalQuestions} câu hỏi. Có thể thử lại phần còn thiếu.` });
      loadReviewQueue();
    } else {
      showToast({ type: 'error', message: res?.errorMessage || 'Sinh câu hỏi thất bại. Vui lòng thử lại.' });
    }
  };

  const handleRetryGeneration = async () => {
    if (!failedGenRequest) return;
    setIsGeneratingAi(true);
    try {
      const res = await questionGenerationApi.retry(failedGenRequest.generationRequestId);
      handleGenerationResult(res);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsGeneratingAi(false);
    }
  };

  // Confirm Bloom Level (Strict Rule for AI RAG questions)
  const handleConfirmBloom = async (version) => {
    try {
      await questionBankApi.confirmBloom(version.versionId, {
        bloomLevel: version.bloomLevel,
        expectedVersion: version.lockVersion
      });
      setReviewVersions(prev => prev.map(v => v.versionId === version.versionId ? { ...v, bloomConfirmed: true } : v));
      showToast({ type: 'success', message: 'Đã thẩm định và xác nhận cấp độ nhận thức Bloom.' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  // Approve Draft
  const handleApproveVersion = async (version) => {
    try {
      await questionBankApi.approveVersion(version.versionId, {
        expectedVersion: version.lockVersion
      });
      showToast({ type: 'success', message: 'Phê duyệt thành công! Câu hỏi đã được đưa vào Ngân Hàng Đề Thi.' });
      loadReviewQueue();
      loadBank();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  // Reject Draft
  const handleConfirmReject = async () => {
    if (!rejectVersionTarget || !rejectReason.trim()) return;
    try {
      await questionBankApi.rejectVersion(rejectVersionTarget.versionId, {
        reason: rejectReason.trim(),
        expectedVersion: rejectVersionTarget.lockVersion
      });
      showToast({ type: 'success', message: 'Đã chuyển bản nháp sang trạng thái từ chối (REJECTED).' });
      loadReviewQueue();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setRejectVersionTarget(null);
      setRejectReason('');
    }
  };

  // Delete Draft
  const handleConfirmDeleteDraft = async () => {
    if (!deleteDraftTarget) return;
    try {
      await questionBankApi.deleteDraft(deleteDraftTarget.versionId);
      showToast({ type: 'success', message: 'Đã xóa hoàn toàn bản nháp khỏi hệ thống.' });
      setReviewVersions(prev => prev.filter(v => v.versionId !== deleteDraftTarget.versionId));
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setDeleteDraftTarget(null);
    }
  };

  // Single Question Regenerate with feedback
  const handleConfirmRegenerate = async () => {
    if (!regenVersionTarget) return;
    try {
      await questionBankApi.regenerateQuestion(regenVersionTarget.versionId, {
        feedback: regenFeedback.trim(),
        expectedVersion: regenVersionTarget.lockVersion
      });
      showToast({ type: 'success', message: 'AI đã tạo lại câu hỏi thành công theo yêu cầu của bạn!' });
      loadReviewQueue();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setRegenVersionTarget(null);
      setRegenFeedback('');
    }
  };

  // Edit DRAFT content + rubric in one PUT (UpdateDraftRequest accepts rubric)
  const handleOpenEditDraft = (version) => {
    setEditDraft({
      version,
      content: version.content || '',
      expectedAnswer: version.expectedAnswer || '',
      bloomLevel: version.bloomLevel || 'UNDERSTAND',
      criteria: (version.rubric?.criteria || []).map(c => ({
        name: c.name || '',
        description: c.description || '',
        maxScore: c.maxScore,
        levels: c.levels
      }))
    });
  };

  const handleSaveDraft = async (e) => {
    e.preventDefault();
    if (!editDraft.content.trim() || !editDraft.expectedAnswer.trim()) {
      showToast({ type: 'error', message: 'Vui lòng nhập nội dung câu hỏi và đáp án mong đợi.' });
      return;
    }
    const rubricError = validateCriteria(editDraft.criteria);
    if (rubricError) {
      showToast({ type: 'error', message: rubricError });
      return;
    }

    setIsSavingDraft(true);
    try {
      const updated = await questionBankApi.updateDraft(editDraft.version.versionId, {
        content: editDraft.content.trim(),
        expectedAnswer: editDraft.expectedAnswer.trim(),
        bloomLevel: editDraft.bloomLevel,
        rubric: toRubricPayload(editDraft.criteria),
        expectedVersion: editDraft.version.lockVersion
      });
      setReviewVersions(prev => prev.map(v => v.versionId === updated.versionId ? updated : v));
      showToast({ type: 'success', message: 'Đã lưu thay đổi bản nháp.' });
      setEditDraft(null);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSavingDraft(false);
    }
  };

  // Download Import Template
  const handleDownloadTemplate = async (format) => {
    try {
      const blob = await questionImportApi.downloadTemplate(format);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `question-import-template.${format}`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      showToast({ type: 'success', message: `Đã tải xuống tệp mẫu ${format.toUpperCase()}.` });
    } catch (err) {
      showToast({ type: 'error', message: 'Không thể tải file mẫu từ máy chủ.' });
    }
  };

  // Import Questions
  const handleImportQuestions = async (e) => {
    e.preventDefault();
    if (!importFile) return;

    setIsImporting(true);
    setImportReport(null);
    try {
      const report = await questionImportApi.importQuestions(selectedSubjectId, importFile, isDryRun);
      setImportReport(report);
      if (isDryRun) {
        showToast({ type: 'success', message: 'Thẩm định tệp dữ liệu hoàn tất! Xem báo cáo bên dưới.' });
      } else {
        showToast({ type: 'success', message: `Đã nhập thành công ${report.createdQuestions || 0} câu hỏi vào Hàng đợi duyệt!` });
        loadReviewQueue();
      }
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsImporting(false);
    }
  };

  // Helper toggle expected answer
  const toggleExpectedAnswer = (id) => {
    setExpandedExpectedAnswers(prev => ({ ...prev, [id]: !prev[id] }));
  };

  // Selected subject object
  const activeSubject = useMemo(() => {
    return subjects.find(s => s.subjectId === selectedSubjectId) || null;
  }, [subjects, selectedSubjectId]);

  return (
    <div className="space-y-6 animate-modal-entry">
      
      {/* HEADER SECTION & SUBJECT PICKER */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-sky-600 font-bold text-xs uppercase tracking-wider mb-1">
            <Database className="w-4 h-4" />
            <span>Module 1 &amp; Group 1 — Question Bank &amp; RAG Pipeline</span>
          </div>
          <h1 className="text-xl font-heading font-extrabold text-slate-900 tracking-tight">
            Ngân Hàng Câu Hỏi &amp; Quản Lý RAG Giáo Trình
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Quản trị kho học liệu, bóc tách vector hóa giáo trình, kích hoạt AI RAG và thẩm định ma trận Rubric.
          </p>
        </div>

        {/* SUBJECT SWITCHER DROPDOWN */}
        <div className="flex items-center gap-3">
          <div className="text-right hidden sm:block">
            <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Môn Học Phụ Trách</p>
            <p className="text-xs font-bold text-slate-800">{activeSubject ? activeSubject.subjectCode : 'Chưa chọn'}</p>
          </div>
          <select
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
            className="py-2.5 px-3.5 rounded-xl text-xs bg-slate-50 border border-slate-300 font-bold text-slate-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-sky-500 cursor-pointer shadow-xs"
          >
            {subjects.map(s => (
              <option key={s.subjectId} value={s.subjectId}>
                [{s.subjectCode}] {s.subjectName}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* TOPIC BANNER & SHORTCUT BAR */}
      <div className="bg-slate-900 text-slate-200 p-4 rounded-2xl border border-slate-800 flex flex-wrap items-center justify-between gap-3 shadow-sm">
        <div className="flex items-center gap-2 text-xs">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span className="text-slate-400 font-medium">Chủ đề đề cương:</span>
          <span className="font-bold text-white">{topics.length} chủ đề đang kích hoạt</span>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setIsCreateTopicModalOpen(true)}
            className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 transition active:scale-95 flex items-center gap-1.5"
          >
            <Plus className="w-3.5 h-3.5 text-sky-400" />
            <span>Thêm Chủ Đề</span>
          </button>
        </div>
      </div>

      {/* THREE MAIN WORKSPACE TABS */}
      <div className="flex items-center gap-2 border-b border-slate-200 pb-1">
        <button
          type="button"
          onClick={() => setActiveTab('bank')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'bank'
              ? 'bg-sky-600 text-white shadow-sm shadow-sky-600/20'
              : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
          }`}
        >
          <Database className="w-4 h-4" />
          <span>Ngân Hàng Chính Thức</span>
          <span className={`px-1.5 py-0.5 rounded-full text-[10px] ${activeTab === 'bank' ? 'bg-sky-700 text-white' : 'bg-slate-200 text-slate-700'}`}>
            {bankQuestions.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('documents')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'documents'
              ? 'bg-sky-600 text-white shadow-sm shadow-sky-600/20'
              : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
          }`}
        >
          <FileText className="w-4 h-4" />
          <span>Kho Giáo Trình RAG</span>
          <span className={`px-1.5 py-0.5 rounded-full text-[10px] ${activeTab === 'documents' ? 'bg-sky-700 text-white' : 'bg-slate-200 text-slate-700'}`}>
            {documents.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('review')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'review'
              ? 'bg-sky-600 text-white shadow-sm shadow-sky-600/20'
              : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
          }`}
        >
          <Sparkles className="w-4 h-4 text-amber-300" />
          <span>Hàng Đợi Thẩm Định &amp; AI</span>
          <span className={`px-1.5 py-0.5 rounded-full text-[10px] ${activeTab === 'review' ? 'bg-sky-700 text-white' : 'bg-amber-100 text-amber-800'}`}>
            {reviewVersions.length}
          </span>
        </button>
      </div>

      {/* ========================================================================= */}
      {/* TAB 1: KHO GIÁO TRÌNH RAG (DOCUMENTS)                                      */}
      {/* ========================================================================= */}
      {activeTab === 'documents' && (
        <div className="space-y-6">
          {/* UPLOAD DROPZONE */}
          <div className="bg-white p-6 rounded-2xl border-2 border-dashed border-sky-300/80 bg-sky-50/20 flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-2xl bg-sky-100 text-sky-600 flex items-center justify-center mb-3">
              <Upload className="w-6 h-6" />
            </div>
            <h3 className="font-heading font-extrabold text-slate-900 text-sm">
              Tải Lên Tài Liệu Giáo Trình &amp; Đề Cương (RAG Source)
            </h3>
            <p className="text-xs text-slate-500 max-w-md mt-1">
              Hệ thống tự động sử dụng Apache Tika bóc tách văn bản, cắt đoạn 800 tokens và vector hóa 1536 chiều với pgvector.
            </p>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Hỗ trợ: PDF, DOCX, PPTX, TXT (Tối đa 20MB)
            </p>

            <div className="mt-4 flex flex-col sm:flex-row items-center gap-3">
              <input
                type="file"
                id="doc-upload-input"
                className="hidden"
                accept=".pdf,.docx,.pptx,.txt"
                onChange={(e) => setDocFileToUpload(e.target.files[0] || null)}
              />
              <label
                htmlFor="doc-upload-input"
                className="px-4 py-2 rounded-xl bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold text-xs cursor-pointer shadow-xs transition active:scale-95"
              >
                {docFileToUpload ? docFileToUpload.name : 'Chọn Tệp Tin Từ Máy...'}
              </label>

              <button
                type="button"
                onClick={handleUploadDocument}
                disabled={!docFileToUpload || isUploadingDoc}
                className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-2"
              >
                {isUploadingDoc && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                <span>Bắt Đầu Tải Lên &amp; Lập Chỉ Mục</span>
              </button>
            </div>
          </div>

          {/* DOCUMENTS LIST */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="p-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <FileText className="w-4 h-4 text-slate-600" />
                <h3 className="font-heading font-extrabold text-xs text-slate-800 uppercase tracking-wider">
                  Danh Sách Giáo Trình Đã Tải Lên ({documents.length})
                </h3>
              </div>
              <button
                type="button"
                onClick={loadDocuments}
                className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg transition"
                title="Làm mới danh sách"
              >
                <RefreshCw className="w-4 h-4" />
              </button>
            </div>

            {isLoadingDocs ? (
              <div className="p-6">
                <SkeletonTable rows={3} columns={5} />
              </div>
            ) : documents.length === 0 ? (
              <div className="p-12 text-center text-slate-400 text-xs">
                Chưa có tài liệu giáo trình nào cho môn học này. Hãy tải lên tài liệu đầu tiên để phục vụ sinh câu hỏi AI.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-50/50 border-b border-slate-200/80 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                      <th className="py-3 px-4">Tên Tệp Tin</th>
                      <th className="py-3 px-4">Dung Lượng</th>
                      <th className="py-3 px-4">Trạng Thái RAG</th>
                      <th className="py-3 px-4">Số Đoạn (Chunks)</th>
                      <th className="py-3 px-4">Ghi Chú Lập Chỉ Mục</th>
                      <th className="py-3 px-4 text-right">Thao Tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {documents.map((doc) => (
                      <tr key={doc.documentId} className="hover:bg-slate-50/80 transition">
                        <td className="py-3.5 px-4 font-semibold text-slate-900">
                          <div className="flex items-center gap-2">
                            <FileText className="w-4 h-4 text-sky-600 shrink-0" />
                            <span className="truncate max-w-xs">{doc.fileName}</span>
                          </div>
                        </td>
                        <td className="py-3.5 px-4 text-slate-500 font-mono">
                          {(doc.fileSizeBytes / (1024 * 1024)).toFixed(2)} MB
                        </td>
                        <td className="py-3.5 px-4">
                          {doc.indexingStatus === 'READY' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-200">
                              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                              READY (Đã Lập Chỉ Mục)
                            </span>
                          )}
                          {doc.indexingStatus === 'INDEXING' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-sky-50 text-sky-700 border border-sky-200">
                              <span className="w-3 h-3 border-2 border-sky-600 border-t-transparent rounded-full animate-spin"></span>
                              INDEXING...
                            </span>
                          )}
                          {doc.indexingStatus === 'UPLOADED' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-slate-100 text-slate-700 border border-slate-300">
                              <Clock className="w-3.5 h-3.5" />
                              UPLOADED
                            </span>
                          )}
                          {doc.indexingStatus === 'FAILED' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-rose-50 text-rose-700 border border-rose-200">
                              <AlertCircle className="w-3.5 h-3.5 text-rose-500" />
                              FAILED
                            </span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 font-mono font-bold text-slate-700">
                          {doc.totalChunks || 0} chunks
                        </td>
                        <td className="py-3.5 px-4">
                          <p className="text-[11px] text-slate-500 line-clamp-2 max-w-sm">
                            {doc.errorMessage || `Số lần lập chỉ mục: ${doc.indexAttempts || 0}`}
                          </p>
                        </td>
                        <td className="py-3.5 px-4 text-right space-x-1">
                          {doc.indexingStatus === 'FAILED' && (
                            <button
                              type="button"
                              onClick={() => handleRetryIndexing(doc.documentId)}
                              className="p-1.5 rounded-lg text-amber-600 hover:bg-amber-50 transition"
                              title="Thử lập chỉ mục lại"
                            >
                              <RotateCcw className="w-4 h-4" />
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => setDeleteDocTarget(doc)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition"
                            title="Xóa tài liệu giáo trình"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: NGÂN HÀNG CÂU HỎI CHÍNH THỨC (OFFICIAL BANK)                      */}
      {/* ========================================================================= */}
      {activeTab === 'bank' && (
        <div className="space-y-6">
          {/* ACTION TOOLBAR & FILTERS */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
            <div className="flex flex-wrap items-center gap-2 flex-1">
              {/* Search */}
              <div className="relative min-w-[220px]">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Tìm câu hỏi, từ khóa..."
                  value={searchBankKeyword}
                  onChange={(e) => setSearchBankKeyword(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && loadBank()}
                  className="w-full pl-9 pr-4 py-2 rounded-xl text-xs bg-slate-50 border border-slate-200 focus:bg-white focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              {/* Topic Filter */}
              <select
                value={filterTopicId}
                onChange={(e) => setFilterTopicId(e.target.value)}
                className="py-2 px-3 rounded-xl text-xs bg-slate-50 border border-slate-200 text-slate-700 font-semibold focus:outline-none focus:ring-2 focus:ring-sky-500 cursor-pointer"
              >
                <option value="ALL">Tất cả chủ đề</option>
                {topics.map(t => (
                  <option key={t.topicId} value={t.topicId}>{t.name}</option>
                ))}
              </select>

              {/* Bloom Filter */}
              <select
                value={filterBloom}
                onChange={(e) => setFilterBloom(e.target.value)}
                className="py-2 px-3 rounded-xl text-xs bg-slate-50 border border-slate-200 text-slate-700 font-semibold focus:outline-none focus:ring-2 focus:ring-sky-500 cursor-pointer"
              >
                <option value="ALL">Tất cả cấp độ Bloom</option>
                {BLOOM_LEVELS.map(b => (
                  <option key={b.value} value={b.value}>{b.label}</option>
                ))}
              </select>

              <button
                type="button"
                onClick={loadBank}
                className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 transition"
                title="Lọc kết quả"
              >
                <Filter className="w-4 h-4" />
              </button>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setIsImportModalOpen(true)}
                className="px-3 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition flex items-center gap-1.5 active:scale-95 border border-slate-200"
              >
                <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
                <span>Nhập Excel/CSV</span>
              </button>

              <button
                type="button"
                onClick={() => setIsManualQuestionModalOpen(true)}
                className="px-4 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold shadow-sm transition flex items-center gap-1.5 active:scale-95"
              >
                <Plus className="w-4 h-4" />
                <span>Soạn Câu Hỏi Thủ Công</span>
              </button>
            </div>
          </div>

          {/* QUESTIONS LIST */}
          {isLoadingBank ? (
            <div className="space-y-4">
              <SkeletonCard />
              <SkeletonCard />
            </div>
          ) : bankQuestions.length === 0 ? (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center">
              <Database className="w-12 h-12 text-slate-300 mx-auto mb-3" />
              <h3 className="font-heading font-bold text-slate-800 text-sm">Chưa có câu hỏi chính thức</h3>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                Ngân hàng câu hỏi chính thức chỉ lưu trữ các câu hỏi có phiên bản đã được phê duyệt (APPROVED). Hãy soạn thảo hoặc duyệt các bản nháp từ Hàng đợi.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {bankQuestions.map((q) => {
                const cv = q.currentVersion || {};
                const bloomMeta = BLOOM_LEVELS.find(b => b.value === cv.bloomLevel) || BLOOM_LEVELS[0];
                const isExpanded = !!expandedExpectedAnswers[q.questionId];

                return (
                  <div key={q.questionId} className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs hover:border-slate-300 transition space-y-3">
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-extrabold text-sky-700 bg-sky-50 px-2 py-0.5 rounded-lg border border-sky-100">
                          {q.questionCode}
                        </span>
                        <span className="text-xs font-semibold text-slate-600">
                          {q.topicName || 'Chủ đề chung'}
                        </span>
                      </div>

                      <div className="flex items-center gap-2">
                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold border ${bloomMeta.color}`}>
                          {bloomMeta.label}
                        </span>

                        {q.status === 'ARCHIVED' ? (
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-slate-100 text-slate-600 border border-slate-300">
                            ARCHIVED
                          </span>
                        ) : (
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            APPROVED
                          </span>
                        )}
                        {q.hasPendingDraft && (
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-amber-50 text-amber-700 border border-amber-200">
                            CÓ BẢN NHÁP
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Question Content */}
                    <div className="text-xs text-slate-900 font-semibold leading-relaxed">
                      {cv.content}
                    </div>

                    {/* Collapsible Expected Answer */}
                    <div className="pt-1">
                      <button
                        type="button"
                        onClick={() => toggleExpectedAnswer(q.questionId)}
                        className="text-[11px] font-bold text-sky-600 hover:text-sky-700 inline-flex items-center gap-1 focus:outline-none"
                      >
                        {isExpanded ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                        <span>{isExpanded ? 'Ẩn đáp án mong đợi' : 'Xem đáp án mong đợi & thang điểm'}</span>
                      </button>

                      {isExpanded && (
                        <div className="mt-2.5 p-3.5 bg-slate-50 rounded-xl border border-slate-200/80 text-xs text-slate-700 space-y-2.5 animate-modal-entry">
                          <div>
                            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Đáp Án Mong Đợi:</p>
                            <p className="leading-relaxed font-medium">{cv.expectedAnswer}</p>
                          </div>

                          {cv.rubric && cv.rubric.criteria && (
                            <div className="pt-2 border-t border-slate-200">
                              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1.5 flex justify-between">
                                <span>Ma Trận Rubric Chấm Điểm ({cv.rubric.criteria.length} tiêu chí):</span>
                                <span className="text-sky-600 font-extrabold">Tổng điểm: {cv.rubric.totalScore || 10}đ</span>
                              </p>
                              <div className="space-y-1.5">
                                {cv.rubric.criteria.map((crit, idx) => (
                                  <div key={crit.criterionId || idx} className="flex items-center justify-between text-[11px] p-2 bg-white rounded-lg border border-slate-200">
                                    <div>
                                      <span className="font-bold text-slate-800">{crit.name}</span>
                                      {crit.description && <span className="text-slate-500 ml-1.5">- {crit.description}</span>}
                                    </div>
                                    <span className="font-mono font-bold text-indigo-600 shrink-0 ml-2">
                                      {crit.maxScore}đ
                                    </span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Bottom Toolbar */}
                    <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-[11px] text-slate-500">
                      <span>Cập nhật: {new Date(q.updatedAt || Date.now()).toLocaleDateString('vi-VN')}</span>
                      
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => handleOpenQuestionDetail(q.questionId)}
                          className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs transition active:scale-95 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500"
                        >
                          <Layers className="w-3.5 h-3.5 text-slate-500" />
                          <span>Chi Tiết &amp; Lịch Sử</span>
                        </button>
                        {q.status !== 'ARCHIVED' && (
                          <>
                            <button
                              type="button"
                              onClick={() => handleCreateDraftFromApproved(q.questionId)}
                              disabled={q.hasPendingDraft}
                              className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs transition active:scale-95 flex items-center gap-1.5 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-sky-500"
                            >
                              <Edit3 className="w-3.5 h-3.5 text-sky-600" />
                              <span>Tạo Bản Nháp Mới (Copy-on-Write)</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => setArchiveQuestionTarget(q)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
                              title="Lưu trữ câu hỏi (Archive)"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 3: HÀNG ĐỢI THẨM ĐỊNH & AI RAG (REVIEW QUEUE)                          */}
      {/* ========================================================================= */}
      {activeTab === 'review' && (
        <div className="space-y-6">
          {/* AI RAG GENERATION TRIGGER BANNER */}
          <div className="bg-gradient-to-r from-slate-900 to-indigo-950 text-white p-6 rounded-2xl border border-indigo-900/60 shadow-lg flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="space-y-1 text-center md:text-left">
              <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full bg-indigo-800/80 border border-indigo-700 text-indigo-200 text-[10px] font-bold uppercase tracking-wider">
                <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                <span>Trình Khởi Tạo Câu Hỏi Tự Động (AI RAG Engine)</span>
              </div>
              <h2 className="text-base font-heading font-extrabold tracking-tight">
                Sinh Câu Hỏi Tự Động Từ Giáo Trình Đã Vector Hóa
              </h2>
              <p className="text-xs text-slate-300 max-w-xl">
                AI Agent truy xuất các đoạn văn bản (chunks) từ giáo trình theo chuẩn Bloom 6 cấp độ và tạo ma trận Rubric đối sánh.
              </p>
            </div>

            <button
              type="button"
              onClick={() => setIsAiGenModalOpen(true)}
              className="px-5 py-2.5 rounded-xl bg-sky-500 hover:bg-sky-400 text-slate-950 font-heading font-extrabold text-xs shadow-md transition active:scale-95 flex items-center gap-2 shrink-0"
            >
              <Sparkles className="w-4 h-4 text-slate-950" />
              <span>Thiết Lập &amp; Kích Hoạt RAG</span>
            </button>
          </div>

          {/* STATUS FILTER FOR QUEUE */}
          <div className="flex items-center justify-between bg-white p-3.5 rounded-2xl border border-slate-200 shadow-xs">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-700">Lọc Theo Trạng Thái:</span>
              <div className="flex items-center gap-1.5">
                {['DRAFT', 'APPROVED', 'REJECTED', 'ALL'].map(st => (
                  <button
                    key={st}
                    type="button"
                    onClick={() => setReviewStatusFilter(st)}
                    className={`px-3 py-1 rounded-xl text-xs font-bold transition ${
                      reviewStatusFilter === st
                        ? 'bg-slate-900 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    {st}
                  </button>
                ))}
              </div>
            </div>

            <button
              type="button"
              onClick={loadReviewQueue}
              className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg transition"
              title="Làm mới hàng đợi"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>

          {/* REVIEW QUEUE LIST */}
          {isLoadingReview ? (
            <div className="space-y-4">
              <SkeletonCard />
              <SkeletonCard />
            </div>
          ) : reviewVersions.length === 0 ? (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center">
              <Sparkles className="w-12 h-12 text-slate-300 mx-auto mb-3" />
              <h3 className="font-heading font-bold text-slate-800 text-sm">Hàng đợi đang trống</h3>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                Không có bản nháp nào cần thẩm định. Bạn có thể kích hoạt RAG AI để tạo các câu hỏi mới.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {reviewVersions.map((v) => {
                const bloomMeta = BLOOM_LEVELS.find(b => b.value === v.bloomLevel) || BLOOM_LEVELS[0];

                return (
                  <div key={v.versionId} className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs space-y-4">
                    
                    {/* Header Bar */}
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-extrabold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-lg border border-indigo-100">
                          {v.questionCode || 'DRAFT-VER'}
                        </span>
                        <span className="text-xs font-semibold text-slate-700">
                          {v.topicName || 'Chủ đề học phần'}
                        </span>
                        <span className="text-[10px] px-2 py-0.5 rounded-md font-mono font-bold bg-slate-100 text-slate-600">
                          Origin: {v.origin || 'MANUAL'}
                        </span>
                      </div>

                      <div className="flex items-center gap-2">
                        {/* Bloom Confirmation Badge */}
                        {v.bloomConfirmed ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <Check className="w-3 h-3 text-emerald-500" />
                            Đã Xác Nhận Bloom
                          </span>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleConfirmBloom(v)}
                            className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-amber-50 text-amber-800 border border-amber-300 hover:bg-amber-100 transition active:scale-95"
                            title="Bấm để xác nhận cấp độ nhận thức Bloom này hợp lệ"
                          >
                            <AlertCircle className="w-3 h-3 text-amber-500" />
                            Xác Nhận Bloom ({v.bloomLevel})
                          </button>
                        )}

                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold border ${bloomMeta.color}`}>
                          {bloomMeta.label}
                        </span>

                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold ${
                          v.status === 'APPROVED'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : v.status === 'REJECTED'
                            ? 'bg-rose-50 text-rose-700 border border-rose-200'
                            : 'bg-amber-50 text-amber-700 border border-amber-200'
                        }`}>
                          {v.status}
                        </span>
                      </div>
                    </div>

                    {/* Question Content */}
                    <div>
                      <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Nội Dung Câu Hỏi:</p>
                      <p className="text-xs text-slate-900 font-semibold leading-relaxed">{v.content}</p>
                    </div>

                    {/* Expected Answer */}
                    <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
                      <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Đáp Án Tiêu Chuẩn:</p>
                      <p className="text-xs text-slate-700 leading-relaxed font-medium">{v.expectedAnswer}</p>
                    </div>

                    {/* Source Citations (Grounding Evidence) */}
                    {v.sources && v.sources.length > 0 && (
                      <div className="p-3 bg-indigo-50/40 rounded-xl border border-indigo-100 space-y-1.5">
                        <div className="flex items-center gap-1.5 text-indigo-700 font-bold text-[11px]">
                          <BookOpen className="w-3.5 h-3.5" />
                          <span>Cơ Sở Đối Chiếu Giáo Trình (Grounding Evidence Chunk #{v.sources[0].order}):</span>
                        </div>
                        <p className="text-[11px] text-slate-600 italic bg-white p-2.5 rounded-lg border border-indigo-100/60 leading-relaxed">
                          "{v.sources[0].citationQuote}"
                        </p>
                      </div>
                    )}

                    {/* Rubric Breakdown */}
                    {v.rubric && v.rubric.criteria && (
                      <div className="pt-2 border-t border-slate-100">
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1.5 flex justify-between">
                          <span>Ma Trận Tiêu Chí Rubric ({v.rubric.criteria.length} tiêu chí):</span>
                          <span className="text-indigo-600 font-extrabold">Tổng điểm: {v.rubric.totalScore || 10}đ</span>
                        </p>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                          {v.rubric.criteria.map((c, idx) => (
                            <div key={idx} className="p-2 bg-slate-50 rounded-lg border border-slate-200 flex items-center justify-between text-[11px]">
                              <div>
                                <span className="font-bold text-slate-800">{c.name}</span>
                                {c.description && <p className="text-[10px] text-slate-500 line-clamp-1">{c.description}</p>}
                              </div>
                              <span className="font-mono font-bold text-indigo-700 shrink-0 ml-2">{c.maxScore}đ</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Action Bar */}
                    <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-100">
                      <div className="flex items-center gap-2">
                        {v.origin === 'AI_RAG' && v.status === 'DRAFT' && (
                          <button
                            type="button"
                            onClick={() => { setRegenVersionTarget(v); setRegenFeedback(''); }}
                            className="px-3 py-1.5 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs transition active:scale-95 flex items-center gap-1.5"
                          >
                            <Sparkles className="w-3.5 h-3.5" />
                            <span>Tạo Lại Với AI (Regenerate)</span>
                          </button>
                        )}

                        {v.status === 'DRAFT' && (
                          <button
                            type="button"
                            onClick={() => handleOpenEditDraft(v)}
                            className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs transition active:scale-95 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500"
                          >
                            <Edit3 className="w-3.5 h-3.5 text-sky-600" />
                            <span>Chỉnh Sửa</span>
                          </button>
                        )}

                        {v.status === 'DRAFT' && (
                          <button
                            type="button"
                            onClick={() => setDeleteDraftTarget(v)}
                            className="p-1.5 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition"
                            title="Xóa vĩnh viễn bản nháp"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>

                      {v.status === 'DRAFT' && (
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => { setRejectVersionTarget(v); setRejectReason(''); }}
                            className="px-3.5 py-1.5 rounded-xl bg-slate-100 hover:bg-rose-50 hover:text-rose-600 text-slate-700 font-bold text-xs transition active:scale-95 border border-slate-200"
                          >
                            Từ Chối
                          </button>

                          <button
                            type="button"
                            onClick={() => handleApproveVersion(v)}
                            className="px-4 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-sm transition active:scale-95 flex items-center gap-1.5"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            <span>Phê Duyệt (Approve)</span>
                          </button>
                        </div>
                      )}
                    </div>

                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: TẠO CHỦ ĐỀ MỚI                                                      */}
      {/* ========================================================================= */}
      {isCreateTopicModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-white rounded-2xl max-w-md w-full border border-slate-200 shadow-2xl p-5 space-y-4 animate-modal-entry">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="font-heading font-extrabold text-sm text-slate-900">Thêm Chủ Đề Đề Cương</h3>
              <button onClick={() => setIsCreateTopicModalOpen(false)} className="text-slate-400 hover:text-slate-700 p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateTopic} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Tên Chủ Đề / Chương <span className="text-rose-500">*</span></label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Chương 4: Clean Architecture & SOLID"
                  value={newTopicName}
                  onChange={(e) => setNewTopicName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Mô Tả Nội Dung Chủ Đề</label>
                <textarea
                  rows="2"
                  placeholder="Mô tả chuẩn đầu ra học phần cho chủ đề này..."
                  value={newTopicDesc}
                  onChange={(e) => setNewTopicDesc(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsCreateTopicModalOpen(false)}
                  className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm transition active:scale-95"
                >
                  Lưu Chủ Đề
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: THIẾT LẬP RAG AI SINH CÂU HỎI                                      */}
      {/* ========================================================================= */}
      {isAiGenModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl p-6 space-y-4 animate-modal-entry">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2 text-indigo-600 font-bold text-xs">
                <Sparkles className="w-4 h-4 text-amber-500" />
                <span className="font-heading uppercase tracking-wider">Cấu Hình RAG Prompting Engine</span>
              </div>
              <button onClick={() => setIsAiGenModalOpen(false)} className="text-slate-400 hover:text-slate-700 p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Chủ Đề Áp Dụng (Topic)</label>
                <select
                  value={genTopicId}
                  onChange={(e) => setGenTopicId(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium cursor-pointer"
                >
                  <option value="">-- Mặc định (Toàn bộ học phần) --</option>
                  {topics.map(t => (
                    <option key={t.topicId} value={t.topicId}>{t.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Chọn Giáo Trình Làm Nguồn Ngữ Cảnh (RAG Context) <span className="text-rose-500">*</span>
                </label>
                <div className="space-y-1.5 max-h-36 overflow-y-auto border border-slate-200 p-2.5 rounded-xl bg-slate-50">
                  {documents.filter(d => d.indexingStatus === 'READY').length === 0 ? (
                    <p className="text-slate-400 text-xs italic">Chưa có tài liệu nào ở trạng thái READY.</p>
                  ) : (
                    documents.filter(d => d.indexingStatus === 'READY').map(d => (
                      <label key={d.documentId} className="flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={genDocumentIds.includes(d.documentId)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setGenDocumentIds(prev => [...prev, d.documentId]);
                            } else {
                              setGenDocumentIds(prev => prev.filter(id => id !== d.documentId));
                            }
                          }}
                          className="rounded text-sky-600 focus:ring-sky-500"
                        />
                        <span className="truncate">{d.fileName} ({d.totalChunks} chunks)</span>
                      </label>
                    ))
                  )}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Số Lượng Câu Hỏi (1-20)</label>
                  <input
                    type="number"
                    min="1"
                    max="20"
                    value={genTotalQuestions}
                    onChange={(e) => setGenTotalQuestions(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Phân Bố Cấp Độ Bloom</label>
                  <div className="px-3 py-2 rounded-xl text-xs bg-slate-100 text-slate-600 font-mono font-medium border border-slate-200">
                    30% Nhớ | 40% Hiểu | 30% Dụng
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Ghi Chú Đạo Diễn Cho AI (Lecturer Instruction)</label>
                <textarea
                  rows="2"
                  placeholder="Ví dụ: Đặt câu hỏi xoáy sâu vào các bẫy thường gặp trong xử lý đa luồng..."
                  value={genLecturerNote}
                  onChange={(e) => setGenLecturerNote(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              {failedGenRequest && (
                <div className="p-3 rounded-xl border border-amber-200 bg-amber-50 text-xs text-amber-800 space-y-1">
                  <p className="font-bold">
                    Lần sinh gần nhất: {failedGenRequest.status} ({failedGenRequest.generatedCount}/{failedGenRequest.totalQuestions} câu)
                  </p>
                  {failedGenRequest.errorMessage && <p>{failedGenRequest.errorMessage}</p>}
                  {failedGenRequest.issues?.map((issue, idx) => <p key={idx}>• {issue}</p>)}
                </div>
              )}

              <div className="pt-2 flex justify-end gap-2">
                {failedGenRequest && (
                  <button
                    type="button"
                    onClick={handleRetryGeneration}
                    disabled={isGeneratingAi}
                    className="px-4 py-2 rounded-xl border border-amber-300 text-amber-800 bg-amber-50 text-xs font-bold hover:bg-amber-100 transition active:scale-95 disabled:opacity-50 flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Thử Lại Phần Thiếu</span>
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => { setIsAiGenModalOpen(false); setFailedGenRequest(null); }}
                  disabled={isGeneratingAi}
                  className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="button"
                  onClick={handleTriggerAiGeneration}
                  disabled={isGeneratingAi || genDocumentIds.length === 0}
                  className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-2"
                >
                  {isGeneratingAi && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                  <span>Bắt Đầu Sinh Câu Hỏi</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: SOẠN CÂU HỎI THỦ CÔNG KÈM RUBRIC BUILDER                          */}
      {/* ========================================================================= */}
      {isManualQuestionModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-white rounded-2xl max-w-xl w-full border border-slate-200 shadow-2xl p-6 space-y-4 animate-modal-entry max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="font-heading font-extrabold text-sm text-slate-900">Soạn Câu Hỏi Mới Kèm Ma Trận Rubric</h3>
              <button onClick={() => setIsManualQuestionModalOpen(false)} className="text-slate-400 hover:text-slate-700 p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateManualQuestion} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Chủ Đề (Topic)</label>
                  <select
                    value={manualTopicId}
                    onChange={(e) => setManualTopicId(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium cursor-pointer"
                  >
                    <option value="">-- Chủ đề chung --</option>
                    {topics.map(t => (
                      <option key={t.topicId} value={t.topicId}>{t.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Cấp Độ Nhận Thức (Bloom)</label>
                  <select
                    value={manualBloom}
                    onChange={(e) => setManualBloom(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium cursor-pointer"
                  >
                    {BLOOM_LEVELS.map(b => (
                      <option key={b.value} value={b.value}>{b.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Nội Dung Câu Hỏi <span className="text-rose-500">*</span></label>
                <textarea
                  rows="3"
                  required
                  placeholder="Nhập nội dung câu hỏi vấn đáp chi tiết..."
                  value={manualContent}
                  onChange={(e) => setManualContent(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Đáp Án Tiêu Chuẩn Mong Đợi <span className="text-rose-500">*</span></label>
                <textarea
                  rows="3"
                  required
                  placeholder="Đáp án hoặc các ý chính thí sinh cần nêu bật được..."
                  value={manualExpectedAnswer}
                  onChange={(e) => setManualExpectedAnswer(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              {/* DYNAMIC RUBRIC BUILDER */}
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="text-xs font-bold text-slate-800">Tiêu Chí Chấm Điểm Rubric</h4>
                    <p className="text-[10px] text-slate-400">Thiết lập các tiêu chí con chấm điểm tự động</p>
                  </div>
                  <span className="font-mono text-xs font-extrabold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-lg border border-indigo-200">
                    Tổng: {manualCriteria.reduce((sum, c) => sum + Number(c.maxScore || 0), 0)}đ
                  </span>
                </div>

                <RubricCriteriaEditor criteria={manualCriteria} onChange={setManualCriteria} />
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsManualQuestionModalOpen(false)}
                  disabled={isSubmittingQuestion}
                  className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingQuestion}
                  className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-2"
                >
                  {isSubmittingQuestion && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                  <span>Lưu &amp; Gửi Hàng Đợi Duyệt</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: NHẬP CÂU HỎI TỪ EXCEL / CSV (UC1.6)                                */}
      {/* ========================================================================= */}
      {isImportModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl p-6 space-y-4 animate-modal-entry">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2 text-emerald-600 font-bold text-xs">
                <FileSpreadsheet className="w-4 h-4" />
                <span className="font-heading uppercase tracking-wider">Nhập Câu Hỏi Hàng Loạt (UC1.6)</span>
              </div>
              <button onClick={() => setIsImportModalOpen(false)} className="text-slate-400 hover:text-slate-700 p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleImportQuestions} className="space-y-4">
              <div className="flex items-center justify-between bg-slate-50 p-3 rounded-xl border border-slate-200">
                <span className="text-xs font-semibold text-slate-700">Tải tệp mẫu quy chuẩn:</span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => handleDownloadTemplate('xlsx')}
                    className="px-2.5 py-1 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg text-xs font-bold text-slate-700 inline-flex items-center gap-1 shadow-xs"
                  >
                    <Download className="w-3 h-3 text-emerald-600" />
                    <span>Excel (.xlsx)</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDownloadTemplate('csv')}
                    className="px-2.5 py-1 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg text-xs font-bold text-slate-700 inline-flex items-center gap-1 shadow-xs"
                  >
                    <Download className="w-3 h-3 text-sky-600" />
                    <span>CSV</span>
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Chọn Tệp Tin Cần Nhập (.xlsx, .csv)</label>
                <input
                  type="file"
                  required
                  accept=".xlsx,.csv"
                  onChange={(e) => setImportFile(e.target.files[0] || null)}
                  className="w-full text-xs file:mr-3 file:py-2 file:px-3 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-slate-100 file:text-slate-700 hover:file:bg-slate-200 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl border border-slate-200">
                <div>
                  <p className="text-xs font-bold text-slate-800">Chế Độ Thẩm Định Thử Nghiệm (Dry Run)</p>
                  <p className="text-[10px] text-slate-500">Chỉ kiểm tra tính hợp lệ dữ liệu, chưa ghi vào cơ sở dữ liệu</p>
                </div>
                <input
                  type="checkbox"
                  checked={isDryRun}
                  onChange={(e) => setIsDryRun(e.target.checked)}
                  className="w-4 h-4 text-sky-600 rounded focus:ring-sky-500 cursor-pointer"
                />
              </div>

              {/* Import Report Result Box */}
              {importReport && (
                <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-200 text-xs space-y-1 animate-modal-entry">
                  <p className="font-bold text-emerald-800">Báo Cáo Kết Quả Nhập:</p>
                  <p className="text-emerald-700">Tổng dòng đọc được: {importReport.totalRows || 0} ({importReport.totalQuestions || 0} câu hỏi)</p>
                  <p className="text-emerald-700">Câu hỏi hợp lệ: {importReport.validQuestions || 0}</p>
                  {!importReport.dryRun && (
                    <p className="text-emerald-700">Đã tạo: {importReport.createdQuestions || 0} câu hỏi</p>
                  )}
                  {importReport.invalidQuestions > 0 && (
                    <p className="text-rose-600 font-semibold">Câu hỏi lỗi: {importReport.invalidQuestions}</p>
                  )}
                  {importReport.errors?.length > 0 && (
                    <ul className="mt-1 max-h-32 overflow-y-auto space-y-0.5 text-[11px] text-rose-700">
                      {importReport.errors.map((er, idx) => (
                        <li key={idx}>
                          Dòng {er.row}{er.column ? ` • ${er.column}` : ''}{er.questionRef ? ` • ${er.questionRef}` : ''}: {er.message}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsImportModalOpen(false)}
                  disabled={isImporting}
                  className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition"
                >
                  Đóng
                </button>
                <button
                  type="submit"
                  disabled={isImporting || !importFile}
                  className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-2"
                >
                  {isImporting && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                  <span>{isDryRun ? 'Thẩm Định File (Dry Run)' : 'Nhập Dữ Liệu Ngay'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: TỪ CHỐI BẢN NHÁP (REJECT MODAL)                                    */}
      {/* ========================================================================= */}
      {rejectVersionTarget && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-white rounded-2xl max-w-md w-full border border-slate-200 shadow-2xl p-5 space-y-4 animate-modal-entry">
            <h3 className="font-heading font-extrabold text-sm text-slate-900">Từ Chối Phê Duyệt Câu Hỏi</h3>
            <p className="text-xs text-slate-500">
              Vui lòng nêu rõ lý do từ chối để hỗ trợ giảng viên soạn thảo hoặc định hướng lại prompt cho AI.
            </p>

            <textarea
              rows="3"
              required
              placeholder="Ví dụ: Câu hỏi chưa gắn đúng trích dẫn giáo trình hoặc đáp án chưa đủ độ sâu..."
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-rose-500 font-medium"
            />

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setRejectVersionTarget(null)}
                className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold"
              >
                Hủy Bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmReject}
                disabled={!rejectReason.trim()}
                className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs shadow-sm disabled:opacity-50"
              >
                Xác Nhận Từ Chối
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: TẠO LẠI CÂU HỎI AI (REGENERATE WITH FEEDBACK)                       */}
      {/* ========================================================================= */}
      {regenVersionTarget && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-white rounded-2xl max-w-md w-full border border-slate-200 shadow-2xl p-5 space-y-4 animate-modal-entry">
            <div className="flex items-center gap-2 text-indigo-600 font-bold text-xs">
              <Sparkles className="w-4 h-4" />
              <span>Yêu Cầu AI Tạo Lại Câu Hỏi Này</span>
            </div>
            <p className="text-xs text-slate-500">
              Cung cấp thêm chỉ dẫn điều chỉnh (Feedback). AI sẽ giữ nguyên ngữ cảnh trích dẫn ban đầu và viết lại câu hỏi phù hợp hơn.
            </p>

            <textarea
              rows="3"
              placeholder="Ví dụ: Hãy chuyển câu hỏi thành dạng bài toán tình huống thực tế..."
              value={regenFeedback}
              onChange={(e) => setRegenFeedback(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
            />

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setRegenVersionTarget(null)}
                className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold"
              >
                Hủy Bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmRegenerate}
                className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs shadow-sm flex items-center gap-1.5"
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>Sinh Lại Câu Hỏi</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CONFIRM DELETE DOCUMENT */}
      <ConfirmModal
        isOpen={!!deleteDocTarget}
        title="Xóa Giáo Trình Này?"
        message={`Bạn có chắc muốn xóa tệp "${deleteDocTarget?.fileName}"? Lưu ý: Nếu giáo trình này đang có câu hỏi trích dẫn nguồn (source chunk), hệ thống sẽ từ chối xóa để đảm bảo toàn vẹn dữ liệu.`}
        confirmText="Xác Nhận Xóa"
        cancelText="Giữ Lại"
        isDanger={true}
        onConfirm={handleConfirmDeleteDoc}
        onCancel={() => setDeleteDocTarget(null)}
      />

      {/* CONFIRM DELETE DRAFT */}
      <ConfirmModal
        isOpen={!!deleteDraftTarget}
        title="Xóa Bản Nháp Câu Hỏi?"
        message="Bản nháp này chưa được phê duyệt và sẽ bị xóa hoàn toàn khỏi cơ sở dữ liệu. Bạn có chắc chắn muốn xóa?"
        confirmText="Xóa Bản Nháp"
        cancelText="Hủy Bỏ"
        isDanger={true}
        onConfirm={handleConfirmDeleteDraft}
        onCancel={() => setDeleteDraftTarget(null)}
      />

      {/* CONFIRM ARCHIVE QUESTION */}
      <ConfirmModal
        isOpen={!!archiveQuestionTarget}
        title="Lưu Trữ Câu Hỏi?"
        message={`Câu hỏi "${archiveQuestionTarget?.questionCode}" sẽ chuyển sang ARCHIVED và không còn được dùng cho ca thi mới.`}
        confirmText="Lưu Trữ"
        cancelText="Hủy Bỏ"
        isDanger={true}
        onConfirm={handleConfirmArchive}
        onCancel={() => setArchiveQuestionTarget(null)}
      />

      {/* ========================================================================= */}
      {/* MODAL: CHỈNH SỬA BẢN NHÁP (CONTENT + RUBRIC)                               */}
      {/* ========================================================================= */}
      {editDraft && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog" aria-modal="true">
          <div className="bg-white rounded-2xl max-w-xl w-full border border-slate-200 shadow-2xl p-6 space-y-4 animate-modal-entry max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="font-heading font-extrabold text-sm text-slate-900">
                Chỉnh Sửa Bản Nháp {editDraft.version.questionCode}
              </h3>
              <button onClick={() => setEditDraft(null)} className="text-slate-400 hover:text-slate-700 p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveDraft} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Cấp Độ Nhận Thức (Bloom)</label>
                <select
                  value={editDraft.bloomLevel}
                  onChange={(e) => setEditDraft(d => ({ ...d, bloomLevel: e.target.value }))}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium cursor-pointer"
                >
                  {BLOOM_LEVELS.map(b => (
                    <option key={b.value} value={b.value}>{b.label}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Nội Dung Câu Hỏi <span className="text-rose-500">*</span></label>
                <textarea
                  rows="3"
                  required
                  maxLength={4000}
                  value={editDraft.content}
                  onChange={(e) => setEditDraft(d => ({ ...d, content: e.target.value }))}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Đáp Án Tiêu Chuẩn Mong Đợi <span className="text-rose-500">*</span></label>
                <textarea
                  rows="3"
                  required
                  maxLength={8000}
                  value={editDraft.expectedAnswer}
                  onChange={(e) => setEditDraft(d => ({ ...d, expectedAnswer: e.target.value }))}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="text-xs font-bold text-slate-800">Tiêu Chí Chấm Điểm Rubric</h4>
                  <span className="font-mono text-xs font-extrabold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-lg border border-indigo-200">
                    Tổng: {editDraft.criteria.reduce((sum, c) => sum + Number(c.maxScore || 0), 0)}đ
                  </span>
                </div>
                <RubricCriteriaEditor
                  criteria={editDraft.criteria}
                  onChange={(criteria) => setEditDraft(d => ({ ...d, criteria }))}
                />
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setEditDraft(null)}
                  disabled={isSavingDraft}
                  className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSavingDraft}
                  className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-2"
                >
                  {isSavingDraft && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                  <span>Lưu Bản Nháp</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: CHI TIẾT CÂU HỎI & LỊCH SỬ PHIÊN BẢN                                */}
      {/* ========================================================================= */}
      {questionDetail && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog" aria-modal="true">
          <div className="bg-white rounded-2xl max-w-2xl w-full border border-slate-200 shadow-2xl p-6 space-y-4 animate-modal-entry max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="font-heading font-extrabold text-sm text-slate-900">
                Chi Tiết Câu Hỏi {questionDetail.detail?.questionCode || ''}
              </h3>
              <button onClick={() => setQuestionDetail(null)} className="text-slate-400 hover:text-slate-700 p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            {questionDetail.isLoading ? (
              <SkeletonCard />
            ) : (
              <div className="space-y-4 text-xs">
                <div className="flex flex-wrap gap-2">
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-slate-100 text-slate-700 border border-slate-200">
                    {questionDetail.detail.status}
                  </span>
                  {questionDetail.detail.pendingDraft && (
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-amber-50 text-amber-700 border border-amber-200">
                      Bản nháp đang chờ: v{questionDetail.detail.pendingDraft.versionNumber}
                    </span>
                  )}
                </div>

                {questionDetail.detail.currentVersion && (
                  <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      Phiên bản hiện hành v{questionDetail.detail.currentVersion.versionNumber}
                    </p>
                    <p className="font-semibold text-slate-900 leading-relaxed">{questionDetail.detail.currentVersion.content}</p>
                    <p className="text-slate-700 leading-relaxed">{questionDetail.detail.currentVersion.expectedAnswer}</p>
                  </div>
                )}

                <div>
                  <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">
                    Lịch Sử Phiên Bản ({questionDetail.history.length})
                  </p>
                  <div className="divide-y divide-slate-100 border border-slate-200 rounded-xl">
                    {questionDetail.history.map(h => (
                      <div key={h.versionId} className="p-3 flex items-start justify-between gap-3">
                        <div className="space-y-0.5 min-w-0">
                          <p className="font-bold text-slate-800">
                            v{h.versionNumber} • {h.origin} • {h.bloomLevel}
                          </p>
                          <p className="text-slate-600 line-clamp-2">{h.content}</p>
                          {h.rejectReason && <p className="text-rose-600">Lý do từ chối: {h.rejectReason}</p>}
                        </div>
                        <div className="text-right shrink-0">
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-slate-100 text-slate-700 border border-slate-200">
                            {h.status}
                          </span>
                          <p className="text-[10px] text-slate-400 mt-1 font-mono">
                            {h.createdAt ? new Date(h.createdAt).toLocaleString('vi-VN') : ''}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

    </div>
  );
}
