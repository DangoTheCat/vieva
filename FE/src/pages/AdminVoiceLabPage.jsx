import React, { useState, useEffect } from 'react';
import { speechApi } from '../api/speechApi';
import { getErrorMessage } from '../utils/errorCodes';
import { Volume2, Save, Loader2, Square, Mic, RotateCcw } from 'lucide-react';

const LOCALES = [
  { value: 'vi-VN', label: 'Tiếng Việt (vi-VN)' },
  { value: 'en-US', label: 'English (en-US)' }
];

const inputClass = 'w-full bg-white border-2 border-green-300 rounded-xl p-2.5 text-xs focus:outline-none focus:ring-2 focus:ring-green-600';

const toForm = (s) => ({
  sttLanguage: s?.sttLanguage || 'vi-VN',
  ttsLanguage: s?.ttsLanguage || 'vi-VN',
  vietnameseVoice: s?.vietnameseVoice || '',
  englishVoice: s?.englishVoice || '',
  speed: s?.speed != null ? Number(s.speed) : 1,
  pitch: s?.pitch != null ? Number(s.pitch) : 1
});

/**
 * System speech settings (GET/PATCH /admin/speech) applied to every oral exam room.
 * The BE validates locales/voices against the configured TTS provider.
 */
export function AdminVoiceLabPage({ showToast }) {
  const [settings, setSettings] = useState(null);
  const [form, setForm] = useState(toForm(null));
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isPreviewing, setIsPreviewing] = useState(false);
  const [previewText, setPreviewText] = useState('Xin chào, em hãy trình bày ngắn gọn ý tưởng chính của câu trả lời.');

  const load = async () => {
    setIsLoading(true);
    try {
      const data = await speechApi.getAdminSettings();
      setSettings(data);
      setForm(toForm(data));
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    load();
    return () => window.speechSynthesis?.cancel();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      const updated = await speechApi.updateSettings({
        sttLanguage: form.sttLanguage,
        ttsLanguage: form.ttsLanguage,
        vietnameseVoice: form.vietnameseVoice.trim() || null,
        englishVoice: form.englishVoice.trim() || null,
        speed: Number(form.speed),
        pitch: Number(form.pitch)
      });
      setSettings(updated);
      setForm(toForm(updated));
      showToast({ type: 'success', message: 'Đã lưu cấu hình giọng nói cho phòng thi.' });
    } catch (err) {
      showToast({ type: 'error', message: getErrorMessage(err) });
    } finally {
      setIsSaving(false);
    }
  };

  // Browser preview only approximates speed/pitch; the exam room uses the configured TTS service voice
  const handlePreview = () => {
    if (!('speechSynthesis' in window)) {
      showToast({ type: 'warning', message: 'Trình duyệt không hỗ trợ đọc thử.' });
      return;
    }
    window.speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(previewText);
    utter.lang = form.ttsLanguage;
    utter.rate = Math.min(Number(form.speed) || 1, 10);
    utter.pitch = Math.min(Number(form.pitch) || 1, 2);
    utter.onend = () => setIsPreviewing(false);
    utter.onerror = () => setIsPreviewing(false);
    setIsPreviewing(true);
    window.speechSynthesis.speak(utter);
  };

  const stopPreview = () => {
    window.speechSynthesis?.cancel();
    setIsPreviewing(false);
  };

  return (
    <div className="max-w-3xl mx-auto w-full space-y-6 pb-10 text-ink animate-modal-entry">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-heading font-extrabold text-2xl tracking-tight">Cấu Hình Giọng Nói</h1>
          <p className="text-xs text-green-800 mt-1">Ngôn ngữ nhận diện (STT), giọng đọc (TTS), tốc độ và cao độ dùng trong phòng thi vấn đáp.</p>
        </div>
        <button type="button" onClick={load} disabled={isLoading}
          className="px-3 py-1.5 rounded-xl bg-white border-2 border-green-500 text-xs font-bold flex items-center gap-1.5 hover:bg-green-50 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-green-600">
          <RotateCcw className="w-3.5 h-3.5" /> Tải lại
        </button>
      </header>

      {isLoading ? (
        <div className="h-72 rounded-2xl animate-pulse bg-slate-200/80" />
      ) : (
        <form onSubmit={handleSave} className="bg-white border-2 border-green-500 rounded-2xl p-6 shadow-[4px_4px_0_#DEB9B5] space-y-5 text-xs">
          <div className="grid sm:grid-cols-2 gap-4">
            <label className="block font-bold space-y-1">
              <span className="flex items-center gap-1.5"><Mic className="w-3.5 h-3.5" /> Ngôn ngữ nhận diện (STT)</span>
              <select value={form.sttLanguage} onChange={e => setForm({ ...form, sttLanguage: e.target.value })} className={inputClass}>
                {LOCALES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
              </select>
            </label>
            <label className="block font-bold space-y-1">
              <span className="flex items-center gap-1.5"><Volume2 className="w-3.5 h-3.5" /> Ngôn ngữ đọc (TTS)</span>
              <select value={form.ttsLanguage} onChange={e => setForm({ ...form, ttsLanguage: e.target.value })} className={inputClass}>
                {LOCALES.map(l => <option key={l.value} value={l.value}>{l.label}</option>)}
              </select>
            </label>
            <label className="block font-bold space-y-1">
              <span>Giọng tiếng Việt</span>
              <input value={form.vietnameseVoice} onChange={e => setForm({ ...form, vietnameseVoice: e.target.value })} className={inputClass} placeholder="Mã giọng của dịch vụ TTS" />
            </label>
            <label className="block font-bold space-y-1">
              <span>Giọng tiếng Anh (tuỳ chọn)</span>
              <input value={form.englishVoice} onChange={e => setForm({ ...form, englishVoice: e.target.value })} className={inputClass} placeholder="Để trống nếu không dùng" />
            </label>
          </div>

          <div className="grid sm:grid-cols-2 gap-4">
            {[
              { key: 'speed', label: 'Tốc độ đọc' },
              { key: 'pitch', label: 'Cao độ' }
            ].map(({ key, label }) => (
              <label key={key} className="block font-bold space-y-1">
                <span className="flex justify-between"><span>{label}</span><span className="font-mono">{Number(form[key]).toFixed(2)}×</span></span>
                <input type="range" min="0.25" max="4" step="0.05" value={form[key]}
                  onChange={e => setForm({ ...form, [key]: e.target.value })} className="w-full accent-green-600" />
              </label>
            ))}
          </div>
          <p className="text-[11px] text-green-800">
            Dịch vụ TTS có thể không hỗ trợ đổi tốc độ/cao độ — khi đó hệ thống sẽ báo lỗi và chỉ chấp nhận giá trị 1.00×.
          </p>

          <div className="space-y-2 pt-3 border-t-2 border-green-100">
            <span className="font-bold block">Nghe thử (giọng của trình duyệt, chỉ mô phỏng tốc độ/cao độ)</span>
            <div className="flex gap-2">
              <input value={previewText} onChange={e => setPreviewText(e.target.value)} className={inputClass} />
              <button type="button" onClick={isPreviewing ? stopPreview : handlePreview}
                className="px-3 py-2 rounded-xl bg-white border-2 border-green-400 font-bold flex items-center gap-1.5 hover:bg-green-50 shrink-0 focus:outline-none focus:ring-2 focus:ring-green-600">
                {isPreviewing ? <Square className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />} {isPreviewing ? 'Dừng' : 'Nghe'}
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between gap-3 pt-3 border-t-2 border-green-100">
            <p className="text-[11px] text-green-800">
              {settings?.updatedAt ? `Cập nhật lần cuối: ${new Date(settings.updatedAt).toLocaleString('vi-VN')}` : 'Đang dùng cấu hình mặc định'}
            </p>
            <button type="submit" disabled={isSaving}
              className="px-4 py-2 rounded-xl bg-green-600 hover:bg-green-700 text-white font-bold flex items-center gap-1.5 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-green-600 focus:ring-offset-2">
              {isSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />} Lưu cấu hình
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
