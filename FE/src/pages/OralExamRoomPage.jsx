import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/userApi';
import { 
  Radio, 
  Mic, 
  MicOff, 
  Video, 
  CheckCircle2, 
  X, 
  AlertTriangle, 
  Clock, 
  Sparkles,
  Volume2,
  Check,
  Send,
  ArrowRight,
  ShieldAlert,
  FileCheck2,
  HelpCircle,
  RotateCcw
} from 'lucide-react';
import { VoiceWaveform } from '../components/common/VoiceWaveform';

const VIVA_QUESTIONS = [
  {
    round: 1,
    title: 'Vòng 1: Bản chất Kiến trúc Phân tầng (Onion Architecture)',
    question: 'Em hãy phân tích cấu trúc các tầng trong Onion Architecture và giải thích tại sao tầng Domain Core lại độc lập hoàn toàn với các Framework hay ORM?',
    hint: 'Tập trung vào Dependency Inversion Principle và Business Rules không phụ thuộc vào Infrastructure.',
    rubricCriteria: 'Hiểu rõ các lớp đồng tâm và ranh giới phân tách Domain / Application / Infrastructure.'
  },
  {
    round: 2,
    title: 'Vòng 2: Hỏi xoáy Thích ứng (Adaptive Probing - DIP)',
    question: 'Nếu tầng Application cần lưu trữ dữ liệu vào PostgreSQL, theo Onion Architecture thì Repository Interface sẽ được khai báo ở tầng nào và Implementation nằm ở tầng nào?',
    hint: 'Interface thuộc Application/Domain, JPA/Hibernate RepositoryImpl thuộc tầng Infrastructure.',
    rubricCriteria: 'Áp dụng chính xác Dependency Inversion: Tầng trong định nghĩa Contract, tầng ngoài thực thi.'
  },
  {
    round: 3,
    title: 'Vòng 3: Đảm bảo Độ tin cậy (Transactional Outbox Pattern)',
    question: 'Làm thế nào để đảm bảo dữ liệu chấm thi được lưu an toàn vào DB đồng thời gửi Event sang hệ thống bên ngoài mà không gặp sự cố Dual-Write khi rớt mạng?',
    hint: 'Transactional Outbox lưu Outbox Event cùng Transaction với kết quả thi trước khi Relay xử lý.',
    rubricCriteria: 'Nắm vững Transactional Outbox Pattern và tính bất biến (Eventual Consistency).'
  }
];

