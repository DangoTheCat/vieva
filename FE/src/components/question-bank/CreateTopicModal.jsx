import React, { useState } from 'react';
import { X, FolderPlus } from 'lucide-react';

export function CreateTopicModal({ isOpen, onClose, onSubmit }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSubmit({ name: name.trim(), description: description.trim() });
    setName('');
    setDescription('');
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in" role="dialog">
      <div className="bg-paper rounded-2xl max-w-md w-full border border-pink-200 shadow-md p-5 space-y-4 animate-modal-entry">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-pink-200 pb-3">
          <div className="flex items-center gap-2 text-pink-900 font-bold text-xs">
            <FolderPlus className="w-4 h-4 text-pink-700" />
            <h3 className="font-heading font-extrabold text-sm text-ink">Thêm Chủ Đề Đề Cương</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="block text-xs font-bold text-ink mb-1">
              Tên Chủ Đề / Chương <span className="text-rose-600">*</span>
            </label>
            <input
              type="text"
              required
              placeholder="Ví dụ: Chương 4: Clean Architecture & SOLID"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-sm font-medium"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-ink mb-1">Mô Tả Nội Dung Chủ Đề</label>
            <textarea
              rows="2"
              placeholder="Mô tả chuẩn đầu ra học phần cho chủ đề này..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3 py-2 rounded-xl text-xs bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-sm font-medium"
            />
          </div>

          <div className="pt-2 flex justify-end gap-2 border-t border-pink-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-white border border-pink-200 text-slate-700 text-xs font-bold shadow-2xs hover:bg-slate-50 transition cursor-pointer"
            >
              Hủy Bỏ
            </button>
            <button
              type="submit"
              disabled={!name.trim()}
              className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 cursor-pointer"
            >
              Lưu Chủ Đề
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
