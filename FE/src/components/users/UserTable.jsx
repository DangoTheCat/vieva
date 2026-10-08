import React, { useState } from 'react';
import { Eye, Edit3, Trash2, Shield, Lock, ShieldCheck, ChevronLeft, ChevronRight, UserX, Copy, Check, KeyRound } from 'lucide-react';
import { TableSkeletonRows } from '../common/Skeleton';
import { useAuth } from '../../context/AuthContext';
import { getUserRole } from '../../utils/roles';

export function UserTable({
  users,
  isLoading,
  pageData,
  onPageChange,
  onViewUser,
  onEditUser,
  onDeleteUser,
  onResetPassword,
  onResetFilters
}) {
  const { currentUser } = useAuth();
  const [copiedId, setCopiedId] = useState(null);

  const handleCopyCode = (code) => {
    if (!code) return;
    navigator.clipboard.writeText(code);
    setCopiedId(code);
    setTimeout(() => setCopiedId(null), 1800);
  };

  const getInitials = (name) => {
    if (!name) return 'US';
    return name
      .split(' ')
      .filter(Boolean)
      .map(part => part[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  };

  const getAvatarBg = (roles = []) => {
    if (roles.includes('ROLE_ADMIN') || roles.includes('ADMIN')) {
      return 'bg-purple-100 text-purple-700 border border-purple-200';
    }
    return 'bg-sky-100 text-sky-700 border border-sky-200';
  };

  const getScope = (user) => {
    if (user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ADMIN')) {
      return '* (ALL COURSES)';
    }
    if (user.userCode?.startsWith('LEC')) {
      return 'SWD392, CS402';
    }
    return 'SWD392 (Class SE1701)';
  };

  const get2FaInfo = (user) => {
    if (user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ADMIN')) {
      return {
        label: 'Hardware FIDO2 / YubiKey',
        color: 'text-emerald-700 font-semibold',
        active: true
      };
    }
    if (user.userCode?.startsWith('LEC')) {
      return {
        label: 'Google Authenticator',
        color: 'text-emerald-700 font-semibold',
        active: true
      };
    }
    return {
      label: 'Google OAuth SSO',
      color: 'text-slate-500',
      active: false
    };
  };

  return (
    <div className="bg-cardBg border border-slate-200/90 rounded-2xl shadow-xs overflow-hidden flex flex-col">
      
      {/* Table Container with horizontal overflow protection */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse text-xs min-w-[840px]">
          <thead>
            <tr className="bg-slate-50/80 border-b border-slate-200/80 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
              <th className="py-3.5 px-4">Người Dùng / Email FPT</th>
              <th className="py-3.5 px-4">Vai Trò Hệ Thống</th>
              <th className="py-3.5 px-4">Phạm Vi Môn Học (Scope)</th>
              <th className="py-3.5 px-4">Trạng Thái</th>
              <th className="py-3.5 px-4">Xác Thực 2FA</th>
              <th className="py-3.5 px-4 text-right">Thao Tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading ? (
              <TableSkeletonRows rows={5} cols={6} />
            ) : !users || users.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-14 text-center text-slate-400 space-y-3">
                  <UserX className="w-12 h-12 mx-auto text-slate-300 stroke-1" />
                  <p className="font-heading font-extrabold text-sm text-slate-700">
                    Không tìm thấy người dùng nào phù hợp
                  </p>
                  <p className="text-xs text-slate-400 max-w-sm mx-auto">
                    Hãy kiểm tra lại từ khóa tìm kiếm hoặc bấm nút bên dưới để khôi phục bộ lọc mặc định.
                  </p>
                  {onResetFilters && (
                    <button
                      type="button"
                      onClick={onResetFilters}
                      className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition shadow-2xs"
                    >
                      Khôi Phục Bộ Lọc
                    </button>
                  )}
                </td>
              </tr>
            ) : (
              users.map((user) => {
                const isSelf = user.userId === currentUser?.userId;
                const userRole = getUserRole(user);
                const isAdmin = userRole === 'ROLE_ADMIN';
                const twoFa = get2FaInfo(user);
                const isCopied = copiedId === user.userCode;

                return (
                  <tr key={user.userId || user.email} className="hover:bg-slate-50/90 transition-colors group">
                    
                    {/* User & Email */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="flex items-center gap-3">
                        <div className={`w-9 h-9 rounded-xl ${getAvatarBg(user.roles)} font-bold flex items-center justify-center text-xs shadow-2xs font-heading`}>
                          {getInitials(user.fullName)}
                        </div>
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span className="font-bold text-slate-900 text-sm">{user.fullName}</span>
                            {isSelf && (
                              <span className="text-[9px] font-bold bg-sky-100 text-sky-800 px-1.5 py-0.5 rounded-full border border-sky-200">
                                Bạn
                              </span>
                            )}
                          </div>
                          <div className="flex items-center gap-2 text-[11px] text-slate-500 font-mono mt-0.5">
                            <span className="truncate max-w-[180px]">{user.email}</span>
                            <span>•</span>
                            <button
                              type="button"
                              onClick={() => handleCopyCode(user.userCode)}
                              className="inline-flex items-center gap-1 hover:text-sky-600 transition"
                              title="Nhấn để sao chép mã"
                            >
                              <span>{user.userCode}</span>
                              {isCopied ? (
                                <Check className="w-3 h-3 text-emerald-600" />
                              ) : (
                                <Copy className="w-3 h-3 opacity-0 group-hover:opacity-100 transition-opacity" />
                              )}
                            </button>
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* Roles */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      {isAdmin ? (
                        <span className="px-2.5 py-1 rounded-full bg-purple-50 text-purple-700 border border-purple-200 font-bold text-[10px] tracking-wide inline-flex items-center gap-1">
                          <ShieldCheck className="w-3 h-3 text-purple-600" />
                          <span>SUPER ADMINISTRATOR</span>
                        </span>
                      ) : userRole === 'ROLE_LECTURER' ? (
                        <span className="px-2.5 py-1 rounded-full bg-sky-50 text-sky-700 border border-sky-200 font-bold text-[10px] tracking-wide">
                          LECTURER / EXAMINER
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 border border-slate-200 font-bold text-[10px] tracking-wide">
                          STUDENT / CANDIDATE
                        </span>
                      )}
                      {user.mustChangePassword && (
                        <span
                          className="ml-1.5 px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200 font-semibold text-[10px]"
                          title="Người dùng chưa đổi mật khẩu tạm thời do admin cấp"
                        >
                          Chờ đổi MK
                        </span>
                      )}
                    </td>

                    {/* Scope */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className={`px-2 py-0.5 rounded-md font-mono font-bold text-xs ${
                        isAdmin
                          ? 'bg-purple-50 text-purple-700 border border-purple-200'
                          : 'bg-slate-100 text-slate-700'
                      }`}>
                        {getScope(user)}
                      </span>
                    </td>

                    {/* Status */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                        user.status === 'ACTIVE'
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : user.status === 'INACTIVE'
                          ? 'bg-amber-50 text-amber-700 border border-amber-200'
                          : user.status === 'BANNED'
                          ? 'bg-purple-50 text-purple-700 border border-purple-200'
                          : 'bg-rose-50 text-rose-700 border border-rose-200'
                      }`}>
                        <span className={`w-1.5 h-1.5 rounded-full ${
                          user.status === 'ACTIVE' ? 'bg-emerald-500' : user.status === 'INACTIVE' ? 'bg-amber-500' : 'bg-rose-500'
                        }`}></span>
                        <span>{user.status || 'ACTIVE'}</span>
                      </span>
                    </td>

                    {/* 2FA */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className={`${twoFa.color} font-medium flex items-center gap-1.5 text-xs`}>
                        {twoFa.active && <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>}
                        <span>{twoFa.label}</span>
                      </span>
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 whitespace-nowrap text-right space-x-1.5">
                      <button
                        onClick={() => onViewUser(user)}
                        className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-600 hover:text-sky-600 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
                        title="Xem chi tiết hồ sơ"
                        aria-label="Xem chi tiết"
                      >
                        <Eye className="w-4 h-4" />
                      </button>

                      <button
                        onClick={() => onEditUser(user)}
                        className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-600 hover:text-sky-600 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
                        title="Chỉnh sửa quyền & thông tin"
                        aria-label="Chỉnh sửa người dùng"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>

                      <button
                        onClick={() => onResetPassword(user)}
                        className="p-1.5 rounded-lg border border-slate-200 hover:bg-amber-50 text-slate-600 hover:text-amber-600 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
                        title="Đặt lại mật khẩu"
                        aria-label="Đặt lại mật khẩu"
                      >
                        <KeyRound className="w-4 h-4" />
                      </button>

                      <button
                        onClick={() => onDeleteUser(user)}
                        disabled={isSelf}
                        className={`p-1.5 rounded-lg border transition focus:outline-none focus:ring-2 ${
                          isSelf
                            ? 'border-slate-200 text-slate-300 cursor-not-allowed bg-slate-50'
                            : 'border-slate-200 hover:bg-rose-50 text-slate-600 hover:text-rose-600 hover:border-rose-200 focus:ring-rose-500'
                        }`}
                        title={isSelf ? 'Không thể tự xóa tài khoản của chính mình (Safety Guard)' : 'Xóa mềm người dùng'}
                        aria-label="Xóa người dùng"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>

                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Bar */}
      {pageData && (
        <div className="p-4 bg-slate-50/80 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500 font-medium">
          <span>
            Hiển thị {users?.length || 0} trên tổng số <strong>{pageData.totalElements || users?.length || 0}</strong> người dùng
          </span>

          <div className="flex items-center gap-1.5">
            <button
              onClick={() => onPageChange(pageData.page - 1)}
              disabled={pageData.isFirst || pageData.page === 0}
              className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <ChevronLeft className="w-3.5 h-3.5" />
              <span>Trước</span>
            </button>

            {Array.from({ length: Math.max(1, pageData.totalPages || 1) }).map((_, pIdx) => {
              if (
                pIdx === 0 ||
                pIdx === (pageData.totalPages - 1) ||
                Math.abs(pIdx - pageData.page) <= 1
              ) {
                return (
                  <button
                    key={pIdx}
                    onClick={() => onPageChange(pIdx)}
                    className={`px-3 py-1.5 rounded-xl font-bold transition text-xs focus:outline-none focus:ring-2 ${
                      pageData.page === pIdx
                        ? 'bg-sky-600 text-white shadow-2xs focus:ring-sky-400'
                        : 'border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 focus:ring-slate-300'
                    }`}
                  >
                    {pIdx + 1}
                  </button>
                );
              }
              if (Math.abs(pIdx - pageData.page) === 2) {
                return <span key={pIdx} className="px-1 text-slate-400 font-mono">...</span>;
              }
              return null;
            })}

            <button
              onClick={() => onPageChange(pageData.page + 1)}
              disabled={pageData.isLast || pageData.page >= (pageData.totalPages - 1)}
              className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <span>Sau</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}

    </div>
  );
}
