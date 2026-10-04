import React, { useState } from 'react';
import { 
  Plus, 
  Edit3, 
  Trash2, 
  CheckCircle2, 
  X, 
  Sliders, 
  BookOpen, 
  Sparkles,
  Search
} from 'lucide-react';

export function RubricStudioPage({ showToast }) {
  const [isRubricModalOpen, setIsRubricModalOpen] = useState(false);
  const [criteriaName, setCriteriaName] = useState('');
  const [criteriaWeight, setCriteriaWeight] = useState('20');
  const [criteriaMaxScore, setCriteriaMaxScore] = useState('2.0');

  const rubricList = [
    {
      code: 'C1',
      name: 'Bản chất Kiến trúc Onion & Domain Isolation',
      weight: '30%',
      maxScore: '3.0 điểm',
      keywords: 'Domain Entity, Dependency Inversion, Core Layer',
      weightColor: 'bg-blue-100 text-blue-800'
    },
    {
      code: 'C2',
      name: 'Transactional Outbox Pattern & Dual-Write',
      weight: '25%',
      maxScore: '2.5 điểm',
      keywords: 'Atomic Outbox Table, Background Polling, Idempotent',
      weightColor: 'bg-emerald-100 text-emerald-800'
    },
    {
      code: 'C3',
      name: 'Hỏi Xoáy Thích Ứng (Repository Pattern & DIP)',
      weight: '25%',
      maxScore: '2.5 điểm',
      keywords: 'Repository Interface, Persistence Ignorance',
      weightColor: 'bg-rose-100 text-rose-800'
    },
    {
      code: 'C4',
      name: 'Kỹ Năng Trình Bày & Tác Phong Kỹ Sư',
      weight: '20%',
      maxScore: '2.0 điểm',
      keywords: 'Mạch lạc, súc tích, tự tin, không vòng vo',
      weightColor: 'bg-amber-100 text-amber-800'
    }
  ];

  const handleRubricSubmit = (e) => {
    e.preventDefault();
    setIsRubricModalOpen(false);
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Cập Nhật Rubric',
        message: `Đã lưu tiêu chí "${criteriaName || 'Tiêu chí mới'}" (${criteriaWeight}%) vào ma trận Rubric SWD392!`
      });
    }
    setCriteriaName('');
  };

  return (
    <div className="space-y-6 animate-modal-entry">
      {/* Header Bar */}
      <div className="bg-white border border-slate-200/90 p-5 sm:p-6 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-2xs">
        <div>
          <h2 className="font-heading font-extrabold text-base sm:text-lg text-slate-900">Ma Trận Tiêu Chí Rubric (SWD392)</h2>
          <p className="text-xs text-slate-500 mt-0.5">Quy chuẩn 4 tiêu chí đánh giá vấn đáp của Hội đồng Bộ Môn</p>
        </div>
        <button 
          onClick={() => setIsRubricModalOpen(true)} 
          className="inline-flex items-center gap-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold px-4 py-2 rounded-full shadow-xs active:scale-95 transition cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>Thêm Tiêu Chí Mới</span>
        </button>
      </div>

      {/* Rubric Table Card */}
      <div className="bg-white border border-slate-200/90 rounded-2xl shadow-2xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700 border-collapse">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-bold uppercase text-[10px] tracking-wider">
              <tr>
                <th className="p-4">Mã</th>
                <th className="p-4">Tên Tiêu Chí</th>
                <th className="p-4">Trọng Số</th>
                <th className="p-4">Điểm Tối Đa</th>
                <th className="p-4">Từ Khóa RAG Cốt Lõi</th>
                <th className="p-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rubricList.map((item) => (
                <tr key={item.code} className="hover:bg-slate-50/80 transition">
                  <td className="p-4 font-bold text-blue-600 font-mono">{item.code}</td>
                  <td className="p-4 font-semibold text-slate-900">{item.name}</td>
                  <td className="p-4">
                    <span className={`px-2.5 py-0.5 rounded-full font-bold text-[11px] ${item.weightColor}`}>
                      {item.weight}
                    </span>
                  </td>
                  <td className="p-4 font-bold">{item.maxScore}</td>
                  <td className="p-4 text-slate-500 font-mono text-[11px]">{item.keywords}</td>
                  <td className="p-4 text-right">
                    <button 
                      onClick={() => {
                        setCriteriaName(item.name);
                        setIsRubricModalOpen(true);
                      }} 
                      className="text-blue-600 hover:underline font-semibold"
                    >
                      Chỉnh sửa
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="p-3.5 bg-slate-50/80 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
          <span>Hiển thị 4 trên 4 tiêu chí</span>
          <div className="flex items-center gap-1 font-mono">
            <button className="px-2.5 py-1 rounded-full bg-white border border-slate-200 text-slate-400 cursor-not-allowed" disabled>Trước</button>
            <button className="px-2.5 py-1 rounded-full bg-blue-600 text-white font-bold">1</button>
            <button className="px-2.5 py-1 rounded-full bg-white border border-slate-200 text-slate-400 cursor-not-allowed" disabled>Sau</button>
          </div>
        </div>
      </div>

      {/* RUBRIC MODAL */}
      {isRubricModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-heading font-bold text-base text-slate-900">Thêm / Chỉnh Sửa Tiêu Chí Rubric</h3>
              <button onClick={() => setIsRubricModalOpen(false)} className="text-slate-400 hover:text-slate-700 p-1">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleRubricSubmit} className="space-y-3 text-xs">
              <div>
                <label className="font-bold block mb-1">Tên Tiêu Chí Mới</label>
                <input 
                  type="text" 
                  required
                  value={criteriaName}
                  onChange={(e) => setCriteriaName(e.target.value)}
                  placeholder="Ví dụ: Hiểu cơ chế Event-Driven Architecture..." 
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" 
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-bold block mb-1">Trọng Số (%)</label>
                  <input 
                    type="number" 
                    value={criteriaWeight}
                    onChange={(e) => setCriteriaWeight(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" 
                  />
                </div>
                <div>
                  <label className="font-bold block mb-1">Điểm Tối Đa</label>
                  <input 
                    type="number" 
                    step="0.5"
                    value={criteriaMaxScore}
                    onChange={(e) => setCriteriaMaxScore(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" 
                  />
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <button 
                  type="button" 
                  onClick={() => setIsRubricModalOpen(false)} 
                  className="px-4 py-2 rounded-full border border-slate-200 text-xs font-semibold hover:bg-slate-50"
                >
                  Hủy
                </button>
                <button 
                  type="submit" 
                  className="px-5 py-2 rounded-full bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-xs active:scale-95 transition"
                >
                  Lưu Tiêu Chí
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
