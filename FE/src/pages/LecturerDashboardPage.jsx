import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { lecturerCatalogApi } from '../api/lecturerCatalogApi';
import { lecturerGradingApi } from '../api/lecturerGradingApi';
import { getErrorMessage } from '../utils/errorCodes';
import {
  Plus,
  Search,
  ArrowRight,
  User,
  X,
  Clock,
  CheckCircle2,
  BookOpen,
  Loader2,
  FolderPlus,
  ClipboardList
} from 'lucide-react';

export function LecturerDashboardPage({ onNavigate, showToast }) {
  const { currentUser } = useAuth();
  const lecturerName = currentUser?.fullName || 'Giảng Viên';

  const [subjects, setSubjects] = useState([]);
  const [topicsMap, setTopicsMap] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [gradingCourses, setGradingCourses] = useState([]);
  
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [spaceForm, setSpaceForm] = useState({ title: '', description: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fetch real assigned subjects & topics from Backend API
  useEffect(() => {
    async function loadDashboardData() {
      setIsLoading(true);
      try {
        const [assignedData, courses] = await Promise.all([
          lecturerCatalogApi.getAssignedSubjects(),
          lecturerGradingApi.getCourses()
        ]);
        const list = Array.isArray(assignedData) ? assignedData : assignedData?.content || [];
        setSubjects(list);
        setGradingCourses(Array.isArray(courses) ? courses : []);
        
        if (list.length > 0) {
          setSelectedSubjectId(list[0].subjectId);
          
          const map = {};
          const topicLists = await Promise.all(list.map(sub => lecturerCatalogApi.getTopics(sub.subjectId)));
          list.forEach((sub, i) => {
            map[sub.subjectId] = Array.isArray(topicLists[i]) ? topicLists[i] : [];
          });
          setTopicsMap(map);
        } else {
          setSelectedSubjectId('');
          setTopicsMap({});
        }
      } catch (err) {
        setSubjects([]);
        setSelectedSubjectId('');
        setTopicsMap({});
        showToast({ type: 'error', message: getErrorMessage(err) });
      } finally {
        setIsLoading(false);
      }
    }

    loadDashboardData();
  }, []);

  const handleCreateSpace = async (e) => {
    e.preventDefault();
    if (!spaceForm.title.trim() || !selectedSubjectId) return;

    setIsSubmitting(true);
    try {
      const createdTopic = await lecturerCatalogApi.createTopic(selectedSubjectId, {
        name: spaceForm.title.trim(),
        description: spaceForm.description.trim() || 'Không gian khảo thí tự động'
      });

      setTopicsMap(prev => ({
        ...prev,
        [selectedSubjectId]: [createdTopic, ...(prev[selectedSubjectId] || [])]
      }));

      showToast({
        type: 'success',
        title: 'Khởi Tạo Không Gian Thành Công',
        message: `Đã tạo chủ đề khảo thí "${createdTopic.name}" trên hệ thống!`
      });

      setIsCreateModalOpen(false);
      setSpaceForm({ title: '', description: '' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Filter real subjects & topics by search query
  const filteredSubjects = subjects.filter(sub => 
    sub.subjectName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    sub.subjectCode?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const gradingTotals = gradingCourses.reduce((acc, c) => ({
    examCount: acc.examCount + (c.examCount || 0),
    submittedCount: acc.submittedCount + (c.submittedCount || 0),
    pendingCount: acc.pendingCount + (c.pendingCount || 0),
    totalParticipants: acc.totalParticipants + (c.totalParticipants || 0)
  }), { examCount: 0, submittedCount: 0, pendingCount: 0, totalParticipants: 0 });

  return (
    <div className="space-y-8 max-w-6xl mx-auto w-full animate-modal-entry text-ink pb-10">

      {/* TOPBAR / HEADER SEARCH BAR & CREATE BUTTON */}
      <div className="bg-paper border-2 border-green-500 rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row items-center justify-between gap-4 shadow-[3px_3px_0_#DEB9B5]">
        <div>
          <h1 className="font-heading font-extrabold text-xl text-ink tracking-tight">
            Trang Tổng Quan Khảo Thí (Lecturer Overview)
          </h1>
          <p className="text-xs text-on-surface-variant mt-0.5">
            Xin chào, <strong className="text-ink font-semibold">{lecturerName}</strong> • Quản lý môn học phân công &amp; không gian khảo thí tự động
          </p>
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto">
          {/* Elongated Search Pill */}
          <div className="relative flex-1 sm:w-72">
            <input 
              type="text" 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm môn học, chủ đề khảo thí..." 
              className="w-full bg-green-50 border-2 border-green-500 rounded-full py-2 px-4 pl-9 text-xs text-ink placeholder:text-outline focus:outline-none focus:border-green-700 shadow-[2px_2px_0_#DEB9B5] transition-all" 
            />
            <Search className="w-4 h-4 text-green-700 absolute left-3 top-2.5 pointer-events-none" />
          </div>

          {/* Primary Pill Button: + Tạo Phòng Thi */}
          <button 
            type="button"
            onClick={() => setIsCreateModalOpen(true)}
            className="inline-flex items-center gap-2 bg-pink-500 hover:bg-pink-400 text-ink font-bold text-xs px-5 py-2.5 rounded-full border-2 border-green-600 shadow-[3px_3px_0_#DEB9B5] hover:shadow-[1px_1px_0_#DEB9B5] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all shrink-0 cursor-pointer"
          >
            <Plus className="w-4 h-4 text-pink-900 font-bold" />
            <span>Tạo Chủ Đề</span>
          </button>
        </div>
      </div>

      {/* GRADING OVERVIEW (GET /lecturer/grading/courses) */}
      <section className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Kỳ thi', value: gradingTotals.examCount, icon: ClipboardList },
          { label: 'Bài đã nộp', value: gradingTotals.submittedCount, icon: CheckCircle2 },
          { label: 'Đang thi / chờ chấm', value: gradingTotals.pendingCount, icon: Clock },
          { label: 'Thí sinh', value: gradingTotals.totalParticipants, icon: User }
        ].map(({ label, value, icon: Icon }) => (
          <div key={label} className="bg-paper border-2 border-green-500 rounded-2xl p-4 shadow-[3px_3px_0_#DEB9B5]">
            <div className="flex items-center gap-2 text-[11px] font-bold text-green-700 uppercase tracking-wide"><Icon className="w-4 h-4" /> {label}</div>
            <p className="font-heading font-black text-2xl mt-1">{isLoading ? '…' : value}</p>
          </div>
        ))}
      </section>

      <section className="flex flex-wrap gap-3">
        {[
          { view: 'lecturer-exams', label: 'Quản lý kỳ thi' },
          { view: 'lecturer-grading', label: 'Chấm điểm bài thi' },
          { view: 'lecturer-scores', label: 'Phúc khảo điểm' },
          { view: 'lecturer-questions', label: 'Ngân hàng câu hỏi' }
        ].map(link => (
          <button
            key={link.view}
            type="button"
            onClick={() => onNavigate && onNavigate(link.view)}
            className="inline-flex items-center gap-2 bg-paper text-ink hover:bg-green-50 font-bold text-xs px-4 py-2 rounded-full border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all cursor-pointer focus:outline-none focus:ring-2 focus:ring-green-600"
          >
            <span>{link.label}</span>
            <ArrowRight className="w-4 h-4 text-green-700" />
          </button>
        ))}
      </section>

      {/* ASSIGNED SUBJECTS & TOPICS SECTION (FROM BE API) */}
      <div className="space-y-4">
        <div className="flex items-center justify-between pb-2 border-b-2 border-cream">
          <h3 className="font-heading font-bold text-base text-ink flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-green-700" />
            <span>Môn Học Phụ Trách</span>
          </h3>
          <span className="text-xs font-semibold text-green-700 px-3 py-1 rounded-full bg-green-100 border border-green-500 shadow-[2px_2px_0_#DEB9B5]">
            {subjects.length} Môn học được phân công
          </span>
        </div>

        {isLoading ? (
          <div className="p-8 text-center bg-paper rounded-2xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] space-y-3">
            <Loader2 className="w-6 h-6 text-green-700 animate-spin mx-auto" />
            <p className="text-xs text-on-surface-variant font-medium">Đang tải danh sách môn học từ Backend...</p>
          </div>
        ) : filteredSubjects.length === 0 ? (
          <div className="p-8 text-center bg-paper rounded-2xl border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] space-y-3">
            <FolderPlus className="w-8 h-8 text-outline mx-auto" />
            <p className="text-sm font-bold text-ink">Chưa Có Môn Học Được Phân Công</p>
            <p className="text-xs text-on-surface-variant max-w-md mx-auto">
              Tài khoản hiện chưa được phân công phụ trách môn học nào trong cơ sở dữ liệu. Vui lòng liên hệ Administrator để phân công môn học.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {filteredSubjects.map((sub) => {
              const subTopics = topicsMap[sub.subjectId] || [];
              return (
                <div 
                  key={sub.subjectId}
                  className="bg-paper border-2 border-green-500 rounded-2xl p-6 shadow-[3px_3px_0_#DEB9B5] hover:shadow-[6px_6px_0_#D5A6A2] space-y-4 transition duration-200"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-green-100 text-green-800 border-2 border-green-500 font-mono shadow-[1px_1px_0_#DEB9B5]">
                        {sub.subjectCode}
                      </span>
                      <h4 className="font-heading font-extrabold text-base text-ink mt-2">
                        {sub.subjectName}
                      </h4>
                    </div>
                    <button
                      type="button"
                      onClick={() => onNavigate && onNavigate('lecturer-questions')}
                      className="text-xs font-bold text-green-700 hover:text-green-900 flex items-center gap-1 shrink-0 hover:underline cursor-pointer"
                    >
                      <span>Ngân Hàng</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Topics List from BE */}
                  <div className="space-y-2 pt-2 border-t border-cream">
                    <p className="text-[11px] font-bold text-on-surface-variant uppercase tracking-wider">
                      Chủ đề đề cương ({subTopics.length})
                    </p>
                    {subTopics.length === 0 ? (
                      <p className="text-xs text-outline italic">Chưa có chủ đề nào trong đề cương môn học này.</p>
                    ) : (
                      <div className="flex flex-wrap gap-2">
                        {subTopics.map((top) => (
                          <span 
                            key={top.topicId || top.id}
                            className="px-3 py-1 rounded-full bg-surface-container-lowest border border-outline-variant text-xs font-medium text-ink shadow-[1px_1px_0_#DEB9B5]"
                          >
                            {top.name}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* CREATE SPACE / TOPIC MODAL */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-paper border-2 border-green-500 rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-[6px_6px_0_#DEB9B5] border border-slate-200 space-y-4 animate-modal-entry text-ink">
            
            <div className="flex items-center justify-between pb-3 border-b border-cream">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-pink-100 border-2 border-green-500 text-pink-700 flex items-center justify-center font-bold text-sm shadow-[2px_2px_0_#DEB9B5]">
                  +
                </div>
                <h3 className="font-heading font-bold text-base text-ink">Tạo Chủ Đề Đề Cương Mới</h3>
              </div>
              <button 
                type="button"
                onClick={() => setIsCreateModalOpen(false)} 
                className="text-pink-700 hover:text-pink-900 p-1 cursor-pointer transition-colors"
                aria-label="Đóng"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSpace} className="space-y-3.5 text-xs text-ink">
              <div>
                <label className="font-bold text-ink block mb-1">Môn Học Phụ Trách</label>
                <select 
                  value={selectedSubjectId}
                  onChange={(e) => setSelectedSubjectId(e.target.value)}
                  className="w-full bg-surface-container-lowest border-2 border-green-500 rounded-xl p-2.5 text-xs text-ink font-medium focus:outline-none focus:border-green-700 shadow-[1px_1px_0_#DEB9B5]"
                >
                  {subjects.length === 0 && <option value="">Chưa có môn học phân công</option>}
                  {subjects.map((s) => (
                    <option key={s.subjectId} value={s.subjectId}>
                      {s.subjectCode} - {s.subjectName}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="font-bold text-ink block mb-1">Tên Chủ Đề</label>
                <input 
                  type="text" 
                  required
                  value={spaceForm.title}
                  onChange={(e) => setSpaceForm({ ...spaceForm, title: e.target.value })}
                  placeholder="Ví dụ: Vấn Đáp Kiến Trúc Phân Tầng Onion" 
                  className="w-full bg-surface-container-lowest border-2 border-green-500 rounded-xl p-2.5 text-xs text-ink placeholder:text-outline focus:outline-none focus:border-green-700 shadow-[1px_1px_0_#DEB9B5]" 
                />
              </div>

              <div>
                <label className="font-bold text-ink block mb-1">Mô Tả Chủ Đề</label>
                <textarea 
                  rows={3} 
                  value={spaceForm.description}
                  onChange={(e) => setSpaceForm({ ...spaceForm, description: e.target.value })}
                  placeholder="Mô tả các yêu cầu và khía cạnh cần AI Examiner đào sâu..." 
                  className="w-full bg-surface-container-lowest border-2 border-green-500 rounded-xl p-2.5 text-xs text-ink placeholder:text-outline focus:outline-none focus:border-green-700 shadow-[1px_1px_0_#DEB9B5]"
                />
              </div>

              <div className="pt-3 border-t border-cream flex items-center justify-end gap-2.5">
                <button 
                  type="button" 
                  onClick={() => setIsCreateModalOpen(false)} 
                  className="px-4 py-2 rounded-full border-2 border-outline-variant text-xs font-semibold text-on-surface-variant bg-paper shadow-[2px_2px_0_#DEB9B5] hover:bg-green-50 cursor-pointer"
                >
                  Hủy Bỏ
                </button>
                <button 
                  type="submit" 
                  disabled={isSubmitting || subjects.length === 0}
                  className="px-5 py-2 rounded-full bg-pink-500 hover:bg-pink-400 text-ink text-xs font-bold border-2 border-green-600 shadow-[3px_3px_0_#DEB9B5] hover:shadow-[1px_1px_0_#DEB9B5] hover:translate-x-[2px] hover:translate-y-[2px] active:shadow-none active:translate-x-[3px] active:translate-y-[3px] transition-all disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
                >
                  {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Tạo Chủ Đề Ngay ↗</span>
                </button>
              </div>
            </form>

          </div>
        </div>
      )}

    </div>
  );
}
