import React, { useState } from 'react';
import { Volume2 } from 'lucide-react';

export function CalibrationPage({ onNavigate, showToast }) {
  const [isPlayingTest, setIsPlayingTest] = useState(false);
  const [hasPlayedSound, setHasPlayedSound] = useState(false);

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
          message: 'Đang phát đoạn âm thanh thử nghiệm giọng Giám thị ảo...'
        });
      }
    } else if (showToast) {
      showToast({
        type: 'error',
        title: 'Lỗi Âm Thanh',
        message: 'Trình duyệt không hỗ trợ Web Speech Synthesis API.'
      });
    }
  };

  return (
    <div className="max-w-3xl w-full mx-auto space-y-6 animate-modal-entry py-4 text-slate-800">
      
      {/* Stepper Pill Tabs */}
      <div className="grid grid-cols-3 gap-3 text-xs">
        <div className="p-3 rounded-2xl bg-emerald-50 border border-emerald-300 text-emerald-900 flex items-center gap-2.5 font-bold shadow-xs">
          <span className="w-6 h-6 rounded-full bg-emerald-500 text-white flex items-center justify-center text-[11px]">✓</span>
          <span>1. Microphone (-12dB)</span>
        </div>
        <div className="p-3 rounded-2xl bg-sky-50 border border-sky-300 text-sky-900 flex items-center gap-2.5 font-bold shadow-xs">
          <span className="w-6 h-6 rounded-full bg-sky-600 text-white flex items-center justify-center text-[11px]">2</span>
          <span>2. Loa / Tai Nghe</span>
        </div>
        <div className="p-3 rounded-2xl bg-slate-100 border border-slate-200 text-slate-400 flex items-center gap-2.5 font-medium">
          <span className="w-6 h-6 rounded-full bg-slate-300 text-slate-600 flex items-center justify-center text-[11px]">3</span>
          <span>3. Camera AI</span>
        </div>
      </div>

      {/* Main Testing Card */}
      <div className="bg-white border border-slate-200/90 p-8 rounded-3xl space-y-6 shadow-2xs">
        
        <div className="text-center space-y-1.5">
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-sky-50 text-sky-700 border border-sky-200">
            Bước 2: Kiểm Tra Âm Thanh Đầu Ra (Loa)
          </span>
          <h2 className="font-heading font-extrabold text-2xl text-slate-900">
            Bạn Có Nghe Rõ Giọng Đọc Của Giám Thị?
          </h2>
          <p className="text-xs text-slate-500 max-w-md mx-auto">
            Hệ thống sẽ phát một câu chào ngắn bằng giọng AI tiếng Việt tự nhiên để kiểm tra tai nghe của bạn.
          </p>
        </div>

        {/* Speaker Testing Box */}
        <div className="bg-slate-50 border border-slate-200 rounded-2xl p-6 text-center space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-white border border-slate-200 mx-auto flex items-center justify-center text-3xl shadow-xs">
            🔊
          </div>
          
          <button 
            type="button"
            onClick={playSampleVoice} 
            className="bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs px-6 py-3 rounded-full shadow-xs transition inline-flex items-center gap-2 cursor-pointer active:scale-95"
          >
            <Volume2 className="w-4 h-4" />
            <span>{isPlayingTest ? '▶ Đang Phát Giọng Mẫu (1.0x)...' : '▶ Phát Giọng Đọc Mẫu (1.0x)'}</span>
          </button>
          
          {hasPlayedSound && (
            <p className="text-xs font-semibold text-emerald-600 animate-in fade-in duration-200">
              ✓ Đã phát âm thanh thành công!
            </p>
          )}
        </div>

        {/* Microphone Status Bar (Summary) */}
        <div className="p-4 rounded-2xl bg-emerald-50/70 border border-emerald-200 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2.5">
            <span className="relative flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
            </span>
            <span className="font-bold text-emerald-900">Microphone: Đã nhận diện tín hiệu tốt</span>
          </div>
          <span className="font-mono text-emerald-700 font-bold">-12 dB (Optimal)</span>
        </div>

        {/* Footer Navigation */}
        <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
          <button 
            type="button"
            onClick={() => onNavigate && onNavigate('discover')}
            className="text-xs font-bold text-slate-400 hover:text-slate-700 transition cursor-pointer"
          >
            ← Quay Lại
          </button>
          
          <button 
            type="button"
            onClick={() => onNavigate && onNavigate('exam-room')} 
            className="bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs px-6 py-3 rounded-xl shadow-xs transition flex items-center gap-2 cursor-pointer active:scale-95"
          >
            <span>Tôi Nghe Rõ, Vào Phòng Thi ↗</span>
          </button>
        </div>

      </div>

    </div>
  );
}
