import React from 'react';
import { Search, Filter, ArrowUpDown, X, RotateCcw } from 'lucide-react';

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
    <div className="bg-cardBg border border-slate-200/90 rounded-2xl p-4 shadow-xs flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3 text-xs">
      
      {/* Search Bar */}
      <div className="relative flex-1">
        <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Tìm theo họ tên, email @fpt.edu.vn hoặc MSSV / Mã nhân viên..."
          className="w-full pl-10 pr-9 py-2.5 rounded-xl border border-slate-200 bg-slate-50/50 text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:bg-white transition"
        />
        {keyword && (
          <button
            type="button"
            onClick={() => setKeyword('')}
            className="absolute right-3 top-3 text-slate-400 hover:text-slate-600 p-0.5"
            aria-label="Xóa từ khóa"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {/* Filter Options */}
      <div className="flex flex-wrap items-center gap-2">
        {/* Role Filter */}
        <select
          value={role}
          onChange={(e) => setRole(e.target.value)}
          className="px-3 py-2 rounded-xl border border-slate-200 bg-white font-semibold text-slate-700 focus:ring-2 focus:ring-sky-500 focus:outline-none text-xs transition"
          aria-label="Lọc theo vai trò"
        >
          <option value="">Tất cả vai trò</option>
          <option value="ROLE_ADMIN">Quản Trị Viên (Admin)</option>
          <option value="ROLE_USER">Người Dùng (Lecturer / Student)</option>
        </select>

        {/* Status Filter */}
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="px-3 py-2 rounded-xl border border-slate-200 bg-white font-semibold text-slate-700 focus:ring-2 focus:ring-sky-500 focus:outline-none text-xs transition"
          aria-label="Lọc theo trạng thái"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động (ACTIVE)</option>
          <option value="INACTIVE">Chưa kích hoạt (INACTIVE)</option>
          <option value="BANNED">Đang khóa (BANNED)</option>
          <option value="DELETED">Đã xóa mềm (DELETED)</option>
        </select>

        {/* Sort By */}
        <div className="flex items-center rounded-xl border border-slate-200 bg-white overflow-hidden">
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="px-3 py-2 bg-transparent font-semibold text-slate-700 focus:outline-none text-xs"
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
            className="p-2 border-l border-slate-200 text-slate-500 hover:text-slate-900 hover:bg-slate-50 transition focus:outline-none focus:bg-slate-100"
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
            className="px-3 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold text-xs transition flex items-center gap-1.5 focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
            <RotateCcw className="w-3.5 h-3.5 text-slate-500" />
            <span>Đặt lại</span>
            <span className="w-4 h-4 rounded-full bg-sky-100 text-sky-800 text-[10px] font-bold flex items-center justify-center font-mono">
              {activeFiltersCount}
            </span>
          </button>
        )}
      </div>

    </div>
  );
}
