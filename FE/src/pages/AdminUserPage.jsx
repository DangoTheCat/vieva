import React, { useState, useEffect } from 'react';
import { userApi } from '../api/userApi';
import { getErrorMessage } from '../utils/errorCodes';
import { UserTable } from '../components/users/UserTable';
import { UserFilterBar } from '../components/users/UserFilterBar';
import { CreateUserModal } from '../components/users/CreateUserModal';
import { EditUserModal } from '../components/users/EditUserModal';
import { UserDetailModal } from '../components/users/UserDetailModal';
import { ConfirmModal } from '../components/common/ConfirmModal';
import { 
  Users, 
  ShieldCheck, 
  UserCheck, 
  GraduationCap, 
  UserPlus, 
  RefreshCw, 
  Table2,
  WifiOff,
  RotateCcw
} from 'lucide-react';

export function AdminUserPage({ onOpenMatrix, showToast }) {
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [pageData, setPageData] = useState({
    totalElements: 0,
    totalPages: 0,
    number: 0
  });

  // Filter States
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('DESC');

  // Modal Action States
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [viewingUser, setViewingUser] = useState(null);
  const [deletingUser, setDeletingUser] = useState(null);
  const [resettingUser, setResettingUser] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isResetting, setIsResetting] = useState(false);

  // Resilience & Auto-reconnect
  const [fetchError, setFetchError] = useState(null);
  const [reconnectCountdown, setReconnectCountdown] = useState(60);

  // Fetch Users from Backend
  const fetchUsers = async () => {
    setIsLoading(true);
    setFetchError(null);
    try {
      const params = {
        page,
        size,
        keyword: keyword.trim() || undefined,
        role: role || undefined,
        status: status || undefined,
        sortBy,
        sortDirection
      };

      const data = await userApi.getUsers(params);
      
      if (Array.isArray(data)) {
        setUsers(data);
        setPageData({
          totalElements: data.length,
          totalPages: 1,
          number: 0
        });
      } else if (data && Array.isArray(data.content)) {
        setUsers(data.content);
        setPageData({
          totalElements: data.totalElements || 0,
          totalPages: data.totalPages || 0,
          number: data.number || 0
        });
      } else {
        setUsers([]);
      }
    } catch (err) {
      const errMsg = getErrorMessage(err);
      setFetchError(errMsg);
      setUsers([]);
      if (showToast) {
        showToast({
          type: 'error',
          title: 'Lỗi Truy Vấn Tài Khoản',
          message: errMsg
        });
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Debounced search & filter sync
  useEffect(() => {
    const timer = setTimeout(() => {
      fetchUsers();
    }, 250);
    return () => clearTimeout(timer);
  }, [keyword, role, status, sortBy, sortDirection, page]);

  // Reconnect Countdown Timer
  useEffect(() => {
    if (!fetchError) return;

    const interval = setInterval(() => {
      setReconnectCountdown(prev => {
        if (prev <= 1) {
          fetchUsers();
          return 60;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [fetchError]);

  const handleResetFilters = () => {
    setKeyword('');
    setRole('');
    setStatus('');
    setSortBy('createdAt');
    setSortDirection('DESC');
    setPage(0);
  };

  const handleCreateSuccess = () => {
    setIsCreateOpen(false);
    showToast({
      type: 'success',
      title: 'Tạo Tài Khoản Thành Công',
      message: 'Người dùng mới đã được thêm vào hệ thống.'
    });
    fetchUsers();
  };

  const handleUpdateSuccess = () => {
    setEditingUser(null);
    showToast({
      type: 'success',
      title: 'Cập Nhật Thành Công',
      message: 'Thông tin tài khoản đã được lưu.'
    });
    fetchUsers();
  };

  const handleConfirmResetPassword = async () => {
    if (!resettingUser) return;
    setIsResetting(true);
    try {
      await userApi.resetPassword(resettingUser.userId);
      showToast({
        type: 'success',
        title: 'Đặt Lại Mật Khẩu Thành Công',
        message: `Mật khẩu tạm thời đã được gửi tới email ${resettingUser.email}`
      });
      setResettingUser(null);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsResetting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingUser) return;
    setIsDeleting(true);
    try {
      await userApi.deleteUser(deletingUser.userId);
      showToast({
        type: 'success',
        title: 'Khóa Tài Khoản Thành Công',
        message: `Tài khoản ${deletingUser.email} đã được vô hiệu hóa.`
      });
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
      <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="font-heading text-2xl font-bold text-ink tracking-tight">
            Quản Lý Người Dùng &amp; Phân Quyền (RBAC)
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Quản lý danh sách tài khoản, phân công giảng viên và kiểm soát trạng thái truy cập hệ thống.
          </p>
        </div>

        {/* Header Actions Panel */}
        <div className="flex items-center gap-2.5 shrink-0">
          <button
            onClick={() => fetchUsers()}
            disabled={isLoading}
            className="h-9 w-9 flex items-center justify-center rounded-xl bg-white border border-pink-200 text-slate-700 shadow-2xs hover:bg-slate-50 transition cursor-pointer disabled:opacity-50"
            title="Làm mới danh sách dữ liệu"
            aria-label="Làm mới danh sách"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={() => setIsCreateOpen(true)}
            className="h-9 px-4 flex items-center gap-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white text-xs font-semibold shadow-xs transition active:scale-[0.98] cursor-pointer"
          >
            <UserPlus className="w-4 h-4" />
            <span>Thêm Người Dùng</span>
          </button>
        </div>
      </header>

      {/* OFFLINE / RECONNECT BANNER */}
      {fetchError && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-900 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-2xs animate-modal-entry mb-6">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-rose-100 text-rose-700 flex items-center justify-center shrink-0">
              <WifiOff className="w-4 h-4" />
            </div>
            <div>
              <p className="font-semibold text-rose-950">
                Không thể kết nối đến Máy chủ Backend API (Cổng 8080)
              </p>
              <p className="text-[11px] text-rose-700 mt-0.5">
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
              className="px-3 py-1.5 rounded-lg bg-white border border-rose-200 hover:bg-rose-50 text-rose-800 text-xs font-semibold transition flex items-center gap-1.5 shadow-2xs cursor-pointer"
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
        <div className="bg-white rounded-xl border border-pink-200/90 p-5 shadow-2xs flex flex-col justify-between">
          <div className="flex items-start justify-between">
            <span className="text-xs font-semibold text-slate-500">Tổng Số Người Dùng</span>
            <div className="w-9 h-9 rounded-lg bg-pink-50 border border-blue-100 flex items-center justify-center text-pink-600">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-heading text-3xl font-bold text-ink leading-tight">
              {pageData.totalElements || users.length}
            </div>
            <p className="text-xs text-slate-500 mt-1 flex items-center gap-1.5 font-medium">
              <span className="inline-block w-2 h-2 rounded-full bg-blue-500"></span>
              Đã ghi danh trong hệ thống
            </p>
          </div>
        </div>

        {/* Card 2: Admin Count */}
        <div className="bg-white rounded-xl border border-pink-200/90 p-5 shadow-2xs flex flex-col justify-between">
          <div className="flex items-start justify-between">
            <span className="text-xs font-semibold text-slate-500">Quản Trị Viên (Admin)</span>
            <div className="w-9 h-9 rounded-lg bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
              <ShieldCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-heading text-3xl font-bold text-ink leading-tight">
              {adminCount.toString().padStart(2, '0')}
            </div>
            <p className="text-xs text-slate-500 mt-1 flex items-center gap-1.5 font-medium">
              <span className="inline-block w-2 h-2 rounded-full bg-indigo-500"></span>
              Quyền quản trị toàn hệ thống
            </p>
          </div>
        </div>

        {/* Card 3: Active Accounts */}
        <div className="bg-white rounded-xl border border-pink-200/90 p-5 shadow-2xs flex flex-col justify-between">
          <div className="flex items-start justify-between">
            <span className="text-xs font-semibold text-slate-500">Đang Hoạt Động</span>
            <div className="w-9 h-9 rounded-lg bg-emerald-50 border border-emerald-100 flex items-center justify-center text-emerald-600">
              <UserCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-heading text-3xl font-bold text-ink leading-tight">
              {activeCount.toString().padStart(2, '0')}
            </div>
            <p className="text-xs text-slate-500 mt-1 flex items-center gap-1.5 font-medium">
              <span className="inline-block w-2 h-2 rounded-full bg-emerald-500"></span>
              Trạng thái ACTIVE
            </p>
          </div>
        </div>

        {/* Card 4: Exam Scope */}
        <div className="bg-white rounded-xl border border-pink-200/90 p-5 shadow-2xs flex flex-col justify-between">
          <div className="flex items-start justify-between">
            <span className="text-xs font-semibold text-slate-500">Phạm Vi Khảo Thí</span>
            <div className="w-9 h-9 rounded-lg bg-slate-100 border border-pink-200 flex items-center justify-center text-slate-700">
              <GraduationCap className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-heading text-3xl font-bold text-ink leading-tight">
              SWD392
            </div>
            <p className="text-xs text-slate-500 mt-1 flex items-center gap-1.5 font-medium truncate">
              <span className="inline-block w-2 h-2 rounded-full bg-slate-500"></span>
              Môn học phân công
            </p>
          </div>
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
        onEditUser={(user) => {
          setViewingUser(null);
          setEditingUser(user);
        }}
        showToast={showToast}
      />

      {/* CONFIRMATION DIALOG: SOFT-DELETE USER */}
      <ConfirmModal
        isOpen={Boolean(deletingUser)}
        title="Khóa Quyền Người Dùng"
        message={`Bạn có chắc chắn muốn khóa tài khoản "${deletingUser?.fullName}" (${deletingUser?.email})? Trạng thái tài khoản sẽ chuyển thành DELETED và người dùng sẽ không thể truy cập hệ thống khảo thí.`}
        confirmText="Xác Nhận Khóa"
        cancelText="Hủy Bỏ"
        isDanger={true}
        isLoading={isDeleting}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingUser(null)}
      />

      {/* CONFIRMATION DIALOG: RESET PASSWORD */}
      <ConfirmModal
        isOpen={Boolean(resettingUser)}
        title="Đặt Lại Mật Khẩu"
        message={`Bạn có chắc chắn muốn đặt lại mật khẩu cho tài khoản "${resettingUser?.fullName}" (${resettingUser?.email})? Mật khẩu tạm thời sẽ được tạo và gửi tới email của người dùng.`}
        confirmText="Đặt Lại Mật Khẩu"
        cancelText="Hủy Bỏ"
        isDanger={false}
        isLoading={isResetting}
        onConfirm={handleConfirmResetPassword}
        onCancel={() => setResettingUser(null)}
      />

    </div>
  );
}
