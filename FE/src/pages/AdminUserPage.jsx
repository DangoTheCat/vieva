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
  const { isDemoMode, setIsDemoMode, currentUser } = useAuth();

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
        isFirst: result.isFirst ?? (page === 0),
        isLast: result.isLast ?? true
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

  // Reconnect countdown timer when there is a fetch error and not in demo mode
  useEffect(() => {
    if (!fetchError || isDemoMode) return;

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
  }, [fetchError, isDemoMode, fetchUsers]);

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
      if (isDemoMode) {
        setUsers(prev => prev.filter(u => u.userId !== deletingUser.userId));
        showToast({ type: 'success', message: `Đã xóa mềm người dùng ${deletingUser.fullName} (Demo Mode).` });
        setDeletingUser(null);
        return;
      }

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
      
      {/* HEADER SECTION */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <span className="text-[11px] font-bold text-sky-700 bg-sky-50 px-2.5 py-0.5 rounded-full border border-sky-200">
              Bảo Mật Hệ Thống & Quản Trị Phân Quyền
            </span>
            <span className="text-[11px] font-mono text-slate-400">
              Module 7 • Admin User Management
            </span>
          </div>
          <h1 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
            Quản Lý Người Dùng & Phân Quyền Theo Môn (Course RBAC)
          </h1>
          <p className="text-xs text-slate-500 mt-1 max-w-2xl leading-relaxed">
            Giảng viên chỉ có quyền truy cập, chỉnh sửa ngân hàng câu hỏi và chấm thi trong phạm vi môn học được phân công (Course-Scoped Access Control theo chuẩn Onion Architecture).
          </p>
        </div>

        <div className="flex items-center gap-2.5 self-start md:self-auto">
          <button
            onClick={() => fetchUsers()}
            disabled={isLoading}
            className="p-2.5 rounded-xl border border-slate-300 bg-cardBg hover:bg-slate-50 text-slate-600 hover:text-slate-900 shadow-2xs transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-500"
            title="Làm mới danh sách dữ liệu"
            aria-label="Làm mới danh sách"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-sky-600' : ''}`} />
          </button>

          <button
            onClick={onOpenMatrix}
            className="px-4 py-2.5 rounded-xl border border-slate-300 bg-cardBg hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center gap-1.5 shadow-2xs transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
            <Table2 className="w-4 h-4 text-sky-600" />
            <span>Ma Trận Phân Quyền</span>
          </button>

          <button
            onClick={() => setIsCreateOpen(true)}
            className="px-4 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold flex items-center gap-1.5 shadow-sm shadow-sky-600/20 transition active:scale-95 focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
            <UserPlus className="w-4 h-4" />
            <span>Thêm Người Dùng</span>
          </button>
        </div>
      </div>

      {/* HARDENED RECONNECT & OFFLINE BANNER (fe-resilience-hardening) */}
      {fetchError && !isDemoMode && (
        <div className="p-4 bg-amber-50 border border-amber-200/90 rounded-2xl text-amber-900 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xs animate-modal-entry">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-xl bg-amber-100 flex items-center justify-center text-amber-700 shrink-0">
              <WifiOff className="w-4 h-4" />
            </div>
            <div>
              <p className="font-bold text-amber-950">
                Không thể kết nối đến Máy chủ Backend Spring Boot (Cổng 8080)
              </p>
              <p className="text-[11px] text-amber-700 mt-0.5">
                Đang hiển thị dữ liệu dự phòng. Hệ thống tự động thử kết nối lại sau: <strong className="font-mono">{reconnectCountdown}s</strong>
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 self-end sm:self-auto">
            <button
              onClick={() => {
                setReconnectCountdown(60);
                fetchUsers();
              }}
              className="px-3 py-1.5 rounded-xl bg-white border border-amber-300 hover:bg-amber-100/80 text-amber-900 text-xs font-bold transition flex items-center gap-1.5"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Thử Lại Ngay</span>
            </button>
            <button
              onClick={() => setIsDemoMode(true)}
              className="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold transition shadow-xs"
            >
              Chuyển Demo Mode
            </button>
          </div>
        </div>
      )}

      {/* METRIC OVERVIEW CARDS */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 bg-white rounded-2xl border border-slate-200/90 shadow-2xs space-y-1 hover:border-slate-300 transition-colors">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold text-slate-600">Tổng Số Người Dùng</span>
            <div className="w-7 h-7 rounded-lg bg-sky-50 flex items-center justify-center text-sky-600">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <p className="font-heading font-extrabold text-2xl text-slate-900">
            {pageData.totalElements || users.length}
          </p>
          <div className="flex items-center gap-1.5 text-[11px] text-slate-400 pt-0.5">
            <span className="w-1.5 h-1.5 rounded-full bg-sky-500"></span>
            <span>Đã ghi danh trong hệ thống</span>
          </div>
        </div>

        <div className="p-4 bg-white rounded-2xl border border-slate-200/90 shadow-2xs space-y-1 hover:border-slate-300 transition-colors">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold text-slate-600">Quản Trị Viên (Admin)</span>
            <div className="w-7 h-7 rounded-lg bg-purple-50 flex items-center justify-center text-purple-600">
              <ShieldCheck className="w-4 h-4" />
            </div>
          </div>
          <p className="font-heading font-extrabold text-2xl text-purple-700">
            {adminCount}
          </p>
          <div className="flex items-center gap-1.5 text-[11px] text-slate-400 pt-0.5">
            <span className="w-1.5 h-1.5 rounded-full bg-purple-500"></span>
            <span>Quyền quản trị toàn hệ thống</span>
          </div>
        </div>

        <div className="p-4 bg-white rounded-2xl border border-slate-200/90 shadow-2xs space-y-1 hover:border-slate-300 transition-colors">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold text-slate-600">Tài Khoản Đang Hoạt Động</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-50 flex items-center justify-center text-emerald-600">
              <UserCheck className="w-4 h-4" />
            </div>
          </div>
          <p className="font-heading font-extrabold text-2xl text-emerald-600">
            {activeCount}
          </p>
          <div className="flex items-center gap-1.5 text-[11px] text-slate-400 pt-0.5">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span>Trạng thái ACTIVE</span>
          </div>
        </div>

        <div className="p-4 bg-white rounded-2xl border border-slate-200/90 shadow-2xs space-y-1 hover:border-slate-300 transition-colors">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold text-slate-600">Phạm Vi Khảo Thí</span>
            <div className="w-7 h-7 rounded-lg bg-sky-50 flex items-center justify-center text-sky-600">
              <GraduationCap className="w-4 h-4" />
            </div>
          </div>
          <p className="font-heading font-extrabold text-2xl text-slate-900">
            SWD392
          </p>
          <div className="flex items-center gap-1.5 text-[11px] text-slate-400 pt-0.5">
            <span className="w-1.5 h-1.5 rounded-full bg-sky-500"></span>
            <span className="font-mono">Row-Level Security Enforced</span>
          </div>
        </div>
      </div>

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
        onResetFilters={handleResetFilters}
      />

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
        message={`Bạn có chắc chắn muốn xóa mềm người dùng "${deletingUser?.fullName}" (${deletingUser?.email})? Trạng thái tài khoản sẽ chuyển thành DELETED và người dùng sẽ không thể đăng nhập vào hệ thống thi.`}
        confirmText="Xác Nhận Xóa"
        isDanger={true}
        isLoading={isDeleting}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeletingUser(null)}
      />

    </div>
  );
}
