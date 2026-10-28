import React from 'react';
import { Search, Filter, ArrowUpDown, X, RotateCcw, ChevronDown } from 'lucide-react';
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
    <div className="bg-white border border-pink-200/90 rounded-xl p-3.5 shadow-2xs flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3 text-xs mb-6">
      
      {/* Search Bar */}
      <div className="relative flex-1 min-w-[280px]">
        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Tìm theo họ tên, email @fpt.edu.vn hoặc MSSV / Mã nhân viên..."
          className="w-full pl-9 pr-8 py-2 bg-white rounded-lg border border-pink-200 text-ink text-xs placeholder:text-slate-400 focus:outline-none focus:border-pink-500 focus:ring-1 focus:ring-pink-500 transition-all shadow-2xs"
        />
        {keyword && (
          <button
            type="button"
            onClick={() => setKeyword('')}
            className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-700 p-0.5"
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
            className="appearance-none pl-3 pr-8 py-2 bg-white rounded-lg border border-pink-200 text-slate-800 font-medium text-xs focus:outline-none focus:border-pink-500 focus:ring-1 focus:ring-pink-500 shadow-2xs cursor-pointer"
            aria-label="Lọc theo vai trò"
          >
            <option value="">Tất cả vai trò</option>
            {ROLE_OPTIONS.map(({ code, description }) => (
              <option key={code} value={code}>{description} ({code})</option>
            ))}
          </select>
          <ChevronDown className="w-3.5 h-3.5 text-slate-400 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
        </div>

        {/* Status Filter */}
        <div className="relative">
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="appearance-none pl-3 pr-8 py-2 bg-white rounded-lg border border-pink-200 text-slate-800 font-medium text-xs focus:outline-none focus:border-pink-500 focus:ring-1 focus:ring-pink-500 shadow-2xs cursor-pointer"
            aria-label="Lọc theo trạng thái"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động (ACTIVE)</option>
            <option value="INACTIVE">Chưa kích hoạt (INACTIVE)</option>
            <option value="BANNED">Đang khóa (BANNED)</option>
            <option value="DELETED">Đã xóa mềm (DELETED)</option>
          </select>
          <ChevronDown className="w-3.5 h-3.5 text-slate-400 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
        </div>

        {/* Sort By */}
        <div className="flex items-center rounded-lg border border-pink-200 bg-white shadow-2xs overflow-hidden">
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="appearance-none pl-3 pr-2 py-2 bg-transparent font-medium text-slate-800 focus:outline-none text-xs cursor-pointer"
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
            className="p-2 border-l border-pink-200 text-slate-500 hover:text-ink hover:bg-slate-50 transition focus:outline-none cursor-pointer"
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
            className="px-3 py-2 flex items-center gap-1.5 rounded-lg bg-slate-100 border border-pink-200 text-slate-700 font-semibold text-xs hover:bg-slate-200 shadow-2xs transition active:scale-[0.98] cursor-pointer"
          >
            <RotateCcw className="w-3.5 h-3.5 text-slate-500" />
            <span>Đặt lại</span>
            <span className="w-4 h-4 rounded-full bg-slate-300 text-slate-800 text-[10px] font-bold flex items-center justify-center font-mono">
              {activeFiltersCount}
            </span>
          </button>
        )}
      </div>

    </div>
  );
}
