import React, { useState, useEffect } from 'react';
import { Eye, X, Copy, Check, Calendar, ShieldCheck, UserCheck, Phone, Mail, Clock } from 'lucide-react';

export function UserDetailModal({ isOpen, user, onClose, onEdit }) {
  const [copied, setCopied] = useState(false);

  // Keyboard accessibility: Escape to close
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !user) return null;

  const copyUserId = () => {
    if (user.userId) {
      navigator.clipboard.writeText(user.userId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const formatDate = (isoString) => {
    if (!isoString) return 'Chưa ghi nhận';
    try {
      return new Date(isoString).toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div 
      className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
    >
      <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry max-h-[90vh]">
        
        {/* Header */}
        <div className="p-4 bg-sidebarBg text-white border-b border-slate-700 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-sky-500/20 text-sky-300 flex items-center justify-center">
              <Eye className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-heading font-extrabold text-sm text-white">Chi Tiết Hồ Sơ Người Dùng</h3>
              <p className="text-[11px] text-slate-400 font-mono">GET /api/v1/admin/users/{user.userId?.substring(0, 8)}...</p>
            </div>
          </div>
          <button 
            onClick={onClose} 
            className="text-slate-400 hover:text-white p-1 rounded-lg transition focus:outline-none focus:ring-2 focus:ring-sky-500"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-5 space-y-4 text-xs overflow-y-auto">
          
          {/* User Hero Banner */}
          <div className="p-4 bg-slate-50 border border-slate-200/80 rounded-2xl flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-sky-100 text-sky-700 font-bold flex items-center justify-center text-base font-heading shadow-xs">
              {user.fullName ? user.fullName.substring(0, 2).toUpperCase() : 'US'}
            </div>
            <div className="flex-1 min-w-0">
              <h4 className="font-heading font-extrabold text-sm text-slate-900 truncate">{user.fullName}</h4>
              <p className="text-slate-500 font-mono text-[11px] truncate">{user.email}</p>
            </div>
            <div className="text-right">
              <span className={`inline-block px-2.5 py-1 rounded-full text-[10px] font-bold ${
                user.status === 'ACTIVE'
                  ? 'bg-emerald-50 text-emerald-800 border border-emerald-300'
                  : user.status === 'INACTIVE'
                  ? 'bg-amber-50 text-amber-800 border border-amber-300'
                  : 'bg-rose-50 text-rose-800 border border-rose-300'
              }`}>
                {user.status || 'ACTIVE'}
              </span>
            </div>
          </div>

          {/* Details List */}
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 rounded-xl border border-slate-100 bg-slate-50/50 space-y-1">
              <span className="text-[11px] text-slate-400 font-medium">Mã Người Dùng (User Code)</span>
              <p className="font-mono font-bold text-slate-800">{user.userCode || 'Chưa cấp'}</p>
            </div>

            <div className="p-3 rounded-xl border border-slate-100 bg-slate-50/50 space-y-1">
              <span className="text-[11px] text-slate-400 font-medium">Số Điện Thoại</span>
              <p className="font-mono font-bold text-slate-800">{user.phoneNumber || 'Chưa cập nhật'}</p>
            </div>
          </div>

          <div className="p-3 rounded-xl border border-slate-100 bg-slate-50/50 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[11px] text-slate-400 font-medium">Khóa Định Danh UUID (Database Primary Key)</span>
              <button
                type="button"
                onClick={copyUserId}
                className="text-sky-600 hover:text-sky-800 font-bold flex items-center gap-1 text-[10px] px-2 py-0.5 rounded hover:bg-sky-50 transition"
              >
                {copied ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                <span>{copied ? 'Đã sao chép' : 'Sao chép UUID'}</span>
              </button>
            </div>
            <p className="font-mono text-slate-700 select-all text-[11px] break-all">{user.userId}</p>
          </div>

          <div className="p-3 rounded-xl border border-slate-100 bg-slate-50/50 space-y-1.5">
            <span className="text-[11px] text-slate-400 font-medium">Vai Trò Hệ Thống Được Gán:</span>
            <div className="flex flex-wrap gap-1.5">
              {Array.isArray(user.roles) && user.roles.map((role, idx) => (
                <span
                  key={idx}
                  className={`px-2.5 py-0.5 rounded-full font-mono text-[10px] font-bold ${
                    role.includes('ADMIN')
                      ? 'bg-purple-100 text-purple-800 border border-purple-300'
                      : 'bg-sky-100 text-sky-800 border border-sky-300'
                  }`}
                >
                  {role}
                </span>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 text-[11px] text-slate-500 pt-1 border-t border-slate-100">
            <div className="flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
              <div>
                <span className="block text-slate-400 text-[10px]">Tạo lúc:</span>
                <span className="font-mono text-slate-700">{formatDate(user.createdAt)}</span>
              </div>
            </div>
            <div className="flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
              <div>
                <span className="block text-slate-400 text-[10px]">Cập nhật:</span>
                <span className="font-mono text-slate-700">{formatDate(user.updatedAt)}</span>
              </div>
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-50/80 border-t border-slate-100 flex items-center justify-between">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold text-xs transition"
          >
            Đóng
          </button>
          <button
            type="button"
            onClick={() => {
              onClose();
              onEdit(user);
            }}
            className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
            Chỉnh Sửa Tài Khoản Này
          </button>
        </div>

      </div>
    </div>
  );
}
