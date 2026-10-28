import React, { useState, useEffect, useMemo } from 'react';
import {
  Database,
  FileText,
  Sparkles,
  Upload,
  Download,
  Plus,
  Minus,
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
  ChevronsUpDown,
  Lightbulb,
  FileCheck,
  FileWarning,
  Cpu,
  ListOrdered,
  Sliders,
  FolderPlus
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
import { Pagination } from '../components/common/Pagination';

import {
  BLOOM_LEVELS,
  BLOOM_PRESETS,
  calculateBloomDistribution,
  validateCriteria,
  toRubricPayload,
  RubricCriteriaEditor,
  CreateTopicModal,
  AiQuestionGeneratorModal,
  ManualQuestionModal,
  TextImportModal,
  ImportQuestionsModal,
  RejectVersionModal,
  RegenerateQuestionModal,
  EditDraftModal,
  QuestionDetailModal
} from '../components/question-bank';

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
  const [bankPage, setBankPage] = useState(0);
  const [bankPageSize, setBankPageSize] = useState(10);
  const [bankTotalPages, setBankTotalPages] = useState(1);
  const [bankTotalElements, setBankTotalElements] = useState(0);

  // Tab 3: Review Queue State
  const [reviewVersions, setReviewVersions] = useState([]);
  const [isLoadingReview, setIsLoadingReview] = useState(false);
  const [reviewStatusFilter, setReviewStatusFilter] = useState('DRAFT');
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewPageSize, setReviewPageSize] = useState(10);
  const [reviewTotalPages, setReviewTotalPages] = useState(1);
  const [reviewTotalElements, setReviewTotalElements] = useState(0);
  const [selectedVersionIds, setSelectedVersionIds] = useState([]);
  const [isBatchApproving, setIsBatchApproving] = useState(false);

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
  const [selectedBloomPreset, setSelectedBloomPreset] = useState('BALANCED');
  const [bloomCounts, setBloomCounts] = useState(() => calculateBloomDistribution(3, BLOOM_PRESETS[0].weights));
  const [genLecturerNote, setGenLecturerNote] = useState('');
  const [isGeneratingAi, setIsGeneratingAi] = useState(false);
  const [failedGenRequest, setFailedGenRequest] = useState(null);

  const totalAllocatedBloom = useMemo(() => {
    return Object.values(bloomCounts).reduce((sum, c) => sum + (Number(c) || 0), 0);
  }, [bloomCounts]);

  const isBloomBalanced = useMemo(() => {
    return totalAllocatedBloom === Number(genTotalQuestions);
  }, [totalAllocatedBloom, genTotalQuestions]);

  const handleTotalQuestionsChange = (newVal) => {
    const val = Number(newVal);
    setGenTotalQuestions(newVal);
    if (val >= 1 && val <= 20) {
      const preset = BLOOM_PRESETS.find(p => p.id === selectedBloomPreset);
      if (preset && preset.weights) {
        setBloomCounts(calculateBloomDistribution(val, preset.weights));
      }
    }
  };

  const handleSelectBloomPreset = (presetId) => {
    setSelectedBloomPreset(presetId);
    const preset = BLOOM_PRESETS.find(p => p.id === presetId);
    if (preset && preset.weights) {
      setBloomCounts(calculateBloomDistribution(Number(genTotalQuestions), preset.weights));
    }
  };

  const handleBloomCountChange = (level, deltaOrValue, isAbsolute = false) => {
    setSelectedBloomPreset('CUSTOM');
    setBloomCounts(prev => {
      const current = prev[level] || 0;
      const nextVal = isAbsolute ? Math.max(0, Number(deltaOrValue) || 0) : Math.max(0, current + deltaOrValue);
      return { ...prev, [level]: nextVal };
    });
  };

  const handleAutoBalanceBloom = () => {
    const targetTotal = Math.max(1, Math.min(20, Number(genTotalQuestions) || 3));
    const preset = BLOOM_PRESETS.find(p => p.id === selectedBloomPreset && p.weights) || BLOOM_PRESETS[0];
    setBloomCounts(calculateBloomDistribution(targetTotal, preset.weights));
    setSelectedBloomPreset(preset.id);
  };

  // Import Modal State
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [isDryRun, setIsDryRun] = useState(true);
  const [isImporting, setIsImporting] = useState(false);
  const [importReport, setImportReport] = useState(null);

  // Text import (one question per line)
  const [textImport, setTextImport] = useState(null); // { text, topicId, bloomLevel, rubricTemplateId }
  const [rubricTemplates, setRubricTemplates] = useState([]);
  const [isImportingText, setIsImportingText] = useState(false);

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
        const list = Array.isArray(data) ? data : data?.content || [];
        setSubjects(list);
        if (list.length > 0) {
          if (!selectedSubjectId || !list.some(s => s.subjectId === selectedSubjectId)) {
            setSelectedSubjectId(list[0].subjectId);
          }
        } else {
          setSelectedSubjectId('');
        }
      } catch (err) {
        setSubjects([]);
        setSelectedSubjectId('');
      } finally {
        setIsLoadingCatalog(false);
      }
    }

    loadCatalog();
  }, []);

  // 2. Load Topics whenever selectedSubjectId changes
  useEffect(() => {
    if (!selectedSubjectId) {
      setTopics([]);
      return;
    }

    async function loadTopics() {
      try {
        const list = await lecturerCatalogApi.getTopics(selectedSubjectId);
        setTopics(Array.isArray(list) ? list : []);
      } catch (err) {
        setTopics([]);
        showToast({ type: 'error', message: getErrorMessage(err) });
      }
    }

    loadTopics();
  }, [selectedSubjectId]);

  const loadDocuments = async () => {
    if (!selectedSubjectId) {
      setDocuments([]);
      return;
    }
    setIsLoadingDocs(true);
    try {
      const data = await courseDocumentApi.getDocuments(selectedSubjectId);
      const list = Array.isArray(data) ? data : data?.content || [];
      setDocuments(list);
    } catch (err) {
      setDocuments([]);
    } finally {
      setIsLoadingDocs(false);
    }
  };

  // 3. Load tab specific data & load documents whenever subject changes
  useEffect(() => {
    if (!selectedSubjectId) {
      setDocuments([]);
      setBankQuestions([]);
      setReviewVersions([]);
      return;
    }

    loadDocuments();

    if (activeTab === 'bank') {
      setBankPage(0);
      loadBank(0);
    } else if (activeTab === 'review') {
      setReviewPage(0);
      loadReviewQueue(0);
    }
  }, [selectedSubjectId, activeTab]);

  // Reload documents when AI Generation modal opens
  useEffect(() => {
    if (isAiGenModalOpen && selectedSubjectId) {
      loadDocuments();
    }
  }, [isAiGenModalOpen, selectedSubjectId]);

  // Auto-polling when documents are in UPLOADED or INDEXING state
  useEffect(() => {
    if (!selectedSubjectId) return;

    const hasPendingDocs = documents.some((doc) => {
      const status = doc.indexingStatus || doc.status;
      return status === 'INDEXING' || status === 'UPLOADED';
    });

    if (!hasPendingDocs) return;

    const intervalId = setInterval(async () => {
      try {
        const refreshed = await courseDocumentApi.getDocuments(selectedSubjectId);
        const list = Array.isArray(refreshed) ? refreshed : refreshed?.content || [];
        if (list.length > 0) {
          setDocuments(list);
        }
      } catch (err) {
        // Silently continue
      }
    }, 3000);

    return () => clearInterval(intervalId);
  }, [documents, selectedSubjectId]);

  const loadBank = async (pageOverride, sizeOverride, topicOverride, bloomOverride, keywordOverride) => {
    if (!selectedSubjectId) {
      setBankQuestions([]);
      setBankTotalPages(1);
      setBankTotalElements(0);
      return;
    }
    setIsLoadingBank(true);
    const targetPage = pageOverride !== undefined ? pageOverride : bankPage;
    const targetSize = sizeOverride !== undefined ? sizeOverride : bankPageSize;
    const targetTopic = topicOverride !== undefined ? topicOverride : filterTopicId;
    const targetBloom = bloomOverride !== undefined ? bloomOverride : filterBloom;
    const targetKeyword = keywordOverride !== undefined ? keywordOverride : searchBankKeyword;

    try {
      const params = { page: targetPage, size: targetSize };
      if (targetTopic !== 'ALL') params.topicId = targetTopic;
      if (targetBloom !== 'ALL') params.bloomLevel = targetBloom;
      if (targetKeyword?.trim()) params.keyword = targetKeyword.trim();

      const data = await questionBankApi.searchBank(selectedSubjectId, params);
      const list = data?.content || (Array.isArray(data) ? data : []);
      setBankQuestions(list);
      setBankPage(data?.page ?? targetPage);
      setBankTotalPages(data?.totalPages || 1);
      setBankTotalElements(data?.totalElements ?? list.length);
    } catch (err) {
      setBankQuestions([]);
      setBankTotalPages(1);
      setBankTotalElements(0);
    } finally {
      setIsLoadingBank(false);
    }
  };

  const handleBankPageChange = (newPage) => {
    setBankPage(newPage);
    loadBank(newPage);
  };

  const handleBankPageSizeChange = (newSize) => {
    setBankPageSize(newSize);
    setBankPage(0);
    loadBank(0, newSize);
  };

  const loadReviewQueue = async (pageOverride, sizeOverride, statusOverride) => {
    setSelectedVersionIds([]);
    if (!selectedSubjectId) {
      setReviewVersions([]);
      setReviewTotalPages(1);
      setReviewTotalElements(0);
      return;
    }
    setIsLoadingReview(true);
    const targetPage = pageOverride !== undefined ? pageOverride : reviewPage;
    const targetSize = sizeOverride !== undefined ? sizeOverride : reviewPageSize;
    const targetStatus = statusOverride !== undefined ? statusOverride : reviewStatusFilter;

    try {
      const data = await questionBankApi.searchVersions(selectedSubjectId, {
        status: targetStatus === 'ALL' ? undefined : targetStatus,
        page: targetPage,
        size: targetSize
      });
      const list = data?.content || (Array.isArray(data) ? data : []);
      setReviewVersions(list);
      setReviewPage(data?.page ?? targetPage);
      setReviewTotalPages(data?.totalPages || 1);
      setReviewTotalElements(data?.totalElements ?? list.length);
    } catch (err) {
      setReviewVersions([]);
      setReviewTotalPages(1);
      setReviewTotalElements(0);
    } finally {
      setIsLoadingReview(false);
    }
  };

  const handleReviewPageChange = (newPage) => {
    setReviewPage(newPage);
    loadReviewQueue(newPage);
  };

  const handleReviewPageSizeChange = (newSize) => {
    setReviewPageSize(newSize);
    setReviewPage(0);
    loadReviewQueue(0, newSize);
  };

  // Create Topic
  const handleCreateTopic = async (topicDataOrEvent) => {
    let name = newTopicName;
    let description = newTopicDesc;
    if (topicDataOrEvent && topicDataOrEvent.name !== undefined) {
      name = topicDataOrEvent.name;
      description = topicDataOrEvent.description || '';
    } else if (topicDataOrEvent?.preventDefault) {
      topicDataOrEvent.preventDefault();
    }
    if (!name?.trim()) return;

    try {
      const created = await lecturerCatalogApi.createTopic(selectedSubjectId, {
        name: name.trim(),
        description: (description || '').trim()
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
      await loadDocuments();
      setDocFileToUpload(null);
      showToast({ type: 'success', message: 'Đã nhận tài liệu! Đang xử lý bóc tách và lập chỉ mục ngầm.' });
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
      await loadDocuments();
      showToast({ type: 'success', message: 'Đã gửi yêu cầu lập chỉ mục lại.' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  // Delete Document
  const handleConfirmDeleteDoc = async () => {
    if (!deleteDocTarget) return;
    try {
      await courseDocumentApi.deleteDocument(deleteDocTarget.documentId);
      setDocuments(prev => prev.filter(d => d.documentId !== deleteDocTarget.documentId));
      showToast({ type: 'success', message: 'Xóa tài liệu giáo trình thành công.' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setDeleteDocTarget(null);
    }
  };

  // Create Manual Question
  const handleCreateManualQuestion = async (formDataOrEvent) => {
    let contentVal = manualContent;
    let expectedVal = manualExpectedAnswer;
    let topicIdVal = manualTopicId;
    let bloomVal = manualBloom;
    let criteriaVal = manualCriteria;

    if (formDataOrEvent && formDataOrEvent.content !== undefined) {
      contentVal = formDataOrEvent.content;
      expectedVal = formDataOrEvent.expectedAnswer;
      topicIdVal = formDataOrEvent.topicId;
      bloomVal = formDataOrEvent.bloomLevel;
      criteriaVal = formDataOrEvent.criteria;
    } else if (formDataOrEvent?.preventDefault) {
      formDataOrEvent.preventDefault();
    }

    if (!contentVal?.trim() || !expectedVal?.trim()) {
      showToast({ type: 'error', message: 'Vui lòng nhập nội dung câu hỏi và đáp án mong đợi.' });
      return;
    }

    const rubricError = validateCriteria(criteriaVal);
    if (rubricError) {
      showToast({ type: 'error', message: rubricError });
      return;
    }

    setIsSubmittingQuestion(true);
    try {
      const payload = {
        topicId: topicIdVal || null,
        content: contentVal.trim(),
        expectedAnswer: expectedVal.trim(),
        bloomLevel: bloomVal,
        rubric: toRubricPayload(criteriaVal)
      };

      await questionBankApi.createManualQuestion(selectedSubjectId, payload);
      showToast({ type: 'success', message: 'Tạo câu hỏi thành công! Bản nháp đã được gửi vào hàng đợi duyệt.' });
      setActiveTab('review');
      setReviewPage(0);
      loadReviewQueue(0);
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
      setReviewPage(0);
      loadReviewQueue(0);
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
  const handleTriggerAiGeneration = async (customConfig) => {
    const docIds = customConfig?.documentIds || genDocumentIds;
    if (docIds.length === 0) {
      showToast({ type: 'error', message: 'Vui lòng chọn ít nhất 1 tài liệu giáo trình READY để sinh câu hỏi.' });
      return;
    }

    const total = Number(customConfig?.totalQuestions ?? genTotalQuestions);
    if (!total || total < 1 || total > 20) {
      showToast({ type: 'error', message: 'Số lượng câu hỏi phải từ 1 đến 20.' });
      return;
    }

    const dist = customConfig?.bloomDistribution || bloomCounts;
    const allocated = Object.values(dist).reduce((sum, n) => sum + (Number(n) || 0), 0);
    if (allocated !== total) {
      showToast({
        type: 'error',
        message: `Tổng số câu phân bổ (${allocated}) chưa khớp với số lượng yêu cầu (${total}). Vui lòng cân bằng lại trước khi sinh câu hỏi.`
      });
      return;
    }

    setIsGeneratingAi(true);
    try {
      const distribution = Object.fromEntries(
        Object.entries(dist).filter(([_, count]) => Number(count) > 0)
      );

      const payload = {
        subjectId: selectedSubjectId,
        topicId: (customConfig ? customConfig.topicId : genTopicId) || null,
        documentIds: docIds,
        totalQuestions: total,
        bloomDistribution: distribution,
        lecturerNote: (customConfig ? customConfig.lecturerNote : genLecturerNote)?.trim() || ''
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

  // Batch Approve Drafts
  const handleBatchApprove = async () => {
    if (selectedVersionIds.length === 0) return;
    setIsBatchApproving(true);
    try {
      await questionBankApi.batchApproveVersions(selectedVersionIds);
      showToast({
        type: 'success',
        message: `Phê duyệt thành công ${selectedVersionIds.length} câu hỏi vào Ngân Hàng Đề Thi!`
      });
      setSelectedVersionIds([]);
      loadReviewQueue();
      loadBank();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsBatchApproving(false);
    }
  };

  // Reject Draft
  const handleConfirmReject = async (targetOverride, reasonOverride) => {
    const target = targetOverride || rejectVersionTarget;
    const reason = reasonOverride || rejectReason;
    if (!target || !reason?.trim()) return;
    try {
      await questionBankApi.rejectVersion(target.versionId, {
        reason: reason.trim(),
        expectedVersion: target.lockVersion
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
  const handleConfirmRegenerate = async (targetOverride, feedbackOverride) => {
    const target = targetOverride || regenVersionTarget;
    const feedback = feedbackOverride || regenFeedback;
    if (!target) return;
    try {
      await questionBankApi.regenerateQuestion(target.versionId, {
        feedback: (feedback || '').trim(),
        expectedVersion: target.lockVersion
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
  const handleImportQuestions = async (importPayloadOrEvent) => {
    let file = importFile;
    let dryRun = isDryRun;
    if (importPayloadOrEvent && importPayloadOrEvent.file !== undefined) {
      file = importPayloadOrEvent.file;
      dryRun = importPayloadOrEvent.isDryRun ?? isDryRun;
    } else if (importPayloadOrEvent?.preventDefault) {
      importPayloadOrEvent.preventDefault();
    }
    if (!file) return;

    setIsImporting(true);
    setImportReport(null);
    try {
      const report = await questionImportApi.importQuestions(selectedSubjectId, file, dryRun);
      setImportReport(report);
      if (dryRun) {
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

  const openTextImport = async () => {
    setTextImport({ text: '', topicId: '', bloomLevel: '', rubricTemplateId: '' });
    try {
      const list = await rubricApi.listTemplates(selectedSubjectId);
      setRubricTemplates(Array.isArray(list) ? list : []);
    } catch (err) {
      setRubricTemplates([]);
    }
  };

  const handleImportText = async (e) => {
    e.preventDefault();
    if (!textImport.text.trim()) return;
    setIsImportingText(true);
    try {
      const result = await questionImportApi.importText(selectedSubjectId, textImport);
      showToast({ type: 'success', message: `Đã nhập ${result?.totalImported || 0} câu hỏi vào Hàng đợi duyệt.` });
      setTextImport(null);
      loadReviewQueue();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsImportingText(false);
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
    <div className="space-y-6 animate-modal-entry bg-paper text-ink min-h-screen">

      {/* HEADER SECTION & SUBJECT PICKER */}
      <div className="bg-white p-5 rounded-xl border border-pink-200 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-heading font-bold text-ink tracking-tight">
            Ngân Hàng Câu Hỏi &amp; Quản Lý RAG Giáo Trình
          </h1>
          <p className="text-xs text-slate-500 mt-1 max-w-2xl">
            Quản trị kho câu hỏi thi vấn đáp, liên kết giáo trình RAG và cấu hình tiêu chí đánh giá chuẩn Bloom.
          </p>
        </div>

        {/* SUBJECT SWITCHER BOX */}
        <div className="w-full md:w-80 bg-paper border border-pink-200 rounded-xl p-3.5 shadow-sm transition-all">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-[10px] text-green-700 uppercase tracking-wider font-bold">MÔN HỌC PHỤ TRÁCH</span>
            <span className="px-2 py-0.5 rounded-full bg-green-500 text-paper font-bold text-[10px] tracking-wide">ACTIVE</span>
          </div>
          <div className="relative">
            <select
              value={selectedSubjectId}
              onChange={(e) => setSelectedSubjectId(e.target.value)}
              className="w-full appearance-none bg-surface-container-lowest border border-pink-200 rounded-lg py-1.5 pl-3 pr-8 text-xs font-bold text-ink cursor-pointer focus:outline-none focus:border-pink-500 shadow-sm"
            >
              {subjects.map(s => (
                <option key={s.subjectId} value={s.subjectId}>
                  [{s.subjectCode}] {s.subjectName}
                </option>
              ))}
            </select>
            <ChevronsUpDown className="w-4 h-4 pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-green-700" />
          </div>
          <div className="mt-2 pt-1.5 border-t-2 border-cream flex items-center justify-between text-[11px] text-on-surface-variant">
            <span>Mã: <strong className="text-ink">{activeSubject ? activeSubject.subjectCode : 'Chưa chọn'}</strong></span>
            <span>Học kỳ: <strong className="text-ink">Fall 2026</strong></span>
          </div>
        </div>
      </div>

      {/* TOPIC BANNER & SHORTCUT BAR */}
      <div className="bg-paper border border-pink-200 rounded-xl p-4 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-10 h-10 rounded-lg bg-green-100 border border-pink-200 flex items-center justify-center shrink-0 text-green-700 shadow-sm">
            <FolderPlus className="w-5 h-5 text-green-700" />
          </div>
          <div className="min-w-0">
            <div className="text-xs text-ink flex items-center gap-1.5 flex-wrap">
              <span>Chủ đề đề cương:</span>
              <span className="font-bold text-green-800">{topics.length} chủ đề đang kích hoạt</span>
              <span className="text-pink-600">•</span>
              <span className="text-on-surface-variant font-normal">Quản trị phân bố câu hỏi theo chuẩn đầu ra</span>
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={() => setIsCreateTopicModalOpen(true)}
          className="shrink-0 inline-flex items-center justify-center gap-1.5 px-3.5 py-1.5 bg-paper text-green-700 border border-pink-200 rounded-xl font-bold text-xs shadow-sm hover:bg-green-100 hover:text-green-900 active:scale-[0.98] transition-all"
        >
          <Plus className="w-4 h-4 text-green-700" />
          <span>+ Thêm Chủ Đề Mới</span>
        </button>
      </div>

      {/* THREE MAIN WORKSPACE TABS */}
      <div className="flex items-center gap-2 overflow-x-auto border-b-2 border-cream pb-2">
        <button
          type="button"
          onClick={() => setActiveTab('bank')}
          className={`inline-flex items-center gap-2 px-4 py-2 border-2 rounded-xl text-xs font-bold transition-all shrink-0 ${
            activeTab === 'bank'
              ? 'bg-pink-600 border-slate-900 text-white shadow-xs' : 'bg-white border-pink-200 hover:bg-slate-50 text-slate-600 hover:text-ink'
          }`}
        >
          <Database className="w-4 h-4" />
          <span>Ngân Hàng Chính Thức</span>
          <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border-2 ${
            activeTab === 'bank'
              ? 'bg-slate-800 text-slate-100 border-slate-700'
              : 'bg-emerald-50 text-emerald-800 border-transparent'
          }`}>
            {bankQuestions.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('documents')}
          className={`inline-flex items-center gap-2 px-4 py-2 border-2 rounded-xl text-xs font-bold transition-all shrink-0 ${
            activeTab === 'documents'
              ? 'bg-pink-600 border-slate-900 text-white shadow-xs' : 'bg-white border-pink-200 hover:bg-slate-50 text-slate-600 hover:text-ink'
          }`}
        >
          <FileText className="w-4 h-4" />
          <span>Kho Giáo Trình RAG</span>
          <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border-2 ${
            activeTab === 'documents'
              ? 'bg-slate-800 text-slate-100 border-slate-700'
              : 'bg-emerald-50 text-emerald-800 border-transparent'
          }`}>
            {documents.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('review')}
          className={`inline-flex items-center gap-2 px-4 py-2 border-2 rounded-xl text-xs font-bold transition-all shrink-0 ${
            activeTab === 'review'
              ? 'bg-pink-600 border-slate-900 text-white shadow-xs' : 'bg-white border-pink-200 hover:bg-slate-50 text-slate-600 hover:text-ink'
          }`}
        >
          <Sparkles className="w-4 h-4 text-pink-600" />
          <span>Hàng Đợi Thẩm Định &amp; AI</span>
          <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border-2 ${
            activeTab === 'review'
              ? 'bg-slate-800 text-slate-100 border-slate-700'
              : 'bg-slate-100 text-slate-600 border-pink-200'
          }`}>
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
          <div className="bg-paper p-6 rounded-2xl border border-dashed border-pink-200 shadow-sm flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-2xl bg-green-100 border border-pink-200 text-green-700 flex items-center justify-center mb-3 shadow-sm">
              <Upload className="w-6 h-6" />
            </div>
            <h3 className="font-heading font-extrabold text-ink text-sm">
              Tải Lên Tài Liệu Giáo Trình &amp; Đề Cương (RAG Source)
            </h3>
            <p className="text-xs text-on-surface-variant max-w-md mt-1">
              Hệ thống tự động sử dụng Apache Tika bóc tách văn bản, cắt đoạn 800 tokens và vector hóa 1536 chiều với pgvector.
            </p>
            <p className="text-[11px] text-green-700 mt-0.5 font-medium">
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
                className="px-4 py-2 rounded-xl bg-paper border border-pink-200 hover:bg-emerald-50 text-emerald-800 font-bold text-xs cursor-pointer shadow-sm transition active:scale-95"
              >
                {docFileToUpload ? docFileToUpload.name : 'Chọn Tệp Tin Từ Máy...'}
              </label>

              <button
                type="button"
                onClick={handleUploadDocument}
                disabled={!docFileToUpload || isUploadingDoc}
                className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 flex items-center gap-2"
              >
                {isUploadingDoc && <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>}
                <span>Bắt Đầu Tải Lên &amp; Lập Chỉ Mục</span>
              </button>
            </div>
          </div>

          {/* DOCUMENTS LIST */}
          <div className="bg-paper rounded-2xl border border-pink-200 shadow-md overflow-hidden">
            <div className="p-4 bg-green-50 border-b border-pink-200 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <FileText className="w-4 h-4 text-green-700" />
                <h3 className="font-heading font-extrabold text-xs text-ink uppercase tracking-wider">
                  Danh Sách Giáo Trình Đã Tải Lên ({documents.length})
                </h3>
              </div>
              <button
                type="button"
                onClick={loadDocuments}
                className="p-1.5 text-green-700 hover:text-green-900 rounded-lg hover:bg-green-100 transition"
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
              <div className="p-12 text-center text-on-surface-variant text-xs">
                Chưa có tài liệu giáo trình nào cho môn học này. Hãy tải lên tài liệu đầu tiên để phục vụ sinh câu hỏi AI.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-green-100/60 border-b-2 border-cream text-green-900 font-bold uppercase tracking-wider text-[11px]">
                      <th className="py-3 px-4">Tên Tệp Tin</th>
                      <th className="py-3 px-4">Dung Lượng</th>
                      <th className="py-3 px-4">Trạng Thái RAG</th>
                      <th className="py-3 px-4">Số Đoạn (Chunks)</th>
                      <th className="py-3 px-4">Ghi Chú Lập Chỉ Mục</th>
                      <th className="py-3 px-4 text-right">Thao Tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-cream">
                    {documents.map((doc) => (
                      <tr key={doc.documentId} className="hover:bg-green-50/60 transition">
                        <td className="py-3.5 px-4 font-semibold text-ink">
                          <div className="flex items-center gap-2">
                            <FileText className="w-4 h-4 text-pink-600 shrink-0" />
                            <span className="truncate max-w-xs">{doc.fileName}</span>
                          </div>
                        </td>
                        <td className="py-3.5 px-4 text-on-surface-variant font-mono">
                          {(doc.fileSizeBytes / (1024 * 1024)).toFixed(2)} MB
                        </td>
                        <td className="py-3.5 px-4">
                          {doc.indexingStatus === 'READY' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-emerald-50 text-emerald-800 border border-pink-200 shadow-sm">
                              <CheckCircle2 className="w-3.5 h-3.5 text-green-700" />
                              READY (Đã Lập Chỉ Mục)
                            </span>
                          )}
                          {doc.indexingStatus === 'INDEXING' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-slate-100 text-slate-800 border border-rose-200">
                              <span className="w-3 h-3 border-2 border-pink-700 border-t-transparent rounded-full animate-spin"></span>
                              INDEXING...
                            </span>
                          )}
                          {doc.indexingStatus === 'UPLOADED' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-paper text-ink border border-pink-200">
                              <Clock className="w-3.5 h-3.5 text-green-700" />
                              UPLOADED
                            </span>
                          )}
                          {doc.indexingStatus === 'FAILED' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-rose-50 text-rose-700 border border-rose-200">
                              <AlertCircle className="w-3.5 h-3.5 text-error" />
                              FAILED
                            </span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 font-mono font-bold text-green-900">
                          {doc.totalChunks || 0} chunks
                        </td>
                        <td className="py-3.5 px-4">
                          <p className="text-[11px] text-on-surface-variant line-clamp-2 max-w-sm">
                            {doc.errorMessage || `Số lần lập chỉ mục: ${doc.indexAttempts || 0}`}
                          </p>
                        </td>
                        <td className="py-3.5 px-4 text-right space-x-1">
                          {doc.indexingStatus === 'FAILED' && (
                            <button
                              type="button"
                              onClick={() => handleRetryIndexing(doc.documentId)}
                              className="p-1.5 rounded-lg bg-paper border border-pink-200 text-green-800 hover:bg-green-100 transition shadow-sm"
                              title="Thử lập chỉ mục lại"
                            >
                              <RotateCcw className="w-4 h-4" />
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => setDeleteDocTarget(doc)}
                            className="p-1.5 rounded-lg bg-paper border border-pink-200 text-on-surface-variant hover:text-error hover:border-error hover:bg-pink-50 transition shadow-sm"
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
          <div className="flex flex-col xl:flex-row items-stretch xl:items-center justify-between gap-3 bg-paper p-4 rounded-xl border border-pink-200 shadow-sm">
            <div className="flex flex-col md:flex-row items-stretch md:items-center gap-2 flex-1">
              {/* Search */}
              <div className="relative min-w-[220px] flex-1">
                <Search className="w-4 h-4 text-green-700 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Tìm câu hỏi, từ khóa hoặc mã..."
                  value={searchBankKeyword}
                  onChange={(e) => setSearchBankKeyword(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      setBankPage(0);
                      loadBank(0);
                    }
                  }}
                  className="w-full pl-9 pr-3 py-1.5 rounded-lg text-xs bg-surface-container-lowest border border-pink-200 text-ink placeholder:text-on-surface-variant focus:outline-none focus:border-pink-500 shadow-sm font-medium"
                />
              </div>

              {/* Topic Filter */}
              <div className="relative">
                <select
                  value={filterTopicId}
                  onChange={(e) => {
                    const next = e.target.value;
                    setFilterTopicId(next);
                    setBankPage(0);
                    loadBank(0, undefined, next, undefined, undefined);
                  }}
                  className="w-full md:w-48 appearance-none bg-surface-container-lowest border border-pink-200 rounded-lg py-1.5 pl-3 pr-8 text-xs font-semibold text-ink focus:outline-none focus:border-pink-500 shadow-sm cursor-pointer"
                >
                  <option value="ALL">Tất cả chủ đề</option>
                  {topics.map(t => (
                    <option key={t.topicId} value={t.topicId}>{t.name}</option>
                  ))}
                </select>
                <ChevronDown className="w-4 h-4 pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-green-700" />
              </div>

              {/* Bloom Filter */}
              <div className="relative">
                <select
                  value={filterBloom}
                  onChange={(e) => {
                    const next = e.target.value;
                    setFilterBloom(next);
                    setBankPage(0);
                    loadBank(0, undefined, undefined, next, undefined);
                  }}
                  className="w-full md:w-52 appearance-none bg-surface-container-lowest border border-pink-200 rounded-lg py-1.5 pl-3 pr-8 text-xs font-semibold text-ink focus:outline-none focus:border-pink-500 shadow-sm cursor-pointer"
                >
                  <option value="ALL">Tất cả cấp độ Bloom</option>
                  {BLOOM_LEVELS.map(b => (
                    <option key={b.value} value={b.value}>{b.label}</option>
                  ))}
                </select>
                <ChevronDown className="w-4 h-4 pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-green-700" />
              </div>

              <button
                type="button"
                onClick={() => {
                  setBankPage(0);
                  loadBank(0);
                }}
                className="p-2 rounded-lg bg-paper text-green-700 border border-pink-200 shadow-sm hover:bg-green-100 transition shrink-0"
                title="Lọc kết quả"
              >
                <Filter className="w-4 h-4" />
              </button>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={() => setIsImportModalOpen(true)}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-paper text-green-700 border border-pink-200 rounded-xl font-bold text-xs shadow-sm hover:bg-green-100 hover:text-green-900 active:scale-[0.98] transition-all"
              >
                <FileSpreadsheet className="w-4 h-4 text-green-700" />
                <span>Nhập Excel/CSV</span>
              </button>

              <button
                type="button"
                onClick={openTextImport}
                disabled={!selectedSubjectId}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-paper text-green-700 border border-pink-200 rounded-xl font-bold text-xs shadow-sm hover:bg-green-100 hover:text-green-900 active:scale-[0.98] transition-all disabled:opacity-50"
              >
                <FileText className="w-4 h-4 text-green-700" />
                <span>Nhập Từ Văn Bản</span>
              </button>

              <button
                type="button"
                onClick={() => setIsManualQuestionModalOpen(true)}
                className="inline-flex items-center gap-1.5 px-4 py-1.5 bg-pink-600 text-white rounded-xl font-semibold text-xs shadow-xs hover:bg-pink-700 active:scale-[0.98] transition-all"
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
            <div className="bg-paper p-12 rounded-2xl border border-pink-200 shadow-md text-center">
              <Database className="w-12 h-12 text-green-700 mx-auto mb-3 opacity-60" />
              <h3 className="font-heading font-bold text-ink text-sm">Chưa có câu hỏi chính thức</h3>
              <p className="text-xs text-on-surface-variant mt-1 max-w-sm mx-auto">
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
                  <article key={q.questionId} className="bg-paper border border-pink-200 rounded-2xl p-5 shadow-md transition-all space-y-3">
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b-2 border-cream pb-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border border-pink-200 shadow-sm">
                          {q.questionCode}
                        </span>
                        <span className="text-xs font-semibold text-green-800">
                          {q.topicName || 'Chủ đề chung'}
                        </span>
                      </div>

                      <div className="flex flex-wrap items-center gap-2">
                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border-2 ${bloomMeta.color}`}>
                          {bloomMeta.label}
                        </span>

                        {q.status === 'ARCHIVED' ? (
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-surface-container-high text-on-surface-variant border border-pink-200/50">
                            ARCHIVED
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-green-500 text-paper text-[10px] font-bold shadow-sm">
                            <CheckCircle2 className="w-3 h-3 text-paper" />
                            APPROVED
                          </span>
                        )}
                        {q.hasPendingDraft && (
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-700 border border-rose-200">
                            CÓ BẢN NHÁP
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Question Content */}
                    <div className="text-sm text-ink font-semibold leading-relaxed font-heading">
                      {cv.content}
                    </div>

                    {/* Collapsible Expected Answer */}
                    <div className="pt-1">
                      <button
                        type="button"
                        onClick={() => toggleExpectedAnswer(q.questionId)}
                        className="text-xs font-bold text-green-800 hover:text-green-950 inline-flex items-center gap-1.5 focus:outline-none"
                      >
                        {isExpanded ? <EyeOff className="w-3.5 h-3.5 text-pink-600" /> : <Eye className="w-3.5 h-3.5 text-pink-600" />}
                        <span>{isExpanded ? 'Ẩn đáp án mong đợi & thang điểm' : 'Xem đáp án mong đợi & thang điểm'}</span>
                      </button>

                      {isExpanded && (
                        <div className="mt-2.5 p-4 bg-pink-50 rounded-xl border border-pink-200 shadow-sm text-xs text-ink space-y-3 animate-modal-entry">
                          <div>
                            <div className="flex items-center gap-1.5 text-pink-900 font-bold text-xs mb-1">
                              <Lightbulb className="w-4 h-4 text-pink-600 shrink-0" />
                              <span>Đáp án &amp; Định hướng chấm điểm mong đợi:</span>
                            </div>
                            <p className="leading-relaxed font-normal text-ink">{cv.expectedAnswer}</p>
                          </div>

                          {cv.rubric && cv.rubric.criteria && (
                            <div className="pt-2.5 border-t-2 border-cream">
                              <div className="flex flex-wrap items-center justify-between gap-1 pb-1.5">
                                <span className="font-bold text-ink text-xs inline-flex items-center gap-1">
                                  <Sliders className="w-4 h-4 text-green-700 shrink-0" />
                                  Ma Trận Rubric Đính Kèm ({cv.rubric.criteria.length} Tiêu Chí):
                                </span>
                                <span className="text-pink-900 font-bold text-xs bg-slate-100 border border-pink-200 text-slate-800 px-2 py-0.5 rounded-md shadow-sm">
                                  Tổng điểm: {cv.rubric.totalScore || 10}đ
                                </span>
                              </div>
                              <div className="grid grid-cols-1 md:grid-cols-3 gap-2 mt-2">
                                {cv.rubric.criteria.map((crit, idx) => (
                                  <div key={crit.criterionId || idx} className="bg-surface-container-lowest border border-pink-200/70 rounded-lg p-2.5">
                                    <div className="flex items-center justify-between text-xs font-bold text-pink-800 mb-1">
                                      <span>{crit.name}</span>
                                      <span className="font-mono text-green-800 shrink-0 ml-1.5">{crit.maxScore}đ</span>
                                    </div>
                                    {crit.description && (
                                      <p className="text-[11px] text-on-surface-variant leading-snug">{crit.description}</p>
                                    )}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Bottom Toolbar */}
                    <div className="flex flex-wrap items-center justify-between pt-2.5 border-t-2 border-cream text-xs text-on-surface-variant gap-2">
                      <div className="flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5 text-green-700 shrink-0" />
                        <span>Cập nhật: <strong className="text-ink">{new Date(q.updatedAt || Date.now()).toLocaleDateString('vi-VN')}</strong></span>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => handleOpenQuestionDetail(q.questionId)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-paper text-green-700 border border-pink-200 rounded-xl font-bold text-xs shadow-sm hover:bg-green-100 hover:text-green-900 active:scale-[0.98] transition-all"
                        >
                          <Layers className="w-3.5 h-3.5 text-green-700" />
                          <span>Chi Tiết &amp; Lịch Sử</span>
                        </button>
                        {q.status !== 'ARCHIVED' && (
                          <>
                            <button
                              type="button"
                              onClick={() => handleCreateDraftFromApproved(q.questionId)}
                              disabled={q.hasPendingDraft}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-pink-600 text-white rounded-xl font-semibold text-xs shadow-xs hover:bg-pink-700 active:scale-[0.98] transition-all disabled:opacity-50"
                            >
                              <Edit3 className="w-3.5 h-3.5" />
                              <span>Tạo Bản Nháp Mới (Copy-on-Write)</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => setArchiveQuestionTarget(q)}
                              className="p-1.5 rounded-lg bg-paper border border-pink-200 text-on-surface-variant hover:text-error hover:border-error hover:bg-pink-50 shadow-sm transition"
                              title="Lưu trữ câu hỏi (Archive)"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          )}

          {/* BANK PAGINATION */}
          {!isLoadingBank && bankQuestions.length > 0 && (
            <Pagination
              page={bankPage}
              size={bankPageSize}
              totalPages={bankTotalPages}
              totalElements={bankTotalElements}
              onPageChange={handleBankPageChange}
              onSizeChange={handleBankPageSizeChange}
              itemLabel="câu hỏi đã duyệt"
            />
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 3: HÀNG ĐỢI THẨM ĐỊNH & AI RAG (REVIEW QUEUE)                          */}
      {/* ========================================================================= */}
      {activeTab === 'review' && (
        <div className="space-y-6">
          {/* AI RAG GENERATION TRIGGER BANNER */}
          <div className="relative overflow-hidden bg-pink-100 border border-pink-200 rounded-2xl p-6 shadow-md flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
            <div className="relative z-10 space-y-1.5 max-w-xl">
              <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-paper border border-pink-200 text-pink-800 text-[10px] font-bold uppercase tracking-wider shadow-sm">
                <Sparkles className="w-3.5 h-3.5 text-pink-600" />
                <span>Trình Khởi Tạo Câu Hỏi Tự Động (AI RAG Engine)</span>
              </div>
              <h2 className="text-lg font-heading font-extrabold text-ink tracking-tight">
                Sinh Câu Hỏi Tự Động Từ Giáo Trình Đã Vector Hóa
              </h2>
              <p className="text-xs text-on-surface-variant leading-relaxed">
                AI Agent truy xuất các đoạn ngữ cảnh (chunks) chính xác từ kho học liệu đã đối soát, tự động cấu trúc hóa câu hỏi theo thang tư duy Bloom 6 cấp độ và sinh kèm ma trận Rubric chấm điểm chuẩn xác.
              </p>
            </div>

            <button
              type="button"
              onClick={() => setIsAiGenModalOpen(true)}
              className="relative z-10 px-4 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] flex items-center gap-2 shrink-0"
            >
              <Sparkles className="w-4 h-4 text-ink" />
              <span>Thiết Lập &amp; Kích Hoạt RAG</span>
            </button>
          </div>

          {/* STATUS FILTER FOR QUEUE */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2 bg-paper p-3.5 rounded-2xl border border-pink-200 shadow-sm">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-xs font-bold text-ink">Lọc Theo Trạng Thái:</span>
              <div className="flex items-center gap-1.5">
                {['DRAFT', 'APPROVED', 'REJECTED', 'ALL'].map(st => (
                  <button
                    key={st}
                    type="button"
                    onClick={() => {
                      setReviewStatusFilter(st);
                      setReviewPage(0);
                      loadReviewQueue(0, undefined, st);
                    }}
                    className={`px-3 py-1 rounded-xl text-xs font-bold border-2 transition ${
                      reviewStatusFilter === st
                        ? 'bg-pink-600 text-white border-slate-900 shadow-xs'
                        : 'bg-white text-slate-600 border-transparent hover:border-pink-200'
                    }`}
                  >
                    {st}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex items-center gap-2 self-end sm:self-auto">
              {reviewVersions.filter(v => v.status === 'DRAFT').length > 0 && (
                <label className="flex items-center gap-2 px-2.5 py-1 rounded-xl bg-green-50 border border-pink-200 shadow-sm cursor-pointer select-none text-xs font-bold text-ink">
                  <input
                    type="checkbox"
                    checked={
                      reviewVersions.filter(v => v.status === 'DRAFT').length > 0 &&
                      reviewVersions.filter(v => v.status === 'DRAFT').every(v => selectedVersionIds.includes(v.versionId))
                    }
                    onChange={(e) => {
                      const approvable = reviewVersions.filter(v => v.status === 'DRAFT');
                      if (e.target.checked) {
                        const newIds = Array.from(new Set([...selectedVersionIds, ...approvable.map(v => v.versionId)]));
                        setSelectedVersionIds(newIds);
                      } else {
                        const approvableIds = new Set(approvable.map(v => v.versionId));
                        setSelectedVersionIds(prev => prev.filter(id => !approvableIds.has(id)));
                      }
                    }}
                    className="w-4 h-4 rounded border border-pink-200 text-pink-600 focus:ring-green-400 accent-pink-600 cursor-pointer"
                  />
                  <span>Chọn tất cả ({reviewVersions.filter(v => v.status === 'DRAFT').length})</span>
                </label>
              )}

              <button
                type="button"
                onClick={loadReviewQueue}
                className="p-1.5 text-green-700 hover:text-green-900 rounded-lg hover:bg-green-100 transition"
                title="Làm mới hàng đợi"
              >
                <RefreshCw className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* REVIEW QUEUE LIST */}
          {isLoadingReview ? (
            <div className="space-y-4">
              <SkeletonCard />
              <SkeletonCard />
            </div>
          ) : reviewVersions.length === 0 ? (
            <div className="bg-paper p-12 rounded-2xl border border-pink-200 shadow-md text-center">
              <Sparkles className="w-12 h-12 text-pink-600 mx-auto mb-3 opacity-60" />
              <h3 className="font-heading font-bold text-ink text-sm">Hàng đợi đang trống</h3>
              <p className="text-xs text-on-surface-variant mt-1 max-w-sm mx-auto">
                Không có bản nháp nào cần thẩm định. Bạn có thể kích hoạt RAG AI để tạo các câu hỏi mới.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {reviewVersions.map((v) => {
                const bloomMeta = BLOOM_LEVELS.find(b => b.value === v.bloomLevel) || BLOOM_LEVELS[0];

                return (
                  <article key={v.versionId} className="bg-paper border border-pink-200 rounded-2xl p-5 shadow-md space-y-4">

                    {/* Header Bar */}
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b-2 border-cream pb-3">
                      <div className="flex flex-wrap items-center gap-2">
                        {v.status === 'DRAFT' && (
                          <input
                            type="checkbox"
                            checked={selectedVersionIds.includes(v.versionId)}
                            onChange={(e) => {
                              if (e.target.checked) {
                                setSelectedVersionIds(prev => [...prev, v.versionId]);
                              } else {
                                setSelectedVersionIds(prev => prev.filter(id => id !== v.versionId));
                              }
                            }}
                            className="w-4 h-4 rounded border border-pink-200 text-pink-600 focus:ring-green-400 accent-pink-600 cursor-pointer"
                            title="Chọn câu hỏi này để duyệt hàng loạt"
                          />
                        )}
                        <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border border-pink-200 shadow-sm">
                          {v.questionCode || 'DRAFT-VER'}
                        </span>
                        <span className="text-xs font-semibold text-green-800">
                          {v.topicName || 'Chủ đề học phần'}
                        </span>
                        <span className="text-[10px] px-2 py-0.5 rounded-md font-mono font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
                          Origin: {v.origin || 'MANUAL'}
                        </span>
                      </div>

                      <div className="flex flex-wrap items-center gap-2">
                        {/* Bloom Confirmation Badge */}
                        {v.bloomConfirmed ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-pink-200 shadow-sm">
                            <Check className="w-3 h-3 text-green-700" />
                            Đã Xác Nhận Bloom
                          </span>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleConfirmBloom(v)}
                            className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-800 border border-rose-200 hover:bg-pink-200 transition active:scale-95 shadow-sm"
                            title="Bấm để xác nhận cấp độ nhận thức Bloom này hợp lệ"
                          >
                            <AlertCircle className="w-3 h-3 text-pink-600" />
                            Xác Nhận Bloom ({v.bloomLevel})
                          </button>
                        )}

                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border-2 ${bloomMeta.color}`}>
                          {bloomMeta.label}
                        </span>

                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border-2 ${
                          v.status === 'APPROVED'
                            ? 'bg-emerald-50 text-emerald-800 border-pink-200'
                            : v.status === 'REJECTED'
                              ? 'bg-rose-50 text-rose-700 border-error'
                              : 'bg-slate-100 text-slate-800 border-pink-400'
                        }`}>
                          {v.status}
                        </span>
                      </div>
                    </div>

                    {/* Question Content */}
                    <div>
                      <p className="text-[10px] font-bold text-green-700 uppercase tracking-wider mb-1">Nội Dung Câu Hỏi:</p>
                      <h3 className="text-sm text-ink font-semibold leading-relaxed font-heading">{v.content}</h3>
                    </div>

                    {/* Expected Answer */}
                    <div className="p-3.5 bg-pink-50 rounded-xl border border-pink-200 shadow-sm">
                      <p className="text-[10px] font-bold text-pink-900 uppercase tracking-wider mb-1">Đáp Án Tiêu Chuẩn:</p>
                      <p className="text-xs text-ink leading-relaxed font-normal">{v.expectedAnswer}</p>
                    </div>

                    {/* Source Citations (Grounding Evidence) */}
                    {v.sources && v.sources.length > 0 && (
                      <div className="p-3.5 bg-green-50 rounded-xl border border-pink-200 shadow-sm space-y-1.5">
                        <div className="flex items-center gap-1.5 text-green-900 font-bold text-[11px]">
                          <BookOpen className="w-3.5 h-3.5 text-green-700" />
                          <span>Cơ Sở Đối Chiếu Giáo Trình (Grounding Evidence Chunk #{v.sources[0].order}):</span>
                        </div>
                        <p className="text-[11px] text-ink italic bg-surface-container-lowest p-2.5 rounded-lg border border-pink-200/50 leading-relaxed">
                          "{v.sources[0].citationQuote}"
                        </p>
                      </div>
                    )}

                    {/* Rubric Breakdown */}
                    {v.rubric && v.rubric.criteria && (
                      <div className="pt-2 border-t-2 border-cream">
                        <div className="flex flex-wrap items-center justify-between gap-1 mb-2">
                          <span className="text-[10px] font-bold text-green-800 uppercase tracking-wider">
                            Ma Trận Tiêu Chí Rubric ({v.rubric.criteria.length} tiêu chí):
                          </span>
                          <span className="text-pink-900 font-bold text-xs bg-slate-100 border border-pink-200 text-slate-800 px-2 py-0.5 rounded-md shadow-sm">
                            Tổng điểm: {v.rubric.totalScore || 10}đ
                          </span>
                        </div>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                          {v.rubric.criteria.map((c, idx) => (
                            <div key={idx} className="p-2.5 bg-surface-container-lowest rounded-lg border border-pink-200/70 flex items-center justify-between text-xs">
                              <div>
                                <span className="font-bold text-pink-800">{c.name}</span>
                                {c.description && <p className="text-[10px] text-on-surface-variant line-clamp-1">{c.description}</p>}
                              </div>
                              <span className="font-mono font-bold text-green-800 shrink-0 ml-2">{c.maxScore}đ</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Action Bar */}
                    <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t-2 border-cream">
                      <div className="flex flex-wrap items-center gap-2">
                        {v.origin === 'AI_RAG' && v.status === 'DRAFT' && (
                          <button
                            type="button"
                            onClick={() => { setRegenVersionTarget(v); setRegenFeedback(''); }}
                            className="px-3 py-1.5 rounded-xl bg-paper text-green-700 border border-pink-200 font-bold text-xs shadow-sm hover:bg-green-100 transition active:scale-95 flex items-center gap-1.5"
                          >
                            <Sparkles className="w-3.5 h-3.5 text-pink-600" />
                            <span>Tạo Lại Với AI (Regenerate)</span>
                          </button>
                        )}

                        {v.status === 'DRAFT' && (
                          <button
                            type="button"
                            onClick={() => handleOpenEditDraft(v)}
                            className="px-3 py-1.5 rounded-xl bg-paper text-ink border border-pink-200 font-bold text-xs shadow-sm hover:bg-green-50 transition active:scale-95 flex items-center gap-1.5"
                          >
                            <Edit3 className="w-3.5 h-3.5 text-green-700" />
                            <span>Chỉnh Sửa</span>
                          </button>
                        )}

                        {v.status === 'DRAFT' && (
                          <button
                            type="button"
                            onClick={() => setDeleteDraftTarget(v)}
                            className="p-1.5 rounded-xl bg-paper border border-rose-200 text-error hover:bg-pink-50 transition shadow-sm"
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
                            className="px-3.5 py-1.5 rounded-xl bg-paper text-error border border-rose-200 font-bold text-xs hover:bg-pink-50 transition active:scale-95"
                          >
                            Từ Chối
                          </button>

                          <button
                            type="button"
                            onClick={() => handleApproveVersion(v)}
                            className="px-3.5 py-1.5 rounded-xl bg-pink-600 text-white font-semibold text-xs shadow-xs hover:bg-pink-700 transition active:scale-[0.98] flex items-center gap-1.5"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            <span>Phê Duyệt (Approve)</span>
                          </button>
                        </div>
                      )}
                    </div>

                  </article>
                );
              })}
            </div>
          )}

          {/* REVIEW QUEUE PAGINATION */}
          {!isLoadingReview && reviewVersions.length > 0 && (
            <Pagination
              page={reviewPage}
              size={reviewPageSize}
              totalPages={reviewTotalPages}
              totalElements={reviewTotalElements}
              onPageChange={handleReviewPageChange}
              onSizeChange={handleReviewPageSizeChange}
              itemLabel="bản nháp trong hàng đợi"
            />
          )}

          {/* FLOATING BATCH ACTION BAR */}
          {selectedVersionIds.length > 0 && (
            <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-paper border border-pink-200 rounded-2xl shadow-md px-6 py-3.5 flex items-center gap-4 animate-in fade-in slide-in-from-bottom-4 duration-200">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-pulse" />
                <span className="text-xs sm:text-sm font-bold text-ink">
                  Đã chọn <span className="font-heading text-pink-900 bg-pink-100 px-2 py-0.5 rounded-lg border border-pink-400 font-extrabold">{selectedVersionIds.length}</span> câu hỏi
                </span>
              </div>

              <div className="h-6 w-0.5 bg-cream" />

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={isBatchApproving}
                  onClick={handleBatchApprove}
                  className="px-4 py-2 rounded-xl bg-green-400 hover:bg-green-500 text-ink font-heading font-bold text-xs border border-pink-200 shadow-sm active:scale-[0.98] transition flex items-center gap-2 cursor-pointer disabled:opacity-50"
                >
                  <CheckCircle2 className="w-4 h-4 text-green-900" />
                  <span>{isBatchApproving ? 'Đang duyệt...' : `Duyệt ${selectedVersionIds.length} câu đã chọn`}</span>
                </button>

                <button
                  type="button"
                  onClick={() => setSelectedVersionIds([])}
                  className="px-3 py-2 rounded-xl bg-cream hover:bg-pink-100 text-ink font-bold text-xs border border-pink-200 shadow-sm transition active:scale-[0.98] cursor-pointer"
                >
                  Bỏ chọn
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* QUESTION BANK MODALS (MODULARIZED ARCHITECTURE)                          */}
      {/* ========================================================================= */}
      <CreateTopicModal
        isOpen={isCreateTopicModalOpen}
        onClose={() => setIsCreateTopicModalOpen(false)}
        onSubmit={handleCreateTopic}
      />

      <AiQuestionGeneratorModal
        isOpen={isAiGenModalOpen}
        onClose={() => { setIsAiGenModalOpen(false); setFailedGenRequest(null); }}
        topics={topics}
        documents={documents}
        isLoadingDocs={isLoadingDocs}
        loadDocuments={loadDocuments}
        onTriggerGeneration={handleTriggerAiGeneration}
        isGeneratingAi={isGeneratingAi}
        failedGenRequest={failedGenRequest}
        onRetryGeneration={handleRetryGeneration}
        onSwitchToDocsTab={() => {
          setIsAiGenModalOpen(false);
          setActiveTab('documents');
        }}
      />

      <ManualQuestionModal
        isOpen={isManualQuestionModalOpen}
        onClose={() => setIsManualQuestionModalOpen(false)}
        topics={topics}
        onSubmit={handleCreateManualQuestion}
        isSubmitting={isSubmittingQuestion}
      />

      <TextImportModal
        isOpen={!!textImport}
        onClose={() => setTextImport(null)}
        textImport={textImport}
        setTextImport={setTextImport}
        topics={topics}
        rubricTemplates={rubricTemplates}
        onSubmit={handleImportText}
        isImportingText={isImportingText}
      />

      <ImportQuestionsModal
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        onImport={handleImportQuestions}
        isImporting={isImporting}
        onDownloadTemplate={handleDownloadTemplate}
        importReport={importReport}
      />

      <RejectVersionModal
        target={rejectVersionTarget}
        onClose={() => setRejectVersionTarget(null)}
        onConfirm={handleConfirmReject}
      />

      <RegenerateQuestionModal
        target={regenVersionTarget}
        onClose={() => setRegenVersionTarget(null)}
        onConfirm={handleConfirmRegenerate}
      />

      <EditDraftModal
        editDraft={editDraft}
        setEditDraft={setEditDraft}
        onSave={handleSaveDraft}
        isSaving={isSavingDraft}
      />

      <QuestionDetailModal
        questionDetail={questionDetail}
        onClose={() => setQuestionDetail(null)}
      />

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

    </div>
  );
}
