import React from 'react';
import { ALL_BLOOM_LEVELS } from '../bloomConstants';
import { Pagination } from '../../common/Pagination';
import { SkeletonCard } from '../../common/Skeleton';
import { CheckCircle2, ChevronDown, Clock, Database, Edit3, Eye, EyeOff, FileSpreadsheet, FileText, Filter, Layers, Lightbulb, Plus, Search, Sliders, Trash2 } from 'lucide-react';

/** Question bank page — "bank" tab. Extracted from LecturerQuestionBankPage (plan item 3.7). */
export function BankTab({ bankPage, bankPageSize, bankQuestions, bankTotalElements, bankTotalPages, expandedExpectedAnswers, filterBloom, filterTopicId, handleBankPageChange, handleBankPageSizeChange, handleCreateDraftFromApproved, handleOpenQuestionDetail, isLoadingBank, loadBank, openTextImport, searchBankKeyword, selectedSubjectId, setArchiveQuestionTarget, setBankPage, setFilterBloom, setFilterTopicId, setIsImportModalOpen, setIsManualQuestionModalOpen, setSearchBankKeyword, toggleExpectedAnswer, topics }) {
  return (
    <div className="space-y-6">
      {/* ACTION TOOLBAR & FILTERS */}
      <div className="flex flex-col xl:flex-row items-stretch xl:items-center justify-between gap-3 bg-paper p-4 rounded-xl border border-pink-200 shadow-sm">
        <div className="flex flex-col md:flex-row items-stretch md:items-center gap-2 flex-1">
          {/* Search */}
          <div className="relative min-w-[220px] flex-1">
            <Search className="w-4 h-4 text-green-700 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Tìm câu hỏi, từ khóa hoặc mã..."
              value={searchBankKeyword}
              onChange={(e) => setSearchBankKeyword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  setBankPage(0);
                  loadBank(0);
                }
              }}
              className="w-full pl-9 pr-3 py-1.5 rounded-lg text-xs bg-surface-container-lowest border border-pink-200 text-ink placeholder:text-on-surface-variant focus:outline-none focus:border-pink-500 shadow-sm font-medium"
            />
          </div>

          {/* Topic Filter */}
          <div className="relative">
            <select
              value={filterTopicId}
              onChange={(e) => {
                const next = e.target.value;
                setFilterTopicId(next);
                setBankPage(0);
                loadBank(0, undefined, next, undefined, undefined);
              }}
              className="w-full md:w-48 appearance-none bg-surface-container-lowest border border-pink-200 rounded-lg py-1.5 pl-3 pr-8 text-xs font-semibold text-ink focus:outline-none focus:border-pink-500 shadow-sm cursor-pointer"
            >
              <option value="ALL">Tất cả chủ đề</option>
              {topics.map(t => (
                <option key={t.topicId} value={t.topicId}>{t.name}</option>
              ))}
            </select>
            <ChevronDown className="w-4 h-4 pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-green-700" />
          </div>

          {/* Bloom Filter */}
          <div className="relative">
            <select
              value={filterBloom}
              onChange={(e) => {
                const next = e.target.value;
                setFilterBloom(next);
                setBankPage(0);
                loadBank(0, undefined, undefined, next, undefined);
              }}
              className="w-full md:w-52 appearance-none bg-surface-container-lowest border border-pink-200 rounded-lg py-1.5 pl-3 pr-8 text-xs font-semibold text-ink focus:outline-none focus:border-pink-500 shadow-sm cursor-pointer"
            >
              <option value="ALL">Tất cả cấp độ Bloom</option>
              {ALL_BLOOM_LEVELS.map(b => (
                <option key={b.value} value={b.value}>{b.label}</option>
              ))}
            </select>
            <ChevronDown className="w-4 h-4 pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-green-700" />
          </div>

          <button
            type="button"
            onClick={() => {
              setBankPage(0);
              loadBank(0);
            }}
            className="p-2 rounded-lg bg-paper text-green-700 border border-pink-200 shadow-sm hover:bg-green-100 transition shrink-0"
            title="Lọc kết quả"
          >
            <Filter className="w-4 h-4" />
          </button>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => setIsImportModalOpen(true)}
            className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-paper text-green-700 border border-pink-200 rounded-xl font-bold text-xs shadow-sm hover:bg-green-100 hover:text-green-900 active:scale-[0.98] transition-all"
          >
            <FileSpreadsheet className="w-4 h-4 text-green-700" />
            <span>Nhập Excel/CSV</span>
          </button>

          <button
            type="button"
            onClick={openTextImport}
            disabled={!selectedSubjectId}
            className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-paper text-green-700 border border-pink-200 rounded-xl font-bold text-xs shadow-sm hover:bg-green-100 hover:text-green-900 active:scale-[0.98] transition-all disabled:opacity-50"
          >
            <FileText className="w-4 h-4 text-green-700" />
            <span>Nhập Từ Văn Bản</span>
          </button>

          <button
            type="button"
            onClick={() => setIsManualQuestionModalOpen(true)}
            className="inline-flex items-center gap-1.5 px-4 py-1.5 bg-pink-600 text-white rounded-xl font-semibold text-xs shadow-xs hover:bg-pink-700 active:scale-[0.98] transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>Soạn Câu Hỏi Thủ Công</span>
          </button>
        </div>
      </div>

      {/* QUESTIONS LIST */}
      {isLoadingBank ? (
        <div className="space-y-4">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : bankQuestions.length === 0 ? (
        <div className="bg-paper p-12 rounded-2xl border border-pink-200 shadow-md text-center">
          <Database className="w-12 h-12 text-green-700 mx-auto mb-3 opacity-60" />
          <h3 className="font-heading font-bold text-ink text-sm">Chưa có câu hỏi chính thức</h3>
          <p className="text-xs text-on-surface-variant mt-1 max-w-sm mx-auto">
            Ngân hàng câu hỏi chính thức chỉ lưu trữ các câu hỏi có phiên bản đã được phê duyệt (APPROVED). Hãy soạn thảo hoặc duyệt các bản nháp từ Hàng đợi.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {bankQuestions.map((q) => {
            const cv = q.currentVersion || {};
            const bloomMeta = ALL_BLOOM_LEVELS.find(b => b.value === cv.bloomLevel) || ALL_BLOOM_LEVELS[0];
            const isExpanded = !!expandedExpectedAnswers[q.questionId];

            return (
              <article key={q.questionId} className="bg-paper border border-pink-200 rounded-2xl p-5 shadow-md transition-all space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2 border-b-2 border-cream pb-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border border-pink-200 shadow-sm">
                      {q.questionCode}
                    </span>
                    <span className="text-xs font-semibold text-green-800">
                      {q.topicName || 'Chủ đề chung'}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border-2 ${bloomMeta.color}`}>
                      {bloomMeta.label}
                    </span>

                    {q.status === 'ARCHIVED' ? (
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-surface-container-high text-on-surface-variant border border-pink-200/50">
                        ARCHIVED
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-green-500 text-paper text-[10px] font-bold shadow-sm">
                        <CheckCircle2 className="w-3 h-3 text-paper" />
                        APPROVED
                      </span>
                    )}
                    {q.hasPendingDraft && (
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-700 border border-rose-200">
                        CÓ BẢN NHÁP
                      </span>
                    )}
                  </div>
                </div>

                {/* Question Content */}
                <div className="text-sm text-ink font-semibold leading-relaxed font-heading">
                  {cv.content}
                </div>

                {/* Collapsible Expected Answer */}
                <div className="pt-1">
                  <button
                    type="button"
                    onClick={() => toggleExpectedAnswer(q.questionId)}
                    className="text-xs font-bold text-green-800 hover:text-green-950 inline-flex items-center gap-1.5 focus:outline-none"
                  >
                    {isExpanded ? <EyeOff className="w-3.5 h-3.5 text-pink-600" /> : <Eye className="w-3.5 h-3.5 text-pink-600" />}
                    <span>{isExpanded ? 'Ẩn đáp án mong đợi & thang điểm' : 'Xem đáp án mong đợi & thang điểm'}</span>
                  </button>

                  {isExpanded && (
                    <div className="mt-2.5 p-4 bg-pink-50 rounded-xl border border-pink-200 shadow-sm text-xs text-ink space-y-3 animate-modal-entry">
                      <div>
                        <div className="flex items-center gap-1.5 text-pink-900 font-bold text-xs mb-1">
                          <Lightbulb className="w-4 h-4 text-pink-600 shrink-0" />
                          <span>Đáp án &amp; Định hướng chấm điểm mong đợi:</span>
                        </div>
                        <p className="leading-relaxed font-normal text-ink">{cv.expectedAnswer}</p>
                      </div>

                      {cv.rubric && cv.rubric.criteria && (
                        <div className="pt-2.5 border-t-2 border-cream">
                          <div className="flex flex-wrap items-center justify-between gap-1 pb-1.5">
                            <span className="font-bold text-ink text-xs inline-flex items-center gap-1">
                              <Sliders className="w-4 h-4 text-green-700 shrink-0" />
                              Ma Trận Rubric Đính Kèm ({cv.rubric.criteria.length} Tiêu Chí):
                            </span>
                            <span className="text-pink-900 font-bold text-xs bg-slate-100 border border-pink-200 text-slate-800 px-2 py-0.5 rounded-md shadow-sm">
                              Tổng điểm: {cv.rubric.totalScore || 10}đ
                            </span>
                          </div>
                          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 mt-2">
                            {cv.rubric.criteria.map((crit, idx) => (
                              <div key={crit.criterionId || idx} className="bg-surface-container-lowest border border-pink-200/70 rounded-lg p-2.5">
                                <div className="flex items-center justify-between text-xs font-bold text-pink-800 mb-1">
                                  <span>{crit.name}</span>
                                  <span className="font-mono text-green-800 shrink-0 ml-1.5">{crit.maxScore}đ</span>
                                </div>
                                {crit.description && (
                                  <p className="text-[11px] text-on-surface-variant leading-snug">{crit.description}</p>
                                )}
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Bottom Toolbar */}
                <div className="flex flex-wrap items-center justify-between pt-2.5 border-t-2 border-cream text-xs text-on-surface-variant gap-2">
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-green-700 shrink-0" />
                    <span>Cập nhật: <strong className="text-ink">{new Date(q.updatedAt || Date.now()).toLocaleDateString('vi-VN')}</strong></span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => handleOpenQuestionDetail(q.questionId)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-paper text-green-700 border border-pink-200 rounded-xl font-bold text-xs shadow-sm hover:bg-green-100 hover:text-green-900 active:scale-[0.98] transition-all"
                    >
                      <Layers className="w-3.5 h-3.5 text-green-700" />
                      <span>Chi Tiết &amp; Lịch Sử</span>
                    </button>
                    {q.status !== 'ARCHIVED' && (
                      <>
                        <button
                          type="button"
                          onClick={() => handleCreateDraftFromApproved(q.questionId)}
                          disabled={q.hasPendingDraft}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-pink-600 text-white rounded-xl font-semibold text-xs shadow-xs hover:bg-pink-700 active:scale-[0.98] transition-all disabled:opacity-50"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                          <span>Tạo Bản Nháp Mới (Copy-on-Write)</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => setArchiveQuestionTarget(q)}
                          className="p-1.5 rounded-lg bg-paper border border-pink-200 text-on-surface-variant hover:text-error hover:border-error hover:bg-pink-50 shadow-sm transition"
                          title="Lưu trữ câu hỏi (Archive)"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {/* BANK PAGINATION */}
      {!isLoadingBank && bankQuestions.length > 0 && (
        <Pagination
          page={bankPage}
          size={bankPageSize}
          totalPages={bankTotalPages}
          totalElements={bankTotalElements}
          onPageChange={handleBankPageChange}
          onSizeChange={handleBankPageSizeChange}
          itemLabel="câu hỏi đã duyệt"
        />
      )}
    </div>
  );
}
