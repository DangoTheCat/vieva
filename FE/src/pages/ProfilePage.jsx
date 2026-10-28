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
  const { currentUser, refreshProfile, setCurrentUser, isAdmin, isLecturer } = useAuth();
  const [activeTab, setActiveTab] = useState(isAdmin ? 'info' : initialTab);

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
    if (isAdmin) {
      setActiveTab('info');
    } else if (initialTab) {
      setActiveTab(initialTab);
    }
  }, [initialTab, isAdmin]);

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
      <div className="bg-white border border-pink-200 rounded-2xl p-6 sm:p-8 shadow-2xs relative overflow-hidden">
        

        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex items-center gap-4 sm:gap-5">
            {/* Avatar Pill with Layered Shadow & Interactive Upload */}
            <div className="relative shrink-0 group">
              {currentUser?.avatarUrl ? (
                <img
                  src={currentUser.avatarUrl}
                  alt={currentUser.fullName || 'Ảnh đại diện'}
                  className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl border border-pink-200 object-cover shadow-2xs"
                />
              ) : (
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-slate-200 border border-pink-200 flex items-center justify-center text-slate-700 font-heading font-bold text-2xl shadow-2xs">
                  {getInitials(currentUser?.fullName)}
                </div>
              )}
              <span className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-emerald-500 border-2 border-white flex items-center justify-center shadow-xs" title="Tài khoản hoạt động">
                <Check className="w-3 h-3 text-paper stroke-[3]" />
              </span>

              {/* Quick Camera Upload Overlay */}
              <button
                type="button"
                onClick={() => avatarInputRef.current?.click()}
                disabled={isUploadingAvatar}
                className="absolute -top-1.5 -right-1.5 w-7 h-7 rounded-xl bg-white hover:bg-slate-50 border border-pink-200 text-slate-700 flex items-center justify-center shadow-sm cursor-pointer transition active:scale-95 disabled:opacity-50"
                title="Tải lên ảnh đại diện mới (Cloudinary, dưới 5MB)"
              >
                {isUploadingAvatar ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-slate-600" />
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
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                  {currentUser?.status || 'ACTIVE'}
                </span>
              </div>

              <p className="text-xs text-slate-500 font-medium">
                {currentUser?.email || 'email@fpt.edu.vn'}
              </p>

              <div className="flex items-center gap-2 pt-1 flex-wrap">
                <span className="font-mono text-[11px] font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded-lg border border-pink-200">
                  {currentUser?.userCode || 'USR-CODE'}
                </span>
                <span className="text-[11px] font-medium text-slate-600 bg-slate-100 px-2.5 py-0.5 rounded-lg border border-pink-200">
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
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-white border border-pink-200 text-slate-700 font-semibold text-xs shadow-2xs hover:bg-slate-50 transition active:scale-[0.98] cursor-pointer"
              >
                <ArrowLeft className="w-4 h-4 text-slate-600" />
                <span>Quay Lại Bảng Điều Khiển</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 2. TAB SWITCHER (STAGEBOX BUTTON PILLS)                                  */}
      {/* ========================================================================= */}
      {!isAdmin ? (
        <div className="flex items-center gap-2 border-b border-pink-200 pb-2">
          <button
            type="button"
            onClick={() => setActiveTab('info')}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-xl font-bold text-xs transition border cursor-pointer ${
              activeTab === 'info'
                ? 'bg-pink-600 text-white border-pink-600 shadow-xs'
                : 'bg-white text-slate-700 border-pink-200 hover:bg-pink-50/70'
            }`}
          >
            <User className={`w-4 h-4 ${activeTab === 'info' ? 'text-white' : 'text-slate-600'}`} />
            <span>1. Thông Tin Cá Nhân</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('password')}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-xl font-bold text-xs transition border cursor-pointer ${
              activeTab === 'password'
                ? 'bg-pink-600 text-white border-pink-600 shadow-xs'
                : 'bg-white text-slate-700 border-pink-200 hover:bg-pink-50/70'
            }`}
          >
            <Key className={`w-4 h-4 ${activeTab === 'password' ? 'text-white' : 'text-slate-600'}`} />
            <span>2. Đổi Mật Khẩu</span>
          </button>
        </div>
      ) : (
        <div className="flex items-center justify-between border-b border-pink-200 pb-2">
          <div className="flex items-center gap-2 px-4 py-2 rounded-xl font-bold text-xs bg-pink-100 text-pink-900 border border-pink-300 shadow-2xs">
            <User className="w-4 h-4 text-pink-700" />
            <span>Thông Tin Tài Khoản Quản Trị Viên</span>
          </div>
          <span className="text-[11px] font-semibold text-slate-500 hidden sm:inline">
            Tài khoản Quản trị cấp cao được quản lý theo chính sách an toàn thông tin
          </span>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 3. TAB CONTENT: THÔNG TIN CÁ NHÂN                                         */}
      {/* ========================================================================= */}
      {activeTab === 'info' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* Main Edit Form */}
          <div className="lg:col-span-2 bg-white border border-pink-200 rounded-2xl p-6 sm:p-8 shadow-2xs space-y-6">
            <div className="border-b border-pink-200 pb-3 flex items-center justify-between">
              <div>
                <h2 className="font-heading font-black text-lg text-ink">
                  Cập Nhật Thông Tin Cá Nhân
                </h2>
                <p className="text-xs text-slate-600">
                  Thông tin hiển thị trên bài thi vấn đáp và biên bản đánh giá Rubric
                </p>
              </div>
              
            </div>

            {profileSuccessMessage && (
              <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 font-medium flex items-center gap-2 shadow-sm">
                <CheckCircle2 className="w-4 h-4 text-slate-500 shrink-0" />
                <span>{profileSuccessMessage}</span>
              </div>
            )}

            <form onSubmit={handleUpdateProfile} className="space-y-5">
              
              {/* Row 0: Ảnh Đại Diện (Cloudinary Avatar) */}
              <div className="p-4 rounded-xl bg-slate-50 border border-pink-200 shadow-2xs space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex items-center gap-3.5">
                    {/* Avatar Preview */}
                    <div className="relative shrink-0">
                      {currentUser?.avatarUrl ? (
                        <img
                          src={currentUser.avatarUrl}
                          alt={currentUser.fullName || 'Avatar'}
                          className="w-14 h-14 rounded-xl border border-pink-200 object-cover shadow-2xs"
                        />
                      ) : (
                        <div className="w-14 h-14 rounded-xl bg-slate-200 border border-pink-200 flex items-center justify-center text-slate-700 font-heading font-bold text-xl shadow-2xs">
                          {getInitials(currentUser?.fullName)}
                        </div>
                      )}
                    </div>

                    <div className="space-y-0.5">
                      <h4 className="text-xs font-bold text-ink font-heading flex items-center gap-1.5">
                        <ImageIcon className="w-3.5 h-3.5 text-slate-600" />
                        <span>Ảnh Đại Diện (Cloudinary Storage)</span>
                      </h4>
                      <p className="text-[11px] text-slate-600">
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
                      className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white text-xs font-semibold shadow-xs transition active:scale-[0.98] disabled:opacity-50 cursor-pointer"
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
                        className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white hover:bg-rose-50 text-rose-600 text-xs font-semibold border border-rose-200 shadow-2xs transition active:scale-[0.98] disabled:opacity-50 cursor-pointer"
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
                  <label className="block text-xs font-bold text-slate-600 mb-1 flex items-center gap-1.5">
                    <Lock className="w-3.5 h-3.5 text-green-600" />
                    <span>Mã Người Dùng (User Code):</span>
                  </label>
                  <input
                    type="text"
                    disabled
                    value={currentUser?.userCode || ''}
                    className="w-full px-3.5 py-2.5 rounded-xl text-xs font-mono font-bold bg-slate-100 border border-pink-200 text-slate-700 cursor-not-allowed shadow-sm"
                  />
                  <span className="text-[10px] text-slate-500 mt-0.5 block">Định danh mã sinh viên / cán bộ cố định</span>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1 flex items-center gap-1.5">
                    <ShieldCheck className="w-3.5 h-3.5 text-green-600" />
                    <span>Trạng Thái Tài Khoản:</span>
                  </label>
                  <div className="px-3.5 py-2.5 rounded-xl border border-pink-200 bg-slate-100 text-xs flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                    <span className="font-bold text-green-900 font-mono">{currentUser?.status || 'ACTIVE'}</span>
                    <span className="text-[10px] text-slate-500 ml-auto font-medium">• Đã xác thực</span>
                  </div>
                </div>
              </div>

              {/* Row 2: Read-only Academic Email */}
              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1 flex items-center gap-1.5">
                  <Mail className="w-3.5 h-3.5 text-green-600" />
                  <span>Email Học Thuật (FPT Edu):</span>
                </label>
                <input
                  type="email"
                  disabled
                  value={currentUser?.email || ''}
                  className="w-full px-3.5 py-2.5 rounded-xl text-xs font-mono font-bold bg-slate-100 border border-pink-200 text-slate-700 cursor-not-allowed shadow-sm"
                />
                <p className="text-[10px] text-slate-500 mt-1">
                  Email đăng ký do Admin hoặc Ban Khảo Thí cấp, dùng làm tài khoản đăng nhập và không thể tự thay đổi.
                </p>
              </div>

              {/* Row 3: Editable Full Name */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <User className="w-3.5 h-3.5 text-slate-600" />
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
                  className="w-full px-3.5 py-2.5 rounded-xl text-xs font-bold bg-white border border-pink-200 text-ink placeholder:text-slate-400 focus:outline-none focus:border-pink-500 focus:ring-1 focus:ring-pink-500 shadow-2xs transition"
                />
                <span className="text-[10px] text-slate-500 mt-0.5 block">Họ và tên hiển thị trong bảng điểm thi vấn đáp</span>
              </div>

              {/* Row 4: Editable Phone Number */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 text-slate-600" />
                  <span>Số Điện Thoại Liên Lạc:</span>
                </label>
                <input
                  type="text"
                  maxLength={20}
                  placeholder="Ví dụ: 0912345678"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl text-xs font-mono font-bold bg-white border border-pink-200 text-ink placeholder:text-slate-400 focus:outline-none focus:border-pink-500 focus:ring-1 focus:ring-pink-500 shadow-2xs transition"
                />
                <span className="text-[10px] text-slate-500 mt-0.5 block">Dùng để nhận thông báo lịch thi khẩn cấp hoặc xác thực</span>
              </div>

              {/* Form Buttons */}
              <div className="pt-4 border-t border-pink-100 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={handleResetProfileForm}
                  className="px-4 py-2.5 rounded-xl border border-pink-200 bg-white text-slate-700 text-xs font-semibold shadow-2xs hover:bg-slate-50 transition active:scale-[0.98] cursor-pointer flex items-center gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Hoàn Tác</span>
                </button>

                <button
                  type="submit"
                  disabled={isUpdatingProfile}
                  className="px-6 py-2.5 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 cursor-pointer flex items-center gap-2"
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
            <div className="bg-white border border-pink-200 rounded-2xl p-6 shadow-2xs space-y-3">
              <div className="flex items-center gap-2 border-b border-pink-200 pb-2">
                <Shield className="w-4 h-4 text-slate-600" />
                <h3 className="font-heading font-bold text-sm text-ink">Vai Trò Hệ Thống</h3>
              </div>
              <p className="text-xs text-slate-600">
                Các phân quyền được gán trực tiếp bởi Trạm Quản Trị Khảo Thí:
              </p>
              <div className="flex flex-wrap gap-2 pt-1">
                {currentUser?.roles?.map((role, idx) => (
                  <span
                    key={idx}
                    className="px-3 py-1 rounded-full text-xs font-mono font-bold bg-pink-100 text-pink-900 border-2 border-pink-400 shadow-sm"
                  >
                    {role}
                  </span>
                ))}
              </div>
            </div>

            
            {/* Admin Security Policy Notice */}
            {isAdmin && (
              <div className="bg-pink-50/70 border border-pink-200 rounded-2xl p-6 shadow-2xs space-y-3">
                <div className="flex items-center gap-2 border-b border-pink-200 pb-2">
                  <ShieldCheck className="w-4 h-4 text-pink-700" />
                  <h3 className="font-heading font-bold text-sm text-pink-900">Chính Sách Bảo Mật Quản Trị Viên</h3>
                </div>
                <div className="space-y-2 text-xs text-pink-950 leading-relaxed">
                  <p>
                    Tài khoản <strong>Quản Trị Viên (Admin)</strong> được quản trị và bảo vệ trực tiếp theo giao thức máy chủ cấp cao của FPT University.
                  </p>
                  <p className="text-[11px] text-slate-600">
                    Theo quy chuẩn an toàn thông tin và logic vận hành hệ thống khảo thí, tài khoản Quản trị viên không thực hiện đổi mật khẩu tự phục vụ qua giao diện người dùng mà được cấu hình qua Infrastructure &amp; Biến môi trường hệ thống.
                  </p>
                </div>
              </div>
            )}

            {/* Academic Policy Summary Card */}
            <div className="bg-slate-50 border border-pink-200 rounded-2xl p-6 shadow-2xs space-y-3">
              <div className="flex items-center gap-2 border-b border-pink-200 pb-2">
                <Sparkles className="w-4 h-4 text-slate-600" />
                <h3 className="font-heading font-bold text-sm text-pink-950">Quy Chuẩn Định Danh</h3>
              </div>
              <ul className="space-y-2 text-xs text-slate-600 leading-relaxed">
                <li className="flex items-start gap-2">
                  <span className="text-slate-600 font-bold">•</span>
                  <span>Mã người dùng được đối soát trực tiếp với danh sách thí sinh chính thức của FPT University.</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-slate-600 font-bold">•</span>
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
      {!isAdmin && activeTab === 'password' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* Main Password Change Form */}
          <div className="lg:col-span-2 bg-white border border-pink-200 rounded-2xl p-6 sm:p-8 shadow-2xs space-y-6">
            <div className="border-b border-pink-200 pb-3 flex items-center justify-between">
              <div>
                <h2 className="font-heading font-black text-lg text-ink">
                  Đổi Mật Khẩu Đăng Nhập
                </h2>
                <p className="text-xs text-slate-600">
                  Cập nhật mật khẩu định kỳ để bảo vệ tài khoản khảo thí và học liệu của bạn
                </p>
              </div>
              
            </div>

            {passwordError && (
              <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 font-medium flex items-center gap-2 shadow-sm">
                <AlertCircle className="w-4 h-4 text-slate-600 shrink-0" />
                <span>{passwordError}</span>
              </div>
            )}

            {passwordSuccess && (
              <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 font-medium flex items-center gap-2 shadow-sm">
                <CheckCircle2 className="w-4 h-4 text-slate-500 shrink-0" />
                <span>{passwordSuccess}</span>
              </div>
            )}

            <form onSubmit={handleChangePassword} className="space-y-5">
              
              {/* Field 1: Old Password */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <Key className="w-3.5 h-3.5 text-slate-500" />
                  <span>Mật Khẩu Hiện Tại (*):</span>
                </label>
                <div className="relative">
                  <input
                    type={showOldPassword ? 'text' : 'password'}
                    required
                    placeholder="Nhập mật khẩu đang dùng để xác minh..."
                    value={oldPassword}
                    onChange={(e) => setOldPassword(e.target.value)}
                    className="w-full px-3.5 py-2.5 pr-10 rounded-xl text-xs font-mono font-medium bg-white border border-pink-200 text-ink placeholder:text-slate-400 focus:outline-none focus:border-pink-500 focus:ring-1 focus:ring-pink-500 shadow-2xs transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowOldPassword(!showOldPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-600 transition"
                    aria-label={showOldPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showOldPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Field 2: New Password */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <Lock className="w-3.5 h-3.5 text-slate-600" />
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
                    className="w-full px-3.5 py-2.5 pr-10 rounded-xl text-xs font-mono font-medium bg-white border border-pink-200 text-ink placeholder:text-slate-400 focus:outline-none focus:border-pink-500 focus:ring-1 focus:ring-pink-500 shadow-2xs transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-600 transition"
                    aria-label={showNewPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                <div className="flex items-center gap-2 mt-1">
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded border ${
                    newPassword.length >= 6 ? 'bg-green-100 text-green-900 border-green-400 font-bold' : 'bg-paper text-slate-500 border-green-200'
                  }`}>
                    {newPassword.length >= 6 ? '✓ Đủ độ dài tối thiểu' : '• Cần ít nhất 6 ký tự'}
                  </span>
                </div>
              </div>

              {/* Field 3: Confirm New Password */}
              <div>
                <label className="block text-xs font-bold text-ink mb-1 flex items-center gap-1.5">
                  <Lock className="w-3.5 h-3.5 text-slate-600" />
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
                    className="w-full px-3.5 py-2.5 pr-10 rounded-xl text-xs font-mono font-medium bg-white border border-pink-200 text-ink placeholder:text-slate-400 focus:outline-none focus:border-pink-500 focus:ring-1 focus:ring-pink-500 shadow-2xs transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-600 transition"
                    aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {confirmPassword && (
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded border mt-1 inline-block ${
                    confirmPassword === newPassword ? 'bg-emerald-50 text-emerald-800 border-emerald-200 font-medium' : 'bg-rose-50 text-rose-700 border-rose-200'
                  }`}>
                    {confirmPassword === newPassword ? '✓ Mật khẩu xác nhận trùng khớp' : '⚠ Mật khẩu xác nhận chưa khớp'}
                  </span>
                )}
              </div>

              {/* Form Buttons */}
              <div className="pt-4 border-t border-pink-100 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => {
                    setOldPassword('');
                    setNewPassword('');
                    setConfirmPassword('');
                    setPasswordError('');
                    setPasswordSuccess('');
                  }}
                  className="px-4 py-2.5 rounded-xl border border-pink-200 bg-white text-slate-700 text-xs font-semibold shadow-2xs hover:bg-slate-50 transition active:scale-[0.98] cursor-pointer flex items-center gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Xóa Nhập</span>
                </button>

                <button
                  type="submit"
                  disabled={isChangingPassword}
                  className="px-6 py-2.5 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 cursor-pointer flex items-center gap-2"
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
            <div className="bg-slate-50 border border-pink-200 rounded-2xl p-6 shadow-2xs space-y-3">
              <div className="flex items-center gap-2 border-b border-pink-300 pb-2">
                <ShieldCheck className="w-4 h-4 text-pink-800" />
                <h3 className="font-heading font-bold text-sm text-pink-950">Quy Định Mật Khẩu</h3>
              </div>
              <ul className="space-y-2.5 text-xs text-pink-950 leading-relaxed">
                <li className="flex items-start gap-2">
                  <span className="text-slate-600 font-bold">•</span>
                  <span>Mật khẩu mới <strong>tối thiểu 6 ký tự</strong>, khuyến khích chứa chữ hoa, số và ký tự đặc biệt.</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-slate-600 font-bold">•</span>
                  <span><strong>Không được trùng</strong> với mật khẩu hiện tại nhằm đảm bảo an toàn tài khoản.</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-slate-600 font-bold">•</span>
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
