import React, { useState, useRef } from 'react';
import { UploadCloud, FileText, CheckCircle2, AlertCircle, X, Sparkles } from 'lucide-react';

const ACCEPTED_EXTENSIONS = ['.pdf', '.docx', '.pptx', '.txt'];
const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB

export function MaterialDropzone({
  file,
  onFileSelect,
  onFileRemove,
  isUploading,
  disabled
}) {
  const [isDragOver, setIsDragOver] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  const validateAndSelect = (selectedFile) => {
    setError(null);
    if (!selectedFile) return;

    // Check file extension
    const extension = '.' + selectedFile.name.split('.').pop().toLowerCase();
    if (!ACCEPTED_EXTENSIONS.includes(extension)) {
      setError(`Định dạng "${extension}" không được hỗ trợ. Chỉ chấp nhận PDF, DOCX, PPTX, TXT.`);
      return;
    }

    // Check size (< 20MB)
    if (selectedFile.size > MAX_FILE_SIZE) {
      setError(`Kích thước tệp (${(selectedFile.size / (1024 * 1024)).toFixed(1)} MB) vượt quá giới hạn tối đa 20 MB.`);
      return;
    }

    onFileSelect(selectedFile);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (disabled || isUploading) return;
    setIsDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
    if (disabled || isUploading) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      validateAndSelect(e.dataTransfer.files[0]);
    }
  };

  const handleFileInputChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      validateAndSelect(e.target.files[0]);
    }
  };

  return (
    <div className="space-y-3">
      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept={ACCEPTED_EXTENSIONS.join(',')}
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
          className={`border-2 border-dashed rounded-xl p-6 text-center transition-all cursor-pointer select-none ${
            isDragOver
              ? 'bg-green-100/70 border-green-600 scale-[1.01] shadow-[4px_4px_0_#DEB9B5]'
              : 'bg-paper/70 hover:bg-green-50/50 border-green-400 hover:border-green-600 shadow-[2px_2px_0_#DEB9B5]'
          } ${disabled || isUploading ? 'opacity-60 cursor-not-allowed' : ''}`}
        >
          <div className="flex flex-col items-center justify-center space-y-3">
            <div className={`w-12 h-12 rounded-xl border-2 flex items-center justify-center transition-all ${
              isDragOver
                ? 'bg-green-200 border-green-700 text-green-900 shadow-[3px_3px_0_#DEB9B5]'
                : 'bg-pink-100 border-green-500 text-pink-700 shadow-[2px_2px_0_#DEB9B5]'
            }`}>
              <UploadCloud className="w-6 h-6 animate-pulse" />
            </div>

            <div>
              <p className="text-xs sm:text-sm font-bold text-ink">
                Kéo thả tệp học liệu vào đây, hoặc{' '}
                <span className="text-pink-700 underline underline-offset-2 hover:text-pink-900 font-extrabold">
                  duyệt từ máy tính
                </span>
              </p>
              <p className="text-[11px] text-on-surface-variant mt-1">
                Tài liệu giáo trình sẽ được bóc tách bằng Apache Tika và lập chỉ mục Vector RAG
              </p>
            </div>

            {/* Allowed formats tags */}
            <div className="flex flex-wrap items-center justify-center gap-1.5 pt-1">
              {['PDF', 'DOCX', 'PPTX', 'TXT'].map((ext) => (
                <span
                  key={ext}
                  className="px-2 py-0.5 rounded-full text-[10px] font-bold font-mono bg-pink-100 text-pink-900 border border-pink-300 shadow-[1px_1px_0_#DEB9B5]"
                >
                  .{ext.toLowerCase()}
                </span>
              ))}
              <span className="text-[10px] font-medium text-on-surface-variant font-mono ml-1">
                (Tối đa 20 MB)
              </span>
            </div>
          </div>
        </div>
      ) : (
        /* Selected file info card */
        <div className="bg-surface-container-lowest border-2 border-green-500 rounded-xl p-3.5 shadow-[3px_3px_0_#DEB9B5] flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-10 h-10 rounded-lg bg-pink-100 border-2 border-green-500 flex items-center justify-center text-pink-700 shadow-[2px_2px_0_#DEB9B5] shrink-0">
              <FileText className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <p className="text-xs sm:text-sm font-bold text-ink truncate max-w-xs sm:max-w-md">
                  {file.name}
                </p>
                <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0" />
              </div>
              <div className="flex items-center gap-2 mt-0.5 text-[11px] text-on-surface-variant font-mono">
                <span>{(file.size / (1024 * 1024)).toFixed(2)} MB</span>
                <span>•</span>
                <span className="uppercase text-pink-800 font-bold">
                  {file.name.split('.').pop()}
                </span>
                <span>•</span>
                <span className="text-green-700 font-semibold">Sẵn sàng lập chỉ mục</span>
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
              className="p-1.5 rounded-lg border-2 border-pink-300 bg-pink-50 hover:bg-pink-100 text-pink-700 shadow-[1px_1px_0_#DEB9B5] active:translate-x-[1px] active:translate-y-[1px] active:shadow-none transition-all cursor-pointer disabled:opacity-50"
              title="Gỡ tệp này"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Error alert message */}
      {error && (
        <div className="bg-pink-100/90 border-2 border-pink-400 rounded-lg p-2.5 flex items-start gap-2 text-xs text-pink-900 shadow-[2px_2px_0_#DEB9B5] animate-modal-entry">
          <AlertCircle className="w-4 h-4 text-pink-700 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}
    </div>
  );
}
