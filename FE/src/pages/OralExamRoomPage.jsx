import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/userApi';
import { oralExamApi } from '../api/oralExamApi';
import { getErrorMessage } from '../utils/errorCodes';
import { getActiveExam, gradeLabel } from '../utils/activeExam';
import { speechApi } from '../api/speechApi';
import { useVoiceSTT } from '../hooks/useVoiceSTT';
import { useZeroTTS } from '../hooks/useZeroTTS';
import { pickVoice } from '../utils/speechConfig';
import { TURN_TYPE, isFollowUp, turnBadge, turnTitle } from '../utils/turnTypes';
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
  Check,
  Loader2
} from 'lucide-react';
import { VoiceWaveform } from '../components/common/VoiceWaveform';
import { AudioVisualizerCanvas } from '../components/exam/AudioVisualizerCanvas';

// Placeholder rendered until the attempt's first turn arrives from the BE
const EMPTY_QUESTION = { content: '', title: 'Đang tải câu hỏi...', badge: '', type: TURN_TYPE.MAIN };

/** Text recorded when the answer time runs out with nothing said, so the turn can still be graded. */
const NO_ANSWER_TEXT = '[Thí sinh không trả lời trong thời gian quy định]';

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
    type: t.turnType || TURN_TYPE.MAIN,
    badge: turnBadge(t.turnType),
    title: turnTitle(t.turnType, t.turnOrder || idx + 1),
    content: t.questionText || '',
    ttsAudioUrl: t.ttsAudioUrl,
    hint: isFollowUp(t.turnType) ? 'Giải thích rõ hơn ý bạn vừa trả lời.' : '',
    rubricCriteria: t.rubricEvaluations || 'Đánh giá năng lực chuyên môn và phản xạ kiến trúc.'
  }));
};

/**
 * Maps backend turns to per-turn rubric results
 */
const mapRubricsFromTurns = (turns) => {
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
        name: `Lượt ${t.turnOrder || i + 1}: ${turnBadge(t.turnType)}`,
        score: t.score != null ? Number(t.score) : null,
        max: 10,
        comment: t.feedback || ''
      });
    });
    return list;
  }
  return [];
};

/** Builds the COMPLETED-screen result from the BE attempt detail; score stays null while grading is pending */
const toExamResult = (detail) => ({
  totalScore: detail?.totalScore != null ? Number(detail.totalScore) : null,
  grade: gradeLabel(detail?.gradeLevel),
  maxScore: 10,
  feedback: detail?.detailedFeedback || '',
  rubrics: mapRubricsFromTurns(detail?.turns)
});

