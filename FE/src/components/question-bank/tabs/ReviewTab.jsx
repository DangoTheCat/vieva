import React from 'react';
import { ALL_BLOOM_LEVELS } from '../bloomConstants';
import { Pagination } from '../../common/Pagination';
import { SkeletonCard } from '../../common/Skeleton';
import { AlertCircle, BookOpen, Check, CheckCircle2, Edit3, RefreshCw, Sparkles, Trash2 } from 'lucide-react';

/** Question bank page — "review" tab. Extracted from LecturerQuestionBankPage (plan item 3.7). */
export function ReviewTab({ handleApproveVersion, handleBatchApprove, handleConfirmBloom, handleOpenEditDraft, handleReviewPageChange, handleReviewPageSizeChange, isBatchApproving, isLoadingReview, loadReviewQueue, reviewPage, reviewPageSize, reviewStatusFilter, reviewTotalElements, reviewTotalPages, reviewVersions, selectedVersionIds, setDeleteDraftTarget, setIsAiGenModalOpen, setRegenFeedback, setRegenVersionTarget, setRejectReason, setRejectVersionTarget, setReviewPage, setReviewStatusFilter, setSelectedVersionIds }) {
  return (
    <div className="space-y-6">
      {/* AI RAG GENERATION TRIGGER BANNER */}
      <div className="relative overflow-hidden bg-pink-100 border border-pink-200 rounded-2xl p-6 shadow-md flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="relative z-10 space-y-1.5 max-w-xl">
          <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-paper border border-pink-200 text-pink-800 text-[10px] font-bold uppercase tracking-wider shadow-sm">
            <Sparkles className="w-3.5 h-3.5 text-pink-600" />
            <span>Trình Khởi Tạo Câu Hỏi Tự Động (AI RAG Engine)</span>
          </div>
          <h2 className="text-lg font-heading font-extrabold text-ink tracking-tight">
            Sinh Câu Hỏi Tự Động Từ Giáo Trình Đã Vector Hóa
          </h2>
          <p className="text-xs text-on-surface-variant leading-relaxed">
            AI Agent truy xuất các đoạn ngữ cảnh (chunks) chính xác từ kho học liệu đã đối soát, tự động cấu trúc hóa câu hỏi theo thang tư duy Bloom 6 cấp độ và sinh kèm ma trận Rubric chấm điểm chuẩn xác.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setIsAiGenModalOpen(true)}
          className="relative z-10 px-4 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] flex items-center gap-2 shrink-0"
        >
          <Sparkles className="w-4 h-4 text-ink" />
          <span>Thiết Lập &amp; Kích Hoạt RAG</span>
        </button>
      </div>

      {/* STATUS FILTER FOR QUEUE */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2 bg-paper p-3.5 rounded-2xl border border-pink-200 shadow-sm">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs font-bold text-ink">Lọc Theo Trạng Thái:</span>
          <div className="flex items-center gap-1.5">
            {['DRAFT', 'APPROVED', 'REJECTED', 'ALL'].map(st => (
              <button
                key={st}
                type="button"
                onClick={() => {
                  setReviewStatusFilter(st);
                  setReviewPage(0);
                  loadReviewQueue(0, undefined, st);
                }}
                className={`px-3 py-1 rounded-xl text-xs font-bold border-2 transition ${
                  reviewStatusFilter === st
                    ? 'bg-pink-600 text-white border-slate-900 shadow-xs'
                    : 'bg-white text-slate-600 border-transparent hover:border-pink-200'
                }`}
              >
                {st}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto">
          {reviewVersions.filter(v => v.status === 'DRAFT').length > 0 && (
            <label className="flex items-center gap-2 px-2.5 py-1 rounded-xl bg-green-50 border border-pink-200 shadow-sm cursor-pointer select-none text-xs font-bold text-ink">
              <input
                type="checkbox"
                checked={
                  reviewVersions.filter(v => v.status === 'DRAFT').length > 0 &&
                  reviewVersions.filter(v => v.status === 'DRAFT').every(v => selectedVersionIds.includes(v.versionId))
                }
                onChange={(e) => {
                  const approvable = reviewVersions.filter(v => v.status === 'DRAFT');
                  if (e.target.checked) {
                    const newIds = Array.from(new Set([...selectedVersionIds, ...approvable.map(v => v.versionId)]));
                    setSelectedVersionIds(newIds);
                  } else {
                    const approvableIds = new Set(approvable.map(v => v.versionId));
                    setSelectedVersionIds(prev => prev.filter(id => !approvableIds.has(id)));
                  }
                }}
                className="w-4 h-4 rounded border border-pink-200 text-pink-600 focus:ring-green-400 accent-pink-600 cursor-pointer"
              />
              <span>Chọn tất cả ({reviewVersions.filter(v => v.status === 'DRAFT').length})</span>
            </label>
          )}

          <button
            type="button"
            onClick={loadReviewQueue}
            className="p-1.5 text-green-700 hover:text-green-900 rounded-lg hover:bg-green-100 transition"
            title="Làm mới hàng đợi"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* REVIEW QUEUE LIST */}
      {isLoadingReview ? (
        <div className="space-y-4">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : reviewVersions.length === 0 ? (
        <div className="bg-paper p-12 rounded-2xl border border-pink-200 shadow-md text-center">
          <Sparkles className="w-12 h-12 text-pink-600 mx-auto mb-3 opacity-60" />
          <h3 className="font-heading font-bold text-ink text-sm">Hàng đợi đang trống</h3>
          <p className="text-xs text-on-surface-variant mt-1 max-w-sm mx-auto">
            Không có bản nháp nào cần thẩm định. Bạn có thể kích hoạt RAG AI để tạo các câu hỏi mới.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {reviewVersions.map((v) => {
            const bloomMeta = ALL_BLOOM_LEVELS.find(b => b.value === v.bloomLevel) || ALL_BLOOM_LEVELS[0];

            return (
              <article key={v.versionId} className="bg-paper border border-pink-200 rounded-2xl p-5 shadow-md space-y-4">

                {/* Header Bar */}
                <div className="flex flex-wrap items-center justify-between gap-2 border-b-2 border-cream pb-3">
                  <div className="flex flex-wrap items-center gap-2">
                    {v.status === 'DRAFT' && (
                      <input
                        type="checkbox"
                        checked={selectedVersionIds.includes(v.versionId)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedVersionIds(prev => [...prev, v.versionId]);
                          } else {
                            setSelectedVersionIds(prev => prev.filter(id => id !== v.versionId));
                          }
                        }}
                        className="w-4 h-4 rounded border border-pink-200 text-pink-600 focus:ring-green-400 accent-pink-600 cursor-pointer"
                        title="Chọn câu hỏi này để duyệt hàng loạt"
                      />
                    )}
                    <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border border-pink-200 shadow-sm">
                      {v.questionCode || 'DRAFT-VER'}
                    </span>
                    <span className="text-xs font-semibold text-green-800">
                      {v.topicName || 'Chủ đề học phần'}
                    </span>
                    <span className="text-[10px] px-2 py-0.5 rounded-md font-mono font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
                      Origin: {v.origin || 'MANUAL'}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    {/* Bloom Confirmation Badge */}
                    {v.bloomConfirmed ? (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-pink-200 shadow-sm">
                        <Check className="w-3 h-3 text-green-700" />
                        Đã Xác Nhận Bloom
                      </span>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleConfirmBloom(v)}
                        className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-800 border border-rose-200 hover:bg-pink-200 transition active:scale-95 shadow-sm"
                        title="Bấm để xác nhận cấp độ nhận thức Bloom này hợp lệ"
                      >
                        <AlertCircle className="w-3 h-3 text-pink-600" />
                        Xác Nhận Bloom ({v.bloomLevel})
                      </button>
                    )}

                    <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border-2 ${bloomMeta.color}`}>
                      {bloomMeta.label}
                    </span>

                    <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border-2 ${
                      v.status === 'APPROVED'
                        ? 'bg-emerald-50 text-emerald-800 border-pink-200'
                        : v.status === 'REJECTED'
                          ? 'bg-rose-50 text-rose-700 border-error'
                          : 'bg-slate-100 text-slate-800 border-pink-400'
                    }`}>
                      {v.status}
                    </span>
                  </div>
                </div>

                {/* Question Content */}
                <div>
                  <p className="text-[10px] font-bold text-green-700 uppercase tracking-wider mb-1">Nội Dung Câu Hỏi:</p>
                  <h3 className="text-sm text-ink font-semibold leading-relaxed font-heading">{v.content}</h3>
                </div>

                {/* Expected Answer */}
                <div className="p-3.5 bg-pink-50 rounded-xl border border-pink-200 shadow-sm">
                  <p className="text-[10px] font-bold text-pink-900 uppercase tracking-wider mb-1">Đáp Án Tiêu Chuẩn:</p>
                  <p className="text-xs text-ink leading-relaxed font-normal">{v.expectedAnswer}</p>
                </div>

                {/* Source Citations (Grounding Evidence) */}
                {v.sources && v.sources.length > 0 && (
                  <div className="p-3.5 bg-green-50 rounded-xl border border-pink-200 shadow-sm space-y-1.5">
                    <div className="flex items-center gap-1.5 text-green-900 font-bold text-[11px]">
                      <BookOpen className="w-3.5 h-3.5 text-green-700" />
                      <span>Cơ Sở Đối Chiếu Giáo Trình (Grounding Evidence Chunk #{v.sources[0].order}):</span>
                    </div>
                    <p className="text-[11px] text-ink italic bg-surface-container-lowest p-2.5 rounded-lg border border-pink-200/50 leading-relaxed">
                      "{v.sources[0].citationQuote}"
                    </p>
                  </div>
                )}

                {/* Rubric Breakdown */}
                {v.rubric && v.rubric.criteria && (
                  <div className="pt-2 border-t-2 border-cream">
                    <div className="flex flex-wrap items-center justify-between gap-1 mb-2">
                      <span className="text-[10px] font-bold text-green-800 uppercase tracking-wider">
                        Ma Trận Tiêu Chí Rubric ({v.rubric.criteria.length} tiêu chí):
                      </span>
                      <span className="text-pink-900 font-bold text-xs bg-slate-100 border border-pink-200 text-slate-800 px-2 py-0.5 rounded-md shadow-sm">
                        Tổng điểm: {v.rubric.totalScore || 10}đ
                      </span>
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {v.rubric.criteria.map((c, idx) => (
                        <div key={idx} className="p-2.5 bg-surface-container-lowest rounded-lg border border-pink-200/70 flex items-center justify-between text-xs">
                          <div>
                            <span className="font-bold text-pink-800">{c.name}</span>
                            {c.description && <p className="text-[10px] text-on-surface-variant line-clamp-1">{c.description}</p>}
                          </div>
                          <span className="font-mono font-bold text-green-800 shrink-0 ml-2">{c.maxScore}đ</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Action Bar */}
                <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t-2 border-cream">
                  <div className="flex flex-wrap items-center gap-2">
                    {v.origin === 'AI_RAG' && v.status === 'DRAFT' && (
                      <button
                        type="button"
                        onClick={() => { setRegenVersionTarget(v); setRegenFeedback(''); }}
                        className="px-3 py-1.5 rounded-xl bg-paper text-green-700 border border-pink-200 font-bold text-xs shadow-sm hover:bg-green-100 transition active:scale-95 flex items-center gap-1.5"
                      >
                        <Sparkles className="w-3.5 h-3.5 text-pink-600" />
                        <span>Tạo Lại Với AI (Regenerate)</span>
                      </button>
                    )}

                    {v.status === 'DRAFT' && (
                      <button
                        type="button"
                        onClick={() => handleOpenEditDraft(v)}
                        className="px-3 py-1.5 rounded-xl bg-paper text-ink border border-pink-200 font-bold text-xs shadow-sm hover:bg-green-50 transition active:scale-95 flex items-center gap-1.5"
                      >
                        <Edit3 className="w-3.5 h-3.5 text-green-700" />
                        <span>Chỉnh Sửa</span>
                      </button>
                    )}

                    {v.status === 'DRAFT' && (
                      <button
                        type="button"
                        onClick={() => setDeleteDraftTarget(v)}
                        className="p-1.5 rounded-xl bg-paper border border-rose-200 text-error hover:bg-pink-50 transition shadow-sm"
                        title="Xóa vĩnh viễn bản nháp"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>

                  {v.status === 'DRAFT' && (
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => { setRejectVersionTarget(v); setRejectReason(''); }}
                        className="px-3.5 py-1.5 rounded-xl bg-paper text-error border border-rose-200 font-bold text-xs hover:bg-pink-50 transition active:scale-95"
                      >
                        Từ Chối
                      </button>

                      <button
                        type="button"
                        onClick={() => handleApproveVersion(v)}
                        className="px-3.5 py-1.5 rounded-xl bg-pink-600 text-white font-semibold text-xs shadow-xs hover:bg-pink-700 transition active:scale-[0.98] flex items-center gap-1.5"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Phê Duyệt (Approve)</span>
                      </button>
                    </div>
                  )}
                </div>

              </article>
            );
          })}
        </div>
      )}

      {/* REVIEW QUEUE PAGINATION */}
      {!isLoadingReview && reviewVersions.length > 0 && (
        <Pagination
          page={reviewPage}
          size={reviewPageSize}
          totalPages={reviewTotalPages}
          totalElements={reviewTotalElements}
          onPageChange={handleReviewPageChange}
          onSizeChange={handleReviewPageSizeChange}
          itemLabel="bản nháp trong hàng đợi"
        />
      )}

      {/* FLOATING BATCH ACTION BAR */}
      {selectedVersionIds.length > 0 && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-paper border border-pink-200 rounded-2xl shadow-md px-6 py-3.5 flex items-center gap-4 animate-in fade-in slide-in-from-bottom-4 duration-200">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-pulse" />
            <span className="text-xs sm:text-sm font-bold text-ink">
              Đã chọn <span className="font-heading text-pink-900 bg-pink-100 px-2 py-0.5 rounded-lg border border-pink-400 font-extrabold">{selectedVersionIds.length}</span> câu hỏi
            </span>
          </div>

          <div className="h-6 w-0.5 bg-cream" />

          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={isBatchApproving}
              onClick={handleBatchApprove}
              className="px-4 py-2 rounded-xl bg-green-400 hover:bg-green-500 text-ink font-heading font-bold text-xs border border-pink-200 shadow-sm active:scale-[0.98] transition flex items-center gap-2 cursor-pointer disabled:opacity-50"
            >
              <CheckCircle2 className="w-4 h-4 text-green-900" />
              <span>{isBatchApproving ? 'Đang duyệt...' : `Duyệt ${selectedVersionIds.length} câu đã chọn`}</span>
            </button>

            <button
              type="button"
              onClick={() => setSelectedVersionIds([])}
              className="px-3 py-2 rounded-xl bg-cream hover:bg-pink-100 text-ink font-bold text-xs border border-pink-200 shadow-sm transition active:scale-[0.98] cursor-pointer"
            >
              Bỏ chọn
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
