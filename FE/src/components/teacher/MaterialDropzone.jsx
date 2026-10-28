import React, { useState, useRef } from 'react';
import { UploadCloud, FileText, X, CheckCircle2, AlertCircle } from 'lucide-react';

const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'text/plain'
];

const ALLOWED_EXTENSIONS = ['pdf', 'docx', 'pptx', 'txt'];
const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB

export function MaterialDropzone({
  file,
  onFileSelect,
  onFileRemove,
  isUploading,
  disabled = false
}) {
  const [isDragOver, setIsDragOver] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  const validateAndSetFile = (selectedFile) => {
    setError(null);
    if (!selectedFile) return;

    const fileExt = selectedFile.name.split('.').pop()?.toLowerCase();
    const isExtensionValid = ALLOWED_EXTENSIONS.includes(fileExt);
    const isMimeValid = ALLOWED_MIME_TYPES.includes(selectedFile.type) || isExtensionValid;

    if (!isMimeValid) {
      setError(`Định dạng tệp không được hỗ trợ (.${fileExt}). Vui lòng tải lên file PDF, DOCX, PPTX hoặc TXT.`);
      return;
    }

    if (selectedFile.size > MAX_FILE_SIZE) {
      setError(`Kích thước tệp vượt quá giới hạn 20MB (${(selectedFile.size / (1024 * 1024)).toFixed(1)}MB).`);
      return;
    }

    onFileSelect(selectedFile);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    if (disabled || isUploading) return;
    setIsDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    if (disabled || isUploading) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileInputChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      validateAndSetFile(e.target.files[0]);
    }
  };

  return (
    <div className="space-y-3">
      {/* Hidden native input */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf,.docx,.pptx,.txt"
        onChange={handleFileInputChange}
        className="hidden"
        disabled={disabled || isUploading}
      />

      {!file ? (
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => !disabled && !isUploading && fileInputRef.current?.click()}
          className={`border border-dashed rounded-xl p-6 text-center transition-all cursor-pointer select-none ${
            isDragOver
              ? 'bg-pink-50/70 border-blue-500 scale-[1.01] shadow-xs'
              : 'bg-slate-50/60 hover:bg-slate-50 border-pink-200 hover:border-slate-400 shadow-2xs'
          } ${disabled || isUploading ? 'opacity-60 cursor-not-allowed' : ''}`}
        >
          <div className="flex flex-col items-center justify-center space-y-3">
            <div className={`w-12 h-12 rounded-xl border flex items-center justify-center transition-all ${
              isDragOver
                ? 'bg-blue-100 border-blue-300 text-pink-700 shadow-2xs'
                : 'bg-white border-pink-200 text-slate-600 shadow-2xs'
            }`}>
              <UploadCloud className="w-6 h-6" />
            </div>

            <div>
              <p className="text-xs sm:text-sm font-semibold text-slate-800">
                Kéo thả tệp học liệu vào đây, hoặc{' '}
                <span className="text-pink-600 underline underline-offset-2 hover:text-pink-700 font-semibold">
                  chọn từ máy tính
                </span>
              </p>
              <p className="text-[11px] text-slate-500 mt-1">
                Tài liệu giáo trình sẽ được bóc tách và lập chỉ mục Vector RAG phục vụ đề thi
              </p>
            </div>

            {/* Allowed formats tags */}
            <div className="flex flex-wrap items-center justify-center gap-1.5 pt-1">
              {['PDF', 'DOCX', 'PPTX', 'TXT'].map((ext) => (
                <span
                  key={ext}
                  className="px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-slate-100 text-slate-700 border border-pink-200"
                >
                  .{ext.toLowerCase()}
                </span>
              ))}
              <span className="text-[10px] font-medium text-slate-400 font-mono ml-1">
                (Tối đa 20 MB)
              </span>
            </div>
          </div>
        </div>
      ) : (
        /* Selected file info card */
        <div className="bg-white border border-pink-200 rounded-xl p-3.5 shadow-2xs flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-10 h-10 rounded-lg bg-slate-100 border border-pink-200 flex items-center justify-center text-slate-700 shadow-2xs shrink-0">
              <FileText className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <p className="text-xs sm:text-sm font-semibold text-ink truncate max-w-xs sm:max-w-md">
                  {file.name}
                </p>
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              </div>
              <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-500 font-mono">
                <span>{(file.size / (1024 * 1024)).toFixed(2)} MB</span>
                <span>•</span>
                <span className="uppercase text-slate-700 font-semibold">
                  {file.name.split('.').pop()}
                </span>
                <span>•</span>
                <span className="text-emerald-700 font-medium">Sẵn sàng lập chỉ mục</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-1 shrink-0">
            <button
              type="button"
              disabled={isUploading}
              onClick={() => {
                setError(null);
                onFileRemove();
              }}
              className="p-1.5 rounded-lg border border-pink-200 hover:bg-rose-50 text-slate-400 hover:text-rose-600 transition cursor-pointer disabled:opacity-50"
              title="Gỡ tệp này"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Error alert message */}
      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-lg p-2.5 flex items-start gap-2 text-xs text-rose-800 shadow-2xs animate-modal-entry">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}
    </div>
  );
}