export function OralExamRoomPage({
  onNavigate,
  showToast
}) {
  // The attempt comes from DiscoverPage (POST /student/exams/{id}/enter)
  const activeExam = getActiveExam();
  const initialAttemptId = activeExam?.attemptId || null;
  const { currentUser } = useAuth();
  const [profile, setProfile] = useState(currentUser || null);

  // 6-State Machine: 'READY' -> 'IN_PROGRESS' <-> 'RECONNECTING' -> 'FINALIZING' -> 'COMPLETED' / 'INTERRUPTED'
  const [examStage, setExamStage] = useState('READY');

  // Attempt & Questions state
  const [attemptId, setAttemptId] = useState(initialAttemptId);
  const [questions, setQuestions] = useState([]);
  const [totalQuestions, setTotalQuestions] = useState(activeExam?.questionCount || 0);
  const [activeQuestionIndex, setActiveQuestionIndex] = useState(0);
  const [secondsLeft, setSecondsLeft] = useState(activeExam?.durationSeconds || 900);
  const [isSubmittingTurn, setIsSubmittingTurn] = useState(false);
  // Per-answer timer, started by the server once the question has been read (POST .../ready)
  const [answerDeadlineMs, setAnswerDeadlineMs] = useState(null);
  const [answerSecondsLeft, setAnswerSecondsLeft] = useState(null);
  const autoSubmittedTurnRef = useRef(null);
  // Admin speech settings (Nhóm 7): STT/TTS language and voice used in this room
  const [speechSettings, setSpeechSettings] = useState(null);
  // Set when the STT service fails; only then may the student type (fallback)
  const [sttUnavailable, setSttUnavailable] = useState(false);

  // Reconnection state (Grace period max 120s)
  const [reconnectGraceLeft, setReconnectGraceLeft] = useState(120);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const [interruptedReason, setInterruptedReason] = useState('');

  // Finalizing synchronization progress (step 1, 2, 3)
  const [finalizingStep, setFinalizingStep] = useState(1);

  // Support / Certificate Modal states
  const [showSupportModal, setShowSupportModal] = useState(false);
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
  // Voice STT Hook. An STT failure only warns the student: the recording is still uploaded
  // with the answer, and losing the STT stream is not a lost exam connection.
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
    language: speechSettings?.sttLanguage || 'vi-VN',
    onError: (err) => {
      console.warn('STT Hook Warning:', err);
      setSttUnavailable(true);
      showToast?.({
        type: 'warning',
        title: 'Nhận dạng giọng nói tạm gián đoạn',
        message: 'Bạn vẫn có thể tiếp tục trả lời, bản ghi âm sẽ được gửi kèm câu trả lời.'
      });
    }
  });

  useEffect(() => {
    if (isWsConnected) setSttUnavailable(false);
  }, [isWsConnected]);

  // Voice TTS Hook (voice, language and speed from the admin speech settings)
  const { isPlaying: isAiSpeaking, play: playTTS, stop: stopTTS } = useZeroTTS({
    voiceId: pickVoice(speechSettings),
    language: speechSettings?.ttsLanguage || 'vi-VN',
    speed: speechSettings?.speed || 1.0,
    onError: (err) => {
      console.warn('TTS Hook Warning:', err);
    }
  });

  // Current Question
  const currentQuestion = questions[activeQuestionIndex] || questions[0] || EMPTY_QUESTION;

  // Sync profile & exam data on mount with Auto-Resume
  useEffect(() => {
    let isMounted = true;

    async function loadData() {
      try {
        const u = await userApi.getCurrentUser();
        if (isMounted) setProfile(u);
      } catch (e) {}

      try {
        const settings = await speechApi.getSettings();
        if (isMounted && settings) setSpeechSettings(settings);
      } catch (e) {
        console.warn('Speech settings unavailable, using Vietnamese defaults:', e);
      }

      let targetAttemptId = attemptId || initialAttemptId;

      // Session Auto-Resume: If no active attemptId in state, recover from active student exam session
      if (!targetAttemptId) {
        try {
          const examsData = await oralExamApi.listExams();
          const examList = Array.isArray(examsData) ? examsData : examsData?.content || [];
          const activeItem = examList.find(e => 
            e.studentAttemptId && ['IN_PROGRESS', 'READY', 'RECONNECTING'].includes(e.studentAttemptStatus)
          );
          if (activeItem) {
            targetAttemptId = activeItem.studentAttemptId;
            if (isMounted) {
              setAttemptId(targetAttemptId);
              if (activeItem.subjectName && showToast) {
                showToast({
                  type: 'info',
                  title: 'Khôi Phục Ca Thi',
                  message: `Đã tự động kết nối lại ca thi đang diễn ra môn ${activeItem.subjectName}.`
                });
              }
            }
          }
        } catch (recoverErr) {
          console.warn('Auto-resume failed to find active exam attempt:', recoverErr);
        }
      }

      if (targetAttemptId) {
        try {
          const detail = await oralExamApi.getAttemptDetail(targetAttemptId);
          if (isMounted && detail) {
            if (detail.totalQuestions) setTotalQuestions(detail.totalQuestions);
            const mappedQuestions = mapTurnsToQuestions(detail.turns, detail.totalQuestions);
            if (mappedQuestions && mappedQuestions.length > 0) {
              setQuestions(mappedQuestions);
              // Resume on the latest turn that still awaits an answer
              const pendingIdx = detail.turns.findIndex(t => t.status === 'PENDING');
              setActiveQuestionIndex(pendingIdx >= 0 ? pendingIdx : mappedQuestions.length - 1);
            }
            if (detail.remainingSeconds != null && detail.remainingSeconds > 0) {
              setSecondsLeft(detail.remainingSeconds);
            }
            if (detail.graceDeadline) {
              const diffSec = Math.max(0, Math.floor((new Date(detail.graceDeadline).getTime() - Date.now()) / 1000));
              setReconnectGraceLeft(diffSec);
            }
            if (detail.status && ['READY', 'IN_PROGRESS', 'RECONNECTING', 'FINALIZING', 'COMPLETED', 'INTERRUPTED'].includes(detail.status)) {
              setExamStage(detail.status);
              if (detail.status === 'COMPLETED') {
                setExamResult(toExamResult(detail));
              } else if (detail.status === 'INTERRUPTED') {
                setInterruptedReason(detail.detailedFeedback || 'Ca thi bị gián đoạn do mất kết nối hoặc sự cố đồng bộ.');
              }
            }
          }
        } catch (err) {
          if (isMounted && showToast) {
            showToast({ type: 'error', title: 'Không Tải Được Ca Thi', message: getErrorMessage(err) });
          }
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

  // 4. Read the question aloud, then ask the server to start the answer timer.
  //    Reading time is not counted against the student (ExamTurn.askedAt is set after TTS).
  useEffect(() => {
    if (examStage !== 'IN_PROGRESS' || !currentQuestion?.turnId) return undefined;
    let cancelled = false;
    resetTranscript();
    setManualText(answersByQuestion[activeQuestionIndex]?.text || '');
    setAnswerDeadlineMs(null);
    setAnswerSecondsLeft(null);
    const turnId = currentQuestion.turnId;
    const timer = setTimeout(async () => {
      try {
        await playTTS(currentQuestion.content);
      } catch (e) {
        // Reading failed: start the timer anyway so the exam keeps moving
      }
      if (cancelled || !attemptId) return;
      try {
        const ready = await oralExamApi.markTurnReady(attemptId, turnId);
        if (cancelled || !ready) return;
        const skewMs = ready.serverTime ? Date.now() - new Date(ready.serverTime).getTime() : 0;
        if (ready.answerDeadline) {
          setAnswerDeadlineMs(new Date(ready.answerDeadline).getTime() + skewMs);
        }
        if (ready.remainingSeconds != null) setSecondsLeft(ready.remainingSeconds);
      } catch (err) {
        console.warn('markTurnReady failed:', err);
      }
    }, 500);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [activeQuestionIndex, examStage, currentQuestion?.turnId]);

  // 5. Per-answer countdown; when it reaches zero the answer is submitted automatically
  useEffect(() => {
    if (examStage !== 'IN_PROGRESS' || !answerDeadlineMs) return undefined;
    const tick = () => setAnswerSecondsLeft(Math.max(0, Math.ceil((answerDeadlineMs - Date.now()) / 1000)));
    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [examStage, answerDeadlineMs]);

  useEffect(() => {
    const turnId = currentQuestion?.turnId;
    if (examStage === 'IN_PROGRESS' && answerSecondsLeft === 0 && turnId
        && !isSubmittingTurn && autoSubmittedTurnRef.current !== turnId) {
      autoSubmittedTurnRef.current = turnId;
      showToast?.({ type: 'warning', title: 'Hết giờ trả lời câu này', message: 'Câu trả lời hiện tại được nộp tự động.' });
      handleSubmitTurn({ auto: true });
    }
  }, [answerSecondsLeft, examStage, isSubmittingTurn, currentQuestion?.turnId]);

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

    if (!attemptId) {
      showToast?.({ type: 'error', title: 'Chưa Chọn Ca Thi', message: 'Vui lòng chọn ca thi tại Cổng Thi trước khi bắt đầu.' });
      onNavigate?.('discover');
      return;
    }

    let sessionRes;
    try {
      sessionRes = await oralExamApi.startExamSession(attemptId, true);
    } catch (err) {
      showToast?.({ type: 'error', title: 'Không Thể Bắt Đầu Bài Thi', message: getErrorMessage(err) });
      return;
    }

    if (sessionRes?.totalQuestions) setTotalQuestions(sessionRes.totalQuestions);
    if (sessionRes?.remainingSeconds != null && sessionRes.remainingSeconds > 0) {
      setSecondsLeft(sessionRes.remainingSeconds);
    }
    if (sessionRes?.currentTurnId) {
      const existingIdx = questions.findIndex(q => q.turnId === sessionRes.currentTurnId);
      if (existingIdx >= 0) {
        setActiveQuestionIndex(existingIdx);
      } else {
        const number = questions.length + 1;
        setQuestions(prev => [...prev, {
          id: number,
          turnId: sessionRes.currentTurnId,
          questionNumber: number,
          type: TURN_TYPE.MAIN,
          badge: turnBadge(TURN_TYPE.MAIN),
          title: turnTitle(TURN_TYPE.MAIN, number),
          content: sessionRes.currentQuestionText || '',
          ttsAudioUrl: sessionRes.currentTtsAudioUrl
        }]);
        setActiveQuestionIndex(questions.length);
      }
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
        const disRes = await oralExamApi.disconnectExam(attemptId);
        if (disRes?.graceDeadline) {
          const diffSec = Math.max(0, Math.floor((new Date(disRes.graceDeadline).getTime() - Date.now()) / 1000));
          setReconnectGraceLeft(diffSec);
        } else {
          setReconnectGraceLeft(120);
        }
        if (disRes?.remainingSeconds != null && disRes.remainingSeconds > 0) {
          setSecondsLeft(disRes.remainingSeconds);
        }
      } catch (err) {
        // Expected while offline: the BE grace window (120s) still applies server-side
        setReconnectGraceLeft(120);
      }
    } else {
      setReconnectGraceLeft(120);
    }
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
        const reconRes = await oralExamApi.reconnectExam(attemptId);
        if (reconRes?.remainingSeconds != null && reconRes.remainingSeconds > 0) {
          setSecondsLeft(reconRes.remainingSeconds);
        }
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
  const handleSubmitTurn = async ({ auto = false } = {}) => {
    if (isSubmittingTurn) return;
    if (!attemptId || !currentQuestion.turnId) {
      showToast?.({ type: 'error', message: 'Không xác định được lượt hỏi hiện tại. Vui lòng tải lại phòng thi.' });
      return;
    }
    setIsSubmittingTurn(true);

    // Wait for the STT "final" so the whole answer is submitted, not the last partial
    const recorded = await stopRecording();
    const spoken = recorded?.transcript || transcript;
    const answerAudio = recorded?.audioBlob || audioBlob;
    let answerContent = [manualText, spoken].filter(Boolean).join(' ').trim();

    if (!answerContent && !answerAudio) {
      if (!auto) {
        setIsSubmittingTurn(false);
        showToast?.({ type: 'warning', title: 'Chưa Có Câu Trả Lời', message: 'Hãy trả lời bằng giọng nói trước khi nộp.' });
        return;
      }
      answerContent = NO_ANSWER_TEXT;
    }

    let evalRes;
    try {
      evalRes = await oralExamApi.submitTurnAnswer(attemptId, currentQuestion.turnId, {
        transcriptText: answerContent,
        audioBlob: answerAudio
      });
    } catch (err) {
      setIsSubmittingTurn(false);
      showToast?.({ type: 'error', title: 'Nộp Câu Trả Lời Thất Bại', message: getErrorMessage(err) });
      return;
    }

    // Save answer locally with evaluation metrics if available
    setAnswersByQuestion((prev) => ({
      ...prev,
      [activeQuestionIndex]: {
        questionId: currentQuestion.id,
        turnId: currentQuestion.turnId,
        text: answerContent,
        audioBlob: answerAudio,
        score: evalRes?.score,
        feedback: evalRes?.feedback,
        rubricEvaluations: evalRes?.rubricEvaluations
      }
    }));

    setIsSubmittingTurn(false);
    if (evalRes?.remainingSeconds != null) {
      setSecondsLeft(evalRes.remainingSeconds);
    }
    if (evalRes?.timedOut && !auto) {
      showToast?.({
        type: 'warning',
        title: 'Câu trả lời nộp trễ',
        message: 'Câu trả lời đã được ghi nhận nhưng quá thời gian quy định, giảng viên sẽ thấy ghi chú này.'
      });
    }

    // If backend indicates attempt is finished
    if (evalRes?.isAttemptFinished) {
      handleFinishExam('ALL_QUESTIONS_ANSWERED');
      return;
    }

    // If backend generated adaptive probe question or dynamic next turn
    if (evalRes?.nextQuestionText && evalRes?.nextTurnId) {
      const isProbing = isFollowUp(evalRes.nextTurnType);
      const nextIndex = activeQuestionIndex + 1;
      setQuestions((prevQuestions) => {
        const updated = [...prevQuestions];
        const nextQData = {
          id: nextIndex + 1,
          turnId: evalRes.nextTurnId,
          questionNumber: nextIndex + 1,
          totalQuestions: Math.max(updated.length, nextIndex + 1),
          type: evalRes.nextTurnType || (isProbing ? TURN_TYPE.FOLLOW_UP : TURN_TYPE.MAIN),
          badge: turnBadge(evalRes.nextTurnType),
          title: turnTitle(evalRes.nextTurnType, nextIndex + 1),
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
          title: 'Câu hỏi làm rõ',
          message: 'Giám khảo AI hỏi thêm để làm rõ câu trả lời vừa rồi.'
        });
      }
      return;
    }

    // BE returned no next turn: the interview is over
    handleFinishExam('ALL_QUESTIONS_ANSWERED');
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
      try {
        await oralExamApi.finishExam(attemptId);
      } catch (apiErr) {
        // A 4xx means the BE already moved the attempt past IN_PROGRESS (e.g. last turn auto-finished it);
        // polling below picks up the real status. Network/5xx errors are real failures.
        if (!apiErr?.status || apiErr.status >= 500) {
          throw apiErr;
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

      // syncSuccess(), or still grading after the polling window: the score stays empty until the BE has it
      setExamResult(toExamResult(syncStatus === 'COMPLETED' ? detailResult : null));
      setExamStage('COMPLETED');
      if (showToast) {
        showToast(syncStatus === 'COMPLETED'
          ? { type: 'success', title: 'Ca Thi Hoàn Tất', message: 'Dữ liệu đã được đồng bộ thành công. Báo cáo đánh giá đã sẵn sàng!' }
          : { type: 'info', title: 'Đã Nộp Bài', message: 'Bài thi đang được chấm. Xem điểm tại Cổng Thi khi có kết quả.' });
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
  if (!attemptId) {
    return (
      <div className="max-w-md mx-auto py-12 px-4">
        <div className="bg-white border border-pink-200 rounded-3xl p-8 shadow-md text-center space-y-4">
          <BookOpen className="w-10 h-10 text-green-700 mx-auto" />
          <h2 className="font-heading font-black text-xl text-ink">Chưa chọn ca thi</h2>
          <p className="text-xs text-green-800">Hãy chọn một ca thi đang mở tại Cổng Thi để vào phòng thi vấn đáp.</p>
          <button
            type="button"
            onClick={() => onNavigate?.('discover')}
            className="px-6 py-2.5 rounded-xl bg-green-500 hover:bg-green-600 text-white font-bold text-xs border border-pink-200 cursor-pointer focus:outline-none focus:ring-2 focus:ring-green-600"
          >
            Về Cổng Thi
          </button>
        </div>
      </div>
    );
  }

  if (examStage === 'READY') {
    return (
      <div className="max-w-3xl mx-auto py-8 px-4 animate-in fade-in duration-300">
        <div className="bg-white border border-pink-200 rounded-3xl p-6 sm:p-10 shadow-md space-y-8">
          
          {/* Header */}
          <div className="flex items-center gap-4 pb-6 border-b border-pink-200">
            <div className="w-14 h-14 rounded-2xl bg-slate-100 border border-pink-200 text-slate-700 flex items-center justify-center text-2xl shadow-sm shrink-0">
              🎙️
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-[11px] font-bold uppercase tracking-wider text-green-700 block">
                  Khảo Thí Vấn Đáp AI • Trạng Thái: READY
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
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
            <div className="bg-white border border-pink-200 rounded-2xl p-5 shadow-sm flex flex-col justify-between space-y-4">
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
                <div className="h-5 bg-slate-100 border border-pink-200 rounded-lg overflow-hidden p-0.5 flex items-center">
                  <div 
                    className="h-full bg-pink-600 rounded-md transition-all duration-75"
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
                  className={`w-full py-2.5 px-3 rounded-xl font-bold text-xs border border-pink-200 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-1.5 ${
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
                  className="w-full py-2.5 px-3 rounded-xl font-bold text-xs bg-white hover:bg-emerald-50 text-emerald-800 border-2 border-emerald-400 shadow-sm cursor-pointer flex items-center justify-center gap-1.5"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>{preflightMicOk ? 'Micro Đạt ✓' : 'Xác Nhận Mic'}</span>
                </button>
              </div>
            </div>

            {/* 2. Test Speaker */}
            <div className="bg-white border border-pink-200 rounded-2xl p-5 shadow-sm flex flex-col justify-between space-y-4">
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

              <div className="p-3 bg-slate-50 border border-pink-200 rounded-xl text-xs text-slate-700 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-pink-600 shrink-0" />
                <span>ZeroTTS &amp; Web Audio Engine: Sẵn sàng phát âm.</span>
              </div>

              <button
                type="button"
                onClick={handleTestSpeaker}
                className="w-full py-2.5 px-4 rounded-xl font-bold text-xs bg-green-100 hover:bg-green-200 text-green-900 border border-pink-200 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2"
              >
                <Volume2 className="w-4 h-4" />
                <span>{isAiSpeaking ? 'Đang Phát Âm Thử...' : 'Phát Thử Giọng Giám Thị AI'}</span>
              </button>
            </div>

          </div>

          {/* Action Footer: "Bắt đầu thi" */}
          <div className="pt-4 border-t border-pink-200 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="text-xs text-green-800">
              * Khuyến nghị dùng tai nghe và micro rời để đạt chất lượng nhận diện giọng nói cao nhất.
            </div>
            <button
              type="button"
              onClick={handleStartExam}
              className="w-full sm:w-auto px-8 py-3.5 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-black text-sm border border-pink-200 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2"
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
        <div className="bg-amber-50 border border-amber-300 rounded-3xl p-6 sm:p-10 shadow-xl text-amber-950 space-y-6">
          
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-amber-100 border border-amber-300 text-amber-800 flex items-center justify-center text-2xl shadow-xs shrink-0">
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

          <div className="bg-white/80 border-2 border-amber-500 rounded-2xl p-5 shadow-sm space-y-4">
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
              className="w-full sm:w-auto px-8 py-3.5 rounded-2xl bg-amber-500 hover:bg-amber-600 text-amber-950 font-black text-xs sm:text-sm border-2 border-amber-800 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${isReconnecting ? 'animate-spin' : ''}`} />
              <span>{isReconnecting ? 'Đang Kiểm Tra Kết Nối...' : 'Thử Kết Nối Lại Ngay'}</span>
            </button>

            <button
              type="button"
              onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
              className="w-full sm:w-auto px-5 py-3 rounded-2xl bg-white hover:bg-amber-50 text-amber-900 font-bold text-xs border-2 border-amber-600 shadow-sm cursor-pointer"
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
        <div className="bg-white border border-pink-200 rounded-3xl p-8 sm:p-10 max-w-xl w-full shadow-lg space-y-6 text-center text-ink">
          
          <div className="w-20 h-20 rounded-3xl bg-slate-100 border border-pink-200 text-slate-700 mx-auto flex items-center justify-center text-3xl shadow-sm animate-pulse">
            <Loader2 className="w-10 h-10 text-pink-600 animate-spin" />
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
              finalizingStep >= 1 ? 'bg-white border-slate-900 text-ink font-semibold shadow-2xs' : 'bg-slate-50 border-pink-200 text-slate-400'
            }`}>
              <div className="flex items-center gap-2">
                <span className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] ${
                  finalizingStep > 1 ? 'bg-emerald-600 text-white' : 'bg-pink-600 text-white'
                }`}>
                  {finalizingStep > 1 ? '✓' : <Loader2 className="w-3 h-3 animate-spin" />}
                </span>
                <span>1. Đóng gói transcript &amp; bản ghi âm các lượt vấn đáp</span>
              </div>
              <span className="text-[10px] text-green-700">
                {finalizingStep > 1 ? 'Hoàn tất ✓' : 'Đang xử lý...'}
              </span>
            </div>

            <div className={`p-3.5 rounded-2xl border-2 flex items-center justify-between text-xs transition ${
              finalizingStep >= 2 ? 'bg-white border-slate-900 font-semibold shadow-sm' : 'bg-gray-100 border-gray-300 text-gray-500'
            }`}>
              <div className="flex items-center gap-2">
                <span className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] ${
                  finalizingStep > 2 ? 'bg-emerald-600 text-white' : finalizingStep === 2 ? 'bg-pink-600 text-white' : 'bg-slate-200 text-slate-600'
                }`}>
                  {finalizingStep > 2 ? '✓' : finalizingStep === 2 ? <Loader2 className="w-3 h-3 animate-spin" /> : '2'}
                </span>
                <span>2. Đồng bộ hóa dữ liệu với máy chủ khảo thí trung tâm</span>
              </div>
              <span className="text-[10px] text-green-700">
                {finalizingStep > 2 ? 'Hoàn tất ✓' : finalizingStep === 2 ? 'Đang đồng bộ...' : 'Chờ...'}
              </span>
            </div>

            <div className={`p-3.5 rounded-2xl border-2 flex items-center justify-between text-xs transition ${
              finalizingStep >= 3 ? 'bg-white border-slate-900 font-semibold shadow-sm' : 'bg-gray-100 border-gray-300 text-gray-500'
            }`}>
              <div className="flex items-center gap-2">
                <span className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] ${
                  finalizingStep === 3 ? 'bg-pink-600 text-white' : 'bg-slate-200 text-slate-600'
                }`}>
                  {finalizingStep === 3 ? <Loader2 className="w-3 h-3 animate-spin" /> : '3'}
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
        <div className="bg-rose-50 border-4 border-rose-600 rounded-3xl p-6 sm:p-10 shadow-md text-rose-950 space-y-6">
          
          <div className="flex items-center gap-4 pb-4 border-b-2 border-rose-200">
            <div className="w-16 h-16 rounded-2xl bg-rose-200 border-2 border-rose-700 text-rose-800 flex items-center justify-center text-3xl shadow-sm shrink-0">
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

          <div className="bg-white border-2 border-rose-400 rounded-2xl p-5 shadow-sm space-y-3">
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
            <span>Mã ca thi (Attempt ID): #{attemptId}</span>
            <button
              type="button"
              onClick={() => copyToClipboard(attemptId)}
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
              className="w-full sm:w-auto px-6 py-3.5 rounded-2xl bg-rose-600 hover:bg-rose-700 text-white font-black text-xs sm:text-sm border-2 border-rose-900 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2"
            >
              <PhoneCall className="w-4 h-4" />
              <span>Liên Hệ Giám Thị / Hỗ Trợ Kỹ Thuật</span>
            </button>

            <button
              type="button"
              onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
              className="w-full sm:w-auto px-6 py-3 rounded-2xl bg-white hover:bg-rose-50 text-rose-900 font-bold text-xs border-2 border-rose-500 shadow-sm cursor-pointer"
            >
              ← Quay Lại Danh Sách Ca Thi
            </button>
          </div>

        </div>

        {/* Support Modal dialog */}
        {showSupportModal && (
          <div className="fixed inset-0 z-50 bg-ink/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-paper border-4 border-rose-600 rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-md space-y-5 text-ink">
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
                  Vui lòng cung cấp <strong>Mã SV: {studentCode}</strong> và <strong>Mã Ca Thi: #{attemptId}</strong> khi liên hệ để được hỗ trợ mở lại ca thi ân hạn.
                </div>
              </div>

              <button
                type="button"
                onClick={() => setShowSupportModal(false)}
                className="w-full py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs border-2 border-rose-900 shadow-sm cursor-pointer"
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
    const displayResult = examResult || toExamResult(null);

    return (
      <div className="max-w-4xl mx-auto py-8 px-4 animate-in fade-in duration-300">
        <div className="bg-white border border-pink-200 rounded-3xl p-6 sm:p-10 shadow-md space-y-8">
          
          {/* Header */}
          <div className="text-center space-y-3 pb-6 border-b border-pink-200">
            <div className="w-20 h-20 rounded-3xl bg-slate-100 border border-pink-200 text-slate-700 mx-auto flex items-center justify-center text-3xl shadow-sm">
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
            <div className="bg-white border border-pink-200 p-5 rounded-2xl shadow-sm text-center">
              <span className="text-[11px] font-bold text-green-700 uppercase tracking-wider block mb-1">
                Điểm Tổng Kết
              </span>
              <div className="font-heading font-black text-4xl text-pink-600">
                {displayResult.totalScore != null ? displayResult.totalScore : '—'} <span className="text-lg text-ink font-sans">/ {displayResult.maxScore}</span>
              </div>
            </div>

            <div className="bg-white border border-pink-200 p-5 rounded-2xl shadow-sm text-center">
              <span className="text-[11px] font-bold text-green-700 uppercase tracking-wider block mb-1">
                Xếp Loại Đạt Được
              </span>
              <div className="font-heading font-black text-2xl sm:text-3xl text-green-700">
                {displayResult.grade}
              </div>
            </div>

            <div className="bg-white border border-pink-200 p-5 rounded-2xl shadow-sm text-center">
              <span className="text-[11px] font-bold text-green-700 uppercase tracking-wider block mb-1">
                Số Lượt Câu Hỏi
              </span>
              <div className="font-heading font-black text-2xl sm:text-3xl text-ink">
                {questions.length}
              </div>
            </div>
          </div>

          {displayResult.totalScore == null && (
            <div className="p-4 bg-amber-50 border-2 border-amber-300 rounded-2xl text-xs text-amber-900 font-medium">
              Bài thi đã được nộp và đang chờ chấm điểm. Điểm sẽ hiển thị tại Cổng Thi khi giảng viên/AI hoàn tất.
            </div>
          )}

          {/* AI Comprehensive Feedback */}
          {displayResult.feedback && (
            <div className="bg-white border border-pink-200 p-5 sm:p-6 rounded-2xl shadow-2xs space-y-2">
              <div className="flex items-center gap-2 text-pink-900 font-bold text-sm">
                <Sparkles className="w-5 h-5 text-pink-600" />
                <span>Nhận Xét Chuyên Môn Từ Giám Thị AI</span>
              </div>
              <p className="text-xs sm:text-sm text-pink-950 leading-relaxed font-medium">
                "{displayResult.feedback}"
              </p>
            </div>
          )}

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
                  className="bg-white border border-pink-200 p-4 rounded-2xl shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="space-y-1">
                    <span className="font-bold text-xs text-ink block">{r.name}</span>
                    <p className="text-xs text-green-800">{r.comment}</p>
                  </div>
                  <div className="shrink-0 font-mono font-bold text-sm bg-slate-100 text-slate-800 border border-pink-200 px-3 py-1 rounded-xl self-start sm:self-center">
                    {r.score != null ? r.score : '—'} / {r.max}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Actions */}
          <div className="pt-6 border-t border-pink-200 flex flex-col sm:flex-row items-center justify-between gap-4">
            <button
              type="button"
              onClick={() => onNavigate ? onNavigate('discover') : window.location.reload()}
              className="px-6 py-3 rounded-2xl bg-white hover:bg-green-50 text-green-900 font-bold text-xs border border-pink-200 shadow-sm cursor-pointer"
            >
              ← Trở Về Danh Sách Học Phần
            </button>

          </div>

        </div>

      </div>
    );
  }

  // ==========================================
  // RENDER STATE 2: IN_PROGRESS (Active Exam Room)
  // ==========================================
  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12 animate-in fade-in duration-200 text-ink">
      
      {/* 1. Header: Exam Title, Question counter, Timer, Reconnect Test Trigger */}
      <div className="bg-white border border-pink-200 rounded-3xl p-5 shadow-md flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-2xl bg-slate-100 border border-pink-200 text-slate-700 flex items-center justify-center text-xl shadow-sm shrink-0">
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
                {isWsConnected ? 'Nhận dạng giọng nói: sẵn sàng' : 'Nhận dạng giọng nói: chờ kết nối'}
              </span>
            </div>
            <p className="text-xs text-green-800 mt-0.5">
              Thí sinh: <strong>{studentName} ({studentCode})</strong> • Ca thi: <strong>{activeExam?.title || '—'}</strong>
            </p>
          </div>
        </div>

        {/* Question Counter & Countdown Timer */}
        <div className="flex items-center gap-3 w-full sm:w-auto justify-end flex-wrap">
          <div className="text-right font-mono bg-white border border-pink-200 px-3.5 py-1.5 rounded-xl shadow-sm">
            <span className="text-[9px] font-bold text-green-700 uppercase tracking-wider block">Tiến độ câu hỏi</span>
            <p className="text-xs sm:text-sm font-extrabold text-ink">
              Lượt {activeQuestionIndex + 1}{totalQuestions ? ` • ${totalQuestions} câu chính` : ''}
            </p>
          </div>

          <div className={`text-right font-mono px-3.5 py-1.5 rounded-xl border transition-all duration-300 ${
            secondsLeft <= 30 && secondsLeft > 0
              ? 'bg-rose-100 border-rose-300 text-rose-900 animate-pulse shadow-sm ring-2 ring-rose-200' 
              : 'bg-pink-50 border-pink-200 text-pink-900 shadow-2xs'
          }`}>
            <span className={`text-[9px] font-bold uppercase tracking-wider block ${secondsLeft <= 30 && secondsLeft > 0 ? 'text-rose-700 font-extrabold' : 'text-pink-700'}`}>
              {secondsLeft <= 30 && secondsLeft > 0 ? '⚡ Sắp Hết Giờ!' : 'Thời Gian Ca Thi'}
            </span>
            <p className={`text-xs sm:text-sm font-black ${secondsLeft <= 30 && secondsLeft > 0 ? 'text-rose-800' : 'text-pink-900'}`}>
              {formatTime(secondsLeft)}
            </p>
          </div>

          {/* Per-answer timer: starts after the question has been read aloud */}
          <div
            aria-live="polite"
            className={`text-right font-mono px-3.5 py-1.5 rounded-xl border transition-all duration-300 ${
              answerSecondsLeft != null && answerSecondsLeft <= 15
                ? 'bg-rose-100 border-rose-300 text-rose-900 ring-2 ring-rose-200'
                : 'bg-white border-pink-200 text-ink shadow-2xs'
            }`}
          >
            <span className="text-[11px] font-bold uppercase tracking-wider block text-green-700">
              Thời gian trả lời câu này
            </span>
            <p className="text-xs sm:text-sm font-black">
              {answerSecondsLeft == null ? (isAiSpeaking ? 'Đang đọc câu hỏi' : '--:--') : formatTime(answerSecondsLeft)}
            </p>
          </div>

          <button
            type="button"
            onClick={() => handleFinishExam('USER_EARLY_SUBMIT')}
            className="px-5 py-2.5 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] cursor-pointer"
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
          <div className="bg-white border border-pink-200 rounded-3xl p-6 shadow-md flex flex-col sm:flex-row items-center gap-6 relative overflow-hidden">
            {/* AI Avatar */}
            <div className="relative shrink-0">
              <div className={`w-28 h-28 rounded-3xl bg-pink-100 border border-pink-200 flex items-center justify-center text-5xl shadow-sm relative transition-transform ${
                isAiSpeaking ? 'scale-105 ring-4 ring-pink-400' : ''
              }`}>
                👩‍🏫
              </div>
              {/* Pulsing indicator */}
              <div className={`absolute -bottom-2 -right-2 px-2 py-0.5 rounded-full text-[10px] font-bold border-2 ${
                isAiSpeaking 
                  ? 'bg-pink-600 text-white border-blue-700 animate-pulse' 
                  : 'bg-white text-slate-700 border-pink-200 shadow-2xs'
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

              {/* High-performance Canvas soundwave when TTS is speaking */}
              <div className="h-10 flex items-center justify-center sm:justify-start">
                {isAiSpeaking ? (
                  <div className="flex items-center gap-2 px-3 py-1.5 bg-pink-100 border border-pink-200 rounded-xl shadow-2xs">
                    <AudioVisualizerCanvas isActive={true} volume={0.85} barCount={14} width={130} height={22} primaryColor="#CE9893" accentColor="#819266" />
                    <span className="text-xs font-bold text-pink-950 ml-1">Đang đọc câu hỏi...</span>
                  </div>
                ) : (
                  <span className="text-xs text-slate-600 italic">
                    Nhấn "Nghe Lại Câu Hỏi" nếu bạn muốn giám thị đọc lại đề bài.
                  </span>
                )}
              </div>

              <div className="flex items-center justify-center sm:justify-start gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => playTTS(currentQuestion.content)}
                  className="px-3.5 py-1.5 rounded-xl bg-white hover:bg-green-50 text-ink font-bold text-xs border border-pink-200 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center gap-1.5"
                >
                  <Volume2 className="w-3.5 h-3.5 text-pink-600" />
                  <span>Nghe Lại Câu Hỏi</span>
                </button>
                {isAiSpeaking && (
                  <button
                    type="button"
                    onClick={stopTTS}
                    className="px-3 py-1.5 rounded-xl bg-rose-100 hover:bg-rose-200 text-rose-800 font-bold text-xs border border-pink-200 shadow-sm cursor-pointer flex items-center gap-1"
                  >
                    <Square className="w-3 h-3 text-rose-600" />
                    <span>Dừng Đọc</span>
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Question Card: Question content, badge "Câu hỏi chính" hoặc "Câu hỏi xoáy" */}
          <div className="bg-white border border-pink-200 rounded-3xl p-6 shadow-md space-y-4">
            <div className="flex items-start justify-between gap-3 pb-3 border-b border-pink-200">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border-2 ${
                    isFollowUp(currentQuestion.type)
                      ? 'bg-rose-50 text-rose-800 border-rose-200'
                      : 'bg-green-100 text-green-900 border-green-400'
                  }`}>
                    {currentQuestion.badge || turnBadge(currentQuestion.type)}
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

            <div className="bg-white border border-pink-200 rounded-2xl p-4 shadow-sm">
              <p className="text-sm sm:text-base text-ink leading-relaxed font-medium">
                {currentQuestion.content}
              </p>
            </div>

            {currentQuestion.hint && (
              <div className="p-3 bg-slate-50 border border-pink-200 rounded-xl text-xs text-slate-700 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-pink-600 shrink-0" />
                <span><strong>Gợi ý sư phạm:</strong> {currentQuestion.hint}</span>
              </div>
            )}
          </div>

        </div>

        {/* Right Column (5 cols): Answer Section & Action Controls */}
        <div className="lg:col-span-5 space-y-6">
          
          <div className="bg-white border border-pink-200 rounded-3xl p-6 shadow-md space-y-4">
            
            <div className="flex items-center justify-between pb-3 border-b border-pink-200">
              <div className="flex items-center gap-2">
                <span className={`w-3 h-3 rounded-full border border-pink-200 ${
                  isRecording ? 'bg-rose-500 animate-ping' : 'bg-emerald-500'
                }`} />
                <h3 className="font-heading font-black text-sm text-ink uppercase tracking-wider">
                  Khu Vực Trả Lời Vấn Đáp
                </h3>
              </div>
              <span className="text-[11px] font-mono text-green-700 font-bold">
                {isRecording ? 'Đang Thu Âm...' : 'Micro Sẵn Sàng'}
              </span>
            </div>

            {/* Realtime 60fps Canvas Visualizer when student speaks */}
            {isRecording && (
              <div className="p-3 bg-paper rounded-2xl border border-pink-200 flex items-center justify-between shadow-2xs animate-in fade-in">
                <div className="flex items-center gap-2.5">
                  <AudioVisualizerCanvas isActive={true} volume={0.9} barCount={18} width={150} height={24} primaryColor="#CE9893" accentColor="#819266" />
                  <span className="text-xs font-mono text-pink-950 font-bold">Đang nhận diện giọng nói (STT)...</span>
                </div>
                <span className="text-[10px] font-mono text-slate-500 bg-white px-2 py-0.5 rounded-md border border-pink-200 font-bold">250ms Slices</span>
              </div>
            )}

            {/* Realtime transcript live typing effect */}
            <div className="space-y-1.5">
              <label className="text-[11px] font-bold text-green-800 uppercase tracking-wider block">
                Nội dung câu trả lời (Bóc băng giọng nói &amp; Soạn thảo):
              </label>
              
              <div className="relative">
                {/* Oral exam: answers are spoken. Typing is only a fallback while speech recognition is unavailable. */}
                <textarea
                  rows={6}
                  value={sttUnavailable ? manualText : [transcript, partialTranscript].filter(Boolean).join(' ')}
                  onChange={(e) => { if (sttUnavailable) setManualText(e.target.value); }}
                  readOnly={!sttUnavailable}
                  aria-label="Nội dung câu trả lời"
                  placeholder={sttUnavailable
                    ? 'Nhận dạng giọng nói đang gián đoạn: bạn vẫn ghi âm câu trả lời, có thể gõ thêm nội dung chính tại đây.'
                    : "Nhấn 'Bắt đầu nói' bên dưới và trả lời bằng giọng nói; nội dung sẽ hiện tại đây."}
                  className={`w-full p-4 rounded-2xl border border-pink-200 focus:border-pink-500 focus:ring-0 text-ink text-xs sm:text-sm leading-relaxed placeholder:text-slate-400 font-medium shadow-sm ${sttUnavailable ? 'bg-white' : 'bg-slate-50 cursor-default'}`}
                />
              </div>

              {/* Live transcript preview if recording */}
              {(transcript || partialTranscript) && (
                <div className="p-3 bg-slate-50 border border-pink-200 rounded-xl text-xs text-slate-800 font-medium animate-in fade-in">
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
                  className={`py-3 px-4 rounded-2xl font-black text-xs border border-pink-200 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 ${
                    isRecording
                      ? 'bg-rose-600 hover:bg-rose-700 text-white animate-pulse'
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
                  className="py-3 px-4 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-black text-xs border border-pink-200 shadow-sm active:translate-x-0.5 active:translate-y-0.5 transition cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
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
          <div className="bg-white border border-pink-200 rounded-3xl p-5 shadow-md space-y-3">
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
                        ? 'bg-pink-600 text-white border-slate-900 font-semibold shadow-xs'
                        : isAnswered
                        ? 'bg-emerald-50 border-emerald-400 text-emerald-900'
                        : 'bg-white border-green-300 text-green-900'
                    }`}
                  >
                    <div className="flex items-center gap-2 truncate">
                      <span className={`w-5 h-5 rounded-lg flex items-center justify-center text-[10px] font-bold ${
                        isCurrent ? 'bg-white text-ink font-bold' : isAnswered ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-700'
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
