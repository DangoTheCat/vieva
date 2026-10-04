import React, { useState, useEffect } from 'react';
import { 
  Volume2, 
  Mic, 
  Sliders, 
  Play, 
  Save, 
  Plus, 
  Activity, 
  Zap, 
  Check, 
  X, 
  Radio, 
  ShieldCheck, 
  Sparkles,
  Gauge
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

  // New Voice Form State
  const [newVoiceName, setNewVoiceName] = useState('');
  const [newVoiceLang, setNewVoiceLang] = useState('vi-VN');
  const [newVoiceGender, setNewVoiceGender] = useState('male');
  const [newVoicePrompt, setNewVoicePrompt] = useState('');

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
          title: 'Acoustic Synthesizer',
          message: `Đang phát âm thử nghiệm giọng ${activeProfile === 'minh' ? 'Dr. Minh' : activeProfile === 'sarah' ? 'Prof. Sarah' : 'Dr. Anh'}...`
        });
      }
    } else if (showToast) {
      showToast({
        type: 'error',
        title: 'Trình Duyệt Không Hỗ Trợ',
        message: 'Trình duyệt không hỗ trợ Web Speech Synthesis API.'
      });
    }
  };

  const handleSaveConfig = () => {
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Đã Lưu Cấu Hình',
        message: `Đã cập nhật tham số âm học (Rate: ${speakingRate}x, Pitch: ${pitchShift}, Pause: ${pauseDuration}ms) cho phòng thi vấn đáp!`
      });
    }
  };

  const handleCreateVoiceSubmit = (e) => {
    e.preventDefault();
    setIsNewVoiceModalOpen(false);
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Tạo Hồ Sơ Giọng Nói',
        message: `Đã khởi tạo thành công giọng nói ${newVoiceName} (${newVoiceLang})!`
      });
    }
    setNewVoiceName('');
    setNewVoicePrompt('');
  };

  const handleRunStressTest = () => {
    setIsStressTestModalOpen(false);
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Stress Test Hoàn Tất',
        message: 'Đã hoàn tất kiểm thử tải 50 phòng thi đồng thời! Latency: 154ms, Packet Loss: 0%.'
      });
    }
  };

  return (
    <div className="space-y-6 animate-modal-entry">
      {/* HEADER & TELEMETRY ROW */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-bold text-sky-700 bg-sky-50 px-2.5 py-0.5 rounded-full border border-sky-200">
              Module 3 • Viva Core Realtime Engine
            </span>
          </div>
          <h1 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
            Hiệu Chuẩn Giọng Nói Giám Thị Ảo (TTS &amp; STT)
          </h1>
          <p className="text-xs text-slate-500 mt-1 max-w-2xl leading-relaxed">
            Tinh chỉnh tham số mô hình chuyển văn bản thành giọng nói (Neural TTS), tốc độ phát âm thanh sư phạm và độ trễ phản hồi đàm thoại hai chiều.
          </p>
        </div>

        {/* 3 Quick Metrics */}
        <div className="flex items-center gap-3">
          <div className="px-3.5 py-2 rounded-xl bg-white border border-slate-200/90 shadow-2xs text-center">
            <p className="text-[10px] text-slate-400 font-bold uppercase">STT Latency</p>
            <p className="font-heading font-extrabold text-base text-emerald-600">142 ms</p>
          </div>
          <div className="px-3.5 py-2 rounded-xl bg-white border border-slate-200/90 shadow-2xs text-center">
            <p className="text-[10px] text-slate-400 font-bold uppercase">TTS First-Byte</p>
            <p className="font-heading font-extrabold text-base text-sky-600">185 ms</p>
          </div>
          <div className="px-3.5 py-2 rounded-xl bg-white border border-slate-200/90 shadow-2xs text-center">
            <p className="text-[10px] text-slate-400 font-bold uppercase">Audio Quality</p>
            <p className="font-heading font-extrabold text-base text-slate-900">48kHz / 24bit</p>
          </div>
        </div>
      </div>

      {/* TOP TOOLBAR ACTIONS */}
      <div className="flex items-center justify-between bg-white border border-slate-200/90 p-3.5 rounded-2xl shadow-2xs">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
          <span className="text-xs font-bold text-slate-800">Trạng Thái Bộ Tổng Hợp: Hoạt ĐộngTốt</span>
          <span className="text-xs text-slate-400 hidden sm:inline">• Latency 24ms • WebSocket OPUS Realtime</span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsStressTestModalOpen(true)}
            className="px-3.5 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center gap-1.5 transition active:scale-95"
          >
            <Gauge className="w-4 h-4 text-purple-600" />
            <span>Kiểm Thử Tải Âm Thanh</span>
          </button>
          <button
            onClick={() => setIsNewVoiceModalOpen(true)}
            className="px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-sky-600 to-sky-500 hover:from-sky-500 hover:to-sky-400 text-white text-xs font-bold shadow-md shadow-sky-600/20 flex items-center gap-1.5 transition active:scale-95"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo Giọng Nói Mới</span>
          </button>
        </div>
      </div>

      {/* MAIN SPLIT WORKSPACE: VOICE PROFILES + PARAMETERS TUNER */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* LEFT: VOICE PROFILES SELECTOR (6 COLS) */}
        <div className="lg:col-span-6 bg-white border border-slate-200/90 rounded-2xl p-5 shadow-2xs space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div className="flex items-center gap-2">
              <Volume2 className="w-5 h-5 text-sky-600" />
              <h3 className="font-heading font-bold text-slate-900 text-sm">Hồ Sơ Giọng Nói Giám Thị Ảo (Profiles)</h3>
            </div>
            <span className="text-xs font-bold text-sky-700 bg-sky-50 px-2.5 py-0.5 rounded-full border border-sky-200">
              3 Active Profiles
            </span>
          </div>

          {/* Voice Profile 1: Dr. Minh (Active Default) */}
          <div 
            onClick={() => setActiveProfile('minh')}
            className={`p-4 rounded-xl border-2 cursor-pointer transition-all space-y-2 ${
              activeProfile === 'minh' ? 'border-sky-500 bg-sky-50/40' : 'border-slate-200 bg-white hover:border-slate-300'
            }`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-sky-600 text-white font-bold flex items-center justify-center text-sm shadow-md">
                  DM
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="font-bold text-slate-900 text-sm">Dr. Minh (Giám Thị Chính)</h4>
                    <span className="text-[10px] font-bold bg-sky-500 text-white px-2 py-0.5 rounded-full">DEFAULT</span>
                  </div>
                  <p class="text-[11px] text-slate-500">Giọng Nam miền Bắc • Phong thái điềm đạm, chuẩn sư phạm</p>
                </div>
              </div>
              <button 
                onClick={(e) => { e.stopPropagation(); speakText('Chào em, Thầy là Dr. Minh, Giám thị ảo phòng thi số 4. Em hãy sẵn sàng cho câu hỏi đầu tiên.'); }}
                className="w-8 h-8 rounded-full bg-sky-100 hover:bg-sky-200 text-sky-700 flex items-center justify-center transition active:scale-95" 
                title="Phát thử"
              >
                <Play className="w-4 h-4 fill-current ml-0.5" />
              </button>
            </div>
            <div className="flex items-center gap-3 pt-2 text-[11px] text-slate-600 font-mono">
              <span>Mô hình: Neural-vi-VN-Standard</span>
              <span>•</span>
              <span>Sampling: 24kHz</span>
            </div>
          </div>

          {/* Voice Profile 2: Prof. Sarah (English Oral) */}
          <div 
            onClick={() => setActiveProfile('sarah')}
            className={`p-4 rounded-xl border cursor-pointer transition-all space-y-2 ${
              activeProfile === 'sarah' ? 'border-purple-500 bg-purple-50/40 border-2' : 'border-slate-200 bg-white hover:border-slate-300'
            }`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-purple-100 text-purple-700 font-bold flex items-center justify-center text-sm">
                  PS
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="font-bold text-slate-900 text-sm">Prof. Sarah (English Oral Exams)</h4>
                    <span className="text-[10px] font-bold bg-purple-100 text-purple-800 px-2 py-0.5 rounded-full">EN-US</span>
                  </div>
                  <p className="text-[11px] text-slate-500">Giọng Nữ Bắc Mỹ • Chuẩn khảo thí Quốc tế</p>
                </div>
              </div>
              <button 
                onClick={(e) => { e.stopPropagation(); speakText('Welcome candidate to the software architecture oral examination.'); }}
                className="w-8 h-8 rounded-full bg-purple-100 hover:bg-purple-200 text-purple-700 flex items-center justify-center transition active:scale-95" 
                title="Phát thử"
              >
                <Play className="w-4 h-4 fill-current ml-0.5" />
              </button>
            </div>
            <div className="flex items-center gap-3 pt-2 text-[11px] text-slate-500 font-mono">
              <span>Mô hình: Neural2-en-US-F</span>
              <span>•</span>
              <span>Sampling: 48kHz</span>
            </div>
          </div>

          {/* Voice Profile 3: Dr. Anh (Female Academic) */}
          <div 
            onClick={() => setActiveProfile('anh')}
            className={`p-4 rounded-xl border cursor-pointer transition-all space-y-2 ${
              activeProfile === 'anh' ? 'border-emerald-500 bg-emerald-50/40 border-2' : 'border-slate-200 bg-white hover:border-slate-300'
            }`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 font-bold flex items-center justify-center text-sm">
                  DA
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="font-bold text-slate-900 text-sm">Dr. Anh (Giảng Viên Nữ)</h4>
                    <span className="text-[10px] font-bold bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full">VI-VN</span>
                  </div>
                  <p className="text-[11px] text-slate-500">Giọng Nữ miền Nam • Âm vực nhẹ nhàng, rõ chữ</p>
                </div>
              </div>
              <button 
                onClick={(e) => { e.stopPropagation(); speakText('Chào bạn, mời bạn trả lời câu hỏi về kiến trúc phần mềm.'); }}
                className="w-8 h-8 rounded-full bg-emerald-100 hover:bg-emerald-200 text-emerald-700 flex items-center justify-center transition active:scale-95" 
                title="Phát thử"
              >
                <Play className="w-4 h-4 fill-current ml-0.5" />
              </button>
            </div>
            <div className="flex items-center gap-3 pt-2 text-[11px] text-slate-500 font-mono">
              <span>Mô hình: Neural-vi-VN-Female</span>
              <span>•</span>
              <span>Sampling: 24kHz</span>
            </div>
          </div>
        </div>

        {/* RIGHT: LIVE TUNING SLIDERS & SPEECH TEST PLAYGROUND (6 COLS) */}
        <div className="lg:col-span-6 bg-white border border-slate-200/90 rounded-2xl p-5 shadow-2xs space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <h3 className="font-heading font-bold text-slate-900 text-sm">Bộ Điều Khiển Tham Số Âm Học (Acoustic Tuner)</h3>
            <span className="text-[11px] text-sky-700 font-bold bg-sky-50 px-2 py-0.5 rounded">
              Profile: {activeProfile === 'minh' ? 'Dr. Minh' : activeProfile === 'sarah' ? 'Prof. Sarah' : 'Dr. Anh'}
            </span>
          </div>

          {/* Parameter Sliders */}
          <div className="space-y-4 text-xs">
            {/* Speed / Speaking Rate */}
            <div className="space-y-1.5">
              <div className="flex justify-between font-bold">
                <span className="text-slate-800">Tốc Độ Đọc (Speaking Rate):</span>
                <span className="text-sky-700 font-mono">{speakingRate.toFixed(2)}x {speakingRate === 1.0 ? '(Chuẩn)' : ''}</span>
              </div>
              <input 
                type="range" 
                min="0.75" 
                max="1.5" 
                step="0.05" 
                value={speakingRate} 
                onChange={(e) => setSpeakingRate(parseFloat(e.target.value))} 
                className="w-full accent-sky-600 cursor-pointer"
              />
              <div className="flex justify-between text-[10px] text-slate-400 font-mono">
                <span>0.75x (Chậm)</span>
                <span>1.00x</span>
                <span>1.50x (Nhanh)</span>
              </div>
            </div>

            {/* Pitch */}
            <div className="space-y-1.5">
              <div className="flex justify-between font-bold">
                <span className="text-slate-800">Cao Độ Giọng (Pitch Shift):</span>
                <span className="text-sky-700 font-mono">
                  {pitchShift >= 0 ? `+${pitchShift}` : pitchShift} semitones
                </span>
              </div>
              <input 
                type="range" 
                min="-4" 
                max="4" 
                step="0.5" 
                value={pitchShift} 
                onChange={(e) => setPitchShift(parseFloat(e.target.value))} 
                className="w-full accent-sky-600 cursor-pointer"
              />
            </div>

            {/* Pause Duration between sentences */}
            <div className="space-y-1.5">
              <div className="flex justify-between font-bold">
                <span className="text-slate-800">Khoảng Nghỉ Giữa Các Câu (Sentence Pause):</span>
                <span className="text-sky-700 font-mono">{pauseDuration} ms</span>
              </div>
              <input 
                type="range" 
                min="200" 
                max="1000" 
                step="50" 
                value={pauseDuration} 
                onChange={(e) => setPauseDuration(parseInt(e.target.value, 10))} 
                className="w-full accent-sky-600 cursor-pointer"
              />
            </div>
          </div>

          {/* Interactive Test Playground Text Area */}
          <div className="space-y-2 text-xs pt-2 border-t border-slate-100">
            <label className="block font-bold text-slate-700">Nội Dung Thử Nghiệm Phát Âm:</label>
            <textarea 
              rows={3} 
              value={testText}
              onChange={(e) => setTestText(e.target.value)}
              className="w-full p-3 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none leading-relaxed text-slate-800"
            />
            
            <div className="flex items-center justify-between pt-1">
              <span className="text-[11px] text-slate-400">Web Speech API Browser Sound Engine</span>
              <button 
                onClick={() => speakText(testText)} 
                disabled={isPlaying}
                className="px-4 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold flex items-center gap-1.5 shadow-md shadow-sky-600/20 active:scale-95 transition disabled:opacity-50"
              >
                <Play className={`w-4 h-4 fill-current ${isPlaying ? 'animate-pulse' : ''}`} />
                <span>{isPlaying ? 'Đang Phát...' : 'Phát Âm Thử Ngay'}</span>
              </button>
            </div>
          </div>

          {/* Save Button */}
          <div className="pt-4 border-t border-slate-100 flex justify-end">
            <button 
              onClick={handleSaveConfig} 
              className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-md shadow-emerald-600/20 flex items-center gap-1.5 transition active:scale-95"
            >
              <Save className="w-4 h-4" />
              <span>Lưu Cấu Hình Mới</span>
            </button>
          </div>
        </div>

      </div>

      {/* ================= MODAL 1: NEW VOICE PROFILE ================= */}
      {isNewVoiceModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry">
            <div className="p-5 border-b border-slate-100 bg-slate-950 text-white flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-sky-500/20 text-sky-300 flex items-center justify-center">
                  <Volume2 className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-heading font-bold text-sm">Thêm Hồ Sơ Giọng Nói Giám Thị Mới</h3>
                  <p className="text-[11px] text-slate-400">Hỗ trợ Neural TTS hoặc mẫu âm thanh mẫu</p>
                </div>
              </div>
              <button onClick={() => setIsNewVoiceModalOpen(false)} className="text-slate-400 hover:text-white p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateVoiceSubmit} className="p-5 space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Tên Giám Thị Ảo:</label>
                <input 
                  type="text" 
                  required 
                  value={newVoiceName}
                  onChange={(e) => setNewVoiceName(e.target.value)}
                  placeholder="Ví dụ: Dr. Hoang, Prof. David..." 
                  className="w-full p-2.5 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Ngôn Ngữ:</label>
                  <select 
                    value={newVoiceLang}
                    onChange={(e) => setNewVoiceLang(e.target.value)}
                    className="w-full p-2 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                  >
                    <option value="vi-VN">Tiếng Việt (vi-VN)</option>
                    <option value="en-US">English (en-US)</option>
                    <option value="en-GB">English (en-GB)</option>
                  </select>
                </div>
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Giới Tính / Tone:</label>
                  <select 
                    value={newVoiceGender}
                    onChange={(e) => setNewVoiceGender(e.target.value)}
                    className="w-full p-2 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                  >
                    <option value="male">Nam (Trầm, Thư thái)</option>
                    <option value="female">Nữ (Trong trẻo, Chuẩn mực)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Prompt Chỉ Dẫn Phong Thái Sư Phạm:</label>
                <textarea 
                  rows={2} 
                  value={newVoicePrompt}
                  onChange={(e) => setNewVoicePrompt(e.target.value)}
                  placeholder="Ví dụ: Đọc chậm rãi, nhấn giọng ở các thuật ngữ kỹ thuật..." 
                  className="w-full p-2.5 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                />
              </div>

              <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-3 -mx-5 -mb-5 mt-5">
                <button 
                  type="button" 
                  onClick={() => setIsNewVoiceModalOpen(false)} 
                  className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold"
                >
                  Hủy
                </button>
                <button 
                  type="submit" 
                  className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold shadow-sm"
                >
                  Tạo Giọng Nói
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ================= MODAL 2: STRESS TEST ================= */}
      {isStressTestModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry">
            <div className="p-5 border-b border-slate-100 bg-slate-950 text-white flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-purple-500/20 text-purple-300 flex items-center justify-center">
                  <Gauge className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-heading font-bold text-sm">Kiểm Thử Tải Âm Thanh Đa Lượt (Stress Test)</h3>
                  <p className="text-[11px] text-slate-400">Giả lập 50 phòng thi đồng thời</p>
                </div>
              </div>
              <button onClick={() => setIsStressTestModalOpen(false)} className="text-slate-400 hover:text-white p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 space-y-4 text-xs">
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="flex justify-between">
                  <span className="text-slate-600">Số phòng thi ảo giả lập:</span>
                  <span className="font-bold text-slate-900 font-mono">50 Rooms</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Audio Bitrate:</span>
                  <span class="font-bold text-slate-900 font-mono">64 kbps Opus</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Buffer Health:</span>
                  <span className="font-bold text-emerald-600 font-mono">99.8% No Packet Loss</span>
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  onClick={() => setIsStressTestModalOpen(false)}
                  className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold"
                >
                  Đóng
                </button>
                <button 
                  onClick={handleRunStressTest} 
                  className="px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold shadow-md shadow-purple-600/20 active:scale-95 transition"
                >
                  Chạy Kiểm Thử Ngay (Run Test)
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
