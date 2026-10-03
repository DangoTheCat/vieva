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

