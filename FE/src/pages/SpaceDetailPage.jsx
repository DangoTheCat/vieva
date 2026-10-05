import React, { useState, useEffect } from 'react';
import { lecturerCatalogApi } from '../api/lecturerCatalogApi';
import { questionBankApi } from '../api/questionBankApi';
import { getErrorMessage } from '../utils/errorCodes';
import { 
  ArrowLeft, 
  Globe, 
  Play, 
  RefreshCw, 
  Bookmark, 
  Share2, 
  User, 
  CheckCircle2, 
  X,
  FileText,
  Loader2,
  FolderPlus,
  BookOpen
} from 'lucide-react';

const MOCK_SUBJECTS = [
  { subjectId: 'SWD392', subjectCode: 'SWD392', subjectName: 'Phát Triển Phần Mềm Theo Kiến Trúc Đối Tượng' }
];

const MOCK_TOPICS = [
  { topicId: 'top-detail-01', name: 'Kiến Trúc Phân Tầng Onion & Clean Architecture' },
  { topicId: 'top-detail-02', name: 'Domain-Driven Design (DDD) & Aggregate Root' }
];

const MOCK_QUESTIONS = [
  {
    questionId: 'q-space-01',
    content: 'Phân tích nguyên lý đảo ngược phụ thuộc Dependency Inversion trong kiến trúc phần mềm.',
    bloomLevel: 'ANALYSIS'
  }
];