export function OralExamRoomPage({ onNavigate, showToast }) {
  const { currentUser } = useAuth();
  const [profile, setProfile] = useState(currentUser || null);
  const [secondsLeft, setSecondsLeft] = useState(900); // 15:00
  const [activeQuestionIndex, setActiveQuestionIndex] = useState(0);

  // Voice & Interaction states
  const [isAiSpeaking, setIsAiSpeaking] = useState(false);
  const [isCandidateRecording, setIsCandidateRecording] = useState(false);
  const [candidateAnswers, setCandidateAnswers] = useState({
    0: 'Trong kiến trúc Onion, Domain Core nằm ở trung tâm và chứa các Entities, Business Rules thuần túy. Nó không phụ thuộc vào bất kỳ thư viện ngoài nào như Spring Data JPA hay Hibernate. Mọi sự phụ thuộc đều hướng từ ngoài vào trong.',
    1: 'Repository Interface được định nghĩa ở tầng Application/Domain (Output Port). Implementation cụ thể (sử dụng Spring Data JPA hay JDBC) sẽ nằm ở tầng ngoài cùng là Infrastructure (Persistence Adapter).',
    2: 'Ta sử dụng Transactional Outbox Pattern: Khi chấm thi xong, điểm số và bản ghi sự kiện OutboxEvent được lưu vào cùng một Local Database Transaction. Một background worker sẽ đọc Outbox table và gửi sang hệ thống bên ngoài, tránh được lỗi Dual-Write.'
  });
  const [currentResponseText, setCurrentResponseText] = useState(candidateAnswers[0] || '');

  // Live Rubric Evaluation Status
  const [rubricScores, setRubricScores] = useState({
    0: { score: 9.5, status: 'PASSED', text: 'Đạt chuẩn Bloom 4: Phân tích kiến trúc chính xác' },
    1: { score: 9.0, status: 'PASSED', text: 'Đạt chuẩn DIP: Xác định đúng vị trí Interface và Adapter' },
    2: { score: 9.2, status: 'PASSED', text: 'Đạt chuẩn Resilience: Nắm vững Transactional Outbox' }
  });

  const [isFinishModalOpen, setIsFinishModalOpen] = useState(false);

  const recognitionRef = useRef(null);

  useEffect(() => {
    async function loadCandidateProfile() {
      try {
        const user = await userApi.getCurrentUser();
        setProfile(user);
      } catch (err) {
        // Safe fallback
      }
    }
    loadCandidateProfile();

    const interval = setInterval(() => {
      setSecondsLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => {
      clearInterval(interval);
      if (recognitionRef.current) {
        try { recognitionRef.current.stop(); } catch (e) {}
      }
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }
    };
  }, []);

  // Escape key closes modal
  useEffect(() => {
    if (!isFinishModalOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') setIsFinishModalOpen(false);
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isFinishModalOpen]);

  const studentName = profile?.fullName || currentUser?.fullName || 'Nguyễn Văn An';
  const studentCode = profile?.userCode || currentUser?.userCode || 'SE170001';

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  const currentQ = VIVA_QUESTIONS[activeQuestionIndex];

  // AI Examiner Voice Synthesis
  const speakQuestion = () => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utter = new SpeechSynthesisUtterance(currentQ.question);
      utter.lang = 'vi-VN';
      utter.rate = 1.0;
      utter.onstart = () => setIsAiSpeaking(true);
      utter.onend = () => setIsAiSpeaking(false);
      utter.onerror = () => setIsAiSpeaking(false);
      window.speechSynthesis.speak(utter);
      if (showToast) {
        showToast({
          type: 'info',
          title: 'Giám Thị AI Dr. Sophia',
          message: 'Đang phát âm câu hỏi khảo thí bằng giọng tiếng Việt tự nhiên...'
        });
      }
    }
  };

  // Student Recording Handler with Speech-to-Text
  const toggleRecording = () => {
    if (isCandidateRecording) {
      // Stop recording
      if (recognitionRef.current) {
        try { recognitionRef.current.stop(); } catch (e) {}
      }
      setIsCandidateRecording(false);
      // Save answer
      setCandidateAnswers(prev => ({ ...prev, [activeQuestionIndex]: currentResponseText }));
    } else {
      // Start recording
      setIsCandidateRecording(true);
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (SpeechRecognition) {
        const recognition = new SpeechRecognition();
        recognition.lang = 'vi-VN';
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.onresult = (event) => {
          let text = '';
          for (let i = event.resultIndex; i < event.results.length; ++i) {
            text += event.results[i][0].transcript;
          }
          if (text.trim()) {
            setCurrentResponseText(prev => `${prev ? prev + ' ' : ''}${text}`);
          }
        };
        recognition.onerror = () => setIsCandidateRecording(false);
        recognition.onend = () => setIsCandidateRecording(false);
        recognition.start();
        recognitionRef.current = recognition;
      }
    }
  };

  const handleSelectQuestion = (idx) => {
    // Save current response
    setCandidateAnswers(prev => ({ ...prev, [activeQuestionIndex]: currentResponseText }));
    setActiveQuestionIndex(idx);
    setCurrentResponseText(candidateAnswers[idx] || '');
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    setIsAiSpeaking(false);
    if (isCandidateRecording) {
      toggleRecording();
    }
  };

  const handleNextQuestion = () => {
    setCandidateAnswers(prev => ({ ...prev, [activeQuestionIndex]: currentResponseText }));
    if (activeQuestionIndex < VIVA_QUESTIONS.length - 1) {
      handleSelectQuestion(activeQuestionIndex + 1);
    } else {
      setIsFinishModalOpen(true);
    }
  };

  const handleFinishExam = () => {
    setIsFinishModalOpen(false);
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Hoàn Tất Bài Thi Vấn Đáp',
        message: 'Đã nộp bài thi thành công! Kết quả đã được mã hóa SHA-256 và chuyển giao sang Module 4.'
      });
    }
    if (onNavigate) {
      onNavigate('submission-success');
    }
  };

  return (
    <div className="space-y-6 animate-modal-entry text-slate-800 pb-12">
      
      {/* Top Header Card */}
      <div className="bg-white border border-slate-200/90 p-5 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-2xs">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-2xl bg-sky-50 border border-sky-200 text-sky-600 flex items-center justify-center text-xl shrink-0">
            🎙️
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-heading font-extrabold text-base sm:text-lg text-slate-900">
                Phòng Thi Vấn Đáp Trực Tuyến (Oral Exam Room)
              </h1>
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300 flex items-center gap-1">
                <Radio className="w-3 h-3 text-emerald-600 animate-pulse" />
                STOMP Realtime
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Thí sinh: <strong>{studentName} ({studentCode})</strong> • Học phần: <strong>SWD392 - Software Architecture</strong>
            </p>
          </div>
        </div>

        {/* Timer & Submission Actions */}
        <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
          <div className="text-right font-mono bg-slate-50 border border-slate-200 px-3.5 py-1.5 rounded-xl">
            <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Thời Gian Ca Thi</span>
            <p className="text-sm sm:text-base font-extrabold text-slate-900">{formatTime(secondsLeft)}</p>
          </div>

          <button
            type="button"
            onClick={handleFinishExam}
            className="px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition flex items-center gap-1 cursor-pointer"
            title="Nộp thử bài thi ngay lập tức"
          >
            <FileCheck2 className="w-3.5 h-3.5 text-slate-500" />
            <span className="hidden sm:inline">Test Nộp Nhanh</span>
          </button>

          <button 
            type="button"
            onClick={() => setIsFinishModalOpen(true)} 
            className="bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs px-5 py-2.5 rounded-xl shadow-xs active:scale-95 transition cursor-pointer"
          >
            Nộp Bài Thi ↗
          </button>
        </div>
      </div>

      {/* Question Stepper Tabs */}
      <div className="grid grid-cols-3 gap-2.5 text-xs select-none">
        {VIVA_QUESTIONS.map((q, idx) => {
          const isActive = activeQuestionIndex === idx;
          const isAnswered = !!candidateAnswers[idx];

          return (
            <button
              key={q.round}
              type="button"
              onClick={() => handleSelectQuestion(idx)}
              className={`p-3 rounded-2xl border text-left transition flex items-center justify-between cursor-pointer ${
                isActive
                  ? 'bg-sky-50 border-sky-400 text-sky-950 font-bold ring-2 ring-sky-500/20 shadow-xs'
                  : isAnswered
                    ? 'bg-emerald-50/60 border-emerald-300 text-emerald-900 font-medium'
                    : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              <div className="flex items-center gap-2 truncate">
                <span className={`w-5 h-5 rounded-lg flex items-center justify-center text-[10px] font-bold shrink-0 ${
                  isAnswered ? 'bg-emerald-600 text-white' : isActive ? 'bg-sky-600 text-white' : 'bg-slate-200 text-slate-700'
                }`}>
                  {q.round}
                </span>
                <span className="truncate text-xs">Vòng {q.round}</span>
              </div>
              {isAnswered && <span className="text-[10px] text-emerald-700 font-bold shrink-0">Đã trả lời ✓</span>}
            </button>
          );
        })}
      </div>

      {/* Main 2-Column Viva Exam Studio */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Column 1: AI Examiner & Candidate Speech Workspace (7 cols) */}
        <div className="lg:col-span-7 space-y-4">
          
          {/* Question Presentation Card */}
          <div className="bg-white border border-slate-200/90 p-6 rounded-3xl space-y-4 shadow-2xs">
            <div className="flex items-start justify-between gap-3 pb-3 border-b border-slate-100">
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider text-sky-600">
                  {currentQ.title}
                </span>
                <h2 className="font-heading font-extrabold text-base sm:text-lg text-slate-900 mt-1 leading-snug">
                  {currentQ.question}
                </h2>
              </div>

              <button
                type="button"
                onClick={speakQuestion}
                className="shrink-0 p-2.5 rounded-xl bg-sky-50 hover:bg-sky-100 text-sky-700 border border-sky-200 transition flex items-center gap-1.5 text-xs font-bold cursor-pointer"
                title="Nghe Giám thị AI phát âm câu hỏi"
              >
                <Volume2 className="w-4 h-4" />
                <span className="hidden sm:inline">{isAiSpeaking ? 'Đang đọc...' : 'Nghe câu hỏi'}</span>
              </button>
            </div>

            <div className="p-3 bg-slate-50 border border-slate-200/70 rounded-xl text-slate-600 text-xs flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-sky-600 shrink-0" />
              <span><strong>Gợi ý sư phạm:</strong> {currentQ.hint}</span>
            </div>
          </div>

          {/* Candidate Voice Answering Area */}
          <div className="bg-white border border-slate-200/90 p-6 rounded-3xl space-y-4 shadow-2xs">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className={`w-2.5 h-2.5 rounded-full ${isCandidateRecording ? 'bg-rose-500 animate-pulse' : 'bg-emerald-500'}`}></span>
                <span className="font-heading font-extrabold text-sm text-slate-900">
                  Nội Dung Câu Trả Lời Của Thí Sinh (Bóc Băng &amp; Biên Tập)
                </span>
              </div>

              <button
                type="button"
                onClick={toggleRecording}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 cursor-pointer shadow-xs active:scale-95 ${
                  isCandidateRecording
                    ? 'bg-rose-600 hover:bg-rose-500 text-white animate-pulse'
                    : 'bg-slate-900 hover:bg-slate-800 text-white'
                }`}
              >
                {isCandidateRecording ? <MicOff className="w-3.5 h-3.5" /> : <Mic className="w-3.5 h-3.5 text-sky-400" />}
                <span>{isCandidateRecording ? 'Dừng Nói (Save Voice)' : '🎙️ Bật Mic Trả Lời'}</span>
              </button>
            </div>

            {/* Live Audio Visualizer Banner */}
            {isCandidateRecording && (
              <div className="p-3 bg-slate-950 text-white rounded-2xl border border-slate-800 flex items-center justify-between text-xs animate-in fade-in">
                <div className="flex items-center gap-2">
                  <VoiceWaveform isSpeaking={true} volume={0.8} barCount={8} height={18} colorClass="bg-rose-500" />
                  <span className="text-[11px] font-mono text-rose-300 font-bold">Đang thu âm microphone thời gian thực...</span>
                </div>
                <span className="text-[10px] font-mono text-slate-400">-12 dB Optimal</span>
              </div>
            )}

            {/* Response Textarea */}
            <div>
              <textarea
                rows={5}
                value={currentResponseText}
                onChange={(e) => setCurrentResponseText(e.target.value)}
                placeholder="Câu trả lời của bạn sẽ được tự động bóc băng qua giọng nói, hoặc bạn có thể nhập/bổ sung văn bản trực tiếp tại đây..."
                className="w-full p-4 rounded-2xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800 text-xs transition leading-relaxed placeholder:text-slate-400 font-medium"
              />
            </div>

            {/* Step navigation bar */}
            <div className="flex items-center justify-between pt-2">
              <span className="text-[11px] text-slate-400">
                Câu hỏi {activeQuestionIndex + 1} / {VIVA_QUESTIONS.length}
              </span>

              <button
                type="button"
                onClick={handleNextQuestion}
                className="bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs px-6 py-2.5 rounded-xl shadow-xs transition flex items-center gap-1.5 cursor-pointer active:scale-95"
              >
                <span>{activeQuestionIndex < VIVA_QUESTIONS.length - 1 ? 'Lưu & Sang Câu Tiếp Theo →' : 'Xem Lại & Nộp Bài ↗'}</span>
              </button>
            </div>
          </div>

        </div>

        {/* Column 2: Candidate Video & Live Rubric Scoring (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          
          {/* Candidate Webcam Feed & VU Meter */}
          <div className="bg-white border border-slate-200/90 p-4 rounded-3xl space-y-3 shadow-2xs">
            <div className="aspect-video bg-slate-900 rounded-2xl overflow-hidden relative">
              <img 
                src="https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&auto=format&fit=crop&q=80" 
                alt="Thí Sinh Dự Thi" 
                className="w-full h-full object-cover" 
              />
              <span className="absolute top-2.5 left-2.5 bg-slate-900/80 backdrop-blur-xs text-white text-[10px] font-bold px-2.5 py-1 rounded-full flex items-center gap-1 border border-slate-700">
                <Video className="w-3 h-3 text-emerald-400" />
                CAM 30FPS • ĐÚNG GÓC NHÌN
              </span>
            </div>

            {/* Microphone VU Meter */}
            <div>
              <div className="flex justify-between text-[11px] font-bold text-slate-700 mb-1">
                <span>Trạng Thái Micro Thí Sinh</span>
                <span className="text-emerald-600 font-mono">-12 dB (Tín hiệu tốt)</span>
              </div>
              <div className="w-full h-2 rounded-full bg-slate-100 overflow-hidden flex">
                <div className="h-full bg-emerald-500 w-3/4"></div>
                <div className="h-full bg-amber-400 w-1/6"></div>
                <div className="h-full bg-rose-500 w-1/12"></div>
              </div>
            </div>
          </div>

          {/* Rubric Evaluation Progress Matrix */}
          <div className="bg-white border border-slate-200/90 p-5 rounded-3xl space-y-3 shadow-2xs">
            <div className="flex items-center justify-between">
              <h3 className="font-heading font-extrabold text-xs uppercase tracking-wider text-slate-900">
                Đối Chiếu Ma Trận Rubric (Realtime)
              </h3>
              <span className="text-[10px] font-mono text-emerald-600 font-bold bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                AI Chấm Tự Động
              </span>
            </div>

            <div className="space-y-2 text-xs">
              {VIVA_QUESTIONS.map((q, idx) => {
                const scoreInfo = rubricScores[idx];
                const isCurrent = activeQuestionIndex === idx;

                return (
                  <div 
                    key={q.round} 
                    className={`p-3 rounded-2xl border transition ${
                      isCurrent
                        ? 'bg-sky-50/80 border-sky-300 ring-1 ring-sky-400/30'
                        : 'bg-slate-50 border-slate-200/80'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-bold text-slate-900">
                        Tiêu chí {q.round}: {q.title.split(':')[1] || q.title}
                      </span>
                      <span className="font-mono font-bold text-emerald-700 bg-emerald-100/80 px-2 py-0.5 rounded-lg text-[10px]">
                        {scoreInfo.score} / 10
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-500 leading-snug">
                      {scoreInfo.text}
                    </p>
                  </div>
                );
              })}
            </div>

            <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs">
              <span className="font-bold text-slate-600">Điểm Ước Tính Trung Bình:</span>
              <span className="font-heading font-extrabold text-sm text-emerald-600">9.2 / 10 (Xuất sắc)</span>
            </div>
          </div>

        </div>

      </div>

      {/* FINISH EXAM CONFIRMATION MODAL */}
      {isFinishModalOpen && (
        <div 
          className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800 text-center">
            <div className="w-14 h-14 rounded-2xl bg-sky-50 text-sky-600 mx-auto flex items-center justify-center border border-sky-100 shadow-xs">
              <FileCheck2 className="w-7 h-7 text-sky-600" />
            </div>
            
            <div>
              <h3 className="font-heading font-extrabold text-lg text-slate-900">
                Xác Nhận Nộp Bài Thi Vấn Đáp?
              </h3>
              <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                Bạn đã hoàn thành <strong>3/3 câu hỏi</strong> vấn đáp cùng Giám thị AI. Bài làm sẽ được mã hóa SHA-256 và chuyển giao lưu trữ vào sổ điểm khảo thí FAP.
              </p>
            </div>

            {/* Answer Summary Card */}
            <div className="bg-slate-50 border border-slate-200 rounded-2xl p-3.5 text-left text-xs space-y-2">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Tóm Tắt Bài Thi</span>
              <div className="space-y-1.5 text-[11px] text-slate-700">
                <div className="flex items-center justify-between">
                  <span>Thí sinh:</span>
                  <strong className="text-slate-900">{studentName} ({studentCode})</strong>
                </div>
                <div className="flex items-center justify-between">
                  <span>Thời gian làm bài:</span>
                  <strong>{formatTime(900 - secondsLeft)}</strong>
                </div>
                <div className="flex items-center justify-between">
                  <span>Số lượt câu hỏi đã trả lời:</span>
                  <strong className="text-emerald-600">3 / 3 (Hoàn thành 100%)</strong>
                </div>
              </div>
            </div>

            <div className="pt-2 flex items-center justify-center gap-3">
              <button 
                type="button"
                onClick={() => setIsFinishModalOpen(false)} 
                className="px-5 py-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-xs font-semibold text-slate-700 transition"
              >
                Tiếp Tục Làm Bài
              </button>
              <button 
                type="button"
                onClick={handleFinishExam} 
                className="px-6 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold shadow-md shadow-sky-600/25 active:scale-95 transition cursor-pointer"
              >
                Xác Nhận Nộp Bài Ngay
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
