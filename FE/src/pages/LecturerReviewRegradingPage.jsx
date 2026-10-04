import React, { useState } from 'react';
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
  Award
} from 'lucide-react';

export function LecturerReviewRegradingPage({ onNavigate, showToast }) {
  const [selectedQuestion, setSelectedQuestion] = useState(2);
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const [isSignOffModalOpen, setIsSignOffModalOpen] = useState(false);

  // Rubric Sliders State
  const [c1, setC1] = useState(2.7);
  const [c2, setC2] = useState(2.5);
  const [c3, setC3] = useState(2.0);
  const [c4, setC4] = useState(1.3);

  const [lecturerNotes, setLecturerNotes] = useState(
    'Thí sinh trả lời lưu loát, nắm chắc khái niệm SAGA và Outbox Pattern khi được hỏi xoáy. Điểm đánh giá AI phù hợp thực tế.'
  );

  const totalScore = (c1 + c2 + c3 + c4).toFixed(1);

  const toggleAudio = () => {
    setIsPlayingAudio(!isPlayingAudio);
    if (showToast) {
      showToast({
        type: 'info',
        title: 'Phát Âm Thanh Ca Thi',
        message: !isPlayingAudio ? 'Đang phát băng ghi âm câu trả lời của thí sinh (03:10)...' : 'Đã tạm dừng âm thanh.'
      });
    }
  };

  const handleSaveQuestionScore = () => {
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Đã Lưu Điểm Câu Hỏi',
        message: `Đã cập nhật điểm Câu ${selectedQuestion}: ${totalScore}/10 điểm theo Rubric!`
      });
    }
  };

  const handleFinalSignOffSubmit = () => {
    setIsSignOffModalOpen(false);
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Ký Số Hoàn Tất',
        message: 'Đã ký số biên bản khảo thí và đồng bộ kết quả 8.6 điểm vào cổng FAP thành công!'
      });
    }
    if (onNavigate) {
      onNavigate('lecturer-grading-queue');
    }
  };

  return (
    <div className="space-y-6 animate-modal-entry pb-12">
      {/* HEADER & TOP NAVIGATION */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <button 
              onClick={() => onNavigate && onNavigate('lecturer-grading-queue')}
              className="text-xs font-bold text-sky-700 bg-sky-50 px-2.5 py-0.5 rounded-full border border-sky-200 hover:bg-sky-100 flex items-center gap-1 transition"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Trở Lại Hàng Đợi Chấm</span>
            </button>
            <span className="text-xs text-slate-400">• SWD392 Hậu Kiểm &amp; Phúc Khảo</span>
          </div>
          <h1 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
            Hậu Kiểm Âm Thanh &amp; Chấm Chi Tiết Bài Thi Vấn Đáp
          </h1>
        </div>

        <button 
          onClick={() => setIsSignOffModalOpen(true)}
          className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-white text-xs font-bold shadow-md shadow-emerald-600/20 flex items-center gap-1.5 transition active:scale-95 cursor-pointer"
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>Ký Duyệt &amp; Chốt Điểm FAP</span>
        </button>
      </div>

      {/* 1. CANDIDATE OVERVIEW BANNER */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-2xs flex flex-col lg:flex-row lg:items-center justify-between gap-5">
        {/* Left: Candidate Info */}
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-sky-600 to-sky-400 text-white font-heading font-extrabold text-lg flex items-center justify-center shadow-md">
            NB
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h1 className="font-heading font-extrabold text-xl text-slate-900">Nguyen Van B</h1>
              <span className="font-mono text-xs font-bold text-sky-700 bg-sky-50 border border-sky-200 px-2 py-0.5 rounded">
                SE160982
              </span>
              <span className="text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                Cohort SE1701 • SWD392
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-1 flex items-center gap-2 flex-wrap">
              <span>Hoàn thành ca thi: 09:41 AM (GMT+7)</span>
              <span>•</span>
              <span>Hội đồng: AI Examiner Dr. Minh</span>
              <span>•</span>
              <span className="font-mono text-emerald-600 font-bold">SHA-256 Verified ✓</span>
            </p>
          </div>
        </div>

        {/* Center: AI Preliminary Score Pill */}
        <div className="flex items-center gap-4 bg-slate-50 border border-slate-200 p-3 rounded-xl">
          <div className="text-center px-2">
            <span className="text-[10px] text-slate-400 font-bold uppercase block">Điểm AI Sơ Bộ</span>
            <span className="font-heading font-extrabold text-2xl text-slate-900">8.5 <span className="text-xs text-slate-400 font-normal">/ 10</span></span>
          </div>
          <div className="h-8 w-px bg-slate-200"></div>
          <div>
            <span className="px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 font-bold text-xs border border-emerald-300">
              Tier 3: Proficient
            </span>
            <p className="text-[11px] text-slate-500 mt-1">Khớp 91.4% chuẩn Rubric</p>
          </div>
        </div>

        {/* Right: Status & Actions */}
        <div className="flex items-center gap-3">
          <span className="px-3 py-1.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200 text-xs font-bold flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse"></span>
            <span>Chờ Ký Biên Bản</span>
          </span>
          <button 
            onClick={() => {
              if (showToast) showToast({ type: 'info', message: 'Đang khởi tạo tệp Báo cáo PDF bài thi...' });
            }}
            className="px-3 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center gap-1 transition active:scale-95"
          >
            <Download className="w-4 h-4 text-sky-600" />
            <span>Tải Báo Cáo PDF</span>
          </button>
        </div>
      </div>

      {/* 2. QUESTION SELECTOR TABS */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 text-xs font-sans">
        <button 
          onClick={() => setSelectedQuestion(1)}
          className={`px-3.5 py-2 rounded-xl flex items-center gap-2 whitespace-nowrap transition ${
            selectedQuestion === 1 
              ? 'bg-sky-600 text-white font-bold shadow-md shadow-sky-600/20' 
              : 'bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-semibold'
          }`}
        >
          <span>Câu 1: Onion Architecture</span>
          <span className={`px-1.5 py-0.5 rounded font-mono text-[11px] ${selectedQuestion === 1 ? 'bg-white/20' : 'bg-slate-100 font-bold'}`}>9.0/10</span>
        </button>

        <button 
          onClick={() => setSelectedQuestion(2)}
          className={`px-3.5 py-2 rounded-xl flex items-center gap-2 whitespace-nowrap transition ${
            selectedQuestion === 2 
              ? 'bg-sky-600 text-white font-bold shadow-md shadow-sky-600/20' 
              : 'bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-semibold'
          }`}
        >
          <Mic className="w-3.5 h-3.5" />
          <span>Câu 2: Monolith vs Microservices</span>
          <span className={`px-1.5 py-0.5 rounded font-mono text-[11px] ${selectedQuestion === 2 ? 'bg-white/20' : 'bg-slate-100 font-bold'}`}>{totalScore}/10</span>
        </button>

        <button 
          onClick={() => setSelectedQuestion(3)}
          className={`px-3.5 py-2 rounded-xl flex items-center gap-2 whitespace-nowrap transition ${
            selectedQuestion === 3 
              ? 'bg-sky-600 text-white font-bold shadow-md shadow-sky-600/20' 
              : 'bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-semibold'
          }`}
        >
          <span>Câu 3: Outbox Pattern</span>
          <span className={`px-1.5 py-0.5 rounded font-mono text-[11px] ${selectedQuestion === 3 ? 'bg-white/20' : 'bg-slate-100 font-bold'}`}>8.5/10</span>
        </button>

        <button 
          onClick={() => setSelectedQuestion(4)}
          className={`px-3.5 py-2 rounded-xl flex items-center gap-2 whitespace-nowrap transition ${
            selectedQuestion === 4 
              ? 'bg-sky-600 text-white font-bold shadow-md shadow-sky-600/20' 
              : 'bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-semibold'
          }`}
        >
          <span>Câu 4: Real-time STOMP</span>
          <span className={`px-1.5 py-0.5 rounded font-mono text-[11px] ${selectedQuestion === 4 ? 'bg-white/20' : 'bg-slate-100 font-bold'}`}>9.0/10</span>
        </button>

        <button 
          onClick={() => setSelectedQuestion(5)}
          className={`px-3.5 py-2 rounded-xl flex items-center gap-2 whitespace-nowrap transition ${
            selectedQuestion === 5 
              ? 'bg-sky-600 text-white font-bold shadow-md shadow-sky-600/20' 
              : 'bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-semibold'
          }`}
        >
          <span>Câu 5: Course RBAC</span>
          <span className={`px-1.5 py-0.5 rounded font-mono text-[11px] ${selectedQuestion === 5 ? 'bg-white/20' : 'bg-slate-100 font-bold'}`}>8.0/10</span>
        </button>
      </div>

      {/* 3. MAIN WORKSPACE: AUDIO PLAYBACK + TRANSCRIPT vs RUBRIC SLIDERS */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* LEFT: AUDIO PLAYBACK & REAL-TIME TRANSCRIPT (7 COLS) */}
        <div className="lg:col-span-7 space-y-4">
          
          {/* Audio Waveform Player Card */}
          <div className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-2xs space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <Mic className="w-5 h-5 text-sky-600" />
                <h3 className="font-heading font-bold text-slate-900 text-sm">Bản Ghi Âm Câu Trả Lời Của Thí Sinh</h3>
              </div>
              <span className="text-xs text-slate-400 font-mono">Thời lượng: 03:10</span>
            </div>

            {/* Waveform Bar Visualizer (High-Contrast Navy) */}
            <div className="bg-slate-950 text-white p-4 rounded-xl space-y-3">
              <div className="flex items-center gap-3">
                <button 
                  onClick={toggleAudio}
                  className="w-10 h-10 rounded-full bg-sky-500 hover:bg-sky-400 text-white flex items-center justify-center shrink-0 shadow-md transition-transform active:scale-95"
                >
                  {isPlayingAudio ? <Pause className="w-5 h-5 fill-current" /> : <Play className="w-5 h-5 fill-current ml-0.5" />}
                </button>
                <div className="flex-1">
                  {/* Simulated Waveform Bars */}
                  <div className="flex items-center justify-between h-8 gap-1 px-2">
                    {[30, 60, 40, 85, 50, 95, 70, 45, 90, 65, 40, 80, 50, 75, 60, 35].map((h, i) => (
                      <span 
                        key={i} 
                        style={{ height: isPlayingAudio ? `${h}%` : '25%' }}
                        className={`w-1 rounded-full transition-all duration-300 ${isPlayingAudio ? 'bg-sky-400' : 'bg-slate-700'}`}
                      />
                    ))}
                  </div>
                </div>
                <div className="text-right text-xs font-mono text-slate-300">
                  <span>{isPlayingAudio ? '01:45' : '00:00'}</span> / 03:10
                </div>
              </div>
            </div>

            {/* Focus Question Prompt */}
            <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl text-xs space-y-1">
              <span className="text-slate-400 font-bold block uppercase text-[10px]">Câu hỏi phát ra:</span>
              <p className="font-bold text-slate-900 leading-relaxed">
                Hãy phân tích các đánh đổi cốt lõi giữa Kiến trúc Monolith và Microservices về deployment complexity và independent scalability?
              </p>
            </div>
          </div>

          {/* Synchronized Transcript Card */}
          <div className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-2xs space-y-3">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-heading font-bold text-slate-900 text-sm">Bóc Băng Đồng Bộ Thời Gian Thực (Whisper STT)</h3>
              <span className="text-[10px] font-mono text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded font-bold border border-emerald-200">Confidence: 97.8%</span>
            </div>

            {/* Dialogue Stream */}
            <div className="space-y-3 text-xs max-h-[360px] overflow-y-auto pr-1 leading-relaxed">
              {/* AI Examiner Turn */}
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 space-y-1">
                <div className="flex items-center justify-between font-bold text-sky-700">
                  <span>AI Examiner Dr. Minh</span>
                  <span className="font-mono text-slate-400 text-[10px]">00:00 - 00:25</span>
                </div>
                <p className="text-slate-700">
                  "Chào em. Ở câu số 2, hãy phân tích các đánh đổi cốt lõi giữa kiến trúc Monolith và Microservices, đặc biệt là về độ phức tạp khi triển khai và khả năng mở rộng độc lập?"
                </p>
              </div>

              {/* Candidate Turn */}
              <div className="p-3.5 rounded-xl bg-sky-50/60 border border-sky-100 space-y-1">
                <div className="flex items-center justify-between font-bold text-slate-900">
                  <span>Thí sinh: Nguyen Van B</span>
                  <span className="font-mono text-slate-400 text-[10px]">00:26 - 02:15</span>
                </div>
                <p className="text-slate-800">
                  "Dạ thưa Thầy, theo em điểm khác biệt lớn nhất là về <mark className="bg-amber-100 text-amber-900 px-1 rounded font-semibold">blast radius</mark>. Trong Monolith, một lỗi nhỏ ở module thanh toán có thể làm crash toàn bộ ứng dụng và phải build lại toàn bộ artifact. Còn với Microservices, các service chạy độc lập trong container, nếu một service gặp sự cố thì các service khác vẫn hoạt động bình thường."
                </p>
                <p className="text-slate-800 mt-2">
                  "Về khả năng mở rộng, Monolith chỉ có thể scale theo chiều dọc hoặc nhân bản toàn bộ binary gây lãng phí tài nguyên RAM/CPU. Trong khi đó, Microservices cho phép scale chiều ngang chọn lọc chỉ cho service chịu tải cao như Order Service trong đợt flash sale."
                </p>
              </div>

              {/* AI Follow-up Probe Turn */}
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 space-y-1">
                <div className="flex items-center justify-between font-bold text-sky-700">
                  <span>AI Examiner (Hỏi Xoáy Thích Ứng - Adaptive Probing)</span>
                  <span className="font-mono text-slate-400 text-[10px]">02:16 - 02:35</span>
                </div>
                <p className="text-slate-700">
                  "Rất tốt. Vậy em hãy nêu rõ: khi chuyển sang Microservices thì sự phức tạp về mặt dữ liệu và giao dịch (database transaction) sẽ được xử lý ra sao?"
                </p>
              </div>

              {/* Candidate Answer Turn */}
              <div className="p-3.5 rounded-xl bg-sky-50/60 border border-sky-100 space-y-1">
                <div className="flex items-center justify-between font-bold text-slate-900">
                  <span>Thí sinh: Nguyen Van B</span>
                  <span className="font-mono text-slate-400 text-[10px]">02:36 - 03:10</span>
                </div>
                <p className="text-slate-800">
                  "Dạ, trong Microservices ta áp dụng nguyên tắc Database-per-service nên không thể dùng ACID transaction xuyên service. Ta phải dùng mô hình <mark className="bg-amber-100 text-amber-900 px-1 rounded font-semibold">SAGA Pattern</mark> (Orchestration hoặc Choreography) kết hợp với Transactional Outbox Pattern để đảm bảo eventual consistency."
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* RIGHT: RUBRIC SCORING SLIDERS & VERDICT (5 COLS) */}
        <div className="lg:col-span-5 bg-white border border-slate-200/90 rounded-2xl p-5 shadow-2xs space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div>
              <h3 className="font-heading font-bold text-slate-900 text-sm">Chấm Điểm Đối Chiếu Rubric</h3>
              <p className="text-[11px] text-slate-400">Giảng viên có quyền điều chỉnh điểm của AI</p>
            </div>
            <span className="px-2.5 py-1 rounded-full bg-sky-50 text-sky-700 font-bold text-xs border border-sky-200">
              Q2: {totalScore} pts
            </span>
          </div>

          {/* 4 Rubric Criteria Sliders */}
          <div className="space-y-4 text-xs">
            {/* C1: Kiến thức kiến trúc */}
            <div className="space-y-1.5">
              <div className="flex justify-between font-bold">
                <span className="text-slate-800">C1: Nắm vững kiến trúc Monolith vs Service (30%)</span>
                <span class="text-sky-700 font-mono">{c1.toFixed(1)} / 3.0</span>
              </div>
              <input 
                type="range" 
                min="0" 
                max="3" 
                step="0.1" 
                value={c1} 
                onChange={(e) => setC1(parseFloat(e.target.value))} 
                className="w-full accent-sky-600 cursor-pointer"
              />
              <p className="text-[11px] text-slate-500">Phân tích chính xác blast radius và tính tự chủ release.</p>
            </div>

            {/* C2: Khả năng mở rộng */}
            <div className="space-y-1.5">
              <div className="flex justify-between font-bold">
                <span className="text-slate-800">C2: Phân tích Scalability &amp; Trade-offs (30%)</span>
                <span className="text-sky-700 font-mono">{c2.toFixed(1)} / 3.0</span>
              </div>
              <input 
                type="range" 
                min="0" 
                max="3" 
                step="0.1" 
                value={c2} 
                onChange={(e) => setC2(parseFloat(e.target.value))} 
                className="w-full accent-sky-600 cursor-pointer"
              />
              <p className="text-[11px] text-slate-500">Giải thích rõ scale dọc vs scale ngang chọn lọc.</p>
            </div>

            {/* C3: Xử lý giao dịch phân tán */}
            <div className="space-y-1.5">
              <div className="flex justify-between font-bold">
                <span className="text-slate-800">C3: Phản hồi câu hỏi xoáy SAGA &amp; Outbox (25%)</span>
                <span className="text-sky-700 font-mono">{c3.toFixed(1)} / 2.5</span>
              </div>
              <input 
                type="range" 
                min="0" 
                max="2.5" 
                step="0.1" 
                value={c3} 
                onChange={(e) => setC3(parseFloat(e.target.value))} 
                className="w-full accent-sky-600 cursor-pointer"
              />
              <p className="text-[11px] text-slate-500">Nhắc đúng SAGA Pattern và Eventual consistency.</p>
            </div>

            {/* C4: Diễn đạt & Độ trôi chảy */}
            <div className="space-y-1.5">
              <div className="flex justify-between font-bold">
                <span className="text-slate-800">C4: Kỹ năng diễn đạt &amp; Phong thái tự tin (15%)</span>
                <span className="text-sky-700 font-mono">{c4.toFixed(1)} / 1.5</span>
              </div>
              <input 
                type="range" 
                min="0" 
                max="1.5" 
                step="0.1" 
                value={c4} 
                onChange={(e) => setC4(parseFloat(e.target.value))} 
                className="w-full accent-sky-600 cursor-pointer"
              />
              <p className="text-[11px] text-slate-500">Âm lượng đều đặn, không ngắc ngứ, đúng thuật ngữ.</p>
            </div>
          </div>

          {/* Total Question Score Summary Box */}
          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-between">
            <div>
              <span className="text-[10px] text-slate-400 font-bold uppercase block">Tổng Điểm Câu 2</span>
              <p className="font-heading font-extrabold text-2xl text-slate-900">{totalScore} / 10</p>
            </div>
            <button 
              onClick={handleSaveQuestionScore}
              className="px-4 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-2xs transition active:scale-95"
            >
              Lưu Điểm Câu Này
            </button>
          </div>

          {/* Lecturer Notes Box */}
          <div className="space-y-1.5 text-xs">
            <label className="block font-bold text-slate-700">Ghi chú hậu kiểm của Giảng viên:</label>
            <textarea 
              rows={3} 
              value={lecturerNotes}
              onChange={(e) => setLecturerNotes(e.target.value)}
              className="w-full p-2.5 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none text-slate-800"
            />
          </div>
        </div>

      </div>

      {/* ================= MODAL: FINAL GRADE SIGN-OFF ================= */}
      {isSignOffModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry">
            <div className="p-5 border-b border-slate-100 bg-slate-950 text-white flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-300 flex items-center justify-center">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-heading font-bold text-sm">Xác Nhận Ký Biên Bản &amp; Chốt Điểm FAP</h3>
                  <p className="text-[11px] text-slate-400">Nguyen Van B • SE160982 • SWD392</p>
                </div>
              </div>
              <button onClick={() => setIsSignOffModalOpen(false)} className="text-slate-400 hover:text-white p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 space-y-4 text-xs">
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="flex justify-between">
                  <span className="text-slate-500">Điểm AI Sơ Bộ:</span>
                  <span className="font-bold text-slate-800">8.5 / 10</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Điểm Giảng Viên Chốt:</span>
                  <span className="font-bold text-emerald-600 text-sm">8.6 / 10 (Tier 3: Proficient)</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Giảng viên ký xác thực:</span>
                  <span className="font-bold text-slate-800">Dr. Marcus Vance</span>
                </div>
              </div>

              <div>
                <label className="flex items-center gap-2 cursor-pointer font-semibold text-slate-800">
                  <input type="checkbox" defaultChecked className="rounded text-sky-600 focus:ring-sky-500 w-4 h-4 mt-0.5"/>
                  <span>Tôi xác nhận đã nghe lại bản ghi âm và đồng ý chuyển điểm này vào hệ thống FAP chính thức.</span>
                </label>
              </div>
            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-3">
              <button 
                onClick={() => setIsSignOffModalOpen(false)} 
                className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold"
              >
                Hủy
              </button>
              <button 
                onClick={handleFinalSignOffSubmit} 
                className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold shadow-md shadow-emerald-600/20 flex items-center gap-1.5 transition active:scale-95"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>Ký Số &amp; Hoàn Tất</span>
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
