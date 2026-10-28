import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export function Pagination({
  page = 0,
  size = 10,
  totalPages = 1,
  totalElements = 0,
  onPageChange,
  onSizeChange,
  sizeOptions = [5, 10, 20, 50],
  itemLabel = 'mục'
}) {
  if (totalElements === 0) return null;

  const currentPage = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));
  const from = totalElements === 0 ? 0 : currentPage * size + 1;
  const to = Math.min((currentPage + 1) * size, totalElements);

  // Generate page numbers with intelligent ellipsis
  const getPageNumbers = () => {
    const pages = [];
    const delta = 1;
    const left = Math.max(0, currentPage - delta);
    const right = Math.min(totalPages - 1, currentPage + delta);

    for (let i = 0; i < totalPages; i++) {
      if (i === 0 || i === totalPages - 1 || (i >= left && i <= right)) {
        pages.push(i);
      } else if (pages[pages.length - 1] !== '...') {
        pages.push('...');
      }
    }
    return pages;
  };

  return (
    <nav
      aria-label="Phân trang danh sách"
      className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3.5 rounded-xl border border-pink-200 shadow-2xs mt-4"
    >
      {/* Information text */}
      <div className="flex items-center gap-2 text-xs font-medium text-slate-600 flex-wrap">
        <span>
          Hiển thị <span className="font-mono font-semibold text-ink">{from}</span> - <span className="font-mono font-semibold text-ink">{to}</span> trong <span className="font-mono font-bold text-ink">{totalElements}</span> {itemLabel}
        </span>

        {/* Page size selector */}
        {onSizeChange && (
          <div className="flex items-center gap-1.5 ml-1 sm:ml-2 sm:pl-3 sm:border-l sm:border-pink-200">
            <span className="text-[11px] text-slate-500">Hiển thị:</span>
            <select
              value={size}
              onChange={(e) => onSizeChange(Number(e.target.value))}
              className="bg-white border border-pink-200 rounded-lg px-2 py-0.5 text-xs font-medium text-slate-800 focus:outline-none focus:border-pink-500 shadow-2xs cursor-pointer"
            >
              {sizeOptions.map(opt => (
                <option key={opt} value={opt}>{opt} / trang</option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Page navigation controls */}
      <div className="flex items-center gap-1.5">
        {/* Previous page */}
        <button
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 0}
          className="px-2.5 py-1 rounded-lg text-xs font-medium border border-pink-200 bg-white text-slate-700 shadow-2xs hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition flex items-center gap-1 cursor-pointer"
          title="Trang trước"
        >
          <ChevronLeft className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Trước</span>
        </button>

        {/* Page buttons */}
        <div className="flex items-center gap-1">
          {getPageNumbers().map((p, idx) => {
            if (p === '...') {
              return (
                <span key={`dots-${idx}`} className="px-1.5 py-1 text-xs text-slate-400">
                  ...
                </span>
              );
            }
            const isCurrent = p === currentPage;
            return (
              <button
                key={p}
                type="button"
                onClick={() => onPageChange(p)}
                className={`w-7 h-7 sm:w-8 sm:h-8 rounded-lg text-xs font-semibold transition flex items-center justify-center cursor-pointer ${
                  isCurrent
                    ? 'bg-pink-600 text-white border border-slate-900 shadow-xs'
                    : 'bg-white text-slate-700 border border-pink-200 hover:bg-slate-50 shadow-2xs'
                }`}
              >
                {p + 1}
              </button>
            );
          })}
        </div>

        {/* Next page */}
        <button
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          className="px-2.5 py-1 rounded-lg text-xs font-medium border border-pink-200 bg-white text-slate-700 shadow-2xs hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition flex items-center gap-1 cursor-pointer"
          title="Trang sau"
        >
          <span className="hidden sm:inline">Sau</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </nav>
  );
}
