import React, { useState } from 'react';
import { X, Plus, Trash2 } from 'lucide-react';
import { BLOOM_LEVELS } from './bloomConstants';
import { RubricCriteriaEditor } from './RubricCriteriaEditor';

export function ManualQuestionModal({
  isOpen,
  onClose,
  topics = [],
  onSubmit,
  isSubmitting = false
}) {
  const [topicId, setTopicId] = useState('');
  const [bloomLevel, setBloomLevel] = useState('UNDERSTAND');
  const [content, setContent] = useState('');
  const [expectedAnswer, setExpectedAnswer] = useState('');
  const [criteria, setCriteria] = useState([
    { name: 'Tính chính xác chuyên môn', description: 'Nêu đúng định nghĩa, thuật toán hoặc nguyên lý cốt lõi', maxScore: 5 },
    { name: 'Khả năng phân tích & phản biện', description: 'Giải thích được lý do áp dụng và so sánh ưu nhược điểm', maxScore: 5 }
  ]);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      topicId: topicId || null,
      bloomLevel,
      content,
      expectedAnswer,
      criteria
    });
  };

  const totalScore = criteria.reduce((sum, c) => sum + (Number(c.maxScore) || 0), 0);

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in" role="dialog">
      <div className="bg-paper rounded-2xl max-w-xl w-full border border-pink-200 shadow-md p-6 space-y-4 animate-modal-entry max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-pink-200 pb-3">
          <h3 className="font-heading font-extrabold text-sm text-ink">Soạn Câu Hỏi Mới Kèm Ma Trận Rubric</h3>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-ink mb-1">Chủ Đề (Topic)</label>
              <select
                value={topicId}
                onChange={(e) => setTopicId(e.target.value)}
                className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-sm font-medium cursor-pointer"
              >
                <option value="">-- Chủ đề chung --</option>
                {topics.map((t) => (
                  <option key={t.topicId} value={t.topicId}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-ink mb-1">Cấp Độ Nhận Thức (Bloom)</label>
              <select
                value={bloomLevel}
                onChange={(e) => setBloomLevel(e.target.value)}
                className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-sm font-medium cursor-pointer"
              >
                {BLOOM_LEVELS.map((b) => (
                  <option key={b.value} value={b.value}>
                    {b.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-ink mb-1">
              Nội Dung Câu Hỏi <span className="text-rose-600">*</span>
            </label>
            <textarea
              rows="3"
              required
              placeholder="Nhập nội dung câu hỏi vấn đáp chi tiết..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-sm font-medium"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-ink mb-1">
              Đáp Án Tiêu Chuẩn Mong Đợi <span className="text-rose-600">*</span>
            </label>
            <textarea
              rows="3"
              required
              placeholder="Đáp án hoặc các ý chính thí sinh cần nêu bật được..."
              value={expectedAnswer}
              onChange={(e) => setExpectedAnswer(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-sm font-medium"
            />
          </div>

          {/* Dynamic Rubric Builder */}
          <div className="p-3.5 bg-white rounded-xl border border-pink-200 shadow-2xs space-y-3">
            <div className="flex items-center justify-between border-b border-pink-100 pb-2">
              <div>
                <h4 className="text-xs font-bold text-ink">Tiêu Chí Chấm Điểm Rubric</h4>
                <p className="text-[10px] text-slate-500">Thiết lập các tiêu chí chấm điểm tự động</p>
              </div>
              <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border border-pink-200 shadow-2xs">
                Tổng: {totalScore}đ
              </span>
            </div>

            <RubricCriteriaEditor criteria={criteria} onChange={setCriteria} />
          </div>

          <div className="pt-2 flex justify-end gap-2 border-t border-pink-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-xl bg-white border border-pink-200 text-slate-700 text-xs font-bold shadow-sm hover:bg-slate-50 transition cursor-pointer"
            >
              Hủy Bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !content.trim() || !expectedAnswer.trim()}
              className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 flex items-center gap-2 cursor-pointer"
            >
              {isSubmitting && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
              <span>Lưu &amp; Gửi Hàng Đợi Duyệt</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
