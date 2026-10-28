import React, { useState, useEffect } from 'react';
import { adminAnalyticsApi } from '../api/adminAnalyticsApi';
import { getErrorMessage } from '../utils/errorCodes';
import { gradeLabel } from '../utils/activeExam';
import { Pagination } from '../components/common/Pagination';
import { Users, GraduationCap, School, Shield, ClipboardList, Activity, RotateCcw, BarChart3 } from 'lucide-react';

const fmt = (n) => (n != null ? Number(n).toFixed(2).replace(/\.00$/, '') : '—');

function StatTile({ icon: Icon, label, value, hint }) {
  return (
    <div className="bg-white border border-pink-200 rounded-xl p-4 shadow-2xs">
      <div className="flex items-center gap-2 text-[11px] font-bold text-green-700 uppercase tracking-wide"><Icon className="w-4 h-4" /> {label}</div>
      <p className="font-heading font-black text-2xl mt-1">{value ?? '—'}</p>
      {hint && <p className="text-[11px] text-green-800 mt-0.5">{hint}</p>}
    </div>
  );
}

/**
 * Admin system overview: GET /admin/stats counters and the paged GET /admin/performance results.
 */
export function AdminOperationsPage({ showToast }) {
  const [stats, setStats] = useState(null);
  const [perf, setPerf] = useState({ content: [], page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [isLoading, setIsLoading] = useState(true);

  const loadStats = async () => {
    try {
      setStats(await adminAnalyticsApi.getStats());
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    }
  };

  const loadPerformance = async (page = perf.page, size = perf.size) => {
    setIsLoading(true);
    try {
      const data = await adminAnalyticsApi.getPerformance({ page, size });
      setPerf({
        content: data?.content || [],
        page: data?.page ?? page,
        size: data?.size ?? size,
        totalPages: data?.totalPages || 1,
        totalElements: data?.totalElements || 0
      });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadStats();
    loadPerformance(0, 10);
  }, []);

  return (
    <div className="max-w-6xl mx-auto w-full space-y-6 pb-10 text-ink animate-modal-entry">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-heading font-extrabold text-2xl tracking-tight">Thống Kê Hệ Thống</h1>
          <p className="text-xs text-green-800 mt-1">Số liệu người dùng, kỳ thi, lượt thi và kết quả trên toàn hệ thống.</p>
        </div>
        <button type="button" onClick={() => { loadStats(); loadPerformance(); }}
          className="px-3.5 py-1.5 rounded-xl bg-white border border-pink-200 text-xs font-semibold text-slate-700 flex items-center gap-1.5 hover:bg-slate-50 shadow-2xs cursor-pointer">
          <RotateCcw className="w-3.5 h-3.5" /> Làm mới
        </button>
      </header>

      <section className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatTile icon={Users} label="Người dùng" value={stats?.totalUsers} />
        <StatTile icon={GraduationCap} label="Sinh viên" value={stats?.studentCount} />
        <StatTile icon={School} label="Giảng viên" value={stats?.lecturerCount} />
        <StatTile icon={Shield} label="Quản trị" value={stats?.adminCount} />
        <StatTile icon={ClipboardList} label="Kỳ thi" value={stats?.totalExams}
          hint={stats && `${stats.draftExams} nháp • ${stats.inProgressExams} đang mở • ${stats.completedExams} đã đóng`} />
        <StatTile icon={Activity} label="Lượt thi" value={stats?.totalAttempts}
          hint={stats && `${stats.inProgressAttempts + stats.reconnectingAttempts} đang thi • ${stats.finalizingAttempts} đang chấm`} />
        <StatTile icon={BarChart3} label="Hoàn thành" value={stats?.completedAttempts} />
        <StatTile icon={Activity} label="Gián đoạn" value={stats?.interruptedAttempts} />
      </section>

      <section className="bg-white border border-pink-200 rounded-2xl shadow-2xs overflow-hidden">
        <div className="p-4 border-b border-pink-200">
          <h2 className="font-heading font-bold text-sm">Kết quả thi gần đây</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-pink-200 text-slate-700 font-semibold uppercase text-[10px] tracking-wider">
              <tr>
                <th className="py-3 px-4">Thí sinh</th>
                <th className="py-3 px-4">Môn học</th>
                <th className="py-3 px-4">Kỳ thi</th>
                <th className="py-3 px-4">Điểm</th>
                <th className="py-3 px-4">Xếp loại</th>
                <th className="py-3 px-4">Hoàn tất</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-green-100">
              {isLoading ? (
                [0, 1, 2].map(i => <tr key={i}><td colSpan={6} className="p-3"><div className="h-8 rounded-lg animate-pulse bg-slate-200/80" /></td></tr>)
              ) : perf.content.length === 0 ? (
                <tr><td colSpan={6} className="p-10 text-center text-green-800">Chưa có lượt thi nào hoàn tất.</td></tr>
              ) : perf.content.map(r => (
                <tr key={r.attemptId} className="hover:bg-green-50/50">
                  <td className="py-3 px-4"><p className="font-bold">{r.studentName}</p><p className="text-[11px] text-green-800">{r.studentEmail}</p></td>
                  <td className="py-3 px-4">{r.subjectName}</td>
                  <td className="py-3 px-4">{r.examTitle}</td>
                  <td className="py-3 px-4 font-mono font-black text-pink-700">{fmt(r.totalScore)}</td>
                  <td className="py-3 px-4">{gradeLabel(r.gradeLevel)}</td>
                  <td className="py-3 px-4">{r.completedAt ? new Date(r.completedAt).toLocaleString('vi-VN') : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="p-3 border-t border-pink-200">
          <Pagination
            page={perf.page}
            size={perf.size}
            totalPages={perf.totalPages}
            totalElements={perf.totalElements}
            onPageChange={(p) => loadPerformance(p, perf.size)}
            onSizeChange={(s) => loadPerformance(0, s)}
            itemLabel="lượt thi"
          />
        </div>
      </section>
    </div>
  );
}