export function SpaceDetailPage({ onNavigate, showToast }) {
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [topics, setTopics] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const [activeSubTab, setActiveSubTab] = useState('about');
  const [isNodeModalOpen, setIsNodeModalOpen] = useState(false);
  const [modalNodeData, setModalNodeData] = useState({
    title: 'Prompt Giám Thị AI',
    type: 'MAIN',
    prompt: 'Em hãy giải thích lý do tại sao Onion Architecture đảo ngược chiều phụ thuộc của Data Access layer?',
    timeout: 120
  });

  // 1. Fetch assigned subjects from BE
  useEffect(() => {
    async function loadData() {
      setIsLoading(true);
      try {
        const data = await lecturerCatalogApi.getAssignedSubjects();
        const list = Array.isArray(data) ? data : data?.content || [];
        if (list.length > 0) {
          setSubjects(list);
          setSelectedSubjectId(list[0].subjectId);
        } else {
          setSubjects(MOCK_SUBJECTS);
          setSelectedSubjectId(MOCK_SUBJECTS[0].subjectId);
        }
      } catch (err) {
        setSubjects(MOCK_SUBJECTS);
        setSelectedSubjectId(MOCK_SUBJECTS[0].subjectId);
      } finally {
        setIsLoading(false);
      }
    }
    loadData();
  }, []);

  // 2. Fetch topics & questions for selected subject from BE
  useEffect(() => {
    if (!selectedSubjectId) return;

    async function loadSpaceData() {
      setIsLoading(true);
      try {
        const custom = JSON.parse(localStorage.getItem(`aives_custom_topics_${selectedSubjectId}`) || '[]');
        const [topList, qData] = await Promise.all([
          lecturerCatalogApi.getTopics(selectedSubjectId).catch(() => []),
          questionBankApi.searchBank(selectedSubjectId, { page: 0, size: 20 }).catch(() => ({ content: [] }))
        ]);

        const fetchedTopics = Array.isArray(topList) ? topList : [];
        const fetchedQuestions = qData?.content || (Array.isArray(qData) ? qData : []);

        const baseTopics = fetchedTopics.length > 0 ? fetchedTopics : MOCK_TOPICS;
        // Unique topics by topicId or name
        const combined = [...custom, ...baseTopics];
        const uniqueMap = new Map();
        combined.forEach(t => uniqueMap.set(t.topicId || t.name, t));
        setTopics(Array.from(uniqueMap.values()));
        setQuestions(fetchedQuestions.length > 0 ? fetchedQuestions : MOCK_QUESTIONS);
      } catch (err) {
        const custom = JSON.parse(localStorage.getItem(`aives_custom_topics_${selectedSubjectId}`) || '[]');
        const combined = [...custom, ...MOCK_TOPICS];
        const uniqueMap = new Map();
        combined.forEach(t => uniqueMap.set(t.topicId || t.name, t));
        setTopics(Array.from(uniqueMap.values()));
        setQuestions(MOCK_QUESTIONS);
      } finally {
        setIsLoading(false);
      }
    }

    loadSpaceData();
  }, [selectedSubjectId]);

  const selectedSubject = subjects.find(s => s.subjectId === selectedSubjectId);

  const handleRemixClick = () => {
    setModalNodeData({
      title: 'Remix Đề Thi: Tùy Chỉnh 3 Whys',
      type: 'MAIN',
      prompt: 'Phân tích tại sao tầng Domain Core không được phép phụ thuộc trực tiếp vào ORM?',
      timeout: 120
    });
    setIsNodeModalOpen(true);
  };

  const handleBookmark = () => {
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Bộ Sưu Tập',
        message: 'Đã lưu phòng thi vào danh sách cá nhân!'
      });
    }
  };

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
    }
    if (showToast) {
      showToast({
        type: 'info',
        title: 'Sao Chép Liên Kết',
        message: 'Đã sao chép liên kết phòng thi vấn đáp vào bộ nhớ tạm!'
      });
    }
  };

  return (
    <div className="space-y-6 animate-modal-entry max-w-4xl mx-auto text-slate-800 pb-10">
      {/* Breadcrumbs & Subject Selector */}
      <div className="flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
        <button 
          onClick={() => onNavigate && onNavigate('lecturer-dashboard')} 
          className="inline-flex items-center gap-1.5 hover:text-slate-900 font-semibold transition cursor-pointer"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Trang Tổng Quan</span>
        </button>

        <div className="flex items-center gap-2">
          {/* Select subject */}
          <select 
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
            className="bg-white border border-slate-200 rounded-xl p-1.5 px-3 text-xs font-bold text-slate-800"
          >
            {subjects.length === 0 && <option value="">Chưa có môn học phân công</option>}
            {subjects.map((s) => (
              <option key={s.subjectId} value={s.subjectId}>
                {s.subjectCode} - {s.subjectName}
              </option>
            ))}
          </select>

          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-100 text-slate-700 font-medium">
            <Globe className="w-3.5 h-3.5 text-blue-600" />
            <span>Không gian công khai (Public Space)</span>
          </div>
        </div>
      </div>

      {/* Space Hero Block */}
      <div className="flex flex-col md:flex-row items-start justify-between gap-6 pt-2">
        <div className="space-y-3 flex-1">
          <h1 className="font-heading font-extrabold text-2xl sm:text-3xl text-slate-900 tracking-tight">
            {selectedSubject ? `${selectedSubject.subjectCode} • ${selectedSubject.subjectName}` : 'QuestionStudio - Khảo Thí Vấn Đáp AI'}
          </h1>
          <div className="flex items-center gap-2 text-xs text-slate-600">
            <div className="w-5 h-5 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center text-[10px]">
              GV
            </div>
            <span>Bộ môn SE • FPT University</span>
            <span>•</span>
            <span className="font-bold text-blue-700">{topics.length} Chủ đề khảo thí</span>
          </div>
          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed pt-1">
            Phòng thi vấn đáp hướng dẫn sinh viên đào sâu vào bản chất môn học thông qua phương pháp "3 Lần Tại Sao" (The 3 Whys), giúp phát hiện kiến thức cốt lõi và tư duy thiết kế hệ thống.
          </p>
        </div>

        {/* Thumbnail Art */}
        <div className="w-36 h-28 rounded-2xl bg-gradient-to-tr from-blue-100 to-sky-200 border border-slate-200 flex items-center justify-center text-4xl shadow-2xs shrink-0">
          🧅
        </div>
      </div>

      {/* Action Buttons Bar */}
      <div className="flex flex-wrap items-center gap-3 pt-2">
        <button 
          onClick={() => onNavigate && onNavigate('exam-room')} 
          className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs sm:text-sm px-6 py-2.5 rounded-full shadow-2xs active:scale-95 transition cursor-pointer"
        >
          <Play className="w-4 h-4 fill-current" />
          <span>Preview &amp; Vào Thi Ngay</span>
        </button>

        <button 
          onClick={handleRemixClick} 
          className="inline-flex items-center gap-1.5 bg-white border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium text-xs sm:text-sm px-4 py-2.5 rounded-full shadow-2xs transition cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5 text-purple-600" />
          <span>Remix Đề Thi</span>
        </button>

        <button 
          onClick={handleBookmark} 
          className="inline-flex items-center gap-1.5 bg-white border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium text-xs sm:text-sm px-4 py-2.5 rounded-full shadow-2xs transition cursor-pointer"
        >
          <Bookmark className="w-3.5 h-3.5 text-amber-500" />
          <span>Lưu Bộ Sưu Tập</span>
        </button>

        <button 
          onClick={handleShare} 
          className="inline-flex items-center gap-1.5 bg-white border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium text-xs sm:text-sm px-4 py-2.5 rounded-full shadow-2xs transition cursor-pointer"
        >
          <Share2 className="w-3.5 h-3.5 text-emerald-600" />
          <span>Chia Sẻ</span>
        </button>
      </div>

      {/* Sub-Navigation Tabs */}
      <div className="flex items-center gap-8 border-b border-slate-200 text-xs sm:text-sm font-semibold pt-4">
        <button 
          onClick={() => setActiveSubTab('about')}
          className={`pb-3 border-b-2 transition cursor-pointer ${activeSubTab === 'about' ? 'border-slate-900 text-slate-900 font-bold' : 'border-transparent text-slate-500 hover:text-slate-800'}`}
        >
          Chủ Đề &amp; Tiến Trình 3 Whys ({topics.length})
        </button>
        <button 
          onClick={() => {
            setActiveSubTab('prompt');
            setIsNodeModalOpen(true);
          }}
          className={`pb-3 border-b-2 transition cursor-pointer ${activeSubTab === 'prompt' ? 'border-slate-900 text-slate-900 font-bold' : 'border-transparent text-slate-500 hover:text-slate-800'}`}
        >
          Prompt Giám Thị (Prompt)
        </button>
        <button 
          onClick={() => setActiveSubTab('questions')}
          className={`pb-3 border-b-2 transition cursor-pointer ${activeSubTab === 'questions' ? 'border-slate-900 text-slate-900 font-bold' : 'border-transparent text-slate-500 hover:text-slate-800'}`}
        >
          Câu Hỏi Từ Backend ({questions.length})
        </button>
      </div>

      {/* Main Content Card */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-6 sm:p-8 space-y-6 shadow-2xs">
        {isLoading ? (
          <div className="p-8 text-center space-y-3">
            <Loader2 className="w-6 h-6 text-blue-600 animate-spin mx-auto" />
            <p className="text-xs text-slate-500">Đang nạp dữ liệu từ Backend...</p>
          </div>
        ) : activeSubTab === 'questions' ? (
          <div className="space-y-4">
            <h3 className="font-heading font-bold text-sm text-slate-900 uppercase tracking-wider">
              Danh Sách Câu Hỏi Trong Ngân Hàng ({questions.length})
            </h3>
            {questions.length === 0 ? (
              <p className="text-xs text-slate-500 italic">Chưa có câu hỏi nào trong ngân hàng môn học này.</p>
            ) : (
              <div className="divide-y divide-slate-100 text-xs">
                {questions.map((q, idx) => (
                  <div key={q.questionId || idx} className="py-3 space-y-1">
                    <div className="flex items-center justify-between font-bold text-slate-900">
                      <span>#{q.code || `Q${idx + 1}`} • {q.bloomLevel || 'UNDERSTAND'}</span>
                      <span className="text-blue-600 text-[11px]">Active Version</span>
                    </div>
                    <p className="text-slate-700">{q.content}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className="space-y-6">
            <h3 className="font-heading font-bold text-sm text-slate-900 uppercase tracking-wider">
              Chủ Đề &amp; Tiến Trình 3 Lượt Hỏi Vấn Đáp (The 3 Whys Agenda)
            </h3>

            {topics.length === 0 ? (
              <div className="p-6 text-center bg-slate-50 rounded-2xl border border-slate-200 space-y-2">
                <FolderPlus className="w-6 h-6 text-slate-300 mx-auto" />
                <p className="text-xs text-slate-600 font-bold">Chưa có chủ đề nào trong đề cương Backend.</p>
                <p className="text-[11px] text-slate-400">Tạo chủ đề mới tại Trang Tổng Quan hoặc Ngân Hàng Câu Hỏi.</p>
              </div>
            ) : (
              <div className="space-y-4 text-xs sm:text-sm text-slate-700">
                {topics.map((t, idx) => (
                  <div key={t.topicId || t.id || idx} className="flex items-start gap-3.5 p-3.5 bg-slate-50 border border-slate-200/80 rounded-2xl">
                    <span className="w-6 h-6 rounded-full bg-slate-900 text-white font-bold text-xs flex items-center justify-center shrink-0 mt-0.5 font-mono">
                      {idx + 1}
                    </span>
                    <div className="space-y-1">
                      <h4 className="font-bold text-slate-900">{t.name}</h4>
                      <p className="text-slate-600 leading-relaxed text-xs">{t.description || 'Chủ đề khảo thí thuộc đề cương môn học'}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 text-xs text-slate-600 italic">
              "Hệ thống tự động chấm điểm theo 4 tiêu chí Rubric ngay khi hoàn thành các lượt hỏi và đồng bộ sang Module Grading."
            </div>
          </div>
        )}
      </div>

      {/* NODE CONFIG MODAL */}
      {isNodeModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-heading font-bold text-base text-slate-900">{modalNodeData.title}</h3>
              <button onClick={() => setIsNodeModalOpen(false)} className="text-slate-400 hover:text-slate-700 p-1 cursor-pointer">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3.5 text-xs">
              <div>
                <label className="font-bold text-slate-800 block mb-1">Loại Lượt Hỏi</label>
                <select 
                  value={modalNodeData.type}
                  onChange={(e) => setModalNodeData({ ...modalNodeData, type: e.target.value })}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                >
                  <option value="MAIN">Hỏi Chính (MAIN Turn)</option>
                  <option value="PROBING">Hỏi Xoáy Thích Ứng (Adaptive Probing)</option>
                  <option value="OUTBOX">Bàn Giao Kết Quả (Outbox Handoff)</option>
                </select>
              </div>

              <div>
                <label className="font-bold text-slate-800 block mb-1">Nội Dung Prompt Giám Thị AI</label>
                <textarea 
                  rows={3} 
                  value={modalNodeData.prompt}
                  onChange={(e) => setModalNodeData({ ...modalNodeData, prompt: e.target.value })}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>
            </div>

            <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2.5">
              <button 
                type="button"
                onClick={() => setIsNodeModalOpen(false)} 
                className="px-4 py-2 rounded-full border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 cursor-pointer"
              >
                Hủy
              </button>
              <button 
                type="button"
                onClick={() => {
                  setIsNodeModalOpen(false);
                  if (showToast) showToast({ type: 'success', message: 'Đã lưu cấu hình prompt giám thị AI!' });
                }} 
                className="px-5 py-2 rounded-full bg-slate-900 text-white text-xs font-bold active:scale-95 transition cursor-pointer"
              >
                Lưu Thay Đổi
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
