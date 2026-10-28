import React from 'react';
import { X } from 'lucide-react';
import { BLOOM_LEVELS } from './bloomConstants';
import { RubricCriteriaEditor } from './RubricCriteriaEditor';

export function EditDraftModal({
  editDraft,
  setEditDraft,
  onSave,
  isSaving = false
}) {
  if (!editDraft) return null;

  const totalScore = editDraft.criteria?.reduce((sum, c) => sum + (Number(c.maxScore) || 0), 0) || 0;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in" role="dialog" aria-modal="true">
      <div className="bg-paper rounded-2xl max-w-xl w-full border border-pink-200 shadow-md p-6 space-y-4 animate-modal-entry max-h-[90vh] overflow-y-auto">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-pink-200 pb-3">
          <h3 className="font-heading font-extrabold text-sm text-ink">
            Chỉnh Sửa Bản Nháp {editDraft.version?.questionCode || ''}
          </h3>
          <button
            type="button"
            onClick={() => setEditDraft(null)}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={onSave} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-ink mb-1">Cấp Độ Nhận Thức (Bloom)</label>
            <select
              value={editDraft.bloomLevel}
              onChange={(e) => setEditDraft((d) => ({ ...d, bloomLevel: e.target.value }))}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-2xs font-medium cursor-pointer"
            >
              {BLOOM_LEVELS.map((b) => (
                <option key={b.value} value={b.value}>
                  {b.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-ink mb-1">
              Nội Dung Câu Hỏi <span className="text-rose-600">*</span>
            </label>
            <textarea
              rows="3"
              required
              maxLength={4000}
              value={editDraft.content}
              onChange={(e) => setEditDraft((d) => ({ ...d, content: e.target.value }))}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-2xs font-medium"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-ink mb-1">
              Đáp Án Tiêu Chuẩn Mong Đợi <span className="text-rose-600">*</span>
            </label>
            <textarea
              rows="3"
              required
              maxLength={8000}
              value={editDraft.expectedAnswer}
              onChange={(e) => setEditDraft((d) => ({ ...d, expectedAnswer: e.target.value }))}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-2xs font-medium"
            />
          </div>

          {/* Rubric Criteria Editor */}
          <div className="p-3.5 bg-white rounded-xl border border-pink-200 shadow-2xs space-y-3">
            <div className="flex items-center justify-between border-b border-pink-100 pb-2">
              <h4 className="text-xs font-bold text-ink">Tiêu Chí Chấm Điểm Rubric</h4>
              <span className="font-mono text-xs font-bold text-pink-900 bg-pink-100 px-2.5 py-0.5 rounded-lg border border-pink-200 shadow-2xs">
                Tổng: {totalScore}đ
              </span>
            </div>
            <RubricCriteriaEditor
              criteria={editDraft.criteria || []}
              onChange={(criteria) => setEditDraft((d) => ({ ...d, criteria }))}
            />
          </div>

          {/* Actions */}
          <div className="pt-2 flex justify-end gap-2 border-t border-pink-100">
            <button
              type="button"
              onClick={() => setEditDraft(null)}
              disabled={isSaving}
              className="px-4 py-2 rounded-xl bg-white border border-pink-200 text-slate-700 text-xs font-bold shadow-2xs hover:bg-slate-50 transition cursor-pointer"
            >
              Hủy Bỏ
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 flex items-center gap-2 cursor-pointer"
            >
              {isSaving && <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
              <span>Lưu Bản Nháp</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
