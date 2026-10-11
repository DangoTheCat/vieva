import React, { useState, useEffect } from 'react';
import {
  Eye,
  X,
  Copy,
  Check,
  Calendar,
  ShieldCheck,
  UserCheck,
  Phone,
  Mail,
  Clock,
  BookOpen,
  KeyRound,
  Loader2,
  ShieldAlert
} from 'lucide-react';
import { adminSubjectApi } from '../../api/adminSubjectApi';
import { getUserRole } from '../../utils/roles';

export function UserDetailModal({ isOpen, user, onClose, onEdit }) {
  const [copiedField, setCopiedField] = useState(null);
  const [assignedSubjects, setAssignedSubjects] = useState([]);
  const [isLoadingSubjects, setIsLoadingSubjects] = useState(false);

  // Keyboard accessibility: Escape to close
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Load assigned subjects when viewing a Lecturer
  useEffect(() => {
    if (!isOpen || !user?.userId) return;

    const role = getUserRole(user);
    if (role !== 'ROLE_LECTURER') {
      setAssignedSubjects([]);
      return;
    }

    let isMounted = true;
    async function fetchLecturerSubjects() {
      setIsLoadingSubjects(true);
      try {
        const [assignRes, subjectsRes] = await Promise.allSettled([
          adminSubjectApi.getSubjectsByLecturer(user.userId),
          adminSubjectApi.getSubjects({ page: 0, size: 100 })
        ]);

        if (!isMounted) return;

        const assignments = assignRes.status === 'fulfilled' && Array.isArray(assignRes.value) ? assignRes.value : [];
        const allSubjects = subjectsRes.status === 'fulfilled' ? (subjectsRes.value?.content || subjectsRes.value || []) : [];
        const subjectMap = new Map((Array.isArray(allSubjects) ? allSubjects : []).map(s => [s.subjectId, s]));

        const combined = assignments.map(a => {
          const sub = subjectMap.get(a.subjectId);
          return {
            ...a,
            subjectCode: sub?.subjectCode || a.subjectId?.substring(0, 8),
            subjectName: sub?.subjectName || 'Môn học phân công',
            credits: sub?.credits || 3
          };
        });

        setAssignedSubjects(combined);
      } catch (err) {
        if (isMounted) setAssignedSubjects([]);
      } finally {
        if (isMounted) setIsLoadingSubjects(false);
      }
    }

    fetchLecturerSubjects();
    return () => {
      isMounted = false;
    };
  }, [isOpen, user]);

  if (!isOpen || !user) return null;

  const handleCopy = (field, text) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const formatDate = (isoString) => {
    if (!isoString) return 'Chưa ghi nhận';
    try {
      return new Date(isoString).toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoString;
    }
  };

  const userRole = getUserRole(user);
  const isAdmin = userRole === 'ROLE_ADMIN';
  const isLecturer = userRole === 'ROLE_LECTURER';

  return (
    <div
      className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in duration-150"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-user-detail-title"
    >
      <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry max-h-[92vh]">
        
        {/* Header */}
        <div className="p-4 bg-[#0B132B] text-white border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-sky-500/20 text-sky-400 flex items-center justify-center border border-sky-500/30">
              <Eye className="w-4 h-4" />
            </div>
            <div>
              <h3 id="modal-user-detail-title" className="font-heading font-extrabold text-sm text-white">
                Hồ Sơ Chi Tiết Người Dùng
              </h3>
              <p className="text-[11px] text-slate-400 font-mono">
                Mã: {user.userCode || 'Chưa cấp'} • ID: {user.userId ? `${user.userId.substring(0, 8)}...` : 'N/A'}
              </p>
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
          <div className="p-4 bg-slate-50 border border-slate-200/90 rounded-2xl flex items-center gap-3.5">
            <div className={`w-12 h-12 rounded-2xl font-heading font-black text-base flex items-center justify-center shadow-xs ${
              isAdmin
                ? 'bg-purple-100 text-purple-800 border border-purple-200'
                : isLecturer
                ? 'bg-sky-100 text-sky-800 border border-sky-200'
                : 'bg-slate-200 text-slate-800 border border-slate-300'
            }`}>
              {user.fullName ? user.fullName.substring(0, 2).toUpperCase() : 'US'}
            </div>
            <div className="flex-1 min-w-0">
              <h4 className="font-heading font-extrabold text-sm text-slate-900 truncate">{user.fullName}</h4>
              <div className="flex items-center gap-2 mt-0.5">
                <span className="text-slate-600 font-mono text-[11px] truncate">{user.email}</span>
                <button
                  type="button"
                  onClick={() => handleCopy('email', user.email)}
                  className="text-slate-400 hover:text-sky-600 transition"
                  title="Sao chép email"
                >
                  {copiedField === 'email' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                </button>
              </div>
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

          {/* Details Grid */}
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 rounded-xl border border-slate-200/80 bg-slate-50/50 space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[11px] text-slate-500 font-medium">Mã Người Dùng</span>
                <button
                  type="button"
                  onClick={() => handleCopy('userCode', user.userCode)}
                  className="text-slate-400 hover:text-sky-600 transition"
                  title="Sao chép mã người dùng"
                >
                  {copiedField === 'userCode' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                </button>
              </div>
              <p className="font-mono font-bold text-slate-900 text-sm">{user.userCode || 'Chưa cấp'}</p>
            </div>

            <div className="p-3 rounded-xl border border-slate-200/80 bg-slate-50/50 space-y-1">
              <span className="text-[11px] text-slate-500 font-medium">Số Điện Thoại</span>
              <p className="font-mono font-bold text-slate-900 text-sm">{user.phoneNumber || 'Chưa cập nhật'}</p>
            </div>
          </div>

          {/* Primary Key UUID */}
          <div className="p-3 rounded-xl border border-slate-200/80 bg-slate-50/50 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[11px] text-slate-500 font-medium">Mã Định Danh UUID (Database Primary Key)</span>
              <button
                type="button"
                onClick={() => handleCopy('userId', user.userId)}
                className="text-sky-600 hover:text-sky-800 font-bold flex items-center gap-1 text-[10px] px-2 py-0.5 rounded hover:bg-sky-50 transition"
              >
                {copiedField === 'userId' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                <span>{copiedField === 'userId' ? 'Đã sao chép' : 'Sao chép UUID'}</span>
              </button>
            </div>
            <p className="font-mono text-slate-800 select-all text-[11px] break-all">{user.userId || 'N/A'}</p>
          </div>

          {/* Single Role & Password Policy */}
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 rounded-xl border border-slate-200/80 bg-slate-50/50 space-y-1.5">
              <span className="text-[11px] text-slate-500 font-medium block">Vai Trò Hệ Thống:</span>
              <div>
                {isAdmin ? (
                  <span className="px-2.5 py-1 rounded-full bg-purple-50 text-purple-700 border border-purple-200 font-bold text-[10px] inline-flex items-center gap-1">
                    <ShieldCheck className="w-3 h-3 text-purple-600" />
                    <span>SUPER ADMIN</span>
                  </span>
                ) : isLecturer ? (
                  <span className="px-2.5 py-1 rounded-full bg-sky-50 text-sky-700 border border-sky-200 font-bold text-[10px]">
                    GIẢNG VIÊN / GIÁM THỊ
                  </span>
                ) : (
                  <span className="px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 border border-slate-200 font-bold text-[10px]">
                    SINH VIÊN / THÍ SINH
                  </span>
                )}
              </div>
            </div>

            <div className="p-3 rounded-xl border border-slate-200/80 bg-slate-50/50 space-y-1.5">
              <span className="text-[11px] text-slate-500 font-medium block">Bảo Mật Mật Khẩu:</span>
              <div>
                {user.mustChangePassword ? (
                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-amber-50 text-amber-800 border border-amber-200 text-[10px] font-bold">
                    <KeyRound className="w-3 h-3 text-amber-600" />
                    <span>Cần Đổi Mật Khẩu Tạm</span>
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-800 border border-emerald-200 text-[10px] font-bold">
                    <Check className="w-3 h-3 text-emerald-600" />
                    <span>Mật Khẩu Cá Nhân Hóa</span>
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Assigned Subjects (For Lecturers) */}
          {isLecturer && (
            <div className="p-3.5 rounded-xl border border-sky-200/80 bg-sky-50/40 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5 font-bold text-slate-800 text-xs">
                  <BookOpen className="w-4 h-4 text-sky-600" />
                  <span>Môn Học Được Phân Công Phụ Trách ({assignedSubjects.length})</span>
                </div>
                {isLoadingSubjects && (
                  <div className="flex items-center gap-1 text-[11px] text-slate-400">
                    <Loader2 className="w-3 h-3 animate-spin text-sky-600" />
                    <span>Đang tải...</span>
                  </div>
                )}
              </div>

              {!isLoadingSubjects && assignedSubjects.length === 0 ? (
                <p className="text-[11px] text-slate-500 italic py-1">
                  Chưa được phân công môn học nào trong kỳ. Quản trị viên có thể gán tại trang Quản Lý Môn Học.
                </p>
              ) : (
                <div className="flex flex-wrap gap-2 pt-1">
                  {assignedSubjects.map((sub, idx) => (
                    <div
                      key={sub.lecturerSubjectId || idx}
                      className="px-2.5 py-1.5 rounded-lg bg-white border border-sky-200 text-slate-800 text-[11px] flex items-center gap-2 shadow-2xs"
                    >
                      <span className="font-mono font-bold text-sky-700">{sub.subjectCode}</span>
                      <span className="text-slate-600 truncate max-w-[150px]">{sub.subjectName}</span>
                      {sub.isActive ? (
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 shrink-0" title="Đang phụ trách"></span>
                      ) : (
                        <span className="w-1.5 h-1.5 rounded-full bg-slate-300 shrink-0" title="Đã thu hồi"></span>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Timestamps */}
          <div className="grid grid-cols-2 gap-3 text-[11px] text-slate-500 pt-2 border-t border-slate-100">
            <div className="flex items-center gap-2">
              <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
              <div>
                <span className="block text-slate-400 text-[10px]">Thời điểm tạo:</span>
                <span className="font-mono text-slate-700">{formatDate(user.createdAt)}</span>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
              <div>
                <span className="block text-slate-400 text-[10px]">Lần cập nhật cuối:</span>
                <span className="font-mono text-slate-700">{formatDate(user.updatedAt)}</span>
              </div>
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-50/80 border-t border-slate-200 flex items-center justify-between">
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
              if (onEdit) onEdit(user);
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
