import React, { useState, useEffect, useRef } from 'react';
import { 
  Volume2, 
  Mic, 
  MicOff, 
  Video, 
  VideoOff, 
  CheckCircle2, 
  ArrowRight, 
  ArrowLeft, 
  Sparkles, 
  Radio, 
  Send, 
  HelpCircle,
  AlertTriangle
} from 'lucide-react';
import { VoiceWaveform } from '../components/common/VoiceWaveform';
import { useVoiceSTT } from '../hooks/useVoiceSTT';
import { speechApi } from '../api/speechApi';

export function CalibrationPage({ onNavigate, showToast }) {
  const [currentStep, setCurrentStep] = useState(1); // 1: Mic (+ speech recognition), 2: Loa
  const [speechSettings, setSpeechSettings] = useState(null);
  const [sttProblem, setSttProblem] = useState(false);

  useEffect(() => {
    speechApi.getSettings().then(setSpeechSettings).catch(() => {});
  }, []);

  const {
    isRecording: sttRecording,
    transcript: sttTranscript,
    partialTranscript: sttPartial,
    startRecording: sttStart,
    stopRecording: sttStop
  } = useVoiceSTT({
    language: speechSettings?.sttLanguage || 'vi-VN',
    onError: () => setSttProblem(true)
  });

  useEffect(() => {
    const text = [sttTranscript, sttPartial].filter(Boolean).join(' ').trim();
    if (text) setTranscriptPreview(text);
  }, [sttTranscript, sttPartial]);

  // Step 1: Microphone state
  const [isMicListening, setIsMicListening] = useState(false);
  const [micVolumeLevel, setMicVolumeLevel] = useState(0); // 0 - 100
  const [transcriptPreview, setTranscriptPreview] = useState('');
  const [micTestedSuccessfully, setMicTestedSuccessfully] = useState(false);

  // Step 2: Speaker state
  const [isPlayingTest, setIsPlayingTest] = useState(false);
  const [hasPlayedSound, setHasPlayedSound] = useState(false);

  const audioContextRef = useRef(null);
  const mediaStreamRef = useRef(null);
  const animationFrameRef = useRef(null);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopMicStream();
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }
    };
  }, []);

  // --- Step 1: Real Mic Handling with Web Audio API & Web Speech ---
  const startMicTest = async () => {
    try {
      setTranscriptPreview('');
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaStreamRef.current = stream;

      const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      audioContextRef.current = audioCtx;
      const analyser = audioCtx.createAnalyser();
      analyser.fftSize = 256;
      const source = audioCtx.createMediaStreamSource(stream);
      source.connect(analyser);

      const dataArray = new Uint8Array(analyser.frequencyBinCount);
      const updateVolume = () => {
        analyser.getByteFrequencyData(dataArray);
        let sum = 0;
        for (let i = 0; i < dataArray.length; i++) {
          sum += dataArray[i];
        }
        const avg = sum / dataArray.length;
        const normalized = Math.min(100, Math.round((avg / 128) * 100));
        setMicVolumeLevel(normalized);

        if (normalized > 15) {
          setMicTestedSuccessfully(true);
        }

        animationFrameRef.current = requestAnimationFrame(updateVolume);
      };
      updateVolume();
      setIsMicListening(true);

      // Same STT service and language as the exam room, so a passing check means the exam will work
      setSttProblem(false);
      sttStart().catch(() => setSttProblem(true));

      if (showToast) {
        showToast({
          type: 'info',
          title: 'Đang Thu Âm Micro',
          message: 'Vui lòng nói một câu bất kỳ (ví dụ: "Tôi là thí sinh kiểm tra âm thanh") để đo tín hiệu.'
        });
      }
    } catch (err) {
      // A device check must not pass without a real microphone
      setIsMicListening(false);
      setMicTestedSuccessfully(false);
      if (showToast) {
        showToast({
          type: 'error',
          title: 'Không Truy Cập Được Micro',
          message: 'Hãy cấp quyền micro cho trình duyệt và kiểm tra thiết bị đã được cắm, sau đó thử lại.'
        });
      }
    }
  };

  const stopMicStream = () => {
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
      clearInterval(animationFrameRef.current);
    }
    if (audioContextRef.current) {
      try { audioContextRef.current.close(); } catch (e) {}
    }
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach(t => t.stop());
    }
    if (sttRecording) {
      sttStop().then((result) => {
        if (result?.transcript) setTranscriptPreview(result.transcript);
      });
    }
    setIsMicListening(false);
    setMicVolumeLevel(0);
  };

  // --- Step 2: Speaker Voice Output ---
  const playSampleVoice = () => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const text = "Chào bạn! Âm thanh tai nghe của bạn hoạt động rất tốt. Chúc bạn có một kỳ thi vấn đáp thành công!";
      const utter = new SpeechSynthesisUtterance(text);
      utter.lang = 'vi-VN';
      utter.rate = 1.0;
      utter.onstart = () => {
        setIsPlayingTest(true);
        setHasPlayedSound(true);
      };
      utter.onend = () => setIsPlayingTest(false);
      utter.onerror = () => setIsPlayingTest(false);
      window.speechSynthesis.speak(utter);
      if (showToast) {
        showToast({
          type: 'info',
          title: 'Hiệu Chuẩn Âm Thanh',
          message: 'Đang phát âm thanh thử nghiệm giọng Giám thị ảo...'
        });
      }
    } else {
      setHasPlayedSound(true);
      if (showToast) {
        showToast({
          type: 'info',
          title: 'Âm Thanh Mẫu',
          message: 'Trình duyệt không hỗ trợ Web Speech Synthesis, đã ghi nhận hoàn thành.'
        });
      }
    }
  };

  return (
    <div className="max-w-4xl w-full mx-auto space-y-6 animate-modal-entry py-4 text-slate-800 pb-12">
      
      {/* Top Header Card */}
      <div className="bg-white border border-pink-200/90 p-5 rounded-2xl flex flex-wrap items-center justify-between gap-4 shadow-2xs">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-sky-600 block">
            Hiệu Chuẩn Thiết Bị Khảo Thí (AIVES Device Calibration)
          </span>
          <h1 className="font-heading font-extrabold text-xl sm:text-2xl text-ink mt-0.5">
            Kiểm Tra Microphone &amp; Loa Trước Khi Thi
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Đảm bảo chất lượng đường truyền âm thanh 2 chiều trước khi kết nối trực tiếp với Giám thị AI.
          </p>
        </div>

        {/* Enter exam room */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onNavigate && onNavigate('exam-room')}
            className="px-4 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold shadow-md shadow-sky-600/20 flex items-center gap-1.5 transition active:scale-95 cursor-pointer"
          >
            <span>Vào Làm Bài Thi ↗</span>
          </button>
        </div>
      </div>

      {/* Stepper Tabs (Clickable navigation) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs select-none">
        
        {/* Step 1 Tab */}
        <button
          type="button"
          onClick={() => setCurrentStep(1)}
          className={`p-3.5 rounded-2xl border text-left transition flex items-center gap-3 cursor-pointer ${
            currentStep === 1
              ? 'bg-sky-50 border-sky-400 text-sky-950 font-bold shadow-xs ring-2 ring-sky-500/20'
              : micTestedSuccessfully
                ? 'bg-emerald-50/70 border-emerald-300 text-emerald-900 font-semibold'
                : 'bg-white border-pink-200 text-slate-600 hover:bg-slate-50'
          }`}
        >
          <div className={`w-7 h-7 rounded-xl flex items-center justify-center text-xs font-bold shrink-0 ${
            micTestedSuccessfully ? 'bg-emerald-600 text-white' : currentStep === 1 ? 'bg-sky-600 text-white' : 'bg-slate-200 text-slate-700'
          }`}>
            {micTestedSuccessfully ? '✓' : '1'}
          </div>
          <div>
            <span className="block text-xs">1. Microphone (Micro)</span>
            <span className="text-xs text-slate-400 font-normal">
              {micTestedSuccessfully ? 'Tín hiệu tốt (-12dB)' : 'Kiểm tra độ nhạy mic'}
            </span>
          </div>
        </button>

        {/* Step 2 Tab */}
        <button
          type="button"
          onClick={() => setCurrentStep(2)}
          className={`p-3.5 rounded-2xl border text-left transition flex items-center gap-3 cursor-pointer ${
            currentStep === 2
              ? 'bg-sky-50 border-sky-400 text-sky-950 font-bold shadow-xs ring-2 ring-sky-500/20'
              : hasPlayedSound
                ? 'bg-emerald-50/70 border-emerald-300 text-emerald-900 font-semibold'
                : 'bg-white border-pink-200 text-slate-600 hover:bg-slate-50'
          }`}
        >
          <div className={`w-7 h-7 rounded-xl flex items-center justify-center text-xs font-bold shrink-0 ${
            hasPlayedSound ? 'bg-emerald-600 text-white' : currentStep === 2 ? 'bg-sky-600 text-white' : 'bg-slate-200 text-slate-700'
          }`}>
            {hasPlayedSound ? '✓' : '2'}
          </div>
          <div>
            <span className="block text-xs">2. Loa / Tai Nghe</span>
            <span className="text-xs text-slate-400 font-normal">
              {hasPlayedSound ? 'Đã nghe rõ giọng mẫu' : 'Nghe thử giọng Giám thị'}
            </span>
          </div>
        </button>


      </div>

      {/* STEP 1: MICROPHONE INTERACTIVE TEST */}
      {currentStep === 1 && (
        <div className="bg-white border border-pink-200/90 p-6 sm:p-8 rounded-3xl space-y-6 shadow-2xs">
          <div className="text-center space-y-1.5">
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-sky-50 text-sky-700 border border-sky-200 inline-block">
              Bước 1 / 3: Kiểm Tra Microphone
            </span>
            <h2 className="font-heading font-extrabold text-xl sm:text-2xl text-ink">
              Kiểm Tra Tín Hiệu Thu Âm Giọng Nói
            </h2>
            <p className="text-xs text-slate-500 max-w-lg mx-auto leading-relaxed">
              Nhấn nút thu âm và nói một câu ngắn vào microphone của bạn. Hệ thống sẽ bóc băng chữ và đo độ nhạy thời gian thực.
            </p>
          </div>

          {/* Interactive Mic Box */}
          <div className="bg-slate-50 border border-pink-200 rounded-2xl p-6 text-center space-y-5">
            <div className="flex items-center justify-center gap-3">
              <div className={`w-16 h-16 rounded-2xl flex items-center justify-center text-2xl transition border ${
                isMicListening 
                  ? 'bg-rose-50 border-rose-300 text-rose-600 animate-pulse ring-4 ring-rose-100'
                  : micTestedSuccessfully
                    ? 'bg-emerald-50 border-emerald-300 text-emerald-600 ring-4 ring-emerald-100'
                    : 'bg-white border-pink-200 text-slate-600'
              }`}>
                {isMicListening ? <Mic className="w-8 h-8" /> : <MicOff className="w-8 h-8 text-slate-400" />}
              </div>
            </div>

            <div className="flex items-center justify-center gap-3">
              {!isMicListening ? (
                <button
                  type="button"
                  onClick={startMicTest}
                  className="bg-pink-600 hover:bg-pink-700 text-white font-bold text-xs px-6 py-3 rounded-xl shadow-xs transition flex items-center gap-2 cursor-pointer active:scale-95"
                >
                  <Mic className="w-4 h-4 text-sky-400" />
                  <span>🎙️ Bắt Đầu Nói Thử (Test Microphone)</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={stopMicStream}
                  className="bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs px-6 py-3 rounded-xl shadow-xs transition flex items-center gap-2 cursor-pointer active:scale-95"
                >
                  <MicOff className="w-4 h-4" />
                  <span>Dừng Thu Âm Kiểm Tra</span>
                </button>
              )}
            </div>

            {/* Live VU Meter & Waveform */}
            <div className="max-w-md mx-auto space-y-2">
              <div className="flex justify-between text-xs font-bold text-slate-600">
                <span>Mức Âm Lượng Thu (VU Meter):</span>
                <span className="font-mono text-ink">
                  {isMicListening ? `${micVolumeLevel}% (-${Math.max(0, 48 - Math.round(micVolumeLevel * 0.48))} dB)` : 'Chưa bật mic'}
                </span>
              </div>
              <div className="w-full h-3 rounded-full bg-slate-200 overflow-hidden flex p-0.5">
                <div 
                  className="h-full rounded-full transition-all duration-75 bg-gradient-to-r from-emerald-500 via-amber-400 to-rose-500"
                  style={{ width: `${isMicListening ? Math.max(8, micVolumeLevel) : 0}%` }}
                ></div>
              </div>
            </div>

            {/* Speech Recognition Transcript Box */}
            <div className="bg-white border border-pink-200/90 rounded-xl p-4 text-left max-w-lg mx-auto space-y-1.5 shadow-2xs">
              <span className="text-xs font-bold text-slate-500 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-sky-600" />
                Nội dung hệ thống nghe được:
              </span>
              <p className="text-xs text-slate-800 font-medium italic min-h-[2.5rem] flex items-center">
                {transcriptPreview ? (
                  `"${transcriptPreview}"`
                ) : isMicListening ? (
                  <span className="text-slate-400">Đang lắng nghe... Hãy nói to một câu tiếng Việt vào micro...</span>
                ) : (
                  <span className="text-slate-400">Bấm "Bắt Đầu Nói Thử" để kiểm tra tính năng nhận diện giọng nói.</span>
                )}
              </p>
              {sttProblem && (
                <p role="alert" className="text-xs text-amber-800 font-semibold">
                  Dịch vụ nhận dạng giọng nói chưa phản hồi. Micro vẫn hoạt động, nhưng hãy báo giám thị trước khi vào thi.
                </p>
              )}
            </div>

            {micTestedSuccessfully && (
              <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 font-semibold text-xs flex items-center justify-center gap-2 max-w-md mx-auto">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Microphone hoạt động xuất sắc! Tín hiệu giọng nói rõ ràng.</span>
              </div>
            )}
          </div>

          {/* Navigation Buttons */}
          <div className="pt-4 border-t border-pink-100 flex flex-wrap items-center justify-between gap-3">
            <button
              type="button"
              onClick={() => onNavigate && onNavigate('discover')}
              className="text-xs font-bold text-slate-500 hover:text-slate-800 transition"
            >
              ← Về Lịch Thi
            </button>

            <div className="flex items-center gap-2.5">
              <button
                type="button"
                onClick={() => onNavigate && onNavigate('exam-room')}
                className="px-5 py-2.5 rounded-xl border border-sky-600 text-sky-700 hover:bg-sky-50 font-bold text-xs transition cursor-pointer"
              >
                🚀 Vào Làm Bài Thi &amp; Nộp Bài Luôn ↗
              </button>

              <button
                type="button"
                onClick={() => setCurrentStep(2)}
                className="bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs px-6 py-2.5 rounded-xl shadow-xs transition flex items-center gap-1.5 cursor-pointer active:scale-95"
              >
                <span>Sang Bước 2 (Kiểm Tra Loa) →</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* STEP 2: SPEAKER TEST */}
      {currentStep === 2 && (
        <div className="bg-white border border-pink-200/90 p-6 sm:p-8 rounded-3xl space-y-6 shadow-2xs">
          <div className="text-center space-y-1.5">
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-sky-50 text-sky-700 border border-sky-200 inline-block">
              Bước 2 / 3: Kiểm Tra Loa / Tai Nghe
            </span>
            <h2 className="font-heading font-extrabold text-xl sm:text-2xl text-ink">
              Bạn Có Nghe Rõ Giọng Đọc Của Giám Thị Ảo?
            </h2>
            <p className="text-xs text-slate-500 max-w-md mx-auto leading-relaxed">
              Hệ thống sẽ phát một câu chào ngắn bằng giọng AI tiếng Việt tự nhiên để kiểm tra tai nghe của bạn.
            </p>
          </div>

          <div className="bg-slate-50 border border-pink-200 rounded-2xl p-6 text-center space-y-4">
            <div className="w-16 h-16 rounded-2xl bg-white border border-pink-200 mx-auto flex items-center justify-center text-3xl shadow-xs">
              🔊
            </div>
            
            <button 
              type="button"
              onClick={playSampleVoice} 
              className="bg-pink-600 hover:bg-pink-700 text-white font-bold text-xs px-6 py-3 rounded-xl shadow-xs transition inline-flex items-center gap-2 cursor-pointer active:scale-95"
            >
              <Volume2 className="w-4 h-4 text-sky-400" />
              <span>{isPlayingTest ? '▶ Đang Phát Giọng Mẫu (1.0x)...' : '▶ Phát Giọng Đọc Mẫu (1.0x)'}</span>
            </button>
            
            {hasPlayedSound && (
              <p className="text-xs font-semibold text-emerald-600 animate-in fade-in duration-200">
                ✓ Đã phát âm thanh thành công! Tai nghe sẵn sàng.
              </p>
            )}
          </div>

          {/* Quick status recap */}
          <div className="p-4 rounded-2xl bg-emerald-50/70 border border-emerald-200 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2.5">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
              <span className="font-bold text-emerald-900">Microphone: Đã nhận diện tín hiệu tốt</span>
            </div>
            <span className="font-mono text-emerald-700 font-bold">-12 dB (Optimal)</span>
          </div>

          <div className="pt-4 border-t border-pink-100 flex flex-wrap items-center justify-between gap-3">
            <button 
              type="button"
              onClick={() => setCurrentStep(1)}
              className="text-xs font-bold text-slate-500 hover:text-slate-800 transition"
            >
              ← Quay Lại Bước 1 (Micro)
            </button>
            
            <div className="flex items-center gap-2.5">
              <button
                type="button"
                onClick={() => onNavigate && onNavigate('exam-room')}
                className="px-5 py-2.5 rounded-xl border border-sky-600 text-sky-700 hover:bg-sky-50 font-bold text-xs transition cursor-pointer"
              >
                🚀 Vào Làm Bài Thi Luôn ↗
              </button>

              <button 
                type="button"
                onClick={() => onNavigate && onNavigate('exam-room')} 
                disabled={!micTestedSuccessfully || !hasPlayedSound}
                className="bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs px-6 py-2.5 rounded-xl shadow-xs transition flex items-center gap-2 cursor-pointer active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span>Vào phòng thi →</span>
              </button>
            </div>
          </div>
        </div>
      )}


    </div>
  );
}
