import React, { useState, useEffect } from 'react';
import { WifiOff, AlertTriangle, RefreshCw, ArrowRight } from 'lucide-react';

export function ReconnectGuardPage({ onNavigate, showToast }) {
  const [countdown, setCountdown] = useState(46);

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const handleReconnect = () => {
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Khôi Phục Kết Nối',
        message: 'Đã kết nối lại thành công tới STOMP Broker! Tiến trình ca thi được bảo toàn.'
      });
    }
    if (onNavigate) {
      onNavigate('exam-room');
    }
  };

  return (
    <div className="space-y-6 animate-modal-entry max-w-md mx-auto py-8">
      <div className="bg-white border-2 border-amber-300 rounded-3xl p-8 text-center space-y-4 shadow-md text-slate-800">
        <div className="w-14 h-14 rounded-full bg-amber-50 text-amber-600 mx-auto flex items-center justify-center text-2xl shadow-2xs">
          <WifiOff className="w-6 h-6 text-amber-600" />
        </div>
        <span className="px-3 py-1 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-200">
          MẤT KẾT NỐI MẠNG TẠM THỜI
        </span>
        <h3 className="font-heading font-bold text-lg text-slate-900">Bảo Lưu Phòng Thi Trong 60 Giây</h3>
        <p className="text-xs text-slate-500 leading-relaxed">
          Hệ thống đang giữ ấm kết nối STOMP và bảo toàn snapshot câu hỏi.
        </p>

        <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200">
          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Thời Gian Tự Động Kết Nối Lại</p>
          <p className="font-mono text-4xl font-extrabold text-amber-600 mt-1">
            00:{countdown.toString().padStart(2, '0')}
          </p>
        </div>

        <div className="flex items-center justify-center gap-3 pt-2">
          <button 
            onClick={handleReconnect} 
            className="bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs px-6 py-2.5 rounded-full shadow-xs active:scale-95 transition cursor-pointer flex items-center gap-1.5"
          >
            <span>Tái Kết Nối Ngay ↗</span>
          </button>
        </div>
      </div>
    </div>
  );
}
