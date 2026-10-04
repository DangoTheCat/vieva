import React, { useState, useEffect, useMemo } from 'react';
import { 
  BookOpen, 
  Plus, 
  Search, 
  Filter, 
  Users, 
  Edit3, 
  PowerOff, 
  CheckCircle2, 
  XCircle, 
  Sparkles, 
  ShieldAlert, 
  UserCheck, 
  Trash2, 
  X,
  ExternalLink,
  ChevronRight,
  GraduationCap
} from 'lucide-react';
import { adminSubjectApi } from '../api/adminSubjectApi';
import { adminUserApi } from '../api/adminUserApi';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../utils/errorCodes';
import { ConfirmModal } from '../components/common/ConfirmModal';
import { SkeletonTable } from '../components/common/Skeleton';

export function AdminSubjectPage({ showToast }) {
  // State
  const [subjects, setSubjects] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [lecturers, setLecturers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modals state
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingSubject, setEditingSubject] = useState(null); // null = create new
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Assignment Modal
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [activeSubjectForAssign, setActiveSubjectForAssign] = useState(null);
  const [selectedLecturerIdToAdd, setSelectedLecturerIdToAdd] = useState('');
  const [isAssigning, setIsAssigning] = useState(false);

  // Deactivate confirmation
  const [deactivateTarget, setDeactivateTarget] = useState(null);

  // Form fields for subject
  const [formSubjectCode, setFormSubjectCode] = useState('');
  const [formSubjectName, setFormSubjectName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formCredits, setFormCredits] = useState(3);
  const [formStatus, setFormStatus] = useState('ACTIVE');

  // Fetch subjects and assignments from live backend
  const loadData = async () => {
    setIsLoading(true);
    try {
      const [subRes, lecUsersRes] = await Promise.allSettled([
        adminSubjectApi.getSubjects({ page: 0, size: 50 }),
        adminUserApi.getUsers({ page: 0, size: 100, role: 'ROLE_LECTURER' })
      ]);

      if (subRes.status === 'fulfilled') {
        const content = subRes.value.content || subRes.value || [];
        setSubjects(Array.isArray(content) ? content : []);
      } else {
        setSubjects([]);
      }

      if (lecUsersRes.status === 'fulfilled') {
        const userContent = lecUsersRes.value.content || lecUsersRes.value || [];
        setLecturers(Array.isArray(userContent) ? userContent : []);
      } else {
        setLecturers([]);
      }
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
      setSubjects([]);
      setAssignments([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Load assignments when opening assign modal
  const openAssignModal = async (subject) => {
    setActiveSubjectForAssign(subject);
    setIsAssignModalOpen(true);
    setSelectedLecturerIdToAdd('');

    try {
      const data = await adminSubjectApi.getLecturersBySubject(subject.subjectId);
      setAssignments(prev => [
        ...prev.filter(a => a.subjectId !== subject.subjectId),
        ...(Array.isArray(data) ? data : [])
      ]);
    } catch (err) {
      // Quietly keep current assignments or display error
    }
  };

  // Open Create/Edit modal
  const handleOpenCreateModal = () => {
    setEditingSubject(null);
    setFormSubjectCode('');
    setFormSubjectName('');
    setFormDescription('');
    setFormCredits(3);
    setFormStatus('ACTIVE');
    setIsEditModalOpen(true);
  };

  const handleOpenEditModal = (subject) => {
    setEditingSubject(subject);
    setFormSubjectCode(subject.subjectCode);
    setFormSubjectName(subject.subjectName);
    setFormDescription(subject.description || '');
    setFormCredits(subject.credits || 3);
    setFormStatus(subject.status || 'ACTIVE');
    setIsEditModalOpen(true);
  };

  // Submit create or edit
  const handleSubmitSubject = async (e) => {
    e.preventDefault();
    if (!formSubjectCode.trim() || !formSubjectName.trim()) {
      showToast({ type: 'error', message: 'Vui lòng điền mã môn học và tên môn học đầy đủ.' });
      return;
    }

    setIsSubmitting(true);
    try {
      if (editingSubject) {
        const updated = await adminSubjectApi.updateSubject(editingSubject.subjectId, {
          subjectName: formSubjectName.trim(),
          description: formDescription,
          credits: Number(formCredits),
          status: formStatus
        });
        setSubjects(prev => prev.map(s => s.subjectId === updated.subjectId ? updated : s));
        showToast({ type: 'success', message: 'Cập nhật môn học thành công!' });
      } else {
        const created = await adminSubjectApi.createSubject({
          subjectCode: formSubjectCode.toUpperCase().trim(),
          subjectName: formSubjectName.trim(),
          description: formDescription,
          credits: Number(formCredits),
          status: formStatus
        });
        setSubjects(prev => [created, ...prev]);
        showToast({ type: 'success', message: 'Tạo mới môn học thành công!' });
      }
      setIsEditModalOpen(false);
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Deactivate subject
  const handleConfirmDeactivate = async () => {
    if (!deactivateTarget) return;
    try {
      await adminSubjectApi.deactivateSubject(deactivateTarget.subjectId);
      setSubjects(prev => prev.map(s => s.subjectId === deactivateTarget.subjectId ? { ...s, status: 'INACTIVE' } : s));
      showToast({ type: 'success', message: 'Môn học đã chuyển sang trạng thái ngưng hoạt động.' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setDeactivateTarget(null);
    }
  };

  // Assign lecturer
  const handleAssignLecturer = async () => {
    if (!selectedLecturerIdToAdd || !activeSubjectForAssign) return;
    setIsAssigning(true);
    try {
      await adminSubjectApi.assignLecturers(activeSubjectForAssign.subjectId, [selectedLecturerIdToAdd]);
      const refreshed = await adminSubjectApi.getLecturersBySubject(activeSubjectForAssign.subjectId);
      setAssignments(prev => [
        ...prev.filter(a => a.subjectId !== activeSubjectForAssign.subjectId),
        ...(Array.isArray(refreshed) ? refreshed : [])
      ]);
      setSelectedLecturerIdToAdd('');
      showToast({ type: 'success', message: 'Phân công giảng viên phụ trách môn thành công!' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsAssigning(false);
    }
  };

  // Revoke assignment
  const handleRevokeAssignment = async (assignmentId) => {
    try {
      await adminSubjectApi.revokeAssignment(assignmentId);
      setAssignments(prev => prev.filter(a => a.lecturerSubjectId !== assignmentId));
      showToast({ type: 'success', message: 'Đã thu hồi quyền giảng viên phụ trách môn học.' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  // Filtered subjects
  const filteredSubjects = useMemo(() => {
    return subjects.filter(sub => {
      const matchSearch = searchTerm === '' || 
        sub.subjectCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        sub.subjectName?.toLowerCase().includes(searchTerm.toLowerCase());
      const matchStatus = statusFilter === 'ALL' || sub.status === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [subjects, searchTerm, statusFilter]);

  // Current active subject assignments
  const activeSubjectAssignments = useMemo(() => {
    if (!activeSubjectForAssign) return [];
    return assignments.filter(a => a.subjectId === activeSubjectForAssign.subjectId && !a.revokedAt);
  }, [assignments, activeSubjectForAssign]);

  return (
    <div className="space-y-6 animate-modal-entry">
      
      {/* HEADER SECTION */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <div className="flex items-center gap-2 text-sky-600 font-bold text-xs uppercase tracking-wider mb-1">
            <BookOpen className="w-4 h-4" />
            <span>Phân Hệ Quản Trị Học Thuật (Group 7 &amp; Admin)</span>
          </div>
          <h1 className="text-xl font-heading font-extrabold text-slate-900 tracking-tight">
            Quản Lý Môn Học &amp; Phân Công Giảng Viên
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Thiết lập danh mục môn học, tín chỉ và chỉ định quyền phụ trách câu hỏi theo chuẩn Course-scoped RLS.
          </p>
        </div>

        <button
          type="button"
          onClick={handleOpenCreateModal}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm hover:shadow-sky-500/20 active:scale-95 transition focus:outline-none focus:ring-2 focus:ring-sky-500"
        >
          <Plus className="w-4 h-4" />
          <span>Thêm Môn Học Mới</span>
        </button>
      </div>

      {/* METRIC CARDS */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200/90 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-sky-50 flex items-center justify-center text-sky-600 shrink-0">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <p className="text-[11px] font-semibold text-slate-500">Tổng Môn Học</p>
            <p className="text-xl font-heading font-extrabold text-slate-900">{subjects.length}</p>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200/90 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 flex items-center justify-center text-emerald-600 shrink-0">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <p className="text-[11px] font-semibold text-slate-500">Đang Giảng Dạy (Active)</p>
            <p className="text-xl font-heading font-extrabold text-emerald-600">
              {subjects.filter(s => s.status === 'ACTIVE').length}
            </p>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200/90 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 flex items-center justify-center text-indigo-600 shrink-0">
            <GraduationCap className="w-5 h-5" />
          </div>
          <div>
            <p className="text-[11px] font-semibold text-slate-500">Lượt Phân Công Phụ Trách</p>
            <p className="text-xl font-heading font-extrabold text-indigo-600">{assignments.length}</p>
          </div>
        </div>
      </div>

      {/* FILTER BAR */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white p-3.5 rounded-2xl border border-slate-200 shadow-xs">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Tìm theo mã môn (VD: SWD392) hoặc tên môn học..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 rounded-xl text-xs bg-slate-50 border border-slate-200 focus:bg-white focus:outline-none focus:ring-2 focus:ring-sky-500 transition text-slate-800 placeholder-slate-400 font-medium"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="py-2 px-3 rounded-xl text-xs bg-slate-50 border border-slate-200 text-slate-700 font-semibold focus:outline-none focus:ring-2 focus:ring-sky-500 cursor-pointer"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động (Active)</option>
            <option value="INACTIVE">Ngưng hoạt động (Inactive)</option>
          </select>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        {isLoading ? (
          <div className="p-6">
            <SkeletonTable rows={4} columns={5} />
          </div>
        ) : filteredSubjects.length === 0 ? (
          <div className="p-12 text-center">
            <BookOpen className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <h3 className="font-heading font-bold text-slate-800 text-sm">Chưa có môn học nào</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              Không tìm thấy môn học phù hợp với bộ lọc hiện tại. Hãy tạo mới môn học hoặc xóa từ khóa tìm kiếm.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200/80 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                  <th className="py-3 px-4">Mã Môn</th>
                  <th className="py-3 px-4">Tên Môn Học</th>
                  <th className="py-3 px-4">Tín Chỉ</th>
                  <th className="py-3 px-4">Trạng Thái</th>
                  <th className="py-3 px-4">Giảng Viên Phụ Trách</th>
                  <th className="py-3 px-4 text-right">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredSubjects.map((sub) => {
                  const subAssignments = assignments.filter(a => a.subjectId === sub.subjectId && !a.revokedAt);
                  return (
                    <tr key={sub.subjectId} className="hover:bg-slate-50/80 transition group">
                      <td className="py-3 px-4 font-mono font-bold text-sky-700">
                        {sub.subjectCode}
                      </td>
                      <td className="py-3 px-4">
                        <div className="font-bold text-slate-900">{sub.subjectName}</div>
                        {sub.description && (
                          <div className="text-[11px] text-slate-500 line-clamp-1 max-w-md">{sub.description}</div>
                        )}
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-700">
                        {sub.credits} tín chỉ
                      </td>
                      <td className="py-3 px-4">
                        {sub.status === 'ACTIVE' ? (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-200/80">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                            ACTIVE
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-extrabold bg-slate-100 text-slate-600 border border-slate-300">
                            <span className="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
                            INACTIVE
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => openAssignModal(sub)}
                            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-sky-50 hover:bg-sky-100 text-sky-700 border border-sky-200 text-[11px] font-semibold transition active:scale-95"
                          >
                            <Users className="w-3.5 h-3.5" />
                            <span>{subAssignments.length} Giảng viên</span>
                          </button>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-right space-x-1">
                        <button
                          type="button"
                          onClick={() => handleOpenEditModal(sub)}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition inline-flex items-center"
                          title="Chỉnh sửa môn học"
                        >
                          <Edit3 className="w-4 h-4" />
                        </button>
                        {sub.status === 'ACTIVE' && (
                          <button
                            type="button"
                            onClick={() => setDeactivateTarget(sub)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition inline-flex items-center"
                            title="Ngưng hoạt động môn học"
                          >
                            <PowerOff className="w-4 h-4" />
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* CREATE / EDIT SUBJECT MODAL */}
      {isEditModalOpen && (
        <div 
          className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in"
          role="dialog"
        >
          <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden animate-modal-entry flex flex-col">
            <div className="p-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-sky-600" />
                <h3 className="font-heading font-extrabold text-sm text-slate-900">
                  {editingSubject ? `Chỉnh Sửa Môn Học: ${editingSubject.subjectCode}` : 'Thêm Môn Học Mới'}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsEditModalOpen(false)}
                className="text-slate-400 hover:text-slate-700 p-1.5 rounded-lg hover:bg-slate-200 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmitSubject} className="p-5 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Mã Môn Học (Subject Code) <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  disabled={!!editingSubject}
                  placeholder="Ví dụ: SWD392, PRN211..."
                  value={formSubjectCode}
                  onChange={(e) => setFormSubjectCode(e.target.value.toUpperCase())}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 font-mono focus:outline-none focus:ring-2 focus:ring-sky-500 disabled:bg-slate-100 disabled:text-slate-500"
                />
                {editingSubject && (
                  <p className="text-[10px] text-slate-400 mt-1">Mã môn học không được phép sửa đổi sau khi đã khởi tạo.</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Tên Môn Học <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Software Architecture and Design"
                  value={formSubjectName}
                  onChange={(e) => setFormSubjectName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Số Tín Chỉ (Credits)</label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    value={formCredits}
                    onChange={(e) => setFormCredits(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Trạng Thái</label>
                  <select
                    value={formStatus}
                    onChange={(e) => setFormStatus(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium cursor-pointer"
                  >
                    <option value="ACTIVE">ACTIVE (Hoạt động)</option>
                    <option value="INACTIVE">INACTIVE (Ngưng)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Mô Tả Đề Cương / Giới Thiệu</label>
                <textarea
                  rows="3"
                  placeholder="Mô tả nội dung trọng tâm của học phần..."
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium"
                />
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  disabled={isSubmitting}
                  className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-2"
                >
                  {isSubmitting && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                  <span>{editingSubject ? 'Lưu Thay Đổi' : 'Tạo Môn Học'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ASSIGN LECTURERS MODAL */}
      {isAssignModalOpen && activeSubjectForAssign && (
        <div 
          className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in"
          role="dialog"
        >
          <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden animate-modal-entry flex flex-col">
            <div className="p-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 text-indigo-600 font-bold text-xs">
                  <GraduationCap className="w-4 h-4" />
                  <span>Phân Công Giảng Viên Phụ Trách</span>
                </div>
                <h3 className="font-heading font-extrabold text-sm text-slate-900 mt-0.5">
                  {activeSubjectForAssign.subjectCode} — {activeSubjectForAssign.subjectName}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsAssignModalOpen(false)}
                className="text-slate-400 hover:text-slate-700 p-1.5 rounded-lg hover:bg-slate-200 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 space-y-4">
              {/* Add Assignment Section */}
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <label className="block text-xs font-bold text-slate-700">
                  Thêm Giảng Viên Mới Vào Môn Học
                </label>
                <div className="flex gap-2">
                  <select
                    value={selectedLecturerIdToAdd}
                    onChange={(e) => setSelectedLecturerIdToAdd(e.target.value)}
                    className="flex-1 px-3 py-2 rounded-xl text-xs border border-slate-300 focus:outline-none focus:ring-2 focus:ring-sky-500 font-medium cursor-pointer"
                  >
                    <option value="">-- Chọn Giảng Viên (ROLE_LECTURER) --</option>
                    {lecturers.map(lec => (
                      <option key={lec.userId} value={lec.userId}>
                        {lec.fullName} ({lec.email})
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={handleAssignLecturer}
                    disabled={!selectedLecturerIdToAdd || isAssigning}
                    className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs transition active:scale-95 disabled:opacity-50 flex items-center gap-1.5 shrink-0"
                  >
                    {isAssigning && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                    <span>Phân Công</span>
                  </button>
                </div>
              </div>

              {/* Current Assigned Lecturers List */}
              <div>
                <h4 className="text-xs font-bold text-slate-700 mb-2 flex items-center justify-between">
                  <span>Danh Sách Đang Phụ Trách ({activeSubjectAssignments.length})</span>
                  <span className="text-[10px] text-slate-400 font-normal">Chỉ định quyền soạn &amp; duyệt đề</span>
                </h4>

                {activeSubjectAssignments.length === 0 ? (
                  <div className="p-6 text-center border border-dashed border-slate-200 rounded-xl text-slate-400 text-xs">
                    Chưa có giảng viên nào được phân công phụ trách môn này.
                  </div>
                ) : (
                  <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                    {activeSubjectAssignments.map(asg => {
                      // BE LecturerSubjectDto only carries lecturerId; resolve name from loaded lecturer list
                      const lecturer = lecturers.find(l => l.userId === asg.lecturerId);
                      return (
                      <div
                        key={asg.lecturerSubjectId}
                        className="flex items-center justify-between p-3 bg-white border border-slate-200 rounded-xl hover:border-slate-300 transition"
                      >
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-indigo-50 text-indigo-600 font-bold text-xs flex items-center justify-center">
                            {lecturer?.fullName ? lecturer.fullName.charAt(0) : 'L'}
                          </div>
                          <div>
                            <p className="text-xs font-bold text-slate-900">{lecturer?.fullName || 'Giảng viên'}</p>
                            <p className="text-[11px] text-slate-500 font-mono">{lecturer?.email || asg.lecturerId}</p>
                          </div>
                        </div>

                        <button
                          type="button"
                          onClick={() => handleRevokeAssignment(asg.lecturerSubjectId)}
                          className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
                          title="Gỡ quyền phụ trách môn"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-100 flex justify-end">
              <button
                type="button"
                onClick={() => setIsAssignModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-800 text-xs font-bold transition active:scale-95"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CONFIRM DEACTIVATE MODAL */}
      <ConfirmModal
        isOpen={!!deactivateTarget}
        title="Ngưng Hoạt Động Môn Học?"
        message={`Bạn có chắc chắn muốn ngưng hoạt động môn ${deactivateTarget?.subjectCode} (${deactivateTarget?.subjectName})? Môn học ngưng hoạt động sẽ không thể phân công mới hoặc tạo đề thi.`}
        confirmText="Ngưng Hoạt Động"
        cancelText="Giữ Lại"
        isDanger={true}
        onConfirm={handleConfirmDeactivate}
        onCancel={() => setDeactivateTarget(null)}
      />

    </div>
  );
}
