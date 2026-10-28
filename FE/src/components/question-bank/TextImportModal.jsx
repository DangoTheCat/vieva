import React from 'react';
import { X, FileText } from 'lucide-react';
import { BLOOM_LEVELS } from './bloomConstants';

export function TextImportModal({
  isOpen,
  onClose,
  textImport,
  setTextImport,
  topics = [],
  rubricTemplates = [],
  onSubmit,
  isImportingText = false
}) {
  if (!isOpen || !textImport) return null;

  const questionCount = textImport.text ? textImport.text.split(/\r?\n/).filter((l) => l.trim()).length : 0;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <form onSubmit={onSubmit} className="bg-paper rounded-2xl max-w-2xl w-full border border-pink-200 shadow-md p-6 space-y-4 text-xs animate-modal-entry">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-pink-200 pb-3">
          <div className="flex items-center gap-2 text-pink-900 font-bold">
            <FileText className="w-4 h-4 text-pink-700" />
            <span className="font-heading uppercase tracking-wider">Nhập Câu Hỏi Từ Văn Bản (Mỗi Dòng Một Câu)</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Textarea */}
        <label className="block font-bold space-y-1">
          <span className="text-ink">Nội dung — mỗi dòng là một câu hỏi (tối đa 2.000 dòng, dòng trống tự động bỏ qua):</span>
          <textarea
            rows={8}
            required
            autoFocus
            value={textImport.text || ''}
            onChange={(e) => setTextImport({ ...textImport, text: e.target.value })}
            placeholder={'Trình bày nguyên lý Dependency Inversion.\nSo sánh Layered Architecture và Clean Architecture.\nGiải thích cách tính Cosine Similarity trong pgvector.'}
            className="w-full bg-white border border-pink-200 rounded-xl p-3 font-normal text-ink focus:outline-none focus:border-pink-500 shadow-2xs"
          />
        </label>

        <p className="text-slate-500 text-[11px]">
          Có <strong className="text-pink-900 font-bold">{questionCount}</strong> câu hỏi sẽ được tạo ở trạng thái nháp (DRAFT).
        </p>

        {/* Options */}
        <div className="grid sm:grid-cols-3 gap-3">
          <label className="block font-bold space-y-1">
            <span className="text-ink">Chủ đề (Tùy chọn)</span>
            <select
              value={textImport.topicId || ''}
              onChange={(e) => setTextImport({ ...textImport, topicId: e.target.value })}
              className="w-full bg-white border border-pink-200 rounded-xl p-2 font-normal focus:outline-none focus:border-pink-500 shadow-2xs cursor-pointer"
            >
              <option value="">— Không gán —</option>
              {topics.map((t) => (
                <option key={t.topicId} value={t.topicId}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>

          <label className="block font-bold space-y-1">
            <span className="text-ink">Mức Bloom (Tùy chọn)</span>
            <select
              value={textImport.bloomLevel || ''}
              onChange={(e) => setTextImport({ ...textImport, bloomLevel: e.target.value })}
              className="w-full bg-white border border-pink-200 rounded-xl p-2 font-normal focus:outline-none focus:border-pink-500 shadow-2xs cursor-pointer"
            >
              <option value="">— Không gán —</option>
              {BLOOM_LEVELS.map((b) => (
                <option key={b.value} value={b.value}>
                  {b.label}
                </option>
              ))}
            </select>
          </label>

          <label className="block font-bold space-y-1">
            <span className="text-ink">Mẫu Rubric (Tùy chọn)</span>
            <select
              value={textImport.rubricTemplateId || ''}
              onChange={(e) => setTextImport({ ...textImport, rubricTemplateId: e.target.value })}
              className="w-full bg-white border border-pink-200 rounded-xl p-2 font-normal focus:outline-none focus:border-pink-500 shadow-2xs cursor-pointer"
            >
              <option value="">— Không áp dụng —</option>
              {rubricTemplates.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-2 pt-3 border-t border-pink-100">
          <button
            type="button"
            onClick={onClose}
            disabled={isImportingText}
            className="px-4 py-2 rounded-xl bg-white border border-pink-200 text-slate-700 font-bold hover:bg-slate-50 transition cursor-pointer"
          >
            Hủy Bỏ
          </button>
          <button
            type="submit"
            disabled={isImportingText || !textImport.text?.trim()}
            className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold shadow-xs disabled:opacity-50 flex items-center gap-2 cursor-pointer"
          >
            {isImportingText && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
            <span>Nhập Câu Hỏi</span>
          </button>
        </div>
      </form>
    </div>
  );
}
