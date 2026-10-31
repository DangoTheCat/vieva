import React from 'react';
import { SkeletonTable } from '../../common/Skeleton';
import { AlertCircle, CheckCircle2, Clock, FileText, RefreshCw, RotateCcw, Trash2, Upload } from 'lucide-react';

/** Question bank page — "documents" tab. Extracted from LecturerQuestionBankPage (plan item 3.7). */
export function DocumentsTab({ docFileToUpload, documents, handleRetryIndexing, handleUploadDocument, isLoadingDocs, isUploadingDoc, loadDocuments, setDeleteDocTarget, setDocFileToUpload }) {
  return (
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
  );
}
