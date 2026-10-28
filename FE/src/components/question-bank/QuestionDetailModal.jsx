import React from 'react';
import { X } from 'lucide-react';
import { SkeletonCard } from '../common/Skeleton';

export function QuestionDetailModal({ questionDetail, onClose }) {
  if (!questionDetail) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in" role="dialog" aria-modal="true">
      <div className="bg-paper rounded-2xl max-w-2xl w-full border border-pink-200 shadow-md p-6 space-y-4 animate-modal-entry max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-pink-200 pb-3">
          <h3 className="font-heading font-extrabold text-sm text-ink">
            Chi Tiết Câu Hỏi {questionDetail.detail?.questionCode || ''}
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {questionDetail.isLoading ? (
          <SkeletonCard />
        ) : (
          <div className="space-y-4 text-xs">
            <div className="flex flex-wrap gap-2">
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-pink-200 shadow-2xs">
                {questionDetail.detail?.status}
              </span>
              {questionDetail.detail?.pendingDraft && (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-800 border border-rose-200">
                  Bản nháp đang chờ: v{questionDetail.detail.pendingDraft.versionNumber}
                </span>
              )}
            </div>

            {questionDetail.detail?.currentVersion && (
              <div className="p-4 bg-pink-50 rounded-xl border border-pink-200 shadow-2xs space-y-2">
                <p className="text-[10px] font-bold text-pink-900 uppercase tracking-wider">
                  Phiên bản hiện hành v{questionDetail.detail.currentVersion.versionNumber}
                </p>
                <p className="font-semibold text-ink leading-relaxed font-heading text-sm">
                  {questionDetail.detail.currentVersion.content}
                </p>
                <p className="text-slate-600 leading-relaxed">
                  {questionDetail.detail.currentVersion.expectedAnswer}
                </p>
              </div>
            )}

            <div>
              <p className="text-[10px] font-bold text-pink-900 uppercase tracking-wider mb-2">
                Lịch Sử Phiên Bản ({questionDetail.history?.length || 0})
              </p>
              <div className="divide-y divide-pink-100 border border-pink-200 rounded-xl overflow-hidden bg-white shadow-2xs">
                {questionDetail.history?.map((h) => (
                  <div key={h.versionId} className="p-3.5 flex items-start justify-between gap-3 hover:bg-pink-50/50 transition">
                    <div className="space-y-1 min-w-0">
                      <p className="font-bold text-ink">
                        v{h.versionNumber} • {h.origin} • {h.bloomLevel}
                      </p>
                      <p className="text-slate-600 line-clamp-2">{h.content}</p>
                      {h.rejectReason && <p className="text-rose-600 font-medium">Lý do từ chối: {h.rejectReason}</p>}
                    </div>
                    <div className="text-right shrink-0">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
                        {h.status}
                      </span>
                      <p className="text-[10px] text-slate-400 mt-1 font-mono">
                        {h.createdAt ? new Date(h.createdAt).toLocaleString('vi-VN') : ''}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
