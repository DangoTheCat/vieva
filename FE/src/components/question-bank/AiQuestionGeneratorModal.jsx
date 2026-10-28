import React, { useState, useEffect } from 'react';
import {
  Sparkles,
  X,
  RefreshCw,
  CheckCircle2,
  AlertCircle,
  Sliders,
  Check,
  Minus,
  Plus,
  RotateCcw,
  Cpu,
  Layers,
  BookOpen
} from 'lucide-react';
import {
  BLOOM_LEVELS,
  BLOOM_PRESETS,
  calculateBloomDistribution
} from './bloomConstants';

export function AiQuestionGeneratorModal({
  isOpen,
  onClose,
  topics = [],
  documents = [],
  isLoadingDocs = false,
  loadDocuments,
  onTriggerGeneration,
  isGeneratingAi = false,
  failedGenRequest = null,
  onRetryGeneration,
  onSwitchToDocsTab
}) {
  const [topicId, setTopicId] = useState('');
  const [selectedDocIds, setSelectedDocIds] = useState([]);
  const [totalQuestions, setTotalQuestions] = useState(5);
  const [selectedBloomPreset, setSelectedBloomPreset] = useState('BALANCED');
  const [bloomCounts, setBloomCounts] = useState({
    REMEMBER: 1,
    UNDERSTAND: 2,
    APPLY: 1,
    ANALYZE: 1,
    EVALUATE: 0,
    CREATE: 0
  });
  const [lecturerNote, setLecturerNote] = useState('');

  // Generation Progress Stepper State for real-time visual feedback
  const [activeStep, setActiveStep] = useState(1);

  useEffect(() => {
    let interval;
    if (isGeneratingAi) {
      setActiveStep(1);
      interval = setInterval(() => {
        setActiveStep((prev) => (prev < 4 ? prev + 1 : prev));
      }, 1800);
    } else {
      setActiveStep(1);
    }
    return () => clearInterval(interval);
  }, [isGeneratingAi]);

  // When presets change
  const handleSelectBloomPreset = (presetId) => {
    setSelectedBloomPreset(presetId);
    const p = BLOOM_PRESETS.find((x) => x.id === presetId);
    if (!p) return;
    if (p.weights) {
      setBloomCounts(calculateBloomDistribution(totalQuestions, p.weights));
    }
  };

  const handleTotalQuestionsChange = (val) => {
    const n = Math.max(1, Math.min(20, Number(val) || 1));
    setTotalQuestions(n);
    const p = BLOOM_PRESETS.find((x) => x.id === selectedBloomPreset);
    if (p && p.weights) {
      setBloomCounts(calculateBloomDistribution(n, p.weights));
    }
  };

  const handleBloomCountChange = (level, val, isDirect = false) => {
    setSelectedBloomPreset('CUSTOM');
    setBloomCounts((prev) => {
      const current = prev[level] || 0;
      const next = isDirect ? Math.max(0, Math.min(20, Number(val) || 0)) : Math.max(0, current + val);
      return { ...prev, [level]: next };
    });
  };

  const totalAllocatedBloom = Object.values(bloomCounts).reduce((sum, v) => sum + (Number(v) || 0), 0);
  const isBloomBalanced = totalAllocatedBloom === Number(totalQuestions);

  const handleAutoBalanceBloom = () => {
    const p = BLOOM_PRESETS.find((x) => x.id === 'BALANCED') || BLOOM_PRESETS[0];
    setBloomCounts(calculateBloomDistribution(totalQuestions, p.weights));
    setSelectedBloomPreset('BALANCED');
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in" role="dialog">
      <div className="bg-paper rounded-2xl max-w-2xl w-full max-h-[92vh] overflow-y-auto border border-pink-200 shadow-md p-5 sm:p-6 space-y-4 animate-modal-entry">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-pink-200 pb-3">
          <div className="flex items-center gap-2 text-pink-900 font-bold text-xs">
            <Sparkles className="w-4 h-4 text-pink-600" />
            <span className="font-heading uppercase tracking-wider">Cấu Hình RAG Prompting Engine</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isGeneratingAi}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Progress Stepper when AI is generating */}
        {isGeneratingAi && (
          <div className="p-4 rounded-xl bg-pink-50 border border-pink-200 space-y-3 animate-pulse">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-pink-900 flex items-center gap-1.5">
                <Cpu className="w-4 h-4 text-pink-600 animate-spin" />
                <span>AI RAG Engine Đang Xử Lý Câu Hỏi...</span>
              </span>
              <span className="text-[11px] font-mono font-bold text-pink-700">Bước {activeStep} / 4</span>
            </div>

            <div className="grid grid-cols-4 gap-2 text-[10px]">
              <div className={`p-2 rounded-lg border text-center font-medium ${activeStep >= 1 ? 'bg-pink-200 border-pink-300 text-pink-950 font-bold' : 'bg-white/60 text-slate-400 border-slate-200'}`}>
                1. Trích xuất RAG
              </div>
              <div className={`p-2 rounded-lg border text-center font-medium ${activeStep >= 2 ? 'bg-pink-200 border-pink-300 text-pink-950 font-bold' : 'bg-white/60 text-slate-400 border-slate-200'}`}>
                2. Khoanh vùng Vector
              </div>
              <div className={`p-2 rounded-lg border text-center font-medium ${activeStep >= 3 ? 'bg-pink-200 border-pink-300 text-pink-950 font-bold' : 'bg-white/60 text-slate-400 border-slate-200'}`}>
                3. Bậc thang Bloom
              </div>
              <div className={`p-2 rounded-lg border text-center font-medium ${activeStep >= 4 ? 'bg-pink-200 border-pink-300 text-pink-950 font-bold' : 'bg-white/60 text-slate-400 border-slate-200'}`}>
                4. Tạo Rubric
              </div>
            </div>
          </div>
        )}

        <div className="space-y-4">
          
          {/* Topic Selector */}
          <div>
            <label className="block text-xs font-bold text-ink mb-1">Chủ Đề Áp Dụng (Topic)</label>
            <select
              value={topicId}
              onChange={(e) => setTopicId(e.target.value)}
              disabled={isGeneratingAi}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-sm font-medium cursor-pointer"
            >
              <option value="">-- Mặc định (Toàn bộ học phần) --</option>
              {topics.map((t) => (
                <option key={t.topicId} value={t.topicId}>
                  {t.name}
                </option>
              ))}
            </select>
          </div>

          {/* RAG Context Documents */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-xs font-bold text-ink">
                Chọn Giáo Trình Làm Nguồn Ngữ Cảnh (RAG Context) <span className="text-rose-600">*</span>
              </label>
              <div className="flex items-center gap-2">
                {documents.some((d) => ['READY', 'SUCCESS'].includes(d.indexingStatus || d.status)) && (
                  <button
                    type="button"
                    disabled={isGeneratingAi}
                    onClick={() => {
                      const readyIds = documents
                        .filter((d) => ['READY', 'SUCCESS'].includes(d.indexingStatus || d.status))
                        .map((d) => d.documentId);
                      if (selectedDocIds.length === readyIds.length) {
                        setSelectedDocIds([]);
                      } else {
                        setSelectedDocIds(readyIds);
                      }
                    }}
                    className="text-[11px] font-bold text-pink-700 hover:text-pink-900 cursor-pointer"
                  >
                    {selectedDocIds.length === documents.filter((d) => ['READY', 'SUCCESS'].includes(d.indexingStatus || d.status)).length && selectedDocIds.length > 0
                      ? 'Bỏ chọn tất cả'
                      : 'Chọn tất cả'}
                  </button>
                )}
                <button
                  type="button"
                  onClick={loadDocuments}
                  disabled={isLoadingDocs || isGeneratingAi}
                  className="text-[11px] font-bold text-pink-700 hover:text-pink-900 flex items-center gap-1 cursor-pointer"
                  title="Làm mới danh sách giáo trình"
                >
                  <RefreshCw className={`w-3 h-3 ${isLoadingDocs ? 'animate-spin' : ''}`} />
                  <span>Làm mới</span>
                </button>
              </div>
            </div>

            <div className="space-y-1.5 max-h-36 overflow-y-auto border border-pink-200 p-2.5 rounded-xl bg-white shadow-sm">
              {isLoadingDocs ? (
                <div className="flex items-center justify-center py-4 gap-2 text-xs text-slate-500">
                  <span className="w-3.5 h-3.5 border border-pink-500 border-t-transparent rounded-full animate-spin"></span>
                  <span>Đang kiểm tra tài liệu giáo trình...</span>
                </div>
              ) : documents.length === 0 ? (
                <div className="text-center py-3 space-y-1">
                  <p className="text-slate-500 text-xs italic">Chưa có tài liệu nào được tải lên cho môn học này.</p>
                  {onSwitchToDocsTab && (
                    <button
                      type="button"
                      onClick={() => {
                        onClose();
                        onSwitchToDocsTab();
                      }}
                      className="text-xs font-bold text-pink-700 hover:underline cursor-pointer"
                    >
                      Chuyển sang tab Kho Giáo Trình để tải lên &rarr;
                    </button>
                  )}
                </div>
              ) : (
                documents.map((d) => {
                  const status = d.indexingStatus || d.status;
                  const isReady = status === 'READY' || status === 'SUCCESS';
                  const isIndexing = status === 'INDEXING' || status === 'UPLOADED';
                  const isFailed = status === 'FAILED';

                  return (
                    <label
                      key={d.documentId}
                      className={`flex items-center justify-between gap-2 text-xs p-1.5 rounded-lg transition ${
                        isReady ? 'font-medium text-ink cursor-pointer hover:bg-pink-50' : 'opacity-70 bg-slate-50 cursor-not-allowed'
                      }`}
                    >
                      <div className="flex items-center gap-2 truncate min-w-0">
                        <input
                          type="checkbox"
                          disabled={!isReady || isGeneratingAi}
                          checked={selectedDocIds.includes(d.documentId)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedDocIds((prev) => [...prev, d.documentId]);
                            } else {
                              setSelectedDocIds((prev) => prev.filter((id) => id !== d.documentId));
                            }
                          }}
                          className="rounded text-pink-600 focus:ring-pink-500 disabled:opacity-40"
                        />
                        <span className="truncate">{d.fileName}</span>
                      </div>

                      <div className="shrink-0">
                        {isReady && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-extrabold text-emerald-800 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full">
                            <CheckCircle2 className="w-2.5 h-2.5 text-emerald-600" />
                            READY ({d.totalChunks || d.chunkCount || 0} chunks)
                          </span>
                        )}
                        {isIndexing && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-amber-800 bg-amber-50 border border-amber-300 px-2 py-0.5 rounded-full">
                            <span className="w-2.5 h-2.5 border-2 border-amber-600 border-t-transparent rounded-full animate-spin"></span>
                            {status === 'INDEXING' ? 'Đang index...' : 'Chờ index...'}
                          </span>
                        )}
                        {isFailed && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-rose-700 bg-rose-50 border border-rose-200 px-2 py-0.5 rounded-full">
                            <AlertCircle className="w-2.5 h-2.5 text-rose-600" />
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
              <label className="text-xs font-bold text-ink">
                Số Lượng Câu Hỏi Cần Sinh (1 - 20) <span className="text-rose-600">*</span>
              </label>
              <div className="flex items-center gap-1.5">
                {[3, 5, 10, 15].map((cnt) => (
                  <button
                    key={cnt}
                    type="button"
                    disabled={isGeneratingAi}
                    onClick={() => handleTotalQuestionsChange(cnt)}
                    className={`px-2 py-0.5 rounded-lg text-[11px] font-bold border transition cursor-pointer ${
                      Number(totalQuestions) === cnt
                        ? 'bg-pink-100 text-pink-900 border-pink-300 shadow-2xs'
                        : 'bg-white text-ink border-pink-200 hover:bg-pink-50'
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
              disabled={isGeneratingAi}
              value={totalQuestions}
              onChange={(e) => handleTotalQuestionsChange(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-sm font-medium"
            />
          </div>

          {/* Bloom Taxonomy Presets */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-ink flex items-center gap-1.5">
                <Sliders className="w-3.5 h-3.5 text-pink-700" />
                <span>Mẫu Phân Bổ Cấp Độ Bloom (Presets)</span>
              </label>
              <span className="text-[10px] text-pink-800 font-mono">Đủ 6 bậc thang tư duy</span>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
              {BLOOM_PRESETS.map((p) => {
                const isSelected = selectedBloomPreset === p.id;
                return (
                  <button
                    key={p.id}
                    type="button"
                    disabled={isGeneratingAi}
                    onClick={() => handleSelectBloomPreset(p.id)}
                    className={`p-2 rounded-xl text-left border transition text-xs cursor-pointer ${
                      isSelected
                        ? 'bg-pink-100/80 border-pink-300 text-pink-950 shadow-2xs font-bold'
                        : 'bg-white border-pink-200 text-ink hover:bg-pink-50/70'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="truncate">{p.name}</span>
                      {isSelected && <Check className="w-3.5 h-3.5 text-pink-700 shrink-0" />}
                    </div>
                    <p className="text-[10px] text-slate-500 font-normal truncate mt-0.5">{p.desc}</p>
                  </button>
                );
              })}
            </div>
          </div>

          {/* 6-Level Bloom Interactive Grid */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-ink">Chi Tiết Phân Bổ 6 Cấp Độ Nhận Thức Bloom</label>
              <div className="flex items-center gap-2">
                <span
                  className={`text-[11px] font-mono px-2 py-0.5 rounded-lg border font-bold ${
                    isBloomBalanced ? 'bg-emerald-50 text-emerald-800 border-pink-200' : 'bg-rose-50 text-rose-800 border-rose-300'
                  }`}
                >
                  {totalAllocatedBloom} / {totalQuestions} câu (
                  {Number(totalQuestions) > 0 ? Math.round((totalAllocatedBloom / Number(totalQuestions)) * 100) : 0}%)
                </span>
                {!isBloomBalanced && (
                  <button
                    type="button"
                    disabled={isGeneratingAi}
                    onClick={handleAutoBalanceBloom}
                    className="px-2 py-0.5 rounded-lg text-[11px] font-bold bg-pink-50 text-pink-900 border border-pink-200 hover:bg-pink-100 transition shadow-sm cursor-pointer"
                    title="Tự động chia đều theo tổng số câu hỏi"
                  >
                    Tự động cân bằng
                  </button>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              {BLOOM_LEVELS.map((level) => {
                const count = bloomCounts[level.value] || 0;
                const percent = totalAllocatedBloom > 0 ? Math.round((count / totalAllocatedBloom) * 100) : 0;
                return (
                  <div
                    key={level.value}
                    className={`p-2.5 rounded-xl border transition ${
                      count > 0 ? 'bg-paper border-pink-200 shadow-2xs' : 'bg-white border-pink-100 opacity-70 hover:opacity-100'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="text-[11px] font-bold text-ink truncate" title={level.label}>
                        {level.label}
                      </span>
                      <span className="text-[10px] font-mono text-slate-600 bg-pink-50 px-1 rounded border border-pink-200">
                        {percent}%
                      </span>
                    </div>
                    <div className="flex items-center justify-between gap-1">
                      <button
                        type="button"
                        onClick={() => handleBloomCountChange(level.value, -1)}
                        disabled={count <= 0 || isGeneratingAi}
                        className="w-7 h-7 rounded-lg border border-pink-200 bg-white text-ink font-bold flex items-center justify-center hover:bg-pink-100 active:scale-95 disabled:opacity-30 disabled:pointer-events-none cursor-pointer"
                      >
                        <Minus className="w-3.5 h-3.5" />
                      </button>
                      <input
                        type="number"
                        min="0"
                        max="20"
                        disabled={isGeneratingAi}
                        value={count}
                        onChange={(e) => handleBloomCountChange(level.value, e.target.value, true)}
                        className="w-12 text-center py-1 text-xs font-bold bg-white rounded-lg border border-pink-200 text-ink focus:outline-none focus:border-pink-500"
                      />
                      <button
                        type="button"
                        onClick={() => handleBloomCountChange(level.value, 1)}
                        disabled={totalAllocatedBloom >= 20 || isGeneratingAi}
                        className="w-7 h-7 rounded-lg border border-pink-200 bg-white text-ink font-bold flex items-center justify-center hover:bg-pink-100 active:scale-95 disabled:opacity-30 disabled:pointer-events-none cursor-pointer"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Lecturer Note */}
          <div>
            <label className="block text-xs font-bold text-ink mb-1">Ghi Chú Đạo Diễn Cho AI (Lecturer Instruction)</label>
            <textarea
              rows="2"
              disabled={isGeneratingAi}
              placeholder="Ví dụ: Đặt câu hỏi xoáy sâu vào các bẫy thường gặp trong xử lý đa luồng, yêu cầu sinh viên so sánh ưu nhược điểm..."
              value={lecturerNote}
              onChange={(e) => setLecturerNote(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-sm font-medium"
            />
          </div>

          {failedGenRequest && (
            <div className="p-3 rounded-xl border border-rose-200 bg-rose-50 text-xs text-rose-900 space-y-1 shadow-sm">
              <p className="font-bold">
                Lần sinh gần nhất: {failedGenRequest.status} ({failedGenRequest.generatedCount}/{failedGenRequest.totalQuestions} câu)
              </p>
              {failedGenRequest.errorMessage && <p>{failedGenRequest.errorMessage}</p>}
              {failedGenRequest.issues?.map((issue, idx) => (
                <p key={idx}>• {issue}</p>
              ))}
            </div>
          )}

          {!isBloomBalanced && (
            <div className="p-2.5 rounded-xl border border-rose-200 bg-rose-50 text-[11px] text-rose-900 flex items-center justify-between">
              <span>⚠ Tổng số câu ({totalAllocatedBloom}) chưa khớp với số lượng ({totalQuestions}).</span>
              <button
                type="button"
                onClick={handleAutoBalanceBloom}
                className="px-2.5 py-1 rounded-lg text-xs font-bold bg-white text-rose-800 border border-rose-300 hover:bg-rose-100 transition cursor-pointer"
              >
                Khớp tự động
              </button>
            </div>
          )}

          {/* Modal Actions */}
          <div className="pt-2 flex justify-end gap-2 border-t border-pink-100">
            {failedGenRequest && (
              <button
                type="button"
                onClick={onRetryGeneration}
                disabled={isGeneratingAi}
                className="px-4 py-2 rounded-xl border border-rose-200 text-rose-900 bg-rose-50 text-xs font-bold hover:bg-rose-100 transition active:scale-95 disabled:opacity-50 flex items-center gap-1.5 shadow-sm cursor-pointer"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Thử Lại Phần Thiếu</span>
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              disabled={isGeneratingAi}
              className="px-4 py-2 rounded-xl bg-white border border-pink-200 text-slate-700 text-xs font-bold shadow-sm hover:bg-slate-50 transition cursor-pointer"
            >
              Hủy Bỏ
            </button>
            <button
              type="button"
              onClick={() => {
                onTriggerGeneration({
                  topicId,
                  documentIds: selectedDocIds,
                  totalQuestions: Number(totalQuestions),
                  bloomDistribution: bloomCounts,
                  lecturerNote
                });
              }}
              disabled={isGeneratingAi || selectedDocIds.length === 0 || !isBloomBalanced}
              className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 flex items-center gap-2 cursor-pointer"
            >
              {isGeneratingAi && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
              <span>{isGeneratingAi ? 'Đang Sinh Câu Hỏi...' : 'Bắt Đầu Sinh Câu Hỏi'}</span>
            </button>
          </div>

        </div>
      </div>
    </div>
  );
}
