import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/userApi';
import { 
  Radio, 
  Mic, 
  Video, 
  CheckCircle2, 
  X, 
  AlertTriangle, 
  Clock, 
  Sparkles,
  Volume2,
  Check
} from 'lucide-react';
import { VoiceWaveform } from '../components/common/VoiceWaveform';

export function OralExamRoomPage({ onNavigate, showToast }) {
  const { currentUser } = useAuth();
  const [profile, setProfile] = useState(currentUser || null);
  const [secondsLeft, setSecondsLeft] = useState(522); // 08:42
  const [isSpeaking, setIsSpeaking] = useState(true);
  const [isFinishModalOpen, setIsFinishModalOpen] = useState(false);

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
    return () => clearInterval(interval);
  }, []);

  const studentName = profile?.fullName || currentUser?.fullName || 'Nguyễn Thị Mai';
  const studentCode = profile?.userCode || currentUser?.userCode || 'SE160892';

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  const handleFinishExam = () => {
    setIsFinishModalOpen(false);
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Hoàn Tất Ca Thi',
        message: 'Đã nộp bài thi thành công! Kết quả đã được mã hóa SHA-256 và chuyển giao sang Module 4.'
      });
    }
    if (onNavigate) {
      onNavigate('submission-success');
    }
  };

  return (
    <div className="space-y-6 animate-modal-entry">
      {/* Header Banner with Status Pills */}
      <div className="bg-white border border-slate-200/90 p-5 sm:p-6 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-2xs">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-full bg-blue-50 border border-blue-200 flex items-center justify-center text-xl shrink-0">
            🎙️
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="font-heading font-extrabold text-base sm:text-lg text-slate-900">Phòng Vấn Đáp Trực Tuyến</h2>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300 flex items-center gap-1">
                <Radio className="w-3 h-3 text-emerald-600 animate-pulse" />
                STOMP Realtime
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Thí sinh: <strong>{studentName} ({studentCode})</strong> • Giám thị AI: <strong>Dr. Sophia</strong>
            </p>
          </div>
        </div>

        {/* Timer & Finish Button */}
        <div className="flex items-center gap-4">
          <div className="text-right font-mono">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Thời Gian Lượt Thi</span>
            <p className="text-xl font-bold text-slate-900">{formatTime(secondsLeft)}</p>
          </div>
          <button 
            onClick={() => setIsFinishModalOpen(true)} 
            className="bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs px-5 py-2.5 rounded-full shadow-xs active:scale-95 transition cursor-pointer"
          >
            Nộp Bài Thi
          </button>
        </div>
      </div>

      {/* 2 Columns: AI Examiner vs Candidate */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Column 1: AI Examiner & Voice Waves (7 cols) */}
        <div className="lg:col-span-7 space-y-4">
          
          <div className="bg-white border border-slate-200/90 p-6 rounded-2xl text-center space-y-4 shadow-2xs">
            <div className="w-24 h-24 rounded-full bg-slate-100 mx-auto overflow-hidden ring-4 ring-blue-100 shadow-md relative">
              <img 
                src="https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&auto=format&fit=crop&q=80" 
                alt="AI Examiner" 
                className="w-full h-full object-cover" 
              />
            </div>
            
            <div>
              <h3 className="font-heading font-bold text-base text-slate-900">Giám Thị Ảo: Dr. Sophia</h3>
              <p className="text-xs text-blue-600 font-semibold mt-0.5 flex items-center justify-center gap-1">
                <span className="w-2 h-2 rounded-full bg-blue-600 animate-ping"></span>
                <span>Đang phát âm câu hỏi: The Second Why (DIP &amp; Onion)</span>
              </p>
            </div>

            {/* Voice Waveform Indicator */}
            <div className="flex items-center justify-center gap-2 py-3 bg-slate-900/90 rounded-2xl border border-slate-800 text-white max-w-sm mx-auto">
              <Volume2 className="w-4 h-4 text-sky-400" />
              <VoiceWaveform isSpeaking={isSpeaking} volume={0.65} barCount={8} height={20} colorClass="bg-sky-400" />
              <span className="text-[11px] font-mono text-emerald-400 font-bold ml-1">Live Audio</span>
            </div>
          </div>

          {/* Live Transcript Card */}
          <div className="bg-white border border-slate-200/90 p-5 rounded-2xl space-y-2 shadow-2xs">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse"></span>
                Bóc Băng Trực Tiếp (Live STT Transcript)
              </span>
              <span className="text-[11px] font-mono text-emerald-600 font-semibold">Độ tin cậy: 98.4%</span>
            </div>
            <div className="bg-slate-50 border border-slate-200/80 rounded-xl p-3.5 text-xs text-slate-700 leading-relaxed max-h-32 overflow-y-auto font-sans">
              "Thưa cô, trong kiến trúc Onion, tầng Domain Core chỉ chứa các Business Rules và Entities độc lập. Chúng ta áp dụng Dependency Inversion Principle để tầng ngoài (Infrastructure) phụ thuộc vào Interface của tầng trong chứ tầng trong không được phụ thuộc trực tiếp vào ORM..."
            </div>
          </div>

        </div>

        {/* Column 2: Candidate Video & Rubric Progress (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          
          {/* Student Cam Feed & VU Meter */}
          <div className="bg-white border border-slate-200/90 p-4 rounded-2xl space-y-3 shadow-2xs">
            <div className="aspect-video bg-slate-900 rounded-xl overflow-hidden relative">
              <img 
                src="https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&auto=format&fit=crop&q=80" 
                alt="Thí Sinh" 
                className="w-full h-full object-cover" 
              />
              <span className="absolute top-2 left-2 bg-slate-900/80 backdrop-blur-xs text-white text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1">
                <Video className="w-3 h-3 text-emerald-400" />
                CAM 30FPS • ĐÚNG GÓC NHÌN
              </span>
            </div>

            {/* VU Meter */}
            <div>
              <div className="flex justify-between text-[11px] font-bold text-slate-700 mb-1">
                <span>Mức Thu Microphone</span>
                <span className="text-emerald-600 font-mono">-12 dB (Tốt)</span>
              </div>
              <div className="w-full h-2 rounded-full bg-slate-100 overflow-hidden flex">
                <div className="h-full bg-emerald-500 w-3/4"></div>
                <div className="h-full bg-amber-400 w-1/6"></div>
                <div className="h-full bg-rose-500 w-1/12"></div>
              </div>
            </div>
          </div>

          {/* Rubric Evaluation Progress */}
          <div className="bg-white border border-slate-200/90 p-4 rounded-2xl space-y-2.5 shadow-2xs">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">Tiêu Chí Đang Đánh Giá</span>
            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-emerald-50 text-emerald-900 font-medium">
                <span>1. Bản chất Onion Layer</span>
                <span className="font-bold text-emerald-700">Đạt (10/10)</span>
              </div>
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-blue-50 text-blue-900 font-medium">
                <span>2. Dependency Inversion (DIP)</span>
                <span className="font-bold text-blue-600 animate-pulse">Đang phân tích...</span>
              </div>
            </div>
          </div>

        </div>

      </div>

      {/* FINISH EXAM CONFIRMATION MODAL */}
      {isFinishModalOpen && (
        <div className="fixed inset-0 z-50 bg-neutral-950/75 flex items-center justify-center p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800 text-center">
            <div className="w-12 h-12 rounded-full bg-blue-100 text-blue-600 mx-auto flex items-center justify-center text-xl font-bold">
              📝
            </div>
            <h3 className="font-heading font-extrabold text-lg text-slate-900">Xác Nhận Nộp Bài Thi Vấn Đáp?</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Bài thi sẽ được đóng lại, mã hóa chữ ký SHA-256 và chuyển giao kết quả trực tiếp sang hội đồng chấm điểm FAP.
            </p>
            
            <div className="pt-3 flex items-center justify-center gap-3">
              <button 
                onClick={() => setIsFinishModalOpen(false)} 
                className="px-4 py-2 rounded-full border border-slate-300 hover:bg-slate-50 text-xs font-semibold text-slate-700"
              >
                Hủy Bỏ
              </button>
              <button 
                onClick={handleFinishExam} 
                className="px-6 py-2 rounded-full bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-xs active:scale-95 transition"
              >
                Xác Nhận Nộp Bài
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
