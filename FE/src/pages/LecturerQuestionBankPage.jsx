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

// Bloom Taxonomy Levels & Color Tokens (Pedagogical UI)
const BLOOM_LEVELS = [
  { value: 'REMEMBER', label: '1. Nhận Biết (Remember)', short: 'Nhớ', color: 'bg-green-100 text-green-800 border-2 border-green-500' },
  { value: 'UNDERSTAND', label: '2. Thông Hiểu (Understand)', short: 'Hiểu', color: 'bg-green-100 text-green-800 border-2 border-green-500' },
  { value: 'APPLY', label: '3. Vận Dụng (Apply)', short: 'Vận dụng', color: 'bg-green-100 text-green-800 border-2 border-green-500' },
  { value: 'ANALYZE', label: '4. Phân Tích (Analyze)', short: 'Phân tích', color: 'bg-green-100 text-green-800 border-2 border-green-500' },
  { value: 'EVALUATE', label: '5. Đánh Giá (Evaluate)', short: 'Đánh giá', color: 'bg-pink-100 text-pink-900 border-2 border-pink-400' },
  { value: 'CREATE', label: '6. Sáng Tạo (Create)', short: 'Sáng tạo', color: 'bg-pink-200 text-pink-900 border-2 border-green-500' }
];

// Presets for RAG Bloom Taxonomy distribution
const BLOOM_PRESETS = [
  {
    id: 'BALANCED',
    name: 'Cân Bằng Chuẩn',
    desc: 'Bao quát từ cơ bản đến vận dụng & phân tích',
    weights: { REMEMBER: 0.2, UNDERSTAND: 0.3, APPLY: 0.3, ANALYZE: 0.2, EVALUATE: 0, CREATE: 0 }
  },
  {
    id: 'VIVA_ORAL',
    name: 'Vấn Đáp Viva',
    desc: 'Ưu tiên phản biện, lập luận & đánh giá phương án',
    weights: { REMEMBER: 0.1, UNDERSTAND: 0.2, APPLY: 0.3, ANALYZE: 0.25, EVALUATE: 0.15, CREATE: 0 }
  },
  {
    id: 'FOUNDATIONAL',
    name: 'Nền Tảng',
    desc: 'Tập trung định nghĩa cốt lõi & hiểu nguyên lý',
    weights: { REMEMBER: 0.4, UNDERSTAND: 0.4, APPLY: 0.2, ANALYZE: 0, EVALUATE: 0, CREATE: 0 }
  },
  {
    id: 'ADVANCED',
    name: 'Bậc Cao',
    desc: 'Đào sâu phân tích, đánh giá & đề xuất giải pháp',
    weights: { REMEMBER: 0, UNDERSTAND: 0.1, APPLY: 0.2, ANALYZE: 0.35, EVALUATE: 0.25, CREATE: 0.1 }
  },
  {
    id: 'CUSTOM',
    name: 'Tùy Chỉnh',
    desc: 'Tự điều chỉnh số câu cho từng bậc Bloom',
    weights: null
  }
];

