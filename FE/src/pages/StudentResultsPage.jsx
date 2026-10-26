import React, { useState, useEffect } from 'react';
import { oralExamApi } from '../api/oralExamApi';
import { getErrorMessage } from '../utils/errorCodes';
import { gradeLabel } from '../utils/activeExam';
import { Award, ChevronRight, Loader2, RotateCcw, MessageSquareText, X } from 'lucide-react';

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
    <div className="max-w-4xl mx-auto w-full space-y-6 pb-10 text-ink animate-modal-entry">
      <header>
        <h1 className="font-heading font-extrabold text-2xl tracking-tight">Kết Quả Thi Của Tôi</h1>
        <p className="text-xs text-green-800 mt-1">Các ca thi vấn đáp đã hoàn tất và điểm số được hệ thống ghi nhận.</p>
      </header>

      {loadError && (
        <div className="p-4 bg-pink-50 border-2 border-pink-300 rounded-2xl text-xs text-pink-900 flex items-center justify-between gap-3">
          <span>{loadError}</span>
          <button
            type="button"
            onClick={loadScores}
            className="px-3 py-1.5 rounded-xl bg-white border-2 border-green-500 font-bold flex items-center gap-1.5 hover:bg-green-50 focus:outline-none focus:ring-2 focus:ring-green-600"
          >
            <RotateCcw className="w-3.5 h-3.5" /> Thử lại
          </button>
        </div>
      )}

      <section className="bg-white border-2 border-green-500 rounded-2xl shadow-[4px_4px_0_#DEB9B5] overflow-hidden">
        {isLoading ? (
          <div className="p-4 space-y-3">
            {[0, 1, 2].map(i => <div key={i} className="h-14 rounded-xl animate-pulse bg-slate-200/80" />)}
          </div>
        ) : scores.length === 0 && !loadError ? (
          <div className="p-10 text-center space-y-2">
            <Award className="w-8 h-8 text-green-600 mx-auto" />
            <p className="font-bold text-sm">Chưa có kết quả nào</p>
            <p className="text-xs text-green-800">Kết quả sẽ xuất hiện ở đây sau khi bạn hoàn thành một ca thi.</p>
          </div>
        ) : (
          <ul className="divide-y-2 divide-green-100">
            {scores.map((s) => (
              <li key={s.attemptId}>
                <button
                  type="button"
                  onClick={() => openDetail(s)}
                  className="w-full text-left p-4 flex items-center justify-between gap-4 hover:bg-green-50 transition focus:outline-none focus:bg-green-50"
                >
                  <div className="min-w-0">
                    <p className="font-bold text-sm truncate">{s.examTitle || 'Ca thi'}</p>
                    <p className="text-[11px] text-green-800 mt-0.5">Hoàn tất: {formatDate(s.completedAt)}</p>
                  </div>
                  <div className="flex items-center gap-3 shrink-0">
                    <div className="text-right">
                      <p className="font-heading font-black text-lg text-pink-600 leading-none">
                        {s.totalScore != null ? Number(s.totalScore) : '—'}<span className="text-xs text-ink font-sans"> / 10</span>
                      </p>
                      <p className="text-[11px] font-bold text-green-700">{gradeLabel(s.gradeLevel)}</p>
                    </div>
                    <ChevronRight className="w-4 h-4 text-green-700" />
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {selected && (
        <div className="fixed inset-0 z-50 bg-ink/50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
          <div className="bg-paper border-2 border-green-500 rounded-3xl w-full max-w-2xl max-h-[85vh] overflow-y-auto shadow-[8px_8px_0_#DEB9B5]">
            <div className="sticky top-0 bg-paper p-5 border-b-2 border-green-200 flex items-start justify-between gap-3">
              <div>
                <h2 className="font-heading font-black text-lg">{selected.examTitle || 'Chi tiết ca thi'}</h2>
                <p className="text-[11px] text-green-800">Mã ca thi: <span className="font-mono">{selected.attemptId}</span></p>
              </div>
              <button
                type="button"
                onClick={() => setSelected(null)}
                aria-label="Đóng"
                className="p-1 rounded-lg hover:bg-green-100 focus:outline-none focus:ring-2 focus:ring-green-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 space-y-4 text-xs">
              {isLoadingDetail || !detail ? (
                <div className="flex items-center gap-2 text-green-800"><Loader2 className="w-4 h-4 animate-spin" /> Đang tải chi tiết...</div>
              ) : (
                <>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                    <div className="bg-white border-2 border-green-400 rounded-xl p-3">
                      <p className="text-[10px] uppercase font-bold text-green-700">Điểm</p>
                      <p className="font-heading font-black text-xl text-pink-600">{detail.totalScore != null ? Number(detail.totalScore) : '—'} / 10</p>
                    </div>
                    <div className="bg-white border-2 border-green-400 rounded-xl p-3">
                      <p className="text-[10px] uppercase font-bold text-green-700">Xếp loại</p>
                      <p className="font-heading font-black text-xl">{gradeLabel(detail.gradeLevel)}</p>
                    </div>
                    <div className="bg-white border-2 border-green-400 rounded-xl p-3">
                      <p className="text-[10px] uppercase font-bold text-green-700">Số lượt</p>
                      <p className="font-heading font-black text-xl">{detail.turns?.length || 0}</p>
                    </div>
                  </div>

                  {detail.detailedFeedback && (
                    <div className="bg-pink-50 border-2 border-pink-300 rounded-xl p-3">
                      <p className="font-bold text-pink-900 mb-1 flex items-center gap-1.5"><MessageSquareText className="w-4 h-4" /> Nhận xét tổng</p>
                      <p className="text-pink-950 leading-relaxed">{detail.detailedFeedback}</p>
                    </div>
                  )}

                  <ol className="space-y-3">
                    {(detail.turns || []).map((t, i) => (
                      <li key={t.turnId} className="bg-white border-2 border-green-300 rounded-xl p-3 space-y-2">
                        <div className="flex items-center justify-between gap-2">
                          <span className="font-bold">
                            Lượt {t.turnOrder || i + 1} • {t.turnType === 'PROBING' ? 'Câu hỏi xoáy' : 'Câu hỏi chính'}
                          </span>
                          <span className="font-mono font-black bg-green-100 border border-green-400 rounded-lg px-2 py-0.5">
                            {t.score != null ? Number(t.score) : '—'} / 10
                          </span>
                        </div>
                        <p className="text-ink">{t.questionText}</p>
                        {t.transcriptText && (
                          <p className="text-green-900 bg-green-50 rounded-lg p-2"><strong>Bạn trả lời:</strong> {t.transcriptText}</p>
                        )}
                        {t.feedback && <p className="text-green-800"><strong>Nhận xét:</strong> {t.feedback}</p>}
                      </li>
                    ))}
                  </ol>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
