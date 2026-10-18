import React from 'react';
import { Search, Filter, ArrowUpDown, X, RotateCcw } from 'lucide-react';
import { ROLE_OPTIONS } from '../../utils/roles';

export function UserFilterBar({
  keyword,
  setKeyword,
  role,
  setRole,
  status,
  setStatus,
  sortBy,
  setSortBy,
  sortDirection,
  setSortDirection,
  onReset
}) {
  const activeFiltersCount = [
    Boolean(keyword.trim()),
    Boolean(role),
    Boolean(status),
    sortBy !== 'createdAt',
    sortDirection !== 'DESC'
  ].filter(Boolean).length;

  return (
    <div className="bg-paper border-2 border-green-500 rounded-2xl p-4 shadow-[3px_3px_0px_#DEB9B5] flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3 text-xs mb-6">
      
      {/* Search Bar */}
      <div className="relative flex-1 min-w-[280px]">
        <Search className="w-4 h-4 text-green-700 absolute left-3.5 top-3 pointer-events-none" />
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Tìm theo họ tên, email @fpt.edu.vn hoặc MSSV / Mã nhân viên..."
          className="w-full pl-10 pr-9 py-2.5 bg-surface-container-lowest rounded-xl border-2 border-green-500 text-ink text-xs font-sans placeholder:text-outline focus:outline-none focus:border-green-700 focus:shadow-[2px_2px_0px_#DEB9B5] transition-all"
        />
        {keyword && (
          <button
            type="button"
            onClick={() => setKeyword('')}
            className="absolute right-3 top-3 text-pink-700 hover:text-pink-900 p-0.5"
            aria-label="Xóa từ khóa"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {/* Filter Options */}
      <div className="flex flex-wrap items-center gap-2">
        {/* Role Filter */}
        <div className="relative">
          <select
            value={role}
            onChange={(e) => setRole(e.target.value)}
            className="appearance-none pl-3 pr-8 py-2.5 bg-surface-container-lowest rounded-xl border-2 border-green-500 text-ink font-semibold text-xs focus:outline-none focus:border-green-700 shadow-[2px_2px_0px_#DEB9B5] cursor-pointer"
            aria-label="Lọc theo vai trò"
          >
            <option value="">Tất cả vai trò</option>
            {ROLE_OPTIONS.map(({ code, description }) => (
              <option key={code} value={code}>{description} ({code})</option>
            ))}
          </select>
          <span className="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none text-green-700 text-[18px]">expand_more</span>
        </div>

        {/* Status Filter */}
        <div className="relative">
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="appearance-none pl-3 pr-8 py-2.5 bg-surface-container-lowest rounded-xl border-2 border-green-500 text-ink font-semibold text-xs focus:outline-none focus:border-green-700 shadow-[2px_2px_0px_#DEB9B5] cursor-pointer"
            aria-label="Lọc theo trạng thái"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động (ACTIVE)</option>
            <option value="INACTIVE">Chưa kích hoạt (INACTIVE)</option>
            <option value="BANNED">Đang khóa (BANNED)</option>
            <option value="DELETED">Đã xóa mềm (DELETED)</option>
          </select>
          <span className="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none text-green-700 text-[18px]">expand_more</span>
        </div>

        {/* Sort By */}
        <div className="flex items-center rounded-xl border-2 border-green-500 bg-surface-container-lowest shadow-[2px_2px_0px_#DEB9B5] overflow-hidden">
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="appearance-none pl-3 pr-2 py-2.5 bg-transparent font-semibold text-ink focus:outline-none text-xs cursor-pointer"
            aria-label="Sắp xếp theo trường"
          >
            <option value="createdAt">Thời gian tạo</option>
            <option value="fullName">Họ và tên</option>
            <option value="email">Email</option>
            <option value="status">Trạng thái</option>
          </select>
          <button
            type="button"
            onClick={() => setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')}
            className="p-2.5 border-l-2 border-green-500 text-green-800 hover:text-ink hover:bg-green-100 transition focus:outline-none cursor-pointer"
            title={`Sắp xếp: ${sortDirection === 'ASC' ? 'Tăng dần' : 'Giảm dần'}`}
            aria-label={`Sắp xếp ${sortDirection === 'ASC' ? 'Tăng dần' : 'Giảm dần'}`}
          >
            <ArrowUpDown className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Reset filter button if filters are active */}
        {activeFiltersCount > 0 && (
          <button
            type="button"
            onClick={onReset}
            className="px-4 py-2.5 flex items-center gap-1.5 rounded-xl bg-pink-100 border-2 border-green-500 text-pink-800 font-semibold text-xs hover:bg-pink-200 shadow-[2px_2px_0px_#DEB9B5] active:translate-x-[1px] active:translate-y-[1px] transition-all cursor-pointer"
          >
            <RotateCcw className="w-3.5 h-3.5 text-pink-700" />
            <span>Đặt lại bộ lọc</span>
            <span className="w-4 h-4 rounded-full bg-pink-300 text-pink-900 text-[10px] font-bold flex items-center justify-center font-mono">
              {activeFiltersCount}
            </span>
          </button>
        )}
      </div>

    </div>
  );
}