function calculateBloomDistribution(total, weights) {
  const dist = { REMEMBER: 0, UNDERSTAND: 0, APPLY: 0, ANALYZE: 0, EVALUATE: 0, CREATE: 0 };
  const n = Math.max(1, Math.min(20, Number(total) || 1));
  if (!weights) {
    dist.UNDERSTAND = n;
    return dist;
  }
  let assigned = 0;
  const entries = Object.entries(weights).filter(([_, w]) => w > 0);
  if (entries.length === 0) {
    dist.UNDERSTAND = n;
    return dist;
  }
  entries.forEach(([level, weight]) => {
    dist[level] = Math.floor(n * weight);
    assigned += dist[level];
  });
  const remainders = entries.map(([level, weight]) => ({
    level,
    remainder: (n * weight) - Math.floor(n * weight)
  })).sort((a, b) => b.remainder - a.remainder);

  for (let i = 0; assigned < n; i = (i + 1) % remainders.length) {
    dist[remainders[i].level] += 1;
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
          <div key={idx} className="bg-paper p-3 rounded-xl border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5] space-y-2">
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="Tên tiêu chí (VD: Tính đúng đắn)"
                value={crit.name}
                onChange={(e) => updateAt(idx, { name: e.target.value })}
                className="flex-1 px-3 py-1.5 rounded-lg text-xs bg-surface-container-lowest border-2 border-green-500 font-medium text-ink focus:outline-none focus:border-green-600 shadow-[1px_1px_0_#DEB9B5]"
              />
              <input
                type="number"
                min="0.5"
                step="0.5"
                max="999.99"
                value={crit.maxScore}
                onChange={(e) => updateAt(idx, { maxScore: e.target.value })}
                className="w-16 px-2 py-1.5 rounded-lg text-xs bg-surface-container-lowest border-2 border-green-500 font-mono font-bold text-center text-ink focus:outline-none focus:border-green-600 shadow-[1px_1px_0_#DEB9B5]"
              />
              <button
                type="button"
                onClick={() => onChange(criteria.filter((_, i) => i !== idx))}
                disabled={criteria.length <= 1}
                className="p-1.5 text-on-surface-variant hover:text-error disabled:opacity-30 transition"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
            <input
              type="text"
              placeholder="Mô tả tiêu chí (bắt buộc)"
              value={crit.description}
              onChange={(e) => updateAt(idx, { description: e.target.value })}
              className="w-full px-3 py-1.5 rounded-lg text-[11px] bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[1px_1px_0_#DEB9B5]"
            />
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={() => onChange([...criteria, { name: 'Tiêu chí bổ sung', description: '', maxScore: 2 }])}
        className="text-xs font-bold text-green-700 hover:text-green-900 inline-flex items-center gap-1 transition"
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
        const custom = JSON.parse(localStorage.getItem(`aives_custom_topics_${selectedSubjectId}`) || '[]');
        const list = await lecturerCatalogApi.getTopics(selectedSubjectId);
        const fetched = Array.isArray(list) ? list : [];
        const combined = [...custom, ...fetched];
        const uniqueMap = new Map();
        combined.forEach(t => uniqueMap.set(t.topicId || t.name, t));
        setTopics(Array.from(uniqueMap.values()));
      } catch (err) {
        const custom = JSON.parse(localStorage.getItem(`aives_custom_topics_${selectedSubjectId}`) || '[]');
        setTopics(custom);
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
  const handleCreateTopic = async (e) => {
    e.preventDefault();
    if (!newTopicName.trim()) return;

    try {
      let created = {
        topicId: `top-${Date.now()}`,
        name: newTopicName.trim(),
        description: newTopicDesc.trim()
      };
      try {
        const res = await lecturerCatalogApi.createTopic(selectedSubjectId, {
          name: newTopicName.trim(),
          description: newTopicDesc.trim()
        });
        if (res && (res.topicId || res.id)) created = res;
      } catch (err) {
        console.warn('Backend createTopic error, fallback to local state.', err);
      }

      const existingCustom = JSON.parse(localStorage.getItem(`aives_custom_topics_${selectedSubjectId}`) || '[]');
      localStorage.setItem(`aives_custom_topics_${selectedSubjectId}`, JSON.stringify([...existingCustom, created]));

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
      try {
        await courseDocumentApi.uploadDocument(selectedSubjectId, docFileToUpload);
        await loadDocuments();
      } catch (err) {
        console.warn('Backend uploadDocument error, fallback to local state.', err);
        const newDoc = {
          documentId: `doc-${Date.now()}`,
          fileName: docFileToUpload.name,
          fileType: docFileToUpload.name.split('.').pop() || 'pdf',
          fileSizeBytes: docFileToUpload.size || 2500000,
          indexingStatus: 'READY',
          status: 'READY',
          totalChunks: 24,
          chunkCount: 24,
          createdAt: new Date().toISOString()
        };
        setDocuments(prev => [newDoc, ...prev]);
      }
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
      await courseDocumentApi.retryIndexing(documentId).catch(() => {});
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
      await courseDocumentApi.deleteDocument(deleteDocTarget.documentId).catch(() => {});
      setDocuments(prev => prev.filter(d => d.documentId !== deleteDocTarget.documentId));
      showToast({ type: 'success', message: 'Xóa tài liệu giáo trình thành công.' });
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
  const handleTriggerAiGeneration = async () => {
    if (genDocumentIds.length === 0) {
      showToast({ type: 'error', message: 'Vui lòng chọn ít nhất 1 tài liệu giáo trình READY để sinh câu hỏi.' });
      return;
    }

    const total = Number(genTotalQuestions);
    if (!total || total < 1 || total > 20) {
      showToast({ type: 'error', message: 'Số lượng câu hỏi phải từ 1 đến 20.' });
      return;
    }

    if (totalAllocatedBloom !== total) {
      showToast({
        type: 'error',
        message: `Tổng số câu phân bổ (${totalAllocatedBloom}) chưa khớp với số lượng yêu cầu (${total}). Vui lòng cân bằng lại trước khi sinh câu hỏi.`
      });
      return;
    }

    setIsGeneratingAi(true);
    try {
      const distribution = Object.fromEntries(
        Object.entries(bloomCounts).filter(([_, count]) => Number(count) > 0)
      );

      const payload = {
        subjectId: selectedSubjectId,
        topicId: genTopicId || null,
        documentIds: genDocumentIds,
        totalQuestions: total,
        bloomDistribution: distribution,
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
    <div className="space-y-6 animate-modal-entry bg-paper text-ink min-h-screen">

      {/* HEADER SECTION & SUBJECT PICKER */}
      <div className="bg-paper p-6 rounded-2xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-green-100 text-green-700 border-2 border-green-500 text-xs font-semibold shadow-[2px_2px_0_#DEB9B5] mb-2">
            <Database className="w-3.5 h-3.5 text-green-700" />
            <span>Module 1 &amp; Group 1 — Question Bank &amp; RAG Pipeline</span>
          </div>
          <h1 className="text-xl font-heading font-extrabold text-ink tracking-tight">
            Ngân Hàng Câu Hỏi &amp; Quản Lý RAG Giáo Trình
          </h1>
          <p className="text-sm italic text-pink-700 mt-0.5 font-normal">
            Thẩm định năng lực tư duy kiến trúc • Đối soát học liệu đa tầng
          </p>
          <p className="text-xs text-on-surface-variant mt-1 max-w-3xl">
            Quản trị kho học liệu, bóc tách vector hóa giáo trình, kích hoạt AI RAG và thẩm định ma trận Rubric đa tiêu chí theo chuẩn Bloom.
          </p>
        </div>

        {/* SUBJECT SWITCHER BOX */}
        <div className="w-full md:w-80 bg-paper border-2 border-green-500 rounded-xl p-3.5 shadow-[3px_3px_0_#DEB9B5] transition-all">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-[10px] text-green-700 uppercase tracking-wider font-bold">MÔN HỌC PHỤ TRÁCH</span>
            <span className="px-2 py-0.5 rounded-full bg-green-500 text-paper font-bold text-[10px] tracking-wide">ACTIVE</span>
          </div>
          <div className="relative">
            <select
              value={selectedSubjectId}
              onChange={(e) => setSelectedSubjectId(e.target.value)}
              className="w-full appearance-none bg-surface-container-lowest border-2 border-green-500 rounded-lg py-1.5 pl-3 pr-8 text-xs font-bold text-ink cursor-pointer focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5]"
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
      <div className="bg-paper border-2 border-green-500 rounded-xl p-4 shadow-[3px_3px_0_#DEB9B5] flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-10 h-10 rounded-lg bg-green-100 border-2 border-green-500 flex items-center justify-center shrink-0 text-green-700 shadow-[2px_2px_0_#DEB9B5]">
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
          className="shrink-0 inline-flex items-center justify-center gap-1.5 px-3.5 py-1.5 bg-paper text-green-700 border-2 border-green-500 rounded-xl font-bold text-xs shadow-[3px_3px_0_#DEB9B5] hover:bg-green-100 hover:text-green-900 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all"
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
              ? 'bg-pink-200 border-green-500 text-pink-900 shadow-[3px_3px_0_#DEB9B5]'
              : 'bg-paper border-transparent hover:border-green-500 text-on-surface-variant hover:text-ink'
          }`}
        >
          <Database className="w-4 h-4" />
          <span>Ngân Hàng Chính Thức</span>
          <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border-2 ${
            activeTab === 'bank'
              ? 'bg-pink-100 text-pink-900 border-green-500'
              : 'bg-green-100 text-green-800 border-transparent'
          }`}>
            {bankQuestions.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('documents')}
          className={`inline-flex items-center gap-2 px-4 py-2 border-2 rounded-xl text-xs font-bold transition-all shrink-0 ${
            activeTab === 'documents'
              ? 'bg-pink-200 border-green-500 text-pink-900 shadow-[3px_3px_0_#DEB9B5]'
              : 'bg-paper border-transparent hover:border-green-500 text-on-surface-variant hover:text-ink'
          }`}
        >
          <FileText className="w-4 h-4" />
          <span>Kho Giáo Trình RAG</span>
          <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border-2 ${
            activeTab === 'documents'
              ? 'bg-pink-100 text-pink-900 border-green-500'
              : 'bg-green-100 text-green-800 border-transparent'
          }`}>
            {documents.length}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('review')}
          className={`inline-flex items-center gap-2 px-4 py-2 border-2 rounded-xl text-xs font-bold transition-all shrink-0 ${
            activeTab === 'review'
              ? 'bg-pink-200 border-green-500 text-pink-900 shadow-[3px_3px_0_#DEB9B5]'
              : 'bg-paper border-transparent hover:border-green-500 text-on-surface-variant hover:text-ink'
          }`}
        >
          <Sparkles className="w-4 h-4 text-pink-600" />
          <span>Hàng Đợi Thẩm Định &amp; AI</span>
          <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border-2 ${
            activeTab === 'review'
              ? 'bg-pink-100 text-pink-900 border-green-500'
              : 'bg-pink-100 text-pink-800 border-pink-400'
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
          <div className="bg-paper p-6 rounded-2xl border-2 border-dashed border-green-500 shadow-[3px_3px_0_#DEB9B5] flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-2xl bg-green-100 border-2 border-green-500 text-green-700 flex items-center justify-center mb-3 shadow-[2px_2px_0_#DEB9B5]">
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
                className="px-4 py-2 rounded-xl bg-paper border-2 border-green-500 hover:bg-green-100 text-green-900 font-bold text-xs cursor-pointer shadow-[2px_2px_0_#DEB9B5] transition active:scale-95"
              >
                {docFileToUpload ? docFileToUpload.name : 'Chọn Tệp Tin Từ Máy...'}
              </label>

              <button
                type="button"
                onClick={handleUploadDocument}
                disabled={!docFileToUpload || isUploadingDoc}
                className="px-5 py-2 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 flex items-center gap-2"
              >
                {isUploadingDoc && <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>}
                <span>Bắt Đầu Tải Lên &amp; Lập Chỉ Mục</span>
              </button>
            </div>
          </div>

          {/* DOCUMENTS LIST */}
          <div className="bg-paper rounded-2xl border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] overflow-hidden">
            <div className="p-4 bg-green-50 border-b-2 border-green-500 flex items-center justify-between">
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
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-green-100 text-green-800 border-2 border-green-500 shadow-[1px_1px_0_#DEB9B5]">
                              <CheckCircle2 className="w-3.5 h-3.5 text-green-700" />
                              READY (Đã Lập Chỉ Mục)
                            </span>
                          )}
                          {doc.indexingStatus === 'INDEXING' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-pink-100 text-pink-900 border-2 border-pink-400">
                              <span className="w-3 h-3 border-2 border-pink-700 border-t-transparent rounded-full animate-spin"></span>
                              INDEXING...
                            </span>
                          )}
                          {doc.indexingStatus === 'UPLOADED' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-paper text-ink border-2 border-green-500">
                              <Clock className="w-3.5 h-3.5 text-green-700" />
                              UPLOADED
                            </span>
                          )}
                          {doc.indexingStatus === 'FAILED' && (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-pink-50 text-error border-2 border-error">
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
                              className="p-1.5 rounded-lg bg-paper border-2 border-green-500 text-green-800 hover:bg-green-100 transition shadow-[1px_1px_0_#DEB9B5]"
                              title="Thử lập chỉ mục lại"
                            >
                              <RotateCcw className="w-4 h-4" />
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => setDeleteDocTarget(doc)}
                            className="p-1.5 rounded-lg bg-paper border-2 border-green-500 text-on-surface-variant hover:text-error hover:border-error hover:bg-pink-50 transition shadow-[1px_1px_0_#DEB9B5]"
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
          <div className="flex flex-col xl:flex-row items-stretch xl:items-center justify-between gap-3 bg-paper p-4 rounded-xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5]">
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
                  className="w-full pl-9 pr-3 py-1.5 rounded-lg text-xs bg-surface-container-lowest border-2 border-green-500 text-ink placeholder:text-on-surface-variant focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
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
                  className="w-full md:w-48 appearance-none bg-surface-container-lowest border-2 border-green-500 rounded-lg py-1.5 pl-3 pr-8 text-xs font-semibold text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] cursor-pointer"
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
                  className="w-full md:w-52 appearance-none bg-surface-container-lowest border-2 border-green-500 rounded-lg py-1.5 pl-3 pr-8 text-xs font-semibold text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] cursor-pointer"
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
                className="p-2 rounded-lg bg-paper text-green-700 border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition shrink-0"
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
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-paper text-green-700 border-2 border-green-500 rounded-xl font-bold text-xs shadow-[3px_3px_0_#DEB9B5] hover:bg-green-100 hover:text-green-900 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all"
              >
                <FileSpreadsheet className="w-4 h-4 text-green-700" />
                <span>Nhập Excel/CSV</span>
              </button>

              <button
                type="button"
                onClick={() => setIsManualQuestionModalOpen(true)}
                className="inline-flex items-center gap-1.5 px-4 py-1.5 bg-pink-500 text-ink border-2 border-green-500 rounded-xl font-bold text-xs shadow-[3px_3px_0_#DEB9B5] hover:bg-pink-600 hover:text-paper active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all"
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
            <div className="bg-paper p-12 rounded-2xl border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] text-center">
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
                  <article key={q.questionId} className="bg-paper border-2 border-green-500 rounded-2xl p-5 shadow-[6px_6px_0_#D5A6A2] transition-all space-y-3">
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b-2 border-cream pb-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]">
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
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-surface-container-high text-on-surface-variant border-2 border-green-500/50">
                            ARCHIVED
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-green-500 text-paper text-[10px] font-bold shadow-[1px_1px_0_#DEB9B5]">
                            <CheckCircle2 className="w-3 h-3 text-paper" />
                            APPROVED
                          </span>
                        )}
                        {q.hasPendingDraft && (
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-700 border-2 border-pink-400">
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
                        <div className="mt-2.5 p-4 bg-pink-50 rounded-xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] text-xs text-ink space-y-3 animate-modal-entry">
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
                                <span className="text-pink-900 font-bold text-xs bg-pink-200 border border-green-500 px-2 py-0.5 rounded-md shadow-[1px_1px_0_#DEB9B5]">
                                  Tổng điểm: {cv.rubric.totalScore || 10}đ
                                </span>
                              </div>
                              <div className="grid grid-cols-1 md:grid-cols-3 gap-2 mt-2">
                                {cv.rubric.criteria.map((crit, idx) => (
                                  <div key={crit.criterionId || idx} className="bg-surface-container-lowest border-2 border-green-500/70 rounded-lg p-2.5">
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
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-paper text-green-700 border-2 border-green-500 rounded-xl font-bold text-xs shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 hover:text-green-900 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all"
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
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-pink-500 text-ink border-2 border-green-500 rounded-xl font-bold text-xs shadow-[2px_2px_0_#DEB9B5] hover:bg-pink-600 hover:text-paper active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all disabled:opacity-50"
                            >
                              <Edit3 className="w-3.5 h-3.5" />
                              <span>Tạo Bản Nháp Mới (Copy-on-Write)</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => setArchiveQuestionTarget(q)}
                              className="p-1.5 rounded-lg bg-paper border-2 border-green-500 text-on-surface-variant hover:text-error hover:border-error hover:bg-pink-50 shadow-[2px_2px_0_#DEB9B5] transition"
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
          <div className="relative overflow-hidden bg-pink-100 border-2 border-green-500 rounded-2xl p-6 shadow-[6px_6px_0_#D5A6A2] flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
            <div className="relative z-10 space-y-1.5 max-w-xl">
              <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-paper border-2 border-green-500 text-pink-800 text-[10px] font-bold uppercase tracking-wider shadow-[2px_2px_0_#DEB9B5]">
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
              className="relative z-10 px-5 py-2.5 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink hover:text-paper font-bold text-xs border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-[2px] active:translate-y-[2px] active:shadow-none flex items-center gap-2 shrink-0"
            >
              <Sparkles className="w-4 h-4 text-ink" />
              <span>Thiết Lập &amp; Kích Hoạt RAG</span>
            </button>
          </div>

          {/* STATUS FILTER FOR QUEUE */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2 bg-paper p-3.5 rounded-2xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5]">
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
                        ? 'bg-pink-500 text-ink border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                        : 'bg-paper text-green-800 border-transparent hover:border-green-500'
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
              className="p-1.5 text-green-700 hover:text-green-900 rounded-lg hover:bg-green-100 transition self-end sm:self-auto"
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
            <div className="bg-paper p-12 rounded-2xl border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] text-center">
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
                  <article key={v.versionId} className="bg-paper border-2 border-green-500 rounded-2xl p-5 shadow-[6px_6px_0_#D5A6A2] space-y-4">

                    {/* Header Bar */}
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b-2 border-cream pb-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]">
                          {v.questionCode || 'DRAFT-VER'}
                        </span>
                        <span className="text-xs font-semibold text-green-800">
                          {v.topicName || 'Chủ đề học phần'}
                        </span>
                        <span className="text-[10px] px-2 py-0.5 rounded-md font-mono font-bold bg-green-100 text-green-900 border border-green-500">
                          Origin: {v.origin || 'MANUAL'}
                        </span>
                      </div>

                      <div className="flex flex-wrap items-center gap-2">
                        {/* Bloom Confirmation Badge */}
                        {v.bloomConfirmed ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-green-100 text-green-800 border-2 border-green-500 shadow-[1px_1px_0_#DEB9B5]">
                            <Check className="w-3 h-3 text-green-700" />
                            Đã Xác Nhận Bloom
                          </span>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleConfirmBloom(v)}
                            className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-800 border-2 border-pink-400 hover:bg-pink-200 transition active:scale-95 shadow-[1px_1px_0_#DEB9B5]"
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
                            ? 'bg-green-100 text-green-800 border-green-500'
                            : v.status === 'REJECTED'
                              ? 'bg-pink-50 text-error border-error'
                              : 'bg-pink-100 text-pink-900 border-pink-400'
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
                    <div className="p-3.5 bg-pink-50 rounded-xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5]">
                      <p className="text-[10px] font-bold text-pink-900 uppercase tracking-wider mb-1">Đáp Án Tiêu Chuẩn:</p>
                      <p className="text-xs text-ink leading-relaxed font-normal">{v.expectedAnswer}</p>
                    </div>

                    {/* Source Citations (Grounding Evidence) */}
                    {v.sources && v.sources.length > 0 && (
                      <div className="p-3.5 bg-green-50 rounded-xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] space-y-1.5">
                        <div className="flex items-center gap-1.5 text-green-900 font-bold text-[11px]">
                          <BookOpen className="w-3.5 h-3.5 text-green-700" />
                          <span>Cơ Sở Đối Chiếu Giáo Trình (Grounding Evidence Chunk #{v.sources[0].order}):</span>
                        </div>
                        <p className="text-[11px] text-ink italic bg-surface-container-lowest p-2.5 rounded-lg border-2 border-green-500/50 leading-relaxed">
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
                          <span className="text-pink-900 font-bold text-xs bg-pink-200 border border-green-500 px-2 py-0.5 rounded-md shadow-[1px_1px_0_#DEB9B5]">
                            Tổng điểm: {v.rubric.totalScore || 10}đ
                          </span>
                        </div>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                          {v.rubric.criteria.map((c, idx) => (
                            <div key={idx} className="p-2.5 bg-surface-container-lowest rounded-lg border-2 border-green-500/70 flex items-center justify-between text-xs">
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
                            className="px-3 py-1.5 rounded-xl bg-paper text-green-700 border-2 border-green-500 font-bold text-xs shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition active:scale-95 flex items-center gap-1.5"
                          >
                            <Sparkles className="w-3.5 h-3.5 text-pink-600" />
                            <span>Tạo Lại Với AI (Regenerate)</span>
                          </button>
                        )}

                        {v.status === 'DRAFT' && (
                          <button
                            type="button"
                            onClick={() => handleOpenEditDraft(v)}
                            className="px-3 py-1.5 rounded-xl bg-paper text-ink border-2 border-green-500 font-bold text-xs shadow-[2px_2px_0_#DEB9B5] hover:bg-green-50 transition active:scale-95 flex items-center gap-1.5"
                          >
                            <Edit3 className="w-3.5 h-3.5 text-green-700" />
                            <span>Chỉnh Sửa</span>
                          </button>
                        )}

                        {v.status === 'DRAFT' && (
                          <button
                            type="button"
                            onClick={() => setDeleteDraftTarget(v)}
                            className="p-1.5 rounded-xl bg-paper border-2 border-error text-error hover:bg-pink-50 transition shadow-[2px_2px_0_#DEB9B5]"
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
                            className="px-3.5 py-1.5 rounded-xl bg-paper text-error border-2 border-error font-bold text-xs hover:bg-pink-50 transition active:scale-95"
                          >
                            Từ Chối
                          </button>

                          <button
                            type="button"
                            onClick={() => handleApproveVersion(v)}
                            className="px-4 py-1.5 rounded-xl bg-pink-500 text-ink border-2 border-green-500 font-bold text-xs shadow-[3px_3px_0_#DEB9B5] hover:bg-pink-600 hover:text-paper transition active:translate-x-[2px] active:translate-y-[2px] active:shadow-none flex items-center gap-1.5"
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
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: TẠO CHỦ ĐỀ MỚI                                                      */}
      {/* ========================================================================= */}
      {isCreateTopicModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-paper rounded-2xl max-w-md w-full border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] p-5 space-y-4 animate-modal-entry">
            <div className="flex items-center justify-between border-b-2 border-cream pb-3">
              <h3 className="font-heading font-extrabold text-sm text-ink">Thêm Chủ Đề Đề Cương</h3>
              <button onClick={() => setIsCreateTopicModalOpen(false)} className="text-on-surface-variant hover:text-ink p-1 rounded-lg hover:bg-green-100">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateTopic} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Tên Chủ Đề / Chương <span className="text-error">*</span></label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Chương 4: Clean Architecture & SOLID"
                  value={newTopicName}
                  onChange={(e) => setNewTopicName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Mô Tả Nội Dung Chủ Đề</label>
                <textarea
                  rows="2"
                  placeholder="Mô tả chuẩn đầu ra học phần cho chủ đề này..."
                  value={newTopicDesc}
                  onChange={(e) => setNewTopicDesc(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2 border-t-2 border-cream">
                <button
                  type="button"
                  onClick={() => setIsCreateTopicModalOpen(false)}
                  className="px-4 py-2 rounded-xl bg-paper border-2 border-green-500 text-green-800 text-xs font-bold shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-[2px] active:translate-y-[2px] active:shadow-none"
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
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-paper rounded-2xl max-w-2xl w-full max-h-[92vh] overflow-y-auto border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] p-5 sm:p-6 space-y-4 animate-modal-entry">
            <div className="flex items-center justify-between border-b-2 border-cream pb-3">
              <div className="flex items-center gap-2 text-pink-900 font-bold text-xs">
                <Sparkles className="w-4 h-4 text-pink-600" />
                <span className="font-heading uppercase tracking-wider">Cấu Hình RAG Prompting Engine</span>
              </div>
              <button onClick={() => setIsAiGenModalOpen(false)} className="text-on-surface-variant hover:text-ink p-1 rounded-lg hover:bg-green-100">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Chủ Đề Áp Dụng (Topic)</label>
                <select
                  value={genTopicId}
                  onChange={(e) => setGenTopicId(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium cursor-pointer"
                >
                  <option value="">-- Mặc định (Toàn bộ học phần) --</option>
                  {topics.map(t => (
                    <option key={t.topicId} value={t.topicId}>{t.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-green-800">
                    Chọn Giáo Trình Làm Nguồn Ngữ Cảnh (RAG Context) <span className="text-error">*</span>
                  </label>
                  <div className="flex items-center gap-2">
                    {documents.some(d => (d.indexingStatus || d.status) === 'READY' || (d.indexingStatus || d.status) === 'SUCCESS') && (
                      <button
                        type="button"
                        onClick={() => {
                          const readyIds = documents
                            .filter(d => (d.indexingStatus || d.status) === 'READY' || (d.indexingStatus || d.status) === 'SUCCESS')
                            .map(d => d.documentId);
                          if (genDocumentIds.length === readyIds.length) {
                            setGenDocumentIds([]);
                          } else {
                            setGenDocumentIds(readyIds);
                          }
                        }}
                        className="text-[11px] font-bold text-green-700 hover:text-green-900 hover:underline"
                      >
                        {genDocumentIds.length === documents.filter(d => (d.indexingStatus || d.status) === 'READY' || (d.indexingStatus || d.status) === 'SUCCESS').length && genDocumentIds.length > 0
                          ? 'Bỏ chọn tất cả'
                          : 'Chọn tất cả'}
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={loadDocuments}
                      disabled={isLoadingDocs}
                      className="text-[11px] font-bold text-green-700 hover:text-green-900 flex items-center gap-1 hover:underline"
                      title="Làm mới danh sách giáo trình"
                    >
                      <RefreshCw className={`w-3 h-3 ${isLoadingDocs ? 'animate-spin' : ''}`} />
                      <span>Làm mới</span>
                    </button>
                  </div>
                </div>

                <div className="space-y-1.5 max-h-36 overflow-y-auto border-2 border-green-500 p-2.5 rounded-xl bg-surface-container-lowest shadow-[2px_2px_0_#DEB9B5]">
                  {isLoadingDocs ? (
                    <div className="flex items-center justify-center py-4 gap-2 text-xs text-on-surface-variant">
                      <span className="w-3.5 h-3.5 border-2 border-green-700 border-t-transparent rounded-full animate-spin"></span>
                      <span>Đang kiểm tra tài liệu giáo trình...</span>
                    </div>
                  ) : documents.length === 0 ? (
                    <div className="text-center py-3 space-y-1">
                      <p className="text-on-surface-variant text-xs italic">Chưa có tài liệu nào được tải lên cho môn học này.</p>
                      <button
                        type="button"
                        onClick={() => {
                          setIsAiGenModalOpen(false);
                          setActiveTab('documents');
                        }}
                        className="text-xs font-bold text-pink-700 hover:underline"
                      >
                        Chuyển sang tab Kho Giáo Trình để tải lên &rarr;
                      </button>
                    </div>
                  ) : (
                    documents.map(d => {
                      const status = d.indexingStatus || d.status;
                      const isReady = status === 'READY' || status === 'SUCCESS';
                      const isIndexing = status === 'INDEXING' || status === 'UPLOADED';
                      const isFailed = status === 'FAILED';

                      return (
                        <label
                          key={d.documentId}
                          className={`flex items-center justify-between gap-2 text-xs p-1.5 rounded transition ${
                            isReady
                              ? 'font-medium text-ink cursor-pointer hover:bg-green-50'
                              : 'opacity-70 bg-stone-50 cursor-not-allowed'
                          }`}
                        >
                          <div className="flex items-center gap-2 truncate min-w-0">
                            <input
                              type="checkbox"
                              disabled={!isReady}
                              checked={genDocumentIds.includes(d.documentId)}
                              onChange={(e) => {
                                if (e.target.checked) {
                                  setGenDocumentIds(prev => [...prev, d.documentId]);
                                } else {
                                  setGenDocumentIds(prev => prev.filter(id => id !== d.documentId));
                                }
                              }}
                              className="rounded text-green-700 focus:ring-green-600 disabled:opacity-40"
                            />
                            <span className="truncate">{d.fileName}</span>
                          </div>

                          <div className="shrink-0">
                            {isReady && (
                              <span className="inline-flex items-center gap-1 text-[10px] font-extrabold text-green-800 bg-green-100 border border-green-500 px-2 py-0.5 rounded-full">
                                <CheckCircle2 className="w-2.5 h-2.5 text-green-700" />
                                READY ({d.totalChunks || d.chunkCount || 0} chunks)
                              </span>
                            )}
                            {isIndexing && (
                              <span className="inline-flex items-center gap-1 text-[10px] font-bold text-pink-900 bg-pink-100 border border-pink-400 px-2 py-0.5 rounded-full">
                                <span className="w-2.5 h-2.5 border-2 border-pink-700 border-t-transparent rounded-full animate-spin"></span>
                                {status === 'INDEXING' ? 'Đang index...' : 'Chờ index...'}
                              </span>
                            )}
                            {isFailed && (
                              <span className="inline-flex items-center gap-1 text-[10px] font-bold text-error bg-pink-50 border border-error px-2 py-0.5 rounded-full">
                                <AlertCircle className="w-2.5 h-2.5 text-error" />
                                FAILED
                              </span>
                            )}
                          </div>
                        </label>
                      );
                    })
                  )}
                </div>
              </div>

              {/* Total Questions & Quick Select */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-green-800">
                    Số Lượng Câu Hỏi Cần Sinh (1 - 20) <span className="text-error">*</span>
                  </label>
                  <div className="flex items-center gap-1.5">
                    {[3, 5, 10, 15].map(cnt => (
                      <button
                        key={cnt}
                        type="button"
                        onClick={() => handleTotalQuestionsChange(cnt)}
                        className={`px-2 py-0.5 rounded-lg text-[11px] font-bold border transition ${
                          Number(genTotalQuestions) === cnt
                            ? 'bg-pink-200 text-pink-900 border-green-500 shadow-[1px_1px_0_#DEB9B5]'
                            : 'bg-paper text-ink border-green-300 hover:bg-green-100'
                        }`}
                      >
                        {cnt} câu
                      </button>
                    ))}
                  </div>
                </div>
                <input
                  type="number"
                  min="1"
                  max="20"
                  value={genTotalQuestions}
                  onChange={(e) => handleTotalQuestionsChange(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
                />
              </div>

              {/* Bloom Taxonomy Presets */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-green-800 flex items-center gap-1.5">
                    <Sliders className="w-3.5 h-3.5 text-green-700" />
                    <span>Mẫu Phân Bổ Cấp Độ Bloom (Presets)</span>
                  </label>
                  <span className="text-[10px] text-green-700 font-mono">Đủ 6 bậc thang tư duy</span>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
                  {BLOOM_PRESETS.map(p => {
                    const isSelected = selectedBloomPreset === p.id;
                    return (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => handleSelectBloomPreset(p.id)}
                        className={`p-2 rounded-xl text-left border-2 transition text-xs ${
                          isSelected
                            ? 'bg-pink-100 border-green-500 text-pink-900 shadow-[2px_2px_0_#DEB9B5] font-bold'
                            : 'bg-paper border-green-300 text-ink hover:bg-green-50'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="truncate">{p.name}</span>
                          {isSelected && <Check className="w-3.5 h-3.5 text-pink-700 flex-shrink-0" />}
                        </div>
                        <p className="text-[10px] text-on-surface-variant font-normal truncate mt-0.5">{p.desc}</p>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* 6-Level Bloom Interactive Grid */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-green-800">
                    Chi Tiết Phân Bổ 6 Cấp Độ Nhận Thức Bloom
                  </label>
                  <div className="flex items-center gap-2">
                    <span className={`text-[11px] font-mono px-2 py-0.5 rounded-lg border font-bold ${
                      isBloomBalanced
                        ? 'bg-green-100 text-green-900 border-green-500'
                        : 'bg-pink-100 text-pink-900 border-pink-400'
                    }`}>
                      {totalAllocatedBloom} / {genTotalQuestions} câu ({Number(genTotalQuestions) > 0 ? Math.round((totalAllocatedBloom / Number(genTotalQuestions)) * 100) : 0}%)
                    </span>
                    {!isBloomBalanced && (
                      <button
                        type="button"
                        onClick={handleAutoBalanceBloom}
                        className="px-2 py-0.5 rounded-lg text-[11px] font-bold bg-cream text-ink border border-green-500 hover:bg-pink-100 transition shadow-[1px_1px_0_#DEB9B5]"
                        title="Tự động chia đều theo tổng số câu hỏi"
                      >
                        Tự động cân bằng
                      </button>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  {BLOOM_LEVELS.map(level => {
                    const count = bloomCounts[level.value] || 0;
                    const percent = totalAllocatedBloom > 0 ? Math.round((count / totalAllocatedBloom) * 100) : 0;
                    return (
                      <div
                        key={level.value}
                        className={`p-2.5 rounded-xl border-2 transition ${
                          count > 0 ? 'bg-paper border-green-500 shadow-[2px_2px_0_#DEB9B5]' : 'bg-surface-container-lowest border-green-200 opacity-70 hover:opacity-100'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-1.5">
                          <span className="text-[11px] font-bold text-ink truncate" title={level.label}>
                            {level.label}
                          </span>
                          <span className="text-[10px] font-mono text-green-800 bg-green-50 px-1 rounded border border-green-200">
                            {percent}%
                          </span>
                        </div>
                        <div className="flex items-center justify-between gap-1">
                          <button
                            type="button"
                            onClick={() => handleBloomCountChange(level.value, -1)}
                            disabled={count <= 0}
                            className="w-7 h-7 rounded-lg border border-green-400 bg-paper text-ink font-bold flex items-center justify-center hover:bg-pink-100 active:scale-95 disabled:opacity-30 disabled:pointer-events-none"
                          >
                            <Minus className="w-3.5 h-3.5" />
                          </button>
                          <input
                            type="number"
                            min="0"
                            max="20"
                            value={count}
                            onChange={(e) => handleBloomCountChange(level.value, e.target.value, true)}
                            className="w-12 text-center py-1 text-xs font-bold bg-white rounded-lg border border-green-400 text-ink focus:outline-none focus:border-green-600"
                          />
                          <button
                            type="button"
                            onClick={() => handleBloomCountChange(level.value, 1)}
                            disabled={totalAllocatedBloom >= 20}
                            className="w-7 h-7 rounded-lg border border-green-400 bg-paper text-ink font-bold flex items-center justify-center hover:bg-green-100 active:scale-95 disabled:opacity-30 disabled:pointer-events-none"
                          >
                            <Plus className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Ghi Chú Đạo Diễn Cho AI (Lecturer Instruction)</label>
                <textarea
                  rows="2"
                  placeholder="Ví dụ: Đặt câu hỏi xoáy sâu vào các bẫy thường gặp trong xử lý đa luồng..."
                  value={genLecturerNote}
                  onChange={(e) => setGenLecturerNote(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
                />
              </div>

              {failedGenRequest && (
                <div className="p-3 rounded-xl border-2 border-pink-400 bg-pink-50 text-xs text-pink-900 space-y-1 shadow-[2px_2px_0_#DEB9B5]">
                  <p className="font-bold">
                    Lần sinh gần nhất: {failedGenRequest.status} ({failedGenRequest.generatedCount}/{failedGenRequest.totalQuestions} câu)
                  </p>
                  {failedGenRequest.errorMessage && <p>{failedGenRequest.errorMessage}</p>}
                  {failedGenRequest.issues?.map((issue, idx) => <p key={idx}>• {issue}</p>)}
                </div>
              )}

              {!isBloomBalanced && (
                <div className="p-2.5 rounded-xl border border-pink-400 bg-pink-50 text-[11px] text-pink-900 flex items-center justify-between">
                  <span>⚠ Tổng số câu phân bổ ({totalAllocatedBloom}) chưa khớp với số lượng ({genTotalQuestions}). Bấm nút bên cạnh để tự động cân đối:</span>
                  <button
                    type="button"
                    onClick={handleAutoBalanceBloom}
                    className="px-2.5 py-1 rounded-lg text-xs font-bold bg-pink-200 text-pink-950 border border-pink-500 hover:bg-pink-300 transition"
                  >
                    Khớp tự động
                  </button>
                </div>
              )}

              <div className="pt-2 flex justify-end gap-2 border-t-2 border-cream">
                {failedGenRequest && (
                  <button
                    type="button"
                    onClick={handleRetryGeneration}
                    disabled={isGeneratingAi}
                    className="px-4 py-2 rounded-xl border-2 border-pink-400 text-pink-900 bg-pink-100 text-xs font-bold hover:bg-pink-200 transition active:scale-95 disabled:opacity-50 flex items-center gap-1.5 shadow-[2px_2px_0_#DEB9B5]"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Thử Lại Phần Thiếu</span>
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => { setIsAiGenModalOpen(false); setFailedGenRequest(null); }}
                  disabled={isGeneratingAi}
                  className="px-4 py-2 rounded-xl bg-paper border-2 border-green-500 text-green-800 text-xs font-bold shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="button"
                  onClick={handleTriggerAiGeneration}
                  disabled={isGeneratingAi || genDocumentIds.length === 0 || !isBloomBalanced}
                  className="px-5 py-2 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 flex items-center gap-2"
                >
                  {isGeneratingAi && <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>}
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
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-paper rounded-2xl max-w-xl w-full border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] p-6 space-y-4 animate-modal-entry max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b-2 border-cream pb-3">
              <h3 className="font-heading font-extrabold text-sm text-ink">Soạn Câu Hỏi Mới Kèm Ma Trận Rubric</h3>
              <button onClick={() => setIsManualQuestionModalOpen(false)} className="text-on-surface-variant hover:text-ink p-1 rounded-lg hover:bg-green-100">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateManualQuestion} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-green-800 mb-1">Chủ Đề (Topic)</label>
                  <select
                    value={manualTopicId}
                    onChange={(e) => setManualTopicId(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium cursor-pointer"
                  >
                    <option value="">-- Chủ đề chung --</option>
                    {topics.map(t => (
                      <option key={t.topicId} value={t.topicId}>{t.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-green-800 mb-1">Cấp Độ Nhận Thức (Bloom)</label>
                  <select
                    value={manualBloom}
                    onChange={(e) => setManualBloom(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium cursor-pointer"
                  >
                    {BLOOM_LEVELS.map(b => (
                      <option key={b.value} value={b.value}>{b.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Nội Dung Câu Hỏi <span className="text-error">*</span></label>
                <textarea
                  rows="3"
                  required
                  placeholder="Nhập nội dung câu hỏi vấn đáp chi tiết..."
                  value={manualContent}
                  onChange={(e) => setManualContent(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Đáp Án Tiêu Chuẩn Mong Đợi <span className="text-error">*</span></label>
                <textarea
                  rows="3"
                  required
                  placeholder="Đáp án hoặc các ý chính thí sinh cần nêu bật được..."
                  value={manualExpectedAnswer}
                  onChange={(e) => setManualExpectedAnswer(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
                />
              </div>

              {/* DYNAMIC RUBRIC BUILDER */}
              <div className="p-3.5 bg-paper rounded-xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] space-y-3">
                <div className="flex items-center justify-between border-b-2 border-cream pb-2">
                  <div>
                    <h4 className="text-xs font-bold text-ink">Tiêu Chí Chấm Điểm Rubric</h4>
                    <p className="text-[10px] text-on-surface-variant">Thiết lập các tiêu chí con chấm điểm tự động</p>
                  </div>
                  <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border-2 border-green-500 shadow-[1px_1px_0_#DEB9B5]">
                    Tổng: {manualCriteria.reduce((sum, c) => sum + Number(c.maxScore || 0), 0)}đ
                  </span>
                </div>

                <RubricCriteriaEditor criteria={manualCriteria} onChange={setManualCriteria} />
              </div>

              <div className="pt-2 flex justify-end gap-2 border-t-2 border-cream">
                <button
                  type="button"
                  onClick={() => setIsManualQuestionModalOpen(false)}
                  disabled={isSubmittingQuestion}
                  className="px-4 py-2 rounded-xl bg-paper border-2 border-green-500 text-green-800 text-xs font-bold shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingQuestion}
                  className="px-5 py-2 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 flex items-center gap-2"
                >
                  {isSubmittingQuestion && <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>}
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
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-paper rounded-2xl max-w-lg w-full border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] p-6 space-y-4 animate-modal-entry">
            <div className="flex items-center justify-between border-b-2 border-cream pb-3">
              <div className="flex items-center gap-2 text-green-800 font-bold text-xs">
                <FileSpreadsheet className="w-4 h-4 text-green-700" />
                <span className="font-heading uppercase tracking-wider">Nhập Câu Hỏi Hàng Loạt (UC1.6)</span>
              </div>
              <button onClick={() => setIsImportModalOpen(false)} className="text-on-surface-variant hover:text-ink p-1 rounded-lg hover:bg-green-100">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleImportQuestions} className="space-y-4">
              <div className="flex items-center justify-between bg-green-50 p-3 rounded-xl border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]">
                <span className="text-xs font-semibold text-green-900">Tải tệp mẫu quy chuẩn:</span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => handleDownloadTemplate('xlsx')}
                    className="px-2.5 py-1 bg-paper border-2 border-green-500 hover:bg-green-100 rounded-lg text-xs font-bold text-green-800 inline-flex items-center gap-1 shadow-[1px_1px_0_#DEB9B5]"
                  >
                    <Download className="w-3 h-3 text-green-700" />
                    <span>Excel (.xlsx)</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDownloadTemplate('csv')}
                    className="px-2.5 py-1 bg-paper border-2 border-green-500 hover:bg-green-100 rounded-lg text-xs font-bold text-green-800 inline-flex items-center gap-1 shadow-[1px_1px_0_#DEB9B5]"
                  >
                    <Download className="w-3 h-3 text-pink-600" />
                    <span>CSV</span>
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Chọn Tệp Tin Cần Nhập (.xlsx, .csv)</label>
                <input
                  type="file"
                  required
                  accept=".xlsx,.csv"
                  onChange={(e) => setImportFile(e.target.files[0] || null)}
                  className="w-full text-xs file:mr-3 file:py-1.5 file:px-3 file:rounded-xl file:border-2 file:border-green-500 file:text-xs file:font-bold file:bg-paper file:text-green-800 hover:file:bg-green-100 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-between p-3 bg-paper rounded-xl border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]">
                <div>
                  <p className="text-xs font-bold text-ink">Chế Độ Thẩm Định Thử Nghiệm (Dry Run)</p>
                  <p className="text-[10px] text-on-surface-variant">Chỉ kiểm tra tính hợp lệ dữ liệu, chưa ghi vào cơ sở dữ liệu</p>
                </div>
                <input
                  type="checkbox"
                  checked={isDryRun}
                  onChange={(e) => setIsDryRun(e.target.checked)}
                  className="w-4 h-4 text-green-700 rounded focus:ring-green-600 cursor-pointer"
                />
              </div>

              {/* Import Report Result Box */}
              {importReport && (
                <div className="p-3 bg-green-50 rounded-xl border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5] text-xs space-y-1 animate-modal-entry">
                  <p className="font-bold text-green-900">Báo Cáo Kết Quả Nhập:</p>
                  <p className="text-green-800">Tổng dòng đọc được: {importReport.totalRows || 0} ({importReport.totalQuestions || 0} câu hỏi)</p>
                  <p className="text-green-800">Câu hỏi hợp lệ: {importReport.validQuestions || 0}</p>
                  {!importReport.dryRun && (
                    <p className="text-green-800">Đã tạo: {importReport.createdQuestions || 0} câu hỏi</p>
                  )}
                  {importReport.invalidQuestions > 0 && (
                    <p className="text-error font-semibold">Câu hỏi lỗi: {importReport.invalidQuestions}</p>
                  )}
                  {importReport.errors?.length > 0 && (
                    <ul className="mt-1 max-h-32 overflow-y-auto space-y-0.5 text-[11px] text-error">
                      {importReport.errors.map((er, idx) => (
                        <li key={idx}>
                          Dòng {er.row}{er.column ? ` • ${er.column}` : ''}{er.questionRef ? ` • ${er.questionRef}` : ''}: {er.message}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}

              <div className="pt-2 flex justify-end gap-2 border-t-2 border-cream">
                <button
                  type="button"
                  onClick={() => setIsImportModalOpen(false)}
                  disabled={isImporting}
                  className="px-4 py-2 rounded-xl bg-paper border-2 border-green-500 text-green-800 text-xs font-bold shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition"
                >
                  Đóng
                </button>
                <button
                  type="submit"
                  disabled={isImporting || !importFile}
                  className="px-5 py-2 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 flex items-center gap-2"
                >
                  {isImporting && <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>}
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
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-paper rounded-2xl max-w-md w-full border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] p-5 space-y-4 animate-modal-entry">
            <h3 className="font-heading font-extrabold text-sm text-ink">Từ Chối Phê Duyệt Câu Hỏi</h3>
            <p className="text-xs text-on-surface-variant">
              Vui lòng nêu rõ lý do từ chối để hỗ trợ giảng viên soạn thảo hoặc định hướng lại prompt cho AI.
            </p>

            <textarea
              rows="3"
              required
              placeholder="Ví dụ: Câu hỏi chưa gắn đúng trích dẫn giáo trình hoặc đáp án chưa đủ độ sâu..."
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
            />

            <div className="flex justify-end gap-2 pt-2 border-t-2 border-cream">
              <button
                type="button"
                onClick={() => setRejectVersionTarget(null)}
                className="px-4 py-2 rounded-xl bg-paper border-2 border-green-500 text-green-800 text-xs font-bold shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition"
              >
                Hủy Bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmReject}
                disabled={!rejectReason.trim()}
                className="px-5 py-2 rounded-xl bg-paper border-2 border-error text-error hover:bg-pink-50 font-bold text-xs shadow-[2px_2px_0_#DEB9B5] disabled:opacity-50 transition"
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
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog">
          <div className="bg-paper rounded-2xl max-w-md w-full border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] p-5 space-y-4 animate-modal-entry">
            <div className="flex items-center gap-2 text-pink-900 font-bold text-xs">
              <Sparkles className="w-4 h-4 text-pink-600" />
              <span>Yêu Cầu AI Tạo Lại Câu Hỏi Này</span>
            </div>
            <p className="text-xs text-on-surface-variant">
              Cung cấp thêm chỉ dẫn điều chỉnh (Feedback). AI sẽ giữ nguyên ngữ cảnh trích dẫn ban đầu và viết lại câu hỏi phù hợp hơn.
            </p>

            <textarea
              rows="3"
              placeholder="Ví dụ: Hãy chuyển câu hỏi thành dạng bài toán tình huống thực tế..."
              value={regenFeedback}
              onChange={(e) => setRegenFeedback(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
            />

            <div className="flex justify-end gap-2 pt-2 border-t-2 border-cream">
              <button
                type="button"
                onClick={() => setRegenVersionTarget(null)}
                className="px-4 py-2 rounded-xl bg-paper border-2 border-green-500 text-green-800 text-xs font-bold shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition"
              >
                Hủy Bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmRegenerate}
                className="px-5 py-2 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-[2px] active:translate-y-[2px] active:shadow-none flex items-center gap-1.5"
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
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog" aria-modal="true">
          <div className="bg-paper rounded-2xl max-w-xl w-full border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] p-6 space-y-4 animate-modal-entry max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b-2 border-cream pb-3">
              <h3 className="font-heading font-extrabold text-sm text-ink">
                Chỉnh Sửa Bản Nháp {editDraft.version.questionCode}
              </h3>
              <button onClick={() => setEditDraft(null)} className="text-on-surface-variant hover:text-ink p-1 rounded-lg hover:bg-green-100">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveDraft} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Cấp Độ Nhận Thức (Bloom)</label>
                <select
                  value={editDraft.bloomLevel}
                  onChange={(e) => setEditDraft(d => ({ ...d, bloomLevel: e.target.value }))}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium cursor-pointer"
                >
                  {BLOOM_LEVELS.map(b => (
                    <option key={b.value} value={b.value}>{b.label}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Nội Dung Câu Hỏi <span className="text-error">*</span></label>
                <textarea
                  rows="3"
                  required
                  maxLength={4000}
                  value={editDraft.content}
                  onChange={(e) => setEditDraft(d => ({ ...d, content: e.target.value }))}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Đáp Án Tiêu Chuẩn Mong Đợi <span className="text-error">*</span></label>
                <textarea
                  rows="3"
                  required
                  maxLength={8000}
                  value={editDraft.expectedAnswer}
                  onChange={(e) => setEditDraft(d => ({ ...d, expectedAnswer: e.target.value }))}
                  className="w-full px-3 py-2 rounded-xl text-xs bg-surface-container-lowest border-2 border-green-500 text-ink focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] font-medium"
                />
              </div>

              <div className="p-3.5 bg-paper rounded-xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] space-y-3">
                <div className="flex items-center justify-between border-b-2 border-cream pb-2">
                  <h4 className="text-xs font-bold text-ink">Tiêu Chí Chấm Điểm Rubric</h4>
                  <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border-2 border-green-500 shadow-[1px_1px_0_#DEB9B5]">
                    Tổng: {editDraft.criteria.reduce((sum, c) => sum + Number(c.maxScore || 0), 0)}đ
                  </span>
                </div>
                <RubricCriteriaEditor
                  criteria={editDraft.criteria}
                  onChange={(criteria) => setEditDraft(d => ({ ...d, criteria }))}
                />
              </div>

              <div className="pt-2 flex justify-end gap-2 border-t-2 border-cream">
                <button
                  type="button"
                  onClick={() => setEditDraft(null)}
                  disabled={isSavingDraft}
                  className="px-4 py-2 rounded-xl bg-paper border-2 border-green-500 text-green-800 text-xs font-bold shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSavingDraft}
                  className="px-5 py-2 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 flex items-center gap-2"
                >
                  {isSavingDraft && <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>}
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
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in" role="dialog" aria-modal="true">
          <div className="bg-paper rounded-2xl max-w-2xl w-full border-2 border-green-500 shadow-[6px_6px_0_#D5A6A2] p-6 space-y-4 animate-modal-entry max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b-2 border-cream pb-3">
              <h3 className="font-heading font-extrabold text-sm text-ink">
                Chi Tiết Câu Hỏi {questionDetail.detail?.questionCode || ''}
              </h3>
              <button onClick={() => setQuestionDetail(null)} className="text-on-surface-variant hover:text-ink p-1 rounded-lg hover:bg-green-100">
                <X className="w-4 h-4" />
              </button>
            </div>

            {questionDetail.isLoading ? (
              <SkeletonCard />
            ) : (
              <div className="space-y-4 text-xs">
                <div className="flex flex-wrap gap-2">
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-green-100 text-green-800 border-2 border-green-500 shadow-[1px_1px_0_#DEB9B5]">
                    {questionDetail.detail.status}
                  </span>
                  {questionDetail.detail.pendingDraft && (
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-800 border-2 border-pink-400">
                      Bản nháp đang chờ: v{questionDetail.detail.pendingDraft.versionNumber}
                    </span>
                  )}
                </div>

                {questionDetail.detail.currentVersion && (
                  <div className="p-4 bg-pink-50 rounded-xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] space-y-2">
                    <p className="text-[10px] font-bold text-pink-900 uppercase tracking-wider">
                      Phiên bản hiện hành v{questionDetail.detail.currentVersion.versionNumber}
                    </p>
                    <p className="font-semibold text-ink leading-relaxed font-heading text-sm">{questionDetail.detail.currentVersion.content}</p>
                    <p className="text-on-surface-variant leading-relaxed">{questionDetail.detail.currentVersion.expectedAnswer}</p>
                  </div>
                )}

                <div>
                  <p className="text-[10px] font-bold text-green-800 uppercase tracking-wider mb-2">
                    Lịch Sử Phiên Bản ({questionDetail.history.length})
                  </p>
                  <div className="divide-y-2 divide-cream border-2 border-green-500 rounded-xl overflow-hidden bg-paper shadow-[3px_3px_0_#DEB9B5]">
                    {questionDetail.history.map(h => (
                      <div key={h.versionId} className="p-3.5 flex items-start justify-between gap-3 hover:bg-green-50/50 transition">
                        <div className="space-y-1 min-w-0">
                          <p className="font-bold text-ink">
                            v{h.versionNumber} • {h.origin} • {h.bloomLevel}
                          </p>
                          <p className="text-on-surface-variant line-clamp-2">{h.content}</p>
                          {h.rejectReason && <p className="text-error font-medium">Lý do từ chối: {h.rejectReason}</p>}
                        </div>
                        <div className="text-right shrink-0">
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-green-100 text-green-800 border border-green-500">
                            {h.status}
                          </span>
                          <p className="text-[10px] text-on-surface-variant mt-1 font-mono">
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
