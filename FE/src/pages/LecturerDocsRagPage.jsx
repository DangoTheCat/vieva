import React, { useState } from 'react';
import { 
  FileText, 
  UploadCloud, 
  Layers, 
  Database, 
  RefreshCw, 
  ArrowRight, 
  CheckCircle2, 
  Eye, 
  X, 
  Search, 
  Sparkles,
  Paperclip,
  Check
} from 'lucide-react';

export function LecturerDocsRagPage({ onNavigate, showToast }) {
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isChunksModalOpen, setIsChunksModalOpen] = useState(false);
  const [selectedDocForChunks, setSelectedDocForChunks] = useState(null);

  // Upload Form State
  const [chunkSize, setChunkSize] = useState('512');
  const [chunkOverlap, setChunkOverlap] = useState('50');
  const [embeddingModel, setEmbeddingModel] = useState('text-embedding-3-large');

  const documentsList = [
    {
      id: 'doc-1',
      fileName: 'Software_Architecture_Patterns_v2.pdf',
      md5: 'a4f8...b912',
      category: 'Giáo trình chính',
      pages: 184,
      size: '14.2 MB',
      chunksCount: 412,
      chunkConfig: '512 tokens / overlap 50',
      splitter: 'RecursiveCharacter',
      status: 'Indexed 100%'
    },
    {
      id: 'doc-2',
      fileName: 'SWD392_Course_Syllabus_Fall2026.pdf',
      md5: 'e1c9...88a2',
      category: 'Đề cương chi tiết & Rubric chuẩn',
      pages: 28,
      size: '2.1 MB',
      chunksCount: 64,
      chunkConfig: '512 tokens / overlap 50',
      splitter: 'HeaderMarkdownSplitter',
      status: 'Indexed 100%'
    },
    {
      id: 'doc-3',
      fileName: 'Microservices_Architecture_Design.pdf',
      md5: 'f7b3...d104',
      category: 'Tài liệu tham khảo nâng cao Module 4',
      pages: 120,
      size: '8.9 MB',
      chunksCount: 280,
      chunkConfig: '512 tokens / overlap 50',
      splitter: 'RecursiveCharacter',
      status: 'Indexed 100%'
    }
  ];

  const handleUploadSubmit = (e) => {
    e.preventDefault();
    setIsUploadModalOpen(false);
    if (showToast) {
      showToast({
        type: 'success',
        title: 'RAG Indexing Complete',
        message: `Đã tải tài liệu lên và băm nhỏ (${chunkSize} tokens) nhúng vector thành công vào ChromaDB!`
      });
    }
  };

  const handleReindex = (docName) => {
    if (showToast) {
      showToast({
        type: 'info',
        title: 'Tái Lập Chỉ Mục',
        message: `Đang tính toán lại Vector Embeddings cho ${docName}...`
      });
    }
  };

  const handleOpenChunks = (doc) => {
    setSelectedDocForChunks(doc);
    setIsChunksModalOpen(true);
  };

  return (
    <div className="space-y-6 animate-modal-entry">
      {/* STEPPER NAVIGATION BAR */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-2xs">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
          {/* Step 1: Active */}
          <div className="flex items-center gap-3 p-3 rounded-xl bg-sky-50 border border-sky-200">
            <div className="w-8 h-8 rounded-lg bg-sky-600 text-white font-bold flex items-center justify-center shrink-0">1</div>
            <div>
              <span className="text-[10px] uppercase font-bold text-sky-700 block">Đang Thực Hiện</span>
              <p className="font-bold text-slate-900">Kho Tài Liệu RAG &amp; Vector Store</p>
            </div>
            <CheckCircle2 className="w-5 h-5 text-sky-600 ml-auto" />
          </div>

          {/* Step 2 */}
          <button 
            onClick={() => onNavigate && onNavigate('lecturer-questions')}
            className="flex items-center gap-3 p-3 rounded-xl bg-slate-50 border border-slate-200 hover:bg-slate-100 transition-colors text-left"
          >
            <div className="w-8 h-8 rounded-lg bg-slate-200 text-slate-700 font-bold flex items-center justify-center shrink-0">2</div>
            <div>
              <span className="text-[10px] uppercase font-bold text-slate-400 block">Bước Tiếp Theo</span>
              <p className="font-bold text-slate-800">Question Studio &amp; Adaptive Probing</p>
            </div>
            <ArrowRight className="w-4 h-4 text-slate-400 ml-auto" />
          </button>

          {/* Step 3 */}
          <button 
            onClick={() => onNavigate && onNavigate('lecturer-grading-queue')}
            className="flex items-center gap-3 p-3 rounded-xl bg-slate-50 border border-slate-200 hover:bg-slate-100 transition-colors text-left"
          >
            <div className="w-8 h-8 rounded-lg bg-slate-200 text-slate-700 font-bold flex items-center justify-center shrink-0">3</div>
            <div>
              <span className="text-[10px] uppercase font-bold text-slate-400 block">Bước 3</span>
              <p className="font-bold text-slate-800">Hàng Đợi Chấm &amp; Duyệt FAP</p>
            </div>
            <ArrowRight className="w-4 h-4 text-slate-400 ml-auto" />
          </button>
        </div>
      </div>

      {/* HEADER & EMBEDDING ENGINE STATUS */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h1 className="font-heading font-extrabold text-2xl text-slate-900 tracking-tight">
            Kho Tài Liệu Khảo Thí &amp; Đánh Chỉ Mục Vector (RAG)
          </h1>
          <p className="text-xs text-slate-500 mt-1 max-w-2xl leading-relaxed">
            Các giáo trình, slide bài giảng và tài liệu chuẩn được băm nhỏ (chunking) và nhúng vector vào ChromaDB để AI Examiner đối chiếu câu trả lời của thí sinh.
          </p>
        </div>

        {/* Embedding Engine Telemetry Pill */}
        <div className="p-3 bg-white border border-slate-200/90 rounded-2xl shadow-2xs flex items-center gap-3 text-xs">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
          <div>
            <p className="font-bold text-slate-900">text-embedding-3-large • 1536 dims</p>
            <p className="text-[11px] text-slate-500 font-mono">Vector DB: ChromaDB v0.4.22 (PostgreSQL pgvector)</p>
          </div>
        </div>
      </div>

      {/* DRAG AND DROP INGESTION CARD */}
      <div 
        onClick={() => setIsUploadModalOpen(true)} 
        className="border-2 border-dashed border-sky-400/50 hover:border-sky-500 bg-sky-50/40 hover:bg-sky-50/80 rounded-2xl p-8 text-center cursor-pointer transition-all shadow-2xs"
      >
        <div className="max-w-md mx-auto space-y-3 pointer-events-none">
          <div className="w-14 h-14 rounded-2xl bg-white text-sky-600 flex items-center justify-center mx-auto shadow-md border border-sky-100">
            <UploadCloud className="w-8 h-8" />
          </div>
          <div>
            <h3 className="font-heading font-bold text-slate-900 text-base">Kéo &amp; thả file PDF tài liệu môn học vào đây</h3>
            <p className="text-xs text-slate-500 mt-1">Hỗ trợ PDF giáo trình, slide thuyết trình (tối đa 50MB/file)</p>
          </div>
          <button className="px-4 py-2 rounded-xl bg-white border border-slate-300 text-slate-800 text-xs font-bold shadow-2xs inline-flex items-center gap-1.5">
            <Paperclip className="w-4 h-4 text-sky-600" />
            <span>Chọn File Từ Máy Tính</span>
          </button>
        </div>
      </div>

      {/* INDEXED DOCUMENTS TABLE */}
      <div className="bg-white border border-slate-200/90 rounded-2xl shadow-2xs overflow-hidden">
        {/* Table Toolbar */}
        <div className="p-4 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2">
            <h3 className="font-heading font-bold text-slate-900 text-sm">Danh Mục Tài Liệu Đã Đánh Chỉ Mục</h3>
            <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 font-bold text-[10px]">3 Tài Liệu Active</span>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-2.5 top-2 text-slate-400" />
              <input 
                type="text" 
                placeholder="Lọc theo tên tài liệu..." 
                className="pl-8 pr-3 py-1.5 rounded-xl border border-slate-200 text-xs w-48 focus:ring-2 focus:ring-sky-500 focus:outline-none"
              />
            </div>
            <button 
              onClick={() => {
                if (showToast) showToast({ type: 'success', message: 'Đã đồng bộ lại toàn bộ Vector Index với ChromaDB!' });
              }} 
              className="p-1.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 transition" 
              title="Đồng bộ lại"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Table View */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200/80 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                <th className="py-3.5 px-4">Tên Tài Liệu &amp; Định Dạng</th>
                <th className="py-3.5 px-4">Số Trang / Kích Thước</th>
                <th className="py-3.5 px-4">Số Chunks</th>
                <th className="py-3.5 px-4">Cấu Hình Chunking</th>
                <th className="py-3.5 px-4">Trạng Thái Vector</th>
                <th className="py-3.5 px-4 text-right">Hành Động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {documentsList.map((doc) => (
                <tr key={doc.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="py-3.5 px-4">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center font-bold">
                        PDF
                      </div>
                      <div>
                        <p className="font-bold text-slate-900">{doc.fileName}</p>
                        <p className="text-[11px] text-slate-400 font-mono">MD5: {doc.md5} • {doc.category}</p>
                      </div>
                    </div>
                  </td>
                  <td className="py-3.5 px-4 whitespace-nowrap">
                    <span className="font-medium text-slate-700">{doc.pages} Trang</span>
                    <span className="block text-[11px] text-slate-400">{doc.size}</span>
                  </td>
                  <td className="py-3.5 px-4 whitespace-nowrap">
                    <span className="font-mono font-bold text-sky-700 bg-sky-50 px-2 py-0.5 rounded border border-sky-200">
                      {doc.chunksCount} Chunks
                    </span>
                  </td>
                  <td className="py-3.5 px-4 whitespace-nowrap">
                    <span className="text-slate-600">{doc.chunkConfig}</span>
                    <span className="block text-[11px] text-slate-400">{doc.splitter}</span>
                  </td>
                  <td className="py-3.5 px-4 whitespace-nowrap">
                    <span className="px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 font-bold text-[10px] inline-flex items-center gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
                      {doc.status}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 whitespace-nowrap text-right space-x-1">
                    <button 
                      onClick={() => handleOpenChunks(doc)} 
                      className="px-2.5 py-1 rounded-lg border border-slate-200 hover:bg-slate-100 text-sky-700 font-bold transition active:scale-95"
                    >
                      Xem Chunks
                    </button>
                    <button 
                      onClick={() => handleReindex(doc.fileName)} 
                      className="p-1 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-600 transition" 
                      title="Re-index"
                    >
                      <RefreshCw className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* ================= MODAL 1: UPLOAD & RAG SETTINGS ================= */}
      {isUploadModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col animate-modal-entry">
            <div className="p-5 border-b border-slate-100 bg-slate-950 text-white flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-sky-500/20 text-sky-300 flex items-center justify-center">
                  <UploadCloud className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-heading font-bold text-sm">Tải Lên &amp; Cấu Hình Chunking RAG</h3>
                  <p className="text-[11px] text-slate-400">Tùy biến kích thước đoạn và mô hình vector</p>
                </div>
              </div>
              <button onClick={() => setIsUploadModalOpen(false)} className="text-slate-400 hover:text-white p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleUploadSubmit} className="p-5 space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1">Chọn File PDF:</label>
                <input 
                  type="file" 
                  accept=".pdf" 
                  required 
                  className="w-full p-2 border border-slate-200 rounded-xl bg-slate-50 text-slate-700 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Kích thước Chunk (Tokens):</label>
                  <select 
                    value={chunkSize}
                    onChange={(e) => setChunkSize(e.target.value)}
                    className="w-full p-2 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                  >
                    <option value="256">256 tokens (Nhỏ)</option>
                    <option value="512">512 tokens (Khuyên Dùng)</option>
                    <option value="1024">1024 tokens (Lớn)</option>
                  </select>
                </div>
                <div>
                  <label className="block font-bold text-slate-700 mb-1">Độ Gối Đầu (Chunk Overlap):</label>
                  <select 
                    value={chunkOverlap}
                    onChange={(e) => setChunkOverlap(e.target.value)}
                    className="w-full p-2 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                  >
                    <option value="20">20 tokens</option>
                    <option value="50">50 tokens (Chuẩn)</option>
                    <option value="100">100 tokens</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Embedding Model:</label>
                <select 
                  value={embeddingModel}
                  onChange={(e) => setEmbeddingModel(e.target.value)}
                  className="w-full p-2 text-xs rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                >
                  <option value="text-embedding-3-large">text-embedding-3-large (1536 dim - Chất lượng cao)</option>
                  <option value="text-embedding-3-small">text-embedding-3-small (512 dim - Tốc độ nhanh)</option>
                  <option value="bge-large-en-v1.5">bge-large-en-v1.5 (Local On-Premises)</option>
                </select>
              </div>

              <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-3 -mx-5 -mb-5 mt-5">
                <button 
                  type="button" 
                  onClick={() => setIsUploadModalOpen(false)} 
                  className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold"
                >
                  Hủy
                </button>
                <button 
                  type="submit" 
                  className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold shadow-sm active:scale-95 transition"
                >
                  Bắt Đầu Đánh Chỉ Mục
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ================= MODAL 2: INSPECT VECTOR CHUNKS ================= */}
      {isChunksModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/25 backdrop-blur-[2px] flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col max-h-[85vh] animate-modal-entry">
            <div className="p-5 border-b border-slate-100 bg-slate-950 text-white flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-sky-500/20 text-sky-300 flex items-center justify-center">
                  <Database className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-heading font-bold text-sm">
                    Chi Tiết Vector Chunks: {selectedDocForChunks?.fileName || 'Document'}
                  </h3>
                  <p className="text-[11px] text-slate-400">
                    {selectedDocForChunks?.chunksCount || 412} Chunks được lưu trữ tại ChromaDB
                  </p>
                </div>
              </div>
              <button onClick={() => setIsChunksModalOpen(false)} className="text-slate-400 hover:text-white p-1 rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 overflow-y-auto space-y-3 text-xs">
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-1">
                <div className="flex items-center justify-between font-bold text-sky-700">
                  <span>Chunk #142-A • Trang 141</span>
                  <span className="font-mono text-[10px] bg-sky-100 text-sky-800 px-2 py-0.5 rounded font-bold">Tokens: 489</span>
                </div>
                <p className="text-slate-700 leading-relaxed">
                  "Kiến trúc Monolith truyền thống thường gặp vấn đề khi cơ sở dữ liệu phình to và các team bị phụ thuộc chéo vào nhau trong chu kỳ phát hành..."
                </p>
              </div>

              <div className="p-3.5 bg-amber-50 rounded-xl border border-amber-200 space-y-1">
                <div className="flex items-center justify-between font-bold text-amber-900">
                  <span>Chunk #142-B • Trang 142 (Căn cứ Question #SWD-M4-019)</span>
                  <span className="font-mono text-[10px] bg-amber-200 text-amber-900 px-2 py-0.5 rounded font-bold">Active Grounding</span>
                </div>
                <p className="text-amber-950 leading-relaxed italic">
                  "Điểm khác biệt kiến trúc cốt lõi giữa Monolith và Microservices nằm ở tính tự chủ triển khai (deployment autonomy) và sự cô lập vùng ảnh hưởng khi xảy ra sự cố (blast radius isolation)..."
                </p>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-1">
                <div className="flex items-center justify-between font-bold text-sky-700">
                  <span>Chunk #142-C • Trang 143</span>
                  <span className="font-mono text-[10px] bg-sky-100 text-sky-800 px-2 py-0.5 rounded font-bold">Tokens: 504</span>
                </div>
                <p className="text-slate-700 leading-relaxed">
                  "Để giải quyết bài toán giao dịch phân tán khi chuyển đổi sang microservices, các mẫu thiết kế SAGA và Transactional Outbox được áp dụng..."
                </p>
              </div>
            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end">
              <button 
                onClick={() => setIsChunksModalOpen(false)} 
                className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold shadow-sm"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
