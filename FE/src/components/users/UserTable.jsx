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
      <div className="w-10 h-10 rounded-xl border-2 border-green-600 shadow-[2px_2px_0px_#DEB9B5] overflow-hidden shrink-0 bg-paper">
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
    <div className={`w-10 h-10 rounded-xl ${getAvatarBg(user.roles)} font-bold flex items-center justify-center text-xs overflow-hidden shrink-0`}>
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
      return 'bg-tertiary-fixed-dim text-ink border-2 border-green-700 shadow-[2px_2px_0px_#819266]';
    }
    if (roles.includes('ROLE_LECTURER') || roles.includes('LECTURER')) {
      return 'bg-pink-200 text-pink-900 border-2 border-green-600 shadow-[2px_2px_0px_#DEB9B5]';
    }
    return 'bg-green-100 text-green-800 border-2 border-green-500 shadow-[2px_2px_0px_#DEB9B5]';
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
    <div className="bg-surface-container-lowest rounded-2xl border-2 border-green-500 shadow-[6px_6px_0px_#D5A6A2] overflow-hidden flex flex-col mb-8">
      
      {/* Table Header Info Bar */}
      <div className="bg-green-50/70 border-b-2 border-green-500 px-5 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-green-700 text-[20px]">badge</span>
          <span className="font-heading text-base md:text-lg text-ink font-bold">Danh Sách Tài Khoản Phân Quyền</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-pink-50 border-2 border-pink-400 text-pink-800 shadow-[1px_1px_0px_#DEB9B5]">
            {users?.length || 0} / {pageData?.totalElements || users?.length || 0} Bản ghi
          </span>
        </div>
      </div>

      {/* Table Container with horizontal overflow protection */}
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse text-xs min-w-[840px]">
          <thead>
            <tr className="bg-surface-container-low border-b-2 border-green-500 text-green-900 font-sans font-bold uppercase tracking-wider text-[11px]">
              <th className="py-3.5 px-4">Người Dùng / Email FPT</th>
              <th className="py-3.5 px-4">Mã Số</th>
              <th className="py-3.5 px-4">Vai Trò Hệ Thống</th>
              <th className="py-3.5 px-4">Trạng Thái &amp; Bảo Mật</th>
              <th className="py-3.5 px-4">Số Điện Thoại</th>
              <th className="py-3.5 px-4">Ngày Khởi Tạo</th>
              <th className="py-3.5 px-4 text-right">Thao Tác</th>
            </tr>
          </thead>
          <tbody className="divide-y-2 divide-green-200 font-sans text-ink">
            {isLoading ? (
              <TableSkeletonRows rows={5} cols={7} />
            ) : !users || users.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-14 text-center text-on-surface-variant space-y-3">
                  <UserX className="w-12 h-12 mx-auto text-green-600 stroke-1" />
                  <p className="font-heading font-bold text-base text-ink">
                    Không tìm thấy người dùng nào phù hợp
                  </p>
                  <p className="text-xs text-on-surface-variant max-w-sm mx-auto">
                    Hãy kiểm tra lại từ khóa tìm kiếm hoặc bấm nút bên dưới để khôi phục bộ lọc mặc định.
                  </p>
                  {onResetFilters && (
                    <button
                      type="button"
                      onClick={onResetFilters}
                      className="px-4 py-2 rounded-xl bg-pink-100 border-2 border-green-500 hover:bg-pink-200 text-pink-800 text-xs font-bold transition shadow-[2px_2px_0px_#DEB9B5] cursor-pointer"
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
                  <tr key={user.userId || user.email} className="hover:bg-green-50/50 transition-colors group">
                    
                    {/* User & Email */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
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
                            <span className="font-bold text-ink text-sm">{user.fullName}</span>
                            {isSelf && (
                              <span className="text-[10px] font-bold bg-pink-100 text-pink-900 px-1.5 py-0.5 rounded-full border border-pink-300">
                                Bạn
                              </span>
                            )}
                          </div>
                          <div className="text-[11px] text-on-surface-variant font-sans mt-0.5 truncate max-w-[200px]">
                            {user.email}
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* User Code */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <button
                        type="button"
                        onClick={() => handleCopyCode(user.userCode)}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded border border-green-500 bg-paper text-green-900 font-mono text-xs font-semibold shadow-[1px_1px_0px_#DEB9B5] hover:bg-green-100 transition cursor-pointer"
                        title="Nhấn để sao chép mã"
                      >
                        <span>{user.userCode || '—'}</span>
                        {isCopied ? (
                          <Check className="w-3 h-3 text-green-700" />
                        ) : (
                          <Copy className="w-3 h-3 opacity-0 group-hover:opacity-100 text-green-700 transition-opacity" />
                        )}
                      </button>
                    </td>

                    {/* Roles */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      {isAdmin ? (
                        <span className="px-2.5 py-0.5 rounded-full bg-cream text-ink border-2 border-green-700 font-bold text-[10px] tracking-wide inline-flex items-center gap-1 shadow-[1px_1px_0px_#DEB9B5]">
                          <ShieldCheck className="w-3 h-3 text-green-800" />
                          <span>ROLE_ADMIN</span>
                        </span>
                      ) : userRole === 'ROLE_LECTURER' ? (
                        <span className="px-2.5 py-0.5 rounded-full bg-pink-100 text-pink-900 border-2 border-green-500 font-bold text-[10px] tracking-wide inline-flex items-center gap-1 shadow-[1px_1px_0px_#DEB9B5]">
                          <span>ROLE_LECTURER</span>
                        </span>
                      ) : (
                        <span className="px-2.5 py-0.5 rounded-full bg-green-50 text-green-800 border-2 border-green-500 font-bold text-[10px] tracking-wide inline-flex items-center gap-1 shadow-[1px_1px_0px_#DEB9B5]">
                          <span>ROLE_STUDENT</span>
                        </span>
                      )}
                    </td>

                    {/* Status & Security */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="flex flex-col gap-1 items-start">
                        <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                          user.status === 'ACTIVE'
                            ? 'bg-green-100 text-green-800 border-2 border-green-500'
                            : user.status === 'INACTIVE'
                            ? 'bg-surface-container text-on-surface-variant border-2 border-outline-variant'
                            : 'bg-pink-100 text-pink-900 border-2 border-pink-500'
                        }`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${
                            user.status === 'ACTIVE' ? 'bg-green-600' : user.status === 'INACTIVE' ? 'bg-outline' : 'bg-pink-600'
                          }`}></span>
                          <span>{user.status || 'ACTIVE'}</span>
                        </span>
                        {user.mustChangePassword ? (
                          <span
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-pink-50 text-pink-800 border border-pink-300 text-[10px] font-semibold"
                            title="Tài khoản đang dùng mật khẩu tạm, người dùng phải đổi khi đăng nhập"
                          >
                            <KeyRound className="w-2.5 h-2.5 text-pink-700" />
                            <span>Chờ đổi MK</span>
                          </span>
                        ) : (
                          <span className="text-[10px] text-green-700 font-medium px-1">Đã kích hoạt</span>
                        )}
                      </div>
                    </td>

                    {/* Phone Number */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="font-mono text-ink text-xs">
                        {user.phoneNumber || <span className="text-on-surface-variant italic">Chưa cập nhật</span>}
                      </span>
                    </td>

                    {/* Created At */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="font-mono text-on-surface-variant text-xs">
                        {formatDate(user.createdAt)}
                      </span>
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => onViewUser(user)}
                          className="p-1.5 rounded-lg border border-green-500 bg-white hover:bg-green-100 text-green-800 shadow-[1px_1px_0px_#DEB9B5] transition-all cursor-pointer"
                          title="Xem chi tiết hồ sơ"
                          aria-label="Xem chi tiết"
                        >
                          <Eye className="w-4 h-4" />
                        </button>

                        <button
                          onClick={() => onEditUser(user)}
                          className="p-1.5 rounded-lg border border-green-500 bg-white hover:bg-pink-100 text-pink-700 shadow-[1px_1px_0px_#DEB9B5] transition-all cursor-pointer"
                          title="Chỉnh sửa quyền & thông tin"
                          aria-label="Chỉnh sửa người dùng"
                        >
                          <Edit3 className="w-4 h-4" />
                        </button>

                        <button
                          onClick={() => onResetPassword(user)}
                          className="p-1.5 rounded-lg border border-green-500 bg-white hover:bg-cream text-ink shadow-[1px_1px_0px_#DEB9B5] transition-all cursor-pointer"
                          title="Đặt lại mật khẩu"
                          aria-label="Đặt lại mật khẩu"
                        >
                          <KeyRound className="w-4 h-4" />
                        </button>

                        <button
                          onClick={() => onDeleteUser(user)}
                          disabled={isSelf}
                          className={`p-1.5 rounded-lg border transition-all ${
                            isSelf
                              ? 'border-outline-variant bg-surface-container opacity-40 cursor-not-allowed text-outline'
                              : 'border-pink-700 bg-pink-50 hover:bg-pink-300 text-pink-800 shadow-[1px_1px_0px_#DEB9B5] cursor-pointer'
                          }`}
                          title={isSelf ? 'Không thể tự xóa tài khoản của chính mình (Safety Guard)' : 'Xóa mềm người dùng'}
                          aria-label="Xóa người dùng"
                        >
                          {isSelf ? <Lock className="w-4 h-4" /> : <Trash2 className="w-4 h-4" />}
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
        <div className="border-t-2 border-green-500 px-5 py-3.5 bg-paper flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-on-surface-variant font-medium">
          <div>
            Hiển thị <span className="font-bold text-ink">{users?.length || 0}</span> trên tổng số <span className="font-bold text-ink">{pageData.totalElements || users?.length || 0}</span> người dùng
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={() => onPageChange(pageData.page - 1)}
              disabled={pageData.isFirst || pageData.page === 0}
              className="px-3 py-1.5 rounded-xl border-2 border-green-500 bg-surface-container-lowest text-on-surface-variant font-bold hover:bg-green-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 transition shadow-[2px_2px_0px_#DEB9B5] active:translate-x-[1px] active:translate-y-[1px] cursor-pointer"
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
                    className={`w-9 h-9 rounded-xl font-bold transition text-xs flex items-center justify-center cursor-pointer ${
                      pageData.page === pIdx
                        ? 'border-2 border-green-700 bg-pink-500 text-ink shadow-[2px_2px_0px_#713D38]'
                        : 'border-2 border-green-500 bg-surface-container-lowest text-ink hover:bg-green-100 shadow-[2px_2px_0px_#DEB9B5]'
                    }`}
                  >
                    {pIdx + 1}
                  </button>
                );
              }
              if (Math.abs(pIdx - pageData.page) === 2) {
                return <span key={pIdx} className="px-1 text-on-surface-variant font-mono">...</span>;
              }
              return null;
            })}

            <button
              onClick={() => onPageChange(pageData.page + 1)}
              disabled={pageData.isLast || pageData.page >= (pageData.totalPages - 1)}
              className="px-3 py-1.5 rounded-xl border-2 border-green-500 bg-surface-container-lowest text-on-surface-variant font-bold hover:bg-green-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 transition shadow-[2px_2px_0px_#DEB9B5] active:translate-x-[1px] active:translate-y-[1px] cursor-pointer"
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
