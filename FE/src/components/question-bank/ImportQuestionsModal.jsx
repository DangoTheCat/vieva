import React, { useState } from 'react';
import { X, FileSpreadsheet, Download, CheckCircle2, AlertCircle } from 'lucide-react';

export function ImportQuestionsModal({
  isOpen,
  onClose,
  onImport,
  isImporting = false,
  onDownloadTemplate,
  importReport = null
}) {
  const [file, setFile] = useState(null);
  const [isDryRun, setIsDryRun] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!file) return;
    onImport({ file, isDryRun });
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in" role="dialog">
      <div className="bg-paper rounded-2xl max-w-lg w-full border border-pink-200 shadow-md p-6 space-y-4 animate-modal-entry">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-pink-200 pb-3">
          <div className="flex items-center gap-2 text-pink-900 font-bold text-xs">
            <FileSpreadsheet className="w-4 h-4 text-pink-700" />
            <span className="font-heading uppercase tracking-wider">Nhập Câu Hỏi Hàng Loạt (UC1.6)</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isImporting}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          
          {/* Template Download */}
          <div className="flex items-center justify-between bg-pink-50 p-3 rounded-xl border border-pink-200 shadow-2xs">
            <span className="text-xs font-semibold text-pink-950">Tải tệp mẫu quy chuẩn:</span>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => onDownloadTemplate('xlsx')}
                className="px-2.5 py-1 bg-white border border-pink-200 hover:bg-pink-100 rounded-lg text-xs font-bold text-pink-800 inline-flex items-center gap-1 shadow-2xs cursor-pointer transition"
              >
                <Download className="w-3 h-3 text-pink-700" />
                <span>Excel (.xlsx)</span>
              </button>
              <button
                type="button"
                onClick={() => onDownloadTemplate('csv')}
                className="px-2.5 py-1 bg-white border border-pink-200 hover:bg-pink-100 rounded-lg text-xs font-bold text-pink-800 inline-flex items-center gap-1 shadow-2xs cursor-pointer transition"
              >
                <Download className="w-3 h-3 text-pink-700" />
                <span>CSV</span>
              </button>
            </div>
          </div>

          {/* File input */}
          <div>
            <label className="block text-xs font-bold text-ink mb-1">Chọn Tệp Tin Cần Nhập (.xlsx, .csv)</label>
            <input
              type="file"
              required
              disabled={isImporting}
              accept=".xlsx,.csv"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="w-full text-xs file:mr-3 file:py-1.5 file:px-3 file:rounded-xl file:border file:border-pink-200 file:text-xs file:font-bold file:bg-white file:text-pink-900 hover:file:bg-pink-50 cursor-pointer"
            />
          </div>

          {/* Dry Run Toggle */}
          <div className="flex items-center justify-between p-3 bg-white rounded-xl border border-pink-200 shadow-2xs">
            <div>
              <p className="text-xs font-bold text-ink">Chế Độ Thẩm Định Thử Nghiệm (Dry Run)</p>
              <p className="text-[10px] text-slate-500">Chỉ kiểm tra tính hợp lệ dữ liệu, chưa ghi vào cơ sở dữ liệu</p>
            </div>
            <input
              type="checkbox"
              disabled={isImporting}
              checked={isDryRun}
              onChange={(e) => setIsDryRun(e.target.checked)}
              className="w-4 h-4 text-pink-600 rounded focus:ring-pink-500 cursor-pointer"
            />
          </div>

          {/* Import Report Box */}
          {importReport && (
            <div className="p-3 bg-pink-50 rounded-xl border border-pink-200 shadow-2xs text-xs space-y-1 animate-modal-entry">
              <p className="font-bold text-pink-950">Báo Cáo Kết Quả Nhập:</p>
              <p className="text-slate-700">
                Tổng dòng đọc được: {importReport.totalRows || 0} ({importReport.totalQuestions || 0} câu hỏi)
              </p>
              <p className="text-emerald-700 font-semibold">Câu hỏi hợp lệ: {importReport.validQuestions || 0}</p>
              {!importReport.dryRun && (
                <p className="text-pink-900 font-semibold">Đã tạo thành công: {importReport.createdQuestions || 0} câu hỏi</p>
              )}
              {importReport.invalidQuestions > 0 && (
                <p className="text-rose-600 font-semibold">Câu hỏi lỗi: {importReport.invalidQuestions}</p>
              )}
              {importReport.errors?.length > 0 && (
                <ul className="mt-1 max-h-32 overflow-y-auto space-y-0.5 text-[11px] text-rose-600">
                  {importReport.errors.map((er, idx) => (
                    <li key={idx}>
                      Dòng {er.row}
                      {er.column ? ` • ${er.column}` : ''}
                      {er.questionRef ? ` • ${er.questionRef}` : ''}: {er.message}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}

          {/* Actions */}
          <div className="pt-2 flex justify-end gap-2 border-t border-pink-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isImporting}
              className="px-4 py-2 rounded-xl bg-white border border-pink-200 text-slate-700 text-xs font-bold shadow-2xs hover:bg-slate-50 transition cursor-pointer"
            >
              Đóng
            </button>
            <button
              type="submit"
              disabled={isImporting || !file}
              className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 flex items-center gap-2 cursor-pointer"
            >
              {isImporting && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
              <span>{isDryRun ? 'Thẩm Định File (Dry Run)' : 'Nhập Dữ Liệu Ngay'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
