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
  itemLabel = 'câu hỏi'
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
      className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-paper p-3.5 rounded-2xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] mt-4"
    >
      {/* Information text */}
      <div className="flex items-center gap-2 text-xs font-semibold text-green-900 flex-wrap">
        <span>
          Hiển thị <span className="font-mono font-bold text-ink">{from}</span> - <span className="font-mono font-bold text-ink">{to}</span> / <span className="font-mono font-bold text-pink-900">{totalElements}</span> {itemLabel}
        </span>

        {/* Page size selector */}
        {onSizeChange && (
          <div className="flex items-center gap-1.5 ml-1 sm:ml-2 sm:pl-3 sm:border-l-2 sm:border-cream">
            <span className="text-[11px] text-green-800">Số lượng:</span>
            <select
              value={size}
              onChange={(e) => onSizeChange(Number(e.target.value))}
              className="bg-surface-container-lowest border-2 border-green-500 rounded-lg px-2 py-0.5 text-xs font-bold text-ink focus:outline-none focus:border-green-600 shadow-[1px_1px_0_#DEB9B5] cursor-pointer"
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
          className="px-2.5 py-1 rounded-xl text-xs font-bold border-2 border-green-500 bg-paper text-ink shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 disabled:opacity-40 disabled:cursor-not-allowed transition flex items-center gap-1"
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
                <span key={`dots-${idx}`} className="px-1.5 py-1 text-xs font-bold text-green-700">
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
                className={`w-7 h-7 sm:w-8 sm:h-8 rounded-xl text-xs font-bold transition flex items-center justify-center ${
                  isCurrent
                    ? 'bg-pink-500 text-ink border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]'
                    : 'bg-paper text-ink border-2 border-green-300 hover:border-green-500 hover:bg-green-100'
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
          className="px-2.5 py-1 rounded-xl text-xs font-bold border-2 border-green-500 bg-paper text-ink shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 disabled:opacity-40 disabled:cursor-not-allowed transition flex items-center gap-1"
          title="Trang sau"
        >
          <span className="hidden sm:inline">Sau</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </nav>
  );
}
