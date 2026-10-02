import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/common/Navbar';
import { Sidebar } from './components/common/Sidebar';
import { Toast } from './components/common/Toast';
import { VoiceWaveform } from './components/common/VoiceWaveform';
import { AdminUserPage } from './pages/AdminUserPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ProfileModal } from './components/profile/ProfileModal';
import { RbacMatrixModal } from './components/users/RbacMatrixModal';
import { CreateUserModal } from './components/users/CreateUserModal';
import { Activity, Mic2, Mic, MicOff, Volume2, ShieldCheck, CheckCircle2, Radio, Server, ArrowLeft } from 'lucide-react';

function AppContent() {
  const { currentUser, isLoading } = useAuth();

  // Navigation state: 'admin-users', 'login', 'register', 'telemetry', 'voice-lab'
  const [currentView, setCurrentView] = useState('admin-users');

  // Modals state
  const [profileModalState, setProfileModalState] = useState({ isOpen: false, tab: 'info' });
  const [isMatrixOpen, setIsMatrixOpen] = useState(false);
  const [isQuickAddOpen, setIsQuickAddOpen] = useState(false);

  // Global Toast state
  const [toast, setToast] = useState(null);

  // Interactive Voice Lab State
  const [isMicTesting, setIsMicTesting] = useState(false);
  const [selectedVoiceModel, setSelectedVoiceModel] = useState('vi-VN-SophiaNeural');
  const [micVolume, setMicVolume] = useState(0.7);

  const showToast = (toastData) => {
    setToast(toastData);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center text-white space-y-3 flex-col">
        <div className="w-10 h-10 border-4 border-sky-400 border-t-transparent rounded-full animate-spin"></div>
        <p className="font-heading text-sm font-semibold tracking-wide">Đang khởi tạo AIVES System...</p>
      </div>
    );
  }

  // Standalone Auth Screens (No Admin Navbar/Sidebar)
  if (currentView === 'login') {
    return (
      <>
        <LoginPage onNavigate={(view) => setCurrentView(view)} showToast={showToast} />
        <Toast toast={toast} onClose={() => setToast(null)} />
      </>
    );
  }

  if (currentView === 'register') {
    return (
      <>
        <RegisterPage onNavigate={(view) => setCurrentView(view)} showToast={showToast} />
        <Toast toast={toast} onClose={() => setToast(null)} />
      </>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-canvasBg text-slate-800 antialiased font-sans selection:bg-sky-500 selection:text-white">
      
      {/* GLOBAL TOP NAV */}
      <Navbar
        currentTab={currentView}
        onNavigate={(view) => setCurrentView(view)}
        onOpenAddUser={() => setIsQuickAddOpen(true)}
        onOpenProfile={(tab) => setProfileModalState({ isOpen: true, tab })}
        onOpenMatrix={() => setIsMatrixOpen(true)}
      />

      {/* MAIN CONTAINER WITH SIDEBAR */}
      <div className="flex-1 flex max-w-[1440px] w-full mx-auto">
        
        {/* LEFT PERSISTENT SIDEBAR */}
        <Sidebar
          currentTab={currentView}
          onNavigate={(view) => setCurrentView(view)}
          onOpenMatrix={() => setIsMatrixOpen(true)}
        />

        {/* CENTER MAIN CANVAS */}
        <main className="flex-1 canvas-dot-grid py-8 px-4 sm:px-8 overflow-y-auto">
          {currentView === 'admin-users' && (
            <AdminUserPage
              onOpenMatrix={() => setIsMatrixOpen(true)}
              showToast={showToast}
            />
          )}

          {/* TELEMETRY HUB SCREEN */}
          {currentView === 'telemetry' && (
            <div className="space-y-6 animate-modal-entry">
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-[11px] font-bold text-sky-700 bg-sky-50 px-2.5 py-0.5 rounded-full border border-sky-200">
                      Module 5 • Real-time STOMP WebSocket Monitoring
                    </span>
                  </div>
                  <h1 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
                    Live Telemetry &amp; Phiên Thi Vấn Đáp Trực Tuyến
                  </h1>
                </div>
                <button
                  onClick={() => setCurrentView('admin-users')}
                  className="px-4 py-2 rounded-xl bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center gap-1.5 transition"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Quay Lại RBAC</span>
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-2xs space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500">WebSocket STOMP Broker</span>
                    <Radio className="w-4 h-4 text-emerald-500 animate-pulse" />
                  </div>
                  <p className="font-heading font-extrabold text-2xl text-emerald-600">ONLINE</p>
                  <p className="text-[11px] text-slate-400 font-mono">Channel: /topic/exam-room.*</p>
                </div>

                <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-2xs space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500">Độ Trễ Âm Thanh (Audio Latency)</span>
                    <Server className="w-4 h-4 text-sky-500" />
                  </div>
                  <p className="font-heading font-extrabold text-2xl text-sky-600">42 ms</p>
                  <p className="text-[11px] text-slate-400 font-mono">OPUS Codec • 24kHz Sample Rate</p>
                </div>

                <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-2xs space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500">Cửa Sổ Cứu Vớt Mạng (Recovery)</span>
                    <ShieldCheck className="w-4 h-4 text-purple-500" />
                  </div>
                  <p className="font-heading font-extrabold text-2xl text-purple-600">60 Giây</p>
                  <p className="text-[11px] text-slate-400 font-mono">Auto-Reconnect Guard Activated</p>
                </div>
              </div>

              {/* Real-time telemetry feed table */}
              <div className="bg-white rounded-2xl border border-slate-200/90 shadow-xs overflow-hidden">
                <div className="p-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
                  <h3 className="font-heading font-bold text-xs text-slate-900 uppercase tracking-wider">
                    Nhật Ký Tác Vụ WebSocket Thời Gian Thực (Live STOMP Events)
                  </h3>
                  <span className="flex items-center gap-1.5 text-xs text-emerald-600 font-bold">
                    <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                    Đang truyền phát
                  </span>
                </div>
                <div className="divide-y divide-slate-100 text-xs">
                  <div className="p-3.5 flex items-center justify-between hover:bg-slate-50/70">
                    <div className="flex items-center gap-2.5">
                      <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 font-mono text-[10px] font-bold">200 OK</span>
                      <span className="font-semibold text-slate-800">CONNECT /ws/viva-session</span>
                      <span className="text-slate-400 text-[11px]">User SE160892 (Nguyễn Thị Mai) vào phòng</span>
                    </div>
                    <span className="font-mono text-slate-400 text-[11px]">16:15:20</span>
                  </div>
                  <div className="p-3.5 flex items-center justify-between hover:bg-slate-50/70">
                    <div className="flex items-center gap-2.5">
                      <span className="px-2 py-0.5 rounded bg-sky-50 text-sky-700 font-mono text-[10px] font-bold">STT CHUNK</span>
                      <span className="font-semibold text-slate-800">Neural STT Transcribing:</span>
                      <span className="text-slate-600 italic text-[11px]">"Em áp dụng Transactional Outbox Pattern để..."</span>
                    </div>
                    <span className="font-mono text-slate-400 text-[11px]">16:15:35</span>
                  </div>
                  <div className="p-3.5 flex items-center justify-between hover:bg-slate-50/70">
                    <div className="flex items-center gap-2.5">
                      <span className="px-2 py-0.5 rounded bg-purple-50 text-purple-700 font-mono text-[10px] font-bold">RUBRIC MATCH</span>
                      <span className="font-semibold text-slate-800">Dr. Sophia AI Examiner:</span>
                      <span className="text-slate-600 text-[11px]">Khớp Rubric tiêu chí C2.1 (Kiến trúc phân tầng)</span>
                    </div>
                    <span className="font-mono text-slate-400 text-[11px]">16:15:48</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* VOICE & SPEECH LAB SCREEN */}
          {currentView === 'voice-lab' && (
            <div className="space-y-6 animate-modal-entry">
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-[11px] font-bold text-sky-700 bg-sky-50 px-2.5 py-0.5 rounded-full border border-sky-200">
                      Module 7.2 • Neural Speech &amp; Voice Synthesis Lab
                    </span>
                  </div>
                  <h1 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
                    Voice &amp; Speech Lab: Thử Nghiệm Giọng Đọc AI &amp; Micro
                  </h1>
                </div>
                <button
                  onClick={() => setCurrentView('admin-users')}
                  className="px-4 py-2 rounded-xl bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center gap-1.5 transition"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Quay Lại RBAC</span>
                </button>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Micro Test Card */}
                <div className="bg-white rounded-3xl border border-slate-200/90 p-6 shadow-xs space-y-5">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-2xl bg-sky-50 text-sky-600 flex items-center justify-center">
                        <Mic2 className="w-5 h-5" />
                      </div>
                      <div>
                        <h3 className="font-heading font-extrabold text-base text-slate-900">
                          Kiểm Tra Micro Thời Gian Thực
                        </h3>
                        <p className="text-xs text-slate-500">Đo âm lượng VU Meter &amp; Sóng 60 FPS GPU Waveform</p>
                      </div>
                    </div>
                    <button
                      onClick={() => {
                        setIsMicTesting(!isMicTesting);
                        showToast({
                          type: isMicTesting ? 'info' : 'success',
                          message: isMicTesting ? 'Đã tắt thử nghiệm micro.' : 'Đang thu tín hiệu micro ảo (60 FPS)!'
                        });
                      }}
                      className={`px-4 py-2 rounded-xl font-bold text-xs flex items-center gap-2 transition active:scale-95 ${
                        isMicTesting
                          ? 'bg-rose-600 hover:bg-rose-700 text-white shadow-sm shadow-rose-600/20'
                          : 'bg-sky-600 hover:bg-sky-700 text-white shadow-sm shadow-sky-600/20'
                      }`}
                    >
                      {isMicTesting ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
                      <span>{isMicTesting ? 'Dừng Kiểm Tra' : 'Bật Thử Micro'}</span>
                    </button>
                  </div>

                  {/* GPU Waveform Display */}
                  <div className="p-6 bg-slate-950 rounded-2xl border border-slate-800 space-y-4 text-center">
                    <p className="text-xs text-slate-400 font-mono">
                      {isMicTesting ? 'Đang phát hiện âm lượng micro (Voice Activity Detected)' : 'Micro đang ở trạng thái nghỉ (Muted)'}
                    </p>
                    <div className="flex justify-center">
                      <VoiceWaveform
                        isSpeaking={isMicTesting}
                        volume={isMicTesting ? micVolume : 0.15}
                        barCount={24}
                        height={40}
                        barWidth="w-1.5"
                        barGap="gap-1"
                        colorClass={isMicTesting ? 'bg-sky-400' : 'bg-slate-700'}
                      />
                    </div>
                    <div className="flex items-center justify-center gap-3 text-xs text-slate-400">
                      <span>Độ nhạy thu âm:</span>
                      <input
                        type="range"
                        min="0.2"
                        max="1.0"
                        step="0.05"
                        value={micVolume}
                        onChange={(e) => setMicVolume(parseFloat(e.target.value))}
                        className="accent-sky-500"
                      />
                      <span className="font-mono text-white font-bold">{Math.round(micVolume * 100)}%</span>
                    </div>
                  </div>
                </div>

                {/* Voice Model Selector Card */}
                <div className="bg-white rounded-3xl border border-slate-200/90 p-6 shadow-xs space-y-5">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center">
                      <Volume2 className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="font-heading font-extrabold text-base text-slate-900">
                        Mô Hình Giọng Giám Thị Ảo AI (AI Examiner Voice)
                      </h3>
                      <p className="text-xs text-slate-500">Neural TTS Models tích hợp cho ca thi</p>
                    </div>
                  </div>

                  <div className="space-y-3 text-xs">
                    <label
                      onClick={() => setSelectedVoiceModel('vi-VN-SophiaNeural')}
                      className={`p-3.5 rounded-2xl border flex items-center justify-between cursor-pointer transition ${
                        selectedVoiceModel === 'vi-VN-SophiaNeural'
                          ? 'border-sky-500 bg-sky-50/50 shadow-xs'
                          : 'border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      <div>
                        <p className="font-bold text-slate-900">Dr. Sophia (Giọng Nữ Tiêu Chuẩn Học Thuật)</p>
                        <p className="text-[11px] text-slate-500 font-mono">vi-VN-SophiaNeural • Tông giọng trang trọng, sư phạm</p>
                      </div>
                      {selectedVoiceModel === 'vi-VN-SophiaNeural' && (
                        <CheckCircle2 className="w-5 h-5 text-sky-600" />
                      )}
                    </label>

                    <label
                      onClick={() => setSelectedVoiceModel('vi-VN-MarcusNeural')}
                      className={`p-3.5 rounded-2xl border flex items-center justify-between cursor-pointer transition ${
                        selectedVoiceModel === 'vi-VN-MarcusNeural'
                          ? 'border-sky-500 bg-sky-50/50 shadow-xs'
                          : 'border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      <div>
                        <p className="font-bold text-slate-900">Prof. Marcus (Giọng Nam Trầm Hỏi Xoáy)</p>
                        <p className="text-[11px] text-slate-500 font-mono">vi-VN-MarcusNeural • Phong thái hội đồng phản biện</p>
                      </div>
                      {selectedVoiceModel === 'vi-VN-MarcusNeural' && (
                        <CheckCircle2 className="w-5 h-5 text-sky-600" />
                      )}
                    </label>

                    <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 text-slate-600 leading-relaxed text-[11px]">
                      Giọng đọc được dùng khi AI Giám thị đọc câu hỏi vấn đáp và thực hiện các lượt hỏi xoáy (Adaptive Probing) đối chiếu theo khung chấm Rubric môn SWD392.
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </main>

      </div>

      {/* GLOBAL MODALS */}
      <ProfileModal
        isOpen={profileModalState.isOpen}
        initialTab={profileModalState.tab}
        onClose={() => setProfileModalState({ isOpen: false, tab: 'info' })}
        showToast={showToast}
      />

      <RbacMatrixModal
        isOpen={isMatrixOpen}
        onClose={() => setIsMatrixOpen(false)}
      />

      <CreateUserModal
        isOpen={isQuickAddOpen}
        onClose={() => setIsQuickAddOpen(false)}
        onSuccess={() => {
          showToast({ type: 'success', message: 'Tạo tài khoản thành công!' });
        }}
        showToast={showToast}
      />

      {/* TOAST NOTIFICATION CONTAINER */}
      <Toast toast={toast} onClose={() => setToast(null)} />

    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
