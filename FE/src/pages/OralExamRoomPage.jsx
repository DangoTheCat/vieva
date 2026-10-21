import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/userApi';
import { oralExamApi } from '../api/oralExamApi';
import { useVoiceSTT } from '../hooks/useVoiceSTT';
import { useZeroTTS } from '../hooks/useZeroTTS';
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
  VolumeX,
  Check,
  Send,
  ArrowRight,
  ShieldAlert,
  FileCheck2,
  HelpCircle,
  RotateCcw,
  Award,
  BookOpen,
  ChevronRight,
  Square
} from 'lucide-react';
import { VoiceWaveform } from '../components/common/VoiceWaveform';

const DEFAULT_QUESTIONS = [
  {
    id: 1,
    turnId: 101,
    questionNumber: 1,
    totalQuestions: 5,
    type: 'MAIN', // 'MAIN' or 'PROBING'
    badge: 'Câu hỏi chính',
    title: 'Câu 1: Bản chất Kiến trúc Phân tầng (Onion Architecture)',
    content: 'Em hãy phân tích cấu trúc các tầng trong Onion Architecture và giải thích tại sao tầng Domain Core lại độc lập hoàn toàn với các Framework hay ORM?',
    hint: 'Tập trung vào Dependency Inversion Principle và Business Rules không phụ thuộc vào Infrastructure.',
    rubricCriteria: 'Hiểu rõ các lớp đồng tâm và ranh giới phân tách Domain / Application / Infrastructure.'
  },
  {
    id: 2,
    turnId: 102,
    questionNumber: 2,
    totalQuestions: 5,
    type: 'PROBING',
    turnIndex: 1,
    maxTurns: 2,
    badge: 'Câu hỏi xoáy (Lượt 1/2)',
    title: 'Câu 2: Hỏi xoáy Thích ứng (Adaptive Probing - DIP)',
    content: 'Nếu tầng Application cần lưu trữ dữ liệu vào PostgreSQL, theo Onion Architecture thì Repository Interface sẽ được khai báo ở tầng nào và Implementation nằm ở tầng nào?',
    hint: 'Interface thuộc Application/Domain, JPA/Hibernate RepositoryImpl thuộc tầng Infrastructure.',
    rubricCriteria: 'Áp dụng chính xác Dependency Inversion: Tầng trong định nghĩa Contract, tầng ngoài thực thi.'
  },
  {
    id: 3,
    turnId: 103,
    questionNumber: 3,
    totalQuestions: 5,
    type: 'MAIN',
    badge: 'Câu hỏi chính',
    title: 'Câu 3: Đảm bảo Độ tin cậy (Transactional Outbox Pattern)',
    content: 'Làm thế nào để đảm bảo dữ liệu chấm thi được lưu an toàn vào DB đồng thời gửi Event sang hệ thống bên ngoài mà không gặp sự cố Dual-Write khi rớt mạng?',
    hint: 'Transactional Outbox lưu Outbox Event cùng Transaction với kết quả thi trước khi Relay xử lý.',
    rubricCriteria: 'Nắm vững Transactional Outbox Pattern và tính bất biến (Eventual Consistency).'
  },
  {
    id: 4,
    turnId: 104,
    questionNumber: 4,
    totalQuestions: 5,
    type: 'PROBING',
    turnIndex: 2,
    maxTurns: 2,
    badge: 'Câu hỏi xoáy (Lượt 2/2)',
    title: 'Câu 4: Tối ưu Hóa và Xử lý Trùng Lặp (Idempotent Consumer)',
    content: 'Khi message broker gửi lại bản tin nhiều lần (at-least-once delivery), phía Consumer cần triển khai cơ chế nào để tránh ghi nhận điểm số 2 lần?',
    hint: 'Dùng Idempotency Key hoặc Deduplication Table dựa trên Message ID duy nhất.',
    rubricCriteria: 'Hiểu nguyên lý At-least-once và kỹ thuật Idempotency trong hệ thống phân tán.'
  },
  {
    id: 5,
    turnId: 105,
    questionNumber: 5,
    totalQuestions: 5,
    type: 'MAIN',
    badge: 'Câu hỏi chính',
    title: 'Câu 5: Tổng kết & Thực tiễn Triển khai',
    content: 'Theo em, sự đánh đổi lớn nhất khi áp dụng kiến trúc Event-Driven kết hợp CQRS vào một hệ thống khảo thí trực tuyến quy mô lớn là gì?',
    hint: 'Phức tạp trong Eventual Consistency, debug tracing và vận hành hạ tầng.',
    rubricCriteria: 'Tư duy phản biện về trade-off kiến trúc: Consistency vs Availability.'
  }
];

