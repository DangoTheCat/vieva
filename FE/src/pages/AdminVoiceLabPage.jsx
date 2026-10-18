import React, { useState, useEffect } from 'react';
import { 
  Volume2, 
  Mic, 
  Sliders, 
  Play, 
  Square,
  Save, 
  Plus, 
  Activity, 
  Zap, 
  Check, 
  X, 
  Radio, 
  ShieldCheck, 
  Sparkles,
  Gauge,
  Cpu,
  Waves,
  RefreshCw,
  Clock
} from 'lucide-react';

export function AdminVoiceLabPage({ showToast }) {
  const [activeProfile, setActiveProfile] = useState('minh');
  const [speakingRate, setSpeakingRate] = useState(1.0);
  const [pitchShift, setPitchShift] = useState(0);
  const [pauseDuration, setPauseDuration] = useState(450);
  const [testText, setTestText] = useState(
    'Chào em, em hãy phân tích sự khác nhau giữa kiến trúc Monolith và Microservices về tính tự chủ triển khai và vùng ảnh hưởng sự cố?'
  );
  const [isPlaying, setIsPlaying] = useState(false);

  // Modals state
  const [isNewVoiceModalOpen, setIsNewVoiceModalOpen] = useState(false);
  const [isStressTestModalOpen, setIsStressTestModalOpen] = useState(false);

  // Stress test simulation state
  const [stressProgress, setStressProgress] = useState(0);
  const [stressStatusText, setStressStatusText] = useState('Sẵn sàng chạy');
  const [isRunningStress, setIsRunningStress] = useState(false);

  // New Voice Form State
  const [newVoiceName, setNewVoiceName] = useState('');
  const [newVoiceLang, setNewVoiceLang] = useState('vi-VN');
  const [newVoiceGender, setNewVoiceGender] = useState('male');
  const [newVoicePrompt, setNewVoicePrompt] = useState('');

  // Keyboard accessibility: Escape to close active modal
  useEffect(() => {
    if (!isNewVoiceModalOpen && !isStressTestModalOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        setIsNewVoiceModalOpen(false);
        setIsStressTestModalOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isNewVoiceModalOpen, isStressTestModalOpen]);

  const speakText = (text) => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utter = new SpeechSynthesisUtterance(text);
      utter.lang = activeProfile === 'sarah' ? 'en-US' : 'vi-VN';
      utter.rate = speakingRate;
      utter.pitch = 1.0 + (pitchShift * 0.1);
      
      utter.onstart = () => setIsPlaying(true);
      utter.onend = () => setIsPlaying(false);
      utter.onerror = () => setIsPlaying(false);

      window.speechSynthesis.speak(utter);
      if (showToast) {
        showToast({
          type: 'info',
          message: `Đang phát âm thử nghiệm giọng ${activeProfile === 'minh' ? 'Dr. Minh' : activeProfile === 'sarah' ? 'Prof. Sarah' : 'Dr. Anh'}...`
        });
      }
    } else if (showToast) {
      showToast({
        type: 'error',
        message: 'Trình duyệt không hỗ trợ Web Speech Synthesis API.'
      });
    }
  };

  const stopSpeaking = () => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      setIsPlaying(false);
    }
  };

  const handleSaveConfig = () => {
    if (showToast) {
      showToast({
        type: 'success',
        message: `Đã lưu thông số âm học cho Profile ${activeProfile === 'minh' ? 'Dr. Minh' : activeProfile === 'sarah' ? 'Prof. Sarah' : 'Dr. Anh'}!`
      });
    }
  };

  const handleCreateVoiceSubmit = (e) => {
    e.preventDefault();
    setIsNewVoiceModalOpen(false);
    if (showToast) {
      showToast({
        type: 'success',
        message: `Đã khởi tạo thành công giọng nói ${newVoiceName || 'Mới'} (${newVoiceLang})!`
      });
    }
    setNewVoiceName('');
    setNewVoicePrompt('');
  };

  const handleRunStressTest = () => {
    setIsRunningStress(true);
    setStressProgress(20);
    setStressStatusText('Đang đẩy 50 luồng WebSocket...');

    setTimeout(() => {
      setStressProgress(65);
      setStressStatusText('Đang phân tích Jitter & P99 Latency...');
    }, 700);

    setTimeout(() => {
      setStressProgress(100);
      setStressStatusText('Hoàn tất: 100% Gói Tin Đạt Chuẩn Realtime!');
      setIsRunningStress(false);
      if (showToast) {
        showToast({
          type: 'success',
          message: 'Stress Test hoàn tất: Đạt tiêu chuẩn Viva Realtime!'
        });
      }
    }, 1500);
  };

  const getActiveProfileLabel = () => {
    if (activeProfile === 'minh') return 'Dr. Minh';
    if (activeProfile === 'sarah') return 'Prof. Sarah';
    return 'Dr. Anh';
  };

  return (
    <div className="space-y-6 animate-modal-entry text-ink font-body">
      {/* 1. HEADER & TELEMETRY ROW */}
      <section className="space-y-4">
        {/* Badge Phân Hệ */}
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg bg-paper border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5]">
          <span className="w-2.5 h-2.5 rounded-full bg-green-500"></span>
          <span className="text-xs font-bold uppercase tracking-wider text-green-700">Module 3 • Viva Core Realtime Engine</span>
        </div>

        {/* Tiêu đề H1 & Mô tả */}
        <div className="space-y-1">
          <h1 className="font-heading text-2xl sm:text-3xl font-bold tracking-tight text-ink">
            Hiệu Chuẩn Giọng Nói Giám Thị Ảo (TTS &amp; STT)
          </h1>
          <p className="text-xs sm:text-sm text-on-surface-variant max-w-4xl font-normal leading-relaxed">
            Tinh chỉnh tham số mô hình chuyển văn bản thành giọng nói (Neural TTS), tốc độ phát âm thanh sư phạm và độ trễ phản hồi đàm thoại hai chiều.
          </p>
        </div>

        {/* 3 Quick Metric Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-1">
          {/* Metric 1: STT Latency */}
          <div className="bg-paper border-2 border-green-500 rounded-xl p-5 shadow-[3px_3px_0_#DEB9B5] flex flex-col justify-between transition-transform hover:-translate-y-0.5">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-green-800">STT Latency</span>
              <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-green-100 text-green-700 border border-green-300">
                -12%
              </span>
            </div>
            <div className="mt-3 flex items-baseline gap-2">
              <span className="font-heading text-3xl font-bold text-green-600">142</span>
              <span className="text-sm font-semibold text-on-surface-variant">ms</span>
            </div>
            <p className="text-xs text-on-surface-variant mt-2">Thời gian xử lý giọng nói thành văn bản đầu vào</p>
          </div>

          {/* Metric 2: TTS First-Byte */}
          <div className="bg-paper border-2 border-green-500 rounded-xl p-5 shadow-[3px_3px_0_#DEB9B5] flex flex-col justify-between transition-transform hover:-translate-y-0.5">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-green-800">TTS First-Byte</span>
              <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-pink-100 text-pink-700 border border-pink-300">
                +4%
              </span>
            </div>
            <div className="mt-3 flex items-baseline gap-2">
              <span className="font-heading text-3xl font-bold text-pink-600">185</span>
              <span className="text-sm font-semibold text-on-surface-variant">ms</span>
            </div>
            <p className="text-xs text-on-surface-variant mt-2">Độ trễ dòng byte âm thanh đầu tiên trả về</p>
          </div>

          {/* Metric 3: Audio Quality */}
          <div className="bg-paper border-2 border-green-500 rounded-xl p-5 shadow-[3px_3px_0_#DEB9B5] flex flex-col justify-between transition-transform hover:-translate-y-0.5">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-green-800">Audio Quality</span>
              <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-surface-container text-ink border border-outline-variant">
                0%
              </span>
            </div>
            <div className="mt-3 flex items-baseline gap-2">
              <span className="font-heading text-3xl font-bold text-ink">48kHz / 24bit</span>
            </div>
            <p className="text-xs text-on-surface-variant mt-2">Chuẩn phòng thu cách ly nhiễu và đục âm</p>
          </div>
        </div>
      </section>

      {/* 2. TOP TOOLBAR ACTIONS */}
      <section className="bg-paper border-2 border-green-500 rounded-xl p-4 sm:p-5 shadow-[3px_3px_0_#DEB9B5] flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="relative flex h-3.5 w-3.5">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-500 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-3.5 w-3.5 bg-green-600"></span>
          </span>
          <div>
            <p className="text-sm sm:text-base font-bold text-ink flex items-center gap-2">
              Trạng Thái Bộ Tổng Hợp: Hoạt Động Tốt
            </p>
            <p className="text-xs sm:text-sm text-green-700 font-medium">
              Latency 24ms • WebSocket OPUS Realtime • Web Speech API Sound Engine
            </p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button 
            type="button"
            onClick={() => setIsStressTestModalOpen(true)}
            className="cursor-pointer inline-flex items-center gap-2 rounded-lg h-10 px-4 bg-paper hover:bg-green-100 text-green-800 text-xs sm:text-sm font-bold border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all"
          >
            <Gauge className="w-4 h-4 text-green-700" />
            <span>Kiểm Thử Tải Âm Thanh</span>
          </button>
          <button 
            type="button"
            onClick={() => setIsNewVoiceModalOpen(true)}
            className="cursor-pointer inline-flex items-center gap-2 rounded-lg h-10 px-5 bg-pink-500 hover:bg-pink-400 text-ink text-xs sm:text-sm font-bold border-2 border-ink shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>+ Tạo Giọng Nói Mới</span>
          </button>
        </div>
      </section>

      {/* 3. WORKSPACE 2 CỘT (SPLIT VIEW) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* CỘT TRÁI (6 COLS) - HỒ SƠ GIỌNG NÓI GIÁM THỊ ẢO */}
        <section className="lg:col-span-6 space-y-4">
          {/* Title bar */}
          <div className="flex items-center justify-between bg-paper border-2 border-green-500 rounded-xl px-4 py-3 shadow-[3px_3px_0_#DEB9B5]">
            <div className="flex items-center gap-2">
              <Volume2 className="w-5 h-5 text-green-700" />
              <h2 className="font-heading text-base sm:text-lg font-bold text-ink">Hồ Sơ Giọng Nói Giám Thị Ảo (Profiles)</h2>
            </div>
            <span className="px-2.5 py-1 text-xs font-bold rounded-lg bg-green-100 text-green-800 border border-green-400">
              3 Active Profiles
            </span>
          </div>

          {/* Danh sách Profile Cards */}
          <div className="space-y-4">
            {/* Profile 1: Active/Default (Dr. Minh) */}
            <div 
              onClick={() => setActiveProfile('minh')}
              className={`rounded-xl p-5 shadow-[3px_3px_0_#DEB9B5] relative transition-all cursor-pointer ${
                activeProfile === 'minh'
                  ? 'bg-pink-100 border-2 border-green-600 ring-2 ring-pink-300'
                  : 'bg-paper border-2 border-green-500/80 hover:border-green-600'
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-pink-400 border-2 border-ink flex items-center justify-center font-heading font-bold text-lg text-ink shadow-[2px_2px_0_#DEB9B5]">
                    DM
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-ink text-base">Dr. Minh (Giám Thị Chính)</h3>
                      <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-green-500 text-ink border border-green-700 shadow-xs">
                        DEFAULT
                      </span>
                    </div>
                    <p className="text-xs text-green-800 font-medium mt-0.5">Giọng Nam miền Bắc • Phong thái điềm đạm, chuẩn sư phạm</p>
                  </div>
                </div>

                {/* Play preview button */}
                <button 
                  type="button"
                  onClick={(e) => { 
                    e.stopPropagation(); 
                    speakText('Chào em, Thầy là Dr. Minh, Giám thị ảo phòng thi số 4. Em hãy sẵn sàng cho câu hỏi đầu tiên.'); 
                  }}
                  className="shrink-0 w-9 h-9 rounded-lg bg-paper border-2 border-ink flex items-center justify-center text-ink hover:bg-pink-300 shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all cursor-pointer"
                  title="Nghe thử giọng"
                >
                  <Play className="w-4 h-4 fill-current ml-0.5" />
                </button>
              </div>

              <div className="mt-4 pt-3 border-t border-green-300 flex flex-wrap items-center justify-between text-xs font-medium text-green-900 gap-2">
                <span className="inline-flex items-center gap-1.5">
                  <Cpu className="w-4 h-4 text-green-700" />
                  Mô hình: <strong>Neural-vi-VN-Standard</strong>
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <Waves className="w-4 h-4 text-green-700" />
                  Sampling: <strong>24kHz</strong>
                </span>
              </div>
            </div>

            {/* Profile 2: Prof. Sarah */}
            <div 
              onClick={() => setActiveProfile('sarah')}
              className={`rounded-xl p-5 shadow-[3px_3px_0_#DEB9B5] relative transition-all cursor-pointer ${
                activeProfile === 'sarah'
                  ? 'bg-pink-100 border-2 border-green-600 ring-2 ring-pink-300'
                  : 'bg-paper border-2 border-green-500/80 hover:border-green-600'
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-green-200 border-2 border-green-700 flex items-center justify-center font-heading font-bold text-lg text-green-900 shadow-[2px_2px_0_#DEB9B5]">
                    PS
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-ink text-base">Prof. Sarah (English Oral Exams)</h3>
                      <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-paper text-green-800 border border-green-400">
                        EN-US
                      </span>
                    </div>
                    <p className="text-xs text-on-surface-variant font-medium mt-0.5">Giọng Nữ Bắc Mỹ • Chuẩn khảo thí Quốc tế</p>
                  </div>
                </div>

                {/* Play button */}
                <button 
                  type="button"
                  onClick={(e) => { 
                    e.stopPropagation(); 
                    speakText('Welcome candidate to the software architecture oral examination.'); 
                  }}
                  className="shrink-0 w-9 h-9 rounded-lg bg-paper border-2 border-ink flex items-center justify-center text-ink hover:bg-pink-300 shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all cursor-pointer"
                  title="Nghe thử giọng"
                >
                  <Play className="w-4 h-4 fill-current ml-0.5" />
                </button>
              </div>

              <div className="mt-4 pt-3 border-t border-green-200 flex flex-wrap items-center justify-between text-xs font-medium text-green-900 gap-2">
                <span className="inline-flex items-center gap-1.5">
                  <Cpu className="w-4 h-4 text-green-700" />
                  Mô hình: <strong>Neural2-en-US-F</strong>
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <Waves className="w-4 h-4 text-green-700" />
                  Sampling: <strong>48kHz</strong>
                </span>
              </div>
            </div>

            {/* Profile 3: Dr. Anh */}
            <div 
              onClick={() => setActiveProfile('anh')}
              className={`rounded-xl p-5 shadow-[3px_3px_0_#DEB9B5] relative transition-all cursor-pointer ${
                activeProfile === 'anh'
                  ? 'bg-pink-100 border-2 border-green-600 ring-2 ring-pink-300'
                  : 'bg-paper border-2 border-green-500/80 hover:border-green-600'
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-pink-200 border-2 border-pink-700 flex items-center justify-center font-heading font-bold text-lg text-pink-900 shadow-[2px_2px_0_#DEB9B5]">
                    DA
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-ink text-base">Dr. Anh (Giảng Viên Nữ)</h3>
                      <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-paper text-green-800 border border-green-400">
                        VI-VN
                      </span>
                    </div>
                    <p className="text-xs text-on-surface-variant font-medium mt-0.5">Giọng Nữ miền Nam • Âm vực nhẹ nhàng, rõ chữ</p>
                  </div>
                </div>

                {/* Play button */}
                <button 
                  type="button"
                  onClick={(e) => { 
                    e.stopPropagation(); 
                    speakText('Chào bạn, mời bạn trả lời câu hỏi về kiến trúc phần mềm.'); 
                  }}
                  className="shrink-0 w-9 h-9 rounded-lg bg-paper border-2 border-ink flex items-center justify-center text-ink hover:bg-pink-300 shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all cursor-pointer"
                  title="Nghe thử giọng"
                >
                  <Play className="w-4 h-4 fill-current ml-0.5" />
                </button>
              </div>

              <div className="mt-4 pt-3 border-t border-green-200 flex flex-wrap items-center justify-between text-xs font-medium text-green-900 gap-2">
                <span className="inline-flex items-center gap-1.5">
                  <Cpu className="w-4 h-4 text-green-700" />
                  Mô hình: <strong>Neural-vi-VN-Female</strong>
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <Waves className="w-4 h-4 text-green-700" />
                  Sampling: <strong>24kHz</strong>
                </span>
              </div>
            </div>
          </div>
        </section>

        {/* CỘT PHẢI (6 COLS) - BỘ ĐIỀU KHIỂN THAM SỐ ÂM HỌC */}
        <section className="lg:col-span-6 space-y-4">
          {/* Title bar */}
          <div className="flex items-center justify-between bg-paper border-2 border-green-500 rounded-xl px-4 py-3 shadow-[3px_3px_0_#DEB9B5]">
            <div className="flex items-center gap-2">
              <Sliders className="w-5 h-5 text-pink-700" />
              <h2 className="font-heading text-base sm:text-lg font-bold text-ink">Bộ Điều Khiển Tham Số Âm Học (Acoustic Tuner)</h2>
            </div>
            <span className="px-2.5 py-1 text-xs font-bold rounded-lg bg-pink-100 text-pink-900 border border-pink-400">
              Profile: {getActiveProfileLabel()}
            </span>
          </div>

          <div className="bg-paper border-2 border-green-500 rounded-xl p-5 shadow-[3px_3px_0_#DEB9B5] space-y-5">
            {/* Sliders Box */}
            <div className="space-y-5">
              {/* 1. Tốc Độ Đọc (Speaking Rate) */}
              <div className="space-y-2">
                <div className="flex justify-between items-center text-xs sm:text-sm">
                  <label className="font-bold text-ink flex items-center gap-1.5">
                    <Activity className="w-4 h-4 text-green-700" />
                    Tốc Độ Đọc (Speaking Rate)
                  </label>
                  <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-green-100 text-green-800 border border-green-400">
                    {speakingRate.toFixed(2)}x {speakingRate === 1.0 ? '(Chuẩn)' : ''}
                  </span>
                </div>
                <input 
                  type="range" 
                  min="0.75" 
                  max="1.50" 
                  step="0.05" 
                  value={speakingRate} 
                  onChange={(e) => setSpeakingRate(parseFloat(e.target.value))}
                  className="w-full h-2 bg-green-200 rounded-lg appearance-none cursor-pointer accent-green-600"
                />
                <div className="flex justify-between text-[11px] text-green-800 font-semibold px-0.5">
                  <span>0.75x (Chậm)</span>
                  <span>1.00x (Chuẩn Sư Phạm)</span>
                  <span>1.50x (Nhanh)</span>
                </div>
              </div>

              {/* 2. Cao Độ Giọng (Pitch Shift) */}
              <div className="space-y-2">
                <div className="flex justify-between items-center text-xs sm:text-sm">
                  <label className="font-bold text-ink flex items-center gap-1.5">
                    <Zap className="w-4 h-4 text-green-700" />
                    Cao Độ Giọng (Pitch Shift)
                  </label>
                  <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-green-100 text-green-800 border border-green-400">
                    {pitchShift >= 0 ? `+${pitchShift}` : pitchShift} semitones
                  </span>
                </div>
                <input 
                  type="range" 
                  min="-4" 
                  max="4" 
                  step="1" 
                  value={pitchShift} 
                  onChange={(e) => setPitchShift(parseInt(e.target.value, 10))}
                  className="w-full h-2 bg-green-200 rounded-lg appearance-none cursor-pointer accent-green-600"
                />
                <div className="flex justify-between text-[11px] text-green-800 font-semibold px-0.5">
                  <span>-4 (Trầm ấm)</span>
                  <span>0 (Trung tính)</span>
                  <span>+4 (Thanh thoát)</span>
                </div>
              </div>

              {/* 3. Khoảng Nghỉ Giữa Các Câu (Sentence Pause) */}
              <div className="space-y-2">
                <div className="flex justify-between items-center text-xs sm:text-sm">
                  <label className="font-bold text-ink flex items-center gap-1.5">
                    <Clock className="w-4 h-4 text-green-700" />
                    Khoảng Nghỉ Giữa Các Câu (Sentence Pause)
                  </label>
                  <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-green-100 text-green-800 border border-green-400">
                    {pauseDuration} ms
                  </span>
                </div>
                <input 
                  type="range" 
                  min="200" 
                  max="1000" 
                  step="50" 
                  value={pauseDuration} 
                  onChange={(e) => setPauseDuration(parseInt(e.target.value, 10))}
                  className="w-full h-2 bg-green-200 rounded-lg appearance-none cursor-pointer accent-green-600"
                />
                <div className="flex justify-between text-[11px] text-green-800 font-semibold px-0.5">
                  <span>200ms (Liền mạch)</span>
                  <span>450ms (Tự nhiên)</span>
                  <span>1000ms (Suy ngẫm)</span>
                </div>
              </div>
            </div>

            {/* Interactive Playground: Thử nghiệm phát âm */}
            <div className="pt-4 border-t-2 border-green-200 space-y-3">
              <label className="block text-xs sm:text-sm font-bold text-ink">
                Nội Dung Thử Nghiệm Phát Âm:
              </label>
              <div className="relative">
                <textarea 
                  rows={3}
                  value={testText}
                  onChange={(e) => setTestText(e.target.value)}
                  placeholder="Nhập văn bản cần giám thị phát âm..."
                  className="w-full p-3 text-xs sm:text-sm text-ink bg-white border-2 border-green-500 rounded-xl focus:ring-2 focus:ring-pink-400 focus:outline-none transition-all resize-none shadow-inner leading-relaxed"
                />
              </div>

              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1">
                <span className="text-xs text-green-800 font-medium flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-green-600" />
                  Web Speech API Browser Sound Engine
                </span>
                <div className="flex items-center gap-2">
                  {isPlaying ? (
                    <button 
                      type="button"
                      onClick={stopSpeaking}
                      className="cursor-pointer inline-flex items-center gap-2 rounded-lg h-9 px-4 bg-pink-600 hover:bg-pink-700 text-white text-xs sm:text-sm font-bold border-2 border-ink shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all"
                    >
                      <Square className="w-4 h-4 fill-current" />
                      <span>Dừng Phát</span>
                    </button>
                  ) : (
                    <button 
                      type="button"
                      onClick={() => speakText(testText)}
                      className="cursor-pointer inline-flex items-center gap-2 rounded-lg h-9 px-4 bg-pink-500 hover:bg-pink-400 text-ink text-xs sm:text-sm font-bold border-2 border-ink shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all"
                    >
                      <Play className="w-4 h-4 fill-current" />
                      <span>Phát Âm Thử Ngay</span>
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Nút Lưu Cấu Hình Mới */}
            <div className="pt-4 border-t-2 border-green-200 flex justify-end">
              <button 
                type="button"
                onClick={handleSaveConfig}
                className="w-full sm:w-auto cursor-pointer inline-flex items-center justify-center gap-2 rounded-lg h-10 px-6 bg-green-500 hover:bg-green-600 text-ink font-bold text-xs sm:text-sm border-2 border-ink shadow-[3px_3px_0_#1F1F1D] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all"
              >
                <Save className="w-4 h-4" />
                <span>Lưu Cấu Hình Mới</span>
              </button>
            </div>
          </div>
        </section>
      </div>

      {/* ================= MODAL 1: TẠO HỒ SƠ GIỌNG NÓI MỚI ================= */}
      {isNewVoiceModalOpen && (
        <div className="fixed inset-0 z-50 bg-ink/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150">
          <div className="bg-paper border-2 border-green-500 rounded-2xl w-full max-w-lg p-6 shadow-[6px_6px_0_#DEB9B5] space-y-4 animate-modal-entry text-ink">
            <div className="flex items-center justify-between border-b-2 border-green-200 pb-3">
              <div className="flex items-center gap-2">
                <Volume2 className="w-5 h-5 text-pink-700" />
                <h3 className="font-heading text-lg sm:text-xl font-bold text-ink">Thêm Hồ Sơ Giọng Nói Mới</h3>
              </div>
              <button 
                type="button"
                onClick={() => setIsNewVoiceModalOpen(false)}
                className="cursor-pointer text-ink hover:text-pink-700 w-8 h-8 rounded-lg border border-green-400 flex items-center justify-center bg-green-50 transition"
                aria-label="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateVoiceSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-ink uppercase mb-1">Tên Giám Thị / Định Danh</label>
                <input 
                  type="text" 
                  required
                  value={newVoiceName}
                  onChange={(e) => setNewVoiceName(e.target.value)}
                  placeholder="VD: Dr. Quang (Ban Hội Thao Tin Học)" 
                  className="w-full px-3 py-2 text-xs sm:text-sm bg-white border-2 border-green-500 rounded-lg focus:outline-none focus:ring-2 focus:ring-pink-400"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-ink uppercase mb-1">Ngôn Ngữ</label>
                  <select 
                    value={newVoiceLang}
                    onChange={(e) => setNewVoiceLang(e.target.value)}
                    className="w-full px-3 py-2 text-xs sm:text-sm bg-white border-2 border-green-500 rounded-lg focus:outline-none"
                  >
                    <option value="vi-VN">Tiếng Việt (vi-VN)</option>
                    <option value="en-US">English (en-US)</option>
                    <option value="en-GB">English (en-GB)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-ink uppercase mb-1">Giới Tính / Tone</label>
                  <select 
                    value={newVoiceGender}
                    onChange={(e) => setNewVoiceGender(e.target.value)}
                    className="w-full px-3 py-2 text-xs sm:text-sm bg-white border-2 border-green-500 rounded-lg focus:outline-none"
                  >
                    <option value="male">Nam (Sư phạm trầm)</option>
                    <option value="female">Nữ (Trong trẻo chuẩn)</option>
                    <option value="modern-male">Nam (Hiện đại trung tính)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-ink uppercase mb-1">Neural Model Engine</label>
                <input 
                  type="text" 
                  readOnly 
                  value="Neural-vi-VN-WaveNet-V3"
                  className="w-full px-3 py-2 text-xs sm:text-sm bg-surface-container border-2 border-green-500 rounded-lg font-mono text-ink"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-ink uppercase mb-1">Ghi Chú Phong Cách Sư Phạm</label>
                <textarea 
                  rows={2}
                  value={newVoicePrompt}
                  onChange={(e) => setNewVoicePrompt(e.target.value)}
                  placeholder="VD: Nhấn nhá từ khóa công nghệ, ngừng nghỉ rõ ràng khi đọc câu hỏi trắc nghiệm..."
                  className="w-full px-3 py-2 text-xs sm:text-sm bg-white border-2 border-green-500 rounded-lg focus:outline-none resize-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t-2 border-green-200">
                <button 
                  type="button"
                  onClick={() => setIsNewVoiceModalOpen(false)}
                  className="cursor-pointer px-4 py-2 text-xs sm:text-sm font-bold text-green-800 hover:text-ink transition"
                >
                  Hủy bỏ
                </button>
                <button 
                  type="submit"
                  className="cursor-pointer px-5 py-2 rounded-lg bg-pink-500 hover:bg-pink-400 text-ink text-xs sm:text-sm font-bold border-2 border-ink shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all"
                >
                  Tạo Hồ Sơ Mới
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ================= MODAL 2: KIỂM THỬ TẢI ÂM THANH (STRESS TEST) ================= */}
      {isStressTestModalOpen && (
        <div className="fixed inset-0 z-50 bg-ink/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150">
          <div className="bg-paper border-2 border-green-500 rounded-2xl w-full max-w-lg p-6 shadow-[6px_6px_0_#DEB9B5] space-y-4 animate-modal-entry text-ink">
            <div className="flex items-center justify-between border-b-2 border-green-200 pb-3">
              <div className="flex items-center gap-2">
                <Gauge className="w-5 h-5 text-green-700" />
                <h3 className="font-heading text-lg sm:text-xl font-bold text-ink">Kiểm Thử Tải Âm Thanh Stress Test</h3>
              </div>
              <button 
                type="button"
                onClick={() => setIsStressTestModalOpen(false)}
                className="cursor-pointer text-ink hover:text-pink-700 w-8 h-8 rounded-lg border border-green-400 flex items-center justify-center bg-green-50 transition"
                aria-label="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs sm:text-sm text-green-900 leading-normal">
              Khởi tạo 50 luồng streaming OPUS đồng thời qua WebSocket để đo kiểm tỷ lệ rớt gói (Packet Loss) và trễ jitter âm thanh thời gian thực.
            </p>

            <div className="space-y-3 bg-surface-container-low p-4 rounded-xl border border-green-300">
              <div className="flex justify-between text-xs font-bold text-ink">
                <span>Tiến Trình Kiểm Tra:</span>
                <span>{stressStatusText}</span>
              </div>
              <div className="w-full bg-green-200 h-3 rounded-full overflow-hidden border border-green-400">
                <div 
                  className="bg-pink-500 h-full transition-all duration-300 rounded-full" 
                  style={{ width: `${stressProgress}%` }}
                />
              </div>
              <div className="grid grid-cols-2 gap-2 text-xs text-green-900 pt-2 font-mono">
                <div>WebSocket Conns: <strong>50 Active</strong></div>
                <div>Avg Latency: <strong>22.4 ms</strong></div>
                <div>Buffer Underrun: <strong>0.00%</strong></div>
                <div>Audio Bitrate: <strong>128 kbps OPUS</strong></div>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2 border-t-2 border-green-200">
              <button 
                type="button"
                onClick={() => setIsStressTestModalOpen(false)}
                className="cursor-pointer px-4 py-2 text-xs sm:text-sm font-bold text-green-800 hover:text-ink transition"
              >
                Đóng
              </button>
              <button 
                type="button"
                disabled={isRunningStress}
                onClick={handleRunStressTest}
                className="cursor-pointer inline-flex items-center gap-2 px-5 py-2 rounded-lg bg-green-500 hover:bg-green-600 text-ink text-xs sm:text-sm font-bold border-2 border-ink shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all disabled:opacity-50"
              >
                <Play className="w-4 h-4 fill-current leading-none" />
                <span>{isRunningStress ? 'Đang Kiểm Thử...' : 'Bắt Đầu Stress Test'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminVoiceLabPage;
