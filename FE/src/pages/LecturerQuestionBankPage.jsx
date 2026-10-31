import React, { useState, useEffect, useMemo } from 'react';
import {
  Database,
  FileText,
  Sparkles,
  Upload,
  Download,
  Plus,
  ChevronsUpDown,
  FolderPlus,
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

import {
  BLOOM_PRESETS,
  calculateBloomDistribution,
  validateCriteria,
  toRubricPayload,
  CreateTopicModal,
  AiQuestionGeneratorModal,
  ManualQuestionModal,
  TextImportModal,
  ImportQuestionsModal,
  RejectVersionModal,
  RegenerateQuestionModal,
  EditDraftModal,
  QuestionDetailModal,
} from '../components/question-bank';
import { DocumentsTab } from '../components/question-bank/tabs/DocumentsTab';
import { BankTab } from '../components/question-bank/tabs/BankTab';
import { ReviewTab } from '../components/question-bank/tabs/ReviewTab';

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
      const updated = await questionBankApi.confirmBloom(version.versionId, {
        bloomLevel: version.bloomLevel,
        expectedVersion: version.lockVersion
      });
      // Confirming bumps the lock token; keep it so the next approve/submit isn't rejected as stale
      setReviewVersions(prev => prev.map(v => v.versionId === version.versionId
        ? { ...v, bloomConfirmed: true, lockVersion: updated?.lockVersion ?? v.lockVersion }
        : v));
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

  // Submit Draft for peer review (DRAFT → UNDER_REVIEW)
  const handleSubmitForReview = async (version) => {
    try {
      await questionBankApi.submitForReview(version.questionId, version.versionId, version.lockVersion);
      showToast({ type: 'success', message: 'Đã gửi duyệt. Câu hỏi chuyển sang trạng thái chờ duyệt (UNDER_REVIEW).' });
      loadReviewQueue();
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
        <DocumentsTab
          docFileToUpload={docFileToUpload}
          documents={documents}
          handleRetryIndexing={handleRetryIndexing}
          handleUploadDocument={handleUploadDocument}
          isLoadingDocs={isLoadingDocs}
          isUploadingDoc={isUploadingDoc}
          loadDocuments={loadDocuments}
          setDeleteDocTarget={setDeleteDocTarget}
          setDocFileToUpload={setDocFileToUpload}
        />
      )}

      {/* ========================================================================= */}
      {/* TAB 2: NGÂN HÀNG CÂU HỎI CHÍNH THỨC (OFFICIAL BANK)                      */}
      {/* ========================================================================= */}
      {activeTab === 'bank' && (
        <BankTab
          bankPage={bankPage}
          bankPageSize={bankPageSize}
          bankQuestions={bankQuestions}
          bankTotalElements={bankTotalElements}
          bankTotalPages={bankTotalPages}
          expandedExpectedAnswers={expandedExpectedAnswers}
          filterBloom={filterBloom}
          filterTopicId={filterTopicId}
          handleBankPageChange={handleBankPageChange}
          handleBankPageSizeChange={handleBankPageSizeChange}
          handleCreateDraftFromApproved={handleCreateDraftFromApproved}
          handleOpenQuestionDetail={handleOpenQuestionDetail}
          isLoadingBank={isLoadingBank}
          loadBank={loadBank}
          openTextImport={openTextImport}
          searchBankKeyword={searchBankKeyword}
          selectedSubjectId={selectedSubjectId}
          setArchiveQuestionTarget={setArchiveQuestionTarget}
          setBankPage={setBankPage}
          setFilterBloom={setFilterBloom}
          setFilterTopicId={setFilterTopicId}
          setIsImportModalOpen={setIsImportModalOpen}
          setIsManualQuestionModalOpen={setIsManualQuestionModalOpen}
          setSearchBankKeyword={setSearchBankKeyword}
          toggleExpectedAnswer={toggleExpectedAnswer}
          topics={topics}
        />
      )}

      {/* ========================================================================= */}
      {/* TAB 3: HÀNG ĐỢI THẨM ĐỊNH & AI RAG (REVIEW QUEUE)                          */}
      {/* ========================================================================= */}
      {activeTab === 'review' && (
        <ReviewTab
          handleApproveVersion={handleApproveVersion}
          handleBatchApprove={handleBatchApprove}
          handleConfirmBloom={handleConfirmBloom}
          handleOpenEditDraft={handleOpenEditDraft}
          handleSubmitForReview={handleSubmitForReview}
          handleReviewPageChange={handleReviewPageChange}
          handleReviewPageSizeChange={handleReviewPageSizeChange}
          isBatchApproving={isBatchApproving}
          isLoadingReview={isLoadingReview}
          loadReviewQueue={loadReviewQueue}
          reviewPage={reviewPage}
          reviewPageSize={reviewPageSize}
          reviewStatusFilter={reviewStatusFilter}
          reviewTotalElements={reviewTotalElements}
          reviewTotalPages={reviewTotalPages}
          reviewVersions={reviewVersions}
          selectedVersionIds={selectedVersionIds}
          setDeleteDraftTarget={setDeleteDraftTarget}
          setIsAiGenModalOpen={setIsAiGenModalOpen}
          setRegenFeedback={setRegenFeedback}
          setRegenVersionTarget={setRegenVersionTarget}
          setRejectReason={setRejectReason}
          setRejectVersionTarget={setRejectVersionTarget}
          setReviewPage={setReviewPage}
          setReviewStatusFilter={setReviewStatusFilter}
          setSelectedVersionIds={setSelectedVersionIds}
        />
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
        feedback={regenFeedback}
        setFeedback={setRegenFeedback}
        // Called with the click event: don't let it pass as targetOverride
        onConfirm={() => handleConfirmRegenerate()}
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
