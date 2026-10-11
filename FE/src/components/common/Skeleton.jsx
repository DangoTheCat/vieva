import React from 'react';

export function TableSkeletonRows({ rows = 5, cols = 6 }) {
  return (
    <>
      {Array.from({ length: rows }).map((_, rIdx) => (
        <tr key={rIdx} className="animate-pulse border-b border-slate-100">
          {Array.from({ length: cols }).map((_, cIdx) => (
            <td key={cIdx} className="py-4 px-4">
              <div className="h-4 bg-slate-200/80 rounded-md w-full max-w-[120px]"></div>
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}

export function SkeletonCard() {
  return (
    <div className="p-4 bg-white rounded-2xl border border-slate-200/80 shadow-xs animate-pulse space-y-3">
      <div className="h-4 bg-slate-200 rounded w-1/3"></div>
      <div className="h-8 bg-slate-200 rounded w-1/2"></div>
      <div className="h-3 bg-slate-200 rounded w-full"></div>
    </div>
  );
}

export const CardSkeleton = SkeletonCard;

export function SkeletonTable({ rows = 4, columns = 5 }) {
  return (
    <div className="w-full space-y-3 animate-pulse">
      <div className="h-8 bg-slate-100 rounded-xl w-full"></div>
      {Array.from({ length: rows }).map((_, idx) => (
        <div key={idx} className="flex gap-4 items-center py-3 border-b border-slate-100">
          {Array.from({ length: columns }).map((_, cIdx) => (
            <div key={cIdx} className="h-4 bg-slate-200/80 rounded w-full"></div>
          ))}
        </div>
      ))}
    </div>
  );
}

export function PageSkeleton() {
  return (
    <div className="space-y-6 max-w-7xl mx-auto w-full animate-pulse text-slate-800">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-6 bg-white border border-slate-200/80 rounded-2xl">
        <div className="space-y-2 w-full max-w-sm">
          <div className="h-6 bg-slate-200 rounded-lg w-3/4"></div>
          <div className="h-3.5 bg-slate-100 rounded-md w-full"></div>
        </div>
        <div className="h-9 bg-slate-200 rounded-xl w-32"></div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="p-5 bg-white rounded-2xl border border-slate-200/80 space-y-3">
            <div className="h-3.5 bg-slate-100 rounded w-24"></div>
            <div className="h-7 bg-slate-200 rounded w-16"></div>
            <div className="h-3 bg-slate-100 rounded w-32"></div>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 p-6 space-y-4">
        <div className="h-5 bg-slate-200 rounded w-48"></div>
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-12 bg-slate-50 border border-slate-100 rounded-xl"></div>
          ))}
        </div>
      </div>
    </div>
  );
}

