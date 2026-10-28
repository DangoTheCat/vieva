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
  GraduationCap,
  Trash2, 
  X,
  Loader2
} from 'lucide-react';
import { adminSubjectApi } from '../api/adminSubjectApi';
import { adminUserApi } from '../api/adminUserApi';
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

  // Keyboard accessibility: Escape to close modals
  useEffect(() => {
    if (!isEditModalOpen && !isAssignModalOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        setIsEditModalOpen(false);
        setIsAssignModalOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isEditModalOpen, isAssignModalOpen]);

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
        const subjectList = Array.isArray(content) ? content : [];
        setSubjects(subjectList);

        // Pre-fetch lecturer assignments for loaded subjects so metrics & table counts are accurate immediately
        if (subjectList.length > 0) {
          try {
            const assignResults = await Promise.allSettled(
              subjectList.map(s => adminSubjectApi.getLecturersBySubject(s.subjectId))
            );
            const allAssignments = [];
            assignResults.forEach(res => {
              if (res.status === 'fulfilled' && Array.isArray(res.value)) {
                allAssignments.push(...res.value);
              }
            });
            setAssignments(allAssignments);
          } catch {
            // Keep default assignments
          }
        }
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
    } catch {
      // Quietly keep current assignments
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

  // Stats calculation
  const activeSubjectsCount = subjects.filter(s => s.status === 'ACTIVE').length;
  const activePercentage = subjects.length > 0 ? Math.round((activeSubjectsCount / subjects.length) * 100) : 0;
  const distinctLecturersCount = new Set(assignments.map(a => a.lecturerId)).size;

  return (
    <div className="space-y-6 animate-modal-entry pb-12">
      
      <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="font-heading text-2xl font-bold text-ink tracking-tight">
            Quản Lý Môn Học &amp; Phân Công Giảng Viên
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Thiết lập danh mục môn học, tín chỉ và chỉ định quyền phụ trách môn cho giảng viên.
          </p>
        </div>

        <button
          type="button"
          onClick={handleOpenCreateModal}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-pink-600 text-white font-semibold text-xs shadow-xs transition-all hover:bg-pink-700 active:scale-[0.98] cursor-pointer shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>Thêm Môn Học Mới</span>
        </button>
      </header>

      {/* ========================================================================= */}
      {/* 2. METRIC CARDS (3 THẺ THỐNG KÊ)                                          */}
      {/* ========================================================================= */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Thẻ 1: Tổng môn học */}
        <div className="relative rounded-xl border border-pink-200 bg-white p-5 shadow-2xs flex items-center justify-between transition-transform hover:-translate-y-0.5">
          <div className="space-y-1">
            <p className="text-xs uppercase tracking-wider text-slate-600 font-semibold">Tổng Môn Học</p>
            <p className="font-heading font-black text-3xl text-ink">{subjects.length}</p>
            <span className="inline-block text-[11px] font-bold text-slate-600 bg-slate-50 px-2 py-0.5 rounded border border-pink-200">
              Niên giám 2024-2025
            </span>
          </div>
          <div className="w-14 h-14 rounded-xl border border-pink-200 bg-green-50 flex items-center justify-center text-green-800 shadow-sm">
            <BookOpen className="w-7 h-7" />
          </div>
        </div>

        {/* Thẻ 2: Đang giảng dạy */}
        <div className="relative rounded-xl border border-pink-200 bg-white p-5 shadow-2xs flex items-center justify-between transition-transform hover:-translate-y-0.5">
          <div className="space-y-1">
            <p className="text-xs uppercase tracking-wider text-slate-600 font-semibold">Đang Giảng Dạy (Active)</p>
            <p className="font-heading font-black text-3xl text-green-700">{activeSubjectsCount}</p>
            <span className="inline-block text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
              {activePercentage}% Đang Kích Hoạt
            </span>
          </div>
          <div className="w-14 h-14 rounded-xl border border-pink-200 bg-green-100 flex items-center justify-center text-green-700 shadow-sm">
            <CheckCircle2 className="w-7 h-7" />
          </div>
        </div>

        {/* Thẻ 3: Phân công phụ trách */}
        <div className="relative rounded-xl border border-pink-200 bg-white p-5 shadow-2xs flex items-center justify-between transition-transform hover:-translate-y-0.5">
          <div className="space-y-1">
            <p className="text-xs uppercase tracking-wider text-slate-500 font-semibold">Lượt Phân Công Phụ Trách</p>
            <p className="font-heading font-bold text-3xl text-ink">{assignments.length}</p>
            <span className="inline-block text-[11px] font-semibold text-slate-600 bg-slate-50 px-2 py-0.5 rounded border border-pink-200">
              {distinctLecturersCount} Giảng viên đã giao môn
            </span>
          </div>
          <div className="w-14 h-14 rounded-xl border border-pink-200 bg-slate-50 flex items-center justify-center text-slate-700 shadow-2xs">
            <GraduationCap className="w-7 h-7" />
          </div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 3. FILTER & SEARCH BAR                                                    */}
      {/* ========================================================================= */}
      <div className="rounded-xl border border-pink-200 bg-white p-4 shadow-2xs flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Ô tìm kiếm */}
        <div className="relative w-full md:w-2/3">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-green-700">
            <Search className="w-4 h-4 text-green-700" />
          </div>
          <input
            type="text"
            placeholder="Tìm theo mã môn (VD: SWD392) hoặc tên môn học..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-lg border border-pink-200 bg-paper font-medium text-xs sm:text-sm text-ink placeholder:text-green-800/60 focus:outline-none focus:border-pink-500 focus:shadow-sm transition-all"
          />
        </div>

        {/* Bộ lọc trạng thái */}
        <div className="flex items-center gap-2 w-full md:w-auto">
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-pink-200 bg-green-50 text-green-800 shrink-0">
            <Filter className="w-3.5 h-3.5 text-green-800" />
            <span className="font-bold text-xs uppercase">Trạng Thái:</span>
          </div>
          <div className="relative flex-1 md:w-56">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full pl-3 pr-8 py-2 rounded-lg border border-pink-200 bg-paper font-bold text-xs text-ink focus:outline-none focus:border-pink-500 focus:shadow-sm cursor-pointer"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động (Active)</option>
              <option value="INACTIVE">Ngưng hoạt động (Inactive)</option>
            </select>
          </div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 4. SUBJECTS TABLE (BẢNG DANH SÁCH MÔN HỌC)                                */}
      {/* ========================================================================= */}
      <div className="rounded-xl border border-pink-200 bg-paper shadow-md overflow-hidden">
        {isLoading ? (
          <div className="p-6">
            <SkeletonTable rows={4} columns={6} />
          </div>
        ) : filteredSubjects.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="w-14 h-14 rounded-2xl bg-slate-100 border border-pink-200 flex items-center justify-center text-slate-700 mx-auto shadow-sm">
              <BookOpen className="w-7 h-7" />
            </div>
            <h3 className="font-heading font-black text-ink text-base">Chưa có môn học nào</h3>
            <p className="text-xs text-green-800 max-w-sm mx-auto">
              Không tìm thấy môn học phù hợp với bộ lọc hiện tại. Hãy tạo mới môn học hoặc xóa từ khóa tìm kiếm.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left">
              <thead>
                <tr className="border-b border-pink-200 bg-slate-50 font-semibold text-xs text-slate-700">
                  <th className="py-3 px-4 w-28">Mã Môn</th>
                  <th className="py-3 px-4">Tên Môn Học</th>
                  <th className="py-3 px-4 text-center w-28">Tín Chỉ</th>
                  <th className="py-3 px-4 text-center w-36">Trạng Thái</th>
                  <th className="py-3 px-4">Giảng Viên Phụ Trách</th>
                  <th className="py-3 px-4 text-right w-32">Thao Tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-xs text-slate-800">
                {filteredSubjects.map((sub) => {
                  const subAssignments = assignments.filter(a => a.subjectId === sub.subjectId && !a.revokedAt);
                  return (
                    <tr key={sub.subjectId} className="hover:bg-slate-50/70 transition-colors">
                      {/* Mã môn */}
                      <td className="py-4 px-4 align-top font-bold text-ink">
                        <span className="inline-block px-2.5 py-1 rounded bg-slate-100 border border-pink-200 font-mono font-semibold text-xs text-slate-800">
                          {sub.subjectCode}
                        </span>
                      </td>

                      {/* Tên môn học */}
                      <td className="py-4 px-4 align-top max-w-md">
                        <div className="space-y-1">
                          <div className="font-heading font-bold text-sm sm:text-base text-ink leading-snug">
                            {sub.subjectName}
                          </div>
                          {sub.description && (
                            <div className="text-xs text-green-800 italic line-clamp-2">
                              {sub.description}
                            </div>
                          )}
                        </div>
                      </td>

                      {/* Tín chỉ */}
                      <td className="py-4 px-4 align-top text-center">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 border border-pink-200 text-slate-700">
                          {sub.credits} tín chỉ
                        </span>
                      </td>

                      {/* Trạng thái */}
                      <td className="py-4 px-4 align-top text-center">
                        {sub.status === 'ACTIVE' ? (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 border border-emerald-200 text-emerald-700">
                            <span className="w-2 h-2 rounded-full bg-green-600 animate-pulse"></span>
                            ACTIVE
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-slate-100 border border-pink-200 text-slate-600">
                            <span className="w-2 h-2 rounded-full bg-slate-400"></span>
                            INACTIVE
                          </span>
                        )}
                      </td>

                      {/* Giảng viên phụ trách */}
                      <td className="py-4 px-4 align-top">
                        {(() => {
                          const assignedLecturers = subAssignments
                            .map(asg => lecturers.find(l => l.userId === asg.lecturerId))
                            .filter(Boolean);
  
                          if (assignedLecturers.length === 0) {
                            return (
                              <button
                                type="button"
                                onClick={() => openAssignModal(sub)}
                                className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-paper border border-dashed border-pink-200 font-semibold text-xs text-slate-600 hover:bg-slate-50 transition cursor-pointer"
                              >
                                <Users className="w-3.5 h-3.5 text-green-700" />
                                <span>+ Phân công GV</span>
                              </button>
                            );
                          }

                          return (
                            <div className="space-y-1.5">
                              <div className="flex items-center gap-2 flex-wrap">
                                <div className="flex -space-x-2 overflow-hidden py-0.5">
                                  {assignedLecturers.slice(0, 4).map((lec) => (
                                    <div
                                      key={lec.userId}
                                      className="w-7 h-7 rounded-full border border-white bg-slate-200 flex items-center justify-center font-bold text-[10px] text-slate-700 shadow-2xs overflow-hidden shrink-0"
                                      title={`${lec.fullName} (${lec.email})`}
                                    >
                                      {(lec.avatarUrl || lec.avatar) ? (
                                        <img
                                          src={(lec.avatarUrl || lec.avatar).trim()}
                                          alt={lec.fullName}
                                          className="w-full h-full object-cover"
                                          onError={(e) => { e.currentTarget.style.display = 'none'; }}
                                        />
                                      ) : (
                                        lec.fullName ? lec.fullName.charAt(0).toUpperCase() : 'G'
                                      )}
                                    </div>
                                  ))}
                                </div>
                                <button
                                  type="button"
                                  onClick={() => openAssignModal(sub)}
                                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-green-100 hover:bg-green-200 border border-pink-200 font-bold text-xs text-green-900 shadow-sm transition cursor-pointer"
                                  title="Xem và chỉnh sửa danh sách phân công"
                                >
                                  <span>{subAssignments.length} GV</span>
                                </button>
                              </div>
                              
                            </div>
                          );
                        })()}
                      </td>

                      {/* Thao tác */}
                      <td className="py-4 px-4 align-top text-right">
                        <div className="inline-flex items-center gap-1.5">
                          <button
                            type="button"
                            onClick={() => handleOpenEditModal(sub)}
                            className="p-1.5 rounded-lg border border-pink-200 bg-paper text-ink hover:bg-green-100 shadow-sm transition-all cursor-pointer"
                            title="Chỉnh sửa môn học"
                          >
                            <Edit3 className="w-4 h-4 text-green-900" />
                          </button>
                          {sub.status === 'ACTIVE' && (
                            <button
                              type="button"
                              onClick={() => setDeactivateTarget(sub)}
                              className="p-1.5 rounded-lg border border-pink-200 bg-paper text-rose-600 hover:bg-rose-50 shadow-sm transition-all cursor-pointer"
                              title="Ngưng hoạt động môn học"
                            >
                              <PowerOff className="w-4 h-4 text-pink-700" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Table Footer with Info & Pagination */}
        <div className="p-4 border-t border-pink-200 bg-white flex flex-col sm:flex-row items-center justify-between gap-3">
          <span className="text-xs font-medium text-green-800">
            Hiển thị {filteredSubjects.length} / {subjects.length} môn học trong hệ thống
          </span>
          <div className="inline-flex items-center gap-1">
            <button
              type="button"
              className="px-3 py-1 rounded border border-pink-200 bg-green-100 text-green-900 font-bold text-xs shadow-sm"
            >
              1
            </button>
          </div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 5. CREATE / EDIT SUBJECT MODAL                                            */}
      {/* ========================================================================= */}
      {isEditModalOpen && (
        <div 
          className="fixed inset-0 z-50 bg-ink/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in"
          role="dialog"
        >
          <div className="relative w-full max-w-lg rounded-2xl border border-pink-200 bg-paper p-6 sm:p-7 shadow-md flex flex-col animate-modal-entry">
            <div className="flex items-center justify-between border-b-2 border-cream pb-3.5">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-slate-100 border border-pink-200 flex items-center justify-center text-slate-700 shadow-sm">
                  <BookOpen className="w-4 h-4" />
                </div>
                <h3 className="font-heading font-black text-base text-ink">
                  {editingSubject ? `Chỉnh Sửa Môn Học: ${editingSubject.subjectCode}` : 'Thêm Môn Học Mới'}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsEditModalOpen(false)}
                className="p-1 rounded-lg border border-pink-200 bg-slate-50 text-slate-600 hover:bg-slate-100 shadow-sm transition cursor-pointer"
                title="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmitSubject} className="pt-4 space-y-4">
              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">
                  Mã Môn Học (Subject Code) <span className="text-pink-700">*</span>
                </label>
                <input
                  type="text"
                  required
                  disabled={!!editingSubject}
                  placeholder="Ví dụ: SWD392, PRN211..."
                  value={formSubjectCode}
                  onChange={(e) => setFormSubjectCode(e.target.value.toUpperCase())}
                  className="w-full px-3 py-2 rounded-xl text-xs font-mono font-bold border border-pink-200 bg-paper text-ink focus:outline-none focus:border-pink-500 focus:shadow-sm disabled:bg-green-50/70 disabled:text-green-800 disabled:cursor-not-allowed"
                />
                {editingSubject && (
                  <p className="text-[10px] text-green-700 mt-1">Mã môn học cố định và không được phép sửa đổi sau khi khởi tạo.</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">
                  Tên Môn Học <span className="text-pink-700">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Software Architecture and Design"
                  value={formSubjectName}
                  onChange={(e) => setFormSubjectName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-pink-200 bg-paper text-ink font-bold focus:outline-none focus:border-pink-500 focus:shadow-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-green-800 mb-1">Số Tín Chỉ (Credits)</label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    value={formCredits}
                    onChange={(e) => setFormCredits(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl text-xs border border-pink-200 bg-paper text-ink font-bold focus:outline-none focus:border-pink-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-green-800 mb-1">Trạng Thái</label>
                  <select
                    value={formStatus}
                    onChange={(e) => setFormStatus(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl text-xs border border-pink-200 bg-paper text-ink font-bold focus:outline-none focus:border-pink-500 cursor-pointer"
                  >
                    <option value="ACTIVE">ACTIVE (Hoạt động)</option>
                    <option value="INACTIVE">INACTIVE (Ngưng)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-green-800 mb-1">Mô Tả Đề Cương / Giới Thiệu</label>
                <textarea
                  rows="3"
                  placeholder="Mô tả nội dung trọng tâm của học phần..."
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl text-xs border border-pink-200 bg-paper text-ink font-medium focus:outline-none focus:border-pink-500"
                />
              </div>

              <div className="pt-3 border-t-2 border-cream flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  disabled={isSubmitting}
                  className="px-4 py-2 rounded-xl border border-pink-200 bg-paper text-green-900 text-xs font-bold hover:bg-green-50 transition cursor-pointer"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs shadow-sm transition active:scale-95 disabled:opacity-50 flex items-center gap-2 cursor-pointer"
                >
                  {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin text-ink" />}
                  <span>{editingSubject ? 'Lưu Thay Đổi' : 'Tạo Môn Học'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 6. ASSIGN LECTURERS MODAL                                                 */}
      {/* ========================================================================= */}
      {isAssignModalOpen && activeSubjectForAssign && (
        <div 
          className="fixed inset-0 z-50 bg-ink/40 backdrop-blur-[2px] flex items-center justify-center p-4 animate-in fade-in"
          role="dialog"
        >
          <div className="relative w-full max-w-3xl rounded-xl border border-pink-200 bg-paper p-6 md:p-8 shadow-md max-h-[90vh] overflow-y-auto animate-modal-entry">
            
            {/* Modal Header */}
            <div className="flex items-start justify-between border-b-2 border-cream pb-4">
              <div className="space-y-1">
                <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-pink-100 border border-pink-200 font-bold text-xs text-pink-800 shadow-sm">
                  <GraduationCap className="w-3.5 h-3.5 text-pink-700" />
                  <span>Phân Công Giảng Viên Phụ Trách</span>
                </div>
                <h2 className="font-heading font-black text-xl text-ink pt-1">
                  {activeSubjectForAssign.subjectCode} — {activeSubjectForAssign.subjectName}
                </h2>
                <div className="text-xs text-green-900 flex items-center gap-2 flex-wrap">
                  <span>Giảng viên được phân công sẽ có quyền biên soạn và quản lý ngân hàng câu hỏi của môn học.</span>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsAssignModalOpen(false)}
                className="p-1.5 rounded-lg border border-pink-200 bg-slate-50 text-slate-600 hover:bg-slate-100 shadow-sm cursor-pointer"
                title="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Khung Thêm Giảng Viên */}
            <div className="my-5 p-4 rounded-xl border border-pink-200 bg-green-50 shadow-sm">
              <label className="block font-bold text-xs text-green-900 mb-2">Thêm giảng viên vào môn học:</label>
              <div className="flex flex-col sm:flex-row gap-3">
                <div className="relative flex-1">
                  <select
                    value={selectedLecturerIdToAdd}
                    onChange={(e) => setSelectedLecturerIdToAdd(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg border border-pink-200 bg-paper font-bold text-xs text-ink focus:outline-none focus:border-pink-500 cursor-pointer"
                  >
                    <option value="">-- Chọn Giảng Viên (ROLE_LECTURER) --</option>
                    {lecturers.map(lec => (
                      <option key={lec.userId} value={lec.userId}>
                        {lec.fullName} ({lec.email})
                      </option>
                    ))}
                  </select>
                </div>
                <button
                  type="button"
                  onClick={handleAssignLecturer}
                  disabled={!selectedLecturerIdToAdd || isAssigning}
                  className="px-5 py-2 rounded-lg bg-pink-600 text-white font-semibold text-xs shadow-xs hover:bg-pink-700 active:scale-[0.98] cursor-pointer shrink-0 disabled:opacity-50 flex items-center justify-center gap-1.5"
                >
                  {isAssigning && <Loader2 className="w-3.5 h-3.5 animate-spin text-ink" />}
                  <span>Phân Công</span>
                </button>
              </div>
            </div>

            {/* Danh Sách Đang Phụ Trách */}
            <div className="space-y-3">
              <h3 className="font-bold text-sm text-ink flex items-center gap-2">
                <span>Giảng viên đang phụ trách môn học ({activeSubjectAssignments.length})</span>
                <span className="w-2.5 h-2.5 rounded-full bg-green-500 inline-block animate-pulse"></span>
              </h3>

              {activeSubjectAssignments.length === 0 ? (
                <div className="p-6 text-center border border-dashed border-pink-200 rounded-xl text-slate-500 text-xs bg-slate-50">
                  Chưa có giảng viên nào được phân công phụ trách môn này.
                </div>
              ) : (
                <div className="space-y-2.5 max-h-72 overflow-y-auto pr-1">
                  {activeSubjectAssignments.map(asg => {
                    const lecturer = lecturers.find(l => l.userId === asg.lecturerId);
                    return (
                      <div
                        key={asg.lecturerSubjectId}
                        className="p-3.5 rounded-xl border border-pink-200 bg-paper flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-sm"
                      >
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-lg border border-pink-200 bg-green-50 flex items-center justify-center text-green-900 font-bold text-sm shadow-sm overflow-hidden shrink-0">
                            {lecturer?.avatarUrl ? (
                              <img src={lecturer.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                            ) : (
                              lecturer?.fullName ? lecturer.fullName.charAt(0).toUpperCase() : 'G'
                            )}
                          </div>
                          <div>
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="font-bold text-xs sm:text-sm text-ink">
                                {lecturer?.fullName || 'Giảng viên'}
                              </span>
                              <span className="font-bold text-[10px] text-green-800 bg-green-100 px-2 py-0.5 rounded border border-green-400">
                                Giảng viên
                              </span>
                            </div>
                            <span className="font-mono text-green-800 text-xs">
                              {lecturer?.email || asg.lecturerId}
                            </span>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                          <button
                            type="button"
                            onClick={() => handleRevokeAssignment(asg.lecturerSubjectId)}
                            className="p-1.5 rounded-lg border border-pink-200 bg-paper text-rose-600 hover:bg-rose-50 shadow-sm transition-all cursor-pointer"
                            title="Gỡ quyền phụ trách môn"
                          >
                            <Trash2 className="w-4 h-4 text-pink-700" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Modal Actions */}
            <div className="mt-6 pt-4 border-t-2 border-cream flex items-center justify-end">
              <button
                type="button"
                onClick={() => setIsAssignModalOpen(false)}
                className="px-5 py-2.5 rounded-xl border border-pink-200 bg-paper font-bold text-xs text-green-900 shadow-sm hover:bg-green-100 active:scale-[0.98] cursor-pointer"
              >
                Đóng Cửa Sổ
              </button>
            </div>

          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 7. CONFIRM DEACTIVATE MODAL                                               */}
      {/* ========================================================================= */}
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
