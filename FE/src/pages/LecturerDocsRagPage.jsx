import React, { useState, useEffect } from 'react';
import { lecturerCatalogApi } from '../api/lecturerCatalogApi';
import { courseDocumentApi } from '../api/courseDocumentApi';
import { getErrorMessage } from '../utils/errorCodes';
import { MaterialDropzone } from '../components/teacher/MaterialDropzone';
import {
  FileText,
  UploadCloud,
  Layers,
  Database,
  CheckCircle2,
  Eye,
  X,
  Loader2,
  Trash2,
  AlertCircle,
  FolderPlus,
  RotateCcw,
  BookOpen
} from 'lucide-react';

export function LecturerDocsRagPage({ onNavigate, showToast }) {
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isChunksModalOpen, setIsChunksModalOpen] = useState(false);
  const [selectedDocForChunks, setSelectedDocForChunks] = useState(null);

  // Chunk Preview State
  const [previewData, setPreviewData] = useState(null);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState(null);

  // Upload Form State
  const [fileToUpload, setFileToUpload] = useState(null);
  const [isUploading, setIsUploading] = useState(false);

  // 1. Load assigned subjects from BE
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

  // 2. Load documents for selected subject from BE
  useEffect(() => {
    if (!selectedSubjectId) {
      setDocuments([]);
      return;
    }

    async function loadDocuments() {
      setIsLoading(true);
      try {
        const docs = await courseDocumentApi.getDocuments(selectedSubjectId);
        const list = Array.isArray(docs) ? docs : docs?.content || [];
        setDocuments(list);
      } catch (err) {
        setDocuments([]);
      } finally {
        setIsLoading(false);
      }
    }
    loadDocuments();
  }, [selectedSubjectId]);

  // 3. Auto-polling when documents are in UPLOADED or INDEXING state
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
        // Silently continue polling
      }
    }, 3000);

    return () => clearInterval(intervalId);
  }, [documents, selectedSubjectId]);

  const handleUploadSubmit = async (e) => {
    e.preventDefault();
    if (!fileToUpload || !selectedSubjectId) return;

    setIsUploading(true);
    try {
      const uploadedDoc = await courseDocumentApi.uploadDocument(selectedSubjectId, fileToUpload);

      if (uploadedDoc && (uploadedDoc.documentId || uploadedDoc.id)) {
        setDocuments(prev => [uploadedDoc, ...prev]);
      } else {
        const refreshed = await courseDocumentApi.getDocuments(selectedSubjectId);
        const list = Array.isArray(refreshed) ? refreshed : refreshed?.content || [];
        setDocuments(list);
      }

      showToast({
        type: 'success',
        title: 'Tải Lên Thành Công',
        message: `Đã tiếp nhận tệp "${fileToUpload.name}" và bắt đầu Lập chỉ mục Vector Store (RAG Indexing)!`
      });

      setIsUploadModalOpen(false);
      setFileToUpload(null);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsUploading(false);
    }
  };

  const handleDeleteDocument = async (docId) => {
    if (!docId) return;
    try {
      await courseDocumentApi.deleteDocument(docId);
      setDocuments(prev => prev.filter(d => (d.documentId || d.id) !== docId));
      showToast({ type: 'success', message: 'Đã xóa tài liệu khỏi hệ thống RAG!' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  const handleRetryIndexing = async (docId) => {
    if (!docId) return;
    try {
      const updated = await courseDocumentApi.retryIndexing(docId);
      setDocuments(prev => prev.map(d =>
        (d.documentId || d.id) === docId
          ? { ...d, ...(updated || {}), indexingStatus: updated?.indexingStatus || updated?.status || 'INDEXING' }
          : d
      ));
      showToast({ type: 'success', message: 'Đã gửi yêu cầu thử lại lập chỉ mục (Retry Index)!' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  const handleOpenChunksModal = async (doc) => {
    const docId = doc.documentId || doc.id;
    setSelectedDocForChunks(doc);
    setIsChunksModalOpen(true);
    setIsPreviewLoading(true);
    setPreviewError(null);
    setPreviewData(null);

    try {
      const preview = await courseDocumentApi.getDocumentPreview(docId);
      setPreviewData(preview);
    } catch (err) {
      // Fallback display if preview endpoint encounters an issue
      setPreviewError(getErrorMessage(err));
    } finally {
      setIsPreviewLoading(false);
    }
  };

  return (
    <div className="space-y-6 animate-modal-entry text-ink pb-10">

      {/* HEADER BAR */}
      <div className="bg-paper border-2 border-green-500 p-5 sm:p-6 rounded-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-[3px_3px_0_#DEB9B5]">
        <div>
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-green-100 border border-green-500 text-green-800 text-[11px] font-semibold shadow-[1px_1px_0_#DEB9B5] mb-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-green-600"></span>
            <span>Kho Tài Liệu RAG Giáo Trình (Group 1)</span>
          </div>
          <h1 className="font-heading font-extrabold text-lg sm:text-xl text-ink tracking-tight">
            Kho Tài Liệu &amp; Lập Chỉ Mục RAG
          </h1>
          <p className="text-xs sm:text-sm text-on-surface-variant mt-0.5">
            Tải lên tài liệu giáo trình, bóc tách phân đoạn Vector Store để AI Giám thị tham chiếu trực tiếp
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
          {/* Select Subject */}
          <select
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
            className="bg-paper border-2 border-green-500 rounded-lg px-3 py-2 text-xs font-bold text-ink shadow-[2px_2px_0_#DEB9B5] focus:outline-none focus:border-green-600 cursor-pointer"
          >
            {subjects.length === 0 && <option value="">Chưa có môn học phân công</option>}
            {subjects.map((s) => (
              <option key={s.subjectId} value={s.subjectId}>
                {s.subjectCode} - {s.subjectName}
              </option>
            ))}
          </select>

          <button
            type="button"
            disabled={!selectedSubjectId}
            onClick={() => setIsUploadModalOpen(true)}
            className="inline-flex items-center gap-2 bg-pink-500 hover:bg-pink-400 text-ink font-bold text-xs px-4 py-2 rounded-lg border-2 border-green-600 shadow-[3px_3px_0_#DEB9B5] hover:shadow-[1px_1px_0_#DEB9B5] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all disabled:opacity-50 cursor-pointer"
          >
            <UploadCloud className="w-4 h-4 text-pink-900 font-bold" />
            <span>+ Tải Tài Liệu Mới</span>
          </button>
        </div>
      </div>

      {/* DOCUMENTS TABLE FROM BE */}
      <div className="bg-surface-container-lowest border-2 border-green-500 rounded-xl shadow-[6px_6px_0_#D5A6A2] overflow-hidden">
        {/* Table Header Toolbar */}
        <div className="p-4 sm:px-6 bg-pink-50/50 border-b-2 border-cream flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Database className="w-4 h-4 text-green-700" />
            <span className="font-heading font-semibold text-sm text-ink">Danh Mục Tài Liệu Huấn Luyện Ngữ Cảnh</span>
            <span className="px-2.5 py-0.5 rounded-full bg-pink-200 text-pink-900 border border-pink-400 text-[11px] font-semibold font-mono">
              Tổng số: {documents.length}
            </span>
          </div>
          <div className="flex items-center gap-1.5 text-xs text-on-surface-variant bg-paper px-3 py-1 rounded-lg border border-green-300 shadow-[1px_1px_0_#DEB9B5]">
            <CheckCircle2 className="w-3.5 h-3.5 text-green-600" />
            <span>Chỉ tài liệu <span className="font-semibold text-green-800 underline decoration-green-400">READY</span> mới được trích xuất ngữ cảnh RAG</span>
          </div>
        </div>

        {isLoading ? (
          <div className="p-10 text-center bg-paper space-y-3">
            <Loader2 className="w-7 h-7 text-green-700 animate-spin mx-auto" />
            <p className="text-xs text-on-surface-variant font-medium">Đang tải danh sách tài liệu RAG từ Backend...</p>
          </div>
        ) : documents.length === 0 ? (
          <div className="p-10 text-center bg-paper space-y-3">
            <FolderPlus className="w-9 h-9 text-outline mx-auto" />
            <p className="text-sm font-bold text-ink">Chưa Có Tài Liệu Giáo Trình Trực Tuyến</p>
            <p className="text-xs text-on-surface-variant max-w-md mx-auto">
              Chưa có tệp tài liệu nào được lập chỉ mục RAG trên Backend cho môn học này. Hãy tải lên file PDF, DOCX hoặc PPTX để tạo tri thức cho AI Examiner.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-ink border-collapse">
              <thead className="bg-paper border-b-2 border-green-500 text-green-900 font-bold uppercase text-[10px] tracking-wider">
                <tr>
                  <th className="py-3 px-4 sm:px-6">Tên Tệp Giáo Trình</th>
                  <th className="py-3 px-4">Trạng Thái RAG</th>
                  <th className="py-3 px-4">Số Phân Đoạn (Chunks)</th>
                  <th className="py-3 px-4">Cấu Hình Token</th>
                  <th className="py-3 px-4 sm:px-6 text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-cream">
                {documents.map((doc) => {
                  const docId = doc.documentId || doc.id;
                  const status = doc.indexingStatus || doc.status || 'UPLOADED';
                  const isReady = status === 'READY' || status === 'SUCCESS';
                  const isFailed = status === 'FAILED' || status === 'ERROR';
                  const isIndexing = status === 'INDEXING' || status === 'UPLOADED';
                  const chunkCount = doc.totalChunks ?? doc.chunksCount ?? doc.chunkCount ?? 0;

                  return (
                    <tr key={docId} className="hover:bg-green-50/40 transition-colors">
                      <td className="py-3.5 px-4 sm:px-6">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-lg bg-pink-100 border-2 border-green-500 flex items-center justify-center text-primary shadow-[2px_2px_0_#DEB9B5] shrink-0">
                            <FileText className="w-4 h-4 text-pink-700" />
                          </div>
                          <div className="min-w-0">
                            <p className="font-bold text-ink truncate max-w-xs sm:max-w-md">{doc.fileName || doc.title || 'Tai_lieu_giao_trinh.pdf'}</p>
                            <div className="flex items-center gap-1.5 mt-0.5">
                              <span className="text-[10px] text-pink-700 bg-pink-50 px-1.5 py-0.5 rounded border border-pink-200 font-medium">
                                {doc.mimeType || doc.fileType || 'PDF Document'}
                              </span>
                              {doc.fileSizeBytes && (
                                <span className="text-[10px] text-on-surface-variant font-mono">
                                  • {(doc.fileSizeBytes / (1024 * 1024)).toFixed(2)} MB
                                </span>
                              )}
                              {doc.errorMessage && (
                                <span className="text-[10px] text-pink-800 bg-pink-100 px-1.5 py-0.2 rounded border border-pink-300 font-medium truncate max-w-xs" title={doc.errorMessage}>
                                  Lỗi: {doc.errorMessage}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border-2 inline-flex items-center gap-1.5 shadow-[1px_1px_0_#DEB9B5] ${
                          isReady
                            ? 'bg-green-100 border-green-400 text-green-900'
                            : isFailed
                            ? 'bg-pink-100 border-pink-400 text-pink-900'
                            : 'bg-amber-100 border-amber-300 text-amber-900'
                        }`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${
                            isReady ? 'bg-green-600' : isFailed ? 'bg-pink-700' : 'bg-amber-500 animate-pulse'
                          }`}></span>
                          {status}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <span className="font-mono font-bold text-green-800 bg-green-50 px-2 py-0.5 rounded border border-green-300">
                          {chunkCount} chunks
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-on-surface-variant font-mono text-[11px] whitespace-nowrap">
                        {doc.chunkSize ? `${doc.chunkSize} tokens` : '512 tokens / overlap 50'}
                      </td>
                      <td className="py-3.5 px-4 sm:px-6 text-right whitespace-nowrap space-x-1.5">
                        <button
                          type="button"
                          onClick={() => handleOpenChunksModal(doc)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-paper hover:bg-green-100 text-green-700 border-2 border-green-500 font-semibold text-xs shadow-[2px_2px_0_#DEB9B5] active:translate-x-[1px] active:translate-y-[1px] active:shadow-none transition-all cursor-pointer"
                          title="Xem phân đoạn Vector"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Xem Chunks</span>
                        </button>
                        {isFailed && (
                          <button
                            type="button"
                            onClick={() => handleRetryIndexing(docId)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-pink-500 hover:bg-pink-400 text-ink border-2 border-green-600 font-semibold text-xs shadow-[2px_2px_0_#DEB9B5] active:translate-x-[1px] active:translate-y-[1px] active:shadow-none transition-all cursor-pointer"
                            title="Thử lại bóc tách chỉ mục"
                          >
                            <RotateCcw className="w-3.5 h-3.5 text-pink-900" />
                            <span>Retry</span>
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => handleDeleteDocument(docId)}
                          className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-pink-50 hover:bg-pink-100 text-pink-700 border-2 border-pink-400 font-semibold text-xs shadow-[2px_2px_0_#DEB9B5] active:translate-x-[1px] active:translate-y-[1px] active:shadow-none transition-all cursor-pointer"
                          title="Xóa tài liệu"
                        >
                          <Trash2 className="w-3.5 h-3.5 text-pink-700" />
                          <span>Xóa</span>
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <div className="p-3.5 sm:px-6 bg-paper border-t-2 border-cream flex flex-col sm:flex-row items-center justify-between text-xs text-on-surface-variant gap-2">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-green-500"></span>
            <span className="font-semibold text-ink">Tổng số tài liệu giáo trình: {documents.length}</span>
          </div>
          <div className="font-mono text-green-800 font-semibold flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
            <span>Vector Store Pipeline: ONLINE (pgvector)</span>
          </div>
        </div>
      </div>

      {/* UPLOAD MODAL WITH MATERIAL DROPZONE */}
      {isUploadModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-paper border-2 border-green-500 rounded-2xl max-w-lg w-full p-6 shadow-[6px_6px_0_#DEB9B5] space-y-4 animate-modal-entry text-ink">
            <div className="flex items-center justify-between pb-3 border-b-2 border-cream">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-pink-100 border-2 border-green-500 flex items-center justify-center text-primary shadow-[2px_2px_0_#DEB9B5]">
                  <UploadCloud className="w-4 h-4 text-pink-700" />
                </div>
                <h3 className="font-heading font-bold text-base text-ink">Tải Lên Tài Liệu Giáo Trình</h3>
              </div>
              <button 
                type="button" 
                onClick={() => {
                  if (!isUploading) {
                    setIsUploadModalOpen(false);
                    setFileToUpload(null);
                  }
                }} 
                className="text-pink-700 hover:text-pink-900 p-1 cursor-pointer transition-colors"
                aria-label="Đóng"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleUploadSubmit} className="space-y-4 text-xs sm:text-sm text-ink">
              <MaterialDropzone
                file={fileToUpload}
                onFileSelect={(file) => setFileToUpload(file)}
                onFileRemove={() => setFileToUpload(null)}
                isUploading={isUploading}
              />

              <div className="pt-3 border-t-2 border-cream flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  disabled={isUploading}
                  onClick={() => {
                    setIsUploadModalOpen(false);
                    setFileToUpload(null);
                  }}
                  className="px-4 py-2 rounded-full border-2 border-outline-variant text-xs sm:text-sm font-semibold text-on-surface-variant bg-paper shadow-[2px_2px_0_#DEB9B5] hover:bg-green-50 cursor-pointer transition-all disabled:opacity-50"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isUploading || !fileToUpload}
                  className="px-5 py-2 rounded-full bg-pink-500 hover:bg-pink-400 text-ink text-xs sm:text-sm font-bold border-2 border-green-600 shadow-[3px_3px_0_#DEB9B5] hover:shadow-[1px_1px_0_#DEB9B5] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
                >
                  {isUploading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Tải Lên &amp; Lập Chỉ Mục ↗</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* VIEW CHUNKS MODAL WITH REAL PREVIEW DATA */}
      {isChunksModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-paper border-2 border-green-500 rounded-2xl max-w-2xl w-full p-6 shadow-[6px_6px_0_#DEB9B5] space-y-4 animate-modal-entry text-ink max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b-2 border-cream shrink-0">
              <div className="flex items-center gap-2 min-w-0">
                <div className="w-8 h-8 rounded-lg bg-pink-100 border-2 border-green-500 flex items-center justify-center text-primary shadow-[2px_2px_0_#DEB9B5] shrink-0">
                  <Layers className="w-4 h-4 text-pink-700" />
                </div>
                <div className="min-w-0">
                  <h3 className="font-heading font-bold text-base text-ink truncate max-w-sm sm:max-w-md">
                    Chi Tiết Vector Chunks • {selectedDocForChunks?.fileName || 'Tài liệu'}
                  </h3>
                  {selectedDocForChunks && (
                    <p className="text-[11px] text-on-surface-variant font-mono">
                      Trạng thái: <span className="font-bold text-green-800">{selectedDocForChunks.indexingStatus || selectedDocForChunks.status}</span>
                      {selectedDocForChunks.totalChunks ? ` • ${selectedDocForChunks.totalChunks} chunks` : ''}
                    </p>
                  )}
                </div>
              </div>
              <button 
                type="button" 
                onClick={() => setIsChunksModalOpen(false)} 
                className="text-pink-700 hover:text-pink-900 p-1 cursor-pointer transition-colors shrink-0"
                aria-label="Đóng"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="flex-1 overflow-y-auto space-y-3 pr-1">
              {isPreviewLoading ? (
                <div className="p-8 text-center bg-surface-container-lowest border-2 border-green-400 rounded-xl space-y-3 shadow-[2px_2px_0_#DEB9B5]">
                  <Loader2 className="w-6 h-6 text-green-700 animate-spin mx-auto" />
                  <p className="text-xs text-on-surface-variant font-medium">Đang trích xuất các phân đoạn Vector từ cơ sở dữ liệu...</p>
                </div>
              ) : previewError ? (
                <div className="bg-pink-100/90 border-2 border-pink-400 rounded-xl p-4 flex items-start gap-3 text-xs text-pink-900 shadow-[2px_2px_0_#DEB9B5]">
                  <AlertCircle className="w-5 h-5 text-pink-700 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-bold">Không thể tải trước phân đoạn:</p>
                    <p className="mt-0.5">{previewError}</p>
                  </div>
                </div>
              ) : previewData?.chunks && previewData.chunks.length > 0 ? (
                previewData.chunks.map((chunk, idx) => (
                  <div key={idx} className="bg-surface-container-lowest border-2 border-green-500 rounded-xl p-3.5 shadow-[2px_2px_0_#DEB9B5] space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-[11px] text-green-800 font-mono bg-green-100 px-2 py-0.5 rounded border border-green-300">
                        // {chunk.label || `Đoạn ${idx + 1}`}
                      </span>
                      {chunk.truncated && (
                        <span className="text-[10px] text-on-surface-variant font-mono bg-paper px-2 py-0.5 rounded border border-outline-variant">
                          Hiển thị rút gọn (300 ký tự)
                        </span>
                      )}
                    </div>
                    <p className="text-xs leading-relaxed text-ink/90 font-mono whitespace-pre-wrap bg-paper/60 p-2.5 rounded-lg border border-cream">
                      "{chunk.text}"
                    </p>
                  </div>
                ))
              ) : (
                <div className="p-8 text-center bg-surface-container-lowest border-2 border-green-400 rounded-xl space-y-2 shadow-[2px_2px_0_#DEB9B5]">
                  <BookOpen className="w-8 h-8 text-outline mx-auto" />
                  <p className="text-xs font-bold text-ink">Chưa có phân đoạn Vector nào</p>
                  <p className="text-[11px] text-on-surface-variant max-w-sm mx-auto">
                    Tài liệu này có thể đang trong quá trình bóc tách (INDEXING) hoặc chưa hoàn tất lập chỉ mục vào cơ sở dữ liệu pgvector.
                  </p>
                </div>
              )}
            </div>

            <div className="pt-3 border-t-2 border-cream text-right shrink-0">
              <button
                type="button"
                onClick={() => setIsChunksModalOpen(false)}
                className="px-5 py-2 rounded-full bg-paper border-2 border-green-500 text-ink font-semibold text-xs sm:text-sm shadow-[2px_2px_0_#DEB9B5] hover:bg-green-50 active:translate-x-[1px] active:translate-y-[1px] cursor-pointer transition-all"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
