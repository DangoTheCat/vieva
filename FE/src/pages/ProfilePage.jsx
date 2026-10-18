import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/userApi';
import { getErrorMessage } from '../utils/errorCodes';
import {
  User,
  Key,
  ShieldCheck,
  Mail,
  Phone,
  Lock,
  Eye,
  EyeOff,
  Save,
  RotateCcw,
  AlertCircle,
  CheckCircle2,
  Shield,
  Clock,
  Sparkles,
  ArrowLeft,
  Check,
  Camera,
  UploadCloud,
  Trash2,
  Loader2,
  Image as ImageIcon
} from 'lucide-react';

export function ProfilePage({ showToast, onNavigate, initialTab = 'info' }) {
  const { currentUser, refreshProfile, isDemoMode, setCurrentUser, isAdmin, isLecturer } = useAuth();
  const [activeTab, setActiveTab] = useState(initialTab);

  // Avatar upload state
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const avatarInputRef = useRef(null);

  // Profile form state
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);
  const [profileSuccessMessage, setProfileSuccessMessage] = useState('');

  // Password form state
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');

  const fullNameInputRef = useRef(null);

  useEffect(() => {
    if (currentUser) {
      setFullName(currentUser.fullName || '');
      setPhoneNumber(currentUser.phoneNumber || '');
    }
  }, [currentUser]);

  useEffect(() => {
    if (initialTab) {
      setActiveTab(initialTab);
    }
  }, [initialTab]);

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

  const handleAvatarSelect = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Reset input so user can choose the same file again if desired
    e.target.value = '';

    // Validate size: max 5MB (5 * 1024 * 1024 bytes)
    const MAX_SIZE_BYTES = 5 * 1024 * 1024;
    if (file.size > MAX_SIZE_BYTES) {
      showToast({
        type: 'error',
        message: 'Kích thước ảnh đại diện vượt quá 5MB. Vui lòng chọn ảnh nhỏ hơn (quy định tối đa 5MB).'
      });
      return;
    }

    // Validate file type: must be image
    if (!file.type || !file.type.startsWith('image/')) {
      showToast({
        type: 'warning',
        message: 'Định dạng file không hợp lệ. Vui lòng chọn tệp tin hình ảnh (JPG, PNG, WEBP, GIF).'
      });
      return;
    }

    setIsUploadingAvatar(true);
    try {
      if (isDemoMode) {
        const reader = new FileReader();
        reader.onload = () => {
          const base64Url = reader.result;
          const updated = { ...currentUser, avatarUrl: base64Url };
          setCurrentUser(updated);
          localStorage.setItem('aives_user', JSON.stringify(updated));
          showToast({
            type: 'success',
            message: 'Tải lên ảnh đại diện thành công (Chế độ Demo)!'
          });
        };
        reader.readAsDataURL(file);
        return;
      }

      const updatedUser = await userApi.uploadAvatar(file);
      if (updatedUser) {
        const merged = {
          ...currentUser,
          ...updatedUser,
          selectedRole: currentUser?.selectedRole || 'ROLE_STUDENT'
        };
        setCurrentUser(merged);
        localStorage.setItem('aives_user', JSON.stringify(merged));
      }
      await refreshProfile();
      showToast({
        type: 'success',
        message: 'Ảnh đại diện đã được tải lên Cloudinary và lưu thành công!'
      });
    } catch (err) {
      console.error('Lỗi khi tải ảnh đại diện lên máy chủ:', err);
      showToast({
        type: 'error',
        message: err.message || 'Tải ảnh đại diện lên máy chủ thất bại. Vui lòng kiểm tra lại dịch vụ Backend.'
      });
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  const handleDeleteAvatar = async () => {
    if (!currentUser?.avatarUrl) return;

    setIsUploadingAvatar(true);
    try {
      if (isDemoMode) {
        const updated = { ...currentUser, avatarUrl: null };
        setCurrentUser(updated);
        localStorage.setItem('aives_user', JSON.stringify(updated));
        showToast({ type: 'info', message: 'Đã gỡ ảnh đại diện (Chế độ Demo).' });
        return;
      }

      const updatedUser = await userApi.deleteAvatar();
      if (updatedUser) {
        const merged = {
          ...currentUser,
          ...updatedUser,
          selectedRole: currentUser?.selectedRole || 'ROLE_STUDENT'
        };
        setCurrentUser(merged);
        localStorage.setItem('aives_user', JSON.stringify(merged));
      }
      await refreshProfile();
      showToast({ type: 'info', message: 'Đã xóa ảnh đại diện thành công trên máy chủ và Cloudinary.' });
    } catch (err) {
      console.error('Lỗi khi xóa ảnh đại diện:', err);
      showToast({
        type: 'error',
        message: err.message || 'Không thể xóa ảnh đại diện trên máy chủ.'
      });
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  const handleResetProfileForm = () => {
    if (currentUser) {
      setFullName(currentUser.fullName || '');
      setPhoneNumber(currentUser.phoneNumber || '');
      setProfileSuccessMessage('');
    }
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setProfileSuccessMessage('');

    if (!fullName.trim()) {
      showToast({ type: 'warning', message: 'Họ và tên không được để trống.' });
      return;
    }

    if (phoneNumber && !/^(\+?[0-9]{9,11})$/.test(phoneNumber.trim())) {
      showToast({ type: 'warning', message: 'Số điện thoại phải từ 9 đến 11 chữ số.' });
      return;
    }

    setIsUpdatingProfile(true);
    try {
      if (isDemoMode) {
        const updated = {
          ...currentUser,
          fullName: fullName.trim(),
          phoneNumber: phoneNumber ? phoneNumber.trim() : null,
          updatedAt: new Date().toISOString()
        };
        setCurrentUser(updated);
        localStorage.setItem('aives_user', JSON.stringify(updated));
        setProfileSuccessMessage('Cập nhật hồ sơ cá nhân thành công (Chế độ Demo)!');
        showToast({ type: 'success', message: 'Cập nhật hồ sơ cá nhân thành công!' });
        return;
      }

      await userApi.updateProfile({
        fullName: fullName.trim(),
        phoneNumber: phoneNumber ? phoneNumber.trim() : null
      });

      await refreshProfile();
      setProfileSuccessMessage('Cập nhật hồ sơ cá nhân thành công trên cơ sở dữ liệu!');
      showToast({ type: 'success', message: 'Cập nhật hồ sơ cá nhân thành công!' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPasswordError('');
    setPasswordSuccess('');

    if (!oldPassword) {
      setPasswordError('Vui lòng nhập mật khẩu hiện tại.');
      return;
    }
    if (newPassword.length < 6) {
      setPasswordError('Mật khẩu mới phải có tối thiểu 6 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('Mật khẩu xác nhận không trùng khớp.');
      return;
    }
    if (oldPassword === newPassword) {
      setPasswordError('Mật khẩu mới phải khác mật khẩu hiện tại (Quy chuẩn bảo mật AIVES).');
      return;
    }

    setIsChangingPassword(true);
    try {
      if (isDemoMode) {
        setPasswordSuccess('Đổi mật khẩu thành công (Chế độ Demo)!');
        showToast({ type: 'success', message: 'Đổi mật khẩu thành công (Chế độ Demo)!' });
        setOldPassword('');
        setNewPassword('');
        setConfirmPassword('');
        return;
      }

      await userApi.changePassword({ oldPassword, newPassword });
      setPasswordSuccess('Đổi mật khẩu thành công! Vui lòng ghi nhớ mật khẩu mới cho các lần đăng nhập tiếp theo.');
      showToast({ type: 'success', message: 'Đổi mật khẩu thành công! Mật khẩu mới đã được cập nhật.' });
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const msg = getErrorMessage(err);
      setPasswordError(msg);
      showToast({ type: 'error', message: msg });
    } finally {
      setIsChangingPassword(false);
    }
  };

  const roleNameLabel = isAdmin ? 'Quản Trị Viên (Admin)' : isLecturer ? 'Giảng Viên Khảo Thí (Lecturer)' : 'Thí Sinh / Sinh Viên (Student)';

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12 animate-in fade-in duration-200">
      
      {/* ========================================================================= */}
      {/* 1. TOP HEADER & IDENTITY CARD                                            */}
      {/* ========================================================================= */}
      <div className="bg-paper border-2 border-green-500 rounded-3xl p-6 sm:p-8 shadow-[6px_6px_0_#D5A6A2] relative overflow-hidden">
        {/* Subtle decorative background stamp */}
        <div className="absolute right-4 -top-8 text-pink-200/40 select-none pointer-events-none font-heading font-black text-9xl">
          aives.
        </div>

        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex items-center gap-4 sm:gap-5">
            {/* Avatar Pill with Layered Shadow & Interactive Upload */}
            <div className="relative shrink-0 group">
              {currentUser?.avatarUrl ? (
                <img
                  src={currentUser.avatarUrl}
                  alt={currentUser.fullName || 'Ảnh đại diện'}
                  className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl border-2 border-green-500 object-cover shadow-[4px_4px_0_#DEB9B5]"
                />
              ) : (
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-pink-200 border-2 border-green-500 flex items-center justify-center text-ink font-heading font-extrabold text-2xl shadow-[4px_4px_0_#DEB9B5]">
                  {getInitials(currentUser?.fullName)}
                </div>
              )}
              <span className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-green-500 border-2 border-paper flex items-center justify-center shadow-xs" title="Tài khoản hoạt động">
                <Check className="w-3 h-3 text-paper stroke-[3]" />
              </span>

              {/* Quick Camera Upload Overlay */}
              <button
                type="button"
                onClick={() => avatarInputRef.current?.click()}
                disabled={isUploadingAvatar}
                className="absolute -top-1.5 -right-1.5 w-7 h-7 rounded-xl bg-paper hover:bg-pink-100 border-2 border-green-500 text-pink-700 flex items-center justify-center shadow-[2px_2px_0_#DEB9B5] cursor-pointer transition active:scale-95 disabled:opacity-50"
                title="Tải lên ảnh đại diện mới (Cloudinary, dưới 5MB)"
              >
                {isUploadingAvatar ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-pink-700" />
                ) : (
                  <Camera className="w-3.5 h-3.5" />
                )}
              </button>

              <input
                ref={avatarInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif"
                onChange={handleAvatarSelect}
                className="hidden"
              />
            </div>

            {/* User Title & Metadata */}
            <div className="space-y-1">
              <div className="flex items-center gap-2 flex-wrap">
                <h1 className="font-heading font-extrabold text-xl sm:text-2xl text-ink leading-tight">
                  {currentUser?.fullName || 'Người Dùng AIVES'}
                </h1>
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-green-100 text-green-900 border border-green-500 shadow-[1px_1px_0_#C4CCB7]">
                  <span className="w-1.5 h-1.5 rounded-full bg-green-600 animate-pulse"></span>
                  {currentUser?.status || 'ACTIVE'}
                </span>
              </div>

              <p className="text-xs text-green-800 font-medium">
                {currentUser?.email || 'email@fpt.edu.vn'}
              </p>

              <div className="flex items-center gap-2 pt-1 flex-wrap">
                <span className="font-mono text-[11px] font-bold text-pink-900 bg-pink-100 px-2 py-0.5 rounded-lg border border-pink-300">
                  {currentUser?.userCode || 'USR-CODE'}
                </span>
                <span className="text-[11px] font-semibold text-green-700 bg-green-50 px-2.5 py-0.5 rounded-lg border border-green-300">
                  {roleNameLabel}
                </span>
              </div>
            </div>
          </div>

          {/* Quick Return Button */}
          {onNavigate && (
            <div className="shrink-0">
              <button
                type="button"
                onClick={() => onNavigate(isAdmin ? 'admin-users' : isLecturer ? 'lecturer-questions' : 'discover')}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-paper border-2 border-green-500 text-green-900 font-bold text-xs shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition active:translate-x-0.5 active:translate-y-0.5 cursor-pointer"
              >
                <ArrowLeft className="w-4 h-4 text-green-800" />
                <span>Quay Lại Bảng Điều Khiển</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 2. TAB SWITCHER (STAGEBOX BUTTON PILLS)                                  */}
      {/* ========================================================================= */}
      <div className="flex items-center gap-2 border-b-2 border-cream pb-2">
        <button
          type="button"
          onClick={() => setActiveTab('info')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl font-bold text-xs transition border-2 cursor-pointer ${
            activeTab === 'info'
              ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[3px_3px_0_#DEB9B5]'
              : 'bg-paper text-green-900 border-transparent hover:border-green-300 hover:bg-green-50'
          }`}
        >
          <User className="w-4 h-4 text-pink-700" />
          <span>1. Thông Tin Cá Nhân</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('password')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl font-bold text-xs transition border-2 cursor-pointer ${
            activeTab === 'password'
              ? 'bg-pink-100 text-pink-900 border-green-500 shadow-[3px_3px_0_#DEB9B5]'
              : 'bg-paper text-green-900 border-transparent hover:border-green-300 hover:bg-green-50'
          }`}
        >
          <Key className="w-4 h-4 text-pink-700" />
          <span>2. Bảo Mật &amp; Đổi Mật Khẩu</span>
        </button>
      </div>

      {/* ========================================================================= */}
      {/* 3. TAB CONTENT: THÔNG TIN CÁ NHÂN                                         */}
      {/* ========================================================================= */}
      {activeTab === 'info' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* Main Edit Form */}
          <div className="lg:col-span-2 bg-paper border-2 border-green-500 rounded-3xl p-6 sm:p-8 shadow-[6px_6px_0_#D5A6A2] space-y-6">
            <div className="border-b-2 border-cream pb-3 flex items-center justify-between">
              <div>
                <h2 className="font-heading font-black text-lg text-ink">
                  Cập Nhật Thông Tin Cá Nhân
                </h2>
                <p className="text-xs text-green-800">
                  Thông tin hiển thị trên bài thi vấn đáp và biên bản đánh giá Rubric
                </p>
              </div>
              <span className="text-[10px] font-mono text-green-700 bg-green-50 px-2 py-1 rounded border border-green-300">
                Endpoint: PATCH /api/v1/users/me
              </span>
            </div>

            {profileSuccessMessage && (
              <div className="p-3.5 bg-green-50 border-2 border-green-500 rounded-xl text-xs text-green-900 font-bold flex items-center gap-2 shadow-[2px_2px_0_#C4CCB7]">
                <CheckCircle2 className="w-4 h-4 text-green-700 shrink-0" />
                <span>{profileSuccessMessage}</span>
              </div>
            )}

            <form onSubmit={handleUpdateProfile} className="space-y-5">
              
              {/* Row 0: Ảnh Đại Diện (Cloudinary Avatar) */}
              <div className="p-4 rounded-2xl bg-pink-50/60 border-2 border-green-300 shadow-[2px_2px_0_#DEB9B5] space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex items-center gap-3.5">
                    {/* Avatar Preview */}
                    <div className="relative shrink-0">
                      {currentUser?.avatarUrl ? (
                        <img
                          src={currentUser.avatarUrl}
                          alt={currentUser.fullName || 'Avatar'}
                          className="w-14 h-14 rounded-2xl border-2 border-green-500 object-cover shadow-[2px_2px_0_#DEB9B5]"
                        />
                      ) : (
                        <div className="w-14 h-14 rounded-2xl bg-pink-200 border-2 border-green-500 flex items-center justify-center text-ink font-heading font-extrabold text-xl shadow-[2px_2px_0_#DEB9B5]">
                          {getInitials(currentUser?.fullName)}
                        </div>
                      )}
                    </div>

                    <div className="space-y-0.5">
                      <h4 className="text-xs font-bold text-ink font-heading flex items-center gap-1.5">
                        <ImageIcon className="w-3.5 h-3.5 text-pink-700" />
                        <span>Ảnh Đại Diện (Cloudinary Storage)</span>
                      </h4>
                      <p className="text-[11px] text-green-800">
                        {currentUser?.avatarUrl
                          ? 'Đã đồng bộ ảnh đại diện lên Cloudinary CDN'
                          : 'Đang hiển thị Avatar mặc định theo chữ cái đầu'}
                      </p>
                      <div className="flex items-center gap-2 pt-0.5">
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-green-100 text-green-900 border border-green-300 font-bold">
                          Dưới 5MB
                        </span>
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-pink-100 text-pink-900 border border-pink-300 font-bold">
                          JPG, PNG, WEBP, GIF
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Upload / Delete Action Buttons */}
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      type="button"
                      disabled={isUploadingAvatar}
                      onClick={() => avatarInputRef.current?.click()}
                      className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink text-xs font-bold border-2 border-green-600 shadow-[2px_2px_0_#3A422E] transition active:scale-95 disabled:opacity-50 cursor-pointer"
                    >
                      {isUploadingAvatar ? (
                        <>
                          <Loader2 className="w-3.5 h-3.5 animate-spin text-ink" />
                          <span>Đang Tải...</span>
                        </>
                      ) : (
                        <>
                          <UploadCloud className="w-3.5 h-3.5 text-ink" />
                          <span>{currentUser?.avatarUrl ? 'Thay Ảnh Mới' : 'Tải Lên Ảnh'}</span>
                        </>
                      )}
                    </button>

                    {currentUser?.avatarUrl && (
                      <button
                        type="button"
                        disabled={isUploadingAvatar}
                        onClick={handleDeleteAvatar}
                        className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-paper hover:bg-red-50 text-red-700 text-xs font-bold border-2 border-red-400 shadow-[2px_2px_0_#DEB9B5] transition active:scale-95 disabled:opacity-50 cursor-pointer"
                        title="Gỡ ảnh đại diện"
                      >
                        <Trash2 className="w-3.5 h-3.5 text-red-600" />
                        <span className="hidden sm:inline">Gỡ Ảnh</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>

              {/* Row 1: Read-only Identity Fields */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-green-800 mb-1 flex items-center gap-1.5">
                    <Lock className="w-3.5 h-3.5 text-green-600" />
                    <span>Mã Người Dùng (User Code):</span>
                  </label>
                  <input
                    type="text"
                    disabled
                    value={currentUser?.userCode || ''}
                    className="w-full px-3.5 py-2.5 rounded-xl text-xs font-mono font-bold bg-green-50/70 border-2 border-green-300 text-green-900 cursor-not-allowed shadow-[1px_1px_0_#C4CCB7]"
                  />
                  <span className="text-[10px] text-green-700 mt-0.5 block">Định danh mã sinh viên / cán bộ cố định</span>
                </div>

                <div>
                  <label className="block text-xs font-bold text-green-800 mb-1 flex items-center gap-1.5">
                    <ShieldCheck className="w-3.5 h-3.5 text-green-600" />
                    <span>Trạng Thái Tài Khoản:</span>
                  </label>
                  <div className="px-3.5 py-2.5 rounded-xl border-2 border-green-300 bg-green-50/70 text-xs flex items-center gap-2 shadow-[1px_1px_0_#C4CCB7]">
                    <span className="w-2.5 h-2.5 rounded-full bg-green-600 animate-pulse"></span>
                    <span className="font-bold text-green-900 font-mono">{currentUser?.status || 'ACTIVE'}</span>
                    <span className="text-[10px] text-green-700 ml-auto font-medium">• Đã xác thực</span>
                  </div>
                </div>
              </div>

              {/* Row 2: Read-only Academic Email */}
              <div>
                <label className="block text-xs font-bold text-green-800 mb-1 flex items-center gap-1.5">
                  <Mail className="w-3.5 h-3.5 text-green-600" />
                  <span>Email Học Thuật (FPT Edu):</span>
                </label>
                <input
                  type="email"
                  disabled
                  value={currentUser?.email || ''}
                  className="w-full px-3.5 py-2.5 rounded-xl text-xs font-mono font-bold bg-green-50/70 border-2 border-green-300 text-green-900 cursor-not-allowed shadow-[1px_1px_0_#C4CCB7]"
                />
                <p className="text-[10px] text-green-700 mt-1">
                  Email đăng ký do Admin hoặc Ban Khảo Thí cấp, dùng làm tài khoản đăng nhập và không thể tự thay đổi.
                </p>
              </div>

              {/* Row 3: Editable Full Name */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <User className="w-3.5 h-3.5 text-pink-700" />
                  <span>Họ Và Tên Khảo Thí (*):</span>
                </label>
                <input
                  ref={fullNameInputRef}
                  type="text"
                  required
                  maxLength={50}
                  placeholder="Ví dụ: Nguyễn Văn A"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl text-xs font-bold bg-surface-container-lowest border-2 border-green-500 text-ink placeholder:text-green-700/50 focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] transition"
                />
                <span className="text-[10px] text-green-700 mt-0.5 block">Họ và tên hiển thị trong bảng điểm thi vấn đáp</span>
              </div>

              {/* Row 4: Editable Phone Number */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 text-pink-700" />
                  <span>Số Điện Thoại Liên Lạc:</span>
                </label>
                <input
                  type="text"
                  maxLength={20}
                  placeholder="Ví dụ: 0912345678"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl text-xs font-mono font-bold bg-surface-container-lowest border-2 border-green-500 text-ink placeholder:text-green-700/50 focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] transition"
                />
                <span className="text-[10px] text-green-700 mt-0.5 block">Dùng để nhận thông báo lịch thi khẩn cấp hoặc xác thực</span>
              </div>

              {/* Form Buttons */}
              <div className="pt-4 border-t-2 border-cream flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={handleResetProfileForm}
                  className="px-4 py-2.5 rounded-xl border-2 border-green-500 bg-paper text-green-900 text-xs font-bold shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition active:translate-x-0.5 active:translate-y-0.5 cursor-pointer flex items-center gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Hoàn Tác</span>
                </button>

                <button
                  type="submit"
                  disabled={isUpdatingProfile}
                  className="px-6 py-2.5 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-600 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-0.5 active:translate-y-0.5 active:shadow-none disabled:opacity-50 cursor-pointer flex items-center gap-2"
                >
                  {isUpdatingProfile ? (
                    <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>
                  ) : (
                    <Save className="w-3.5 h-3.5 text-ink" />
                  )}
                  <span>Lưu Thay Đổi Hồ Sơ</span>
                </button>
              </div>

            </form>
          </div>

          {/* Right Side Info Cards */}
          <div className="space-y-6">
            
            {/* System Roles Badge Card */}
            <div className="bg-paper border-2 border-green-500 rounded-3xl p-6 shadow-[4px_4px_0_#DEB9B5] space-y-3">
              <div className="flex items-center gap-2 border-b-2 border-cream pb-2">
                <Shield className="w-4 h-4 text-pink-700" />
                <h3 className="font-heading font-bold text-sm text-ink">Vai Trò Hệ Thống</h3>
              </div>
              <p className="text-xs text-green-800">
                Các phân quyền được gán trực tiếp bởi Trạm Quản Trị Khảo Thí:
              </p>
              <div className="flex flex-wrap gap-2 pt-1">
                {currentUser?.roles?.map((role, idx) => (
                  <span
                    key={idx}
                    className="px-3 py-1 rounded-full text-xs font-mono font-bold bg-pink-100 text-pink-900 border-2 border-pink-400 shadow-[1px_1px_0_#DEB9B5]"
                  >
                    {role}
                  </span>
                ))}
              </div>
            </div>

            {/* Academic Policy Summary Card */}
            <div className="bg-green-50 border-2 border-green-500 rounded-3xl p-6 shadow-[4px_4px_0_#C4CCB7] space-y-3">
              <div className="flex items-center gap-2 border-b border-green-300 pb-2">
                <Sparkles className="w-4 h-4 text-green-800" />
                <h3 className="font-heading font-bold text-sm text-green-950">Quy Chuẩn Định Danh</h3>
              </div>
              <ul className="space-y-2 text-xs text-green-900 leading-relaxed">
                <li className="flex items-start gap-2">
                  <span className="text-pink-700 font-bold">•</span>
                  <span>Mã người dùng được đối soát trực tiếp với danh sách thí sinh chính thức của FPT University.</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-pink-700 font-bold">•</span>
                  <span>Mọi thay đổi thông tin hồ sơ được ghi vết an toàn trong Nhật ký Khảo thí (Audit Logs).</span>
                </li>
              </ul>
            </div>

          </div>

        </div>
      )}

      {/* ========================================================================= */}
      {/* 4. TAB CONTENT: BẢO MẬT & ĐỔI MẬT KHẨU                                   */}
      {/* ========================================================================= */}
      {activeTab === 'password' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* Main Password Change Form */}
          <div className="lg:col-span-2 bg-paper border-2 border-green-500 rounded-3xl p-6 sm:p-8 shadow-[6px_6px_0_#D5A6A2] space-y-6">
            <div className="border-b-2 border-cream pb-3 flex items-center justify-between">
              <div>
                <h2 className="font-heading font-black text-lg text-ink">
                  Đổi Mật Khẩu Đăng Nhập
                </h2>
                <p className="text-xs text-green-800">
                  Cập nhật mật khẩu định kỳ để bảo vệ tài khoản khảo thí và học liệu của bạn
                </p>
              </div>
              <span className="text-[10px] font-mono text-green-700 bg-green-50 px-2 py-1 rounded border border-green-300">
                Endpoint: PUT /api/v1/users/me/password
              </span>
            </div>

            {passwordError && (
              <div className="p-3.5 bg-pink-50 border-2 border-pink-400 rounded-xl text-xs text-pink-900 font-bold flex items-center gap-2 shadow-[2px_2px_0_#DEB9B5]">
                <AlertCircle className="w-4 h-4 text-pink-700 shrink-0" />
                <span>{passwordError}</span>
              </div>
            )}

            {passwordSuccess && (
              <div className="p-3.5 bg-green-50 border-2 border-green-500 rounded-xl text-xs text-green-900 font-bold flex items-center gap-2 shadow-[2px_2px_0_#C4CCB7]">
                <CheckCircle2 className="w-4 h-4 text-green-700 shrink-0" />
                <span>{passwordSuccess}</span>
              </div>
            )}

            <form onSubmit={handleChangePassword} className="space-y-5">
              
              {/* Field 1: Old Password */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <Key className="w-3.5 h-3.5 text-green-700" />
                  <span>Mật Khẩu Hiện Tại (*):</span>
                </label>
                <div className="relative">
                  <input
                    type={showOldPassword ? 'text' : 'password'}
                    required
                    placeholder="Nhập mật khẩu đang dùng để xác minh..."
                    value={oldPassword}
                    onChange={(e) => setOldPassword(e.target.value)}
                    className="w-full px-3.5 py-2.5 pr-10 rounded-xl text-xs font-mono font-medium bg-surface-container-lowest border-2 border-green-500 text-ink placeholder:text-green-700/50 focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowOldPassword(!showOldPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-green-700 hover:text-pink-700 transition"
                    aria-label={showOldPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showOldPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Field 2: New Password */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <Lock className="w-3.5 h-3.5 text-pink-700" />
                  <span>Mật Khẩu Mới (*):</span>
                </label>
                <div className="relative">
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    required
                    minLength={6}
                    placeholder="Tối thiểu 6 ký tự bảo mật..."
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full px-3.5 py-2.5 pr-10 rounded-xl text-xs font-mono font-medium bg-surface-container-lowest border-2 border-green-500 text-ink placeholder:text-green-700/50 focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-green-700 hover:text-pink-700 transition"
                    aria-label={showNewPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                <div className="flex items-center gap-2 mt-1">
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded border ${
                    newPassword.length >= 6 ? 'bg-green-100 text-green-900 border-green-400 font-bold' : 'bg-paper text-green-700 border-green-200'
                  }`}>
                    {newPassword.length >= 6 ? '✓ Đủ độ dài tối thiểu' : '• Cần ít nhất 6 ký tự'}
                  </span>
                </div>
              </div>

              {/* Field 3: Confirm New Password */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <Lock className="w-3.5 h-3.5 text-pink-700" />
                  <span>Xác Nhận Mật Khẩu Mới (*):</span>
                </label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    required
                    minLength={6}
                    placeholder="Nhập lại chính xác mật khẩu mới..."
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="w-full px-3.5 py-2.5 pr-10 rounded-xl text-xs font-mono font-medium bg-surface-container-lowest border-2 border-green-500 text-ink placeholder:text-green-700/50 focus:outline-none focus:border-green-600 shadow-[2px_2px_0_#DEB9B5] transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-green-700 hover:text-pink-700 transition"
                    aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {confirmPassword && (
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded border mt-1 inline-block ${
                    confirmPassword === newPassword ? 'bg-green-100 text-green-900 border-green-400 font-bold' : 'bg-pink-100 text-pink-900 border-pink-400'
                  }`}>
                    {confirmPassword === newPassword ? '✓ Mật khẩu xác nhận trùng khớp' : '⚠ Mật khẩu xác nhận chưa khớp'}
                  </span>
                )}
              </div>

              {/* Form Buttons */}
              <div className="pt-4 border-t-2 border-cream flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => {
                    setOldPassword('');
                    setNewPassword('');
                    setConfirmPassword('');
                    setPasswordError('');
                    setPasswordSuccess('');
                  }}
                  className="px-4 py-2.5 rounded-xl border-2 border-green-500 bg-paper text-green-900 text-xs font-bold shadow-[2px_2px_0_#DEB9B5] hover:bg-green-100 transition active:translate-x-0.5 active:translate-y-0.5 cursor-pointer flex items-center gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Xóa Nhập</span>
                </button>

                <button
                  type="submit"
                  disabled={isChangingPassword}
                  className="px-6 py-2.5 rounded-xl bg-pink-500 hover:bg-pink-600 text-ink font-bold text-xs border-2 border-green-600 shadow-[3px_3px_0_#DEB9B5] transition active:translate-x-0.5 active:translate-y-0.5 active:shadow-none disabled:opacity-50 cursor-pointer flex items-center gap-2"
                >
                  {isChangingPassword ? (
                    <span className="w-3.5 h-3.5 border-2 border-ink border-t-transparent rounded-full animate-spin"></span>
                  ) : (
                    <Key className="w-3.5 h-3.5 text-ink" />
                  )}
                  <span>Cập Nhật Mật Khẩu Mới</span>
                </button>
              </div>

            </form>
          </div>

          {/* Right Side Security Card */}
          <div className="space-y-6">
            <div className="bg-pink-50 border-2 border-pink-400 rounded-3xl p-6 shadow-[4px_4px_0_#DEB9B5] space-y-3">
              <div className="flex items-center gap-2 border-b border-pink-300 pb-2">
                <ShieldCheck className="w-4 h-4 text-pink-800" />
                <h3 className="font-heading font-bold text-sm text-pink-950">Quy Định Mật Khẩu</h3>
              </div>
              <ul className="space-y-2.5 text-xs text-pink-950 leading-relaxed">
                <li className="flex items-start gap-2">
                  <span className="text-pink-700 font-bold">•</span>
                  <span>Mật khẩu mới <strong>tối thiểu 6 ký tự</strong>, khuyến khích chứa chữ hoa, số và ký tự đặc biệt.</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-pink-700 font-bold">•</span>
                  <span><strong>Không được trùng</strong> với mật khẩu hiện tại (Ràng buộc Error 1010 trên hệ thống).</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-pink-700 font-bold">•</span>
                  <span>Nếu quên mật khẩu hiện tại, bạn vui lòng liên hệ trực tiếp <strong>Trạm Khảo Thí FPTU</strong> để được cấp lại mật khẩu tạm thời.</span>
                </li>
              </ul>
            </div>
          </div>

        </div>
      )}

    </div>
  );
}
