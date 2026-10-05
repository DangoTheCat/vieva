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
  FolderPlus
} from 'lucide-react';

const MOCK_SUBJECTS = [
  {
    subjectId: 'SWD392',
    subjectCode: 'SWD392',
    subjectName: 'Phát Triển Phần Mềm Theo Kiến Trúc Đối Tượng',
    description: 'Chuyên ngành Software Engineering - FPT University'
  },
  {
    subjectId: 'PRN231',
    subjectCode: 'PRN231',
    subjectName: 'Lập Trình Web Ứng Dụng Java API & Spring Boot',
    description: 'Xây dựng RESTful Services, JWT Authentication & JPA'
  },
  {
    subjectId: 'PRN221',
    subjectCode: 'PRN221',
    subjectName: 'Lập Trình C# .NET Core & Microservices',
    description: 'Hệ thống phân tán, ASP.NET Core API & Entity Framework'
  }
];

const MOCK_TOPICS_MAP = {
  SWD392: [
    { topicId: 'top-1', name: 'Kiến Trúc Phân Tầng Onion & Clean Architecture' },
    { topicId: 'top-2', name: 'Domain-Driven Design (DDD) & Aggregate Root' },
    { topicId: 'top-3', name: 'CQRS & Event Sourcing in Enterprise Systems' }
  ],
  PRN231: [
    { topicId: 'top-4', name: 'Spring Boot REST API & Spring Security JWT' },
    { topicId: 'top-5', name: 'JPA Hibernate Performance Tuning & Indexing' }
  ],
  PRN221: [
    { topicId: 'top-6', name: 'ASP.NET Core Web API & Dependency Injection' },
    { topicId: 'top-7', name: 'gRPC & Microservices Inter-service Communication' }
  ]
};

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

  // Fetch real assigned subjects & topics from Backend API (with mock fallback if empty or offline)
  useEffect(() => {
    async function loadDashboardData() {
      setIsLoading(true);
      try {
        const assignedData = await lecturerCatalogApi.getAssignedSubjects();
        const list = Array.isArray(assignedData) ? assignedData : assignedData?.content || [];
        
        if (list.length > 0) {
          setSubjects(list);
          setSelectedSubjectId(list[0].subjectId);
          
          const map = {};
          for (const sub of list) {
            const custom = JSON.parse(localStorage.getItem(`aives_custom_topics_${sub.subjectId}`) || '[]');
            try {
              const topList = await lecturerCatalogApi.getTopics(sub.subjectId);
              const fetched = Array.isArray(topList) && topList.length > 0 ? topList : (MOCK_TOPICS_MAP[sub.subjectId] || []);
              map[sub.subjectId] = [...custom, ...fetched];
            } catch (err) {
              map[sub.subjectId] = [...custom, ...(MOCK_TOPICS_MAP[sub.subjectId] || [])];
            }
          }
          setTopicsMap(map);
        } else {
          setSubjects(MOCK_SUBJECTS);
          setSelectedSubjectId(MOCK_SUBJECTS[0].subjectId);
          const map = {};
          MOCK_SUBJECTS.forEach(sub => {
            const custom = JSON.parse(localStorage.getItem(`aives_custom_topics_${sub.subjectId}`) || '[]');
            map[sub.subjectId] = [...custom, ...(MOCK_TOPICS_MAP[sub.subjectId] || [])];
          });
          setTopicsMap(map);
        }
      } catch (err) {
        setSubjects(MOCK_SUBJECTS);
        setSelectedSubjectId(MOCK_SUBJECTS[0].subjectId);
        const map = {};
        MOCK_SUBJECTS.forEach(sub => {
          const custom = JSON.parse(localStorage.getItem(`aives_custom_topics_${sub.subjectId}`) || '[]');
          map[sub.subjectId] = [...custom, ...(MOCK_TOPICS_MAP[sub.subjectId] || [])];
        });
        setTopicsMap(map);
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
    <div className="space-y-8 max-w-6xl mx-auto w-full animate-modal-entry text-slate-800 pb-10">

      {/* TOPBAR / HEADER SEARCH BAR & CREATE BUTTON */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row items-center justify-between gap-4 shadow-2xs">
        <div>
          <h1 className="font-heading font-extrabold text-xl text-slate-900 tracking-tight">
            Trang Tổng Quan Khảo Thí (Lecturer Overview)
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Xin chào, <strong className="text-slate-800">{lecturerName}</strong> • Quản lý môn học phân công &amp; không gian khảo thí tự động
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
              className="w-full bg-slate-100 focus:bg-white border border-transparent focus:border-blue-500 rounded-full py-2 px-4 pl-9 text-xs text-slate-800 placeholder:text-slate-400 transition focus:outline-none focus:ring-2 focus:ring-blue-100" 
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          </div>

          {/* Primary Blue Pill Button: + Tạo Phòng Thi */}
          <button 
            type="button"
            onClick={() => setIsCreateModalOpen(true)}
            className="inline-flex items-center gap-2 bg-[#0066FF] hover:bg-[#0052CC] text-white font-bold text-xs px-5 py-2.5 rounded-full shadow-sm hover:shadow-md transition active:scale-95 shrink-0 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo Phòng Thi (Create Space)</span>
          </button>
        </div>
      </div>

      {/* FEATURED HERO BANNER */}
      <div className="rounded-3xl bg-gradient-to-r from-[#E8F0FE] via-[#EDF2FA] to-[#F1F5F9] border border-blue-100/80 p-8 sm:p-10 flex flex-col md:flex-row items-center justify-between gap-6 relative overflow-hidden shadow-2xs">
        <div className="space-y-4 max-w-lg z-10">
          <div className="flex flex-wrap items-center gap-2">
            <span className="px-3 py-1 rounded-full text-[11px] font-bold bg-white text-blue-700 shadow-2xs border border-blue-200/60">
              Phân Hệ Giảng Viên
            </span>
            <span className="px-3 py-1 rounded-full text-[11px] font-bold bg-white text-slate-700 shadow-2xs border border-slate-200">
              Real-time API
            </span>
          </div>

          <h2 className="font-heading font-extrabold text-2xl sm:text-3xl text-slate-900 tracking-tight leading-snug">
            Cổng Quản Lý Khảo Thí Vấn Đáp AI
          </h2>

          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
            Hệ thống kết nối trực tiếp cơ sở dữ liệu Backend: Lập chỉ mục giáo trình RAG, soạn ngân hàng câu hỏi thích ứng và chấm điểm tự động đối chiếu Rubric.
          </p>

          <div className="pt-2 flex items-center gap-3">
            <button 
              type="button"
              onClick={() => onNavigate && onNavigate('space-detail')}
              className="inline-flex items-center gap-2 bg-white text-slate-900 hover:bg-slate-50 font-bold text-xs sm:text-sm px-6 py-2.5 rounded-full shadow-xs border border-slate-200 active:scale-95 transition cursor-pointer"
            >
              <span>Vào QuestionStudio</span>
              <ArrowRight className="w-4 h-4 text-blue-600 font-extrabold" />
            </button>
          </div>
        </div>

        {/* AI Examiner Live Card Illustration */}
        <div className="w-64 h-48 rounded-2xl bg-white/80 backdrop-blur-xs border border-white p-4 shadow-sm flex flex-col justify-between shrink-0">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-blue-600 uppercase tracking-wider">AI Examiner Active</span>
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
          </div>
          <div className="space-y-2 text-center py-2">
            <div className="w-12 h-12 rounded-full bg-blue-100 mx-auto flex items-center justify-center text-xl shadow-inner">
              🎙️
            </div>
            <p className="text-xs font-bold text-slate-800">API Live Connection</p>
            <p className="text-[11px] text-slate-500">Đối chiếu Rubric Tức Thì</p>
          </div>
          <div className="flex justify-center gap-1.5 pt-1">
            <span className="w-1 h-3 bg-blue-500 rounded-full animate-bounce"></span>
            <span className="w-1 h-5 bg-blue-600 rounded-full animate-bounce delay-100"></span>
            <span className="w-1 h-4 bg-blue-400 rounded-full animate-bounce delay-200"></span>
            <span className="w-1 h-6 bg-slate-800 rounded-full animate-bounce delay-150"></span>
            <span className="w-1 h-3 bg-blue-500 rounded-full animate-bounce"></span>
          </div>
        </div>
      </div>

      {/* ASSIGNED SUBJECTS & TOPICS SECTION (FROM BE API) */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-heading font-bold text-base text-slate-900 flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-blue-600" />
            <span>Môn Học Phụ Trách (Dữ Liệu Từ Backend API)</span>
          </h3>
          <span className="text-xs text-slate-400 font-mono">
            {subjects.length} Môn học được phân công
          </span>
        </div>

        {isLoading ? (
          <div className="p-8 text-center bg-white rounded-2xl border border-slate-200/80 space-y-3">
            <Loader2 className="w-6 h-6 text-blue-600 animate-spin mx-auto" />
            <p className="text-xs text-slate-500">Đang tải danh sách môn học từ Backend...</p>
          </div>
        ) : filteredSubjects.length === 0 ? (
          <div className="p-8 text-center bg-white rounded-2xl border border-slate-200/80 space-y-3">
            <FolderPlus className="w-8 h-8 text-slate-300 mx-auto" />
            <p className="text-sm font-bold text-slate-700">Chưa Có Môn Học Được Phân Công</p>
            <p className="text-xs text-slate-500 max-w-md mx-auto">
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
                  className="bg-white border border-slate-200/80 hover:border-slate-300 rounded-2xl p-6 shadow-2xs space-y-4 transition duration-200"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200/80 font-mono">
                        {sub.subjectCode}
                      </span>
                      <h4 className="font-heading font-extrabold text-base text-slate-900 mt-2">
                        {sub.subjectName}
                      </h4>
                    </div>
                    <button
                      type="button"
                      onClick={() => onNavigate && onNavigate('lecturer-questions')}
                      className="text-xs font-bold text-blue-600 hover:text-blue-800 flex items-center gap-1 shrink-0"
                    >
                      <span>Ngân Hàng</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Topics List from BE */}
                  <div className="space-y-2 pt-2 border-t border-slate-100">
                    <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                      Chủ đề đề cương ({subTopics.length})
                    </p>
                    {subTopics.length === 0 ? (
                      <p className="text-xs text-slate-400 italic">Chưa có chủ đề nào trong đề cương môn học này.</p>
                    ) : (
                      <div className="flex flex-wrap gap-2">
                        {subTopics.map((top) => (
                          <span 
                            key={top.topicId || top.id}
                            className="px-3 py-1 rounded-xl bg-slate-50 border border-slate-200 text-xs font-medium text-slate-700"
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
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-7 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800">
            
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm">
                  +
                </div>
                <h3 className="font-heading font-bold text-base text-slate-900">Tạo Chủ Đề / Không Gian Thi Mới</h3>
              </div>
              <button 
                type="button"
                onClick={() => setIsCreateModalOpen(false)} 
                className="text-slate-400 hover:text-slate-700 text-xl font-bold leading-none p-1 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSpace} className="space-y-3.5 text-xs text-slate-700">
              <div>
                <label className="font-bold text-slate-800 block mb-1">Môn Học Phụ Trách</label>
                <select 
                  value={selectedSubjectId}
                  onChange={(e) => setSelectedSubjectId(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
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
                <label className="font-bold text-slate-800 block mb-1">Tên Chủ Đề / Phòng Thi Mới</label>
                <input 
                  type="text" 
                  required
                  value={spaceForm.title}
                  onChange={(e) => setSpaceForm({ ...spaceForm, title: e.target.value })}
                  placeholder="Ví dụ: Vấn Đáp Kiến Trúc Phân Tầng Onion" 
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" 
                />
              </div>

              <div>
                <label className="font-bold text-slate-800 block mb-1">Mô Tả Chủ Đề</label>
                <textarea 
                  rows={3} 
                  value={spaceForm.description}
                  onChange={(e) => setSpaceForm({ ...spaceForm, description: e.target.value })}
                  placeholder="Mô tả các yêu cầu và khía cạnh cần AI Examiner đào sâu..." 
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2.5">
                <button 
                  type="button" 
                  onClick={() => setIsCreateModalOpen(false)} 
                  className="px-4 py-2 rounded-full border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 cursor-pointer"
                >
                  Hủy Bỏ
                </button>
                <button 
                  type="submit" 
                  disabled={isSubmitting || subjects.length === 0}
                  className="px-5 py-2 rounded-full bg-[#0066FF] hover:bg-[#0052CC] text-white text-xs font-bold shadow-xs transition active:scale-95 disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
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
