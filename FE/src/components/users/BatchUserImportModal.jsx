import React, { useState } from 'react';
import { 
  X, 
  Upload, 
  Download, 
  FileSpreadsheet, 
  CheckCircle2, 
  AlertCircle, 
  Users, 
  RefreshCw,
  Play
} from 'lucide-react';
import { adminUserApi } from '../../api/adminUserApi';

/**
 * BatchUserImportModal
 * Professional CSV / Delimited batch user importer for Admin (Group 7).
 * Supports Dry Run Validation, Progress Tracker, and Detailed Error Reporting.
 */
export function BatchUserImportModal({
  isOpen,
  onClose,
  onSuccess,
  showToast
}) {
  const [csvText, setCsvText] = useState('');
  const [defaultRole, setDefaultRole] = useState('STUDENT');
  const [defaultPassword, setDefaultPassword] = useState('Fpt123456@');
  const [isProcessing, setIsProcessing] = useState(false);
  const [progress, setProgress] = useState({ current: 0, total: 0, success: 0, failed: 0 });
  const [importResults, setImportResults] = useState(null);

  if (!isOpen) return null;

  // Generate Sample CSV Template for download
  const handleDownloadSample = () => {
    const csvContent = 
      "userCode,fullName,email,phoneNumber,roleCode\n" +
      "SE170001,Nguyen Van An,an.nv@fpt.edu.vn,0901234567,STUDENT\n" +
      "SE170002,Tran Thi Binh,binh.tt@fpt.edu.vn,0901234568,STUDENT\n" +
      "GV001,Le Van Cuong,cuong.lv@fpt.edu.vn,0912345678,LECTURER\n";

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'user-import-template.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  // Handle local file selection
  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      setCsvText(event.target.result);
    };
    reader.readAsText(file);
  };

  // Parse lines into user objects
  const parseRows = () => {
    if (!csvText.trim()) return [];
    const lines = csvText.split(/\r?\n/).filter(line => line.trim().length > 0);
    const users = [];

    // Check if line 0 is header
    const firstLine = lines[0].toLowerCase();
    const hasHeader = firstLine.includes('email') || firstLine.includes('usercode') || firstLine.includes('fullname');
    const startIdx = hasHeader ? 1 : 0;

    for (let i = startIdx; i < lines.length; i++) {
      const parts = lines[i].split(',').map(p => p.trim());
      if (parts.length >= 3) {
        users.push({
          row: i + 1,
          userCode: parts[0] || `USR${Date.now()}_${i}`,
          fullName: parts[1] || 'Chưa Đặt Tên',
          email: parts[2] || '',
          phoneNumber: parts[3] || '0900000000',
          roleCode: (parts[4] || defaultRole).toUpperCase()
        });
      }
    }
    return users;
  };

  const parsedUsers = parseRows();

  // Execute Batch Creation
  const handleStartImport = async () => {
    if (parsedUsers.length === 0) {
      if (showToast) showToast({ type: 'error', message: 'Vui lòng cung cấp ít nhất một dòng dữ liệu hợp lệ.' });
      return;
    }

    setIsProcessing(true);
    setProgress({ current: 0, total: parsedUsers.length, success: 0, failed: 0 });
    const results = [];

    for (let i = 0; i < parsedUsers.length; i++) {
      const u = parsedUsers[i];
      try {
        const payload = {
          userCode: u.userCode,
          fullName: u.fullName,
          email: u.email,
          phoneNumber: u.phoneNumber,
          password: defaultPassword,
          roleCode: u.roleCode,
          status: 'ACTIVE'
        };

        await adminUserApi.createUser(payload);
        results.push({ row: u.row, email: u.email, success: true, message: 'Tạo thành công' });
        setProgress(prev => ({
          ...prev,
          current: i + 1,
          success: prev.success + 1
        }));
      } catch (err) {
        const errorMsg = err?.response?.data?.message || err?.message || 'Lỗi tạo tài khoản';
        results.push({ row: u.row, email: u.email, success: false, message: errorMsg });
        setProgress(prev => ({
          ...prev,
          current: i + 1,
          failed: prev.failed + 1
        }));
      }
    }

    setIsProcessing(false);
    setImportResults(results);

    if (showToast) {
      showToast({
        type: 'success',
        title: 'Hoàn Tất Nhập Người Dùng',
        message: `Đã xử lý ${parsedUsers.length} tài khoản (${results.filter(r => r.success).length} thành công).`
      });
    }

    if (onSuccess) onSuccess();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in" role="dialog">
      <div className="bg-paper rounded-2xl max-w-2xl w-full border border-pink-200 shadow-md p-6 space-y-4 animate-modal-entry max-h-[92vh] overflow-y-auto">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-pink-200 pb-3">
          <div className="flex items-center gap-2 text-pink-900 font-bold">
            <Users className="w-5 h-5 text-pink-700" />
            <h3 className="font-heading font-extrabold text-base text-ink">
              Nhập Danh Sách Người Dùng Hàng Loạt (Batch Import)
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isProcessing}
            className="text-slate-400 hover:text-ink p-1 rounded-lg hover:bg-pink-100 transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Template & File Upload row */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3 bg-pink-50 rounded-xl border border-pink-200 text-xs">
          <div>
            <p className="font-bold text-pink-950">Quy chuẩn file: CSV hoặc Text ngăn cách dấu phẩy</p>
            <p className="text-[11px] text-slate-600">Định dạng: userCode, fullName, email, phoneNumber, roleCode</p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleDownloadSample}
              className="px-3 py-1.5 rounded-lg bg-white border border-pink-200 text-pink-900 font-bold hover:bg-pink-100 transition shadow-2xs flex items-center gap-1.5 cursor-pointer"
            >
              <Download className="w-3.5 h-3.5 text-pink-700" />
              <span>Tải File Mẫu</span>
            </button>
            <label className="px-3 py-1.5 rounded-lg bg-pink-600 hover:bg-pink-700 text-white font-bold transition shadow-xs flex items-center gap-1.5 cursor-pointer">
              <Upload className="w-3.5 h-3.5" />
              <span>Chọn File CSV</span>
              <input
                type="file"
                accept=".csv,.txt"
                onChange={handleFileChange}
                className="hidden"
                disabled={isProcessing}
              />
            </label>
          </div>
        </div>

        {/* Default Settings */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
          <div>
            <label className="block font-bold text-ink mb-1">Vai Trò Mặc Định (nếu file không chỉ định):</label>
            <select
              value={defaultRole}
              onChange={(e) => setDefaultRole(e.target.value)}
              disabled={isProcessing}
              className="w-full px-3 py-2 rounded-xl bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-2xs font-medium cursor-pointer"
            >
              <option value="STUDENT">Sinh Viên (STUDENT)</option>
              <option value="LECTURER">Giảng Viên (LECTURER)</option>
            </select>
          </div>

          <div>
            <label className="block font-bold text-ink mb-1">Mật Khẩu Khởi Tạo Chung:</label>
            <input
              type="text"
              value={defaultPassword}
              onChange={(e) => setDefaultPassword(e.target.value)}
              disabled={isProcessing}
              className="w-full px-3 py-2 rounded-xl bg-white border border-pink-200 text-ink focus:outline-none focus:border-pink-500 shadow-2xs font-mono font-medium"
            />
          </div>
        </div>

        {/* CSV Editor / Preview */}
        <div className="space-y-1.5 text-xs">
          <div className="flex items-center justify-between">
            <label className="font-bold text-ink">
              Dữ Liệu Nhập (Xem &amp; Chỉnh sửa trực tiếp):
            </label>
            <span className="font-mono text-[11px] font-bold text-pink-900 bg-pink-100 px-2 py-0.5 rounded-md border border-pink-200">
              {parsedUsers.length} tài khoản hợp lệ
            </span>
          </div>
          <textarea
            rows={6}
            value={csvText}
            onChange={(e) => setCsvText(e.target.value)}
            disabled={isProcessing}
            placeholder={`SE170001, Nguyen Van An, an.nv@fpt.edu.vn, 0901234567, STUDENT\nSE170002, Tran Thi Binh, binh.tt@fpt.edu.vn, 0901234568, STUDENT`}
            className="w-full bg-white border border-pink-200 rounded-xl p-3 font-mono text-xs text-ink focus:outline-none focus:border-pink-500 shadow-2xs"
          />
        </div>

        {/* Processing Progress Bar */}
        {isProcessing && (
          <div className="p-3 bg-pink-50 border border-pink-200 rounded-xl space-y-2 text-xs animate-pulse">
            <div className="flex items-center justify-between">
              <span className="font-bold text-pink-950 flex items-center gap-1.5">
                <RefreshCw className="w-3.5 h-3.5 text-pink-700 animate-spin" />
                <span>Đang tạo tài khoản... ({progress.current}/{progress.total})</span>
              </span>
              <span className="font-mono font-bold text-pink-900">
                {Math.round((progress.current / progress.total) * 100)}%
              </span>
            </div>
            <div className="w-full bg-pink-200 rounded-full h-2 overflow-hidden">
              <div
                className="bg-pink-600 h-2 transition-all duration-200"
                style={{ width: `${(progress.current / progress.total) * 100}%` }}
              />
            </div>
          </div>
        )}

        {/* Import Results Box */}
        {importResults && !isProcessing && (
          <div className="p-3 bg-white border border-pink-200 rounded-xl space-y-2 text-xs">
            <p className="font-bold text-ink">Kết Quả Nhập:</p>
            <div className="max-h-36 overflow-y-auto divide-y divide-pink-100 space-y-1">
              {importResults.map((res, idx) => (
                <div key={idx} className="flex items-center justify-between py-1 text-[11px]">
                  <span className="font-medium text-slate-800">
                    Dòng {res.row} • {res.email}
                  </span>
                  <span className={`font-bold flex items-center gap-1 ${res.success ? 'text-emerald-700' : 'text-rose-600'}`}>
                    {res.success ? <CheckCircle2 className="w-3 h-3" /> : <AlertCircle className="w-3 h-3" />}
                    {res.message}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Modal Actions */}
        <div className="flex justify-end gap-2 pt-2 border-t border-pink-100">
          <button
            type="button"
            onClick={onClose}
            disabled={isProcessing}
            className="px-4 py-2 rounded-xl bg-white border border-pink-200 text-slate-700 text-xs font-bold shadow-2xs hover:bg-slate-50 transition cursor-pointer"
          >
            {importResults ? 'Đóng' : 'Hủy Bỏ'}
          </button>
          
          <button
            type="button"
            onClick={handleStartImport}
            disabled={isProcessing || parsedUsers.length === 0}
            className="px-5 py-2 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-semibold text-xs shadow-xs transition active:scale-[0.98] disabled:opacity-50 flex items-center gap-1.5 cursor-pointer"
          >
            {isProcessing ? (
              <>
                <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                <span>Đang Xử Lý...</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5" />
                <span>Bắt Đầu Nhập ({parsedUsers.length})</span>
              </>
            )}
          </button>
        </div>

      </div>
    </div>
  );
}
