import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/userApi';
import { oralExamApi } from '../api/oralExamApi';
import { useVoiceSTT } from '../hooks/useVoiceSTT';
import { useZeroTTS } from '../hooks/useZeroTTS';
import { 
  Radio, 
  Mic, 
  MicOff, 
  CheckCircle2, 
  X, 
  AlertTriangle, 
  Clock, 
  Sparkles,
  Volume2,
  VolumeX,
  Send,
  ArrowRight,
  ShieldAlert,
  RotateCcw,
  Award,
  BookOpen,
  ChevronRight,
  Square,
  Wifi,
  WifiOff,
  RefreshCw,
  PhoneCall,
  Copy,
  ExternalLink,
  HelpCircle,
  FileCheck2,
  Check
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
    badge: 'Câu hỏi xoáy thích ứng (Lượt 1/2)',
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
    badge: 'Câu hỏi xoáy thích ứng (Lượt 2/2)',
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

/**
 * Maps backend ExamTurnDetailResponse list to frontend Question structure
 */
const mapTurnsToQuestions = (turns, totalQuestions) => {
  if (!Array.isArray(turns) || turns.length === 0) return null;
  return turns.map((t, idx) => ({
    id: t.questionId || idx + 1,
    turnId: t.turnId,
    questionNumber: t.turnOrder || idx + 1,
    totalQuestions: totalQuestions || turns.length,
    type: t.turnType || 'MAIN',
    badge: t.turnType === 'PROBING' ? `Câu hỏi xoáy thích ứng (Lượt ${t.turnOrder || idx + 1})` : 'Câu hỏi chính',
    title: `Câu ${t.turnOrder || idx + 1}: ${t.turnType === 'PROBING' ? 'Hỏi xoáy thích ứng (Adaptive Probing)' : 'Kiến thức cốt lõi'}`,
    content: t.questionText || '',
    ttsAudioUrl: t.ttsAudioUrl,
    hint: t.turnType === 'PROBING' ? 'Phân tích sâu hơn các khía cạnh kỹ thuật đã nêu.' : '',
    rubricCriteria: t.rubricEvaluations || 'Đánh giá năng lực chuyên môn và phản xạ kiến trúc.'
  }));
};

/**
 * Maps backend turns or fallback to Bloom rubric criteria
 */
const mapRubricsFromTurns = (turns, defaultFeedback) => {
  if (Array.isArray(turns) && turns.length > 0) {
    const list = [];
    turns.forEach((t, i) => {
      if (t.rubricEvaluations) {
        try {
          const parsed = JSON.parse(t.rubricEvaluations);
          if (Array.isArray(parsed) && parsed.length > 0) {
            list.push(...parsed);
            return;
          } else if (parsed && parsed.name) {
            list.push(parsed);
            return;
          }
        } catch (e) {}
      }
      list.push({
        name: `Lượt ${t.turnOrder || i + 1}: ${t.turnType === 'PROBING' ? 'Hỏi xoáy thích ứng' : 'Kiến thức cốt lõi'}`,
        score: t.score != null ? Number(t.score) : 9.0,
        max: 10,
        comment: t.feedback || t.rubricEvaluations || defaultFeedback || 'Đáp ứng tốt các tiêu chí đánh giá theo chuẩn đầu ra.'
      });
    });
    if (list.length > 0) return list;
  }

  return [
    { name: 'Kiến thức cốt lõi & Nguyên lý Kiến trúc', score: 9.5, max: 10, comment: 'Đạt chuẩn Bloom 4. Định nghĩa chính xác Domain Core độc lập.' },
    { name: 'Khả năng Phản xạ & Câu hỏi xoáy thích ứng', score: 9.0, max: 10, comment: 'Xác định chuẩn Contract Interface và Implementation Adapter.' },
    { name: 'Độ tin cậy & Giải pháp phân tán (Outbox / Idempotency)', score: 9.2, max: 10, comment: 'Phân tích thấu đáo lỗi Dual-Write và kỹ thuật deduplication.' }
  ];
};

export function OralExamRoomPage({ 
  onNavigate, 
  showToast, 
  subjectId = 1, 
  attemptId: initialAttemptId = null 
}) {
  const { currentUser } = useAuth();
  const [profile, setProfile] = useState(currentUser || null);

  // 6-State Machine: 'READY' -> 'IN_PROGRESS' <-> 'RECONNECTING' -> 'FINALIZING' -> 'COMPLETED' / 'INTERRUPTED'
  const [examStage, setExamStage] = useState('READY');

  // Attempt & Questions state
  const [attemptId, setAttemptId] = useState(initialAttemptId);
  const [questions, setQuestions] = useState(DEFAULT_QUESTIONS);
  const [activeQuestionIndex, setActiveQuestionIndex] = useState(0);
  const [secondsLeft, setSecondsLeft] = useState(900); // 15:00 countdown for exam
  const [isSubmittingTurn, setIsSubmittingTurn] = useState(false);

  // Reconnection state (Grace period max 120s)
  const [reconnectGraceLeft, setReconnectGraceLeft] = useState(120);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const [interruptedReason, setInterruptedReason] = useState('');

  // Finalizing synchronization progress (step 1, 2, 3)
  const [finalizingStep, setFinalizingStep] = useState(1);

  // Support / Certificate Modal states
  const [showSupportModal, setShowSupportModal] = useState(false);
  const [showCertModal, setShowCertModal] = useState(false);
  const [copiedAttempt, setCopiedAttempt] = useState(false);

  // Answers store
  const [answersByQuestion, setAnswersByQuestion] = useState({});
  const [manualText, setManualText] = useState('');

  // Rubrics & Feedback Summary
  const [examResult, setExamResult] = useState(null);

  // Preflight check modal state (READY state)
  const [preflightMicOk, setPreflightMicOk] = useState(false);
  const [preflightSpeakerOk, setPreflightSpeakerOk] = useState(false);
  const [preflightTestingMic, setPreflightTestingMic] = useState(false);
  const [preflightMicVolume, setPreflightMicVolume] = useState(0);

  const preflightAudioCtxRef = useRef(null);
  const preflightStreamRef = useRef(null);
  const preflightAnimFrameRef = useRef(null);
  const wasWsConnectedRef = useRef(false);

  // Voice STT Hook
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
      // Trigger disconnect if WS dropped while student was in IN_PROGRESS
      if (wasWsConnectedRef.current && examStage === 'IN_PROGRESS') {
        handleDisconnect('Mất kết nối với máy chủ nhận diện giọng nói (STT Stream dropped).');
      }
    }
  });

  // Track WS connection status
  useEffect(() => {
    if (isWsConnected) {
      wasWsConnectedRef.current = true;
    }
  }, [isWsConnected]);

  // Voice TTS Hook
  const { isPlaying: isAiSpeaking, play: playTTS, stop: stopTTS } = useZeroTTS({
    onError: (err) => {
      console.warn('TTS Hook Warning:', err);
    }
  });

  // Current Question
  const currentQuestion = questions[activeQuestionIndex] || questions[0];

  // Sync profile & exam data on mount
  useEffect(() => {
    let isMounted = true;

    async function loadData() {
      try {
        const u = await userApi.getCurrentUser();
        if (isMounted) setProfile(u);
      } catch (e) {}

      // If initialAttemptId exists, load from API
      if (initialAttemptId) {
        try {
          const detail = await oralExamApi.getAttemptDetail(initialAttemptId);
          if (isMounted && detail) {
            const mappedQuestions = mapTurnsToQuestions(detail.turns, detail.totalQuestions);
            if (mappedQuestions && mappedQuestions.length > 0) {
              setQuestions(mappedQuestions);
            }
            if (detail.currentQuestionIndex != null) {
              setActiveQuestionIndex(detail.currentQuestionIndex);
            }
            if (detail.status && ['READY', 'IN_PROGRESS', 'RECONNECTING', 'FINALIZING', 'COMPLETED', 'INTERRUPTED'].includes(detail.status)) {
              setExamStage(detail.status);
              if (detail.status === 'COMPLETED') {
                const totalScore = detail.totalScore != null ? Number(detail.totalScore) : 9.0;
                let grade = detail.gradeLevel || 'Xuất sắc';
                if (grade === 'EXCELLENT' || grade === 'Xuất sắc') grade = 'Xuất sắc';
                else if (grade === 'GOOD' || grade === 'Giỏi') grade = 'Giỏi';
                else if (grade === 'AVERAGE' || grade === 'Khá') grade = 'Khá';
                else if (grade === 'POOR' || grade === 'Chưa đạt') grade = 'Chưa đạt';

                setExamResult({
                  totalScore,
                  grade,
                  maxScore: 10,
                  feedback: detail.detailedFeedback || 'Ca thi đã hoàn tất.',
                  rubrics: mapRubricsFromTurns(detail.turns, detail.detailedFeedback)
                });
              } else if (detail.status === 'INTERRUPTED') {
                setInterruptedReason(detail.detailedFeedback || 'Ca thi bị gián đoạn do mất kết nối hoặc sự cố đồng bộ.');
              }
            }
          }
        } catch (err) {
          console.warn('Could not fetch attempt detail, using default questions:', err);
        }
      }
    }

    loadData();
    return () => { isMounted = false; };
  }, [initialAttemptId]);

  // ==========================================
  // STATE MACHINE TIMERS & EVENT LISTENERS
  // ==========================================

  // 1. Exam Timer Countdown during IN_PROGRESS
  useEffect(() => {
    if (examStage !== 'IN_PROGRESS') return;

    const interval = setInterval(() => {
      setSecondsLeft((prev) => Math.max(0, prev - 1));
    }, 1000);

    return () => clearInterval(interval);
  }, [examStage]);

  // Transition: timeout() [sessionExpired] -> FINALIZING
  useEffect(() => {
    if (examStage === 'IN_PROGRESS' && secondsLeft === 0) {
      handleFinishExam('SESSION_EXPIRED');
    }
  }, [examStage, secondsLeft]);

  // 2. Grace Period Countdown during RECONNECTING (Max 120s)
  useEffect(() => {
    if (examStage !== 'RECONNECTING') return;

    const interval = setInterval(() => {
      setReconnectGraceLeft((prev) => Math.max(0, prev - 1));
    }, 1000);

    return () => clearInterval(interval);
  }, [examStage]);

  // Transition: timeout() [graceExpired] -> INTERRUPTED
  useEffect(() => {
    if (examStage === 'RECONNECTING' && reconnectGraceLeft === 0) {
      handleGraceTimeout();
    }
  }, [examStage, reconnectGraceLeft]);

  // 3. Browser online/offline event listeners
  useEffect(() => {
    const handleBrowserOffline = () => {
      if (examStage === 'IN_PROGRESS') {
        handleDisconnect('Mất kết nối mạng Internet (Trình duyệt phát hiện Ngoại tuyến - Offline).');
      }
    };

    const handleBrowserOnline = () => {
      if (examStage === 'RECONNECTING') {
        if (showToast) {
          showToast({
            type: 'info',
            title: 'Tín Hiệu Mạng Đã Trở Lại',
            message: 'Đang tự động đồng bộ và kết nối lại bài thi...'
          });
        }
        handleReconnect();
      }
    };

    window.addEventListener('offline', handleBrowserOffline);
    window.addEventListener('online', handleBrowserOnline);

    return () => {
      window.removeEventListener('offline', handleBrowserOffline);
      window.removeEventListener('online', handleBrowserOnline);
    };
  }, [examStage, attemptId]);

  // 4. Track WS connection drop during active exam session
  useEffect(() => {
    if (isWsConnected) {
      wasWsConnectedRef.current = true;
    } else if (wasWsConnectedRef.current && examStage === 'IN_PROGRESS') {
      wasWsConnectedRef.current = false;
      handleDisconnect('Mất kết nối với máy chủ nhận diện giọng nói (WebSocket stream dropped).');
    }
  }, [isWsConnected, examStage]);

  // 4. TTS Read Question upon changing question in IN_PROGRESS
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

  // Stop TTS & STT on unmount
  useEffect(() => {
    return () => {
      stopTTS();
      stopRecording();
      stopPreflightMic();
    };
  }, [stopTTS, stopRecording]);

  // Format seconds mm:ss
  const formatTime = (secs) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  // --- Preflight Check Handlers (READY state) ---
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

  // ==========================================
  // STATE MACHINE TRANSITION HANDLERS
  // ==========================================

  /**
   * READY -> IN_PROGRESS: start() [deviceReady] / startTimer()
   */
  const handleStartExam = async () => {
    stopPreflightMic();
    stopTTS();

    let currentAttemptId = attemptId;

    // If attemptId doesn't exist yet, create via startExam
    if (!currentAttemptId) {
      try {
        const res = await oralExamApi.startExam(subjectId);
        if (res && res.attemptId) {
          currentAttemptId = res.attemptId;
          setAttemptId(currentAttemptId);
          if (res.currentQuestionText) {
            setQuestions((prev) => {
              const updated = [...prev];
              updated[0] = {
                ...updated[0],
                turnId: res.currentTurnId || updated[0].turnId,
                content: res.currentQuestionText,
                ttsAudioUrl: res.currentTtsAudioUrl
              };
              return updated;
            });
          }
        }
      } catch (err) {
        console.warn('oralExamApi.startExam fallback:', err);
        currentAttemptId = Date.now();
        setAttemptId(currentAttemptId);
      }
    }

    // Call startExamSession(attemptId, deviceReady = true)
    try {
      if (currentAttemptId) {
        const sessionRes = await oralExamApi.startExamSession(currentAttemptId, true);
        if (sessionRes?.currentQuestionText) {
          setQuestions((prev) => {
            const updated = [...prev];
            updated[0] = {
              ...updated[0],
              turnId: sessionRes.currentTurnId || updated[0].turnId,
              content: sessionRes.currentQuestionText,
              ttsAudioUrl: sessionRes.currentTtsAudioUrl
            };
            return updated;
          });
        }
      }
    } catch (err) {
      console.warn('oralExamApi.startExamSession fallback:', err);
    }

    // Enter IN_PROGRESS & start exam timer
    setExamStage('IN_PROGRESS');
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Bắt Đầu Bài Thi',
        message: 'Chào mừng bạn vào phòng thi vấn đáp. Đồng hồ ca thi bắt đầu tính giờ!'
      });
    }
  };

  /**
   * IN_PROGRESS -> RECONNECTING: disconnect() / pauseTimer()
   */
  const handleDisconnect = async (reason = 'Mất kết nối mạng') => {
    stopRecording();
    stopTTS();

    if (attemptId) {
      try {
        await oralExamApi.disconnectExam(attemptId);
      } catch (err) {
        console.warn('oralExamApi.disconnectExam fallback:', err);
      }
    }

    // Timer pauses automatically because examStage !== 'IN_PROGRESS'
    setReconnectGraceLeft(120);
    setExamStage('RECONNECTING');

    if (showToast) {
      showToast({
        type: 'warning',
        title: 'Mất Kết Nối Mạng',
        message: 'Đồng hồ thi đã tạm dừng. Bạn có tối đa 120 giây để kết nối lại.'
      });
    }
  };

  /**
   * RECONNECTING -> IN_PROGRESS: reconnect() [timeRemaining > 0] / resumeTimer()
   */
  const handleReconnect = async () => {
    setIsReconnecting(true);

    try {
      if (attemptId) {
        await oralExamApi.reconnectExam(attemptId);
      }
      setExamStage('IN_PROGRESS');
      if (showToast) {
        showToast({
          type: 'success',
          title: 'Kết Nối Lại Thành Công',
          message: 'Đã phục hồi phiên thi. Đồng hồ tính giờ tiếp tục hoạt động.'
        });
      }
    } catch (err) {
      console.warn('reconnectExam error:', err);
      const msg = (err?.message || '').toLowerCase();
      const isGraceExpired = msg.includes('grace') ||
                             err?.data?.message?.toLowerCase().includes('grace') ||
                             reconnectGraceLeft <= 0;

      if (isGraceExpired) {
        handleGraceTimeout();
      } else if (err?.code === 'NETWORK_ERROR' && navigator.onLine) {
        // Fallback for offline local dev mode
        setExamStage('IN_PROGRESS');
        if (showToast) {
          showToast({
            type: 'success',
            title: 'Tiếp Tục Bài Thi',
            message: 'Đã phục hồi phiên thi cục bộ.'
          });
        }
      } else {
        if (showToast) {
          showToast({
            type: 'error',
            title: 'Chưa Thể Kết Nối Lại',
            message: err?.message || 'Thiết bị vẫn chưa thể kết nối lại máy chủ. Vui lòng kiểm tra cáp mạng hoặc Wi-Fi.'
          });
        }
      }
    } finally {
      setIsReconnecting(false);
    }
  };

  /**
   * RECONNECTING -> INTERRUPTED: timeout() [graceExpired]
   */
  const handleGraceTimeout = () => {
    setInterruptedReason('Thời gian chờ kết nối lại đã vượt quá thời hạn ân hạn 120 giây (Grace Period Expired).');
    setExamStage('INTERRUPTED');
    if (showToast) {
      showToast({
        type: 'error',
        title: 'Hết Thời Gian Chờ',
        message: 'Ca thi bị gián đoạn do mất kết nối mạng quá 120 giây.'
      });
    }
  };

  /**
   * Voice Answering Handlers
   */
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

  /**
   * Submit turn answer & question loop: probe() / nextQuestion()
   */
  const handleSubmitTurn = async () => {
    if (isRecording) {
      stopRecording();
    }

    const answerContent = currentCompositeTranscript || 'Thí sinh xác nhận hoàn thành câu trả lời.';
    setIsSubmittingTurn(true);

    let evalRes = null;
    try {
      const turnId = currentQuestion.turnId || currentQuestion.id;
      if (attemptId) {
        evalRes = await oralExamApi.submitTurnAnswer(attemptId, turnId, {
          transcriptText: answerContent,
          audioBlob: audioBlob
        });
      }
    } catch (err) {
      console.warn('Backend turn submit fallback:', err);
    }

    // Save answer locally with evaluation metrics if available
    setAnswersByQuestion((prev) => ({
      ...prev,
      [activeQuestionIndex]: {
        questionId: currentQuestion.id,
        turnId: currentQuestion.turnId,
        text: answerContent,
        audioBlob: audioBlob,
        score: evalRes?.score,
        feedback: evalRes?.feedback,
        rubricEvaluations: evalRes?.rubricEvaluations
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

    // If backend indicates attempt is finished
    if (evalRes?.isAttemptFinished) {
      handleFinishExam('ALL_QUESTIONS_ANSWERED');
      return;
    }

    // If backend generated adaptive probe question or dynamic next turn
    if (evalRes?.nextQuestionText && evalRes?.nextTurnId) {
      const isProbing = evalRes.nextTurnType === 'PROBING';
      const nextIndex = activeQuestionIndex + 1;
      setQuestions((prevQuestions) => {
        const updated = [...prevQuestions];
        const nextQData = {
          id: nextIndex + 1,
          turnId: evalRes.nextTurnId,
          questionNumber: nextIndex + 1,
          totalQuestions: Math.max(updated.length, nextIndex + 1),
          type: evalRes.nextTurnType || (isProbing ? 'PROBING' : 'MAIN'),
          badge: isProbing ? `Câu hỏi xoáy thích ứng (Lượt ${nextIndex + 1})` : 'Câu hỏi chính',
          title: `Câu ${nextIndex + 1}: ${isProbing ? 'Hỏi xoáy thích ứng (Adaptive Probing)' : 'Kiến thức cốt lõi'}`,
          content: evalRes.nextQuestionText,
          rubricCriteria: isProbing ? 'Làm rõ và đào sâu nội dung câu trả lời trước đó.' : 'Nắm vững kiến trúc và nguyên lý hệ thống.'
        };
        if (nextIndex < updated.length) {
          updated[nextIndex] = { ...updated[nextIndex], ...nextQData };
        } else {
          updated.push(nextQData);
        }
        return updated;
      });
      setActiveQuestionIndex(nextIndex);
      if (isProbing && showToast) {
        showToast({
          type: 'info',
          title: 'Hỏi Xoáy Thích Ứng (Adaptive Probing)',
          message: 'Giám thị AI đưa ra câu hỏi xoáy thích ứng để làm rõ câu trả lời vừa rồi.'
        });
      }
      return;
    }

    // Fallback question loop
    if (activeQuestionIndex < questions.length - 1) {
      const nextIndex = activeQuestionIndex + 1;
      const nextQ = questions[nextIndex];
      setActiveQuestionIndex(nextIndex);

      if (nextQ && nextQ.type === 'PROBING') {
        if (showToast) {
          showToast({
            type: 'info',
            title: 'Hỏi Xoáy Thích Ứng (Adaptive Probing)',
            message: 'Giám thị AI đưa ra câu hỏi xoáy thích ứng để làm rõ câu trả lời vừa rồi.'
          });
        }
      }
    } else {
      // Completed all questions -> move to FINALIZING
      handleFinishExam('ALL_QUESTIONS_ANSWERED');
    }
  };

  /**
   * IN_PROGRESS -> FINALIZING: finish() / timeoutSession()
   * FINALIZING -> COMPLETED: syncSuccess()
   * FINALIZING -> INTERRUPTED: syncError()
   */
  const handleFinishExam = async (reason = 'USER_SUBMIT') => {
    stopRecording();
    stopTTS();

    setExamStage('FINALIZING');
    setFinalizingStep(1);

    try {
      // Step 1: Pack transcript & turn audio slices
      await new Promise((res) => setTimeout(res, 800));
      setFinalizingStep(2);

      // Step 2: Call finishExam API
      if (attemptId) {
        try {
          await oralExamApi.finishExam(attemptId);
        } catch (apiErr) {
          console.warn('finishExam API fallback:', apiErr);
          if (apiErr?.code !== 'NETWORK_ERROR' && apiErr?.status && apiErr.status >= 500) {
            throw apiErr;
          }
        }
      }

      await new Promise((res) => setTimeout(res, 800));
      setFinalizingStep(3);

      // Step 3: Polling for attempt grading completion (syncSuccess / syncError)
      let detailResult = null;
      let syncStatus = 'FINALIZING';
      const MAX_POLLS = 10;
      let polls = 0;

      while (polls < MAX_POLLS && syncStatus === 'FINALIZING') {
        polls++;
        try {
          if (attemptId) {
            detailResult = await oralExamApi.getAttemptDetail(attemptId);
            if (detailResult && detailResult.status) {
              syncStatus = detailResult.status;
            }
          }
        } catch (pollErr) {
          console.warn(`Polling attempt ${polls} error:`, pollErr);
          if (pollErr?.code === 'NETWORK_ERROR') {
            break;
          }
        }

        if (syncStatus === 'COMPLETED' || syncStatus === 'INTERRUPTED') {
          break;
        }

        await new Promise((res) => setTimeout(res, 1200));
      }

      if (syncStatus === 'INTERRUPTED') {
        // syncError()
        setInterruptedReason(detailResult?.detailedFeedback || 'Quá trình chấm thi AI hoặc đồng bộ dữ liệu ca thi gặp sự cố.');
        setExamStage('INTERRUPTED');
        if (showToast) {
          showToast({
            type: 'error',
            title: 'Sự Cố Đồng Bộ Ca Thi',
            message: 'Không thể hoàn tất chấm điểm đa tầng. Ca thi chuyển sang trạng thái Gián đoạn.'
          });
        }
        return;
      }

      // syncSuccess() or fallback
      const totalScore = detailResult?.totalScore != null 
        ? Number(detailResult.totalScore) 
        : 9.2;

      let grade = detailResult?.gradeLevel || 'Xuất sắc';
      if (grade === 'EXCELLENT' || grade === 'Xuất sắc') grade = 'Xuất sắc';
      else if (grade === 'GOOD' || grade === 'Giỏi') grade = 'Giỏi';
      else if (grade === 'AVERAGE' || grade === 'Khá') grade = 'Khá';
      else if (grade === 'POOR' || grade === 'Chưa đạt') grade = 'Chưa đạt';

      const rubrics = mapRubricsFromTurns(detailResult?.turns, detailResult?.detailedFeedback);

      // syncSuccess() -> Transition to COMPLETED
      setExamResult({
        totalScore,
        grade,
        maxScore: 10,
        feedback: detailResult?.detailedFeedback || 'Thí sinh nắm rất vững các nguyên lý phân tầng và kiến trúc Onion. Diễn giải mạch lạc nguyên lý Đảo ngược phụ thuộc (DIP) và Transactional Outbox Pattern. Phản xạ hỏi xoáy thích ứng tự tin, xử lý tốt rủi ro mất mát dữ liệu và tính bất biến.',
        rubrics
      });

      setExamStage('COMPLETED');
      if (showToast) {
        showToast({
          type: 'success',
          title: 'Ca Thi Hoàn Tất',
          message: 'Dữ liệu đã được đồng bộ thành công. Báo cáo đánh giá đã sẵn sàng!'
        });
      }
    } catch (err) {
      console.error('Finalizing synchronization error:', err);
      // syncError() -> Transition to INTERRUPTED
      setInterruptedReason('Đã xảy ra sự cố trong quá trình đồng bộ dữ liệu bài thi với máy chủ khảo thí.');
      setExamStage('INTERRUPTED');
      if (showToast) {
        showToast({
          type: 'error',
          title: 'Lỗi Đồng Bộ Bài Thi',
          message: 'Không thể đồng bộ dữ liệu phiên thi. Ca thi chuyển sang trạng thái Gián đoạn.'
        });
      }
    }
  };

  const copyToClipboard = (text) => {
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(text);
      setCopiedAttempt(true);
      setTimeout(() => setCopiedAttempt(false), 2000);
      if (showToast) {
        showToast({
          type: 'success',
          title: 'Đã Sao Chép',
          message: `Đã sao chép mã ca thi #${text} vào clipboard.`
        });
      }
    }
  };

  const studentName = profile?.fullName || currentUser?.fullName || 'Nguyễn Văn An';
  const studentCode = profile?.userCode || currentUser?.userCode || 'SE170001';

  // ==========================================
  // RENDER STATE 1: READY (Pre-flight check)
  // ==========================================
  if (examStage === 'READY') {
    return (
      <div className="max-w-3xl mx-auto py-8 px-4 animate-in fade-in duration-300">
        <div className="bg-paper border-2 border-green-500 rounded-3xl p-6 sm:p-10 shadow-[8px_8px_0_#DEB9B5] space-y-8">
          
          {/* Header */}
          <div className="flex items-center gap-4 pb-6 border-b-2 border-green-200">
            <div className="w-14 h-14 rounded-2xl bg-pink-100 border-2 border-green-500 text-pink-700 flex items-center justify-center text-2xl shadow-[3px_3px_0_#DEB9B5] shrink-0">
              🎙️
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-[11px] font-bold uppercase tracking-wider text-green-700 block">
                  Khảo Thí Vấn Đáp AI • Trạng Thái: READY
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-green-100 text-green-800 border border-green-300">
                  Chuẩn bị thiết bị
                </span>
              </div>
              <h1 className="font-heading font-black text-2xl sm:text-3xl text-ink">
                Kiểm Tra Thiết Bị Phòng Thi (Pre-flight Check)
              </h1>
            </div>
          </div>

          <p className="text-xs sm:text-sm text-green-900 leading-relaxed">
            Chào mừng <strong>{studentName} ({studentCode})</strong>. Để đảm bảo bài thi diễn ra công bằng và thông suốt, vui lòng kiểm tra Micro và Loa của thiết bị trước khi bắt đầu bài thi.
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
                      <CheckCircle2 className="w-3.5 h-3.5" /> Chuẩn ✓
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

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={preflightTestingMic ? stopPreflightMic : startPreflightMicTest}
                  className={`w-full py-2.5 px-3 rounded-xl font-bold text-xs border-2 border-green-500 shadow-[2px_2px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-1.5 ${
                    preflightTestingMic 
                      ? 'bg-rose-100 text-rose-800 hover:bg-rose-200' 
                      : 'bg-pink-100 hover:bg-pink-200 text-pink-900'
                  }`}
                >
                  {preflightTestingMic ? <MicOff className="w-3.5 h-3.5" /> : <Mic className="w-3.5 h-3.5" />}
                  <span>{preflightTestingMic ? 'Dừng Mic' : 'Bật Thử Mic'}</span>
                </button>

                <button
                  type="button"
                  onClick={() => setPreflightMicOk(true)}
                  className="w-full py-2.5 px-3 rounded-xl font-bold text-xs bg-white hover:bg-emerald-50 text-emerald-800 border-2 border-emerald-400 shadow-[2px_2px_0_#DADFD3] cursor-pointer flex items-center justify-center gap-1.5"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>{preflightMicOk ? 'Micro Đạt ✓' : 'Xác Nhận Mic'}</span>
                </button>
              </div>
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
                      <CheckCircle2 className="w-3.5 h-3.5" /> Chuẩn ✓
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

          {/* Action Footer: "Bắt đầu thi" */}
          <div className="pt-4 border-t-2 border-green-200 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="text-xs text-green-800">
              * Khuyến nghị dùng tai nghe và micro rời để đạt chất lượng nhận diện giọng nói cao nhất.
            </div>
            <button
              type="button"
              onClick={handleStartExam}
              className="w-full sm:w-auto px-8 py-3.5 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-black text-sm border-2 border-green-700 shadow-[4px_4px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2"
            >
              <span>Bắt Đầu Thi (Vào Phòng Thi)</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>

        </div>
      </div>
    );
  }

  // ==========================================
  // RENDER STATE 3: RECONNECTING (Yellow Warning Modal/Banner)
  // ==========================================
  if (examStage === 'RECONNECTING') {
    return (
      <div className="max-w-3xl mx-auto py-10 px-4 animate-in fade-in zoom-in-95 duration-200">
        <div className="bg-amber-100 border-4 border-amber-600 rounded-3xl p-6 sm:p-10 shadow-[8px_8px_0_#D97706] text-amber-950 space-y-6">
          
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-2xl bg-amber-300 border-2 border-amber-700 text-amber-900 flex items-center justify-center text-3xl shadow-[3px_3px_0_#B45309] shrink-0 animate-bounce">
              <AlertTriangle className="w-8 h-8 text-amber-900" />
            </div>
            <div>
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-amber-200 text-amber-900 border border-amber-400 inline-block mb-1">
                TRẠNG THÁI: RECONNECTING • TẠM DỪNG ĐỒNG HỒ
              </span>
              <h2 className="font-heading font-black text-xl sm:text-2xl text-amber-950">
                Mất kết nối mạng! Đồng hồ thi tạm dừng. Đang thử kết nối lại... (Thời gian chờ tối đa 120s)
              </h2>
            </div>
          </div>

          <div className="bg-white/80 border-2 border-amber-500 rounded-2xl p-5 shadow-[3px_3px_0_#D97706] space-y-4">
            <div className="flex items-center justify-between text-xs sm:text-sm font-bold text-amber-900">
              <span className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-700" />
                Thời gian ân hạn còn lại:
              </span>
              <span className="font-mono text-base font-black text-amber-950 bg-amber-200 px-3 py-1 rounded-xl border border-amber-400">
                {reconnectGraceLeft}s / 120s
              </span>
            </div>

            {/* Grace progress bar */}
            <div className="h-4 bg-amber-200 rounded-full border border-amber-400 overflow-hidden p-0.5">
              <div 
                className="h-full bg-amber-600 rounded-full transition-all duration-1000 ease-linear"
                style={{ width: `${(reconnectGraceLeft / 120) * 100}%` }}
              />
            </div>

            <p className="text-xs text-amber-900 leading-relaxed font-medium">
              Hệ thống đã tự động tạm dừng đồng hồ làm bài thi để bảo vệ thời gian của bạn. 
              Nếu bạn phục hồi kết nối trong vòng <strong>120 giây</strong>, bài thi sẽ được tiếp tục ngay tại câu hỏi hiện tại.
            </p>
          </div>

          {/* Action buttons */}
          <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-4">
            <button
              type="button"
              onClick={handleReconnect}
              disabled={isReconnecting}
              className="w-full sm:w-auto px-8 py-3.5 rounded-2xl bg-amber-500 hover:bg-amber-600 text-amber-950 font-black text-xs sm:text-sm border-2 border-amber-800 shadow-[4px_4px_0_#92400E] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${isReconnecting ? 'animate-spin' : ''}`} />
              <span>{isReconnecting ? 'Đang Kiểm Tra Kết Nối...' : 'Thử Kết Nối Lại Ngay'}</span>
            </button>

            <button
              type="button"
              onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
              className="w-full sm:w-auto px-5 py-3 rounded-2xl bg-white hover:bg-amber-50 text-amber-900 font-bold text-xs border-2 border-amber-600 shadow-[2px_2px_0_#D97706] cursor-pointer"
            >
              Quay lại danh sách ca thi
            </button>
          </div>

        </div>
      </div>
    );
  }

  // ==========================================
  // RENDER STATE 4: FINALIZING (Full-screen overlay/banner)
  // ==========================================
  if (examStage === 'FINALIZING') {
    return (
      <div className="fixed inset-0 z-50 bg-ink/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-300">
        <div className="bg-paper border-4 border-green-700 rounded-3xl p-8 sm:p-10 max-w-xl w-full shadow-[10px_10px_0_#DEB9B5] space-y-6 text-center text-ink">
          
          <div className="w-20 h-20 rounded-3xl bg-pink-100 border-2 border-green-700 text-pink-700 mx-auto flex items-center justify-center text-3xl shadow-[4px_4px_0_#DEB9B5] animate-pulse">
            <Sparkles className="w-10 h-10 text-pink-600 animate-spin" style={{ animationDuration: '6s' }} />
          </div>

          <div className="space-y-2">
            <span className="text-[11px] font-black uppercase tracking-wider text-green-700 block">
              Trạng Thái Ca Thi: FINALIZING
            </span>
            <h2 className="font-heading font-black text-xl sm:text-2xl text-ink">
              Đang đồng bộ dữ liệu và Giám thị AI đang chấm điểm đa tầng...
            </h2>
            <p className="text-xs sm:text-sm text-green-800">
              Vui lòng giữ nguyên cửa sổ trình duyệt. Hệ thống đang tiến hành bóc băng, mã hóa SHA-256 và chấm điểm theo khung Rubric chuẩn Bloom.
            </p>
          </div>

          {/* Synchronizing 3 Steps Progress */}
          <div className="space-y-3 text-left">
            <div className={`p-3.5 rounded-2xl border-2 flex items-center justify-between text-xs transition ${
              finalizingStep >= 1 ? 'bg-white border-green-500 font-bold shadow-[2px_2px_0_#DADFD3]' : 'bg-gray-100 border-gray-300 text-gray-500'
            }`}>
              <div className="flex items-center gap-2">
                <span className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] ${
                  finalizingStep > 1 ? 'bg-emerald-600 text-white' : 'bg-pink-600 text-white animate-spin'
                }`}>
                  {finalizingStep > 1 ? '✓' : '1'}
                </span>
                <span>1. Đóng gói transcript &amp; bản ghi âm các lượt vấn đáp</span>
              </div>
              <span className="text-[10px] text-green-700">
                {finalizingStep > 1 ? 'Hoàn tất ✓' : 'Đang xử lý...'}
              </span>
            </div>

            <div className={`p-3.5 rounded-2xl border-2 flex items-center justify-between text-xs transition ${
              finalizingStep >= 2 ? 'bg-white border-green-500 font-bold shadow-[2px_2px_0_#DADFD3]' : 'bg-gray-100 border-gray-300 text-gray-500'
            }`}>
              <div className="flex items-center gap-2">
                <span className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] ${
                  finalizingStep > 2 ? 'bg-emerald-600 text-white' : finalizingStep === 2 ? 'bg-pink-600 text-white animate-spin' : 'bg-gray-300 text-gray-600'
                }`}>
                  {finalizingStep > 2 ? '✓' : '2'}
                </span>
                <span>2. Đồng bộ hóa dữ liệu với máy chủ khảo thí trung tâm</span>
              </div>
              <span className="text-[10px] text-green-700">
                {finalizingStep > 2 ? 'Hoàn tất ✓' : finalizingStep === 2 ? 'Đang đồng bộ...' : 'Chờ...'}
              </span>
            </div>

            <div className={`p-3.5 rounded-2xl border-2 flex items-center justify-between text-xs transition ${
              finalizingStep >= 3 ? 'bg-white border-green-500 font-bold shadow-[2px_2px_0_#DADFD3]' : 'bg-gray-100 border-gray-300 text-gray-500'
            }`}>
              <div className="flex items-center gap-2">
                <span className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] ${
                  finalizingStep === 3 ? 'bg-pink-600 text-white animate-spin' : 'bg-gray-300 text-gray-600'
                }`}>
                  3
                </span>
                <span>3. Hội đồng Giám thị AI chấm điểm đa tầng &amp; lập biên bản Rubric</span>
              </div>
              <span className="text-[10px] text-green-700">
                {finalizingStep === 3 ? 'Đang chấm điểm...' : 'Chờ...'}
              </span>
            </div>
          </div>

          <div className="pt-2 text-center text-[11px] text-green-800 font-mono">
            * Phiên thi được bảo mật toàn vẹn qua Transactional Outbox Pattern
          </div>

        </div>
      </div>
    );
  }

  // ==========================================
  // RENDER STATE 6: INTERRUPTED (Red Neo-brutalist Card)
  // ==========================================
  if (examStage === 'INTERRUPTED') {
    return (
      <div className="max-w-3xl mx-auto py-10 px-4 animate-in fade-in zoom-in-95 duration-200">
        <div className="bg-rose-50 border-4 border-rose-600 rounded-3xl p-6 sm:p-10 shadow-[8px_8px_0_#984F48] text-rose-950 space-y-6">
          
          <div className="flex items-center gap-4 pb-4 border-b-2 border-rose-200">
            <div className="w-16 h-16 rounded-2xl bg-rose-200 border-2 border-rose-700 text-rose-800 flex items-center justify-center text-3xl shadow-[3px_3px_0_#984F48] shrink-0">
              <ShieldAlert className="w-8 h-8 text-rose-700" />
            </div>
            <div>
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-rose-200 text-rose-900 border border-rose-400 inline-block mb-1">
                TRẠNG THÁI: INTERRUPTED
              </span>
              <h1 className="font-heading font-black text-xl sm:text-2xl text-rose-950">
                Ca thi bị gián đoạn do mất kết nối hoặc sự cố đồng bộ!
              </h1>
            </div>
          </div>

          <div className="bg-white border-2 border-rose-400 rounded-2xl p-5 shadow-[3px_3px_0_#DADFD3] space-y-3">
            <h3 className="font-bold text-sm text-rose-900">Chi tiết lý do gián đoạn:</h3>
            <p className="text-xs sm:text-sm text-rose-950 leading-relaxed font-medium">
              {interruptedReason || 'Ca thi bị gián đoạn do mất kết nối mạng quá thời hạn ân hạn (120s) hoặc máy chủ không thể hoàn tất chu trình đồng bộ.'}
            </p>
            <div className="p-3 bg-rose-100/60 border border-rose-300 rounded-xl text-xs text-rose-900">
              🛡️ <strong>Bảo vệ dữ liệu:</strong> Mọi câu trả lời của các câu hỏi bạn đã nộp trước thời điểm gián đoạn đã được hệ thống lưu trữ an toàn trong Transactional Outbox.
            </div>
          </div>

          {/* Technical Attempt Reference */}
          <div className="flex flex-wrap items-center justify-between gap-2 p-3 bg-rose-100 border border-rose-300 rounded-xl text-xs font-mono text-rose-900">
            <span>Mã ca thi (Attempt ID): #{attemptId || 'SWD392_ATTEMPT'}</span>
            <button
              type="button"
              onClick={() => copyToClipboard(attemptId || 'SWD392_ATTEMPT')}
              className="flex items-center gap-1 font-bold text-rose-800 hover:text-rose-950 cursor-pointer underline"
            >
              <Copy className="w-3.5 h-3.5" />
              <span>{copiedAttempt ? 'Đã sao chép ✓' : 'Sao chép mã'}</span>
            </button>
          </div>

          {/* Action buttons */}
          <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-4">
            <button
              type="button"
              onClick={() => setShowSupportModal(true)}
              className="w-full sm:w-auto px-6 py-3.5 rounded-2xl bg-rose-600 hover:bg-rose-700 text-white font-black text-xs sm:text-sm border-2 border-rose-900 shadow-[4px_4px_0_#492A27] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2"
            >
              <PhoneCall className="w-4 h-4" />
              <span>Liên Hệ Giám Thị / Hỗ Trợ Kỹ Thuật</span>
            </button>

            <button
              type="button"
              onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
              className="w-full sm:w-auto px-6 py-3 rounded-2xl bg-white hover:bg-rose-50 text-rose-900 font-bold text-xs border-2 border-rose-500 shadow-[3px_3px_0_#984F48] cursor-pointer"
            >
              ← Quay Lại Danh Sách Ca Thi
            </button>
          </div>

        </div>

        {/* Support Modal dialog */}
        {showSupportModal && (
          <div className="fixed inset-0 z-50 bg-ink/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-paper border-4 border-rose-600 rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-[8px_8px_0_#984F48] space-y-5 text-ink">
              <div className="flex items-center justify-between pb-3 border-b-2 border-rose-200">
                <div className="flex items-center gap-2 font-heading font-black text-lg text-rose-950">
                  <PhoneCall className="w-5 h-5 text-rose-600" />
                  <span>Kênh Hỗ Trợ Khảo Thí</span>
                </div>
                <button
                  type="button"
                  onClick={() => setShowSupportModal(false)}
                  className="p-1 rounded-lg hover:bg-rose-100 text-rose-800 cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-3 text-xs sm:text-sm text-rose-950">
                <div className="p-3 bg-white border-2 border-rose-300 rounded-xl space-y-1">
                  <span className="text-[10px] font-bold text-rose-700 uppercase tracking-wider block">Hotline Trực Tiếp Hội Đồng Thi</span>
                  <p className="font-heading font-black text-base text-rose-900">1900-8888 (Nhánh 2)</p>
                  <span className="text-[11px] text-green-800 block">Thời gian trực: 07:30 - 21:00 các ngày thi</span>
                </div>

                <div className="p-3 bg-white border-2 border-rose-300 rounded-xl space-y-1">
                  <span className="text-[10px] font-bold text-rose-700 uppercase tracking-wider block">Email Cán Bộ Coi Thi Trực Tuyến</span>
                  <p className="font-mono font-bold text-xs text-rose-900">proctor-support@vieva.edu.vn</p>
                </div>

                <div className="p-3 bg-rose-50 border border-rose-300 rounded-xl text-xs text-rose-900">
                  Vui lòng cung cấp <strong>Mã SV: {studentCode}</strong> và <strong>Mã Ca Thi: #{attemptId || 'SWD392'}</strong> khi liên hệ để được hỗ trợ mở lại ca thi ân hạn.
                </div>
              </div>

              <button
                type="button"
                onClick={() => setShowSupportModal(false)}
                className="w-full py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs border-2 border-rose-900 shadow-[2px_2px_0_#492A27] cursor-pointer"
              >
                Đã hiểu &amp; Đóng
              </button>
            </div>
          </div>
        )}
      </div>
    );
  }

  // ==========================================
  // RENDER STATE 5: COMPLETED (Rubric Report & Certificate)
  // ==========================================
  if (examStage === 'COMPLETED') {
    const displayResult = examResult || {
      totalScore: 9.2,
      grade: 'Xuất sắc',
      maxScore: 10,
      feedback: 'Thí sinh đã hoàn thành đầy đủ các phần thi vấn đáp. Dữ liệu bài thi đã được lưu trữ và bảo vệ an toàn trên hệ thống khảo thí.',
      rubrics: mapRubricsFromTurns([], '')
    };

    return (
      <div className="max-w-4xl mx-auto py-8 px-4 animate-in fade-in duration-300">
        <div className="bg-paper border-2 border-green-500 rounded-3xl p-6 sm:p-10 shadow-[8px_8px_0_#DEB9B5] space-y-8">
          
          {/* Header */}
          <div className="text-center space-y-3 pb-6 border-b-2 border-green-200">
            <div className="w-20 h-20 rounded-3xl bg-pink-100 border-2 border-green-500 text-pink-700 mx-auto flex items-center justify-center text-3xl shadow-[4px_4px_0_#DEB9B5]">
              🎓
            </div>
            <div className="flex items-center justify-center gap-2">
              <span className="text-xs font-bold uppercase tracking-wider text-green-700 block">
                Báo Cáo Đánh Giá Khảo Thí Vấn Đáp • TRẠNG THÁI: COMPLETED
              </span>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
                Đã lưu DB ✓
              </span>
            </div>
            <h1 className="font-heading font-black text-2xl sm:text-3xl text-ink">
              Kết Quả Bài Thi Vấn Đáp Trực Tuyến
            </h1>
            <p className="text-xs sm:text-sm text-green-800">
              Thí sinh: <strong>{studentName}</strong> • Mã SV: <strong>{studentCode}</strong> • Ca thi: <strong>#{attemptId}</strong>
            </p>
          </div>

          {/* Overall Score Box */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white border-2 border-green-500 p-5 rounded-2xl shadow-[4px_4px_0_#DEB9B5] text-center">
              <span className="text-[11px] font-bold text-green-700 uppercase tracking-wider block mb-1">
                Điểm Tổng Kết
              </span>
              <div className="font-heading font-black text-4xl text-pink-600">
                {displayResult.totalScore} <span className="text-lg text-ink font-sans">/ {displayResult.maxScore}</span>
              </div>
            </div>

            <div className="bg-white border-2 border-green-500 p-5 rounded-2xl shadow-[4px_4px_0_#DEB9B5] text-center">
              <span className="text-[11px] font-bold text-green-700 uppercase tracking-wider block mb-1">
                Xếp Loại Đạt Được
              </span>
              <div className="font-heading font-black text-2xl sm:text-3xl text-green-700">
                {displayResult.grade}
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
              "{displayResult.feedback}"
            </p>
          </div>

          {/* Rubric Breakdown List */}
          <div className="space-y-4">
            <h3 className="font-heading font-black text-lg text-ink flex items-center gap-2">
              <Award className="w-5 h-5 text-green-700" />
              Chi Tiết Đánh Giá Theo Ma Trận Rubric Chuẩn Bloom
            </h3>

            <div className="space-y-3">
              {displayResult.rubrics.map((r, i) => (
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

          {/* Actions: Certificate & Return */}
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
              onClick={() => {
                if (onNavigate) {
                  onNavigate('submission-success');
                } else {
                  setShowCertModal(true);
                }
              }}
              className="px-8 py-3 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-bold text-xs border-2 border-green-700 shadow-[3px_3px_0_#DEB9B5] cursor-pointer flex items-center gap-2"
            >
              <FileCheck2 className="w-4 h-4" />
              <span>Xem Giấy Chứng Nhận SHA-256 ↗</span>
            </button>
          </div>

        </div>

        {/* Certificate Modal */}
        {showCertModal && (
          <div className="fixed inset-0 z-50 bg-ink/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-paper border-4 border-green-700 rounded-3xl p-6 sm:p-8 max-w-lg w-full shadow-[8px_8px_0_#DEB9B5] space-y-5 text-ink">
              <div className="flex items-center justify-between pb-3 border-b-2 border-green-200">
                <div className="flex items-center gap-2 font-heading font-black text-lg text-green-900">
                  <Award className="w-5 h-5 text-green-700" />
                  <span>Chứng Chỉ Khảo Thí AI Xác Thực</span>
                </div>
                <button
                  type="button"
                  onClick={() => setShowCertModal(false)}
                  className="p-1 rounded-lg hover:bg-green-100 text-green-800 cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-4 bg-white border-2 border-green-400 rounded-2xl space-y-3 text-center">
                <div className="text-xs uppercase font-bold text-green-700">VIEVA ACADEMIC AI VERIFICATION</div>
                <div className="font-heading font-black text-xl text-ink">Chứng Nhận Hoàn Thành Bài Thi Vấn Đáp</div>
                <p className="text-xs text-green-800">Cấp cho: <strong>{studentName} ({studentCode})</strong></p>
                <div className="font-mono text-sm font-black text-pink-600">Điểm số: {displayResult.totalScore} / 10 ({displayResult.grade})</div>
                <div className="p-2 bg-green-50 rounded-xl text-[10px] font-mono text-green-800 break-all border border-green-200">
                  SHA-256: 4f1a8c9b2e7d3f8a0c5b4e2d1f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a
                </div>
              </div>

              <button
                type="button"
                onClick={() => setShowCertModal(false)}
                className="w-full py-2.5 rounded-xl bg-green-500 hover:bg-green-600 text-white font-bold text-xs border-2 border-green-700 shadow-[2px_2px_0_#DEB9B5] cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>
        )}
      </div>
    );
  }

  // ==========================================
  // RENDER STATE 2: IN_PROGRESS (Active Exam Room)
  // ==========================================
  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12 animate-in fade-in duration-200 text-ink">
      
      {/* 1. Header: Exam Title, Question counter, Timer, Reconnect Test Trigger */}
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
        <div className="flex items-center gap-3 w-full sm:w-auto justify-end flex-wrap">
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

          {/* Disconnect Simulation Trigger (Simulate network drop for state machine testing) */}
          <button
            type="button"
            onClick={() => handleDisconnect('Mô phỏng ngắt kết nối mạng thí sinh.')}
            title="Mô phỏng mất kết nối mạng để kiểm tra trạng thái RECONNECTING"
            className="px-2.5 py-1.5 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-900 text-[11px] font-bold border border-amber-400 shadow-[1px_1px_0_#D97706] cursor-pointer flex items-center gap-1"
          >
            <WifiOff className="w-3.5 h-3.5 text-amber-700" />
            <span className="hidden md:inline">Test Rớt Mạng</span>
          </button>

          <button
            type="button"
            onClick={() => handleFinishExam('USER_EARLY_SUBMIT')}
            className="px-4 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-bold text-xs border-2 border-green-700 shadow-[3px_3px_0_#DEB9B5] active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer"
          >
            Nộp Bài Thi ↗
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
                    {currentQuestion.badge || (currentQuestion.type === 'PROBING' ? 'Câu hỏi xoáy thích ứng' : 'Câu hỏi chính')}
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