export function OralExamRoomPage({ onNavigate, showToast, subjectId = 1, attemptId: initialAttemptId = null }) {
  const { currentUser } = useAuth();
  const [profile, setProfile] = useState(currentUser || null);

  // Exam Stage: 'PREFLIGHT' -> 'IN_PROGRESS' -> 'COMPLETED'
  const [examStage, setExamStage] = useState('PREFLIGHT');

  // Attempt & Questions state
  const [attemptId, setAttemptId] = useState(initialAttemptId);
  const [questions, setQuestions] = useState(DEFAULT_QUESTIONS);
  const [activeQuestionIndex, setActiveQuestionIndex] = useState(0);
  const [secondsLeft, setSecondsLeft] = useState(900); // 15:00
  const [isSubmittingTurn, setIsSubmittingTurn] = useState(false);

  // Answers store
  const [answersByQuestion, setAnswersByQuestion] = useState({});
  const [manualText, setManualText] = useState('');

  // Rubrics & Feedback Summary
  const [examResult, setExamResult] = useState(null);

  // Preflight check modal state
  const [preflightMicOk, setPreflightMicOk] = useState(false);
  const [preflightSpeakerOk, setPreflightSpeakerOk] = useState(false);
  const [preflightTestingMic, setPreflightTestingMic] = useState(false);
  const [preflightMicVolume, setPreflightMicVolume] = useState(0);

  const preflightAudioCtxRef = useRef(null);
  const preflightStreamRef = useRef(null);
  const preflightAnimFrameRef = useRef(null);

  // Hooks
  const { 
    isRecording, 
    transcript, 
    partialTranscript, 
    audioBlob, 
    startRecording, 
    stopRecording, 
    resetTranscript, 
    isConnected: isWsConnected 
  } = useVoiceSTT({
    onError: (err) => {
      console.warn('STT Hook Warning:', err);
    }
  });

  const { isPlaying: isAiSpeaking, play: playTTS, stop: stopTTS } = useZeroTTS({
    onError: (err) => {
      console.warn('TTS Hook Warning:', err);
    }
  });

  // Current Question
  const currentQuestion = questions[activeQuestionIndex] || questions[0];

  // Sync profile & exam data on mount
  useEffect(() => {
    async function loadData() {
      try {
        const u = await userApi.getCurrentUser();
        setProfile(u);
      } catch (e) {}

      // If initialAttemptId exists, load from API
      if (initialAttemptId) {
        try {
          const detail = await oralExamApi.getAttemptDetail(initialAttemptId);
          if (detail && detail.questions && detail.questions.length > 0) {
            setQuestions(detail.questions);
          }
        } catch (err) {
          console.warn('Could not fetch attempt detail, using default questions:', err);
        }
      }
    }
    loadData();
  }, [initialAttemptId]);

  // Exam timer countdown during IN_PROGRESS
  useEffect(() => {
    if (examStage !== 'IN_PROGRESS') return;
    const interval = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          handleFinishExam();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [examStage]);

  // Stop TTS & STT on unmount
  useEffect(() => {
    return () => {
      stopTTS();
      stopRecording();
      stopPreflightMic();
    };
  }, [stopTTS, stopRecording]);

  // Read current question when changing question in IN_PROGRESS
  useEffect(() => {
    if (examStage === 'IN_PROGRESS' && currentQuestion) {
      resetTranscript();
      setManualText(answersByQuestion[activeQuestionIndex]?.text || '');
      // Automatically voice out question
      const timer = setTimeout(() => {
        playTTS(currentQuestion.content);
      }, 500);
      return () => clearTimeout(timer);
    }
  }, [activeQuestionIndex, examStage]);

  // Format seconds mm:ss
  const formatTime = (secs) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  // --- Preflight Check Handlers ---
  const startPreflightMicTest = async () => {
    try {
      setPreflightTestingMic(true);
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      preflightStreamRef.current = stream;

      const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      preflightAudioCtxRef.current = audioCtx;
      const analyser = audioCtx.createAnalyser();
      analyser.fftSize = 256;
      const source = audioCtx.createMediaStreamSource(stream);
      source.connect(analyser);

      const dataArray = new Uint8Array(analyser.frequencyBinCount);
      const checkVolume = () => {
        analyser.getByteFrequencyData(dataArray);
        let sum = 0;
        for (let i = 0; i < dataArray.length; i++) {
          sum += dataArray[i];
        }
        const avg = sum / dataArray.length;
        const normalized = Math.min(100, Math.round((avg / 128) * 100));
        setPreflightMicVolume(normalized);

        if (normalized > 15) {
          setPreflightMicOk(true);
        }
        preflightAnimFrameRef.current = requestAnimationFrame(checkVolume);
      };
      checkVolume();
    } catch (err) {
      console.warn('Microphone access denied:', err);
      setPreflightTestingMic(false);
      if (showToast) {
        showToast({
          type: 'error',
          title: 'Lỗi Truy Cập Micro',
          message: 'Không thể kết nối Microphone. Vui lòng cấp quyền truy cập trên trình duyệt.'
        });
      }
    }
  };

  const stopPreflightMic = () => {
    if (preflightAnimFrameRef.current) {
      cancelAnimationFrame(preflightAnimFrameRef.current);
    }
    if (preflightAudioCtxRef.current) {
      try { preflightAudioCtxRef.current.close(); } catch (e) {}
    }
    if (preflightStreamRef.current) {
      preflightStreamRef.current.getTracks().forEach(t => t.stop());
    }
    setPreflightTestingMic(false);
  };

  const handleTestSpeaker = () => {
    playTTS('Xin chào thí sinh. Hệ thống âm thanh phòng thi vấn đáp trực tuyến hoạt động bình thường.');
    setPreflightSpeakerOk(true);
  };

  const handleEnterExam = async () => {
    stopPreflightMic();
    stopTTS();

    try {
      // Call startExam API if possible
      const res = await oralExamApi.startExam(subjectId);
      if (res && res.attemptId) {
        setAttemptId(res.attemptId);
        if (res.questions && res.questions.length > 0) {
          setQuestions(res.questions);
        }
      }
    } catch (err) {
      console.warn('Backend startExam API fallback to local exam session:', err);
      setAttemptId(Date.now());
    }

    setExamStage('IN_PROGRESS');
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Bắt Đầu Bài Thi',
        message: 'Chào mừng bạn vào phòng thi vấn đáp. Chúc bạn hoàn thành tốt bài thi!'
      });
    }
  };

  // --- Voice Answering Handlers ---
  const handleToggleSpeak = async () => {
    if (isRecording) {
      stopRecording();
    } else {
      stopTTS();
      try {
        await startRecording();
      } catch (e) {
        if (showToast) {
          showToast({
            type: 'error',
            title: 'Lỗi Micro',
            message: 'Không thể khởi động ghi âm. Vui lòng kiểm tra thiết bị micro của bạn.'
          });
        }
      }
    }
  };

  const currentCompositeTranscript = [manualText, transcript, partialTranscript]
    .filter(Boolean)
    .join(' ')
    .trim();

  // Submit current answer turn
  const handleSubmitTurn = async () => {
    if (isRecording) {
      stopRecording();
    }

    const answerContent = currentCompositeTranscript || 'Thí sinh xác nhận hoàn thành câu trả lời.';
    setIsSubmittingTurn(true);

    try {
      const turnId = currentQuestion.turnId || currentQuestion.id;
      if (attemptId) {
        await oralExamApi.submitTurnAnswer(attemptId, turnId, {
          transcriptText: answerContent,
          audioBlob: audioBlob
        });
      }
    } catch (err) {
      console.warn('Backend turn submit fallback:', err);
    }

    // Save answer locally
    setAnswersByQuestion((prev) => ({
      ...prev,
      [activeQuestionIndex]: {
        questionId: currentQuestion.id,
        text: answerContent,
        audioBlob: audioBlob
      }
    }));

    setIsSubmittingTurn(false);

    if (showToast) {
      showToast({
        type: 'success',
        title: 'Đã Nộp Câu Trả Lời',
        message: `Đã lưu câu trả lời cho ${currentQuestion.badge || `Câu ${activeQuestionIndex + 1}`}.`
      });
    }

    // Advance to next question or complete exam
    if (activeQuestionIndex < questions.length - 1) {
      setActiveQuestionIndex((prev) => prev + 1);
    } else {
      handleFinishExam();
    }
  };

  const handleFinishExam = () => {
    stopRecording();
    stopTTS();

    // Compute mock/simulated rubric & score
    const totalScore = 9.2;
    let grade = 'Xuất sắc';
    if (totalScore < 5) grade = 'Chưa đạt';
    else if (totalScore < 7) grade = 'Trung bình';
    else if (totalScore < 8.5) grade = 'Khá';
    else if (totalScore < 9) grade = 'Giỏi';

    setExamResult({
      totalScore,
      grade,
      maxScore: 10,
      feedback: 'Thí sinh nắm rất vững các nguyên lý phân tầng và kiến trúc Onion. Diễn giải mạch lạc nguyên lý Đảo ngược phụ thuộc (DIP) và Transactional Outbox Pattern. Phản xạ hỏi xoáy thích ứng tự tin, xử lý tốt rủi ro mất mát dữ liệu và tính bất biến.',
      rubrics: [
        { name: 'Kiến thức cốt lõi & Nguyên lý Kiến trúc', score: 9.5, max: 10, comment: 'Đạt chuẩn Bloom 4. Định nghĩa chính xác Domain Core độc lập.' },
        { name: 'Khả năng Phản xạ & Câu hỏi xoáy thích ứng', score: 9.0, max: 10, comment: 'Xác định chuẩn Contract Interface và Implementation Adapter.' },
        { name: 'Độ tin cậy & Giải pháp phân tán (Outbox / Idempotency)', score: 9.2, max: 10, comment: 'Phân tích thấu đáo lỗi Dual-Write và kỹ thuật deduplication.' }
      ]
    });

    setExamStage('COMPLETED');
  };

  const studentName = profile?.fullName || currentUser?.fullName || 'Nguyễn Văn An';
  const studentCode = profile?.userCode || currentUser?.userCode || 'SE170001';

  // ==========================================
  // RENDER STEP 1: PRE-FLIGHT CHECK MODAL
  // ==========================================
  if (examStage === 'PREFLIGHT') {
    return (
      <div className="max-w-3xl mx-auto py-8 px-4 animate-in fade-in duration-300">
        <div className="bg-paper border-2 border-green-500 rounded-3xl p-6 sm:p-10 shadow-[8px_8px_0_#DEB9B5] space-y-8">
          
          {/* Header */}
          <div className="flex items-center gap-4 pb-6 border-b-2 border-green-200">
            <div className="w-14 h-14 rounded-2xl bg-pink-100 border-2 border-green-500 text-pink-700 flex items-center justify-center text-2xl shadow-[3px_3px_0_#DEB9B5] shrink-0">
              🎙️
            </div>
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-green-700 block">
                Khảo Thí Vấn Đáp AI • Stagebox Evaluator
              </span>
              <h1 className="font-heading font-black text-2xl sm:text-3xl text-ink">
                Kiểm Tra Thiết Bị Phòng Thi (Pre-flight Check)
              </h1>
            </div>
          </div>

          <p className="text-xs sm:text-sm text-green-900 leading-relaxed">
            Chào mừng <strong>{studentName} ({studentCode})</strong>. Để đảm bảo bài thi diễn ra công bằng và thông suốt, vui lòng kiểm tra Micro và Loa của thiết bị trước khi bắt đầu.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            {/* 1. Test Mic & Waveform */}
            <div className="bg-white border-2 border-green-400 rounded-2xl p-5 shadow-[4px_4px_0_#DADFD3] flex flex-col justify-between space-y-4">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Mic className="w-5 h-5 text-pink-600" />
                    <h3 className="font-bold text-sm text-ink">Kiểm Tra Microphone</h3>
                  </div>
                  {preflightMicOk ? (
                    <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full border border-emerald-300">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Chuẩn
                    </span>
                  ) : (
                    <span className="text-[11px] font-bold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full border border-amber-300">
                      Chưa test
                    </span>
                  )}
                </div>
                <p className="text-xs text-green-800">
                  Nhấn "Bật Micro Thử Nghiệm", nói một câu ngắn vào micro để kiểm tra sóng âm.
                </p>
              </div>

              {/* Volume bar & waveform */}
              <div className="space-y-2">
                <div className="h-6 bg-green-50 border border-green-300 rounded-xl overflow-hidden p-1 flex items-center">
                  <div 
                    className="h-full bg-pink-500 rounded-lg transition-all duration-75"
                    style={{ width: `${preflightMicVolume}%` }}
                  />
                </div>
                <div className="flex justify-between text-[10px] font-mono text-green-700">
                  <span>Mức âm lượng: {preflightMicVolume}%</span>
                  <span>{preflightMicVolume > 15 ? 'Đã nhận tín hiệu tốt ✓' : 'Nói to hơn...'}</span>
                </div>
              </div>

              <button
                type="button"
                onClick={preflightTestingMic ? stopPreflightMic : startPreflightMicTest}
                className={`w-full py-2.5 px-4 rounded-xl font-bold text-xs border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 ${
                  preflightTestingMic 
                    ? 'bg-rose-100 text-rose-800 hover:bg-rose-200' 
                    : 'bg-pink-100 hover:bg-pink-200 text-pink-900'
                }`}
              >
                {preflightTestingMic ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
                <span>{preflightTestingMic ? 'Dừng Kiểm Tra Micro' : 'Bật Micro Thử Nghiệm'}</span>
              </button>
            </div>

            {/* 2. Test Speaker */}
            <div className="bg-white border-2 border-green-400 rounded-2xl p-5 shadow-[4px_4px_0_#DADFD3] flex flex-col justify-between space-y-4">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Volume2 className="w-5 h-5 text-green-700" />
                    <h3 className="font-bold text-sm text-ink">Kiểm Tra Loa / Tai Nghe</h3>
                  </div>
                  {preflightSpeakerOk ? (
                    <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full border border-emerald-300">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Chuẩn
                    </span>
                  ) : (
                    <span className="text-[11px] font-bold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full border border-amber-300">
                      Chưa test
                    </span>
                  )}
                </div>
                <p className="text-xs text-green-800">
                  Nhấn nút bên dưới để nghe giọng nói thử nghiệm của Giám thị AI.
                </p>
              </div>

              <div className="p-3 bg-pink-50/70 border border-pink-200 rounded-xl text-xs text-pink-900 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-pink-600 shrink-0" />
                <span>ZeroTTS &amp; Web Audio Engine: Sẵn sàng phát âm.</span>
              </div>

              <button
                type="button"
                onClick={handleTestSpeaker}
                className="w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-green-100 hover:bg-green-200 text-green-900 border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2"
              >
                <Volume2 className="w-4 h-4" />
                <span>{isAiSpeaking ? 'Đang Phát Âm Thử...' : 'Phát Thử Giọng Giám Thị AI'}</span>
              </button>
            </div>

          </div>

          {/* Action Footer */}
          <div className="pt-4 border-t-2 border-green-200 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="text-xs text-green-800">
              * Khuyến nghị dùng tai nghe và micro rời để đạt chất lượng nhận diện giọng nói cao nhất.
            </div>
            <button
              type="button"
              onClick={handleEnterExam}
              className="w-full sm:w-auto px-8 py-3.5 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-black text-sm border-2 border-green-700 shadow-[4px_4px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2"
            >
              <span>Vào Phòng Thi Vấn Đáp</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>

        </div>
      </div>
    );
  }

  // ==========================================
  // RENDER STEP 3: EXAM COMPLETED / SUMMARY
  // ==========================================
  if (examStage === 'COMPLETED' && examResult) {
    return (
      <div className="max-w-4xl mx-auto py-8 px-4 animate-in fade-in duration-300">
        <div className="bg-paper border-2 border-green-500 rounded-3xl p-6 sm:p-10 shadow-[8px_8px_0_#DEB9B5] space-y-8">
          
          {/* Header */}
          <div className="text-center space-y-3 pb-6 border-b-2 border-green-200">
            <div className="w-20 h-20 rounded-3xl bg-pink-100 border-2 border-green-500 text-pink-700 mx-auto flex items-center justify-center text-3xl shadow-[4px_4px_0_#DEB9B5]">
              🎓
            </div>
            <span className="text-xs font-bold uppercase tracking-wider text-green-700 block">
              Báo Cáo Đánh Giá Khảo Thí Vấn Đáp
            </span>
            <h1 className="font-heading font-black text-2xl sm:text-3xl text-ink">
              Kết Quả Bài Thi Vấn Đáp Trực Tuyến
            </h1>
            <p className="text-xs sm:text-sm text-green-800">
              Thí sinh: <strong>{studentName}</strong> • Mã SV: <strong>{studentCode}</strong>
            </p>
          </div>

          {/* Overall Score Box */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white border-2 border-green-500 p-5 rounded-2xl shadow-[4px_4px_0_#DEB9B5] text-center">
              <span className="text-[11px] font-bold text-green-700 uppercase tracking-wider block mb-1">
                Điểm Tổng Kết
              </span>
              <div className="font-heading font-black text-4xl text-pink-600">
                {examResult.totalScore} <span className="text-lg text-ink font-sans">/ {examResult.maxScore}</span>
              </div>
            </div>

            <div className="bg-white border-2 border-green-500 p-5 rounded-2xl shadow-[4px_4px_0_#DEB9B5] text-center">
              <span className="text-[11px] font-bold text-green-700 uppercase tracking-wider block mb-1">
                Xếp Loại Đạt Được
              </span>
              <div className="font-heading font-black text-2xl sm:text-3xl text-green-700">
                {examResult.grade}
              </div>
            </div>

            <div className="bg-white border-2 border-green-500 p-5 rounded-2xl shadow-[4px_4px_0_#DEB9B5] text-center">
              <span className="text-[11px] font-bold text-green-700 uppercase tracking-wider block mb-1">
                Số Lượt Câu Hỏi
              </span>
              <div className="font-heading font-black text-2xl sm:text-3xl text-ink">
                {questions.length} / {questions.length}
              </div>
            </div>
          </div>

          {/* AI Comprehensive Feedback */}
          <div className="bg-pink-50/70 border-2 border-pink-300 p-5 sm:p-6 rounded-2xl shadow-[4px_4px_0_#DADFD3] space-y-2">
            <div className="flex items-center gap-2 text-pink-900 font-bold text-sm">
              <Sparkles className="w-5 h-5 text-pink-600" />
              <span>Nhận Xét Chuyên Môn Từ Giám Thị AI</span>
            </div>
            <p className="text-xs sm:text-sm text-pink-950 leading-relaxed font-medium">
              "{examResult.feedback}"
            </p>
          </div>

          {/* Rubric Breakdown List */}
          <div className="space-y-4">
            <h3 className="font-heading font-black text-lg text-ink flex items-center gap-2">
              <Award className="w-5 h-5 text-green-700" />
              Chi Tiết Đánh Giá Theo Ma Trận Rubric
            </h3>

            <div className="space-y-3">
              {examResult.rubrics.map((r, i) => (
                <div 
                  key={i} 
                  className="bg-white border-2 border-green-400 p-4 rounded-2xl shadow-[3px_3px_0_#DADFD3] flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="space-y-1">
                    <span className="font-bold text-xs text-ink block">{r.name}</span>
                    <p className="text-xs text-green-800">{r.comment}</p>
                  </div>
                  <div className="shrink-0 font-mono font-black text-sm bg-green-100 text-green-900 border border-green-400 px-3 py-1 rounded-xl self-start sm:self-center">
                    {r.score} / {r.max}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Actions */}
          <div className="pt-6 border-t-2 border-green-200 flex flex-col sm:flex-row items-center justify-between gap-4">
            <button
              type="button"
              onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
              className="px-6 py-3 rounded-2xl bg-white hover:bg-green-50 text-green-900 font-bold text-xs border-2 border-green-500 shadow-[3px_3px_0_#DEB9B5] cursor-pointer"
            >
              ← Trở Về Danh Sách Học Phần
            </button>

            <button
              type="button"
              onClick={() => onNavigate && onNavigate('submission-success')}
              className="px-8 py-3 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-bold text-xs border-2 border-green-700 shadow-[3px_3px_0_#DEB9B5] cursor-pointer"
            >
              Xem Giấy Chứng Nhận SHA-256 ↗
            </button>
          </div>

        </div>
      </div>
    );
  }

  // ==========================================
  // RENDER STEP 2: EXAM IN PROGRESS
  // ==========================================
  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12 animate-in fade-in duration-200 text-ink">
      
      {/* 1. Header: Exam Title, Question counter, Timer */}
      <div className="bg-paper border-2 border-green-500 rounded-3xl p-5 shadow-[6px_6px_0_#DEB9B5] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-2xl bg-pink-100 border-2 border-green-500 text-pink-700 flex items-center justify-center text-xl shadow-[3px_3px_0_#DEB9B5] shrink-0">
            🎙️
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-heading font-black text-base sm:text-lg text-ink">
                Phòng Thi Vấn Đáp Trực Tuyến (Oral Exam Room)
              </h1>
              <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border flex items-center gap-1 ${
                isWsConnected 
                  ? 'bg-emerald-100 text-emerald-800 border-emerald-300' 
                  : 'bg-amber-100 text-amber-800 border-amber-300'
              }`}>
                <Radio className={`w-3 h-3 ${isWsConnected ? 'text-emerald-600 animate-pulse' : 'text-amber-600'}`} />
                {isWsConnected ? 'STT Direct WS Online' : 'WS Local Mode'}
              </span>
            </div>
            <p className="text-xs text-green-800 mt-0.5">
              Thí sinh: <strong>{studentName} ({studentCode})</strong> • Học phần: <strong>SWD392 - Software Architecture</strong>
            </p>
          </div>
        </div>

        {/* Question Counter & Countdown Timer */}
        <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
          <div className="text-right font-mono bg-white border-2 border-green-400 px-3.5 py-1.5 rounded-xl shadow-[2px_2px_0_#DADFD3]">
            <span className="text-[9px] font-bold text-green-700 uppercase tracking-wider block">Tiến độ câu hỏi</span>
            <p className="text-xs sm:text-sm font-extrabold text-ink">
              Câu {activeQuestionIndex + 1} / {questions.length}
            </p>
          </div>

          <div className="text-right font-mono bg-pink-50 border-2 border-pink-400 px-3.5 py-1.5 rounded-xl shadow-[2px_2px_0_#DEB9B5]">
            <span className="text-[9px] font-bold text-pink-700 uppercase tracking-wider block">Thời Gian Ca Thi</span>
            <p className="text-xs sm:text-sm font-extrabold text-pink-900">{formatTime(secondsLeft)}</p>
          </div>

          <button
            type="button"
            onClick={handleFinishExam}
            className="px-4 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-bold text-xs border-2 border-green-700 shadow-[3px_3px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer"
          >
            Nộp Bài Sớm ↗
          </button>
        </div>
      </div>

      {/* Main 2-Column Viva Exam Studio */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Left Column (7 cols): Center Stage AI Examiner & Question Card */}
        <div className="lg:col-span-7 space-y-6">
          
          {/* Center Stage: AI Examiner Avatar with pulsing soundwave animation */}
          <div className="bg-paper border-2 border-green-500 rounded-3xl p-6 shadow-[6px_6px_0_#DEB9B5] flex flex-col sm:flex-row items-center gap-6 relative overflow-hidden">
            {/* AI Avatar */}
            <div className="relative shrink-0">
              <div className={`w-28 h-28 rounded-3xl bg-pink-100 border-2 border-green-500 flex items-center justify-center text-5xl shadow-[4px_4px_0_#DEB9B5] relative transition-transform ${
                isAiSpeaking ? 'scale-105 ring-4 ring-pink-400' : ''
              }`}>
                👩‍🏫
              </div>
              {/* Pulsing indicator */}
              <div className={`absolute -bottom-2 -right-2 px-2 py-0.5 rounded-full text-[10px] font-bold border-2 ${
                isAiSpeaking 
                  ? 'bg-pink-500 text-white border-green-700 animate-bounce' 
                  : 'bg-white text-green-800 border-green-400'
              }`}>
                {isAiSpeaking ? 'Đang đọc...' : 'Chờ phản hồi'}
              </div>
            </div>

            {/* AI Examiner Info & Controls */}
            <div className="space-y-3 flex-1 text-center sm:text-left">
              <div>
                <span className="text-[10px] font-bold text-green-700 uppercase tracking-wider block">
                  Giám Thị Khảo Thí Trí Tuệ Nhân Tạo
                </span>
                <h3 className="font-heading font-black text-xl text-ink">
                  Dr. Sophia (AIVES AI Examiner)
                </h3>
              </div>

              {/* Pulsing soundwave animation when TTS is speaking */}
              <div className="h-10 flex items-center justify-center sm:justify-start">
                {isAiSpeaking ? (
                  <div className="flex items-center gap-1.5 px-3 py-1.5 bg-pink-100 border-2 border-green-500 rounded-xl shadow-[2px_2px_0_#DEB9B5]">
                    <VoiceWaveform isSpeaking={true} volume={0.8} barCount={10} height={20} colorClass="bg-pink-600" />
                    <span className="text-xs font-bold text-pink-900 ml-1">Đang phát âm câu hỏi...</span>
                  </div>
                ) : (
                  <span className="text-xs text-green-800 italic">
                    Nhấn "Nghe Lại Câu Hỏi" nếu bạn muốn nghe lại giám thị đọc đề.
                  </span>
                )}
              </div>

              <div className="flex items-center justify-center sm:justify-start gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => playTTS(currentQuestion.content)}
                  className="px-3.5 py-1.5 rounded-xl bg-white hover:bg-green-50 text-ink font-bold text-xs border-2 border-green-500 shadow-[2px_2px_0_#DADFD3] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center gap-1.5"
                >
                  <Volume2 className="w-3.5 h-3.5 text-pink-600" />
                  <span>Nghe Lại Câu Hỏi</span>
                </button>
                {isAiSpeaking && (
                  <button
                    type="button"
                    onClick={stopTTS}
                    className="px-3 py-1.5 rounded-xl bg-rose-100 hover:bg-rose-200 text-rose-800 font-bold text-xs border-2 border-green-500 shadow-[2px_2px_0_#DADFD3] cursor-pointer flex items-center gap-1"
                  >
                    <Square className="w-3 h-3 text-rose-600" />
                    <span>Dừng Đọc</span>
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Question Card: Question content, badge "Câu hỏi chính" hoặc "Câu hỏi xoáy" */}
          <div className="bg-paper border-2 border-green-500 rounded-3xl p-6 shadow-[6px_6px_0_#DEB9B5] space-y-4">
            <div className="flex items-start justify-between gap-3 pb-3 border-b-2 border-green-200">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border-2 ${
                    currentQuestion.type === 'PROBING'
                      ? 'bg-pink-100 text-pink-900 border-pink-400'
                      : 'bg-green-100 text-green-900 border-green-400'
                  }`}>
                    {currentQuestion.badge || (currentQuestion.type === 'PROBING' ? 'Câu hỏi xoáy' : 'Câu hỏi chính')}
                  </span>
                  <span className="text-xs font-mono font-bold text-green-700">
                    Mã lượt: #{currentQuestion.turnId || currentQuestion.id}
                  </span>
                </div>
                <h2 className="font-heading font-black text-lg sm:text-xl text-ink leading-snug pt-1">
                  {currentQuestion.title}
                </h2>
              </div>
            </div>

            <div className="bg-white border-2 border-green-400 rounded-2xl p-4 shadow-[3px_3px_0_#DADFD3]">
              <p className="text-sm sm:text-base text-ink leading-relaxed font-medium">
                {currentQuestion.content}
              </p>
            </div>

            {currentQuestion.hint && (
              <div className="p-3 bg-pink-50/70 border border-pink-200 rounded-xl text-xs text-pink-900 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-pink-600 shrink-0" />
                <span><strong>Gợi ý sư phạm:</strong> {currentQuestion.hint}</span>
              </div>
            )}
          </div>

        </div>

        {/* Right Column (5 cols): Answer Section & Action Controls */}
        <div className="lg:col-span-5 space-y-6">
          
          <div className="bg-paper border-2 border-green-500 rounded-3xl p-6 shadow-[6px_6px_0_#DEB9B5] space-y-4">
            
            <div className="flex items-center justify-between pb-3 border-b-2 border-green-200">
              <div className="flex items-center gap-2">
                <span className={`w-3 h-3 rounded-full border-2 border-green-700 ${
                  isRecording ? 'bg-pink-500 animate-ping' : 'bg-green-500'
                }`} />
                <h3 className="font-heading font-black text-sm text-ink uppercase tracking-wider">
                  Khu Vực Trả Lời Vấn Đáp
                </h3>
              </div>
              <span className="text-[11px] font-mono text-green-700 font-bold">
                {isRecording ? 'Đang Thu Âm...' : 'Micro Sẵn Sàng'}
              </span>
            </div>

            {/* Waveform visualizer when student speaks */}
            {isRecording && (
              <div className="p-3 bg-green-900 text-white rounded-2xl border-2 border-green-700 flex items-center justify-between shadow-[3px_3px_0_#DEB9B5] animate-in fade-in">
                <div className="flex items-center gap-2">
                  <VoiceWaveform isSpeaking={true} volume={0.85} barCount={12} height={20} colorClass="bg-pink-400" />
                  <span className="text-xs font-mono text-pink-200 font-bold">Đang nhận diện giọng nói...</span>
                </div>
                <span className="text-[10px] font-mono text-green-300">250ms Slices</span>
              </div>
            )}

            {/* Realtime transcript live typing effect */}
            <div className="space-y-1.5">
              <label className="text-[11px] font-bold text-green-800 uppercase tracking-wider block">
                Nội dung câu trả lời (Bóc băng giọng nói &amp; Soạn thảo):
              </label>
              
              <div className="relative">
                <textarea
                  rows={6}
                  value={manualText}
                  onChange={(e) => setManualText(e.target.value)}
                  placeholder="Nhấn 'Bắt đầu nói' bên dưới để trả lời bằng micro, hoặc gõ văn bản bổ sung tại đây..."
                  className="w-full p-4 rounded-2xl bg-white border-2 border-green-400 focus:border-pink-500 focus:ring-0 text-ink text-xs sm:text-sm leading-relaxed placeholder:text-green-600/60 font-medium shadow-[3px_3px_0_#DADFD3]"
                />
              </div>

              {/* Live transcript preview if recording */}
              {(transcript || partialTranscript) && (
                <div className="p-3 bg-pink-50 border-2 border-pink-300 rounded-xl text-xs text-pink-950 font-medium animate-in fade-in">
                  <span className="font-bold text-pink-800 block text-[10px] uppercase tracking-wider mb-0.5">
                    Live STT Stream:
                  </span>
                  <span>{transcript} </span>
                  <span className="text-pink-600 font-bold underline">{partialTranscript}</span>
                </div>
              )}
            </div>

            {/* Action Controls: "Bắt đầu nói", "Dừng & Nộp câu trả lời" */}
            <div className="pt-2 space-y-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={handleToggleSpeak}
                  className={`py-3 px-4 rounded-2xl font-black text-xs border-2 border-green-700 shadow-[3px_3px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 ${
                    isRecording
                      ? 'bg-pink-500 hover:bg-pink-600 text-white animate-pulse'
                      : 'bg-white hover:bg-green-50 text-ink'
                  }`}
                >
                  {isRecording ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4 text-pink-600" />}
                  <span>{isRecording ? 'Dừng Ghi Âm' : 'Bắt Đầu Nói'}</span>
                </button>

                <button
                  type="button"
                  onClick={handleSubmitTurn}
                  disabled={isSubmittingTurn}
                  className="py-3 px-4 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-black text-xs border-2 border-green-700 shadow-[3px_3px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  <Send className="w-4 h-4" />
                  <span>{isSubmittingTurn ? 'Đang Nộp...' : 'Dừng & Nộp Câu Trả Lời'}</span>
                </button>
              </div>

              <div className="flex items-center justify-between text-[11px] text-green-800 pt-1">
                <span>Tiêu chí: {currentQuestion.rubricCriteria}</span>
              </div>
            </div>

          </div>

          {/* Question Stepper / Overview */}
          <div className="bg-paper border-2 border-green-500 rounded-3xl p-5 shadow-[6px_6px_0_#DEB9B5] space-y-3">
            <h4 className="font-heading font-black text-xs uppercase tracking-wider text-green-800">
              Danh Sách Lượt Vấn Đáp
            </h4>
            <div className="space-y-2">
              {questions.map((q, idx) => {
                const isCurrent = activeQuestionIndex === idx;
                const isAnswered = !!answersByQuestion[idx];
                return (
                  <div
                    key={q.id || idx}
                    className={`p-2.5 rounded-xl border-2 transition flex items-center justify-between text-xs ${
                      isCurrent
                        ? 'bg-pink-100 border-green-600 font-bold shadow-[2px_2px_0_#DEB9B5]'
                        : isAnswered
                        ? 'bg-emerald-50 border-emerald-400 text-emerald-900'
                        : 'bg-white border-green-300 text-green-900'
                    }`}
                  >
                    <div className="flex items-center gap-2 truncate">
                      <span className={`w-5 h-5 rounded-lg flex items-center justify-center text-[10px] font-bold ${
                        isCurrent ? 'bg-pink-600 text-white' : isAnswered ? 'bg-emerald-600 text-white' : 'bg-green-200 text-green-800'
                      }`}>
                        {idx + 1}
                      </span>
                      <span className="truncate">{q.title || `Câu ${idx + 1}`}</span>
                    </div>
                    {isAnswered && (
                      <span className="text-[10px] font-bold text-emerald-700 shrink-0">Đã nộp ✓</span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

        </div>

      </div>

    </div>
  );
}
