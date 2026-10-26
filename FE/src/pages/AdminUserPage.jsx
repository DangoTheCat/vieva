import React, { useState, useEffect, useCallback } from 'react';
import { adminUserApi } from '../api/adminUserApi';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import { UserTable } from '../components/users/UserTable';
import { UserFilterBar } from '../components/users/UserFilterBar';
import { CreateUserModal } from '../components/users/CreateUserModal';
import { EditUserModal } from '../components/users/EditUserModal';
import { UserDetailModal } from '../components/users/UserDetailModal';
import { ConfirmModal } from '../components/common/ConfirmModal';
import { ResetPasswordModal } from '../components/users/ResetPasswordModal';
import { 
  ShieldCheck, 
  Users, 
  UserCheck, 
  GraduationCap, 
  Table2, 
  UserPlus, 
  RefreshCw,
  AlertCircle,
  WifiOff,
  RotateCcw
} from 'lucide-react';

export function AdminUserPage({ onOpenMatrix, showToast }) {
  const { currentUser } = useAuth();

  // Search & Filter state
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('DESC');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  // Data state
  const [users, setUsers] = useState([]);
  const [pageData, setPageData] = useState({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 1,
    isFirst: true,
    isLast: true
  });
  const [isLoading, setIsLoading] = useState(false);
  const [fetchError, setFetchError] = useState('');

  // Resilience: 60s Reconnect Countdown
  const [reconnectCountdown, setReconnectCountdown] = useState(60);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [viewingUser, setViewingUser] = useState(null);
  const [deletingUser, setDeletingUser] = useState(null);
  const [resettingUser, setResettingUser] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Fetch users function
  const fetchUsers = useCallback(async () => {
    setIsLoading(true);
    setFetchError('');
    try {
      const result = await adminUserApi.getUsers({
        keyword: keyword.trim() || undefined,
        role: role || undefined,
        status: status || undefined,
        page,
        size,
        sortBy,
        sortDirection
      });

      setUsers(result.content || []);
      setPageData({
        page: result.page || 0,
        size: result.size || size,
        totalElements: result.totalElements || 0,
        totalPages: result.totalPages || 1,
        isFirst: result.first ?? (page === 0),
        isLast: result.last ?? true
      });
      setReconnectCountdown(60);
    } catch (err) {
      const msg = getErrorMessage(err);
      setFetchError(msg);
      setUsers([]);
      setPageData({
        page: 0,
        size,
        totalElements: 0,
        totalPages: 1,
        isFirst: true,
        isLast: true
      });
    } finally {
      setIsLoading(false);
    }
  }, [keyword, role, status, page, size, sortBy, sortDirection]);

  // Debounced fetch on filter/search change
  useEffect(() => {
    const timer = setTimeout(() => {
      fetchUsers();
    }, 220);
    return () => clearTimeout(timer);
  }, [fetchUsers]);

  // Reconnect countdown timer while the user list cannot be fetched
  useEffect(() => {
    if (!fetchError) return;

    const interval = setInterval(() => {
      setReconnectCountdown((prev) => {
        if (prev <= 1) {
          fetchUsers();
          return 60;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [fetchError, fetchUsers]);

  const handleResetFilters = () => {
    setKeyword('');
    setRole('');
    setStatus('');
    setSortBy('createdAt');
    setSortDirection('DESC');
    setPage(0);
  };

  const handleCreateSuccess = () => {
    fetchUsers();
  };

  const handleUpdateSuccess = (updatedUser) => {
    setUsers(prev => prev.map(u => u.userId === updatedUser.userId ? updatedUser : u));
    fetchUsers();
  };

  const handleDeleteConfirm = async () => {
    if (!deletingUser) return;

    if (deletingUser.userId === currentUser?.userId) {
      showToast({ type: 'error', message: 'Bạn không thể tự xóa tài khoản của chính mình (Error 1011).' });
      setDeletingUser(null);
      return;
    }

    setIsDeleting(true);
    try {
      await adminUserApi.deleteUser(deletingUser.userId);
      showToast({ type: 'success', message: `Xóa thành công người dùng ${deletingUser.fullName}!` });
      setDeletingUser(null);
      fetchUsers();
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsDeleting(false);
    }
  };

  // Metrics
  const adminCount = users.filter(u => u.roles?.some(r => r.includes('ADMIN'))).length;
  const activeCount = users.filter(u => u.status === 'ACTIVE').length;

  return (
    <div className="space-y-6">
      
      {/* Solitary Expressionist Heart Motif (Pastel Stagebox Papercraft Cutout) */}
      <div className="relative w-full mb-3 flex items-center justify-between pointer-events-none select-none">
        <div className="flex items-center gap-2">
          <div className="h-2 w-12 bg-green-500 rounded-full"></div>
          <div className="h-2 w-4 bg-pink-400 rounded-full"></div>
          <div className="h-2 w-2 bg-cream rounded-full"></div>
        </div>
        <div className="flex items-center gap-1.5 text-pink-700/80 pr-2">
          <svg className="inline-block transform -rotate-6" fill="none" height="22" viewBox="0 0 24 24" width="22">
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" fill="#DEB9B5" stroke="#819266" strokeDasharray="2 1" strokeWidth="1.5" />
            <path d="M12 4v16" stroke="#819266" strokeLinecap="round" strokeWidth="1.5" />
          </svg>
          <span className="font-heading text-xs tracking-widest uppercase text-green-700 font-semibold">STAGE 07 • ACCESS CONTROL MATRIX</span>
        </div>
      </div>

      {/* HEADER SECTION */}
      <header className="flex flex-col xl:flex-row xl:items-start justify-between gap-4 mb-6">
        <div className="flex flex-col max-w-3xl">
          {/* Top Meta Pills */}
          <div className="flex flex-wrap items-center gap-2 mb-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-green-100 border-2 border-green-500 text-ink font-sans text-xs font-semibold shadow-[2px_2px_0px_#DEB9B5]">
              <ShieldCheck className="w-4 h-4 text-green-700" />
              Bảo Mật Hệ Thống &amp; Quản Trị Phân Quyền
            </span>
            <span className="text-xs text-on-surface-variant font-medium tracking-wide">
              Module 7 • Admin User Management
            </span>
          </div>
          {/* Headline in Serif */}
          <h1 className="font-heading text-2xl md:text-3xl font-bold text-ink leading-tight tracking-tight mt-1 mb-2">
            Quản Lý Người Dùng &amp; Phân Quyền Theo Môn (Course RBAC)
          </h1>
          {/* Subtitle */}
          <p className="font-sans text-xs md:text-sm text-on-surface-variant leading-relaxed">
            Giảng viên chỉ có quyền truy cập, chỉnh sửa ngân hàng câu hỏi và chấm thi trong phạm vi môn học được phân công (Course-Scoped Access Control theo chuẩn Onion Architecture).
          </p>
        </div>

        {/* Header Actions Panel */}
        <div className="flex flex-wrap items-center gap-2 pt-1 shrink-0">
          <button
            onClick={() => fetchUsers()}
            disabled={isLoading}
            className="h-11 w-11 flex items-center justify-center rounded-xl bg-surface-container-lowest border-2 border-green-500 text-ink shadow-[3px_3px_0px_#DEB9B5] hover:shadow-[1px_1px_0px_#DEB9B5] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all duration-150 cursor-pointer disabled:opacity-50"
            title="Làm mới danh sách dữ liệu"
            aria-label="Làm mới danh sách"
          >
            <RefreshCw className={`w-5 h-5 text-green-800 ${isLoading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={onOpenMatrix}
            className="h-11 px-4 flex items-center gap-2 rounded-xl bg-surface-container-lowest border-2 border-green-500 text-ink font-sans text-xs font-bold shadow-[3px_3px_0px_#DEB9B5] hover:bg-green-50 hover:shadow-[1px_1px_0px_#DEB9B5] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all duration-150 cursor-pointer"
          >
            <Table2 className="w-4 h-4 text-green-700" />
            <span>Ma Trận Phân Quyền</span>
          </button>

          <button
            onClick={() => setIsCreateOpen(true)}
            className="h-11 px-5 flex items-center gap-2 rounded-xl bg-pink-500 border-2 border-ink text-ink font-sans text-xs font-bold shadow-[3px_3px_0px_#819266] hover:bg-pink-400 hover:shadow-[1px_1px_0px_#819266] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all duration-150 cursor-pointer"
          >
            <UserPlus className="w-4 h-4 font-bold" />
            <span>+ Thêm Người Dùng</span>
          </button>
        </div>
      </header>

      {/* HARDENED RECONNECT & OFFLINE BANNER (fe-resilience-hardening) */}
      {fetchError && (
        <div className="p-4 bg-pink-50 border-2 border-pink-700 rounded-2xl text-pink-900 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-[4px_4px_0px_#713D38] animate-modal-entry mb-6">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-pink-100 border-2 border-pink-700 flex items-center justify-center text-pink-800 shrink-0 shadow-[2px_2px_0px_#DEB9B5]">
              <WifiOff className="w-4 h-4" />
            </div>
            <div>
              <p className="font-bold text-pink-950">
                Không thể kết nối đến Máy chủ Backend Spring Boot (Cổng 8080)
              </p>
              <p className="text-[11px] text-pink-800 mt-0.5">
                Hệ thống tự động thử kết nối lại sau: <strong className="font-mono">{reconnectCountdown}s</strong>
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 self-end sm:self-auto">
            <button
              onClick={() => {
                setReconnectCountdown(60);
                fetchUsers();
              }}
              className="px-3 py-1.5 rounded-xl bg-white border-2 border-green-500 hover:bg-green-50 text-ink text-xs font-bold transition flex items-center gap-1.5 shadow-[2px_2px_0px_#DEB9B5]"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Thử Lại Ngay</span>
            </button>
          </div>
        </div>
      )}

      {/* METRIC OVERVIEW CARDS */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {/* Card 1: Total Users */}
        <div className="relative bg-surface-container-lowest rounded-2xl border-2 border-green-500 p-5 shadow-[3px_3px_0px_#DEB9B5] flex flex-col justify-between overflow-hidden group">
          <div className="flex items-start justify-between">
            <span className="font-sans text-xs font-semibold text-on-surface-variant">Tổng Số Người Dùng</span>
            <div className="w-10 h-10 rounded-xl bg-green-50 border-2 border-green-500 flex items-center justify-center text-green-800 shadow-[2px_2px_0px_#DEB9B5]">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-heading text-3xl font-bold text-ink leading-tight">
              {pageData.totalElements || users.length}
            </div>
            <p className="font-sans text-xs text-green-700 mt-1 flex items-center gap-1.5 font-medium">
              <span className="inline-block w-2 h-2 rounded-full bg-green-500"></span>
              Đã ghi danh trong hệ thống
            </p>
          </div>
          <div className="absolute -bottom-6 -right-6 w-16 h-16 rounded-full bg-pink-100/40 pointer-events-none"></div>
        </div>

        {/* Card 2: Admin Count */}
        <div className="relative bg-surface-container-lowest rounded-2xl border-2 border-green-500 p-5 shadow-[3px_3px_0px_#DEB9B5] flex flex-col justify-between overflow-hidden group">
          <div className="flex items-start justify-between">
            <span className="font-sans text-xs font-semibold text-on-surface-variant">Quản Trị Viên (Admin)</span>
            <div className="w-10 h-10 rounded-xl bg-pink-100 border-2 border-pink-700 flex items-center justify-center text-pink-800 shadow-[2px_2px_0px_#DEB9B5]">
              <ShieldCheck className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-heading text-3xl font-bold text-pink-700 leading-tight">
              {adminCount.toString().padStart(2, '0')}
            </div>
            <p className="font-sans text-xs text-pink-800 mt-1 flex items-center gap-1.5 font-medium">
              <span className="inline-block w-2 h-2 rounded-full bg-pink-600"></span>
              Quyền quản trị toàn hệ thống
            </p>
          </div>
          <div className="absolute -bottom-6 -right-6 w-16 h-16 rounded-full bg-cream/30 pointer-events-none"></div>
        </div>

        {/* Card 3: Active Accounts */}
        <div className="relative bg-surface-container-lowest rounded-2xl border-2 border-green-500 p-5 shadow-[3px_3px_0px_#DEB9B5] flex flex-col justify-between overflow-hidden group">
          <div className="flex items-start justify-between">
            <span className="font-sans text-xs font-semibold text-on-surface-variant">Đang Hoạt Động</span>
            <div className="w-10 h-10 rounded-xl bg-green-100 border-2 border-green-600 flex items-center justify-center text-green-800 shadow-[2px_2px_0px_#DEB9B5]">
              <UserCheck className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-heading text-3xl font-bold text-green-600 leading-tight">
              {activeCount.toString().padStart(2, '0')}
            </div>
            <p className="font-sans text-xs text-green-800 mt-1 flex items-center gap-1.5 font-medium">
              <span className="inline-block w-2 h-2 rounded-full bg-green-600"></span>
              Trạng thái ACTIVE
            </p>
          </div>
          <div className="absolute -bottom-6 -right-6 w-16 h-16 rounded-full bg-green-200/40 pointer-events-none"></div>
        </div>

        {/* Card 4: Exam Scope / RLS */}
        <div className="relative bg-surface-container-lowest rounded-2xl border-2 border-green-500 p-5 shadow-[3px_3px_0px_#DEB9B5] flex flex-col justify-between overflow-hidden group">
          <div className="flex items-start justify-between">
            <span className="font-sans text-xs font-semibold text-on-surface-variant">Phạm Vi Khảo Thí</span>
            <div className="w-10 h-10 rounded-xl bg-pink-50 border-2 border-green-500 flex items-center justify-center text-ink shadow-[2px_2px_0px_#DEB9B5]">
              <GraduationCap className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-heading text-3xl font-bold text-ink leading-tight">
              SWD392
            </div>
            <p className="font-sans text-xs text-status-info mt-1 flex items-center gap-1.5 font-medium truncate">
              <span className="inline-block w-2 h-2 rounded-full bg-status-info"></span>
              Row-Level Security Enforced
            </p>
          </div>
          <div className="absolute -bottom-6 -right-6 w-16 h-16 rounded-full bg-pink-300/30 pointer-events-none"></div>
        </div>
      </section>

      {/* FILTER & SEARCH TOOLBAR */}
      <UserFilterBar
        keyword={keyword}
        setKeyword={setKeyword}
        role={role}
        setRole={setRole}
        status={status}
        setStatus={setStatus}
        sortBy={sortBy}
        setSortBy={setSortBy}
        sortDirection={sortDirection}
        setSortDirection={setSortDirection}
        onReset={handleResetFilters}
      />

      {/* USERS RBAC TABLE */}
      <UserTable
        users={users}
        isLoading={isLoading}
        pageData={pageData}
        onPageChange={(newPage) => setPage(newPage)}
        onViewUser={(user) => setViewingUser(user)}
        onEditUser={(user) => setEditingUser(user)}
        onDeleteUser={(user) => setDeletingUser(user)}
        onResetPassword={(user) => setResettingUser(user)}
        onResetFilters={handleResetFilters}
      />

      {/* Architectural RBAC Information Panel (Callout Note) */}
      <aside className="bg-green-50 rounded-2xl border-2 border-green-500 p-5 shadow-[3px_3px_0px_#DEB9B5] flex flex-col md:flex-row items-start gap-4 mb-6">
        <div className="w-12 h-12 rounded-2xl bg-white border-2 border-green-600 flex items-center justify-center text-green-700 shadow-[2px_2px_0px_#DEB9B5] shrink-0">
          <ShieldCheck className="w-7 h-7" />
        </div>
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1 flex-wrap">
            <h4 className="font-heading text-base md:text-lg font-bold text-ink">Nguyên tắc Phân quyền Course RBAC • FPTU Oral Exam</h4>
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-green-200 text-green-900 border border-green-600">Row-Level Security</span>
          </div>
          <p className="font-sans text-xs md:text-sm text-on-surface-variant leading-relaxed">
            Hệ thống vận hành theo kiến trúc Onion Architecture. Giảng viên phụ trách môn nào chỉ được tải Rubric, xem thống kê ca thi, tạo cấu hình bài thi và chấm điểm sinh viên thuộc môn đó. Các can thiệp chéo môn bị chặn tại tầng Application Core và ghi nhận vào Telemetry Log.
          </p>
        </div>
      </aside>

      {/* MODALS */}
      <CreateUserModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={handleCreateSuccess}
        showToast={showToast}
      />

      <EditUserModal
        isOpen={Boolean(editingUser)}
        user={editingUser}
        onClose={() => setEditingUser(null)}
        onSuccess={handleUpdateSuccess}
        showToast={showToast}
      />

      <UserDetailModal
        isOpen={Boolean(viewingUser)}
        user={viewingUser}
        onClose={() => setViewingUser(null)}
        onEdit={(user) => setEditingUser(user)}
      />

      <ConfirmModal
        isOpen={Boolean(deletingUser)}
        title="Xác Nhận Xóa Mềm Người Dùng"
        message={`Bạn có chắc chắn muốn xóa mềm người dùng "${deletingUser?.fullName}" (${deletingUser?.email})? Trạng thái tài khoản sẽ chuyển thành DELETED và người dùng sẽ không thể đăng nhập vào hệ thống thi. Dữ liệu lịch sử chấm thi và telemetry sẽ được lưu trữ bảo toàn.`}
        confirmText="Xác Nhận Xóa"
        isDanger={true}
        isLoading={isDeleting}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeletingUser(null)}
      />

      <ResetPasswordModal
        user={resettingUser}
        onClose={() => setResettingUser(null)}
        showToast={showToast}
      />

    </div>
  );
}
