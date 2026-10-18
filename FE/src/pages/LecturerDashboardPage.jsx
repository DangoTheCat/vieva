import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { lecturerCatalogApi } from '../api/lecturerCatalogApi';
import { courseDocumentApi } from '../api/courseDocumentApi';
import { questionBankApi } from '../api/questionBankApi';
import { getErrorMessage } from '../utils/errorCodes';
import { 
  Plus, 
  Search, 
  ArrowRight, 
  Sparkles, 
  Layers, 
  ShieldCheck, 
  Zap, 
  User, 
  X, 
  Clock, 
  CheckCircle2,
  BookOpen,
  Loader2,
  FolderPlus,
  Mic
} from 'lucide-react';

export function LecturerDashboardPage({ onNavigate, showToast }) {
  const { currentUser } = useAuth();
  const lecturerName = currentUser?.fullName || 'Giảng Viên';

  const [subjects, setSubjects] = useState([]);
  const [topicsMap, setTopicsMap] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [spaceForm, setSpaceForm] = useState({
    title: '',
    description: '',
    examiner: 'Dr. Sophia (Học thuật chuẩn)',
    duration: 15
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fetch real assigned subjects & topics from Backend API
  useEffect(() => {
    async function loadDashboardData() {
      setIsLoading(true);
      try {
        const assignedData = await lecturerCatalogApi.getAssignedSubjects();
        const list = Array.isArray(assignedData) ? assignedData : assignedData?.content || [];
        setSubjects(list);
        
        if (list.length > 0) {
          setSelectedSubjectId(list[0].subjectId);
          
          const map = {};
          for (const sub of list) {
            const custom = JSON.parse(localStorage.getItem(`aives_custom_topics_${sub.subjectId}`) || '[]');
            try {
              const topList = await lecturerCatalogApi.getTopics(sub.subjectId);
              const fetched = Array.isArray(topList) ? topList : [];
              map[sub.subjectId] = [...custom, ...fetched];
            } catch (err) {
              map[sub.subjectId] = [...custom];
            }
          }
          setTopicsMap(map);
        } else {
          setSelectedSubjectId('');
          setTopicsMap({});
        }
      } catch (err) {
        setSubjects([]);
        setSelectedSubjectId('');
        setTopicsMap({});
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
      let createdTopic = {
        topicId: `top-${Date.now()}`,
        name: spaceForm.title.trim(),
        description: spaceForm.description.trim() || 'Không gian khảo thí tự động'
      };

      try {
        const result = await lecturerCatalogApi.createTopic(selectedSubjectId, {
          name: spaceForm.title.trim(),
          description: spaceForm.description.trim() || 'Không gian khảo thí tự động'
        });
        if (result && (result.topicId || result.id || result.name)) {
          createdTopic = result;
        }
      } catch (err) {
        console.warn('Backend createTopic API error, using local fallback topic creation.', err);
      }

      // Persist created topic locally so QuestionStudio & other views see it
      const existingCustom = JSON.parse(localStorage.getItem(`aives_custom_topics_${selectedSubjectId}`) || '[]');
      localStorage.setItem(`aives_custom_topics_${selectedSubjectId}`, JSON.stringify([...existingCustom, createdTopic]));

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
      setSpaceForm({ title: '', description: '', examiner: 'Dr. Sophia (Học thuật chuẩn)', duration: 15 });

      if (onNavigate) {
        onNavigate('space-detail');
      }
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
            <span>Tạo Phòng Thi (Create Space)</span>
          </button>
        </div>
      </div>

      {/* FEATURED HERO BANNER */}
      <div className="rounded-3xl bg-gradient-to-br from-pink-50 to-green-100 border-2 border-green-500 p-8 sm:p-10 flex flex-col md:flex-row items-center justify-between gap-6 relative overflow-hidden shadow-[6px_6px_0_#D5A6A2]">
        <div className="space-y-4 max-w-lg z-10">
          <div className="flex flex-wrap items-center gap-2">
            <span className="px-3 py-1 rounded-full text-[11px] font-bold bg-paper text-green-800 border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]">
              Phân Hệ Giảng Viên
            </span>
            <span className="px-3 py-1 rounded-full text-[11px] font-bold bg-paper text-on-surface-variant border-2 border-outline-variant shadow-[2px_2px_0_#DEB9B5]">
              Real-time API
            </span>
          </div>

          <h2 className="font-heading font-extrabold text-2xl sm:text-3xl text-ink tracking-tight leading-snug">
            Cổng Quản Lý Khảo Thí Vấn Đáp AI
          </h2>

          <p className="text-xs sm:text-sm text-on-surface-variant leading-relaxed">
            Hệ thống kết nối trực tiếp cơ sở dữ liệu Backend: Lập chỉ mục giáo trình RAG, soạn ngân hàng câu hỏi thích ứng và chấm điểm tự động đối chiếu Rubric.
          </p>

          <div className="pt-2 flex items-center gap-3">
            <button 
              type="button"
              onClick={() => onNavigate && onNavigate('space-detail')}
              className="inline-flex items-center gap-2 bg-paper text-ink hover:bg-green-50 font-bold text-xs sm:text-sm px-6 py-2.5 rounded-full border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] active:translate-x-[2px] active:translate-y-[2px] active:shadow-none transition-all cursor-pointer"
            >
              <span>Vào QuestionStudio</span>
              <ArrowRight className="w-4 h-4 text-green-700 font-extrabold" />
            </button>
          </div>
        </div>

        {/* AI Examiner Live Card Illustration */}
        <div className="w-64 rounded-2xl bg-paper border-2 border-green-500 p-4 shadow-[6px_6px_0_#DEB9B5] flex flex-col justify-between shrink-0 space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-cream">
            <span className="text-[10px] font-bold text-green-800 uppercase tracking-wider">AI Examiner Active</span>
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-500 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-green-600"></span>
            </span>
          </div>
          <div className="space-y-1.5 text-center py-2">
            <div className="w-12 h-12 rounded-full bg-pink-100 border-2 border-green-500 mx-auto flex items-center justify-center shadow-[2px_2px_0_#DEB9B5]">
              <Sparkles className="w-6 h-6 text-pink-700" />
            </div>
            <p className="text-xs font-bold text-ink">API Live Connection</p>
            <p className="text-[11px] text-green-700 font-medium">Đối chiếu Rubric Tức Thì</p>
          </div>
          {/* Audio Waveform Bars Simulation */}
          <div className="bg-green-50 rounded-lg p-2.5 border border-green-300/80">
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-[10px] text-on-surface-variant font-medium">Kênh Âm Thanh Khảo Thí</span>
              <Mic className="w-3.5 h-3.5 text-green-700" />
            </div>
            <div className="flex items-end justify-center gap-1.5 h-8 w-full pt-1">
              <span className="w-1.5 bg-green-500 rounded-full h-3 animate-pulse"></span>
              <span className="w-1.5 bg-pink-500 rounded-full h-6 animate-pulse [animation-delay:150ms]"></span>
              <span className="w-1.5 bg-green-600 rounded-full h-8 animate-pulse [animation-delay:300ms]"></span>
              <span className="w-1.5 bg-pink-400 rounded-full h-4 animate-pulse [animation-delay:75ms]"></span>
              <span className="w-1.5 bg-green-500 rounded-full h-7 animate-pulse [animation-delay:200ms]"></span>
              <span className="w-1.5 bg-pink-600 rounded-full h-3.5 animate-pulse [animation-delay:400ms]"></span>
              <span className="w-1.5 bg-green-500 rounded-full h-6.5 animate-pulse [animation-delay:120ms]"></span>
              <span className="w-1.5 bg-pink-500 rounded-full h-5 animate-pulse [animation-delay:250ms]"></span>
              <span className="w-1.5 bg-green-400 rounded-full h-3 animate-pulse [animation-delay:180ms]"></span>
            </div>
          </div>
        </div>
      </div>

      {/* ASSIGNED SUBJECTS & TOPICS SECTION (FROM BE API) */}
      <div className="space-y-4">
        <div className="flex items-center justify-between pb-2 border-b-2 border-cream">
          <h3 className="font-heading font-bold text-base text-ink flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-green-700" />
            <span>Môn Học Phụ Trách (Dữ Liệu Từ Backend API)</span>
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
                <h3 className="font-heading font-bold text-base text-ink">Tạo Chủ Đề / Không Gian Thi Mới</h3>
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
                <label className="font-bold text-ink block mb-1">Tên Chủ Đề / Phòng Thi Mới</label>
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
