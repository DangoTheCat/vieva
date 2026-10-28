import React from 'react';
import { Trash2, Plus } from 'lucide-react';

export function RubricCriteriaEditor({ criteria, onChange }) {
  const updateAt = (idx, patch) => {
    onChange(criteria.map((c, i) => (i === idx ? { ...c, ...patch } : c)));
  };

  return (
    <div className="space-y-3">
      <div className="space-y-2">
        {criteria.map((crit, idx) => (
          <div key={idx} className="bg-paper p-3 rounded-xl border border-pink-200 shadow-sm space-y-2 transition-all">
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="Tên tiêu chí (VD: Tính đúng đắn, Phân tích kỹ thuật)"
                value={crit.name}
                onChange={(e) => updateAt(idx, { name: e.target.value })}
                className="flex-1 px-3 py-1.5 rounded-lg text-xs bg-white border border-pink-200 font-semibold text-ink placeholder:text-slate-400 focus:outline-none focus:border-pink-500 shadow-sm"
              />
              <div className="flex items-center gap-1 shrink-0">
                <input
                  type="number"
                  min="0.5"
                  step="0.5"
                  max="999.99"
                  value={crit.maxScore}
                  onChange={(e) => updateAt(idx, { maxScore: e.target.value })}
                  className="w-16 px-2 py-1.5 rounded-lg text-xs bg-white border border-pink-200 font-mono font-bold text-center text-ink focus:outline-none focus:border-pink-500 shadow-sm"
                  title="Điểm tối đa của tiêu chí"
                />
                <span className="text-[11px] font-mono text-slate-500 font-bold">đ</span>
              </div>
              <button
                type="button"
                onClick={() => onChange(criteria.filter((_, i) => i !== idx))}
                disabled={criteria.length <= 1}
                className="p-1.5 text-slate-400 hover:text-rose-600 disabled:opacity-30 transition cursor-pointer"
                title="Xóa tiêu chí"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
            <input
              type="text"
              placeholder="Mô tả tiêu chí & chuẩn đầu ra (bắt buộc)"
              value={crit.description}
              onChange={(e) => updateAt(idx, { description: e.target.value })}
              className="w-full px-3 py-1.5 rounded-lg text-[11px] bg-white border border-pink-200 text-ink placeholder:text-slate-400 focus:outline-none focus:border-pink-500 shadow-sm"
            />
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={() => onChange([...criteria, { name: 'Tiêu chí bổ sung', description: '', maxScore: 2 }])}
        className="text-xs font-bold text-pink-700 hover:text-pink-900 inline-flex items-center gap-1 transition cursor-pointer bg-pink-50 hover:bg-pink-100 px-3 py-1.5 rounded-lg border border-pink-200"
      >
        <Plus className="w-3.5 h-3.5" />
        <span>Thêm Tiêu Chí Chấm</span>
      </button>
    </div>
  );
}
