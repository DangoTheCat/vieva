import React, { useState } from 'react';
import { Eye, Edit3, Trash2, Shield, Lock, ShieldCheck, ChevronLeft, ChevronRight, UserX, Copy, Check, KeyRound } from 'lucide-react';
import { TableSkeletonRows } from '../common/Skeleton';
import { useAuth } from '../../context/AuthContext';
import { getUserRole } from '../../utils/roles';

function UserAvatarBadge({ user, isSelf, currentUser, getAvatarBg, getInitials }) {
  const [imgError, setImgError] = useState(false);
  const avatarUrl = (isSelf && currentUser?.avatarUrl) 
    ? currentUser.avatarUrl 
    : (user?.avatarUrl || user?.avatar || user?.avatar_url);

  const hasAvatar = Boolean(avatarUrl && typeof avatarUrl === 'string' && avatarUrl.trim().length > 0 && !imgError);

  if (hasAvatar) {
    return (
      <div className="w-9 h-9 rounded-full border border-pink-200 shadow-2xs overflow-hidden shrink-0 bg-white">
        <img
          src={avatarUrl.trim()}
          alt={user.fullName || 'Avatar'}
          className="w-full h-full object-cover"
          onError={() => setImgError(true)}
        />
      </div>
    );
  }

  return (
    <div className={`w-9 h-9 rounded-full ${getAvatarBg(user.roles)} font-semibold flex items-center justify-center text-xs overflow-hidden shrink-0 shadow-2xs`}>
      {getInitials(user.fullName)}
    </div>
  );
}

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
      return 'bg-pink-600 text-white';
    }
    if (roles.includes('ROLE_LECTURER') || roles.includes('LECTURER')) {
      return 'bg-blue-100 text-blue-800 border border-pink-200';
    }
    return 'bg-slate-100 text-slate-700 border border-pink-200';
  };

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch {
      return '—';
    }
  };

  return (
    <div className="bg-white rounded-xl border border-pink-200/90 shadow-2xs overflow-hidden flex flex-col mb-8">
      
      {/* Table Header Info Bar */}
      <div className="bg-slate-50/80 border-b border-pink-200 px-5 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="font-heading text-sm text-ink font-bold">Danh Sách Tài Khoản Phân Quyền</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-semibold bg-slate-100 border border-pink-200 text-slate-600 font-mono">
            {users?.length || 0} / {pageData?.totalElements || users?.length || 0} bản ghi
          </span>
        </div>
      </div>

      {/* Table Container with horizontal overflow protection */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse text-xs min-w-[840px]">
          <thead>
            <tr className="bg-slate-50/50 border-b border-pink-200 text-slate-500 font-sans font-semibold uppercase tracking-wider text-[11px]">
              <th className="py-3 px-4">Người Dùng / Email FPT</th>
              <th className="py-3 px-4">Mã Số</th>
              <th className="py-3 px-4">Vai Trò Hệ Thống</th>
              <th className="py-3 px-4">Trạng Thái &amp; Bảo Mật</th>
              <th className="py-3 px-4">Số Điện Thoại</th>
              <th className="py-3 px-4">Ngày Khởi Tạo</th>
              <th className="py-3 px-4 text-right">Thao Tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 font-sans text-ink">
            {isLoading ? (
              <TableSkeletonRows rows={5} cols={7} />
            ) : !users || users.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-14 text-center text-slate-500 space-y-3">
                  <UserX className="w-10 h-10 mx-auto text-slate-400 stroke-1" />
                  <p className="font-heading font-bold text-sm text-ink">
                    Không tìm thấy người dùng nào phù hợp
                  </p>
                  <p className="text-xs text-slate-500 max-w-sm mx-auto">
                    Hãy kiểm tra lại từ khóa tìm kiếm hoặc bấm nút bên dưới để khôi phục bộ lọc mặc định.
                  </p>
                  {onResetFilters && (
                    <button
                      type="button"
                      onClick={onResetFilters}
                      className="px-3.5 py-1.5 rounded-lg bg-white border border-pink-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold transition shadow-2xs cursor-pointer"
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
                const isCopied = copiedId === user.userCode;

                return (
                  <tr key={user.userId || user.email} className="hover:bg-slate-50/70 transition-colors group">
                    
                    {/* User & Email */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="flex items-center gap-3">
                        <UserAvatarBadge
                          user={user}
                          isSelf={isSelf}
                          currentUser={currentUser}
                          getAvatarBg={getAvatarBg}
                          getInitials={getInitials}
                        />
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span className="font-semibold text-ink text-xs">{user.fullName}</span>
                            {isSelf && (
                              <span className="text-[10px] font-bold bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-pink-200">
                                Bạn
                              </span>
                            )}
                          </div>
                          <div className="text-[11px] text-slate-500 font-sans mt-0.5 truncate max-w-[200px]">
                            {user.email}
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* User Code */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      <button
                        type="button"
                        onClick={() => handleCopyCode(user.userCode)}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded border border-pink-200 bg-slate-50 text-slate-700 font-mono text-xs font-medium hover:bg-slate-100 transition cursor-pointer"
                        title="Nhấn để sao chép mã"
                      >
                        <span>{user.userCode || '—'}</span>
                        {isCopied ? (
                          <Check className="w-3 h-3 text-emerald-600" />
                        ) : (
                          <Copy className="w-3 h-3 opacity-0 group-hover:opacity-100 text-slate-400 transition-opacity" />
                        )}
                      </button>
                    </td>

                    {/* Roles */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      {isAdmin ? (
                        <span className="px-2.5 py-0.5 rounded-full bg-pink-600 text-white font-semibold text-[10px] tracking-wide inline-flex items-center gap-1">
                          <ShieldCheck className="w-3 h-3 text-blue-400" />
                          <span>ROLE_ADMIN</span>
                        </span>
                      ) : userRole === 'ROLE_LECTURER' ? (
                        <span className="px-2.5 py-0.5 rounded-full bg-pink-50 text-pink-700 border border-pink-200 font-semibold text-[10px] tracking-wide inline-flex items-center gap-1">
                          <span>ROLE_LECTURER</span>
                        </span>
                      ) : (
                        <span className="px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-pink-200 font-semibold text-[10px] tracking-wide inline-flex items-center gap-1">
                          <span>ROLE_STUDENT</span>
                        </span>
                      )}
                    </td>

                    {/* Status & Security */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="flex flex-col gap-1 items-start">
                        <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold border ${
                          user.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                            : user.status === 'INACTIVE'
                            ? 'bg-slate-100 text-slate-600 border-pink-200'
                            : 'bg-rose-50 text-rose-700 border-rose-200'
                        }`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${
                            user.status === 'ACTIVE' ? 'bg-emerald-500' : user.status === 'INACTIVE' ? 'bg-slate-400' : 'bg-rose-500'
                          }`}></span>
                          <span>{user.status || 'ACTIVE'}</span>
                        </span>
                        {user.mustChangePassword ? (
                          <span
                            className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-amber-50 text-amber-800 border border-amber-200 text-[10px] font-medium"
                            title="Tài khoản đang dùng mật khẩu tạm, người dùng phải đổi khi đăng nhập"
                          >
                            <KeyRound className="w-2.5 h-2.5 text-amber-600" />
                            <span>Chờ đổi MK</span>
                          </span>
                        ) : null}
                      </div>
                    </td>

                    {/* Phone Number */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      <span className="font-mono text-slate-600 text-xs">
                        {user.phoneNumber || <span className="text-slate-400 italic">Chưa cập nhật</span>}
                      </span>
                    </td>

                    {/* Created At */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      <span className="font-mono text-slate-500 text-xs">
                        {formatDate(user.createdAt)}
                      </span>
                    </td>

                    {/* Actions */}
                    <td className="py-3 px-4 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => onViewUser(user)}
                          className="p-1.5 rounded-lg border border-pink-200 hover:bg-slate-100 text-slate-600 transition cursor-pointer"
                          title="Xem chi tiết hồ sơ"
                          aria-label="Xem chi tiết"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </button>

                        <button
                          onClick={() => onEditUser(user)}
                          className="p-1.5 rounded-lg border border-pink-200 hover:bg-pink-50 hover:text-pink-600 hover:border-pink-200 text-slate-600 transition cursor-pointer"
                          title="Chỉnh sửa quyền & thông tin"
                          aria-label="Chỉnh sửa người dùng"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                        </button>

                        <button
                          onClick={() => onResetPassword(user)}
                          className="p-1.5 rounded-lg border border-pink-200 hover:bg-amber-50 hover:text-amber-600 hover:border-amber-200 text-slate-600 transition cursor-pointer"
                          title="Đặt lại mật khẩu"
                          aria-label="Đặt lại mật khẩu"
                        >
                          <KeyRound className="w-3.5 h-3.5" />
                        </button>

                        <button
                          onClick={() => onDeleteUser(user)}
                          disabled={isSelf}
                          className={`p-1.5 rounded-lg border transition ${
                            isSelf
                              ? 'border-pink-200 bg-slate-50 opacity-40 cursor-not-allowed text-slate-400'
                              : 'border-pink-200 hover:bg-rose-50 hover:text-rose-600 hover:border-rose-200 text-slate-600 cursor-pointer'
                          }`}
                          title={isSelf ? 'Không thể tự xóa tài khoản của chính mình' : 'Khóa tài khoản'}
                          aria-label="Khóa người dùng"
                        >
                          {isSelf ? <Lock className="w-3.5 h-3.5" /> : <Trash2 className="w-3.5 h-3.5" />}
                        </button>
                      </div>
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
        <div className="border-t border-pink-200 px-5 py-3 bg-white flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500 font-medium">
          <div>
            Hiển thị <span className="font-semibold text-ink">{users?.length || 0}</span> trên tổng số <span className="font-semibold text-ink">{pageData.totalElements || users?.length || 0}</span> người dùng
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={() => onPageChange(pageData.page - 1)}
              disabled={pageData.isFirst || pageData.page === 0}
              className="px-3 py-1.5 rounded-lg border border-pink-200 bg-white text-slate-700 font-semibold hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 transition shadow-2xs active:scale-[0.98] cursor-pointer"
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
                    className={`w-8 h-8 rounded-lg font-semibold transition text-xs flex items-center justify-center cursor-pointer ${
                      pageData.page === pIdx
                        ? 'bg-pink-600 text-white shadow-xs'
                        : 'bg-white border border-pink-200 text-slate-700 hover:bg-slate-50'
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
              className="px-3 py-1.5 rounded-lg border border-pink-200 bg-white text-slate-700 font-semibold hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 transition shadow-2xs active:scale-[0.98] cursor-pointer"
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
