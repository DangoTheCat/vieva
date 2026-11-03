import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { oralExamApi } from '../api/oralExamApi';
import { getErrorMessage } from '../utils/errorCodes';
import { gradeLabel, isProbeTurn } from '../utils/activeExam';
import { Award, ChevronRight, Clock, Loader2, RotateCcw, MessageSquareText, X } from 'lucide-react';

const formatDate = (iso) => (iso ? new Date(iso).toLocaleString('vi-VN') : '—');

/**
 * Student's completed attempts (GET /student/scores) with a per-turn breakdown
 * loaded on demand from GET /student/attempts/{id}.
 */
export function StudentResultsPage({ showToast }) {
  const [scores, setScores] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const [selected, setSelected] = useState(null);
  const [detail, setDetail] = useState(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  // Close modal on Escape
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && selected) {
        setSelected(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selected]);

  const loadScores = async () => {
    setIsLoading(true);
    setLoadError('');
    try {
      const data = await oralExamApi.getScores();
      setScores(Array.isArray(data) ? data : []);
    } catch (err) {
      setLoadError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadScores();
  }, []);

  const openDetail = async (score) => {
    setSelected(score);
    setDetail(null);
    setIsLoadingDetail(true);
    try {
      setDetail(await oralExamApi.getAttemptDetail(score.attemptId));
    } catch (err) {
      showToast?.({ type: 'error', message: getErrorMessage(err) });
      setSelected(null);
    } finally {
      setIsLoadingDetail(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto w-full space-y-6 pb-10 text-ink">
      <header>
        <h1 className="font-heading font-extrabold text-2xl tracking-tight">Kết Quả Thi Của Tôi</h1>
        <p className="text-xs text-slate-500 mt-1">Các ca thi vấn đáp đã hoàn tất và điểm số được hệ thống ghi nhận.</p>
      </header>

      {loadError && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-2xl text-xs text-rose-800 flex items-center justify-between gap-3">
          <span>{loadError}</span>
          <button
            type="button"
            onClick={loadScores}
            className="px-3 py-1.5 rounded-xl bg-white border border-pink-200 font-bold flex items-center gap-1.5 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-green-600 cursor-pointer"
          >
            <RotateCcw className="w-3.5 h-3.5" /> Thử lại
          </button>
        </div>
      )}

      <section className="bg-white border border-pink-200 rounded-2xl shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-4 space-y-3">
            {[0, 1, 2].map(i => <div key={i} className="h-14 rounded-xl animate-pulse bg-slate-200/80" />)}
          </div>
        ) : scores.length === 0 && !loadError ? (
          <div className="p-10 text-center space-y-2">
            <Award className="w-8 h-8 text-slate-600 mx-auto" />
            <p className="font-bold text-sm">Chưa có kết quả nào</p>
            <p className="text-xs text-slate-500">Kết quả sẽ xuất hiện ở đây sau khi bạn hoàn thành một ca thi.</p>
          </div>
        ) : (
          <ul className="divide-y divide-slate-100">
            {scores.map((s) => (
              <li key={s.attemptId}>
                <button
                  type="button"
                  onClick={() => openDetail(s)}
                  className="w-full text-left p-4 flex items-center justify-between gap-4 hover:bg-slate-50 transition focus:outline-none focus:bg-slate-50 cursor-pointer"
                >
                  <div className="min-w-0">
                    <p className="font-bold text-sm truncate">{s.examTitle || 'Ca thi'}</p>
                    <p className="text-xs text-slate-500 mt-0.5">Hoàn tất: {formatDate(s.completedAt)}</p>
                  </div>
                  <div className="flex items-center gap-3 shrink-0">
                    {s.scorePublished === false ? (
                      <span className="inline-flex items-center gap-1 text-xs font-bold text-amber-800 bg-amber-50 border border-amber-200 rounded-lg px-2 py-1">
                        <Clock className="w-3.5 h-3.5" /> Chờ giảng viên chốt điểm
                      </span>
                    ) : (
                      <div className="text-right">
                        <p className="font-heading font-black text-lg text-pink-600 leading-none">
                          {s.totalScore != null ? Number(s.totalScore) : '—'}<span className="text-xs text-ink font-sans"> / 10</span>
                        </p>
                        <p className="text-xs font-bold text-slate-700">{gradeLabel(s.gradeLevel)}</p>
                      </div>
                    )}
                    <ChevronRight className="w-4 h-4 text-slate-700" />
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* DETAILED RESULTS MODAL */}
      {selected && createPortal(
        <div 
          className="fixed inset-0 z-50 bg-ink/50 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6" 
          role="dialog" 
          aria-modal="true"
          onClick={(e) => {
            if (e.target === e.currentTarget) setSelected(null);
          }}
        >
          <div className="bg-white border border-pink-200 rounded-3xl w-full max-w-2xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden animate-modal-entry">
            <div className="bg-white p-5 border-b border-pink-200 flex items-start justify-between gap-3 shrink-0">
              <div>
                <h2 className="font-heading font-black text-lg">{selected.examTitle || 'Chi tiết ca thi'}</h2>
                <p className="text-xs text-slate-500">Mã ca thi: <span className="font-mono">{selected.attemptId}</span></p>
              </div>
              <button
                type="button"
                onClick={() => setSelected(null)}
                aria-label="Đóng"
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 min-h-0 p-5 space-y-4 text-xs overflow-y-auto">
              {isLoadingDetail || !detail ? (
                <div className="py-12 flex items-center justify-center gap-2 text-slate-500">
                  <Loader2 className="w-5 h-5 animate-spin text-pink-600" /> Đang tải chi tiết ca thi...
                </div>
              ) : (
                <>
                  {selected.scorePublished === false && (
                    <div className="p-3.5 bg-amber-50 border border-amber-300 rounded-xl text-xs text-amber-900 font-medium">
                      Bài thi đã được AI chấm sơ bộ và đang chờ giảng viên chốt điểm. Điểm và nhận xét sẽ hiển thị sau khi giảng viên chốt.
                    </div>
                  )}
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                    <div className="bg-white border border-pink-200 rounded-xl p-3 shadow-2xs">
                      <p className="text-xs uppercase font-bold text-slate-700">Điểm</p>
                      <p className="font-heading font-black text-xl text-pink-600">{detail.totalScore != null ? Number(detail.totalScore) : '—'} / 10</p>
                    </div>
                    <div className="bg-white border border-pink-200 rounded-xl p-3 shadow-2xs">
                      <p className="text-xs uppercase font-bold text-slate-700">Xếp loại</p>
                      <p className="font-heading font-black text-xl">{gradeLabel(detail.gradeLevel)}</p>
                    </div>
                    <div className="bg-white border border-pink-200 rounded-xl p-3 shadow-2xs">
                      <p className="text-xs uppercase font-bold text-slate-700">Số lượt</p>
                      <p className="font-heading font-black text-xl">{detail.turns?.length || 0}</p>
                    </div>
                  </div>

                  {detail.detailedFeedback && (
                    <div className="bg-slate-50 border border-pink-200 rounded-xl p-3.5 space-y-1">
                      <p className="font-bold text-slate-800 flex items-center gap-1.5"><MessageSquareText className="w-4 h-4 text-pink-600" /> Nhận xét tổng</p>
                      <p className="text-slate-700 leading-relaxed text-xs">{detail.detailedFeedback}</p>
                    </div>
                  )}

                  <ol className="space-y-3">
                    {(detail.turns || []).map((t, i) => (
                      <li key={t.turnId} className="bg-white border border-pink-200 rounded-xl p-3.5 space-y-2 shadow-2xs">
                        <div className="flex items-center justify-between gap-2">
                          <span className="font-bold text-ink">
                            Lượt {t.turnOrder || i + 1} • {isProbeTurn(t.turnType) ? 'Câu hỏi làm rõ' : 'Câu hỏi chính'}
                          </span>
                          {selected.scorePublished !== false && (
                            <span className="font-mono font-bold text-xs bg-slate-100 border border-pink-200 text-slate-800 rounded-lg px-2.5 py-0.5">
                              {t.score != null ? Number(t.score) : '—'} / 10
                            </span>
                          )}
                        </div>
                        <p className="text-ink font-medium leading-relaxed">{t.questionText}</p>
                        {t.transcriptText && (
                          <div className="text-slate-800 bg-slate-50 rounded-lg p-2.5 border border-pink-100 text-xs leading-relaxed">
                            <span className="font-bold text-slate-700">Bạn trả lời: </span>
                            <span>{t.transcriptText}</span>
                          </div>
                        )}
                        {t.feedback && (
                          <div className="text-xs text-slate-600 leading-relaxed pl-1">
                            <span className="font-bold text-slate-500">Nhận xét: </span>
                            <span>{t.feedback}</span>
                          </div>
                        )}
                      </li>
                    ))}
                  </ol>
                </>
              )}
            </div>

            <div className="p-4 border-t border-pink-200 bg-slate-50/70 flex justify-end shrink-0">
              <button
                type="button"
                onClick={() => setSelected(null)}
                className="px-4 py-2 rounded-xl bg-white border border-pink-200 hover:bg-slate-100 text-slate-700 font-semibold text-xs transition cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
