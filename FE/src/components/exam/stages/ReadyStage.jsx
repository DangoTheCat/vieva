import React from 'react';
import { ArrowRight, Check, CheckCircle2, Mic, MicOff, Sparkles, Volume2 } from 'lucide-react';

/** Oral exam room — screen shown for this stage. Extracted from OralExamRoomPage (plan item 2.12). */
export function ReadyStage({ handleStartExam, handleTestSpeaker, isAiSpeaking, preflightMicOk, preflightMicVolume, preflightSpeakerOk, preflightTestingMic, setPreflightMicOk, startPreflightMicTest, stopPreflightMic, studentCode, studentName }) {
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
              <span className="text-xs font-bold uppercase tracking-wider text-green-700 block">
                Khảo Thí Vấn Đáp AI • Trạng Thái: READY
              </span>
              <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
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
                  <span className="flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full border border-emerald-300">
                    <CheckCircle2 className="w-3.5 h-3.5" /> Chuẩn ✓
                  </span>
                ) : (
                  <span className="text-xs font-bold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full border border-amber-300">
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
              <div className="flex justify-between text-xs font-mono text-green-700">
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
                  <span className="flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full border border-emerald-300">
                    <CheckCircle2 className="w-3.5 h-3.5" /> Chuẩn ✓
                  </span>
                ) : (
                  <span className="text-xs font-bold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full border border-amber-300">
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
