import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { adminUserApi } from '../api/adminUserApi';
import { getErrorMessage } from '../utils/errorCodes';
import {
  Activity,
  Server,
  ShieldCheck,
  Radio,
  FileText,
  Clock,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  X,
  Search,
  Users,
  RefreshCw,
  ExternalLink,
  Loader2,
  Lock,
  Cpu,
  Layers
} from 'lucide-react';

export function AdminOperationsPage({ onNavigate, showToast }) {
  const { currentUser } = useAuth();
  const [isAuditModalOpen, setIsAuditModalOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState('live');

  // Real-time active exam rooms list (Matching Image 1)
  const [activeRooms, setActiveRooms] = useState([
    {
      roomId: 'Phòng #04',
      subjectCode: 'SWD392 • Kiến Trúc PM',
      candidateName: 'Nguyễn Văn An',
      candidateCode: 'SE160892',
      remainingTime: '08:42',
      networkStatus: 'GOOD',
      pingMs: 24,
      statusTag: 'Tốt (24ms)'
    },
    {
      roomId: 'Phòng #02',
      subjectCode: 'PRN231 • .NET Enterprise',
      candidateName: 'Trần Minh Hoàng',
      candidateCode: 'SE150112',
      remainingTime: '02:15',
      networkStatus: 'GOOD',
      pingMs: 18,
      statusTag: 'Tốt (18ms)'
    },
    {
      roomId: 'Phòng #09',
      subjectCode: 'SWD392 • Kiến Trúc PM',
      candidateName: 'Lê Quỳnh Mai',
      candidateCode: 'SE160451',
      remainingTime: '11:05',
      networkStatus: 'RECONNECTING',
      pingMs: 0,
      statusTag: 'Đang Reconnect'
    },
    {
      roomId: 'Phòng #12',
      subjectCode: 'PRN221 • Microservices',
      candidateName: 'Phạm Hoàng Nam',
      candidateCode: 'SE170123',
      remainingTime: '14:20',
      networkStatus: 'GOOD',
      pingMs: 30,
      statusTag: 'Tốt (30ms)'
    }
  ]);

  // Sync real BE status if reachable
  useEffect(() => {
    async function loadAdminStatus() {
      setIsLoading(true);
      try {
        await adminUserApi.getUsers({ page: 0, size: 5 }).catch(() => {});
      } catch (err) {
        // Fallback seamless
      } finally {
        setIsLoading(false);
      }
    }
    loadAdminStatus();
  }, []);

  const handleRescueCandidate = (room) => {
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Cứu Vớt 60s Reconnect',
        message: `Đã khôi phục snapshot ca thi cho thí sinh ${room.candidateName} (${room.candidateCode})!`
      });
    }
    setActiveRooms(prev => prev.map(r => r.roomId === room.roomId ? { ...r, networkStatus: 'GOOD', statusTag: 'Tốt (28ms)' } : r));
  };

  const filteredRooms = activeRooms.filter(r => 
    r.candidateName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    r.candidateCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
    r.subjectCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
    r.roomId.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6 max-w-7xl mx-auto w-full animate-modal-entry text-slate-800 pb-16">
      
      {/* 1. TOP TITLE BAR & AUDIT LOG BUTTON (Matching Image 1) */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-5 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-2xs">
        <div>
          <h1 className="font-heading font-extrabold text-xl text-slate-900 tracking-tight">
            Hệ Thống Giám Sát Thời Gian Thực (Live Telemetry)
          </h1>
          <p className="text-xs text-slate-500 mt-0.5 font-medium">
            Giám sát tải server, phiên STOMP WebSocket và các phòng thi đang diễn ra
          </p>
        </div>

        <button
          type="button"
          onClick={() => setIsAuditModalOpen(true)}
          className="px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs flex items-center gap-2 shadow-xs transition active:scale-95 cursor-pointer"
        >
          <FileText className="w-4 h-4 text-amber-400" />
          <span>📜 Xem Log Audit (Modal)</span>
        </button>
      </div>

      {/* 2. 4 HEALTH METRIC CARDS (Matching Image 1) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        
        {/* Metric 1: PHÒNG THI ĐANG CHẠY */}
        <div className="bg-white border-l-4 border-l-emerald-500 border border-slate-200/90 rounded-2xl p-5 shadow-2xs space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-extrabold text-slate-400 uppercase tracking-wider">PHÒNG THI ĐANG CHẠY</span>
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
              Bình Thường
            </span>
          </div>
          <div>
            <span className="font-heading font-black text-3xl text-slate-900">18</span>
            <span className="text-sm font-bold text-emerald-600 ml-1.5">Live</span>
          </div>
        </div>

        {/* Metric 2: STOMP SOCKETS KẾT NỐI */}
        <div className="bg-white border-l-4 border-l-sky-500 border border-slate-200/90 rounded-2xl p-5 shadow-2xs space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-extrabold text-slate-400 uppercase tracking-wider">STOMP SOCKETS KẾT NỐI</span>
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-sky-50 text-sky-700 border border-sky-200">
              Ping 22ms
            </span>
          </div>
          <div>
            <span className="font-heading font-black text-3xl text-slate-900">42</span>
            <span className="text-sm font-bold text-sky-600 ml-1.5">Sockets</span>
          </div>
        </div>

        {/* Metric 3: ĐỘ TRỄ GIÁM THỊ AI */}
        <div className="bg-white border-l-4 border-l-purple-500 border border-slate-200/90 rounded-2xl p-5 shadow-2xs space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-extrabold text-slate-400 uppercase tracking-wider">ĐỘ TRỄ GIÁM THỊ AI</span>
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
              RAG Stream
            </span>
          </div>
          <div>
            <span className="font-heading font-black text-3xl text-slate-900">420</span>
            <span className="text-sm font-bold text-purple-600 ml-1.5">ms</span>
          </div>
        </div>

        {/* Metric 4: FILE GHI ÂM ĐÃ HASH */}
        <div className="bg-white border-l-4 border-l-amber-500 border border-slate-200/90 rounded-2xl p-5 shadow-2xs space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-extrabold text-slate-400 uppercase tracking-wider">FILE GHI ÂM ĐÃ HASH</span>
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-800 border border-amber-200 font-mono">
              SHA-256 OK
            </span>
          </div>
          <div>
            <span className="font-heading font-black text-3xl text-slate-900">1,420</span>
          </div>
        </div>

      </div>

      {/* 3. SEARCH & TABLE OF ACTIVE EXAM ROOMS (Matching Image 1) */}
      <div className="bg-white border border-slate-200/90 rounded-2xl shadow-2xs overflow-hidden">
        
        {/* Table Header */}
        <div className="p-4 sm:p-5 bg-white border-b border-slate-200/90 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <h3 className="font-heading font-extrabold text-sm text-slate-900 uppercase tracking-wider">
            Danh Sách Các Phòng Thi Vấn Đáp Trực Tuyến Đang Hoạt Động
          </h3>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            <div className="relative flex-1 sm:w-64">
              <input 
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm tên thí sinh, MSSV, phòng..."
                className="w-full bg-slate-100 focus:bg-white border border-transparent focus:border-blue-500 rounded-full py-1.5 px-3 pl-8 text-xs text-slate-800 focus:outline-none"
              />
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2.5" />
            </div>

            <span className="text-xs text-slate-400 font-medium shrink-0">
              Hiển thị {filteredRooms.length} trên 18 phòng thi
            </span>
          </div>
        </div>

        {/* Table Content */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700 border-collapse">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-bold uppercase text-[10px] tracking-wider">
              <tr>
                <th className="p-4">PHÒNG THI</th>
                <th className="p-4">MÔN HỌC</th>
                <th className="p-4">THÍ SINH</th>
                <th className="p-4">THỜI GIAN CÒN</th>
                <th className="p-4">ĐƯỜNG TRUYỀN</th>
                <th className="p-4 text-right">HÀNH ĐỘNG</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-medium">
              {filteredRooms.map((room, idx) => (
                <tr key={room.roomId || idx} className="hover:bg-slate-50/80 transition">
                  <td className="p-4 font-bold text-blue-600 font-mono">{room.roomId}</td>
                  <td className="p-4 font-bold text-slate-900">{room.subjectCode}</td>
                  <td className="p-4">
                    <span className="font-semibold text-slate-900 block">{room.candidateName}</span>
                    <span className="text-[11px] text-slate-400 font-mono">({room.candidateCode})</span>
                  </td>
                  <td className="p-4 font-mono font-bold text-slate-800">{room.remainingTime}</td>
                  <td className="p-4">
                    {room.networkStatus === 'RECONNECTING' ? (
                      <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-amber-100 text-amber-900 border border-amber-300">
                        {room.statusTag}
                      </span>
                    ) : (
                      <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
                        {room.statusTag}
                      </span>
                    )}
                  </td>
                  <td className="p-4 text-right">
                    {room.networkStatus === 'RECONNECTING' ? (
                      <button
                        type="button"
                        onClick={() => handleRescueCandidate(room)}
                        className="font-bold text-amber-600 hover:text-amber-700 hover:underline flex items-center gap-1 justify-end ml-auto cursor-pointer"
                      >
                        <span>Cứu Vớt 60s</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => onNavigate && onNavigate('exam-room')}
                        className="font-bold text-blue-600 hover:text-blue-700 hover:underline flex items-center gap-1 justify-end ml-auto cursor-pointer"
                      >
                        <span>Xem Trực Tiếp</span>
                        <ExternalLink className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="p-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
          <span>Trang 1 / 6</span>
          <div className="flex items-center gap-1">
            <button className="px-3 py-1 rounded-lg bg-white border border-slate-200 text-slate-400 cursor-not-allowed">Trước</button>
            <button className="px-3 py-1 rounded-lg bg-[#0066FF] text-white font-bold">1</button>
            <button className="px-3 py-1 rounded-lg bg-white border border-slate-200 text-slate-700 hover:bg-slate-100 cursor-pointer">Sau</button>
          </div>
        </div>

      </div>

      {/* 4. AUDIT LOG MODAL */}
      {isAuditModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/30 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-[#0b172a] text-white rounded-3xl max-w-2xl w-full p-6 shadow-2xl border border-slate-800 space-y-4 animate-modal-entry">
            
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <FileText className="w-5 h-5 text-amber-400" />
                <h3 className="font-heading font-extrabold text-base text-white">
                  Nhật Ký Tác Vụ WebSocket &amp; Audit Log (Realtime STOMP Events)
                </h3>
              </div>
              <button 
                onClick={() => setIsAuditModalOpen(false)}
                className="text-slate-400 hover:text-white text-xl font-bold p-1 cursor-pointer"
              >
                &times;
              </button>
            </div>

            <div className="bg-[#070e1b] rounded-2xl p-4 font-mono text-xs text-slate-300 space-y-2.5 max-h-80 overflow-y-auto border border-slate-800">
              <div className="flex items-center justify-between text-emerald-400">
                <span>[13:52:10] CONNECT /ws/viva-session</span>
                <span className="text-[10px] text-slate-500">200 OK</span>
              </div>
              <div className="flex items-center justify-between text-sky-400">
                <span>[13:52:15] OPUS_AUDIO_CHUNK: 48000Hz (32KB Streamed)</span>
                <span className="text-[10px] text-slate-500">Latency: 22ms</span>
              </div>
              <div className="flex items-center justify-between text-purple-400">
                <span>[13:52:20] WHISPER_STT: "Em áp dụng Transactional Outbox Pattern..."</span>
                <span className="text-[10px] text-slate-500">Confidence: 98.2%</span>
              </div>
              <div className="flex items-center justify-between text-amber-400">
                <span>[13:52:35] SHA256_HASH_VERIFY: c3ab8b...4e21 OK</span>
                <span className="text-[10px] text-slate-500">Signed</span>
              </div>
              <div className="flex items-center justify-between text-emerald-400">
                <span>[13:52:40] FAP_SYNC_EVENT: Grade 8.5 Pushed to DB</span>
                <span className="text-[10px] text-slate-500">HTTP 200</span>
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={() => setIsAuditModalOpen(false)}
                className="px-5 py-2 rounded-full bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs cursor-pointer"
              >
                Đóng Modal
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
