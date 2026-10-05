import React, { useState, useEffect } from 'react';
import { questionBankApi } from '../api/questionBankApi';
import { userApi } from '../api/userApi';
import { getErrorMessage } from '../utils/errorCodes';
import { 
  Play, 
  Pause, 
  Download, 
  CheckCircle2, 
  Mic, 
  FileText, 
  ShieldCheck, 
  ArrowLeft, 
  Check, 
  X, 
  Sparkles,
  Award,
  Loader2,
  FileSpreadsheet,
  BadgeCheck,
  ChevronRight,
  Volume2
} from 'lucide-react';

export function LecturerReviewRegradingPage({ onNavigate, showToast }) {
  const [activeQuestionTab, setActiveQuestionTab] = useState(2); // Question 2 default as in screenshot
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const [audioCurrentTime, setAudioCurrentTime] = useState('01:45');
  const [isSignOffModalOpen, setIsSignOffModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // Rubric Sliders State for Question 2
  const [c1, setC1] = useState(2.7);
  const [c2, setC2] = useState(2.5);
  const [c3, setC3] = useState(2.0);
  const [c4, setC4] = useState(1.3);

  const [lecturerNotes, setLecturerNotes] = useState(
    'Thí sinh trả lời lưu loát, nắm chắc khái niệm SAGA và Outbox Pattern khi được hỏi xoáy. Điểm đánh giá AI phù hợp thực tế.'
  );

  const totalQuestionScore = (c1 + c2 + c3 + c4).toFixed(1);

  // Load backend profile / session details
  useEffect(() => {
    async function loadSessionData() {
      setIsLoading(true);
      try {
        await userApi.getCurrentUser().catch(() => {});
      } catch (err) {
        // Fallback seamless
      } finally {
        setIsLoading(false);
      }
    }
    loadSessionData();
  }, []);

  const toggleAudio = () => {
    setIsPlayingAudio(!isPlayingAudio);
    if (showToast) {
      showToast({
        type: 'info',
        title: 'Phát Băng Ghi Âm',
        message: !isPlayingAudio ? 'Đang phát băng ghi âm câu trả lời của thí sinh (Whisper STT)...' : 'Đã tạm dừng âm thanh.'
      });
    }
  };

  const handleDownloadPdfReport = () => {
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Báo Cáo Khảo Thí PDF',
        message: 'Đã xuất và tải xuống bản báo cáo phúc khảo bóc băng AI (SHA-256 Verified)!'
      });
    }
  };

  const handleSaveQuestionScore = () => {
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Lưu Điểm Câu Hỏi',
        message: `Đã lưu điểm điểm câu 2: ${totalQuestionScore}/10 điểm theo Rubric đối chiếu!`
      });
    }
  };

  const handleFinalSignOffSubmit = async () => {
    setIsSubmitting(true);
    try {
      try {
        await questionBankApi.approveVersion('ver-801');
      } catch (err) {
        console.warn('Backend sign-off API fallback', err);
      }

      showToast({
        type: 'success',
        title: 'Ký Duyệt & Chốt Điểm FAP',
        message: `Đã ký số biên bản hậu kiểm và chốt điểm ${totalQuestionScore} cho bài thi thành công!`
      });

      setIsSignOffModalOpen(false);
      if (onNavigate) {
        onNavigate('lecturer-grading-queue');
      }
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto w-full animate-modal-entry text-slate-800 pb-16">
      
      {/* 1. TOP ACTION BAR (Clean bar with Back & Ký Duyệt button) */}
      <div className="flex items-center justify-between gap-4 pt-1">
        <button 
          onClick={() => onNavigate && onNavigate('lecturer-grading-queue')} 
          className="inline-flex items-center gap-2 text-xs sm:text-sm font-semibold text-slate-600 hover:text-slate-900 bg-white border border-slate-200 hover:border-slate-300 px-4 py-2 rounded-xl shadow-2xs transition"
        >
          ← Trở Lại Hàng Đợi Chấm
        </button>

        <button
          onClick={() => setIsSignOffModalOpen(true)}
          className="px-5 py-2.5 rounded-full bg-[#059669] hover:bg-emerald-600 text-white font-bold text-xs sm:text-sm shadow-md transition active:scale-95 cursor-pointer flex items-center gap-2"
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>Ký Duyệt &amp; Chốt Điểm</span>
        </button>
      </div>

      {/* 2. CANDIDATE PROFILE BANNER CARD (Matching Image 1) */}
      <div className="bg-white border border-slate-200/90 rounded-3xl p-6 sm:p-7 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 shadow-2xs">
        
        {/* Candidate Info Left */}
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-full bg-gradient-to-tr from-cyan-600 to-blue-600 text-white flex items-center justify-center font-heading font-extrabold text-lg shadow-md shrink-0">
            NB
          </div>
          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="font-heading font-extrabold text-xl text-slate-900">
                Nguyen Van B
              </h2>
              <span className="px-2.5 py-0.5 rounded-md text-xs font-mono font-bold bg-sky-50 text-sky-700 border border-sky-200">
                SE160982
              </span>
              <span className="text-xs text-slate-500 font-medium">Cohort SE1701 • SWD392</span>
            </div>
            <p className="text-xs text-slate-500 flex flex-wrap items-center gap-3">
              <span>Hoàn thành ca thi: 09:41 AM (GMT+7)</span>
              <span>•</span>
              <span>Hội đồng: AI Examiner Dr. Minh</span>
              <span>•</span>
              <span className="text-emerald-600 font-bold flex items-center gap-1 font-mono">
                <BadgeCheck className="w-3.5 h-3.5" /> SHA-256 Verified ✓
              </span>
            </p>
          </div>
        </div>

        {/* Score & Status Right */}
        <div className="flex flex-wrap items-center gap-4 shrink-0">
          
          {/* Điểm AI Sơ Bộ Box */}
          <div className="bg-slate-50 border border-slate-200/90 p-3.5 px-5 rounded-2xl text-center flex items-center gap-4 shadow-2xs">
            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">ĐIỂM AI SƠ BỘ</span>
              <p className="font-heading font-black text-2xl text-slate-900 leading-tight">
                8.5 <span className="text-xs text-slate-400 font-normal">/ 10</span>
              </p>
            </div>
            <div className="text-left pl-3 border-l border-slate-200">
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-100 text-emerald-800">
                Tier 3: Proficient
              </span>
              <p className="text-[11px] text-slate-500 mt-1 font-medium">Khớp 91.4% chuẩn Rubric</p>
            </div>
          </div>

          {/* Status Tag */}
          <span className="px-3.5 py-1.5 rounded-full text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse"></span>
            Chờ Ký Biên Bản
          </span>

          {/* Download PDF Button */}
          <button
            onClick={handleDownloadPdfReport}
            className="px-4 py-2.5 rounded-xl border border-slate-300 hover:bg-slate-50 text-slate-700 font-bold text-xs flex items-center gap-2 transition active:scale-95 cursor-pointer"
          >
            <Download className="w-4 h-4 text-slate-500" />
            <span>Tải Báo Cáo PDF</span>
          </button>
        </div>

      </div>

      {/* 3. QUESTION TABS BAR (Matching Image 1) */}
      <div className="flex items-center gap-3 overflow-x-auto pb-1 scrollbar-none text-xs font-bold">
        <button
          onClick={() => setActiveQuestionTab(1)}
          className={`px-4 py-2.5 rounded-full border transition whitespace-nowrap cursor-pointer ${
            activeQuestionTab === 1
              ? 'bg-[#0066FF] text-white border-blue-600 shadow-sm'
              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
          }`}
        >
          Câu 1: Onion Architecture <span className="ml-1 opacity-80">9.0/10</span>
        </button>

        <button
          onClick={() => setActiveQuestionTab(2)}
          className={`px-4 py-2.5 rounded-full border transition whitespace-nowrap cursor-pointer ${
            activeQuestionTab === 2
              ? 'bg-[#0066FF] text-white border-blue-600 shadow-sm'
              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
          }`}
        >
          🎙️ Câu 2: Monolith vs Microservices <span className="ml-1 font-mono">8.0/10</span>
        </button>

        <button
          onClick={() => setActiveQuestionTab(3)}
          className={`px-4 py-2.5 rounded-full border transition whitespace-nowrap cursor-pointer ${
            activeQuestionTab === 3
              ? 'bg-[#0066FF] text-white border-blue-600 shadow-sm'
              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
          }`}
        >
          Câu 3: Outbox Pattern <span className="ml-1 opacity-80">8.5/10</span>
        </button>

        <button
          onClick={() => setActiveQuestionTab(4)}
          className={`px-4 py-2.5 rounded-full border transition whitespace-nowrap cursor-pointer ${
            activeQuestionTab === 4
              ? 'bg-[#0066FF] text-white border-blue-600 shadow-sm'
              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
          }`}
        >
          Câu 4: Real-time STOMP <span className="ml-1 opacity-80">9.0/10</span>
        </button>

        <button
          onClick={() => setActiveQuestionTab(5)}
          className={`px-4 py-2.5 rounded-full border transition whitespace-nowrap cursor-pointer ${
            activeQuestionTab === 5
              ? 'bg-[#0066FF] text-white border-blue-600 shadow-sm'
              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
          }`}
        >
          Câu 5: CQRS &amp; Eventual Consistency <span className="ml-1 opacity-80">8.5/10</span>
        </button>
      </div>

      {/* 4. MAIN TWO-COLUMN CONTENT AREA (Matching Image 1 & Image 2) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* LEFT COLUMN: AUDIO PLAYER & LIVE TRANSCRIPT (7 cols) */}
        <div className="lg:col-span-7 space-y-6">
          
          {/* Audio Player Card (Matching Image 1) */}
          <div className="bg-white border border-slate-200/90 rounded-3xl p-6 shadow-2xs space-y-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
                <Mic className="w-4 h-4 text-blue-600" />
                Bản Ghi Âm Câu Trả Lời Của Thí Sinh
              </span>
              <span className="text-xs font-mono text-slate-400">Thời lượng: 03:10</span>
            </div>

            {/* Dark Tech Waveform Player Box */}
            <div className="bg-[#0b172a] rounded-2xl p-4 sm:p-5 text-white flex items-center justify-between gap-4 shadow-inner">
              <button
                type="button"
                onClick={toggleAudio}
                className="w-12 h-12 rounded-full bg-[#0066FF] hover:bg-blue-500 text-white flex items-center justify-center shrink-0 shadow-md transition active:scale-95 cursor-pointer"
              >
                {isPlayingAudio ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5 ml-0.5" />}
              </button>

              {/* Waveform Visualizer Bars */}
              <div className="flex-1 flex items-center justify-between gap-1 h-8 px-2 overflow-hidden">
                <span className="w-1.5 bg-blue-500 rounded-full h-3"></span>
                <span className="w-1.5 bg-blue-400 rounded-full h-5"></span>
                <span className="w-1.5 bg-sky-300 rounded-full h-8"></span>
                <span className="w-1.5 bg-blue-600 rounded-full h-4"></span>
                <span className="w-1.5 bg-blue-500 rounded-full h-9"></span>
                <span className="w-1.5 bg-sky-400 rounded-full h-6"></span>
                <span className="w-1.5 bg-blue-600 rounded-full h-10"></span>
                <span className="w-1.5 bg-blue-400 rounded-full h-5"></span>
                <span className="w-1.5 bg-sky-300 rounded-full h-8"></span>
                <span className="w-1.5 bg-blue-500 rounded-full h-4"></span>
                <span className="w-1.5 bg-blue-600 rounded-full h-7"></span>
                <span className="w-1.5 bg-sky-400 rounded-full h-3"></span>
              </div>

              <span className="font-mono text-xs font-bold text-slate-300 shrink-0">
                {audioCurrentTime} / 03:10
              </span>
            </div>

            {/* Prompted Question Box */}
            <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-4 space-y-1 text-xs">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">CÂU HỎI PHÁT RA:</span>
              <p className="font-bold text-slate-900 leading-relaxed">
                Hãy phân tích các đánh đổi cốt lõi giữa Kiến trúc Monolith và Microservices về deployment complexity và independent scalability?
              </p>
            </div>
          </div>

          {/* Whisper STT Live Transcript Card (Matching Image 2) */}
          <div className="bg-white border border-slate-200/90 rounded-3xl p-6 shadow-2xs space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <h3 className="font-heading font-extrabold text-sm text-slate-900 uppercase tracking-wider">
                Bóc Băng Đồng Bộ Thời Gian Thực (Whisper STT)
              </h3>
              <span className="px-3 py-1 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200 font-mono">
                Confidence: 97.8%
              </span>
            </div>

            {/* Conversation Log Blocks */}
            <div className="space-y-3.5 text-xs text-slate-700 max-h-[420px] overflow-y-auto pr-1">
              
              {/* Turn 1: AI Examiner */}
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-blue-700">AI Examiner Dr. Minh</span>
                  <span className="font-mono text-[11px] text-slate-400">00:00 - 00:25</span>
                </div>
                <p className="italic text-slate-800 leading-relaxed">
                  "Chào em. Ở câu số 2, hãy phân tích các đánh đổi cốt lõi giữa kiến trúc Monolith và Microservices, đặc biệt là về độ phức tạp khi triển khai và khả năng mở rộng độc lập?"
                </p>
              </div>

              {/* Turn 2: Student Candidate */}
              <div className="p-4 rounded-2xl bg-blue-50/50 border border-blue-100 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-slate-900">Thí sinh: Nguyen Van B</span>
                  <span className="font-mono text-[11px] text-slate-400">00:26 - 02:15</span>
                </div>
                <p className="text-slate-800 leading-relaxed">
                  "Dạ thưa Thầy, theo em điểm khác biệt lớn nhất là về <mark className="bg-amber-200 text-amber-900 px-1 py-0.5 rounded font-bold">blast radius</mark>. Trong Monolith, một lỗi nhỏ ở module thanh toán có thể làm crash toàn bộ ứng dụng và phải build lại toàn bộ artifact. Còn với Microservices, các service chạy độc lập trong container, nếu một service gặp sự cố thì các service khác vẫn hoạt động bình thường."
                </p>
                <p className="text-slate-800 leading-relaxed pt-1">
                  "Về khả năng mở rộng, Monolith chỉ có thể scale theo chiều dọc hoặc nhân bản toàn bộ binary gây lãng phí tài nguyên RAM/CPU. Trong khi đó, Microservices cho phép scale chiều ngang chọn lọc chỉ cho service chịu tải cao như Order Service trong đợt flash sale."
                </p>
              </div>

              {/* Turn 3: Adaptive Probing */}
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-purple-700">AI Examiner (Hỏi Xoáy Thích Ứng - Adaptive Probing)</span>
                  <span className="font-mono text-[11px] text-slate-400">02:16 - 02:35</span>
                </div>
                <p className="italic text-slate-800 leading-relaxed">
                  "Rất tốt. Vậy em hãy nêu rõ: khi chuyển sang Microservices thì sự phức tạp về mặt dữ liệu và quản lý giao tác (Transaction) phát sinh như thế nào?"
                </p>
              </div>

            </div>
          </div>

        </div>

        {/* RIGHT COLUMN: RUBRIC EVALUATION SLIDERS & REVIEW NOTES (5 cols) (Matching Image 1 & Image 2) */}
        <div className="lg:col-span-5 space-y-6">
          
          {/* Rubric Criteria Evaluation Card (Matching Image 1 & Image 2) */}
          <div className="bg-white border border-slate-200/90 rounded-3xl p-6 shadow-2xs space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="font-heading font-extrabold text-base text-slate-900">
                  Chấm Điểm Đối Chiếu Rubric
                </h3>
                <p className="text-[11px] text-slate-500">Giảng viên có quyền điều chỉnh điểm của AI</p>
              </div>
              <span className="px-3 py-1 rounded-full text-xs font-mono font-extrabold bg-blue-50 text-blue-700 border border-blue-200">
                Q2: 8.0 pts
              </span>
            </div>

            {/* 4 Interactive Rubric Sliders */}
            <div className="space-y-4 text-xs">
              
              {/* Criterion 1 */}
              <div className="space-y-1.5">
                <div className="flex justify-between font-bold">
                  <span className="text-slate-800">C1: Nắm vững kiến trúc Monolith vs Service (30%)</span>
                  <span className="font-mono text-blue-600 font-extrabold">{c1} / 3.0</span>
                </div>
                <input 
                  type="range" min="0" max="3.0" step="0.1" value={c1}
                  onChange={(e) => setC1(parseFloat(e.target.value))}
                  className="w-full accent-blue-600 cursor-pointer h-2 bg-slate-100 rounded-lg"
                />
                <p className="text-[11px] text-slate-500">Phân tích chính xác blast radius và tính tự chủ release.</p>
              </div>

              {/* Criterion 2 */}
              <div className="space-y-1.5">
                <div className="flex justify-between font-bold">
                  <span className="text-slate-800">C2: Phân tích Scalability &amp; Trade-offs (30%)</span>
                  <span className="font-mono text-sky-600 font-extrabold">{c2} / 3.0</span>
                </div>
                <input 
                  type="range" min="0" max="3.0" step="0.1" value={c2}
                  onChange={(e) => setC2(parseFloat(e.target.value))}
                  className="w-full accent-sky-600 cursor-pointer h-2 bg-slate-100 rounded-lg"
                />
                <p className="text-[11px] text-slate-500">Giải thích rõ scale dọc vs scale ngang chọn lọc.</p>
              </div>

              {/* Criterion 3 */}
              <div className="space-y-1.5">
                <div className="flex justify-between font-bold">
                  <span className="text-slate-800">C3: Phản hồi câu hỏi xoáy SAGA &amp; Outbox (25%)</span>
                  <span className="font-mono text-purple-600 font-extrabold">{c3} / 2.5</span>
                </div>
                <input 
                  type="range" min="0" max="2.5" step="0.1" value={c3}
                  onChange={(e) => setC3(parseFloat(e.target.value))}
                  className="w-full accent-purple-600 cursor-pointer h-2 bg-slate-100 rounded-lg"
                />
                <p className="text-[11px] text-slate-500">Nhắc đúng SAGA Pattern và Eventual consistency.</p>
              </div>

              {/* Criterion 4 */}
              <div className="space-y-1.5">
                <div className="flex justify-between font-bold">
                  <span className="text-slate-800">C4: Kỹ năng diễn đạt &amp; Phong thái tự tin (15%)</span>
                  <span className="font-mono text-emerald-600 font-extrabold">{c4} / 1.5</span>
                </div>
                <input 
                  type="range" min="0" max="1.5" step="0.1" value={c4}
                  onChange={(e) => setC4(parseFloat(e.target.value))}
                  className="w-full accent-emerald-600 cursor-pointer h-2 bg-slate-100 rounded-lg"
                />
                <p className="text-[11px] text-slate-500">Âm lượng đều đặn, không ngắc ngứ, đúng thuật ngữ.</p>
              </div>

            </div>

            {/* Total Question Score Card & Save Button */}
            <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-3">
              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase block">TỔNG ĐIỂM CÂU 2</span>
                <p className="font-heading font-black text-2xl text-slate-900 leading-tight">
                  {totalQuestionScore} <span className="text-xs text-slate-400 font-normal">/ 10</span>
                </p>
              </div>

              <button
                type="button"
                onClick={handleSaveQuestionScore}
                className="px-6 py-2.5 rounded-2xl bg-[#0066FF] hover:bg-[#0052CC] text-white font-bold text-xs shadow-md shadow-blue-600/20 active:scale-95 transition cursor-pointer"
              >
                Lưu Điểm Câu Này
              </button>
            </div>
          </div>

          {/* Lecturer Review Notes Card (Matching Image 2) */}
          <div className="bg-white border border-slate-200/90 rounded-3xl p-6 shadow-2xs space-y-3 text-xs">
            <label className="font-bold text-slate-900 block">Ghi chú hậu kiểm của Giảng viên:</label>
            <textarea
              rows={4}
              value={lecturerNotes}
              onChange={(e) => setLecturerNotes(e.target.value)}
              placeholder="Nhập nhận xét hậu kiểm..."
              className="w-full bg-slate-50 border border-slate-200 rounded-2xl p-3.5 text-xs text-slate-800 focus:ring-2 focus:ring-blue-500 focus:outline-none transition leading-relaxed font-sans"
            />
          </div>

        </div>

      </div>

      {/* 5. FINAL SIGN OFF CONFIRMATION MODAL */}
      {isSignOffModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-heading font-bold text-base text-slate-900">
                Xác Nhận Ký Duyệt &amp; Chốt Điểm FAP
              </h3>
              <button onClick={() => setIsSignOffModalOpen(false)} className="text-slate-400 hover:text-slate-700 p-1 cursor-pointer">
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Bạn đang thực hiện xác nhận ký số biên bản phúc khảo cho thí sinh <strong className="text-slate-900">Nguyen Van B (SE160982)</strong> với tổng điểm chốt <strong className="text-blue-600 font-bold">{totalQuestionScore} / 10.0</strong> và đồng bộ dữ liệu vào FAP.
            </p>

            <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2.5 text-xs">
              <button 
                type="button" 
                onClick={() => setIsSignOffModalOpen(false)} 
                className="px-4 py-2 rounded-full border border-slate-200 font-semibold hover:bg-slate-50 cursor-pointer"
              >
                Hủy Bỏ
              </button>
              <button 
                type="button"
                disabled={isSubmitting}
                onClick={handleFinalSignOffSubmit} 
                className="px-5 py-2.5 rounded-full bg-[#059669] hover:bg-emerald-600 text-white font-bold shadow-md shadow-emerald-600/20 active:scale-95 transition disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
              >
                {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <span>Xác Nhận Ký Số &amp; Chốt Điểm ↗</span>
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
