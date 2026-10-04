import React, { useState } from 'react';
import { 
  Users, 
  CheckCircle2, 
  Clock, 
  Search, 
  Download, 
  RefreshCw, 
  Play, 
  Eye, 
  Check, 
  X, 
  ShieldCheck, 
  FileText,
  ChevronRight,
  Sparkles
} from 'lucide-react';

export function LecturerGradingQueuePage({ onNavigate, showToast }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSlot, setSelectedSlot] = useState('all');
  const [selectedStatus, setSelectedStatus] = useState('all');

  // Quick Grade Modal State
  const [activeModalStudent, setActiveModalStudent] = useState(null);
  const [modalNotes, setModalNotes] = useState(
    'Thí sinh nắm chắc lý thuyết Onion Architecture và phân biệt rõ Monolith vs Microservices. Đạt chuẩn.'
  );

  const candidateList = [
    {
      id: 'SE160982',
      name: 'Nguyen Van B',
      mssv: 'SE160982',
      dept: 'Software Eng.',
      slot: 'Slot 4 (09:15 - 09:45)',
      room: 'Phòng thi ảo #4',
      progress: '5 / 5 Câu',
      audioStatus: '100% Ghi âm',
      duration: '26m 40s',
      aiScore: 8.2,
      tier: 'Tier 3: Proficient',
      status: 'pending', // pending, approved
      matchRate: '92%'
    },
    {
      id: 'SE160411',
      name: 'Tran Thi C',
      mssv: 'SE160411',
      dept: 'Software Eng.',
      slot: 'Slot 4 (09:15 - 09:45)',
      room: 'Phòng thi ảo #2',
      progress: '5 / 5 Câu',
      audioStatus: '100% Ghi âm',
      duration: '28m 15s',
      aiScore: 9.1,
      tier: 'Tier 4: Distinction',
      status: 'approved',
      matchRate: '98%'
    },
    {
      id: 'SE161002',
      name: 'Le Hoang D',
      mssv: 'SE161002',
      dept: 'Software Eng.',
      slot: 'Slot 3 (08:00 - 08:30)',
      room: 'Phòng thi ảo #1',
      progress: '5 / 5 Câu',
      audioStatus: '100% Ghi âm',
      duration: '24m 50s',
      aiScore: 6.8,
      tier: 'Tier 2: Developing',
      status: 'pending',
      matchRate: '88%'
    }
  ];

  const filteredCandidates = candidateList.filter((item) => {
    const matchesSearch = 
      item.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.mssv.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = selectedStatus === 'all' || item.status === selectedStatus;
    return matchesSearch && matchesStatus;
  });

  const handleQuickGradeConfirm = () => {
    if (showToast && activeModalStudent) {
      showToast({
        type: 'success',
        title: 'Ký Duyệt Thành Công',
        message: `Đã phê duyệt điểm ${activeModalStudent.aiScore} cho thí sinh ${activeModalStudent.name} (${activeModalStudent.mssv})!`
      });
    }
    setActiveModalStudent(null);
  };

  const handleExportCsv = () => {
    if (showToast) {
      showToast({
        type: 'info',
        title: 'Xuất Báo Cáo',
        message: 'Đang khởi tạo tệp CSV danh sách điểm thi SWD392...'
      });
    }
  };

  const handleSyncFap = () => {
    if (showToast) {
      showToast({
        type: 'success',
        title: 'Đồng Bộ FAP Portal',
        message: 'Đã hoàn tất đồng bộ bảng điểm 30 thí sinh chốt điểm vào hệ thống FAP!'
      });
    }
  };

  return (
    <div className="space-y-6 animate-modal-entry">
      {/* HEADER & ACTION */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-bold text-sky-700 bg-sky-50 px-2.5 py-0.5 rounded-full border border-sky-200">
              Khảo Thí Vấn Đáp • Fall 2026
            </span>
            <span className="text-xs text-slate-400">• Cohort SE1701</span>
          </div>
          <h1 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
            Danh Sách Thí Sinh &amp; Hàng Đợi Hậu Kiểm Điểm
          </h1>
          <p className="text-xs text-slate-500 mt-1 max-w-2xl leading-relaxed">
            Xem lại các lượt vấn đáp của sinh viên, nghe băng ghi âm câu trả lời, so sánh điểm sơ bộ của AI với Rubric và ký xác nhận điểm chính thức.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleSyncFap}
            className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-white text-xs font-bold shadow-md shadow-emerald-600/20 flex items-center gap-1.5 transition active:scale-95 cursor-pointer"
          >
            <CheckCircle2 className="w-4 h-4" />
            <span>Đồng Bộ Điểm FAP</span>
          </button>
          <button 
            onClick={handleExportCsv}
            className="px-4 py-2 rounded-xl border border-slate-300 bg-white hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center gap-1.5 shadow-2xs transition active:scale-95 cursor-pointer"
          >
            <Download className="w-4 h-4 text-sky-600" />
            <span>Xuất Báo Cáo CSV</span>
          </button>
        </div>
      </div>

      {/* 3 HIGH CONTRAST KPI CARDS */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Card 1 */}
        <div className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-2xs flex items-center justify-between">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-xl bg-sky-50 text-sky-700 flex items-center justify-center border border-sky-100">
              <Users className="w-6 h-6" />
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Tổng Số Thí Sinh</p>
              <p className="font-heading font-extrabold text-xl text-slate-900">42 Thí Sinh</p>
            </div>
          </div>
          <span className="text-[11px] font-mono text-slate-500 bg-slate-100 px-2 py-1 rounded">Cohort SE-26A</span>
        </div>

        {/* Card 2 */}
        <div className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-2xs flex items-center justify-between">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-100">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Đã Duyệt Điểm FAP</p>
              <p className="font-heading font-extrabold text-xl text-slate-900">30 Đã Xác Nhận</p>
            </div>
          </div>
          <span className="text-[11px] font-bold text-emerald-600 bg-emerald-50 border border-emerald-200 px-2 py-1 rounded">71.4% Rate</span>
        </div>

        {/* Card 3 */}
        <div className="bg-amber-50/40 border border-amber-200 rounded-2xl p-5 shadow-2xs flex items-center justify-between">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center border border-amber-200">
              <Clock className="w-6 h-6" />
            </div>
            <div>
              <p className="text-[10px] text-amber-800 font-bold uppercase tracking-wider">Chờ Giảng Viên Duyệt</p>
              <p className="font-heading font-extrabold text-xl text-amber-900">12 Bài Cần Ký</p>
            </div>
          </div>
          <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-amber-500 text-white animate-pulse">Cần Xử Lý</span>
        </div>
      </div>

      {/* FILTER & SEARCH TOOLBAR */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-2xs flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3 text-xs">
        {/* Search Input */}
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
          <input 
            type="text" 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo MSSV (SE...), họ tên sinh viên hoặc phòng thi..." 
            className="w-full pl-9 pr-4 py-2 rounded-xl border border-slate-200 bg-slate-50/50 text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        {/* Dropdowns */}
        <div className="flex flex-wrap items-center gap-2">
          <select 
            value={selectedSlot}
            onChange={(e) => setSelectedSlot(e.target.value)}
            className="px-3 py-2 rounded-xl border border-slate-200 bg-white font-semibold text-slate-700 focus:ring-2 focus:ring-sky-500 focus:outline-none"
          >
            <option value="all">Tất cả ca thi hôm nay</option>
            <option value="slot4">Ca Thi: Slot 4 (09:15 - 09:45)</option>
            <option value="slot3">Ca Thi: Slot 3 (08:00 - 08:30)</option>
          </select>

          <select 
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="px-3 py-2 rounded-xl border border-slate-200 bg-white font-semibold text-slate-700 focus:ring-2 focus:ring-sky-500 focus:outline-none"
          >
            <option value="all">Tất cả trạng thái</option>
            <option value="pending">Chờ duyệt (Pending Review)</option>
            <option value="approved">Đã duyệt (Approved)</option>
          </select>
        </div>
      </div>

      {/* CANDIDATE GRADING TABLE */}
      <div className="bg-white border border-slate-200/90 rounded-2xl shadow-2xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr class="bg-slate-50 border-b border-slate-200/80 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                <th className="py-3.5 px-4">Thí Sinh / MSSV</th>
                <th className="py-3.5 px-4">Ca Thi &amp; Phòng</th>
                <th className="py-3.5 px-4">Tiến Độ Vấn Đáp</th>
                <th className="py-3.5 px-4">Điểm AI Sơ Bộ</th>
                <th className="py-3.5 px-4">Trạng Thái</th>
                <th className="py-3.5 px-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredCandidates.map((cand) => (
                <tr key={cand.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="py-4 px-4 whitespace-nowrap">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-sky-100 text-sky-700 font-bold flex items-center justify-center text-xs">
                        {cand.name.split(' ').map(n => n[0]).join('').substring(0, 2)}
                      </div>
                      <div>
                        <div className="flex items-center gap-1.5">
                          <span className="font-bold text-slate-900 text-sm">{cand.name}</span>
                          {cand.status === 'pending' && (
                            <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" title="Cần duyệt điểm"></span>
                          )}
                        </div>
                        <p className="text-[11px] text-slate-400 font-mono">{cand.mssv} • {cand.dept}</p>
                      </div>
                    </div>
                  </td>
                  <td className="py-4 px-4 whitespace-nowrap">
                    <span className="font-medium text-slate-800">{cand.slot}</span>
                    <span className="block text-[11px] text-slate-400">{cand.room}</span>
                  </td>
                  <td className="py-4 px-4 whitespace-nowrap">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-900">{cand.progress}</span>
                      <span className="text-[10px] text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded font-bold">{cand.audioStatus}</span>
                    </div>
                    <span className="block text-[11px] text-slate-400 mt-0.5">Thời lượng: {cand.duration}</span>
                  </td>
                  <td className="py-4 px-4 whitespace-nowrap">
                    <div className="flex items-baseline gap-1">
                      <span className="font-heading font-extrabold text-base text-slate-900">{cand.aiScore}</span>
                      <span className="text-[11px] text-slate-400">/ 10</span>
                    </div>
                    <span className="px-2 py-0.5 rounded-full bg-sky-50 text-sky-700 font-bold text-[10px] border border-sky-200">
                      {cand.tier}
                    </span>
                  </td>
                  <td className="py-4 px-4 whitespace-nowrap">
                    {cand.status === 'pending' ? (
                      <span className="px-2.5 py-1 rounded-full bg-amber-100 text-amber-800 font-bold text-[10px] border border-amber-300">
                        CHỜ DUYỆT ĐIỂM
                      </span>
                    ) : (
                      <span className="px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 font-bold text-[10px] border border-emerald-300">
                        ĐÃ DUYỆT FAP ✓
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-4 whitespace-nowrap text-right space-x-1">
                    <button 
                      onClick={() => setActiveModalStudent(cand)}
                      className="px-3 py-1.5 rounded-lg border border-slate-300 hover:bg-slate-100 text-slate-700 font-bold transition active:scale-95"
                    >
                      Xem Nhanh
                    </button>
                    <button 
                      onClick={() => onNavigate && onNavigate('lecturer-review-regrading')}
                      className="px-3 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-700 text-white font-bold shadow-2xs inline-flex items-center gap-1 transition active:scale-95"
                    >
                      <span>Chấm Chi Tiết</span>
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* PAGINATION BAR */}
        <div className="p-4 bg-slate-50/70 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
          <span>Hiển thị {filteredCandidates.length} trong tổng số 42 thí sinh ca sáng</span>

          <div className="flex items-center gap-1 font-mono">
            <button className="px-2.5 py-1.5 rounded-lg border border-slate-200 bg-white text-slate-400" disabled>
              Trước
            </button>
            <button className="px-3 py-1.5 rounded-lg bg-sky-600 text-white font-bold">1</button>
            <button className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-100 text-slate-700">2</button>
            <button className="px-2.5 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-100 text-slate-600">
              Sau
            </button>
          </div>
        </div>
      </div>

      {/* ================= MODAL: QUICK VIVA EVALUATION ================= */}
      {activeModalStudent && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry">
            <div className="p-5 border-b border-slate-100 bg-slate-950 text-white flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-sky-500/20 text-sky-300 flex items-center justify-center">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-heading font-bold text-sm">Xem Nhanh &amp; Ký Duyệt Điểm Thi</h3>
                  <p className="text-[11px] text-slate-400">{activeModalStudent.name} • {activeModalStudent.mssv}</p>
                </div>
              </div>
              <button onClick={() => setActiveModalStudent(null)} className="text-slate-400 hover:text-white p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 space-y-4 text-xs">
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-between">
                <div>
                  <span className="text-slate-400 font-bold block uppercase text-[10px]">Điểm AI Sơ Bộ:</span>
                  <span className="font-heading font-extrabold text-2xl text-slate-900">{activeModalStudent.aiScore} / 10</span>
                </div>
                <span className="px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 font-bold text-xs border border-emerald-300">
                  Khớp {activeModalStudent.matchRate} Rubric
                </span>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Mẫu Âm Thanh Câu 1 (Nghe Thử):</label>
                <div className="p-3 bg-slate-950 text-white rounded-xl flex items-center gap-3">
                  <button 
                    onClick={() => {
                      if (showToast) showToast({ type: 'info', message: 'Đang phát audio câu trả lời của thí sinh...' });
                    }} 
                    className="w-8 h-8 rounded-full bg-sky-500 text-white flex items-center justify-center shrink-0 active:scale-95 transition"
                  >
                    <Play className="w-4 h-4 fill-current ml-0.5" />
                  </button>
                  <div className="flex-1">
                    <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
                      <div className="bg-sky-400 h-1.5 rounded-full" style={{ width: '45%' }}></div>
                    </div>
                    <div className="flex justify-between text-[10px] text-slate-400 mt-1 font-mono">
                      <span>01:15</span>
                      <span>02:45</span>
                    </div>
                  </div>
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Nhận Xét Của Giảng Viên (Tùy Chọn):</label>
                <textarea 
                  rows={2} 
                  value={modalNotes}
                  onChange={(e) => setModalNotes(e.target.value)}
                  placeholder="Ghi chú phản hồi cho thí sinh hoặc hội đồng..." 
                  className="w-full p-2.5 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none text-slate-800"
                />
              </div>
            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-3">
              <button 
                onClick={() => setActiveModalStudent(null)} 
                className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold"
              >
                Đóng
              </button>
              <button 
                onClick={handleQuickGradeConfirm} 
                className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold shadow-md shadow-emerald-600/20 flex items-center gap-1.5 transition active:scale-95"
              >
                <Check className="w-4 h-4" />
                <span>Ký Duyệt Điểm Ngay</span>
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
