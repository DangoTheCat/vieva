import React, { useState } from 'react';
import { 
  ArrowLeft, 
  Globe, 
  Play, 
  RefreshCw, 
  Bookmark, 
  Share2, 
  User, 
  CheckCircle2, 
  X,
  FileText
} from 'lucide-react';

export function SpaceDetailPage({ onNavigate, showToast }) {
  const [activeSubTab, setActiveSubTab] = useState('about');
  const [isNodeModalOpen, setIsNodeModalOpen] = useState(false);
  const [modalNodeData, setModalNodeData] = useState({
    title: 'Prompt Giám Thị Dr. Sophia',
    type: 'MAIN',
    prompt: 'Em hãy giải thích lý do tại sao Onion Architecture đảo ngược chiều phụ thuộc của Data Access layer?',
    timeout: 120
  });

  const handleRemixClick = () => {
    setModalNodeData({
      title: 'Remix Đề Thi: Tùy Chỉnh 3 Whys',
      type: 'MAIN',
      prompt: 'Phân tích tại sao tầng Domain Core không được phép phụ thuộc trực tiếp vào ORM?',
      timeout: 120
    });
    setIsNodeModalOpen(true);
  };

  const handleBookmark = () => {
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Bộ Sưu Tập',
        message: 'Đã lưu phòng thi "The 3 Whys: Onion Architecture" vào danh sách cá nhân!'
      });
    }
  };

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
    }
    if (showToast) {
      showToast({
        type: 'info',
        title: 'Sao Chép Liên Kết',
        message: 'Đã sao chép liên kết phòng thi vấn đáp vào bộ nhớ tạm!'
      });
    }
  };

  return (
    <div className="space-y-6 animate-modal-entry max-w-4xl mx-auto">
      {/* Breadcrumbs & Public Badge */}
      <div className="flex items-center justify-between text-xs text-slate-500">
        <button 
          onClick={() => onNavigate && onNavigate('discover')} 
          className="inline-flex items-center gap-1.5 hover:text-slate-900 font-semibold transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Khám Phá Phòng Thi</span>
        </button>
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-100 text-slate-700 font-medium">
          <Globe className="w-3.5 h-3.5 text-blue-600" />
          <span>Không gian này mở công khai (Public Space)</span>
        </div>
      </div>

      {/* Space Hero Block */}
      <div className="flex flex-col md:flex-row items-start justify-between gap-6 pt-2">
        <div className="space-y-3 flex-1">
          <h1 className="font-heading font-extrabold text-2xl sm:text-3xl text-slate-900 tracking-tight">
            The 3 Whys: Vấn Đáp Kiến Trúc Phần Mềm (SWD392)
          </h1>
          <div className="flex items-center gap-2 text-xs text-slate-600">
            <div className="w-5 h-5 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center text-[10px]">
              NV
            </div>
            <span>bởi <strong>TS. Nguyễn Văn A</strong></span>
            <span>•</span>
            <span>Bộ môn SE • FPT University</span>
          </div>
          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed pt-1">
            Phòng thi vấn đáp hướng dẫn sinh viên đào sâu vào bản chất kiến trúc Onion Architecture và Transactional Outbox Pattern thông qua phương pháp "3 Lần Tại Sao" (The 3 Whys), giúp phát hiện kiến thức cốt lõi và tư duy thiết kế hệ thống.
          </p>
        </div>

        {/* Thumbnail Art */}
        <div className="w-36 h-28 rounded-2xl bg-gradient-to-tr from-blue-100 to-sky-200 border border-slate-200 flex items-center justify-center text-4xl shadow-2xs shrink-0">
          🧅
        </div>
      </div>

      {/* Pill Action Buttons Bar */}
      <div className="flex flex-wrap items-center gap-3 pt-2">
        {/* Primary Solid Blue Pill */}
        <button 
          onClick={() => onNavigate && onNavigate('exam-room')} 
          className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs sm:text-sm px-6 py-2.5 rounded-full shadow-2xs active:scale-95 transition cursor-pointer"
        >
          <Play className="w-4 h-4 fill-current" />
          <span>Preview &amp; Vào Thi Ngay</span>
        </button>

        {/* Outline Pill: Remix */}
        <button 
          onClick={handleRemixClick} 
          className="inline-flex items-center gap-1.5 bg-white border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium text-xs sm:text-sm px-4 py-2.5 rounded-full shadow-2xs transition cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5 text-purple-600" />
          <span>Remix Đề Thi</span>
        </button>

        {/* Outline Pill: Bookmark */}
        <button 
          onClick={handleBookmark} 
          className="inline-flex items-center gap-1.5 bg-white border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium text-xs sm:text-sm px-4 py-2.5 rounded-full shadow-2xs transition cursor-pointer"
        >
          <Bookmark className="w-3.5 h-3.5 text-amber-500" />
          <span>Lưu Bộ Sưu Tập</span>
        </button>

        {/* Outline Pill: Share */}
        <button 
          onClick={handleShare} 
          className="inline-flex items-center gap-1.5 bg-white border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium text-xs sm:text-sm px-4 py-2.5 rounded-full shadow-2xs transition cursor-pointer"
        >
          <Share2 className="w-3.5 h-3.5 text-emerald-600" />
          <span>Chia Sẻ</span>
        </button>
      </div>

      {/* Underline Sub-Navigation Tabs */}
      <div className="flex items-center gap-8 border-b border-slate-200 text-xs sm:text-sm font-semibold pt-4">
        <button 
          onClick={() => setActiveSubTab('about')}
          className={`pb-3 border-b-2 transition ${activeSubTab === 'about' ? 'border-slate-900 text-slate-900 font-bold' : 'border-transparent text-slate-500 hover:text-slate-800'}`}
        >
          Giới Thiệu (About)
        </button>
        <button 
          onClick={() => {
            setActiveSubTab('prompt');
            setModalNodeData({
              title: 'Prompt Giám Thị Dr. Sophia',
              type: 'MAIN',
              prompt: 'Em hãy giải thích lý do tại sao Onion Architecture đảo ngược chiều phụ thuộc của Data Access layer?',
              timeout: 120
            });
            setIsNodeModalOpen(true);
          }}
          className={`pb-3 border-b-2 transition ${activeSubTab === 'prompt' ? 'border-slate-900 text-slate-900 font-bold' : 'border-transparent text-slate-500 hover:text-slate-800'}`}
        >
          Prompt Giám Thị (Prompt)
        </button>
        <button 
          onClick={() => setActiveSubTab('history')}
          className={`pb-3 border-b-2 transition ${activeSubTab === 'history' ? 'border-slate-900 text-slate-900 font-bold' : 'border-transparent text-slate-500 hover:text-slate-800'}`}
        >
          Lịch Sử Phiên Thi (My sessions)
        </button>
      </div>

      {/* Numbered Agenda List in Elevated Card */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-6 sm:p-8 space-y-6 shadow-2xs">
        <h3 className="font-heading font-bold text-sm text-slate-900 uppercase tracking-wider">
          Tiến Trình 3 Lượt Hỏi Vấn Đáp (The 3 Whys Agenda)
        </h3>

        <div className="space-y-5 text-xs sm:text-sm text-slate-700">
          
          {/* Turn 1 */}
          <div className="flex items-start gap-3.5">
            <span className="w-6 h-6 rounded-full bg-slate-900 text-white font-bold text-xs flex items-center justify-center shrink-0 mt-0.5">
              1
            </span>
            <div className="space-y-1">
              <h4 className="font-bold text-slate-900">First why: Nhận diện ranh giới Domain Core</h4>
              <p className="text-slate-600 leading-relaxed">
                Giám thị AI yêu cầu sinh viên làm rõ tại sao Entities trong Domain Core phải hoàn toàn độc lập và không chứa annotation của Hibernate/EF Core.
              </p>
            </div>
          </div>

          {/* Turn 2 */}
          <div className="flex items-start gap-3.5">
            <span className="w-6 h-6 rounded-full bg-slate-900 text-white font-bold text-xs flex items-center justify-center shrink-0 mt-0.5">
              2
            </span>
            <div className="space-y-1">
              <h4 className="font-bold text-slate-900">Second why: Phân tích Dependency Inversion Principle (DIP)</h4>
              <p className="text-slate-600 leading-relaxed">
                Nếu database thay đổi hoặc hệ thống cần mock data để unit test, cơ chế Dependency Inversion bảo vệ tầng nghiệp vụ như thế nào?
              </p>
            </div>
          </div>

          {/* Turn 3 */}
          <div className="flex items-start gap-3.5">
            <span className="w-6 h-6 rounded-full bg-slate-900 text-white font-bold text-xs flex items-center justify-center shrink-0 mt-0.5">
              3
            </span>
            <div className="space-y-1">
              <h4 className="font-bold text-slate-900">Third why: Hỏi xoáy Transactional Outbox &amp; Dual-Write</h4>
              <p className="text-slate-600 leading-relaxed">
                Đào sâu giải pháp khi lưu dữ liệu bài thi vào DB và gửi message sang Message Broker đồng thời. Sinh viên bảo vệ kiến trúc chống thất thoát dữ liệu.
              </p>
            </div>
          </div>

        </div>

        {/* Bottom Prompt Quote */}
        <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 text-xs text-slate-600 italic">
          "Hệ thống tự động chấm điểm theo 4 tiêu chí Rubric ngay khi hoàn thành câu trả lời thứ 3 và đẩy kết quả sang Module Grading."
        </div>
      </div>

      {/* NODE CONFIG MODAL */}
      {isNodeModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-modal-entry text-slate-800">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-heading font-bold text-base text-slate-900">{modalNodeData.title}</h3>
              <button onClick={() => setIsNodeModalOpen(false)} className="text-slate-400 hover:text-slate-700 p-1">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3.5 text-xs">
              <div>
                <label className="font-bold text-slate-800 block mb-1">Loại Lượt Hỏi</label>
                <select 
                  value={modalNodeData.type}
                  onChange={(e) => setModalNodeData({ ...modalNodeData, type: e.target.value })}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                >
                  <option value="MAIN">Hỏi Chính (MAIN Turn)</option>
                  <option value="PROBING">Hỏi Xoáy Thích Ứng (Adaptive Probing)</option>
                  <option value="OUTBOX">Bàn Giao Kết Quả (Outbox Handoff)</option>
                </select>
              </div>

              <div>
                <label className="font-bold text-slate-800 block mb-1">Nội Dung Prompt Giám Thị AI</label>
                <textarea 
                  rows={3} 
                  value={modalNodeData.prompt}
                  onChange={(e) => setModalNodeData({ ...modalNodeData, prompt: e.target.value })}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-bold text-slate-800 block mb-1">Thời Gian Trả Lời (giây)</label>
                  <input 
                    type="number" 
                    value={modalNodeData.timeout}
                    onChange={(e) => setModalNodeData({ ...modalNodeData, timeout: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" 
                  />
                </div>
                <div>
                  <label className="font-bold text-slate-800 block mb-1">Liên Kết Tiêu Chí Rubric</label>
                  <select className="w-full bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none">
                    <option>C1: Onion Architecture (30%)</option>
                    <option>C2: Transactional Outbox (25%)</option>
                    <option>C3: Adaptive Probing (25%)</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2.5">
              <button 
                type="button"
                onClick={() => setIsNodeModalOpen(false)} 
                className="px-4 py-2 rounded-full border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50"
              >
                Hủy
              </button>
              <button 
                type="button"
                onClick={() => {
                  setIsNodeModalOpen(false);
                  if (showToast) showToast({ type: 'success', message: 'Đã lưu cấu hình prompt giám thị AI!' });
                }} 
                className="px-5 py-2 rounded-full bg-slate-900 text-white text-xs font-bold active:scale-95 transition"
              >
                Lưu Thay Đổi
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
