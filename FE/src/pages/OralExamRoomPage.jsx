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
import { NoAttemptView } from '../components/exam/stages/NoAttemptView';
import { ReadyStage } from '../components/exam/stages/ReadyStage';
import { ReconnectingStage } from '../components/exam/stages/ReconnectingStage';
import { FinalizingStage } from '../components/exam/stages/FinalizingStage';
import { InterruptedStage } from '../components/exam/stages/InterruptedStage';
import { CompletedStage } from '../components/exam/stages/CompletedStage';
import { EMPTY_QUESTION, NO_ANSWER_TEXT, mapTurnsToQuestions, mapRubricsFromTurns, toExamResult, formatTime } from '../components/exam/examRoomModel';


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
    stopTTS();
    setExamStage('FINALIZING');
    setFinalizingStep(1);

    try {
      // Step 1 (real): close the microphone and the STT stream
      await stopRecording();
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

      // Step 3 (real): wait for the server to grade the attempt
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

  const studentName = profile?.fullName || currentUser?.fullName || '—';
  const studentCode = profile?.userCode || currentUser?.userCode || '—';

  // ==========================================
  // RENDER STATE 1: READY (Pre-flight check)
  // ==========================================
  if (!attemptId) {
    return <NoAttemptView onNavigate={onNavigate} />;
  }

  if (examStage === 'READY') {
    return <ReadyStage handleStartExam={handleStartExam} handleTestSpeaker={handleTestSpeaker} isAiSpeaking={isAiSpeaking} preflightMicOk={preflightMicOk} preflightMicVolume={preflightMicVolume} preflightSpeakerOk={preflightSpeakerOk} preflightTestingMic={preflightTestingMic} setPreflightMicOk={setPreflightMicOk} startPreflightMicTest={startPreflightMicTest} stopPreflightMic={stopPreflightMic} studentCode={studentCode} studentName={studentName} />;
  }

  // ==========================================
  // RENDER STATE 3: RECONNECTING (Yellow Warning Modal/Banner)
  // ==========================================
  if (examStage === 'RECONNECTING') {
    return <ReconnectingStage handleReconnect={handleReconnect} isReconnecting={isReconnecting} onNavigate={onNavigate} reconnectGraceLeft={reconnectGraceLeft} />;
  }

  // ==========================================
  // RENDER STATE 4: FINALIZING (Full-screen overlay/banner)
  // ==========================================
  if (examStage === 'FINALIZING') {
    return <FinalizingStage finalizingStep={finalizingStep} transcript={transcript} />;
  }

  // ==========================================
  // RENDER STATE 6: INTERRUPTED (Red Neo-brutalist Card)
  // ==========================================
  if (examStage === 'INTERRUPTED') {
    return <InterruptedStage attemptId={attemptId} copiedAttempt={copiedAttempt} copyToClipboard={copyToClipboard} interruptedReason={interruptedReason} onNavigate={onNavigate} setShowSupportModal={setShowSupportModal} showSupportModal={showSupportModal} studentCode={studentCode} />;
  }

  // ==========================================
  // RENDER STATE 5: COMPLETED (Rubric Report & Certificate)
  // ==========================================
  if (examStage === 'COMPLETED') {
    return <CompletedStage attemptId={attemptId} examResult={examResult} onNavigate={onNavigate} questions={questions} studentCode={studentCode} studentName={studentName} />;
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
              <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border flex items-center gap-1 ${
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
            <span className="text-xs font-bold text-green-700 uppercase tracking-wider block">Tiến độ câu hỏi</span>
            <p className="text-xs sm:text-sm font-extrabold text-ink">
              Lượt {activeQuestionIndex + 1}{totalQuestions ? ` • ${totalQuestions} câu chính` : ''}
            </p>
          </div>

          <div aria-live="polite" aria-label="Thời gian ca thi còn lại" className={`text-right font-mono px-3.5 py-1.5 rounded-xl border transition-all duration-300 ${
            secondsLeft <= 30 && secondsLeft > 0
              ? 'bg-rose-100 border-rose-300 text-rose-900 animate-pulse shadow-sm ring-2 ring-rose-200' 
              : 'bg-pink-50 border-pink-200 text-pink-900 shadow-2xs'
          }`}>
            <span className={`text-xs font-bold uppercase tracking-wider block ${secondsLeft <= 30 && secondsLeft > 0 ? 'text-rose-700 font-extrabold' : 'text-pink-700'}`}>
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
            <span className="text-xs font-bold uppercase tracking-wider block text-green-700">
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
              <div className={`absolute -bottom-2 -right-2 px-2 py-0.5 rounded-full text-xs font-bold border-2 ${
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
                <span className="text-xs font-bold text-green-700 uppercase tracking-wider block">
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
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border-2 ${
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
              <span className="text-xs font-mono text-green-700 font-bold">
                {isRecording ? 'Đang Thu Âm...' : 'Micro Sẵn Sàng'}
              </span>
            </div>

            {/* Realtime 60fps Canvas Visualizer when student speaks */}
            {isRecording && (
              <div className="p-3 bg-paper rounded-2xl border border-pink-200 flex items-center justify-between shadow-2xs animate-in fade-in">
                <div className="flex items-center gap-2.5">
                  <AudioVisualizerCanvas isActive={true} volume={0.9} barCount={18} width={150} height={24} primaryColor="#CE9893" accentColor="#819266" />
                  <span className="text-xs text-pink-950 font-bold">Đang nghe câu trả lời của bạn...</span>
                </div>
              </div>
            )}

            {/* Realtime transcript live typing effect */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-green-800 uppercase tracking-wider block">
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
                <div aria-live="polite" className="p-3 bg-slate-50 border border-pink-200 rounded-xl text-xs text-slate-800 font-medium animate-in fade-in">
                  <span className="font-bold text-pink-800 block text-xs uppercase tracking-wider mb-0.5">
                    Đang nhận dạng:
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

              <div className="flex items-center justify-between text-xs text-green-800 pt-1">
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
                      <span className={`w-5 h-5 rounded-lg flex items-center justify-center text-xs font-bold ${
                        isCurrent ? 'bg-white text-ink font-bold' : isAnswered ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-700'
                      }`}>
                        {idx + 1}
                      </span>
                      <span className="truncate">{q.title || `Câu ${idx + 1}`}</span>
                    </div>
                    {isAnswered && (
                      <span className="text-xs font-bold text-emerald-700 shrink-0">Đã nộp ✓</span>
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
